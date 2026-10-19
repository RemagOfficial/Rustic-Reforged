package rustic.content.network;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.List;
import rustic.Rustic;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.VaseBlock;
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
 * <p>This file grows as later phases need channels. {@link VaseVariantPayload} is the Phase 16 addition: the vase
 * style picker's client-to-server recolour, the 1.21 form of the legacy {@code MessageVaseMeta}.
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
        registrar.playToServer(
                VaseVariantPayload.TYPE,
                VaseVariantPayload.STREAM_CODEC,
                RusticPayloads::handleVaseVariant);
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

    /**
     * The Phase 16 vase style picker's server bound. It carries only the chosen {@code variant}, like the legacy
     * {@code MessageVaseMeta} carried only a meta int. The handler stamps {@link DataComponents#BLOCK_STATE} onto
     * every vase stack in the sending player's inventories - the 1.21 equivalent of the old {@code setItemDamage},
     * recolouring the held item (and all others) rather than a placed block.
     */
    public record VaseVariantPayload(int variant) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vase_variant");
        public static final Type<VaseVariantPayload> TYPE = new Type<>(ID);
        // A hand-rolled varint codec rather than ByteBufCodecs.VAR_INT.map(...): StreamCodec.of infers the buffer
        // type from the target field (RegistryFriendlyByteBuf), so no cast or unchecked warning is needed, and the
        // wire format is a single varint exactly like the legacy MessageVaseMeta's int field.
        public static final StreamCodec<RegistryFriendlyByteBuf, VaseVariantPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeVarInt(payload.variant()),
                        buf -> new VaseVariantPayload(buf.readVarInt()));

        @Override
        public Type<VaseVariantPayload> type() {
            return TYPE;
        }
    }

    private static void handleVaseVariant(VaseVariantPayload payload, IPayloadContext context) {
        // Registrar-wrapped on the main thread, and a PLAY-to-server payload carries the sender.
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int variant = Math.clamp(payload.variant(), VaseBlock.MIN_VARIANT, VaseBlock.MAX_VARIANT);
        BlockItemStateProperties blockState = BlockItemStateProperties.EMPTY.with(VaseBlock.VARIANT, variant);
        // Legacy looped mainInventory + offHandInventory and skipped non-vase stacks; PlayerInventory exposes
        // the same two lists, so the recolour stays faithful and never touches a placed block.
        Inventory inventory = player.getInventory();
        recolorVases(inventory.items, blockState);
        recolorVases(inventory.offhand, blockState);
        player.inventoryMenu.broadcastChanges();
    }

    private static void recolorVases(List<ItemStack> stacks, BlockItemStateProperties blockState) {
        Item vase = RusticMachineBlocks.VASE.get().asItem();
        for (ItemStack stack : stacks) {
            if (stack.is(vase)) {
                stack.set(DataComponents.BLOCK_STATE, blockState);
            }
        }
    }

    private RusticPayloads() {
    }
}
