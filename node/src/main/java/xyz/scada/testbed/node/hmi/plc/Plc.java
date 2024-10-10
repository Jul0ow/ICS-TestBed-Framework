package xyz.scada.testbed.node.hmi.plc;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.client.NettyTcpClientTransport;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.logging.Logger;

@AllArgsConstructor
@Getter
public class Plc {

    private static Logger LOGGER = Logger.getLogger(Plc.class.getName());;

    String ipAddr;
    int port;
    String name;
    String description;

    public Plc(String ipAddr, String name) {
        this.ipAddr = ipAddr;
        this.port = 502;
        this.name = name;
        this.description = "";
    }

    /* Write operations */
    public WriteSingleRegisterResponse writeSingleRegisterResponse(int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var client = connect();

        WriteSingleRegisterRequest request = new WriteSingleRegisterRequest(address, value);

        LOGGER.info("Sending writeSingleRegister: " + request);

        WriteSingleRegisterResponse writeSingleRegisterResponse = client.writeSingleRegister(
                1,
                request);

        return writeSingleRegisterResponse;
    }

    public WriteSingleCoilResponse writeSingleCoil(int address, int value) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var client = connect();

        WriteSingleCoilRequest request = new WriteSingleCoilRequest(address, value);

        LOGGER.info("Sending readCoilsRequest: " + request);

        WriteSingleCoilResponse response = client.writeSingleCoil(
                1,
                request
        );
        return response;
    }

    /* Read operations */

    public ReadHoldingRegistersResponse readHoldingRegister(int address, int quantity) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var client = connect();

        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(address, quantity);

        LOGGER.info("Sending readHoldingRegisters: " + request);

        ReadHoldingRegistersResponse response = client.readHoldingRegisters(
                1,
                request
        );

        return response;
    }

    public ReadCoilsResponse readCoils(int address, int quantity) throws ModbusExecutionException, ModbusTimeoutException, ModbusResponseException {
        var client = connect();

        ReadCoilsRequest request = new ReadCoilsRequest(address, quantity);

        LOGGER.info("Sending readCoilsRequest: " + request);

        ReadCoilsResponse response = client.readCoils(
                1,
                request
        );
        return response;
    }

    @Override
    public String toString() {
        return "Plc{" +
                "name='" + name + '\'' +
                ", ipAddr='" + ipAddr + '\'' +
                ", port=" + port +
                ", description='" + description + '\'' +
                '}';
    }

    private ModbusTcpClient connect() throws ModbusExecutionException {
        var transport = NettyTcpClientTransport.create(cfg -> {
            cfg.hostname = ipAddr;
            cfg.port = port;
        });

        var client = ModbusTcpClient.create(transport);
        client.connect();
        return client;
    }
}
