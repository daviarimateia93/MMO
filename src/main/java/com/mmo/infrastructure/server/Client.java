package com.mmo.infrastructure.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.mmo.core.security.Decryptor;
import com.mmo.core.security.Encryptor;
import com.mmo.core.security.TokenData;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Client {

    private final UUID id = UUID.randomUUID();
    private final Socket socket;
    private final Encryptor encryptor;
    private final Decryptor decryptor;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final Consumer<Client> onDisconnect;
    private final ClientPacketSendSubscriber sendSubscriber;
    private final ClientPacketReceiveSubscriber receiveSubscriber;
    private final BlockingQueue<Packet> sendingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService sendingPool = Executors.newSingleThreadExecutor();
    private final ExecutorService receivingPool = Executors.newSingleThreadExecutor();
    private boolean connected;

    @Builder(builderMethodName = "serverBuilder", buildMethodName = "serverBuild")
    private Client(
            @NonNull Socket socket,
            @NonNull Encryptor encryptor,
            @NonNull Decryptor decryptor,
            Consumer<Client> onDisconnect,
            ClientPacketSendSubscriber sendSubscriber,
            ClientPacketReceiveSubscriber receiveSubscriber) {

        this.socket = socket;
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.inputStream = getDataInputStream();
        this.outputStream = getDataOutputStream();
        this.onDisconnect = onDisconnect;
        this.sendSubscriber = sendSubscriber;
        this.receiveSubscriber = receiveSubscriber;
        this.connected = true;

        startPools();
    }

    @Builder(builderMethodName = "clientBuilder", buildMethodName = "clientBuild")
    private Client(
            @NonNull String host,
            @NonNull Integer port,
            @NonNull Encryptor encryptor,
            @NonNull Decryptor decryptor,
            Consumer<Client> onDisconnect,
            ClientPacketSendSubscriber sendSubscriber,
            ClientPacketReceiveSubscriber receiveSubscriber) {

        this.socket = connect(host, port);
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.inputStream = getDataInputStream();
        this.outputStream = getDataOutputStream();
        this.onDisconnect = onDisconnect;
        this.sendSubscriber = sendSubscriber;
        this.receiveSubscriber = receiveSubscriber;
        this.connected = true;

        startPools();
    }

    public UUID getId() {
        return id;
    }

    public boolean isConnected() {
        return connected;
    }

    private Optional<Consumer<Client>> getOnDisconnect() {
        return Optional.ofNullable(onDisconnect);
    }

    private Optional<ClientPacketSendSubscriber> getSendSubscriber() {
        return Optional.ofNullable(sendSubscriber);
    }

    private Optional<ClientPacketReceiveSubscriber> getReceiveSubscriber() {
        return Optional.ofNullable(receiveSubscriber);
    }

    private DataInputStream getDataInputStream() {
        try {
            return new DataInputStream(socket.getInputStream());
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to get input stream");
        }
    }

    private DataOutputStream getDataOutputStream() {
        try {
            return new DataOutputStream(socket.getOutputStream());
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to get output stream");
        }
    }

    private Socket connect(String host, Integer port) {
        try {
            return new Socket(host, port);
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to create socket");
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception exception) {
            throw new ClientDisconnectException(exception, "Failed to close socket");
        } finally {
            connected = false;
            getOnDisconnect().ifPresent(consumer -> consumer.accept(this));
        }
    }

    public void send(Packet packet) {
        sendingQueue.add(packet);
    }

    private void startPools() {
        sendingPool.execute(this::send);
        receivingPool.execute(this::receive);

    }

    private void send() {
        Packet packet;

        try {
            while ((packet = sendingQueue.take()) != null) {
                sendPacket(packet);
            }
        } catch (Exception exception) {
            throw new ClientSendException(exception, "Failed to send packet");
        } finally {
            disconnect();
        }
    }

    private void receive() {
        try {
            while (true) {
                receivePacket();
            }
        } catch (Exception exception) {
            throw new ClientReadException(exception, "Failed to receive packet");
        } finally {
            disconnect();
        }
    }

    private void sendPacket(Packet packet) throws IOException {
        byte[] bytes = packet.toBytes();
        UUID source = packet.getSource();
        UUID alias = packet.getAliasAsUUID();
        String token = encryptor.encrypt(TokenData.create(source).getToken());

        outputStream.writeUTF(token);
        outputStream.writeLong(alias.getMostSignificantBits());
        outputStream.writeLong(alias.getLeastSignificantBits());
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);

        getSendSubscriber().ifPresent(subscriber -> subscriber.onSend(this, packet));
    }

    private void receivePacket() throws IOException {
        String token = decryptor.decrypt(inputStream.readUTF());
        long aliasHigh = inputStream.readLong();
        long aliasLow = inputStream.readLong();
        int size = inputStream.readInt();

        TokenData tokenData = TokenData.parse(token);

        if (tokenData.isExpired()) {
            throw new ClientReadException("Token is expired %s", tokenData);
        }

        UUID source = tokenData.getSource();
        UUID alias = new UUID(aliasHigh, aliasLow);

        byte[] bytes = new byte[size];
        inputStream.readFully(bytes);

        Packet packet = PacketFactory.getInstance().getPacket(alias, source, bytes);

        getReceiveSubscriber().ifPresent(subscriber -> subscriber.onReceive(this, packet));
    }
}
