package net.nanaky.frost_lava_walker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SpawnParticlePacket(double x, double y, double z, EventType event) implements CustomPacketPayload {

    public enum EventType {
        LAVA_CONVERSION,
        LAVA_SETTLE,
        LAVA_WARN,
        LAVA_REVERT,

        ICE_FREEZE,
        ICE_BREAK
    }

    public static final CustomPacketPayload.Type<SpawnParticlePacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("frost_lava_walker", "spawn_lava_pop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnParticlePacket> CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeEnum(p.event);
            },
            buf -> new SpawnParticlePacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readEnum(EventType.class)
            )
        );

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}