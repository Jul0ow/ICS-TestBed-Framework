package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This PLC is a ModBusTCP server that will be used to simulate the lights
 * ----------------------------------------------------------------------------------------------------------------------------------
 * | Address        |  Type   | Name                                       | Description                                            |
 * |----------------|---------|--------------------------------------------|--------------------------------------------------------|
 * | Coil           |  00006  | Lights                                     | True if the lights are on                              |
 * ----------------------------------------------------------------------------------------------------------------------------------
 */
public abstract class LightModbusService extends ReadWriteModbusServices {
    @Getter
    private enum DataAddresses {
        LIGHTS(6);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }
    }

    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(LightModbusService.class.getName());
    }

    public LightModbusService() throws UnknownUnitIdException {
        super();

        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        processImage.addModificationListener(new ProcessImage.ModificationListener() {
            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();
                    LOGGER.log(Level.INFO, "Coil at address {0} set to {1}", new Object[]{address, value});

                    if (address == DataAddresses.LIGHTS.getAddress()) {
                        LOGGER.info("Lights activated : " + value);
                    }
                });
            }

            @Override
            public void onDiscreteInputsModified(List<ProcessImage.Modification.DiscreteInputModification> list) {
                LOGGER.info("Discrete inputs modified.");
            }

            @Override
            public void onHoldingRegistersModified(List<ProcessImage.Modification.HoldingRegisterModification> list) {
                LOGGER.info("Holding registers modified.");
            }

            @Override
            public void onInputRegistersModified(List<ProcessImage.Modification.InputRegisterModification> list) {
                LOGGER.info("Input registers modified.");
            }
        });    }
}
