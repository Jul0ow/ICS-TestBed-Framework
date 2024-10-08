package xyz.scada.testbed.node;

import com.digitalpetri.modbus.server.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModBusTCP {

    public ModBusTCP() {
    }

    private static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(ModBusTCP.class.getName());
    }

    public static void main(String[] args) {
        new ModBusTCP().start("127.0.0.1", 502);
    }

    public void start(String bindAddress, int port) {
        ModbusTcpServerTransport serverTransport = NettyTcpServerTransport.create(cfg -> {
            cfg.bindAddress = bindAddress;
            cfg.port = port;
        });

        ProcessImage processImage = new ProcessImage();
        ModbusServices services = new ReadWriteModbusServices() {
            @Override
            protected Optional<ProcessImage> getProcessImage(int i) {
                return Optional.of(processImage);
            }
        };
        ModbusTcpServer server = ModbusTcpServer.create(serverTransport, services);

        try {
            server.start();
            LOGGER.log(Level.INFO, " RTU Server started!");
            System.out.println("RTU Started.");
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Unable to start listening: {0}", e.getMessage());
        }
    }

}
