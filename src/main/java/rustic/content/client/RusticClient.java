package rustic.content.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import rustic.Rustic;
import rustic.content.client.screen.RusticChestScreen;
import rustic.content.entities.RusticEntities;
import rustic.content.fluids.RusticFluids;
import rustic.content.menu.RusticMenus;
import rustic.content.network.RusticPayloads;
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
        modEventBus.addListener(RusticClient::registerScreens);
        // Without a client extension every Rustic FluidType answers IClientFluidTypeExtensions.DEFAULT, whose
        // getStillTexture() returns null; FluidSpriteCache then does Map.of().getOrDefault(null, ...) and the render
        // thread throws "Cannot invoke Object.hashCode() because pk is null" the moment a placed fluid is in view,
        // crashing every subsequent world load too. Supplying each fluid's sprites here is the 1.21 replacement for
        // the deprecated FluidType#initializeClient and must run on the mod bus, client side only.
        modEventBus.addListener(RusticClient::registerFluidExtensions);
        // Phase 6 fire power: the client's left-clicks into empty space and onto blocks are not visible to the
        // server, so they ping RusticPayloads.FirePowerAttackPayload back. Registered on the game bus because these
        // are NeoForge gameplay events; this method only runs on the client dist, so a server never sees them.
        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickEmpty);
        NeoForge.EVENT_BUS.addListener(RusticClient::onLeftClickBlock);
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

    private RusticClient() {
    }
}
