package rustic.content.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rustic.Rustic;
import rustic.content.potions.RusticEventHandlerPotions;

/**
 * The mod's custom network payloads, the 1.21 replacement for 1.12.2's {@code PacketHandler} + {@code IMessage}
 * simpleimpl stack ({@code MessageFirePowerAttack} and its {@code MessageHolder}).
 *
 * <p>NeoForge 1.21 registers payloads through {@link RegisterPayloadHandlersEvent} on the mod bus and a
 * {@link PayloadRegistrar} builder, and dispatches on {@link CustomPacketPayload} records with an explicit
 * {@link StreamCodec} - the {@code registry.addDiscriminator}/{@code NetworkRegistry} pair is gone. The registrar
 * wraps every handler in {@code MainThreadPayloadHandler} by default, so {@link #handleFirePowerAttack} already runs
 * on the server thread and needs no manual {@code addScheduledTask} hop that the legacy {@code MessageHolder} used.
 *
 * <p>This is the first payload the port introduces; the same file grows as later phases need channels (the Phase 7
 * vase interaction packet is the next expected tenant, per the plan).
 */
public final class RusticPayloads {

    /** Attaches the payload registration listener to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RusticPayloads::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                FirePowerAttackPayload.TYPE,
                FirePowerAttackPayload.STREAM_CODEC,
                RusticPayloads::handleFirePowerAttack);
    }

    private static void handleFirePowerAttack(FirePowerAttackPayload payload, IPayloadContext context) {
        // The registrar wrapped this on the main thread, and a server-bound PLAY payload carries the sending player.
        if (context.player() instanceof ServerPlayer player) {
            RusticEventHandlerPotions.doFirePowerAttack(player);
        }
    }

    /**
     * The client-to-server "the player left-clicked while under fire power" ping. It carries no fields because the
     * legacy {@code MessageFirePowerAttack} had none either - the server re-reads the amplifier/position from the
     * sending player, so an {@link StreamCodec#unit} codec that always decodes to the shared instance is exact.
     */
    public record FirePowerAttackPayload() implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fire_power_attack");
        public static final Type<FirePowerAttackPayload> TYPE = new Type<>(ID);
        public static final FirePowerAttackPayload INSTANCE = new FirePowerAttackPayload();
        public static final StreamCodec<RegistryFriendlyByteBuf, FirePowerAttackPayload> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<FirePowerAttackPayload> type() {
            return TYPE;
        }
    }

    private RusticPayloads() {
    }
}
