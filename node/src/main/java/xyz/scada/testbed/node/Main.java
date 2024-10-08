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
    private static final int DEFAULT_INTERVAL = 5000;

    private static int DEFAULT_IEC104_PORT = 2404;
    private static int DEFAULT_IEC104_COMMON_ADDR = 1;

    private static final int DEFAULT_MODBUS_PORT = 502;

    private static int DEFAULT_OPCUA_PORT = 0;

    //    Current Running States
    RTU rtu = null;
    OPC_Server opc = null;

    ModBusTCP modbusTCP = null;

    HMI hmi = null;
    Historian hist = null;

    int interval = DEFAULT_INTERVAL;
    String listen = DEFAULT_LISTEN;
    int portModbus = DEFAULT_MODBUS_PORT;
    int portIEC104 = DEFAULT_IEC104_PORT;
    String mode = "N/A";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(HMI.class.getName());
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    public static int getDefaultOpcuaPort() {
        return DEFAULT_OPCUA_PORT;
    }

    public static void setDefaultOpcuaPort(int defaultOpcuaPort) {
        DEFAULT_OPCUA_PORT = defaultOpcuaPort;
    }

    @ShellMethod(value = "Start TCP Modbus Server.", group = "TCP")
    public void tcp() {
        if (hmi != null) {
            System.out.println("Error: Currently configured as a HMI.");
        } else if (hist != null) {
            System.out.println("Error: Currently configured as a Historian.");
        } else if (rtu != null) {
            System.out.println("Error: Currently configured as an RTU.");
        } else if (modbusTCP == null) {
            modbusTCP = new ModBusTCP();
        }
    }

    @ShellMethod(value = "Start RTU.", group = "RTU")
    public void rtu() {
        if (hmi != null) {
            System.out.println("Error: Currently configured as a HMI.");
        } else if (hist != null) {
            System.out.println("Error: Currently configured as a Historian.");
        } else if (modbusTCP != null) {
            System.out.println("Error: Currently configured as a ModbusTCP.");
        } else if (rtu == null) {
            rtu = new RTU();
            mode = "RTU";
        }
    }

    @ShellMethod(value = "Start HMI.", group = "HMI")
    public void hmi() {
        if (rtu != null) {
            System.out.println("Error: Currently configured as a RTU.");
        } else if (modbusTCP != null) {
            System.out.println("Error: Currently configured as a ModbusTCP.");
        } else if (hmi == null) {
            hmi = new HMI();
            if (mode.equals("HIST")) {
                mode += "HMI";
            } else {
                mode = "HMI";
            }
        }
    }


    // TODO Add command to connect to Modbus
    @ShellMethod(value = "Set IEC104 Common Address to query.", group = "HMI")
    public void hmiCommonAddress(int addr) {
        if (hmi != null) hmi.setCommonAddress(addr);
    }

    @ShellMethod(value = "Set interval query (MS).", group = "HMI")
    public void hmiInterval(int interval) {
        if (hmi != null) hmi.setTime_sleep(interval);
    }

    @ShellMethod(value = "Set port.", group = "HMI")
    public void hmiPort(int port) {
        if (hmi != null) hmi.setPort(port);
    }

    @ShellMethod(value = "Show current HMI configuration.", group = "HMI")
    public void hmiShow() {
        if (hmi != null) hmi.show();
    }

    // TODO check for historian
    @ShellMethod(value = "Set hosts to connect to.", group = "HMI")
    public void remoteHosts(String[] hosts) {
        if (hmi != null) hmi.setHosts(hosts);
//        if(hist != null) hist.setHosts(hosts);
    }

    // TODO modify historian to connect to Modbus
    @ShellMethod(value = "Start data historian.", group = "Historian")
    public void hist() {
        if (rtu != null) {
            System.out.println("Error: Currently configured as a RTU.");
        } else if (modbusTCP != null) {
            System.out.println("Error: Currently configured as a ModbusTCP.");
        } else if (hist == null) {
            hist = new Historian();
            if (mode.equals("HMI")) {
                mode += "HIST";
            } else {
                mode = "HIST";
            }
        }
    }

    @ShellMethod(value = "Enable OPC-UA", group = "RTU", prefix = "")
    public void enableOPCUA() {
        if (rtu != null && opc == null) {
            opc = new OPC_Server();
        }
    }

    @ShellMethod(value = "Set listen interface.", group = "RTU", prefix = "")
    public void rtuListen(
            @ShellOption(defaultValue = "127.0.0.1") String listen
    ) {
        try {
            if (!listen.equals(DEFAULT_LISTEN))
                this.listen = listen;

        } catch (NumberFormatException ex) {
            System.out.println("Error: Expecting an integer.");
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @ShellMethod(value = "Set IEC104 Port.", group = "RTU", prefix = "")
    public void rtuIEC104Port(
            @ShellOption(defaultValue = "2404") String port
    ) {
        try {
            if (Integer.parseInt(port) != DEFAULT_IEC104_PORT)
                this.portIEC104 = Integer.parseInt(port);

        } catch (NumberFormatException ex) {
            System.out.println("Error: Expecting an integer.");
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @ShellMethod(value = "Set Modbus Port.", group = "TCP", prefix = "")
    public void modbusPort(
            @ShellOption(defaultValue = "502") String port
    ) {
        try {
            if (Integer.parseInt(port) != DEFAULT_MODBUS_PORT)
                this.portModbus = Integer.parseInt(port);

        } catch (NumberFormatException ex) {
            System.out.println("Error: Expecting an integer.");
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    @ShellMethod(value = "Run Configuration.", prefix = "")
    public void run() {
        if (mode.equals("N/A"))
            System.out.println("Error mode not set. Configure node as either: RTU, HMI or Historian.");
        if (rtu != null) rtu.start();
        if (modbusTCP != null) modbusTCP.start(listen, portModbus);
        if (hmi != null) hmi.start();
        if (hist != null) {
            try {
                hist.main("opc.tcp://127.0.0.1:8666");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ShellMethod(value = "Show Current configuration.")
    public void show() {
        if (rtu != null) {
            System.out.println("IEC104 Enabled: True");
            if (hist != null) System.out.println("OPC-UA Enabled: True");
        }
        if (modbusTCP != null) System.out.println("Modbus Enabled: True");
        if (hmi != null) hmi.show();
        if (hist != null) hist.show();
    }
}