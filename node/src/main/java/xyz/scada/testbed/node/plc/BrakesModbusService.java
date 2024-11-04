package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
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
    /* --------- Simulation Part -------- */
    // Delay between request breaking and breaking pressure increasing (in seconds)
    private final Duration breakingDelay = Duration.of(600, ChronoUnit.MILLIS);
    // time since the start of breaking (in seconds), used for the sigmoid function
    private Instant startBreakingTime = null;
    private int initialBreakingPressure = -1;
    int MINIMAL_PRESSURE = 100;
    int MAXIMAL_PRESSURE = 600;
    boolean isEmergencyBrake = false;
    boolean isParkingBrake = false;

    ScheduledFuture<?> currentBraking;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(BrakesModbusService.class.getName());
    }

    private int getPressureFromPercentage(int percentage) {
        if (percentage > 100 | percentage < 0) {
            throw new IllegalArgumentException("Percentage given is not 0-100 " + percentage);
        }
        return percentage * (MAXIMAL_PRESSURE - MINIMAL_PRESSURE) / 100 + MINIMAL_PRESSURE;
    }

    private int getPercentageFromPressure(int pressure) {
        if (pressure > MAXIMAL_PRESSURE | pressure < MINIMAL_PRESSURE) {
            throw new IllegalArgumentException("Pressure is out of bound" + pressure);
        }
        return (pressure - MINIMAL_PRESSURE) * 100 / (MAXIMAL_PRESSURE - MINIMAL_PRESSURE);
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

    private boolean getEmergencyCoilRegister(ProcessImage processImage) {
        Boolean emergencyBrakesActivated = processImage.get(transaction -> transaction.readCoils(integerMap -> integerMap
                .get(DataAddresses.EMERGENCY_BRAKES.getAddress())));

        LOGGER.info("Getting emergency coil register " + emergencyBrakesActivated);

        return emergencyBrakesActivated;
    }

    private boolean getParkingCoilRegister(ProcessImage processImage) {
        boolean parkingBrakesActivated = processImage.get(transaction -> transaction.readCoils(integerMap -> integerMap
                .get(DataAddresses.PARKING_BRAKES.getAddress())));

        LOGGER.info("Getting parking coil register " + parkingBrakesActivated);

        return parkingBrakesActivated;
    }

    private void setPressureInputRegister(ProcessImage processImage, int pressure) {
        int percentage = getPercentageFromPressure(pressure);
        LOGGER.info("Setting pressure : " + pressure + " and percentage : " + percentage);
        byte[] pressureValues = {(byte) (pressure >> 8), (byte) (pressure & 0xFF)};
        byte[] percentageValues = {(byte) (percentage >> 8), (byte) (percentage & 0xFF)};

        processImage.with(transaction -> transaction
                .writeInputRegisters(map -> {
                    map.put(DataAddresses.BRAKES_PRESSURE.getAddress(), pressureValues);
                    map.put(DataAddresses.ACTIVATION_PERCENTAGE.getAddress(), percentageValues);
                }));
    }

    private void setEmergencyCoil(ProcessImage processImage, boolean isActivated) {
        LOGGER.info("Setting emergency coil to " + isActivated);

        processImage.with(transaction -> transaction
                .writeCoils(map -> map.put(DataAddresses.EMERGENCY_BRAKES.getAddress(), isActivated)));
    }

    private void setParkingCoil(ProcessImage processImage, boolean isActivated) {
        LOGGER.info("Setting parking coil to " + isActivated);

        processImage.with(transaction -> transaction
                .writeCoils(map -> map.put(DataAddresses.PARKING_BRAKES.getAddress(), isActivated)));
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
            return requestedBraking + (int) Math.floor((initialBreakingPressure - requestedBraking) / (1 + (float) Math.exp(k * timeElapsed.minus(breakingDelay).toMillis() / 1000)));
        }
    }

    private void updateBrakePressure(int requestedBraking) throws UnknownUnitIdException {
        LOGGER.info("Scheduling braking to " + requestedBraking);

        if (currentBraking != null && !currentBraking.isDone()) {
            LOGGER.info("Cancelling previous brake");
            initialBreakingPressure = -1;
            currentBraking.cancel(true);
        }

        startBreakingTime = Instant.now();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        ProcessImage image = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));

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
        setEmergencyCoil(processImage, false);
        setParkingCoil(processImage, false);
        processImage.addModificationListener(new ProcessImage.ModificationListener() {

            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();

                    if (address == DataAddresses.EMERGENCY_BRAKES.getAddress()) {
                        LOGGER.info("Emergency brakes activated : " + value);
                        isEmergencyBrake = value;

                        if (isEmergencyBrake) {
                            try {
                                updateBrakePressure(getPressureFromPercentage(100));
                            } catch (UnknownUnitIdException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    if (address == DataAddresses.PARKING_BRAKES.getAddress()) {
                        LOGGER.info("Parking brakes activated : " + value);

                        isParkingBrake = value;
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
                        if (isEmergencyBrake)
                        {
                            LOGGER.warning("Cannot modify brakes pressure, emergency brakes activated");
                            return;
                        }
                        if (isParkingBrake)
                        {
                            LOGGER.warning("Cannot modify brakes pressure, parking brakes activated");
                            return;
                        }
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
