package xyz.scada.testbed.node.hmi;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import xyz.scada.testbed.node.hmi.exceptions.PlcAlreadyPresent;
import xyz.scada.testbed.node.hmi.exceptions.PlcBadArgument;
import xyz.scada.testbed.node.hmi.exceptions.PlcBadType;
import xyz.scada.testbed.node.hmi.exceptions.PlcNotPresent;
import xyz.scada.testbed.node.hmi.plc.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

public class HMI {
    private static Logger LOGGER = null;

    private final Map<String, Plc> plcs = new Hashtable<>();

    public HMI() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Starting HMI");
    }


    public void addPlc(String name, String ipAddr, int port, String type, String description) throws Exception {
        Plc plc;
        System.out.println(type);
        plc = switch (type) {
            case "plc" -> new Plc(ipAddr, port, name, description);
            case "progression" -> new PlcProgression(ipAddr, port, name, description);
            case "brake" -> new PlcBrake(ipAddr, port, name, description);
            case "security" -> new PlcSecurity(ipAddr, port, name, description);
            case "engine" -> new PlcEngine(ipAddr, port, name, description);
            case "lights" -> new PlcLight(ipAddr, port, name, description);
            default -> throw new Exception("No type found for " + type);
        };

        // Test if not already present
        if (plcs.get(name) != null) {
            throw new PlcAlreadyPresent(name);
        }

        plcs.put(name, plc);
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


    /* Plc Brake */

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

    public void setBrake(String plcName, int brakeValue) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcBadArgument {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcBrake bPlc))
            throw new PlcBadType(plcName);
        if (brakeValue < 0 || brakeValue > 100)
            throw new PlcBadArgument("Expected a percentage ([0,100]) but got " + brakeValue);
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

    /* Plc Security */
    public int getFenceStatus(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcSecurity))
            throw new PlcBadType(plcName);
        return ((PlcSecurity) plc).getFenceStatus();
    }

    public void setFence(String plcName, boolean isClose) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcSecurity sPlc))
            throw new PlcBadType(plcName);
        sPlc.setFence(isClose);
    }

    public int getSeatbeltStatus(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcSecurity))
            throw new PlcBadType(plcName);
        return ((PlcSecurity) plc).getSeatbeltStatus();
    }

    public void setSeatbelt(String plcName, boolean isLocked) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcSecurity sPlc))
            throw new PlcBadType(plcName);
        sPlc.setSeatbelt(isLocked);
    }


    /* Engine */

    public int getEngineTemp(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcEngine ePlc))
            throw new PlcBadType(plcName);
        return ePlc.getEngineTemp();
    }

    public int getEngineRPM(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcEngine ePlc))
            throw new PlcBadType(plcName);
        return ePlc.getEngineRMP();
    }

    public void setEnginePower(String plcName, int value) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcEngine ePlc))
            throw new PlcBadType(plcName);
        ePlc.setRequestedEnginPower(value);
    }


    /* Light Plc */

    public int getLightStatus(String plcName) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcLight lPlc))
            throw new PlcBadType(plcName);
        return lPlc.getLightStatus();
    }

    public void setLight(String plcName, boolean isOn) throws PlcNotPresent, PlcBadType, ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var plc = getPlc(plcName);
        if (!(plc instanceof PlcLight lPlc))
            throw new PlcBadType(plcName);
        lPlc.setLight(isOn);
    }

    /* IO Read operations */

    /**
     * @param plcName  the name of the plc where the data will be read
     * @param address  the address of the register to read
     * @param quantity the quantity of bytes to read
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent            if plcName match any plc name's present in the hmi
     */
    public void readHoldingRegisters(String plcName, int address, int quantity) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        var response = getPlc(plcName).readHoldingRegister(address, quantity);
        System.out.println(response);
    }

    /**
     * @param plcName  the name of the plc where the data will be read
     * @param address  the address of the first coil to read
     * @param quantity the quantity of bytes to read
     * @throws PlcNotPresent            if plcName match any plc name's present in the hmi
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
     * @param plcName the name of the plc where the data will be written
     * @param address the address of the register to write
     * @param value   the value to be put in the register
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent            if plcName match any plc name's present in the hmi
     */
    public void writeSingleRegister(String plcName, int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        getPlc(plcName).writeSingleRegister(address, value);
    }

    /**
     * @param plcName the name of the plc where the data will be written
     * @param address the address of the coil to write
     * @param value   the value to be put in the coil
     * @throws ModbusExecutionException
     * @throws ModbusTimeoutException
     * @throws ModbusResponseException
     * @throws PlcNotPresent            if plcName match any plc name's present in the hmi
     */
    public void writeSingleCoil(String plcName, int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException, PlcNotPresent {
        getPlc(plcName).writeSingleCoil(address, value);
    }

    private void autoRunRoutine(String ProgressionName, String BrakesName, Instant now) {
        try {
            setParkBrake(BrakesName, false);

            while (Instant.now().isBefore(now.plus(4, ChronoUnit.HOURS))) {
                setStart(ProgressionName);

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                executor.scheduleAtFixedRate(() -> {
                    try {
                        if (getCheckpoints(ProgressionName).get(4)) {
                            executor.shutdown();
                        }
                    } catch (PlcNotPresent | PlcBadType | ModbusExecutionException | ModbusTimeoutException |
                             ModbusResponseException e) {
                        throw new RuntimeException(e);
                    }
                }, 0, 1, TimeUnit.SECONDS);

                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    LOGGER.warning("A run nerver terminated in 10 minutes");
                }

                // Wait for 10 to 60 seconds
                Thread.sleep(RandomGenerator.getDefault().nextLong(10, 60) * 1000);
            }

            setParkBrake(BrakesName, true);
        } catch (PlcNotPresent | PlcBadType | ModbusExecutionException | ModbusTimeoutException |
                 ModbusResponseException | InterruptedException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    private void getPlcStatusRoutine(String ProgressionName, String BrakesName, String SecurityName,
                                     String EngineName, Instant now) {
        try {
            while (Instant.now().isBefore(now.plus(4, ChronoUnit.HOURS))) {
                int brakePressure = getBrakePressure(BrakesName);
                int activationPercent = getActivationPercent(BrakesName);

                int fenceStatus = getFenceStatus(SecurityName);
                int seatbeltStatus = getSeatbeltStatus(SecurityName);

                int engineTemp = getEngineTemp(EngineName);
                int engineRPM = getEngineRPM(EngineName);

                List<Boolean> checkpoints = getCheckpoints(ProgressionName);

                int currentCheckpoint = 0;
                for (int i = checkpoints.size() - 1; i >= 0; i--) {
                    if (checkpoints.get(i)) {
                        currentCheckpoint = i;
                        break;
                    }
                }

                System.out.println("---------------------------------------------------------------------\n"
                        + "| Brakes Pressure: " + brakePressure + " | Activation percent: " + activationPercent + " |\n"
                        + "| Fence status: " + fenceStatus + " | Seatbelt status: " + seatbeltStatus + " |\n"
                        + "| Engine temp: " + engineTemp + " | Engine RPM: " + engineRPM + " |\n"
                        + "| Current checkpoint: " + currentCheckpoint + " |\n"
                        + "---------------------------------------------------------------------"
                );

                Thread.sleep(5 * 1000);
            }
        } catch (PlcNotPresent | PlcBadType | ModbusExecutionException | ModbusTimeoutException |
                 ModbusResponseException | InterruptedException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void startRoutine(String ProgressionName, String BrakesName, String SecurityName, String LightsName,
                             String EngineName) {
        Instant now = Instant.now();
        CompletableFuture.runAsync(() -> autoRunRoutine(ProgressionName, BrakesName, now));
        CompletableFuture.runAsync(() -> getPlcStatusRoutine(ProgressionName, BrakesName, SecurityName, EngineName, now));

        // Randomly turn on lights within 1 and 4 hours
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Turning on lights");
                setLight(LightsName, true);
            } catch (ModbusExecutionException | ModbusTimeoutException | PlcNotPresent | PlcBadType |
                     ModbusResponseException e) {
                LOGGER.warning(e.getMessage());
            }
        }, CompletableFuture.delayedExecutor(RandomGenerator.getDefault().nextLong(2 * 60 * 60, 4 * 60 * 60), TimeUnit.SECONDS));
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("HMI:\n");
        for (var plc : plcs.values()) {
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
