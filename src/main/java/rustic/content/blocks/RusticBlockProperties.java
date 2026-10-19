package rustic.content.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class RusticBlockProperties {

    private RusticBlockProperties() {
    }

    public static BlockBehaviour.Properties stone(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties wood(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties clay(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.CLAY)
                .sound(SoundType.GRAVEL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties cloth(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .sound(SoundType.WOOL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties dirt(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIRT)
                .sound(SoundType.GRAVEL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    private static final float NO_EXPLOSION_RESISTANCE = 0.0F;

    public static BlockBehaviour.Properties metal(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties anvil(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.ANVIL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties circuits(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    public static BlockBehaviour.Properties crop() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .sound(SoundType.CROP)
                .strength(0.0F, NO_EXPLOSION_RESISTANCE)
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    public static BlockBehaviour.Properties stakeCrop() {
        return crop().forceSolidOff();
    }

    public static BlockBehaviour.Properties stakePole(float hardness) {
        return woodResistant(hardness).forceSolidOff();
    }

    public static BlockBehaviour.Properties woodResistant(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(hardness, RESISTANT_WOOD);
    }

    private static final float RESISTANT_WOOD = 3.0F;

    public static BlockBehaviour.Properties lit(BlockBehaviour.Properties properties) {
        return properties.lightLevel(state -> FULL_BRIGHTNESS);
    }

    private static final int FULL_BRIGHTNESS = 15;

    public static BlockBehaviour.Properties ofBase(Block base) {
        return BlockBehaviour.Properties.ofFullCopy(base);
    }
}
