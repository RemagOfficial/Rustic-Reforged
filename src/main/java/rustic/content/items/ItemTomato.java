package rustic.content.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rustic.content.entities.EntityTomato;

/**
 * 1.12.2's {@code ModItems.TOMATO}: the edible tomato that turns into a projectile when it is thrown.
 *
 * <p>The legacy item was a plain {@code ItemFoodBase("tomato", 4, 0.4F, false)} - the food half is data
 * on the properties, see {@code RusticItems} - plus one {@code onItemRightClick} override: sneaking, it
 * threw; not sneaking, {@code super} ate. {@link Item#use} is the 1.21 form of the same method and its
 * base implementation is the same eat decision (food properties, {@code canEat}, start using), so the
 * override answers only the sneaking branch and hands the rest to {@code super.use}.
 *
 * <p>Faithful legacy numbers: one tomato consumed outside creative, the snowball-throw sound at volume
 * 0.5 and pitch {@code 0.4 / (rand * 0.4 + 0.8)} played on both sides, motion from
 * {@code shoot(player, pitch, yaw, 0.0F, 1.5F, 1.0F)} - 1.12.2's {@code EntityThrowable.shoot} is
 * {@code Projectile.shootFromRotation} with the same argument order - the object-use statistic, and the
 * entity spawned server-side only.
 *
 * <p>Deviation: {@code StatList.getObjectUseStats(this)} is {@code Stats.ITEM_USED.get(this)} in 1.21,
 * and the legacy item's own {@code itemRand} becomes the level's random source - the same job, a
 * per-use throw of the dice, without a {@code Random} field on {@code Item}.
 */
public class ItemTomato extends Item {

    public ItemTomato(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // The legacy isSneaking() gate: a sneak-right-click throws, anything else eats as normal.
        if (!player.isCrouching()) {
            return super.use(level, player, hand);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        // The legacy one-call sound, verbatim in shape: Level.playSound with a null player reaches
        // everyone in earshot - on a dedicated server through the nearby-player broadcast, in
        // singleplayer straight from the local level - and because the use() body runs only on the
        // server there (the client half of a right-click is a separate call), nobody hears it twice.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            EntityTomato tomato = new EntityTomato(level, player);
            tomato.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(tomato);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
