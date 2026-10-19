package rustic.content.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemOlives extends Item {

    private static final int EAT_DURATION_TICKS = 24;

    public ItemOlives(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }
}
