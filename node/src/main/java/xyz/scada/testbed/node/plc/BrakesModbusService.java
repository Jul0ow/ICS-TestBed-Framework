package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

/**
 * This PLC is a ModBusTCP server that will be used to simulate the brakes
 * ------------------------------------------------------------------------------------------------------------------------------------
 * | Address          |  Type   | Name                                       | Description                                            |
 * |------------------|---------|--------------------------------------------|--------------------------------------------------------|
 * | Coil             |  00002  |  Emergency brakes                          | True if the emergency brakes are in used               |
 * | Coil             |  00003  |  Parking brakes                            | True if the parking brakes are in used                 |
 * | Input Register   |  30001  |  Brakes Pressure                           | 16 bits value indicating the pressure in the brakes    |
 * | Input Register   |  30002  |  Activation Percentage                     | Percentage of brakes usage                             |
 * | Holding Register |  40001  |  Requested brakes activation               | Percentage of requested brakes usage                   |
 * ------------------------------------------------------------------------------------------------------------------------------------
 */
public abstract class BrakesModbusService extends ReadWriteModbusServices {
    @Getter
    private enum DataAddresses {
        EMERGENCY_BRAKES(2), PARKING_BRAKES(3), BRAKES_PRESSURE(30001), ACTIVATION_PERCENTAGE(30002), REQUESTED_BRAKES_ACTIVATION(40001);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }
    }

    /* --------- Simulation Part -------- */
    private static final Logger LOGGER;
    // Coefficient that link requested breaking with actual breaking pressure
    private int coeffBrakesPressure = 6;
    // Delay between request breaking and breaking pressure increasing (in seconds)
    private Duration breakingDelay = Duration.of(600, ChronoUnit.MILLIS);
    // time since the start of breaking (in seconds), used for the sigmoid function
    private Instant startBreakingTime = null;
    // Bar pressure for the break while non-breaking
    private float unbreakingPressure = 0;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(BrakesModbusService.class.getName());
    }

    private int getPressureInputRegister(ProcessImage processImage) {
        byte[] pressureValues = processImage.get(transaction -> transaction.readInputRegisters(integerMap -> integerMap
                .get(DataAddresses.BRAKES_PRESSURE.getAddress())));

        // Convert the byte array to a pressure value in kPa
        int pressure = 0;
        for (byte b : pressureValues) {
            pressure = (pressure << 8) + (b & 0xFF);
        }
        return pressure;
    }

    private void setPressureInputRegister(ProcessImage processImage, int pressure) {
        byte[] pressureValues = {(byte) (pressure >> 8), (byte) (pressure & 0xFF)};

        processImage.with(transaction -> transaction
                .writeInputRegisters(map ->
                        map.put(DataAddresses.BRAKES_PRESSURE.getAddress(), pressureValues)));
    }

    /*
    Fonction de latence sigmoïde pour la pression des freins
    Sigmoid function for delay in braking pression
     */
    private int getBrakingPressure(int requestedBraking) {
        int pressureMax = requestedBraking * coeffBrakesPressure;

        Duration timeElapsed = Duration.between(Instant.now(), startBreakingTime);
        float k = 0.7f;
        if (timeElapsed.compareTo(breakingDelay) < 0) {
            return 0;
        } else {
            return (int) (pressureMax / (1 + (float) Math.exp(-k * timeElapsed.toMillis() / 1000)));
        }
    }

    private int getUnbrakingPressure(int requestedBraking) {
        float pressureMax = requestedBraking * coeffBrakesPressure;

        Duration timeElapsed = Duration.between(Instant.now(), startBreakingTime);
        float k = 0.7f;
        if (timeElapsed.compareTo(breakingDelay) < 0) {
            return 0;
        } else {
            return (int) (pressureMax / (1 + (float) Math.exp(k * timeElapsed.toMillis() / 1000)));
        }
    }

    private void updateBrakePressure(int requestedBraking) throws UnknownUnitIdException {
        ProcessImage image = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        startBreakingTime = Instant.now();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            int nextPressure;
            if (getPressureInputRegister(image) < requestedBraking)
            {
                nextPressure = getBrakingPressure(requestedBraking);
            }
            else
            {
                nextPressure = getUnbrakingPressure(requestedBraking);
            }

            setPressureInputRegister(image, nextPressure);
            if (nextPressure == requestedBraking) {
                executor.shutdown();
            }

        }, 600, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /* --------- PLC Part -------- */

    public BrakesModbusService() throws UnknownUnitIdException {
        super();

        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        processImage.addModificationListener(new ProcessImage.ModificationListener() {

            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();

                    if (address == DataAddresses.EMERGENCY_BRAKES.getAddress() && value) {
                        LOGGER.info("Emergency brakes activated.");
                    }

                    if (address == DataAddresses.PARKING_BRAKES.getAddress() && value) {
                        LOGGER.info("Parking brakes activated.");
                    }
                });
            }

            @Override
            public void onDiscreteInputsModified(List<ProcessImage.Modification.DiscreteInputModification> list) {

            }

            @Override
            public void onHoldingRegistersModified(List<ProcessImage.Modification.HoldingRegisterModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    byte[] value = modification.value();

                    if (address == DataAddresses.REQUESTED_BRAKES_ACTIVATION.getAddress()) {
                        LOGGER.info("Requested brakes activation updated to " + (value[0] << 8 + value[1]));
                        try {
                            updateBrakePressure(value[0] << 8 + value[1]);
                        } catch (UnknownUnitIdException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public void onInputRegistersModified(List<ProcessImage.Modification.InputRegisterModification> list) {

            }
        });
    }
}
