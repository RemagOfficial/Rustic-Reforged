package rustic.content.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rustic.Rustic;
import rustic.config.RusticConfig;
import rustic.content.client.IronSkinRenderPolicy;
import rustic.content.client.screen.RusticApiaryScreen;
import rustic.content.client.screen.RusticBrewingBarrelScreen;
import rustic.content.client.screen.RusticChestScreen;
import rustic.content.client.screen.RusticCondenserScreen;
import rustic.content.client.screen.RusticCondenserAdvancedScreen;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.VaseBlock;
import rustic.content.blockentity.RusticBlockEntities;
import rustic.content.client.hud.ArmorOverflowLayer;
import rustic.content.client.hud.ArmorToughnessLayer;
import rustic.content.client.highlight.RotatedPlacementHighlight;
import rustic.content.client.overlay.FullmetalOverlayLayer;
import rustic.content.client.overlay.FluidOverlayLayer;
import rustic.content.client.model.CabinetModel;
import rustic.content.client.renderer.CabinetBlockItemRenderer;
import rustic.content.client.renderer.CabinetRenderer;
import rustic.content.client.renderer.CrushingTubRenderer;
import rustic.content.client.renderer.EvaporatingBasinRenderer;
import rustic.content.client.renderer.IronSkinRenderLayer;
import rustic.content.client.renderer.LiquidBarrelRenderer;
import rustic.content.entities.RusticEntities;
import rustic.content.fluids.RusticFluids;
import rustic.content.menu.RusticMenus;
import rustic.content.network.RusticPayloads;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;
import rustic.content.potions.RusticMobEffects;

