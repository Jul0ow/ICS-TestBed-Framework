package xyz.scada.testbed.node;

//import org.apache.commons.cli.*;

import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.server.ProcessImage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import xyz.scada.testbed.node.hmi.HMI;
import xyz.scada.testbed.node.plc.BrakesModbusService;
import xyz.scada.testbed.node.plc.ModBusTCP;
import xyz.scada.testbed.node.plc.ProgressionModbusService;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


@SpringBootApplication
@ShellComponent
public class Main {

    //    Global Default States
    private static final Logger LOGGER;

    private static final String DEFAULT_LISTEN = "127.0.0.1";

    private static final int DEFAULT_MODBUS_PORT = 502;


    //    Current Running States
    ModBusTCP modbusTCP = null;
    HMI hmi = null;


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


    @ShellMethod(value = "Select PLC type.", group = "TCP")
    public void plcType(@ShellOption(help = "Possible values are 'Progression'") String type) {
        if (modbusTCP == null) {
            if (type.equals("Progression")) {
                ProcessImage image = new ProcessImage();
                try {
                    modbusTCP = new ModBusTCP(new ProgressionModbusService() {
                        @Override
                        protected Optional<ProcessImage> getProcessImage(int i) {
                            return Optional.of(image);
                        }
                    });
                } catch (UnknownUnitIdException e) {
                    System.out.println("Could not create ProgressionModbusService.");
                }
            } else if (type.equals("Brakes")) {
                ProcessImage image = new ProcessImage();
                try {
                    modbusTCP = new ModBusTCP(new BrakesModbusService() {
                        @Override
                        protected Optional<ProcessImage> getProcessImage(int i) {
                            return Optional.of(image);
                        }
                    });
                } catch (UnknownUnitIdException e) {
                    System.out.println("Could not create BrakesModbusService.");
                }
            } else
                System.out.println("Error: Invalid PLC type.");
            mode = "TCP";
        }
    }

    // @ShellMethod(value = "Start HMI.", group = "HMI")
    // public void hmi() {


    // TODO hmi to Modbus

