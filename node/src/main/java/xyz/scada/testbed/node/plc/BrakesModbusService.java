package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
        EMERGENCY_BRAKES(2),
        PARKING_BRAKES(3),
        BRAKES_PRESSURE(30001),
        ACTIVATION_PERCENTAGE(30002),
        REQUESTED_BRAKES_ACTIVATION(40001);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }
    }

    private static final Logger LOGGER;
    // Delay between request breaking and breaking pressure increasing (in seconds)
    private final Duration breakingDelay = Duration.of(600, ChronoUnit.MILLIS);
    // time since the start of breaking (in seconds), used for the sigmoid function
    private Instant startBreakingTime = null;
    private int initialBreakingPressure = -1;

    ScheduledFuture<?> currentBraking;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(BrakesModbusService.class.getName());
    }

    private int getPressureFromPercentage(int percentage) {
        if (percentage > 100 | percentage < 0) {
            throw new IllegalArgumentException("Percentage given is not 0-100 " + percentage);
        }
        /* --------- Simulation Part -------- */
        int MINIMAL_PRESSURE = 100;
        int MAXIMAL_PRESSURE = 600;
        return percentage * (MAXIMAL_PRESSURE - MINIMAL_PRESSURE) / 100 + MINIMAL_PRESSURE;
    }

    private int convertByteToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
    }

    private int getPressureInputRegister(ProcessImage processImage) {
        byte[] pressureValues = processImage.get(transaction -> transaction.readInputRegisters(integerMap -> integerMap
                .get(DataAddresses.BRAKES_PRESSURE.getAddress())));

        // Convert the byte array to a pressure value in kPa
        int res = convertByteToInt(pressureValues);
        LOGGER.info("Getting pressure " + res);

        return res;
    }

    private void setPressureInputRegister(ProcessImage processImage, int pressure) {
        LOGGER.info("Setting pressure " + pressure);
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
        Duration timeElapsed = Duration.between(startBreakingTime, Instant.now());
        float k = 0.7f;
        if (timeElapsed.compareTo(breakingDelay) < 0) {
            return 0;
        } else if (requestedBraking > initialBreakingPressure) {
            return initialBreakingPressure + (int) Math.ceil((requestedBraking - initialBreakingPressure) / (1 + (float) Math.exp(-k * timeElapsed.minus(breakingDelay).toMillis() / 1000)));
        } else {
            return requestedBraking + (int) Math.ceil((initialBreakingPressure - requestedBraking) / (1 + (float) Math.exp(k * timeElapsed.minus(breakingDelay).toMillis() / 1000)));
        }
    }

    private void updateBrakePressure(int requestedBraking) throws UnknownUnitIdException {
        LOGGER.info("Scheduling braking to " + requestedBraking);

        if (currentBraking != null && !currentBraking.isDone()) {
            LOGGER.info("Cancelling previous brake");
            initialBreakingPressure = -1;
            currentBraking.cancel(true);
        }

        ProcessImage image = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));

        startBreakingTime = Instant.now();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        currentBraking = executor.scheduleAtFixedRate(() -> {
            if (initialBreakingPressure == -1) {
                initialBreakingPressure = getPressureInputRegister(image);
            }
            int nextPressure = getBrakingPressure(requestedBraking);

            if (nextPressure != 0) {
                setPressureInputRegister(image, nextPressure);
            }

            if (nextPressure == requestedBraking) {
                initialBreakingPressure = -1;
                executor.shutdown();
            }
        }, 600, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /* --------- PLC Part -------- */

    public BrakesModbusService() throws UnknownUnitIdException {
        super();

        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));

        // Set default pressure
        setPressureInputRegister(processImage, 100);
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

                    LOGGER.info(Arrays.toString(value));

                    if (address == DataAddresses.REQUESTED_BRAKES_ACTIVATION.getAddress()) {
                        int percentage = convertByteToInt(value);

                        int pressure;
                        try {
                            pressure = getPressureFromPercentage(percentage);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning(e.getMessage());
                            return;
                        }

                        LOGGER.info("Requested brakes activation : " + percentage + " updated to " + pressure);
                        try {
                            updateBrakePressure(pressure);
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
