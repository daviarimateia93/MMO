package com.mmo.infrastructure.map;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mmo.core.game.Game;
import com.mmo.core.map.Map;
import com.mmo.core.security.Decryptor;
import com.mmo.core.security.Encryptor;
import com.mmo.infrastructure.map.packet.HelloPacket;
import com.mmo.infrastructure.server.Client;
import com.mmo.infrastructure.server.ClientPacketReceiveSubscriber;
import com.mmo.infrastructure.server.ClientPacketSendSubscriber;
import com.mmo.infrastructure.server.Packet;
import com.mmo.infrastructure.server.Server;

public class MapServer implements ClientPacketSendSubscriber, ClientPacketReceiveSubscriber {

    private static final UUID SERVER_SOURCE = UUID.fromString("39bb6712-db5c-4cae-9e67-143c3a97115d");
    private static final int SERVER_PORT = 5555;
    private static final String SERVER_CIPHER_KEY = "Bar12345Bar12345";
    private static final int HELLO_PACKET_WAITING_DELAY_IN_MINUTES = 5;
    private static final Logger logger = LoggerFactory.getLogger(MapServer.class);

    private final ConcurrentHashMap<Client, UUID> clients = new ConcurrentHashMap<>();
    private final Map map;
    private final Server server;

    private MapServer() {
        logger.info("Loading map");

        map = loadMap();

        logger.info("Starting server");

        server = createServer();
        server.run();

        logger.info("Running game");

        Game.getInstance().run(map);
    }

    private Map loadMap() {
        return Map.builder()
                .name("adventure_plains")
                .description("Located at the southern end, these plains were quiet and peaceful.")
                .nearbyRatio(10)
                .build();
    }

    private Server createServer() {
        Encryptor encryptor = Encryptor.builder()
                .key(SERVER_CIPHER_KEY)
                .build();

        Decryptor decryptor = Decryptor.builder()
                .key(SERVER_CIPHER_KEY)
                .build();

        return Server.builder()
                .port(SERVER_PORT)
                .encryptor(encryptor)
                .decryptor(decryptor)
                .onClientConnect(this::confirmClientConnected)
                .onClientDisconnect(this::removeClient)
                .sendSubscriber(this)
                .receiveSubscriber(this)
                .build();
    }

    private void confirmClientConnected(Client client) {
        logger.info("Client has connected {}, waiting for HelloPacket", client);

        Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> {
                    if (isConnected(client)) {
                        logger.info("Client sent HelloPacket");
                    } else {
                        disconnect(client);
                    }
                }, HELLO_PACKET_WAITING_DELAY_IN_MINUTES, TimeUnit.MINUTES);
    }

    private void removeClient(Client client) {
        if (isConnected(client)) {
            clients.remove(client);
            logger.info("Client has disconnected {}", client);
        }
    }

    @Override
    public void onReceive(Client client, Packet packet) {
        logger.info("Received packet {} from client {}", packet, client);

        boolean connected = isConnected(client);

        if (!connected && packet instanceof HelloPacket) {
            clients.put(client, packet.getSource());

            HelloPacket.builder().build(SERVER_SOURCE, new byte[0]);
            return;
        }

        if (!connected) {
            logger.info("Client did not send HelloPacket, it will disconnect", client);
            client.disconnect();
            return;
        }

        PacketHandlerDelegator.getInstance().delegate(map, packet);
    }

    @Override
    public void onSend(Client client, Packet packet) {
        logger.info("Sent packet {} to client {}", packet, client);
    }

    private boolean isConnected(Client client) {
        return clients.containsKey(client);
    }

    private void disconnect(Client client) {
        logger.info("Client {} did not send HelloPacket, it will disconnect", client);
        client.disconnect();
    }

    public static void main(String... args) {
        new MapServer();
    }
}
