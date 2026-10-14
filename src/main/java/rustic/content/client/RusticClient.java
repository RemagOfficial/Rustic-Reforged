package rustic.content.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rustic.content.entities.RusticEntities;

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
