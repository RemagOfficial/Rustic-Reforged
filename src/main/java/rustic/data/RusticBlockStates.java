package rustic.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;

/**
 * Blockstates and block models, replacing the hand-written JSON that 1.12.2 shipped under
 * {@code assets/rustic/blockstates} and {@code assets/rustic/models}.
 *
 * <p>The legacy assets were written for the 1.8-era model format: every blockstate used
 * {@code "variants": {"normal": ...}} (1.21 uses the empty key), texture references pointed at
 * {@code rustic:blocks/<name>} (1.21's flattened texture folders renamed that directory to
 * {@code block}), and stairs and slabs referenced {@code block/stairs}, {@code block/half_slab} and
 * friends. Generating them means the ids, the property names and the parent models are checked
 * against the real registry instead of against memory, which matters for a port where every family is
 * repeated sixteen times in one colour.
 *
 * <p>Item models come from the same provider: 1.12.2 needed
 * {@code ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(id, "inventory"))}
 * per block, while 1.21 simply parents {@code item/<name>} on {@code block/<name>}.
 */
public class RusticBlockStates extends BlockStateProvider {

    public RusticBlockStates(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Rustic.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        pillars();
        slate();
        clayWalls();
        paintedWood();
    }

    // ------------------------------------------------------------------
    // Stone pillars
    // ------------------------------------------------------------------
    private void pillars() {
        // 1.12.2 side/end textures kept as they were; the three vanilla ends are the 1.12
        // "stone_*_smooth" textures, renamed to polished_* when 1.17 flattened the texture folder.
        pillar(RusticBlocks.STONE_PILLAR, tex("pillar_stone_side"), tex("pillar_stone_top"));
        pillar(RusticBlocks.ANDESITE_PILLAR, tex("pillar_andesite_side"), vanillaTex("polished_andesite"));
        pillar(RusticBlocks.DIORITE_PILLAR, tex("pillar_diorite_side"), vanillaTex("polished_diorite"));
        pillar(RusticBlocks.GRANITE_PILLAR, tex("pillar_granite_side"), vanillaTex("polished_granite"));
        pillar(RusticBlocks.SLATE_PILLAR, tex("slate_pillar"), tex("slate_tile"));
    }

    /**
     * The same thing {@link #axisBlock(RotatedPillarBlock, ResourceLocation, ResourceLocation)} does,
     * written out only so the vertical model can be reused for the item without generating it twice.
     */
    private void pillar(DeferredBlock<? extends RotatedPillarBlock> holder, ResourceLocation side, ResourceLocation end) {
        String name = path(holder);
        ModelFile vertical = models().cubeColumn(name, side, end);
        ModelFile horizontal = models().cubeColumnHorizontal(name + "_horizontal", side, end);
        axisBlock(holder.get(), vertical, horizontal);
        simpleBlockItem(holder.get(), vertical);
    }

    // ------------------------------------------------------------------
    // Slate
    // ------------------------------------------------------------------
    private void slate() {
        cubeWithItem(RusticBlocks.SLATE);
        cubeWithItem(RusticBlocks.SLATE_ROOF);
        cubeWithItem(RusticBlocks.SLATE_TILE);
        cubeWithItem(RusticBlocks.SLATE_BRICK);
        cubeWithItem(RusticBlocks.SLATE_CHISELED);

        // 1.12.2 texture: rustic:blocks/slate_roof for all three stair faces
        stairs(RusticBlocks.SLATE_ROOF_STAIRS, tex(RusticBlocks.SLATE_ROOF));
        stairs(RusticBlocks.SLATE_BRICK_STAIRS, tex(RusticBlocks.SLATE_BRICK));
        slab(RusticBlocks.SLATE_ROOF_SLAB, tex(RusticBlocks.SLATE_ROOF));
        slab(RusticBlocks.SLATE_BRICK_SLAB, tex(RusticBlocks.SLATE_BRICK));
    }

    /** A block that is one texture on all six sides, plus the item that shows it. */
    private void cubeWithItem(DeferredBlock<? extends Block> holder) {
        simpleBlockWithItem(holder.get(), cubeAll(holder.get()));
    }

    private void stairs(DeferredBlock<? extends StairBlock> holder, ResourceLocation texture) {
        String name = path(holder);
        ModelFile stairs = models().stairs(name, texture, texture, texture);
        ModelFile stairsInner = models().stairsInner(name + "_inner", texture, texture, texture);
        ModelFile stairsOuter = models().stairsOuter(name + "_outer", texture, texture, texture);
        stairsBlock(holder.get(), stairs, stairsInner, stairsOuter);
        simpleBlockItem(holder.get(), stairs);
    }

