package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;

public class PlcEngine extends Plc {

    enum Address implements AddressPlc{
        IR_ENGINE_TEMP(0), //TODO
        IR_RPM(0),
        HR_REQUESTED_POWER(0); // TODO

        private final int value;

        Address(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

    }

    public PlcEngine(String ipAddr, int port, String name, String description) {
        super(ipAddr, port, name, description);
    }

    public int getEngineTemp() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadInputRegistersResponse response = readInputRegister(Address.IR_ENGINE_TEMP.getValue(), 1);
        int res = getInt16FromByteArray(response.registers());
        System.out.println(response);

        return res;
    }

    public int getEngineRMP() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadInputRegistersResponse response = readInputRegister(Address.IR_RPM.getValue(), 1);
        int res = getInt16FromByteArray(response.registers());
        System.out.println(response);

        return res;
    }

    public void setRequestedEnginPower(int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleRegister(Address.HR_REQUESTED_POWER.getValue(), value);
    }
}
