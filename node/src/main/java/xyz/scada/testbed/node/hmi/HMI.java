package xyz.scada.testbed.node.hmi;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;
import xyz.scada.testbed.node.ModBusTCP;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HMI {
    private static Logger LOGGER = null;

    public HMI() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Starting HMI");
    }

    public void readHoldingRegisters(String destIp) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var transport = NettyTcpClientTransport.create(cfg -> {
            cfg.hostname = destIp;
            cfg.port = 502;
        });

        var client = ModbusTcpClient.create(transport);
        client.connect();

        LOGGER.info("Sending writeSingleRegister");

        WriteSingleRegisterResponse writeSingleRegisterResponse = client.writeSingleRegister(
                1,
                new WriteSingleRegisterRequest(0, 1));

        LOGGER.info("write response " + writeSingleRegisterResponse);

        LOGGER.info("Sending readHoldingRegisters");

        ReadHoldingRegistersResponse response = client.readHoldingRegisters(
                1,
                new ReadHoldingRegistersRequest(0, 10)
        );

        LOGGER.info("Response: " + response);
    }
}
