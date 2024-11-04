package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This PLC is a ModBusTCP server that will be used to simulate the sensors in the testbed.
 * ----------------------------------------------------------------------------------------------------------------------------------
 * |     Type    |  Address   | Name                                       | Description                                            |
 * |----------------|---------|--------------------------------------------|--------------------------------------------------------|
 * | Coil           |  00004  |  Fence                                     | True if the fence are closed                           |
 * | Coil           |  00005  |  Seatbelt                                  | True if the seatbelt is locked                         |
 * ----------------------------------------------------------------------------------------------------------------------------------
 */
public abstract class SecurityModbusService extends ReadWriteModbusServices {
    @Getter
    private enum DataAddresses {
        C_FENCE(4),
        C_SEATBELT(5);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }

    }

    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(SecurityModbusService.class.getName());
    }

    public SecurityModbusService() throws UnknownUnitIdException {
        super();
        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        processImage.addModificationListener(new ProcessImage.ModificationListener() {
            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();
                    LOGGER.log(Level.INFO, "Coil at address {0} set to {1}", new Object[]{address, value});

                    if (address == DataAddresses.C_FENCE.getAddress()) {
                        LOGGER.info("Fence set to " + value + ", " + (value ? "closing the fence": "opening the fence"));
                    }

                    if (address == DataAddresses.C_SEATBELT.getAddress()) {
                        LOGGER.info("Seatbelt set to " + value + ", " + (value ? "locking the seatbelt": "unlocking the seatbelt"));
                    }
                });
            }

            @Override
            public void onDiscreteInputsModified(List<ProcessImage.Modification.DiscreteInputModification> list) {
                LOGGER.severe("Discrete inputs modified but no discrete inputs are manged by this plc.");
            }

            @Override
            public void onHoldingRegistersModified(List<ProcessImage.Modification.HoldingRegisterModification> list) {
                LOGGER.severe("Holding registers modified but no holding registers are manged by this plc.");
            }

            @Override
            public void onInputRegistersModified(List<ProcessImage.Modification.InputRegisterModification> list) {
                LOGGER.severe("Input registers modified but no input registers are manged by this plc.");
            }
        });
    }

}
