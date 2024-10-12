package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.*;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This PLC is a ModBusTCP server that will be used to simulate the sensors in the testbed.
 * ----------------------------------------------------------------------------------------------------------------------------------
 * | Address        |  Type   | Name                                       | Description                                            |
 * |----------------|---------|--------------------------------------------|--------------------------------------------------------|
 * | Coil           |  00001  | Start command                              | True if the ride is running                            |
 * | Discrete Input |  10001  | Sensors checkpoint 1 (before first slope)  | Boolean indicating if the ride has passed checkpoint 1 |
 * | Discrete Input |  10002  | Sensors checkpoint 2 (first brake)         | Boolean indicating if the ride has passed checkpoint 2 |
 * | Discrete Input |  10003  | Sensors checkpoint 3 (second brake)        | Boolean indicating if the ride has passed checkpoint 3 |
 * | Discrete Input |  10004  | Sensors checkpoint 4 (last brake)          | Boolean indicating if the ride has passed checkpoint 4 |
 * | Discrete Input |  10005  | Sensors checkpoint 5 (ready to start)      | Boolean indicating if the ride has passed checkpoint 5 |
 * ----------------------------------------------------------------------------------------------------------------------------------
 */
public abstract class ProgressionModbusService extends ReadWriteModbusServices {
    @Getter
    private enum DataAddresses {
        START(1),
        CHECKPOINT_1(10001),
        CHECKPOINT_2(10002),
        CHECKPOINT_3(10003),
        CHECKPOINT_4(10004),
        CHECKPOINT_5(10005);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }

    }

    private static int elapsedTime = 0;       // Temps écoulé en secondes
    private static final int DURATION = 4 * 60 + 52; // Durée du trajet en secondes
    // TODO : add IP addresses for brakes

    private static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(ProgressionModbusService.class.getName());
    }

    public ProgressionModbusService() throws UnknownUnitIdException {
        super();
        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        processImage.addModificationListener(new ProcessImage.ModificationListener() {
            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();
                    LOGGER.log(Level.INFO, "Coil at address {0} set to {1}", new Object[]{address, value});

                    if (address == DataAddresses.START.getAddress() && value) {
                        try {
                            launchRide();
                        } catch (UnknownUnitIdException e) {
                            LOGGER.severe("Could not start ride.");
                        }
                    }
                });
            }

            @Override
            public void onDiscreteInputsModified(List<ProcessImage.Modification.DiscreteInputModification> list) {

            }

            @Override
            public void onHoldingRegistersModified(List<ProcessImage.Modification.HoldingRegisterModification> list) {

            }

            @Override
            public void onInputRegistersModified(List<ProcessImage.Modification.InputRegisterModification> list) {

            }
        });
    }

    private void resetCheckpoints(ProcessImage processImage) {
        processImage.with(tx -> tx.writeDiscreteInputs(coilMap -> {
            coilMap.remove(DataAddresses.CHECKPOINT_1.getAddress());
            coilMap.remove(DataAddresses.CHECKPOINT_2.getAddress());
            coilMap.remove(DataAddresses.CHECKPOINT_3.getAddress());
            coilMap.remove(DataAddresses.CHECKPOINT_4.getAddress());
            coilMap.remove(DataAddresses.CHECKPOINT_5.getAddress());
        }));
    }

    private void setCheckpoint(ProcessImage processImage, DataAddresses checkpoint) {
        processImage.with(tx -> tx.writeDiscreteInputs(coilMap -> {
            coilMap.put(checkpoint.getAddress(), true);
        }));
    }

    public void launchRide() throws UnknownUnitIdException {
        LOGGER.info("ProgressionPLC routine started.");
        if (elapsedTime != 0) {
            LOGGER.warning("ProgressionPLC routine already running.");
            return;
        }

        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        resetCheckpoints(processImage);

        LOGGER.info("Starting ride.");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        // TODO : Implement modbus messages to activate brakes
        executor.scheduleAtFixedRate(() -> {
            elapsedTime++;
            if (elapsedTime == 30) {
                setCheckpoint(processImage, DataAddresses.CHECKPOINT_1);
                LOGGER.info("Checkpoint 1 reached (Before first slope).");
            } else if (elapsedTime == 70) {
                setCheckpoint(processImage, DataAddresses.CHECKPOINT_2);
                LOGGER.info("Checkpoint 2 reached (First brake).");
            } else if (elapsedTime == 144) {
                setCheckpoint(processImage, DataAddresses.CHECKPOINT_3);
                LOGGER.info("Checkpoint 3 reached (Second brake).");
            } else if (elapsedTime == 216) {
                setCheckpoint(processImage, DataAddresses.CHECKPOINT_4);
                LOGGER.info("Checkpoint 4 reached (Last brake).\nRide completed.");
            } else if (elapsedTime == 289) {
                setCheckpoint(processImage, DataAddresses.CHECKPOINT_5);
                LOGGER.info("Checkpoint 5 reached (Ready to start).");
            }

            if (elapsedTime >= DURATION) {
                elapsedTime = 0;
                executor.shutdown();
            }
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
}
