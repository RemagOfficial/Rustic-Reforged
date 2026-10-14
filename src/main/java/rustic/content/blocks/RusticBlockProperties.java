package rustic.content.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 1.12.2 handed every block a {@code Material} ({@code Material.ROCK}, {@code Material.WOOD},
 * {@code Material.CLAY}) and the Material quietly supplied the sound, the particle colour and which
 * tool counted as effective. 1.21 deleted {@code Material}, so the three Rustic actually used are
 * translated here once instead of at every block.
 *
 * <p>Fidelity notes, read out of the 1.12.2 bytecode (the merged jar plus the MCP name map) rather than
 * from memory:
 * <ul>
 *   <li>Explosion resistance was <em>not</em> derived from hardness in 1.12.2. {@code Block} kept a separate
 *       {@code blockResistance} field that its {@code Material} constructor never initialised, so a block
 *       which only called {@code setHardness(x)} - every block in this family - reported
 *       {@code getExplosionResistance() == 0}. {@link BlockBehaviour.Properties#strength(float)} instead
 *       sets the resistance <em>equal to</em> the hardness, which would quietly make the whole decor set
 *       blast-proof, so the zero is written out explicitly. (1.12.2's {@code setResistance(x)} stored
 *       {@code x * 3} and the getter returned a fifth of the stored value; only three blocks in the mod
 *       ever called it, all with 5F, i.e. a reported 3.0F - which is what Phase 3's planks need.)</li>
 *   <li>No block in this family set a harvest level in 1.12.2, which meant it always dropped its
 *       item, even when mined by hand. {@code requiresCorrectToolForDrops()} is therefore
 *       deliberately not set anywhere here; mining <em>speed</em> comes from the
 *       {@code minecraft:mineable/} tags instead.</li>
 *   <li>Flammability was never part of the properties either - 1.12.2 called
 *       {@code Blocks.FIRE.setFireInfo(block, 5, 20)} on each wooden block, and 1.21.1 still keeps
 *       that lookup table on the fire block itself, so it is applied after registration by
 *       {@link RusticBlocks#setFlammables()} rather than here.</li>
 * </ul>
 */
public final class RusticBlockProperties {

    private RusticBlockProperties() {
    }

    /** 1.12.2 {@code Material.ROCK}: stone sound, stone particles, pickaxe-mined. */
    public static BlockBehaviour.Properties stone(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /** 1.12.2 {@code Material.WOOD}: wood sound, wood particles, axe-mined. */
    public static BlockBehaviour.Properties wood(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * 1.12.2 {@code Material.CLAY}, whose sound Rustic overrode with {@code SoundType.GROUND} - which
     * is the same thing, because 1.12.2's GROUND constants played the gravel samples and 1.21 simply
     * calls that {@link SoundType#GRAVEL} (vanilla clay uses it too). Hardness comes from the caller:
     * 1.12.2 set 1.0F on the clay walls rather than the material's usual 0.5F.
     */
    public static BlockBehaviour.Properties clay(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.CLAY)
                .sound(SoundType.GRAVEL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * 1.12.2 {@code SoundType.CLOTH}, which 1.21 names {@link SoundType#WOOL}, plus the map colour of the
     * same name - the rope's {@code MaterialRope} was built on {@code MapColor.CLOTH} and asked for that
     * sound by hand over the material's own.
     *
     * <p>No tool is declared for it anywhere: 1.12.2's mining-speed table is keyed on the vanilla
     * {@code Material} constants and a custom subclass matched none of them, so rope was mined at hand
     * speed by everything. See {@link RusticBlocks#DECOR_FAMILIES} for the family that carries the null.
     */
    public static BlockBehaviour.Properties cloth(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .sound(SoundType.WOOL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * 1.12.2 {@code Material.GROUND} at {@code SoundType.GROUND}, which is 1.21's
     * {@link SoundType#GRAVEL} again - the same rename the clay walls went through, and the reason dirt,
     * farmland and the fertile soil all play gravel samples.
     */
    public static BlockBehaviour.Properties dirt(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIRT)
                .sound(SoundType.GRAVEL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * What a 1.12.2 block that never called {@code setResistance} reported to the explosion code. Named
     * rather than inlined because it looks like an omission, and the next phase's wooden blocks really
     * do have a non-zero value.
     */
    private static final float NO_EXPLOSION_RESISTANCE = 0.0F;

    /**
     * 1.12.2 {@code Material.IRON}, which is what the chains, lanterns, chandeliers and the iron torch
     * were made of, and which Rustic then gave {@code SoundType.METAL} or {@code SoundType.ANVIL} by
     * hand anyway. The map colour is the one 1.21's own iron blocks use: the legacy {@code Material}
     * constant's colour is not readable from the mod's source and only decides the minimap tint, so it is
     * listed in the baseline notes rather than guessed at here.
     */
    public static BlockBehaviour.Properties metal(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /** 1.12.2's chandelier, which overrode the iron sound with {@code SoundType.ANVIL}. */
    public static BlockBehaviour.Properties anvil(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.ANVIL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * 1.12.2 {@code Material.CIRCUITS} - the material the candles and the iron torch were built from -
     * with the {@code SoundType.METAL} Rustic put on all of them. Nothing else in the mod used CIRCUITS.
     */
    public static BlockBehaviour.Properties circuits(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(hardness, NO_EXPLOSION_RESISTANCE);
    }

    /**
     * 1.12.2 {@code Material.WOOD} at the hardness the planks used, but with the explosion resistance
     * that {@code BlockPlanksRustic.setResistance(5F)} reported - the first non-zero resistance in the
     * port, and the reason {@link #NO_EXPLOSION_RESISTANCE} is spelled out everywhere else.
     */
    public static BlockBehaviour.Properties woodResistant(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(hardness, RESISTANT_WOOD);
    }

    /** The 1.12.2 value of {@code setResistance(5F)}: the setter stored {@code 5 * 3}, the getter of a
     * block that was queried through {@code getExplosionResistance} returned a fifth of it. */
    private static final float RESISTANT_WOOD = 3.0F;

    /**
     * 1.12.2's {@code setLightLevel(1.0F)}, which every lit decor block in this slice called. The float
     * was a 0..1 fraction of full brightness; 1.21 stores the 0..15 level directly, so 1.0 is 15.
     */
    public static BlockBehaviour.Properties lit(BlockBehaviour.Properties properties) {
        return properties.lightLevel(state -> FULL_BRIGHTNESS);
    }

    /** {@code Block#LIGHT_MAX} is not public, so the number the legacy {@code 1.0F} meant is written out. */
    private static final int FULL_BRIGHTNESS = 15;

    /**
     * The 1.21 way to build a derived block (stairs, slab) from its parent: inherit everything the
     * parent declares instead of restating it, exactly like vanilla does for its own stone stairs.
     *
     * <p>This is also the faithful choice for 1.12.2, whose {@code BlockStairs(IBlockState)} constructor
     * copied the source block's {@code blockHardness} straight over and its raw {@code blockResistance}
     * verbatim (it divided the field by 3 before handing it to a setter that multiplied by 3 again).
     * {@code ofFullCopy} copies both numbers, so a slate stair inherits hardness 2.0F and resistance 0.0F
     * just as the legacy one did.
     */
    public static BlockBehaviour.Properties ofBase(Block base) {
        return BlockBehaviour.Properties.ofFullCopy(base);
    }
}
