package rustic.content.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

/**
 * The entities 1.12.2 registered in {@code ModEntities.init()}: the chair seat and, from Phase 6, the
 * thrown tomato.
 *
 * <p>The legacy call was {@code EntityRegistry.registerModEntity(id, class, "rustic.chair", index,
 * instance, trackingRange, updateInterval, sendsVelocityUpdates)}; the id and the network numbers are
 * the parts that survive, and the integer index disappears because 1.21 keys entities by their
 * registry name alone. The classification argument the modern builder demands is the one Forge
 * assigned to anything that is not a mob: {@link MobCategory#MISC}.
 *
 * <p><b>Unit change:</b> {@code registerModEntity}'s tracking range was measured in blocks, while
 * {@link EntityType.Builder#clientTrackingRange(int)} counts 16 block sections - so the legacy 64
 * becomes 4 here rather than 64. The value only has to cover a player who is close enough to click the
 * chair, which is roughly two blocks (the legacy distance test is the same in this port), so four
 * sections is comfortable, and it is what vanilla gives its size-less MISC entities - {@code ARROW} is
 * {@code sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)}, and the projectiles beside it read
 * the same way. The update interval and the velocity flag are copied verbatim.
 *
 * <p>The tomato of {@code ModEntities.init()} arrives with Phase 6's thrown produce (its SHAME effect is a
 * Phase 6 potion); the chair's numbers and its own are recorded on the two holders below.
 * {@code initRenderers()} is
 * ported as {@link rustic.content.client.RusticClient}, which is not inert here the way it was in 1.12.2:
 * the legacy build got away with giving the chair no renderer because Forge 1.12.2 silently skipped an
 * entity whose type had no registered handler, whereas 1.21.1 calls into the lookup result and throws
 * {@code NullPointerException} on the render thread - so the seat does need its no-op renderer, and has
 * one. See that class for the reasoning and {@code PORT_BASELINE.md} for the record.
 */
public final class RusticEntities {

    /** Owned here rather than by {@code RusticRegistries}, exactly like blocks and items are. */
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Rustic.MODID);

    /** 1.12.2's 64 block tracking range, in the 16 block sections 1.21 counts. */
    private static final int CHAIR_TRACKING_RANGE_SECTIONS = 4;

    /** Same legacy unit swap for the tomato's 64 block tracking range. */
    private static final int TOMATO_TRACKING_RANGE_SECTIONS = 4;

    public static final DeferredHolder<EntityType<?>, EntityType<EntityChair>> CHAIR =
            ENTITY_TYPES.register("chair", () -> EntityType.Builder
                    .<EntityChair>of(EntityChair::new, MobCategory.MISC)
                    // 1.12.2's setSize(0F, 0F) in the constructor; the size is part of the type now.
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(CHAIR_TRACKING_RANGE_SECTIONS)
                    .updateInterval(128)
                    .setShouldReceiveVelocityUpdates(false)
                    // A seat is made by clicking a chair block and nothing else. /summon would put one in
                    // empty air, where the life cycle test in EntityChair#tick removes it a tick later -
                    // the outcome 1.12.2 reached by the same route, without the wasted command.
                    .noSummon()
                    .build("chair"));

    /**
     * 1.12.2's {@code ModEntities.init()} line for the thrown tomato, first entity and id 0 - the id is
     * gone with the integer index, {@code rustic:tomato} is the whole key. Legacy network numbers copied:
     * the 64 block tracking range in its 1.21 sections unit (vanilla gives its snowballs the same 4),
     * update interval 1 and velocity updates {@code true}, the pair every throwable projectile needed to
     * show its arc on clients.
     *
     * <p>The 0.25 x 0.25 size is {@code EntityThrowable}'s own hitbox in 1.12.2, which is also exactly
     * what vanilla declares for its snowball, egg and thrown potion beside it.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTomato>> TOMATO =
            ENTITY_TYPES.register("tomato", () -> EntityType.Builder
                    .<EntityTomato>of(EntityTomato::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(TOMATO_TRACKING_RANGE_SECTIONS)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("tomato"));

    /** Attaches the entity register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private RusticEntities() {
    }
}
