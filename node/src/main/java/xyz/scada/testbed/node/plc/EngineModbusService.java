package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import lombok.Getter;
import xyz.scada.testbed.node.hmi.plc.Plc;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This PLC is a ModBusTCP server that will be used to simulate the sensors in the testbed.
 * ---------------------------------------------------------------------------------------------------------------------
 * |     Type            |  Address   | Name                     | Description                                         |
 * |----------------     |---------   |--------------------------|-----------------------------------------------------|
 * | Input Register      |  30003     |  Engine Temperature      | The engine temp in Celsius                          |
 * | Input Register      |  30004     |  Engine RPM              | The number of rate per minute in the engine         |
 * | Holding Register    |  40002     |  Requested power         | The percentage of power requested to the engine     |
 * ---------------------------------------------------------------------------------------------------------------------
 */
public abstract class EngineModbusService extends ReadWriteModbusServices {
    @Getter
    private enum DataAddresses {
        IR_TEMPERATURE(30003),
        IR_RPM(30004),
        HR_REQUESTED_POWER(40002);

        private final int address;

        DataAddresses(int address) {
            this.address = address;
        }

    }

    private static final Logger LOGGER;

    Random rand;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(EngineModbusService.class.getName());
    }

    public EngineModbusService() throws UnknownUnitIdException {
        super();

        rand = new Random();

        ProcessImage processImage = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));

        // test
        int temperature = 130;
        int rpm = 1100;

        setData(processImage, temperature, rpm);

        processImage.addModificationListener(new ProcessImage.ModificationListener() {
            @Override
            public void onCoilsModified(List<ProcessImage.Modification.CoilModification> list) {
                list.forEach(modification -> {
                    int address = modification.address();
                    boolean value = modification.value();
                    LOGGER.log(Level.SEVERE, "Coil at address {0} set to {1} but no coils are managed by this plc", new Object[]{address, value});
                });

            }

            @Override
            public void onDiscreteInputsModified(List<ProcessImage.Modification.DiscreteInputModification> list) {
                LOGGER.severe("Discrete inputs modified but no discrete inputs are manged by this plc.");
            }

            @Override
            public void onHoldingRegistersModified(List<ProcessImage.Modification.HoldingRegisterModification> list) {
                try {
                    setData(processImage, 20, 60);
                } catch (UnknownUnitIdException e) {
                    throw new RuntimeException(e);
                }
                list.forEach(modification -> {
                    int address = modification.address();
                    byte[] value = modification.value();
                    int requested_power = Plc.getInt16FromByteArray(value);
                    LOGGER.log(Level.INFO, "Holding register at address {0} set to {1}({2})", new Object[]{address, value, requested_power});

                    int temperature = rand.nextInt(40) + 60 + (requested_power / 2);
                    int rpm = requested_power * 50;

                    if (address == DataAddresses.HR_REQUESTED_POWER.getAddress()) {
                        try {
                            setData(processImage, temperature, rpm);
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

    private void setData(ProcessImage image, int temperature, int rpm) throws UnknownUnitIdException {
        byte[] temperatureValues = {(byte) (temperature >> 8), (byte) (temperature & 0xFF)};
        byte[] rpmValues = {(byte) (rpm >> 8), (byte) (rpm & 0xFF)};

        LOGGER.info("setting temperature: " + temperature + " and rpm " + rpm);
        // ProcessImage image = getProcessImage(0).orElseThrow(() -> new UnknownUnitIdException(0));
        image.with(transaction -> transaction.writeInputRegisters(map -> {
            map.put(DataAddresses.IR_TEMPERATURE.getAddress(), temperatureValues);
            map.put(DataAddresses.IR_RPM.getAddress(), rpmValues);
        }));
    }

}
