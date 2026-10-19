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

/**
 * The client half of the mod, reached only when {@link FMLEnvironment#dist} is {@link Dist#CLIENT} -
 * the 1.21 replacement for the legacy {@code ClientProxy}/{@code GuiProxy} pair, of which this file so
 * far needs one member: the chair seat's entity renderer.
 *
 * <p>Nothing in here may be loaded on a dedicated server, which is why {@link #register(IEventBus)} is
 * the only entry point and why the caller in {@code Rustic} tests the dist before it takes the method
 * reference below. Adding listeners here as later phases need them is the intended growth path; the
 * alternative - a second {@code @Mod} class annotated {@code distType = DistType.CLIENT} - would split
 * the bootstrap across two files for no gain.
 */
public final class RusticClient {

    /** Attaches the client listeners to the mod event bus. Call only on the client dist. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RusticClient::registerRenderers);
        modEventBus.addListener(RusticClient::registerLayerDefinitions);
        modEventBus.addListener(RusticClient::addLayers);
        // Phase 9 S9.5: the extra armour rows (vanilla clips the bar at 20 points) and the custom toughness meter,
        // both as GuiLayers so they slot into 1.21's LayeredDraw pipeline instead of the legacy overlay-event hooks.
        modEventBus.addListener(RusticClient::registerGuiLayers);
        modEventBus.addListener(RusticClient::registerScreens);
        // Without a client extension every Rustic FluidType answers IClientFluidTypeExtensions.DEFAULT, whose
        // getStillTexture() returns null; FluidSpriteCache then does Map.of().getOrDefault(null, ...) and the render
        // thread throws "Cannot invoke Object.hashCode() because pk is null" the moment a placed fluid is in view,
        // crashing every subsequent world load too. Supplying each fluid's sprites here is the 1.21 replacement for
        // the deprecated FluidType#initializeClient and must run on the mod bus, client side only.
        modEventBus.addListener(RusticClient::registerFluidExtensions);
        modEventBus.addListener(RusticClient::registerItemExtensions);
        // Phase 8 worldgen: vanilla BlockColors uses an IdentityHashMap keyed by Block instance and only registers
        // the foliage tint for its own OAK_LEAVES / JUNGLE_LEAVES / etc. Rustic's olive, ironwood and apple leaves
        // are distinct instances (plain LeavesBlock or BlockLeavesApple subclass) that do not match any vanilla key,
        // so getColor returns -1 (white) and the grayscale textures render without biome tinting.
        modEventBus.addListener(RusticClient::registerBlockColors);
        // Companion to registerBlockColors: ItemColors is a separate IdentityHashMap keyed by Item instance and
        // vanilla only registers the leaf-item handler for its own OAK_LEAVES / JUNGLE_LEAVES / etc. items. Without
        // an equivalent registration for our leaf items the inventory / hand model resolves tintIndex against a
        // null handler and the icon renders flat gray even though the placed block tints correctly.
        modEventBus.addListener(RusticClient::registerItemColors);
        // Phase 17 S17.5: the status-effect icons. 1.21 dropped the legacy per-effect setIconIndex, so each Rustic
        // effect's HUD / inventory icon is blitted from rustic:textures/gui/potion_icons.png through a client
        // extension (RusticMobEffectIcons) - the direct replacement for PotionBase.getStatusIconIndex binding the sheet.
        modEventBus.addListener(RusticClient::registerMobEffectExtensions);
        // Phase 16: the vase variant item model. 1.21.1 has no per-stack ITEM_MODEL component, so the per-variant
        // icon rides an ItemProperties predicate (rustic:vase_variant) read off the stack's BLOCK_STATE, driving the
        // overrides in the hand-authored models/item/vase.json - the replacement for legacy BlockVase.initModel's
        // per-damage ModelResourceLocation loop. There is no RegisterClientCapabilities hook in 21.1, so register on
        // the mod-bus client-setup event (enqueueWork'd because client setup runs in parallel).
        modEventBus.addListener(RusticClient::onClientSetup);
        // Phase 6 fire power: the client's left-clicks into empty space and onto blocks are not visible to the
        // server, so they ping RusticPayloads.FirePowerAttackPayload back. Registered on the game bus because these
        // are NeoForge gameplay events; this method only runs on the client dist, so a server never sees them.
        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickEmpty);
        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickBlock);
        // Phase 9 S9.6: fullmetal FOV clamp + movement freeze. Both events fire on the game bus, client side only,
        // so they are registered the same way as the fire-power click handlers above.
        NeoForge.EVENT_BUS.addListener(RusticClient::onComputeFovModifier);
        NeoForge.EVENT_BUS.addListener(RusticClient::onMovementInput);
        // Phase 9 S9.7: the rotated-placement marker, an extra pair of diagonal lines painted on top of the vanilla
        // selection box when a Y face of an AdvancedRotationPlacement block is hovered with that block in hand.
        NeoForge.EVENT_BUS.addListener(RotatedPlacementHighlight::render);
        // Phase 16: the vase style picker, the sneak + mouse-wheel held-item recolour that was legacy
        // EventHandlerClient.onVaseMouseWheel. Game bus, client side only, cancellable so the vanilla inventory
        // scroll never also fires while cycling styles.
        NeoForge.EVENT_BUS.addListener(RusticClient::onVaseMouseWheel);
    }

    /**
     * Phase 16: bind the {@code rustic:vase_variant} item-property that selects the vase's per-variant model.
     * The value is the stack's {@link VaseBlock#VARIANT} (via its {@code BLOCK_STATE} component) as a float, so
     * variant 0 uses the base parent and overrides 1..12 each pick their own block model.
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                RusticMachineBlocks.VASE.get().asItem(),
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vase_variant"),
                (stack, level, entity, seed) -> (float) VaseBlock.variantOf(stack)));
    }

    /**
     * Phase 16: {@code onVaseMouseWheel}. Sneaking and scrolling the wheel while the vase block item is in the
     * main hand cycles every vase stack's variant (server-authoritative). The notch count is the integer scroll
     * delta - the 1.21 analogue of legacy {@code dwheel / 120}. Unlike the legacy handler this does not mutate the
     * client stack locally first: {@code BLOCK_STATE} is network-synchronised, so the server's recolour syncs back
     * and repaints the held item, at worst a one-tick round trip.
     */
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

