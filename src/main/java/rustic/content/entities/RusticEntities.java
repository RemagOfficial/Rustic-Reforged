package rustic.content.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

public final class RusticEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Rustic.MODID);

    private static final int CHAIR_TRACKING_RANGE_SECTIONS = 4;

    private static final int TOMATO_TRACKING_RANGE_SECTIONS = 4;

    public static final DeferredHolder<EntityType<?>, EntityType<EntityChair>> CHAIR =
            ENTITY_TYPES.register("chair", () -> EntityType.Builder
                    .<EntityChair>of(EntityChair::new, MobCategory.MISC)

                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(CHAIR_TRACKING_RANGE_SECTIONS)
                    .updateInterval(128)
                    .setShouldReceiveVelocityUpdates(false)

                    .noSummon()
                    .build("chair"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityTomato>> TOMATO =
            ENTITY_TYPES.register("tomato", () -> EntityType.Builder
                    .<EntityTomato>of(EntityTomato::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(TOMATO_TRACKING_RANGE_SECTIONS)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("tomato"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private RusticEntities() {
    }
}
