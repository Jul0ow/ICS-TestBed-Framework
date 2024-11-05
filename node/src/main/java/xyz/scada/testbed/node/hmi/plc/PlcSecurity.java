package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;

public class PlcSecurity extends Plc {

    enum Address implements AddressPlc{
        C_FENCE(4),
        C_SEATBELT(5);

        private final int value;

        Address(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

    }

    public PlcSecurity(String ipAddr, int port, String name, String description) {
        super(ipAddr, port, name, description);
    }

    public int getFenceStatus() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadCoilsResponse response = readCoils(Address.C_FENCE.getValue(), 1);
        int res = response.coils()[0];
        System.out.println(response);

        return res;
    }

    public void setFence(boolean isClose) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_FENCE.getValue(), isClose ? 1: 0);
    }

    public int getSeatbeltStatus() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadCoilsResponse response = readCoils(Address.C_SEATBELT.getValue(), 1);
        int res = response.coils()[0];
        System.out.println(response);

        return res;
    }

    public void setSeatbelt(boolean isLocked) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_SEATBELT.getValue(), isLocked ? 1: 0);
    }
}
