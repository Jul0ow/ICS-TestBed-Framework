package xyz.scada.testbed.node.hmi.exceptions;

public class PlcNotPresent extends Exception{
    public PlcNotPresent(String plcName)
    {
        super("plc '" + plcName + "' is not present");
    }
}
