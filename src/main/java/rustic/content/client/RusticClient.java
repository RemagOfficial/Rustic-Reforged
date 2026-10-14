package rustic.content.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import rustic.content.client.screen.RusticChestScreen;
import rustic.content.entities.RusticEntities;
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
     * <p><b>Forced deviation:</b> 1.21.1 does not tolerate that omission. {@code EntityRenderDispatcher}
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
    }

    private RusticClient() {
    }
}
