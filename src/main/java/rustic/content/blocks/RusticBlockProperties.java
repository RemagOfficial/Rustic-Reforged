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
     * What a 1.12.2 block that never called {@code setResistance} reported to the explosion code. Named
     * rather than inlined because it looks like an omission, and the next phase's wooden blocks really
     * do have a non-zero value.
     */
    private static final float NO_EXPLOSION_RESISTANCE = 0.0F;

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
