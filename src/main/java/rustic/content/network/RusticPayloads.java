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

public final class RusticPayloads {

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

        if (context.player() instanceof ServerPlayer player) {
            RusticEventHandlerPotions.doFirePowerAttack(player);
        }
    }

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

    public record VaseVariantPayload(int variant) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vase_variant");
        public static final Type<VaseVariantPayload> TYPE = new Type<>(ID);

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

        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int variant = Math.clamp(payload.variant(), VaseBlock.MIN_VARIANT, VaseBlock.MAX_VARIANT);
        BlockItemStateProperties blockState = BlockItemStateProperties.EMPTY.with(VaseBlock.VARIANT, variant);

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