    /** Left click on nothing, client only. */
    private static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        sendFirePowerIfApt(event.getEntity());
    }

    /** Left click on a block. Fires on both sides, so keep to the client start-of-break only. */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            sendFirePowerIfApt(event.getEntity());
        }
    }

    /**
     * The legacy client guard ({@code !isSneaking && potion active}) that used to gate sending {@code
     * MessageFirePowerAttack}. The old {@code !isSwingInProgress} sub-check has no public 1.21 equivalent and is
     * dropped as a cosmetic anti-double-fire nicety (parked with the other client polish); the server re-validates
     * the effect before spawning, so a stray ping is harmless.
     */
    private static void sendFirePowerIfApt(Player player) {
        if (player.isCrouching() || !player.hasEffect(RusticMobEffects.FIRE_POWER)) {
            return;
        }
        PacketDistributor.sendToServer(RusticPayloads.FirePowerAttackPayload.INSTANCE);
    }

    /**
     * S9.6: {@code onFullmetalFov}. While the client player is under the fullmetal effect the FOV is pinned to a
     * stable {@code 1.0} (cancelling sprint/scope/etc. modifiers, exactly as the legacy HIGHEST-priority override
     * did) and only re-clamped by the vanilla bow-draw shrink, so a fullmetal archer still gets the zoom-in while
     * drawing.
     */
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

    /**
     * S9.6: {@code onPlayerInputEvent}. Fullmetal roots the player in place - no jump, no forward, no strafe -
     * the 1.21 {@link Input}-impulse equivalent of the legacy {@code MovementInput} zeroing.
     */
    private static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity().hasEffect(RusticMobEffects.FULLMETAL)) {
            Input input = event.getInput();
            input.jumping = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }
    }

    /**
     * Binds the tile-entity {@code MenuType}s to the {@code RusticChestScreen} that replaces the legacy
     * {@code GuiCabinet}/{@code GuiBarrel}/{@code GuiVase} trio. This is the client half of what
     * {@code GuiProxy.getClientGuiElement} used to do; the proxy's other half ({@code openGui} ids) is
     * gone because the menu now opens straight from the block entity's {@code MenuProvider}.
     */
    private static void registerScreens(RegisterMenuScreensEvent event) {
        // One screen class serves all four: RusticChestScreen reads the menu's row count to pick the
        // generic_27 or generic_54 panel and the matching window height, exactly as the four legacy Gui
        // classes did when they each hard-coded their own.
        event.register(RusticMenus.BARREL.get(), RusticChestScreen::new);
        event.register(RusticMenus.VASE.get(), RusticChestScreen::new);
        event.register(RusticMenus.CABINET.get(), RusticChestScreen::new);
        event.register(RusticMenus.DOUBLE_CABINET.get(), RusticChestScreen::new);
        // The brewing barrel is the phase's first bespoke GUI: its own screen draws the fluid tanks and brew
        // progress over the brewing_barrel panel, replacing the legacy GuiBrewingBarrel.
        event.register(RusticMenus.BREWING_BARREL.get(), RusticBrewingBarrelScreen::new);
        // The two condensers each draw their own panel (water tank fill + flame + brew bar), replacing the legacy
        // GuiCondenser / GuiCondenserAdvanced pair.
        event.register(RusticMenus.CONDENSER.get(), RusticCondenserScreen::new);
        event.register(RusticMenus.CONDENSER_ADVANCED.get(), RusticCondenserAdvancedScreen::new);
        // The apiary panel is the legacy GuiApiary: the two titles over the apiary.png sheet, no bars.
        event.register(RusticMenus.APIARY.get(), RusticApiaryScreen::new);
    }

    /**
     * 1.12.2's {@code ModEntities.initRenderers()}, whose chair line was commented out - the engine at
     * the time drew an entity with no registered handler as nothing, so a seat needed no entry.
     *
     * <p>The tomato keeps its legacy renderer in spirit: {@code RenderTomato} drew the tomato item in
     * the GROUND transform against the block atlas, which is exactly what vanilla's own snowball, egg
     * and thrown potion use - {@link ThrownItemRenderer} at the default scale, rendering the entity's
     * synced item stack, and {@code EntityTomato.getDefaultItem} answers {@code rustic:tomato}.
     *
     * <p><b>Forced deviation (chair):</b> 1.21.1 does not tolerate that omission. {@code EntityRenderDispatcher}
     * hands back whatever its map lookup returns ({@code return (EntityRenderer<? super T>)this.renderers
     * .get(entity.getType());}) and calls into it without testing it, so an unregistered type makes the
     * client throw {@code NullPointerException: ... because "entityrenderer" is null} from inside
     * {@code LevelRenderer.renderLevel} the moment the seat enters view - an "Unexpected error" crash on
     * client tick.
     *
     * <p>{@link NoopRenderer} is the legacy outcome restored rather than approximated: it overrides only
     * {@code getTextureLocation}, and the {@code EntityRenderer#render} it inherits draws nothing for an
     * entity that is not {@code Leashable}. The seat therefore stays as invisible as it was, and the
     * lookup simply stops returning null.
     */
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RusticEntities.CHAIR.get(), NoopRenderer::new);
        // The P3 NoopRenderer lesson in reverse: an entity with no renderer at all crashes the client
        // lookup, so the tomato gets the vanilla projectile renderer the moment it becomes throwable.
        event.registerEntityRenderer(RusticEntities.TOMATO.get(), ThrownItemRenderer::new);
        // Phase 9 S9.1: the fluid-tank BERs replacing the legacy TileEntitySpecialRenderer trio. Each draws the
        // tank fluid as a top surface quad; the tub and basin additionally stack their held item as flat layers.
        event.registerBlockEntityRenderer(RusticBlockEntities.CRUSHING_TUB.get(), CrushingTubRenderer::new);
        event.registerBlockEntityRenderer(RusticBlockEntities.EVAPORATING_BASIN.get(), EvaporatingBasinRenderer::new);
        event.registerBlockEntityRenderer(RusticBlockEntities.LIQUID_BARREL.get(), LiquidBarrelRenderer::new);
        // Phase 9 S9.2: the cabinet door BER. Its four body layers are baked via registerLayerDefinitions below.
        event.registerBlockEntityRenderer(RusticBlockEntities.CABINET.get(), CabinetRenderer::new);
    }

    /**
     * The four cabinet body layers (single/single-mirror/double/double-mirror) the {@code CabinetRenderer} bakes.
     * Mirror moves the door hinge to the opposite vertical edge, exactly as the legacy {@code ModelCabinet(boolean)}
     * chose its box signs; the double is the two-block-tall variant whose texture sheet is twice as tall.
     */
    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CabinetModel.SINGLE, () -> CabinetModel.createBodyLayer(false, false));
        event.registerLayerDefinition(CabinetModel.SINGLE_MIRROR, () -> CabinetModel.createBodyLayer(true, false));
        event.registerLayerDefinition(CabinetModel.DOUBLE, () -> CabinetModel.createBodyLayer(false, true));
        event.registerLayerDefinition(CabinetModel.DOUBLE_MIRROR, () -> CabinetModel.createBodyLayer(true, true));
    }

    /**
     * Phase 9 S9.4: attaches the iron-skin / fullmetal metal-plate overlay to both player renderers
     * (the regular and slim skins), replacing the legacy {@code ClientProxy.postInit} loop that reached into
     * {@code RenderManager.entityRenderMap}/{@code getSkinMap} and called {@code addLayer} on every living renderer.
     *
     * <p>Scope narrowed to players on purpose: the legacy layer picked its metal texture size from
     * {@code model.textureWidth/textureHeight}, fields 1.21 entity models no longer expose to a render layer, so only
     * the fixed 64x32 player sheet can be reproduced faithfully (see {@code IronSkinRenderPolicy}). The
     * {@code ironskin_render_entity_blacklist} is honoured here - adding {@code minecraft:player} disables the paint -
     * preserving the config's contract even though its slime/shulker defaults target renderers we no longer touch.
     */
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

    /**
     * S9.5: slots the two HUD meters into 1.21's {@code LayeredDraw} pipeline. The 1.12.2 equivalents were
     * {@code RenderGameOverlayEvent} hooks in {@code EventHandlerClient}. {@link VanillaGuiLayers#ARMOR_LEVEL}
     * already paints the first armour row, so {@link ArmorOverflowLayer} registers immediately above it and adds only
     * the rows vanilla omits; {@link VanillaGuiLayers#FOOD_LEVEL} anchors {@link ArmorToughnessLayer} on the
     * right-hand stack (vanilla draws no toughness meter at all).
     */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "armor_overflow"), new ArmorOverflowLayer());
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "armor_toughness"), new ArmorToughnessLayer());
        // Fullmetal first-person screen tint: bottom-most layer so the entire HUD (and the crosshair) still draw over it.
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_overlay"),
                new FullmetalOverlayLayer());
        // Fluid-submersion screen tint: also bottom-most. Owned by the mod (not the engine's faint renderFluid pass)
        // so it is actually visible; see FluidOverlayLayer and FLUID_RENDER_OVERLAYS.
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fluid_overlay"),
                new FluidOverlayLayer());
    }

    /**
     * Registers a still/flow/overlay {@link IClientFluidTypeExtensions} for every Rustic fluid type so
     * {@code FluidSpriteCache.getFluidSprites} never receives a null sprite location. The eight placeables plus
     * vanta oil render as blocks and so need all three sprites; the seven boozes are source-only, never place, and
     * correctly carry a null overlay (see {@code RusticFluidTextureTest} for the headless guard on these paths).
     */
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

    /** A placeable fluid: still/flow/overlay sprites under {@code block/fluids/}. */
    private static void placeableExtension(RegisterClientExtensionsEvent event, Holder<FluidType> type, String name) {
        String base = "block/fluids/" + name;
        event.registerFluidType(fluidTextures(base + "_still", base + "_flow", base + "_overlay"), type);
        // Standalone file location (assets/rustic/textures/...) that FluidOverlayLayer paints full-screen when the
        // camera is submerged in this fluid type. Keyed by the FluidType instance getEyeInFluidType() returns.
        FLUID_RENDER_OVERLAYS.put(type.value(),
                ResourceLocation.parse(Rustic.MODID + ":textures/" + base + "_overlay.png"));
    }

    /** A booze fluid: still/flow sprites under {@code block/fluids/booze/}, deliberately no overlay. */
    private static void boozeExtension(RegisterClientExtensionsEvent event, Holder<FluidType> type, String name) {
        String base = "block/fluids/booze/" + name;
        event.registerFluidType(fluidTextures(base + "_still", base + "_flow", null), type);
    }

    /** Builds the extension returning the given sprite locations; {@code overlay} may be null. */
    private static IClientFluidTypeExtensions fluidTextures(String still, String flowing, @Nullable String overlay) {
        ResourceLocation stillLoc = ResourceLocation.parse(Rustic.MODID + ":" + still);
        ResourceLocation flowingLoc = ResourceLocation.parse(Rustic.MODID + ":" + flowing);
        ResourceLocation overlayLoc = overlay == null ? null : ResourceLocation.parse(Rustic.MODID + ":" + overlay);
        // The camera-submersion screen tint is deliberately NOT routed through getRenderOverlayTexture: vanilla's
        // ScreenEffectRenderer#renderFluid composites it at a hard-coded 0.1 alpha, far too faint to read as an
        // overlay. The mod owns the effect instead - FluidOverlayLayer paints the same overlay sheet (addressed as a
        // standalone file in FLUID_RENDER_OVERLAYS) as a full-screen quad at a clearly-visible alpha. Leaving
        // getRenderOverlayTexture at its null default suppresses the engine pass so the two never double up.
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

    /**
     * Registers the same foliage-tint handler that vanilla applies to OAK_LEAVES, JUNGLE_LEAVES, etc. for the Rustic
     * leaf blocks. Without this the IdentityHashMap in BlockColors returns -1 for our non-vanilla instances
     * and the grayscale leaf textures render flat white/gray. Phase 17 S17.1 adds GRAPE_LEAVES and S17.2 adds
     * WILDBERRY_BUSH: their restored art carries tintindex (0 and 1 respectively) exactly like legacy
     * BlockGrapeLeaves.getBlockColor / BlockBerryBush.getBlockColor (the 1.12.2 mod tinted them through IColoredBlock),
     * and the handler ignores the index because each model only tints its leaf faces. Grape leaves need no item-tint
     * twin because that leaf has no block item at all; the bush does, so it appears in registerItemColors below.
     */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        // Mirrors vanilla BlockColors.create() line for OAK_LEAVES:
        // (state, level, pos, tintIndex) -> level != null && pos != null
        //         ? BiomeColors.getAverageFoliageColor(level, pos)
        //         : FoliageColor.getDefaultColor()
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

    /**
     * The companion item-tint registration. Mirrors vanilla ItemColors.create() for Blocks.OAK_LEAVES and friends:
     * the item handler delegates to the block handler with {@code level = pos = null}, which in our foliage lambda
     * falls through to {@link FoliageColor#getDefaultColor()} - the fixed green inventory icon. Without this the
     * inventory stack's tintIndex resolves to -1 (white) and the icon renders gray even though the placed block
     * tints correctly.
     */
    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            BlockState blockstate = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            return event.getBlockColors().getColor(blockstate, null, null, tintIndex);
        },
                RusticBlocks.olive().leaves().get(),
                RusticBlocks.ironwood().leaves().get(),
                RusticFarmBlocks.APPLE_LEAVES.get(),
                RusticFarmBlocks.WILDBERRY_BUSH.get());
        // Phase 17 S17.4: the elixir bottle tint, the 1.21 form of the legacy ItemElixir.getItemColor. Its
        // models/item/elixir.json paints the elixir_overlay mask at tint index 0 and the glass bottle at index 1; the
        // legacy colourMultiplier returned ElixirUtils.getColor(stack) for index 0 (the first stored effect's colour,
        // or the magenta NO_EFFECTS fallback) and white for any other index, so only the liquid mask is recoloured
        // while the bottle stays un-tinted. getTint forces opacity on both branches - 1.21 honours the alpha byte
        // the legacy renderer ignored, so a raw 0xRRGGBB would render the whole item transparent.
        event.register(ElixirUtils::getTint,
                RusticItems.ELIXIR.get());
    }

    /**
     * Phase 17 S17.5: bind each Rustic status effect to its {@code potion_icons.png} tile. The (x, y) pairs are the
     * legacy {@code PotionsRustic}/{@code PotionXxx} {@code setIconIndex} values verbatim, so the HUD / inventory
     * icon for every effect matches 1.12.2. {@link RusticMobEffectIcons#tileFor} turns a grid cell into the sheet
     * pixel; the extension then blits that 18x18 tile in both the inventory and HUD effect rows.
     */
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

    /** Registers one effect's icon extension from its legacy {@code setIconIndex(x, y)} grid cell. */
    private static void registerEffectIcon(RegisterClientExtensionsEvent event,
            DeferredHolder<MobEffect, ? extends MobEffect> effect, int iconX, int iconY) {
        int[] tile = RusticMobEffectIcons.tileFor(iconX, iconY);
        IClientMobEffectExtensions extensions = RusticMobEffectIcons.forTile(tile[0], tile[1]);
        event.registerMobEffect(extensions, effect.get());
    }

    /** The cabinet block item is drawn as the 3D cabinet body (its animated BER model), replacing the legacy
     * {@code CabinetTEISR}. {@code builtin/entity} on the item model plus this extension is what routes the inventory
     * stack through the shared {@link CabinetBlockItemRenderer}; the renderer instance is cached so it is built once. */
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                // Lazily built: Minecraft.getInstance() is only valid once the client is up, and this getter is first
                // called during item rendering, well after the extension registration itself.
                if (cabinetItemRenderer == null) {
                    cabinetItemRenderer = new CabinetBlockItemRenderer();
                }
                return cabinetItemRenderer;
            }
        }, RusticMachineBlocks.CABINET.get().asItem());
    }

    /** A single BEWLR for the cabinet block item, built on first use and reused across every inventory draw. */
    @Nullable
    private static BlockEntityWithoutLevelRenderer cabinetItemRenderer;

    /**
     * FluidType -> the standalone overlay texture {@code FluidOverlayLayer} paints full-screen when the camera is
     * submerged in that fluid. Populated during {@link #registerFluidExtensions} (client only) and read on the render
     * thread; keyed by the same {@code FluidType} instance {@code Entity#getEyeInFluidType()} hands back.
     */
    public static final Map<FluidType, ResourceLocation> FLUID_RENDER_OVERLAYS = new HashMap<>();

    private RusticClient() {
    }
}