    @ShellMethod(value = "Add a new plc to the hmi", group = "HMI", prefix = "")
    public void addPlc(@ShellOption() String name, @ShellOption() String ipAddr,
                       @ShellOption(defaultValue = "502") String port,
                       @ShellOption(defaultValue = "plc", help = "supported type are plc (default one), progression") String type,
                       @ShellOption(defaultValue = "") String description) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.addPlc(name, ipAddr, Integer.parseInt(port), type, description);
        } catch (Exception e) {
            System.err.println("Failed to add the plc: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Remove an existing plc to the hmi", group = "HMI", prefix = "")
    public void removePlc(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.removePlc(name);
        } catch (Exception e) {
            System.err.println("Failed to add the plc: " + e.getMessage());
        }
    }


    @ShellMethod(value = "Print all the plc added to the hmi", group = "HMI", prefix = "")
    public void printPlc() {
        if (hmi == null)
            hmi = new HMI();

        System.out.println(hmi);
    }

    /* Write operations */

    @ShellMethod(value = "Write a single register to the given plc at the given address and value", group = "HMI", prefix = "")
    public void writeSingleRegister(@ShellOption() String name, @ShellOption(help = "The register address") String address, @ShellOption(help = "The value") String value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.writeSingleRegister(name, Integer.parseInt(address), Integer.parseInt(value));
        } catch (Exception e) {
            System.err.println("Failed to write: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Write a single coil to the given plc at the given address with the given value", group = "HMI", prefix = "")
    public void writeSingleCoil(@ShellOption() String name, @ShellOption(help = "The register address") String address, @ShellOption(help = "The value, 0 or 1 (if greater than 1 it will be put to 1)") String value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.writeSingleCoil(name, Integer.parseInt(address), Integer.parseInt(value));
        } catch (Exception e) {
            System.err.println("Failed to write coil: " + e.getMessage());
        }
    }

    /* Read operations */

    @ShellMethod(value = "Read a register to the given plc at the given address with the given quantity", group = "HMI", prefix = "")
    public void readHoldingRegister(@ShellOption() String name, @ShellOption(help = "The register address") String address, @ShellOption(help = "The quantity to read") String quantity) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.readHoldingRegisters(name, Integer.parseInt(address), Integer.parseInt(quantity));
        } catch (Exception e) {
            System.err.println("Failed to read register: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Read the given quantity of coils in the given plc at the given address", group = "HMI", prefix = "")
    public void readCoils(@ShellOption() String name, @ShellOption(help = "The first coil address") String address, @ShellOption(help = "The quantity to read") String quantity) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.readCoils(name, Integer.parseInt(address), Integer.parseInt(quantity));
        } catch (Exception e) {
            System.err.println("Failed to read coils: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Gets the checkpoint from the given progression Plc", group = "HMI-Progression", prefix = "")
    public void getCheckpoints(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getCheckpoints(name));
        } catch (Exception e) {
            System.err.println("Failed to get checkpoints: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the start of the progression Plc", group = "HMI-Progression", prefix = "")
    public void setStart(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setStart(name);
        } catch (Exception e) {
            System.err.println("Failed to set start: " + e.getMessage());
        }
    }

    /* Breaking plc operations */

    @ShellMethod(value = "Gets the brake pressure from the given brake Plc", group = "HMI-Brake", prefix = "")
    public void getBrakePressure(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getBrakePressure(name));
        } catch (Exception e) {
            System.err.println("Failed to get brake pressure: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Gets the brake activation percent from the given brake Plc", group = "HMI-Brake", prefix = "")
    public void getBrakeActivationPercent(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getActivationPercent(name) + "%");
        } catch (Exception e) {
            System.err.println("Failed to get brake activation percent: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the brake from the given brake Plc with the given value", group = "HMI-Brake", prefix = "")
    public void setBrake(@ShellOption() String name, @ShellOption(help = "The value for the brake to be set") Integer value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setBrake(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set brake: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the emergency brake from the given brake Plc, set it to true to activate emergency braking and to false to deactivate it", group = "HMI-Brake", prefix = "")
    public void setEmergencyBrake(@ShellOption() String name, @ShellOption(help = "true if the emergency breaking should be activated, false to deactivate it", defaultValue = "true") Boolean value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setEmergencyBrake(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set emergency brake: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the parking brake from the given brake Plc, set it to true to activate parking brake and to false to deactivate it", group = "HMI-Brake", prefix = "")
    public void setParkingBrake(@ShellOption() String name, @ShellOption(help = "true if the emergency breaking should be activated, false to deactivate it", defaultValue = "true") Boolean value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setParkBrake(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set parking brake: " + e.getMessage());
        }
    }

    /* Security Plc operations */
    @ShellMethod(value = "Get the fence status of the attraction from the given security Plc", group = "HMI-Security", prefix = "")
    public void getFenceStatus(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getFenceStatus(name));
        } catch (Exception e) {
            System.err.println("Failed to get fence status: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the fences from the given security Plc, set it to true to close the fences and to false to open them", group = "HMI-Security", prefix = "")
    public void setFence(@ShellOption() String name, @ShellOption(help = "true if the fence should be closed, false to open it", defaultValue = "true") Boolean value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setFence(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set fence: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Get the seatbelt status of the attraction from the given security Plc", group = "HMI-Security", prefix = "")
    public void getSeatbeltStatus(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getSeatbeltStatus(name));
        } catch (Exception e) {
            System.err.println("Failed to get seatbelt status: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the seatbelts from the given security Plc, set it to true to lock the seatbelts and to false to open them", group = "HMI-Security", prefix = "")
    public void setSeatbelt(@ShellOption() String name, @ShellOption(help = "true if the seatbelts should be locked, false to open them", defaultValue = "true") Boolean value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setSeatbelt(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set seatbelt: " + e.getMessage());
        }
    }

    /* Engine Plc operations */

    @ShellMethod(value = "Get the engine temperature of the engine from the given engine Plc", group = "HMI-Engine", prefix = "")
    public void getEngineTemperature(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getEngineTemp(name));
        } catch (Exception e) {
            System.err.println("Failed to get engine temperature: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Get the engine RPM of the engine from the given engine Plc", group = "HMI-Engine", prefix = "")
    public void getEngineRPM(@ShellOption() String name) {
        if (hmi == null)
            hmi = new HMI();

        try {
            System.out.println(hmi.getEngineRPM(name));
        } catch (Exception e) {
            System.err.println("Failed to get engine RPM: " + e.getMessage());
        }
    }

    @ShellMethod(value = "Set the power of the engine from the given engine Plc", group = "HMI-Engine", prefix = "")
    public void setEnginePower(@ShellOption() String name, @ShellOption(help = "") int value) {
        if (hmi == null)
            hmi = new HMI();

        try {
            hmi.setEnginePower(name, value);
        } catch (Exception e) {
            System.err.println("Failed to set engine RPM: " + e.getMessage());
        }
    }


    // TODO check for historian

    // TODO modify historian to connect to Modbus

    @ShellMethod(value = "Set listen interface.", group = "TCP", prefix = "")
    public void tcpListen(@ShellOption(defaultValue = "127.0.0.1") String listen) {
        if (!listen.equals(DEFAULT_LISTEN))
            this.listen = listen;

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