public final class RusticClient {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RusticClient::registerRenderers);
        modEventBus.addListener(RusticClient::registerLayerDefinitions);
        modEventBus.addListener(RusticClient::addLayers);

        modEventBus.addListener(RusticClient::registerGuiLayers);
        modEventBus.addListener(RusticClient::registerScreens);

        modEventBus.addListener(RusticClient::registerFluidExtensions);
        modEventBus.addListener(RusticClient::registerItemExtensions);

        modEventBus.addListener(RusticClient::registerBlockColors);

        modEventBus.addListener(RusticClient::registerItemColors);

        modEventBus.addListener(RusticClient::registerMobEffectExtensions);

        modEventBus.addListener(RusticClient::onClientSetup);

        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickEmpty);
        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickBlock);

        NeoForge.EVENT_BUS.addListener(RusticClient::onComputeFovModifier);
        NeoForge.EVENT_BUS.addListener(RusticClient::onMovementInput);

        NeoForge.EVENT_BUS.addListener(RotatedPlacementHighlight::render);

        NeoForge.EVENT_BUS.addListener(RusticClient::onVaseMouseWheel);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                RusticMachineBlocks.VASE.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vase_variant"),
                (stack, level, entity, seed) -> (float) VaseBlock.variantOf(stack)));
    }

    private static void onVaseMouseWheel(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(RusticMachineBlocks.VASE.get().asItem())) {
            return;
        }
        int offset = (int) event.getScrollDeltaY();
        if (offset == 0) {
            return;
        }
        event.setCanceled(true);
        int next = VaseBlock.nextVariant(VaseBlock.variantOf(held), offset);
        PacketDistributor.sendToServer(new RusticPayloads.VaseVariantPayload(next));
    }

    private static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        sendFirePowerIfApt(event.getEntity());
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            sendFirePowerIfApt(event.getEntity());
        }
    }

    private static void sendFirePowerIfApt(Player player) {
        if (player.isCrouching() || !player.hasEffect(RusticMobEffects.FIRE_POWER)) {
            return;
        }
        PacketDistributor.sendToServer(RusticPayloads.FirePowerAttackPayload.INSTANCE);
    }

    private static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (!player.hasEffect(RusticMobEffects.FULLMETAL)) {
            return;
        }
        float fov = 1.0F;
        if (player.isUsingItem() && player.getUseItem().is(Items.BOW)) {
            float draw = player.getTicksUsingItem() / 20.0F;
            if (draw > 1.0F) {
                draw = 1.0F;
            } else {
                draw = draw * draw;
            }
            fov *= 1.0F - draw * 0.15F;
        }
        event.setNewFovModifier(fov);
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity().hasEffect(RusticMobEffects.FULLMETAL)) {
            Input input = event.getInput();
            input.jumping = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {

        event.register(RusticMenus.BARREL.get(), RusticChestScreen::new);
        event.register(RusticMenus.VASE.get(), RusticChestScreen::new);
        event.register(RusticMenus.CABINET.get(), RusticChestScreen::new);
        event.register(RusticMenus.DOUBLE_CABINET.get(), RusticChestScreen::new);

        event.register(RusticMenus.BREWING_BARREL.get(), RusticBrewingBarrelScreen::new);

        event.register(RusticMenus.CONDENSER.get(), RusticCondenserScreen::new);
        event.register(RusticMenus.CONDENSER_ADVANCED.get(), RusticCondenserAdvancedScreen::new);

        event.register(RusticMenus.APIARY.get(), RusticApiaryScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RusticEntities.CHAIR.get(), NoopRenderer::new);

        event.registerEntityRenderer(RusticEntities.TOMATO.get(), ThrownItemRenderer::new);

        event.registerBlockEntityRenderer(RusticBlockEntities.CRUSHING_TUB.get(), CrushingTubRenderer::new);
        event.registerBlockEntityRenderer(RusticBlockEntities.EVAPORATING_BASIN.get(), EvaporatingBasinRenderer::new);
        event.registerBlockEntityRenderer(RusticBlockEntities.LIQUID_BARREL.get(), LiquidBarrelRenderer::new);

        event.registerBlockEntityRenderer(RusticBlockEntities.CABINET.get(), CabinetRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CabinetModel.SINGLE, () -> CabinetModel.createBodyLayer(false, false));
        event.registerLayerDefinition(CabinetModel.SINGLE_MIRROR, () -> CabinetModel.createBodyLayer(true, false));
        event.registerLayerDefinition(CabinetModel.DOUBLE, () -> CabinetModel.createBodyLayer(false, true));
        event.registerLayerDefinition(CabinetModel.DOUBLE_MIRROR, () -> CabinetModel.createBodyLayer(true, true));
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        List<? extends String> blacklist = RusticConfig.IRONSKIN_RENDER_ENTITY_BLACKLIST.get();
        if (IronSkinRenderPolicy.isEntityBlacklisted(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER), blacklist)) {
            return;
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new IronSkinRenderLayer(renderer));
            }
        }
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "armor_overflow"), new ArmorOverflowLayer());
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "armor_toughness"), new ArmorToughnessLayer());

        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_overlay"),
                new FullmetalOverlayLayer());

        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fluid_overlay"),
                new FluidOverlayLayer());
    }

    private static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        placeableExtension(event, RusticFluids.OLIVE_OIL_TYPE, "olive_oil");
        placeableExtension(event, RusticFluids.IRONBERRY_JUICE_TYPE, "ironberry_juice");
        placeableExtension(event, RusticFluids.WILDBERRY_JUICE_TYPE, "wildberry_juice");
        placeableExtension(event, RusticFluids.GRAPE_JUICE_TYPE, "grape_juice");
        placeableExtension(event, RusticFluids.APPLE_JUICE_TYPE, "apple_juice");
        placeableExtension(event, RusticFluids.ALE_WORT_TYPE, "ale_wort");
        placeableExtension(event, RusticFluids.HONEY_TYPE, "honey");
        placeableExtension(event, RusticFluids.GOLDEN_APPLE_JUICE_TYPE, "golden_apple_juice");
        placeableExtension(event, RusticFluids.VANTA_OIL_TYPE, "vanta_oil");
        boozeExtension(event, RusticFluids.ALE_TYPE, "ale");
        boozeExtension(event, RusticFluids.CIDER_TYPE, "cider");
        boozeExtension(event, RusticFluids.IRON_WINE_TYPE, "iron_wine");
        boozeExtension(event, RusticFluids.MEAD_TYPE, "mead");
        boozeExtension(event, RusticFluids.WILDBERRY_WINE_TYPE, "wildberry_wine");
        boozeExtension(event, RusticFluids.WINE_TYPE, "wine");
        boozeExtension(event, RusticFluids.AMBROSIA_TYPE, "ambrosia");
    }

    private static void placeableExtension(RegisterClientExtensionsEvent event, Holder<FluidType> type, String name) {
        String base = "block/fluids/" + name;
        event.registerFluidType(fluidTextures(base + "_still", base + "_flow", base + "_overlay"), type);

        FLUID_RENDER_OVERLAYS.put(type.value(),
                ResourceLocation.parse(Rustic.MODID + ":textures/" + base + "_overlay.png"));
    }

    private static void boozeExtension(RegisterClientExtensionsEvent event, Holder<FluidType> type, String name) {
        String base = "block/fluids/booze/" + name;
        event.registerFluidType(fluidTextures(base + "_still", base + "_flow", null), type);
    }

    private static IClientFluidTypeExtensions fluidTextures(String still, String flowing, @Nullable String overlay) {
        ResourceLocation stillLoc = ResourceLocation.parse(Rustic.MODID + ":" + still);
        ResourceLocation flowingLoc = ResourceLocation.parse(Rustic.MODID + ":" + flowing);
        ResourceLocation overlayLoc = overlay == null ? null : ResourceLocation.parse(Rustic.MODID + ":" + overlay);

        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillLoc;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingLoc;
            }

            @Override
            @Nullable
            public ResourceLocation getOverlayTexture() {
                return overlayLoc;
            }
        };
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {

        BlockColor foliageTint = (state, level, pos, tintIndex) ->
                level != null && pos != null
                        ? BiomeColors.getAverageFoliageColor(level, pos)
                        : FoliageColor.getDefaultColor();
        event.register(foliageTint,
                RusticBlocks.olive().leaves().get(),
                RusticBlocks.ironwood().leaves().get(),
                RusticFarmBlocks.APPLE_LEAVES.get(),
                RusticFarmBlocks.GRAPE_LEAVES.get(),
                RusticFarmBlocks.WILDBERRY_BUSH.get());
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            BlockState blockstate = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            return event.getBlockColors().getColor(blockstate, null, null, tintIndex);
        },
                RusticBlocks.olive().leaves().get(),
                RusticBlocks.ironwood().leaves().get(),
                RusticFarmBlocks.APPLE_LEAVES.get(),
                RusticFarmBlocks.WILDBERRY_BUSH.get());

        event.register(ElixirUtils::getTint,
                RusticItems.ELIXIR.get());
    }

    private static void registerMobEffectExtensions(RegisterClientExtensionsEvent event) {
        registerEffectIcon(event, RusticMobEffects.IRON_SKIN, 0, 0);
        registerEffectIcon(event, RusticMobEffects.FEATHER, 1, 0);
        registerEffectIcon(event, RusticMobEffects.BLAZING_TRAIL, 2, 0);
        registerEffectIcon(event, RusticMobEffects.SHAME, 3, 0);
        registerEffectIcon(event, RusticMobEffects.TIPSY, 4, 0);
        registerEffectIcon(event, RusticMobEffects.FULL, 5, 0);
        registerEffectIcon(event, RusticMobEffects.MAGIC_RESISTANCE, 6, 0);
        registerEffectIcon(event, RusticMobEffects.WITHER_WARD, 7, 0);
        registerEffectIcon(event, RusticMobEffects.FULLMETAL, 0, 1);
        registerEffectIcon(event, RusticMobEffects.UNDYING, 1, 1);
        registerEffectIcon(event, RusticMobEffects.FIRE_POWER, 2, 1);
    }

    private static void registerEffectIcon(RegisterClientExtensionsEvent event,
            DeferredHolder<MobEffect, ? extends MobEffect> effect, int iconX, int iconY) {
        int[] tile = RusticMobEffectIcons.tileFor(iconX, iconY);
        IClientMobEffectExtensions extensions = RusticMobEffectIcons.forTile(tile[0], tile[1]);
        event.registerMobEffect(extensions, effect.get());
    }

    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {

                if (cabinetItemRenderer == null) {
                    cabinetItemRenderer = new CabinetBlockItemRenderer();
                }
                return cabinetItemRenderer;
            }
        }, RusticMachineBlocks.CABINET.get().asItem());
    }

    @Nullable
    private static BlockEntityWithoutLevelRenderer cabinetItemRenderer;

    public static final Map<FluidType, ResourceLocation> FLUID_RENDER_OVERLAYS = new HashMap<>();

    private RusticClient() {
    }
}