    /**
     * One slab block covers bottom, top and double in 1.21, so three models replace the legacy
     * {@code <name>_slab_bottom}/{@code _top}/{@code _double} trio. The {@code *_doubleslab} blocks
     * 1.12.2 registered alongside them have no state of their own any more.
     */
    private void slab(DeferredBlock<? extends SlabBlock> holder, ResourceLocation texture) {
        String name = path(holder);
        ModelFile bottom = models().slab(name, texture, texture, texture);
        ModelFile top = models().slabTop(name + "_top", texture, texture, texture);
        ModelFile doubleSlab = models().cubeAll(name + "_double", texture);
        slabBlock(holder.get(), bottom, top, doubleSlab);
        simpleBlockItem(holder.get(), bottom);
    }

    // ------------------------------------------------------------------
    // Clay walls
    // ------------------------------------------------------------------
    private void clayWalls() {
        cubeWithItem(RusticBlocks.CLAY_WALL);
        cubeWithItem(RusticBlocks.CLAY_WALL_CROSS);

        /*
         * 1.12.2 drew this one by hand in MrCrayfish's model creator, and the file that tool exported
         * is not reproducible with a plain "cube" parent: the legacy west face carries uv [16,0,0,16]
         * rather than [0,0,16,16], which makes the east and west faces run the diagonal the same way in
         * world space. Vanilla's cube parent instead bakes every side face upright-as-seen-from-outside,
         * so its u axis reverses between east and west and the two diagonals would face each other like
         * a mirror. Hence the explicit element below. Only the cull faces are new; the legacy file had
         * none, and they merely let a neighbouring block skip a face that could never be seen.
         *
         * The rotation mapping still has to be reproduced: the legacy blockstate faced north at y=0,
         * east at 90, south at 180 and west at 270, while Direction#toYRot answers south=0, west=90,
         * north=180, east=270 - a constant 180 degree offset.
         */
        ResourceLocation wall = tex("clay_wall");
        ResourceLocation diag = tex("clay_wall_diag");
        String name = path(RusticBlocks.CLAY_WALL_DIAG);
        ModelFile model = diagonalCube(name, wall, diag);
        horizontalBlock(RusticBlocks.CLAY_WALL_DIAG.get(), model, 180);
        simpleBlockItem(RusticBlocks.CLAY_WALL_DIAG.get(), model);
    }

    /**
     * A full cube in the 1.12.2 shape: one texture on north, south, up and down, and a second one on
     * east and west laid out so the two faces agree in world space instead of mirroring each other.
     */
    private ModelFile diagonalCube(String name, ResourceLocation plain, ResourceLocation diagonal) {
        return models().getBuilder(name)
                .texture("plain", plain)
                .texture("diagonal", diagonal)
                // The legacy model used the diagonal texture for its break particles.
                .texture("particle", diagonal)
                .element()
                .from(0, 0, 0).to(16, 16, 16)
                .face(Direction.DOWN).texture("#plain").uvs(0, 0, 16, 16).cullface(Direction.DOWN).end()
                .face(Direction.UP).texture("#plain").uvs(0, 0, 16, 16).cullface(Direction.UP).end()
                .face(Direction.NORTH).texture("#plain").uvs(0, 0, 16, 16).cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).texture("#plain").uvs(0, 0, 16, 16).cullface(Direction.SOUTH).end()
                .face(Direction.EAST).texture("#diagonal").uvs(0, 0, 16, 16).cullface(Direction.EAST).end()
                // u descending as the face's own axis ascends is the horizontal flip described above.
                .face(Direction.WEST).texture("#diagonal").uvs(16, 0, 0, 16).cullface(Direction.WEST).end()
                .end();
    }

    // ------------------------------------------------------------------
    // Painted wood
    // ------------------------------------------------------------------
    private void paintedWood() {
        for (DyeColor color : DyeColor.values()) {
            cubeWithItem(RusticBlocks.PAINTED_WOOD.get(color));
        }
    }

    // ------------------------------------------------------------------
    // Naming helpers. BlockStateProvider's own name(Block)/key(Block) are private, and the blocks
    // are not bound to the registry yet at class-initialisation time, so the path comes from the
    // deferred holder instead.
    // ------------------------------------------------------------------
    private static String path(DeferredBlock<? extends Block> holder) {
        return holder.getId().getPath();
    }

    /** A Rustic block texture, in the 1.21 {@code block/} folder. */
    private ResourceLocation tex(String name) {
        return modLoc("block/" + name);
    }

    private ResourceLocation tex(DeferredBlock<? extends Block> holder) {
        return tex(path(holder));
    }

    /** A vanilla block texture, by its 1.21 name. */
    private ResourceLocation vanillaTex(String name) {
        return mcLoc("block/" + name);
    }
}
