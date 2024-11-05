package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;

public class PlcLight extends Plc {

    enum Address implements AddressPlc{
        C_LIGHT(6);

        private final int value;

        Address(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

    }

    public PlcLight(String ipAddr, int port, String name, String description) {
        super(ipAddr, port, name, description);
    }

    public int getLightStatus() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadCoilsResponse response = readCoils(Address.C_LIGHT.getValue(), 1);
        int res = response.coils()[0];
        System.out.println(response);

        return res;
    }

    public void setLight(boolean isOn) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_LIGHT.getValue(), isOn ? 1 : 0);
    }
}
