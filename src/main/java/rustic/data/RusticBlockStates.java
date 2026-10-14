package rustic.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import rustic.Rustic;
import rustic.content.blocks.BlockCandle;
import rustic.content.blocks.BlockCandleBase;
import rustic.content.blocks.BlockCandleDouble;
import rustic.content.blocks.BlockCandleDouble.CandleOrientation;
import rustic.content.blocks.BlockCandleLever;
import rustic.content.blocks.BlockChain;
import rustic.content.blocks.BlockChandelier;
import rustic.content.blocks.BlockLattice;
import rustic.content.blocks.BlockLantern;
import rustic.content.blocks.BlockRopeBase;
import rustic.content.blocks.BlockTable;
import rustic.content.blocks.BlockWoodLantern;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticMachineBlocks;

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
 *
 * <p>The mounted metal decor of {@link #decor()} is the exception to generating the geometry: a chandelier
 * is two hundred lines of elements and a gargoyle almost four hundred, and the plan keeps the conversion of
 * that 1.8-era art as its own phase, so those models stay static assets under {@code models/block} - with
 * their texture paths renamed to {@code block/} and a {@code "render_type": "cutout"} added to every
 * parent-less root, which is how NeoForge now expresses the {@code CUTOUT} layer 1.12.2 set in code. Only
 * the state files and the item parents are generated here, and they are what check the ids, the property
 * names and the rotations against the live registry.
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
        woodFamily();
        decor();
        woodLantern();
        furniture();
        rope();
        lattice();
        fertileSoil();
        machines();
    }

    // ------------------------------------------------------------------
    // Phase 4 storage machines.
    //
    // The barrel block was a CUTOUT custom model in 1.12.2 (BlockBarrelRustic's see-through top), but the
    // port hands it an opaque cube so it can be registered before the baked-model rework in Phase 9; the
    // orientation of the legacy model is not part of the behaviour contract. Generating a plain cube keeps
    // runData from failing on a multi-element model this phase has no reason to reconstruct yet.
    //
    // The cabinet is the same deferral: its two animated doors were drawn by a CabinetRenderer over a
    // custom model (textures/models/cabinet*.png), which is Phase 9 art. Its facing/mirror/top properties
    // drive behaviour (placement and the 54-slot double), not this placeholder, so every property
    // combination is mapped to one static cube - forAllStates rather than a single default variant, because
    // the block does declare properties and each state still has to resolve to a model.
    //
    // The vase follows the cabinet again: it declares a scaffolded VARIANT(0-12) property whose thirteen real
    // models are deferred to Phase 9, so all thirteen states resolve to one cube. There is no single vase.png
    // in the legacy art - the styles were vase_side_N / _top_N / _bottom_N - so the placeholder borrows the
    // brown-ceramic side texture (vase_side_0), which exists and still reads as a vase.
    // ------------------------------------------------------------------
    private void machines() {
        simpleBlockWithItem(RusticMachineBlocks.BARREL.get(),
                models().cubeAll(path(RusticMachineBlocks.BARREL), tex("barrel")));

        ModelFile cabinet = models().cubeAll(path(RusticMachineBlocks.CABINET), tex("cabinet"));
        getVariantBuilder(RusticMachineBlocks.CABINET.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(cabinet).build());
        simpleBlockItem(RusticMachineBlocks.CABINET.get(), cabinet);

        ModelFile vase = models().cubeAll(path(RusticMachineBlocks.VASE), tex("vase_side_0"));
        getVariantBuilder(RusticMachineBlocks.VASE.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(vase).build());
        simpleBlockItem(RusticMachineBlocks.VASE.get(), vase);
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
    // Olive and ironwood
    //
    // The wood family is the group whose legacy assets were already vanilla-shaped: every model parented a
    // vanilla template and only swapped in the Rustic texture, which is what these helpers do too, so the
    // generated files differ from the 1.12 ones by the folder rename and nothing else. Two names are new:
    // the log's horizontal model (1.12.2 rotated one cube_column model per axis state instead) and the
    // gate's four models, which follow vanilla's <wood>_fence_gate[_open|_wall|_wall_open] rather than the
    // legacy _closed / wall_gate_closed set - the geometry is the same template either way.
    //
    // Only the fence texture has to be spelled out: 1.12.2 drew the fence, the gate, the slab and the
    // stairs from the planks texture, not from a texture named after each block.
    // ------------------------------------------------------------------
    private void woodFamily() {
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            String name = wood.name();
            ResourceLocation planks = tex("planks_" + name);

            cubeWithItem(wood.planks());
            pillar(wood.log(), tex("log_" + name), tex("log_" + name + "_top"));

            /*
             * Leaves take the vanilla block/leaves template, which is a plain cube whose faces carry
             * tintindex 0 - the hook the biome foliage colour is applied through, and the reason the
             * textures here are grayscale. 1.21.1 answers the render layer for a LeavesBlock by itself
             * (cutout with mipmaps on fancy graphics), so unlike the metal decor no render_type is written.
             * The tint handler that reads the colour off the texture is client-side work.
             */
            ModelFile leaves = models().leaves(path(wood.leaves()), tex("leaves_" + name));
            simpleBlockWithItem(wood.leaves().get(), leaves);

            // The sapling is a cross and needs the cutout named out loud; its item is the flat sprite
            // 1.12.2 gave it, which is also what vanilla's own sapling items use.
            ModelFile sapling = models().cross(path(wood.sapling()), tex("sapling_" + name))
                    .renderType("cutout");
            getVariantBuilder(wood.sapling().get())
                    .forAllStates(state -> ConfiguredModel.builder().modelFile(sapling).build());
            itemModels().withExistingParent(path(wood.sapling()), mcLoc("item/generated"))
                    .texture("layer0", tex("sapling_" + name));

            fenceBlock(wood.fence().get(), name, planks);
            simpleBlockItem(wood.fence().get(), models().fenceInventory(name + "_fence_inventory", planks));

            ModelFile gate = models().fenceGate(name + "_fence_gate", planks);
            ModelFile gateOpen = models().fenceGateOpen(name + "_fence_gate_open", planks);
            ModelFile gateWall = models().fenceGateWall(name + "_fence_gate_wall", planks);
            ModelFile gateWallOpen = models().fenceGateWallOpen(name + "_fence_gate_wall_open", planks);
            fenceGateBlock(wood.fenceGate().get(), gate, gateOpen, gateWall, gateWallOpen);
            simpleBlockItem(wood.fenceGate().get(), gate);

            slab(wood.slab(), planks);
            stairs(wood.stairs(), planks);
        }
    }

    // ------------------------------------------------------------------
    // Mounted metal decor: chain, candles, chandeliers, iron torch, lanterns, gargoyle
    // ------------------------------------------------------------------

    /**
     * The wall-hung metal decor 1.12.2 registered in iron, then in gold, then - behind a config flag - in
     * silver. The suffix is the model-name suffix the legacy asset files used, because that is the only
     * difference between the sets: each one is the same five blocks with the same five blockstates.
     */
    private record MetalDecor(
            String suffix,
            DeferredBlock<BlockChain> chain,
            DeferredBlock<BlockCandle> candle,
            DeferredBlock<BlockCandleDouble> candleDouble,
            DeferredBlock<BlockCandleLever> candleLever,
            DeferredBlock<BlockChandelier> chandelier
    ) {
    }

    private void decor() {
        metalSet(new MetalDecor("", RusticBlocks.CHAIN, RusticBlocks.CANDLE, RusticBlocks.CANDLE_DOUBLE,
                RusticBlocks.CANDLE_LEVER, RusticBlocks.CHANDELIER));
        metalSet(new MetalDecor("_gold", RusticBlocks.CHAIN_GOLD, RusticBlocks.CANDLE_GOLD,
                RusticBlocks.CANDLE_DOUBLE_GOLD, RusticBlocks.CANDLE_LEVER_GOLD, RusticBlocks.CHANDELIER_GOLD));
        lantern(RusticBlocks.IRON_LANTERN, "");
        lantern(RusticBlocks.GOLDEN_LANTERN, "_gold");

        ironTorch();
        gargoyle();

        // The silver family is the one group that may not be registered at all, so its assets follow it.
        if (RusticBlocks.silverDecorEnabled()) {
            RusticBlocks.SilverDecor silver = RusticBlocks.SILVER_DECOR;
            metalSet(new MetalDecor("_silver", silver.chain(), silver.candle(), silver.candleDouble(),
                    silver.candleLever(), silver.chandelier()));
            lantern(silver.lantern(), "_silver");
        }
    }

    private void metalSet(MetalDecor set) {
        chain(set.chain(), set.suffix());
        candle(set.candle(), set.suffix());
        candleDouble(set.candleDouble(), set.suffix());
        candleLever(set.candleLever(), set.suffix());
        chandelier(set.chandelier(), set.suffix());
    }

    /*
     * 1.12.2's multipart, part for part. It is a multipart rather than six variants because a dangling
     * chain is the body plus an overlay: the last two parts are applied on top of the first three, which is
     * why the fourth repeats {@code axis=x} instead of narrowing it. "dangle" is the flag the block sets
     * when it hangs from something rather than being bolted through it.
     */
    private void chain(DeferredBlock<BlockChain> holder, String suffix) {
        ModelFile vertical = existing("chain" + suffix);
        ModelFile horizontal = existing("chain_horizontal" + suffix);
        ModelFile dangle = existing("chain_dangle" + suffix);
        getMultipartBuilder(holder.get())
                .part().modelFile(horizontal).rotationY(90).addModel()
                    .condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(vertical).addModel()
                    .condition(BlockRopeBase.AXIS, Direction.Axis.Y).end()
                .part().modelFile(horizontal).addModel()
                    .condition(BlockRopeBase.AXIS, Direction.Axis.Z).end()
                .part().modelFile(dangle).rotationY(90).addModel()
                    .condition(BlockRopeBase.AXIS, Direction.Axis.X)
                    .condition(BlockRopeBase.DANGLE, true).end()
                .part().modelFile(dangle).addModel()
                    .condition(BlockRopeBase.AXIS, Direction.Axis.Y, Direction.Axis.Z)
                    .condition(BlockRopeBase.DANGLE, true).end();

        // The legacy item is a flat sprite rather than the block: a length of chain drawn crosswise in the
        // inventory, which is the one decor item of this group that was never a 3-D model.
        itemModels().withExistingParent(path(holder), mcLoc("item/generated"))
                .texture("layer0", tex("chain" + suffix));
    }

    private void candle(DeferredBlock<BlockCandle> holder, String suffix) {
        wallOrFloor(getVariantBuilder(holder.get()), BlockCandleBase.FACING,
                existing("candle_wall" + suffix), existing("candle" + suffix), WALL_ANGLE_NORTH);
        simpleBlockItem(holder.get(), existing("candle" + suffix));
    }

    /*
     * The candelabra is not a subclass of the candle: 1.12.2 gave it its own orientation enum, because a
     * pair of candles standing on a floor has two forms - the holder bar along X or along Z - where one
     * would be enough for a single candle. The legacy blockstate names them up_x and up_z, and they are the
     * same model a quarter turn apart: the art is drawn with the bar along Z, so up_x is the one that turns.
     */
    private void candleDouble(DeferredBlock<BlockCandleDouble> holder, String suffix) {
        ModelFile wall = existing("candle_double_wall" + suffix);
        ModelFile standing = existing("candle_double" + suffix);
        VariantBlockStateBuilder builder = getVariantBuilder(holder.get());
        for (CandleOrientation orientation : BlockCandleDouble.FACING.getPossibleValues()) {
            int rotation = switch (orientation) {
                case UP_X -> 90;
                case UP_Z -> 0;
                default -> wallY(orientation.getFacing(), WALL_ANGLE_NORTH);
            };
            builder.partialState().with(BlockCandleDouble.FACING, orientation).modelForState()
                    .modelFile(orientation.isUp() ? standing : wall)
                    .rotationY(rotation)
                    .addModel();
        }
        simpleBlockItem(holder.get(), standing);
    }

    /**
     * The candle lever, which is a candle on a switch: 1.12.2 drew the pulled handle as four more models
     * rather than as a rotation, so the legacy file is the candle's five variants twice over - and the item
     * shows the unpulled candle, as it did there.
     */
    private void candleLever(DeferredBlock<BlockCandleLever> holder, String suffix) {
        VariantBlockStateBuilder builder = getVariantBuilder(holder.get());
        for (boolean powered : new boolean[]{false, true}) {
            ModelFile wall = existing((powered ? "candle_wall_pulled" : "candle_wall") + suffix);
            ModelFile standing = existing((powered ? "candle_pulled" : "candle") + suffix);
            for (Direction facing : BlockCandleBase.FACING.getPossibleValues()) {
                boolean upright = facing == Direction.UP;
                builder.partialState()
                        .with(BlockCandleBase.FACING, facing)
                        .with(BlockCandleLever.POWERED, powered)
                        .modelForState()
                        .modelFile(upright ? standing : wall)
                        .rotationY(upright ? 0 : wallY(facing, WALL_ANGLE_NORTH))
                        .addModel();
            }
        }
        simpleBlockItem(holder.get(), existing("candle" + suffix));
    }

    private void chandelier(DeferredBlock<BlockChandelier> holder, String suffix) {
        // One state and one model: the frame is hung by whatever is above it, so it has nothing to turn.
        simpleBlockWithItem(holder.get(), existing("chandelier" + suffix));
    }

    /*
     * The iron torch shares the candle's five states but not the candle's angles: its legacy wall model is
     * authored as if clipped to an east wall instead of a north one, which is the whole reason the offset is
     * a quarter turn smaller. The standing model is called _alt because 1.12.2 also shipped an unused
     * iron_torch model, a first draft of the same torch that nothing references.
     */
    private void ironTorch() {
        wallOrFloor(getVariantBuilder(RusticBlocks.IRON_TORCH.get()), BlockCandleBase.FACING,
                existing("iron_torch_wall"), existing("iron_torch_alt"), WALL_ANGLE_EAST);
        simpleBlockItem(RusticBlocks.IRON_TORCH.get(), existing("iron_torch_alt"));
    }

    /*
     * The lantern is the one mount of this group that can also hang from a ceiling, so its property is the
     * full six-way one. Up and down are separate models rather than a rotation of the wall model because the
     * glass and the iron cap are not symmetric front to back.
     */
    private void lantern(DeferredBlock<BlockLantern> holder, String suffix) {
        ModelFile wall = existing("lantern_wall" + suffix);
        VariantBlockStateBuilder builder = getVariantBuilder(holder.get());
        for (Direction facing : BlockLantern.FACING.getPossibleValues()) {
            ModelFile model = switch (facing) {
                case UP -> existing("lantern_up" + suffix);
                case DOWN -> existing("lantern_down" + suffix);
                default -> wall;
            };
            builder.partialState().with(BlockLantern.FACING, facing).modelForState()
                    .modelFile(model)
                    .rotationY(facing.getAxis().isHorizontal() ? wallY(facing, WALL_ANGLE_NORTH) : 0)
                    .addModel();
        }
        simpleBlockItem(holder.get(), wall);
    }

    private void gargoyle() {
        ModelFile model = existing("gargoyle");
        // Unlike the wall mounts above, the legacy rotations here are exactly Direction#toYRot() - north=180,
        // east=270, south=0, west=90 - so the art already faces south and the offset is zero.
        horizontalBlock(RusticBlocks.GARGOYLE.get(), model, 0);
        simpleBlockItem(RusticBlocks.GARGOYLE.get(), model);
    }

    /*
     * The wooden lantern, the mount that 1.12.2 registered with the furniture rather than with the metal set.
     * It is the candle's five states again - up plus the four walls, never down - over two models that already
     * exist as static assets, and its legacy blockstate turns north at 0, east at 90, south at 180 and west at
     * 270, which is the same authored-as-a-north-wall art the candles and metal lanterns use. The inventory
     * showed the standing lantern, unlike the metal one, which showed its wall model.
     */
    private void woodLantern() {
        wallOrFloor(getVariantBuilder(RusticBlocks.LANTERN_WOOD.get()), BlockWoodLantern.FACING,
                existing("lantern_wood_wall"), existing("lantern_wood"), WALL_ANGLE_NORTH);
        simpleBlockItem(RusticBlocks.LANTERN_WOOD.get(), existing("lantern_wood"));
    }

    /*
     * Chairs and tables, the eight woods of 1.12.2's furniture set.
     *
     * The chair is the plain horizontal case: its legacy blockstate turned north at 0, east at 90, south at
     * 180 and west at 270, so the art faces north as authored and the offset is the usual half turn. The item
     * was the block itself, which is what {@code simpleBlockItem} writes.
     *
     * The table is not rotated at all but assembled: one top plus up to four corner legs, each leg shown when
     * the two walls meeting at that corner are bare. 1.12.2 worked that out in {@code getActualState} from the
     * blocks around it; {@link BlockTable} stores the same four flags in the state, so datagen conditions on
     * them directly and the multipart matches the legacy file part for part. One leg model serves all four
     * corners, quarter-turned, and the inventory drew the whole table at once from its own _inventory model.
     */
    private void furniture() {
        for (RusticBlocks.Furniture set : RusticBlocks.FURNITURE) {
            String wood = set.name();
            ModelFile chair = existing("chair_" + wood);
            horizontalBlock(set.chair().get(), chair, WALL_ANGLE_NORTH);
            simpleBlockItem(set.chair().get(), chair);

            ModelFile top = existing("table_" + wood + "_top");
            ModelFile leg = existing("table_" + wood + "_leg_nw");
            getMultipartBuilder(set.table().get())
                    .part().modelFile(top).addModel().end()
                    .part().modelFile(leg).addModel().condition(BlockTable.NW, true).end()
                    .part().modelFile(leg).rotationY(90).addModel().condition(BlockTable.NE, true).end()
                    .part().modelFile(leg).rotationY(180).addModel().condition(BlockTable.SE, true).end()
                    .part().modelFile(leg).rotationY(270).addModel().condition(BlockTable.SW, true).end();
            itemModels().withExistingParent(path(set.table()), modLoc("block/table_" + wood + "_inventory"));
        }
    }

    /*
     * Rope, the other multipart of 1.12.2's hanging set.
     *
     * The legacy blockstate was Forge multipart over four static models - a vertical {@code rope}, a
     * {@code rope_horizontal} reused at two rotations, and the {@code rope_dangle} and {@code rope_knot}
     * overlays that appear only on the sagging variant. The {@code rope_attachment_n} model it also shipped
     * is referenced by nothing and was deleted with the state file.
     */
    private void rope() {
        ModelFile vertical = existing("rope");
        ModelFile horizontal = existing("rope_horizontal");
        ModelFile dangle = existing("rope_dangle");
        ModelFile knot = existing("rope_knot");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticBlocks.ROPE.get());
        // The body: one of three orientations, chosen by the axis property.
        builder.part().modelFile(horizontal).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(vertical).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Y).end()
                .part().modelFile(horizontal).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Z).end();
        /*
         * The sag: the legacy file turned the overlays with the body, hence the x case repeating the quarter
         * turn, while y and z share the upright art. Both overlays carry the same condition, so a sagging
         * rope draws body, dangle and knot together.
         */
        builder.part().modelFile(dangle).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).condition(BlockRopeBase.DANGLE, true).end()
                .part().modelFile(knot).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).condition(BlockRopeBase.DANGLE, true).end()
                .part().modelFile(dangle).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Y, Direction.Axis.Z)
                .condition(BlockRopeBase.DANGLE, true).end()
                .part().modelFile(knot).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Y, Direction.Axis.Z)
                .condition(BlockRopeBase.DANGLE, true).end();

        // 1.12.2's inventory model was a flat item drawing of the rope texture.
        itemModels().withExistingParent(path(RusticBlocks.ROPE), mcLoc("item/generated"))
                .texture("layer0", tex("rope"));
    }

    /*
     * The iron lattice, which 1.12.2 did not describe in JSON at all - a custom baked model
     * ({@code LatticeModel}) assembled it from five static models and the unlisted connection properties.
     * With those properties now listed in the state, multipart can express the same assembly, and the
     * rotations below are the model's own {@code ModelRotation} table, in {@code Direction.values()} order:
     * down X90, up X270, north none, south Y180, west Y270, east Y90. The bar art points north.
     */
    private void lattice() {
        ModelFile ironBase = existing("lattice_iron_base");
        ModelFile leavesBase = existing("lattice_leaves_base");
        ModelFile ironBar = existing("lattice_iron_bar");
        ModelFile leavesBar = existing("lattice_leaves_bar");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticBlocks.IRON_LATTICE.get());
        // The frame's hub, and the foliage over it once leaves have been planted on it.
        builder.part().modelFile(ironBase).addModel().end()
                .part().modelFile(leavesBase).addModel().condition(BlockLattice.LEAVES, true).end();
        arm(builder, BlockLattice.DOWN, ironBar, leavesBar, 90, 0);
        arm(builder, BlockLattice.UP, ironBar, leavesBar, 270, 0);
        arm(builder, BlockLattice.NORTH, ironBar, leavesBar, 0, 0);
        arm(builder, BlockLattice.SOUTH, ironBar, leavesBar, 0, 180);
        arm(builder, BlockLattice.WEST, ironBar, leavesBar, 0, 270);
        arm(builder, BlockLattice.EAST, ironBar, leavesBar, 0, 90);

        /*
         * The legacy item did not reuse the world model either: {@code models/item/iron_lattice_item.json}
         * parented a hand-authored {@code _inventory} model with gui transforms showing a complete frame, so
         * that file stays static and the generated item simply parents on it.
         */
        itemModels().withExistingParent(path(RusticBlocks.IRON_LATTICE), modLoc("block/lattice_iron_inventory"));
    }

    /** One arm of the lattice: the iron bar, plus the leaf bar over it when the frame is planted. */
    private void arm(MultiPartBlockStateBuilder builder, BooleanProperty connection,
                    ModelFile ironBar, ModelFile leavesBar, int rotationX, int rotationY) {
        builder.part().modelFile(ironBar).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(connection, true).end()
                .part().modelFile(leavesBar).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(connection, true).condition(BlockLattice.LEAVES, true).end();
    }

    /*
     * Fertile soil, a full cube of the mod's own texture except on top, where 1.12.2 borrowed the wet
     * farmland art so it read as ground you can plant in. The item was the block.
     */
    private void fertileSoil() {
        ResourceLocation soil = tex(RusticBlocks.FERTILE_SOIL);
        ModelFile model = models().withExistingParent(path(RusticBlocks.FERTILE_SOIL), mcLoc("block/cube_all"))
                .texture("all", soil)
                .texture("up", vanillaTex("farmland_moist"))
                .texture("particle", soil);
        simpleBlockWithItem(RusticBlocks.FERTILE_SOIL.get(), model);
    }

    /** The five states of a wall or floor mount: four turns of the wall model, plus the standing one. */
    private void wallOrFloor(VariantBlockStateBuilder builder, DirectionProperty facing,
                             ModelFile wall, ModelFile standing, int wallAngle) {
        for (Direction direction : facing.getPossibleValues()) {
            boolean upright = direction == Direction.UP;
            builder.partialState().with(facing, direction).modelForState()
                    .modelFile(upright ? standing : wall)
                    .rotationY(upright ? 0 : wallY(direction, wallAngle))
                    .addModel();
        }
    }

    /**
     * The y-rotation of a wall-mounted model, given the direction it points away from.
     * {@link Direction#toYRot()} answers south=0, west=90, north=180, east=270; the offset says which way
     * the authored model faces, {@link #WALL_ANGLE_NORTH} for art drawn on a north wall and
     * {@link #WALL_ANGLE_EAST} for art drawn on an east one.
     */
    private static int wallY(Direction facing, int wallAngle) {
        return Math.floorMod((int) facing.toYRot() + wallAngle, 360);
    }

    /** The candles, lanterns and gargoyles are modelled as if hung on a north wall. */
    private static final int WALL_ANGLE_NORTH = 180;
    /** ... and the iron torch alone as if clipped to an east one. */
    private static final int WALL_ANGLE_EAST = 90;

    /**
     * A block model that ships as a static asset instead of being generated, so the provider only has to
     * name it. The existence check is the point: a typo here fails datagen rather than showing up as the
     * missing-model texture in a game window.
     */
    private ModelFile existing(String name) {
        return models().getExistingFile(modLoc("block/" + name));
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
