package xyz.scada.testbed.node.hmi.exceptions;

public class PlcBadArgument extends Exception{
    public PlcBadArgument(String what)
    {
        super("PlcBadArgument exception: " + what);
    }
}
