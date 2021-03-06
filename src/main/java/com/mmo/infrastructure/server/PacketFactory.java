package com.mmo.infrastructure.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PacketFactory {

    private static PacketFactory instance;
    private final Map<UUID, PacketBuilder<? extends Packet>> builders = new LinkedHashMap<>();

    public static PacketFactory getInstance() {
        if (Objects.isNull(instance)) {
            instance = new PacketFactory();
        }

        return instance;
    }

    private PacketFactory() {

    }

    public <T extends Packet> PacketBuilder<T> getBuilder(UUID alias) {
        return Optional.ofNullable(builders.get(alias))
                .map(builder -> this.<T>cast(builder))
                .orElseThrow(
                        () -> new PacketBuilderNotFoundException("PacketBuilder not registered for alias %s", alias));
    }

    public <T extends Packet> void register(Packet packet, PacketBuilder<T> builder) {
        register(packet.getAliasAsUUID(), builder);
    }

    public <T extends Packet> void register(String alias, PacketBuilder<T> builder) {
        register(getAliasAsUUID(alias), builder);
    }

    public <T extends Packet> T getPacket(String alias, UUID source, byte[] bytes) {
        return getPacket(getAliasAsUUID(alias), source, bytes);
    }

    public <T extends Packet> void register(UUID alias, PacketBuilder<T> builder) {
        builders.put(alias, builder);
    }

    public <T extends Packet> T getPacket(UUID alias, UUID source, byte[] bytes) {
        PacketBuilder<T> builder = this.<T>getBuilder(alias);

        return builder.build(source, bytes);
    }

    @SuppressWarnings("unchecked")
    private <T extends Packet> PacketBuilder<T> cast(PacketBuilder<?> builder) {
        try {
            return (PacketBuilder<T>) builder;
        } catch (Exception exception) {
            throw new PacketBuilderCastException(exception, "Failed to cast builder");
        }
    }

    private UUID getAliasAsUUID(String alias) {
        return UUID.nameUUIDFromBytes(alias.getBytes());
    }
}
