package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterResponse;

public class PlcBrake extends Plc {

    enum Address implements AddressPlc{
        IR_BREAK_PRESSURE(30001),
        IR_ACTIVATION_PERCENT(30002),
        HR_BREAK_CLASSIC(40001),
        C_EMERGENCY_BREAK(2),
        C_BREAK_PARK(3);

        private final int value;

        Address(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

    }

    public PlcBrake(String ipAddr, int port, String name, String description) {
        super(ipAddr, port, name, description);
    }

    public int getBrakePressure() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadInputRegistersResponse response = readInputRegister(Address.IR_BREAK_PRESSURE.getValue(), 1);
        int res = response.registers()[0];
        System.out.println(response);

        return res;
    }

    public int getActivationPercent() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadInputRegistersResponse response = readInputRegister(Address.IR_ACTIVATION_PERCENT.getValue(), 1);
        int res = response.registers()[0];
        System.out.println(response);

        return res;
    }

    public void setBrake(int brakeValue) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleRegister(Address.HR_BREAK_CLASSIC.getValue(), brakeValue);
    }

    public void setEmergencyBrake(boolean value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_EMERGENCY_BREAK.getValue(), value ? 1 : 0);
    }

    public void setParkBrake(boolean value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_BREAK_PARK.getValue(), value ? 1 : 0);
    }
}
