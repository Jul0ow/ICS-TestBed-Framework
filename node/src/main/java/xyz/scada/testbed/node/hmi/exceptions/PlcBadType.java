package xyz.scada.testbed.node.hmi.exceptions;

public class PlcBadType extends Exception{
    public PlcBadType(String name)
    {
        super( "Plc "+ name + " does has no the correct type for this operation");
    }
}
