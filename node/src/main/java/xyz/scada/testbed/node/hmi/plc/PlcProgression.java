package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlcProgression extends Plc {

    enum Address implements AddressPlc{
        C_CHECKPOINTS(10001),
        C_STARTING(1);

        private final int value;

        Address(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }

    }

    List<Boolean> checkpoints;

    public PlcProgression(String ipAddr, int port, String name, String description) {
        super(ipAddr, port, name, description);
        checkpoints = Arrays.asList(false, false, false, false, false);
    }

    public List<Boolean> getCheckpoints() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        ReadDiscreteInputsResponse response = readDiscreteInput(Address.C_CHECKPOINTS.getValue(), 5);
        var coils = response.inputs();
        System.out.println(response);
        checkpoints.set(0, getCheckpoint(coils[0], 0));
        checkpoints.set(1, getCheckpoint(coils[0], 1));
        checkpoints.set(2, getCheckpoint(coils[0], 2));
        checkpoints.set(3, getCheckpoint(coils[0], 3));
        checkpoints.set(4, getCheckpoint(coils[0], 4));

        return checkpoints;
    }

    public void setStart() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        writeSingleCoil(Address.C_STARTING.getValue(), 1);
    }

    public ReadCoilsResponse getStart() throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        return readCoils(Address.C_STARTING.getValue(), 1);
    }

    private Boolean getCheckpoint(byte coils, int checkpointNb)
    {
        return (coils & (1 << checkpointNb)) != 0;
    }
}
