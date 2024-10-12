package xyz.scada.testbed.node.plc;

import com.digitalpetri.modbus.server.*;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModBusTCP {
    private final ModbusServices service;

    public ModBusTCP(ModbusServices service) {
        this.service = service;
    }

    protected static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(ModBusTCP.class.getName());
    }

    public void start(String bindAddress, int port) {
        ModbusTcpServerTransport serverTransport = NettyTcpServerTransport.create(cfg -> {
            cfg.bindAddress = bindAddress;
            cfg.port = port;
        });

        ModbusTcpServer server = ModbusTcpServer.create(serverTransport, service);

        try {
            server.start();
            LOGGER.log(Level.INFO, " ProgressionPLC Server started!");
            System.out.println("ProgressionPLC Started.");
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Unable to start listening: {0}", e.getMessage());
        }
    }

}
