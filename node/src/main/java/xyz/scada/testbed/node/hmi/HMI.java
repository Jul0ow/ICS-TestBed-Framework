package xyz.scada.testbed.node.hmi;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;
import xyz.scada.testbed.node.ModBusTCP;
import xyz.scada.testbed.node.hmi.exceptions.PlcAlreadyPresent;
import xyz.scada.testbed.node.hmi.exceptions.PlcNotPresent;
import xyz.scada.testbed.node.hmi.plc.Plc;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HMI {
    private static Logger LOGGER = null;

    private Map<String, Plc> plcs = new Hashtable<>();

    public HMI() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Starting HMI");
    }


    public void addPlc(String name, String ipAddr, int port, String description) throws PlcAlreadyPresent {
        var plc = new Plc(ipAddr, port, name, description);

        // Test if not already present
        if (plcs.get(name) != null)
        {
            throw new PlcAlreadyPresent(name);
        }

        plcs.put(name,plc);
        LOGGER.info("Add plc: " + plc);
    }

    public void readHoldingRegisters(String plcName, int address, int quantity) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        var response = getPlc(plcName).readHoldingRegister(address,quantity);
        System.out.println(response);
    }

    public void writeSingleRegisterResponse(String plcName, int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        getPlc(plcName).writeSingleRegisterResponse(address, value);
    }
    
    @Override
    public String toString() {
        String res = "HMI:\n";
        for (var plc : plcs.values())
        {
            res += "\t" + plc + "\n";
        }
        return res;
    }

    private Plc getPlc(String plcName) throws PlcNotPresent {
        var plc = plcs.get(plcName);
        if (plc == null)
            throw new PlcNotPresent(plcName);

        return plc;
    }
}
