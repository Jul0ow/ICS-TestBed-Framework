package xyz.scada.testbed.node;

//import org.apache.commons.cli.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.logging.Level;
import java.util.logging.Logger;


@SpringBootApplication
@ShellComponent
public class Main {

    //    Global Default States
    private static Logger LOGGER = null;

    private static final String DEFAULT_LISTEN = "127.0.0.1";

    private static final int DEFAULT_MODBUS_PORT = 502;


    //    Current Running States
    ModBusTCP modbusTCP = null;

    String listen = DEFAULT_LISTEN;
    int portModbus = DEFAULT_MODBUS_PORT;
    String mode = "N/A";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(ModBusTCP.class.getName());
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @ShellMethod(value = "Start TCP Modbus Server.", group = "TCP")
    public void tcp() {
        // if (hmi != null) {
        //     System.out.println("Error: Currently configured as a HMI.");
        // } else if (hist != null) {
        //     System.out.println("Error: Currently configured as a Historian.");
        // } else if (rtu != null) {
        //     System.out.println("Error: Currently configured as an RTU.");
        // } else if (modbusTCP == null) {
        //     modbusTCP = new ModBusTCP();
        // }
        if (modbusTCP == null) {
            modbusTCP = new ModBusTCP();
            mode = "TCP";
        }
    }

    // @ShellMethod(value = "Start HMI.", group = "HMI")
    // public void hmi() {


    // TODO hmi to Modbus

    // TODO check for historian

    // TODO modify historian to connect to Modbus

    @ShellMethod(value = "Set listen interface.", group = "TCP", prefix = "")
    public void tcpListen(@ShellOption(defaultValue = "127.0.0.1") String listen) {
        try {
            if (!listen.equals(DEFAULT_LISTEN)) this.listen = listen;

        } catch (NumberFormatException ex) {
            System.out.println("Error: Expecting an integer.");
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @ShellMethod(value = "Set Modbus Port.", group = "TCP", prefix = "")
    public void modbusPort(@ShellOption(defaultValue = "502") String port) {
        try {
            if (Integer.parseInt(port) != DEFAULT_MODBUS_PORT) this.portModbus = Integer.parseInt(port);

        } catch (NumberFormatException ex) {
            System.out.println("Error: Expecting an integer.");
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @ShellMethod(value = "Run Configuration.", prefix = "")
    public void run() {
        if (mode.equals("N/A"))
            System.out.println("Error mode not set. Configure node as either: RTU, HMI or Historian.");
        // if (rtu != null) rtu.start();
        if (modbusTCP != null) modbusTCP.start(listen, portModbus);
        // if (hmi != null) hmi.start();
        // if (hist != null) {
        //     try {
        //         hist.main("opc.tcp://127.0.0.1:8666");
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //     }
        // }
    }

    @ShellMethod(value = "Show Current configuration.")
    public void show() {
        if (modbusTCP != null) System.out.println("Modbus Enabled: True");
    }
}