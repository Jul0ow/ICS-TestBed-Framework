package xyz.scada.testbed.node.hmi.exceptions;

public class PlcAlreadyPresent extends Exception{
    public PlcAlreadyPresent(String name)
    {
        super("Plc " + name + " already present");
    }
}
