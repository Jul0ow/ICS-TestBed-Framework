package xyz.scada.testbed.node.hmi;

import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import xyz.scada.testbed.node.hmi.exceptions.PlcAlreadyPresent;
import xyz.scada.testbed.node.hmi.exceptions.PlcBadType;
import xyz.scada.testbed.node.hmi.exceptions.PlcNotPresent;
import xyz.scada.testbed.node.hmi.plc.Plc;
import xyz.scada.testbed.node.hmi.plc.PlcBrake;
import xyz.scada.testbed.node.hmi.plc.PlcProgression;

import java.util.*;
import java.util.logging.Logger;

public class HMI {
    private static Logger LOGGER = null;

    private final Map<String, Plc> plcs = new Hashtable<>();

    public HMI() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Starting HMI");
    }


    public void addPlc(String name, String ipAddr, int port, String type,String description) throws Exception {
        Plc plc;
        System.out.println(type);
        plc = switch (type) {
            case "plc" -> new Plc(ipAddr, port, name, description);
            case "progression" -> new PlcProgression(ipAddr, port, name, description);
            case "brake" -> new PlcBrake(ipAddr, port, name, description);
            default -> throw new Exception("No type found for " + type);
        };

        // Test if not already present
        if (plcs.get(name) != null)
        {
            throw new PlcAlreadyPresent(name);
        }

        plcs.put(name,plc);
        LOGGER.info("Add plc: " + plc);
    }

    public void removePlc(String plcName) throws PlcNotPresent {
        var plcRemoved = plcs.remove(plcName);
        if (plcRemoved == null)
            throw new PlcNotPresent(plcName);
        LOGGER.info("Plc: " + plcRemoved + " removed");
        System.out.println("Successfully removed plc named: " + plcName);
    }


    /* Progression */
    public List<Boolean> getCheckpoints(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcProgression))
            throw new PlcBadType(plcName);
        return ((PlcProgression) plc).getCheckpoints();
    }

    public void setStart(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcProgression))
            throw new PlcBadType(plcName);
        ((PlcProgression) plc).setStart();
    }


    /* Brake */

    public int getBrakePressure(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake))
            throw new PlcBadType(plcName);
        return ((PlcBrake) plc).getBrakePressure();
    }

    public int getActivationPercent(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake))
            throw new PlcBadType(plcName);
        return ((PlcBrake) plc).getActivationPercent();
    }

    public void setBrake(String plcName, int brakeValue) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake bPlc))
            throw new PlcBadType(plcName);
        bPlc.setBrake(brakeValue);
    }

    public void setEmergencyBrake(String plcName, boolean breaking) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake bPlc))
            throw new PlcBadType(plcName);
        bPlc.setEmergencyBrake(breaking);
    }

    public void setParkBrake(String plcName, boolean breaking) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake bPlc))
            throw new PlcBadType(plcName);
        bPlc.setParkBrake(breaking);
    }

    /* IO Read operations */

    /**
     *
     * @param plcName the name of the plc where the data will be read
     * @param address the address of the register to read
     * @param quantity the quantity of bytes to read
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent if plcName match any plc name's present in the hmi
     */
    public void readHoldingRegisters(String plcName, int address, int quantity) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        var response = getPlc(plcName).readHoldingRegister(address,quantity);
        System.out.println(response);
    }

    /**
     *
     * @param plcName the name of the plc where the data will be read
     * @param address the address of the first coil to read
     * @param quantity the quantity of bytes to read
     * @throws PlcNotPresent if plcName match any plc name's present in the hmi
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     */
    public void readCoils(String plcName, int address, int quantity) throws PlcNotPresent, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var response = getPlc(plcName).readCoils(address, quantity);
        System.out.println(response);
    }

    /* Write operations */

    /**
     *
     * @param plcName the name of the plc where the data will be written
     * @param address the address of the register to write
     * @param value the value to be put in the register
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent if plcName match any plc name's present in the hmi
     */
    public void writeSingleRegister(String plcName, int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        getPlc(plcName).writeSingleRegister(address, value);
    }

    /**
     *
     * @param plcName the name of the plc where the data will be written
     * @param address the address of the coil to write
     * @param value the value to be put in the coil
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent if plcName match any plc name's present in the hmi
     */
    public void writeSingleCoil(String plcName, int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        getPlc(plcName).writeSingleCoil(address, value);
    }


    
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("HMI:\n");
        for (var plc : plcs.values())
        {
            res.append("\t").append(plc).append("\n");
        }
        return res.toString();
    }

    private Plc getPlc(String plcName) throws PlcNotPresent {
        var plc = plcs.get(plcName);
        if (plc == null)
            throw new PlcNotPresent(plcName);

        return plc;
    }
}
