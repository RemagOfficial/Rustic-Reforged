package rustic.data;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
import rustic.content.blocks.CondenserAdvancedBlock;
import rustic.content.blocks.CondenserBlock;
import rustic.content.blocks.RetortBlock;
import rustic.content.blocks.StakeTiedBlock;
import rustic.content.blocks.AppleSeedsBlock;
import rustic.content.blocks.GrapeLeavesBlock;
import rustic.content.blocks.GrapeStemBlock;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.HerbBlock;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.VaseBlock;
import rustic.content.fluids.RusticFluidBlocks;

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
        farmBlocks();
        herbs();
        grape();
        berry();
        worldgen();
        appleSeeds();
        fluidBlocks();
    }

    private void fluidBlocks() {
        fluidBlock(RusticFluidBlocks.OLIVE_OIL);
        fluidBlock(RusticFluidBlocks.IRONBERRY_JUICE);
        fluidBlock(RusticFluidBlocks.WILDBERRY_JUICE);
        fluidBlock(RusticFluidBlocks.GRAPE_JUICE);
        fluidBlock(RusticFluidBlocks.APPLE_JUICE);
        fluidBlock(RusticFluidBlocks.ALE_WORT);
        fluidBlock(RusticFluidBlocks.HONEY);
        fluidBlock(RusticFluidBlocks.GOLDEN_APPLE_JUICE);
    }

    private void fluidBlock(DeferredBlock<? extends LiquidBlock> holder) {
        String name = path(holder);
        ModelFile fluid = models().getBuilder(name).texture("particle", tex("fluids/" + name + "_still"));
        simpleBlockWithItem(holder.get(), fluid);
    }

    private void farmBlocks() {
        simpleBlockWithItem(RusticFarmBlocks.CROP_STAKE.get(), existing("crop_stake"));

        ModelFile stakeTied = existing("stake_tied");
        ModelFile attachment = existing("rope_attachment_n");
        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticFarmBlocks.STAKE_TIED.get());
        builder.part().modelFile(stakeTied).addModel().end()
                .part().modelFile(attachment).addModel().condition(StakeTiedBlock.NORTH, true).end()
                .part().modelFile(attachment).rotationY(90).addModel().condition(StakeTiedBlock.EAST, true).end()
                .part().modelFile(attachment).rotationY(180).addModel().condition(StakeTiedBlock.SOUTH, true).end()
                .part().modelFile(attachment).rotationY(270).addModel().condition(StakeTiedBlock.WEST, true).end();

        cropCrop(RusticFarmBlocks.TOMATO_CROP, "tomato_crop");
        cropCrop(RusticFarmBlocks.CHILI_CROP, "chili_crop");
    }

    private void cropCrop(
            DeferredBlock<? extends rustic.content.blocks.StakeCropBlock> holder,
            String modelPrefix
    ) {
        IntegerProperty age = rustic.content.blocks.StakeCropBlock.AGE;
        getVariantBuilder(holder.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(existing(modelPrefix + "_" + state.getValue(age))).build());
    }

    private void herbs() {
        herb(RusticFarmBlocks.ALOE_VERA, "seed");
        herb(RusticFarmBlocks.BLOOD_ORCHID, "seed");
        herb(RusticFarmBlocks.CHAMOMILE, "seed");
        herb(RusticFarmBlocks.CLOUDSBLUFF, "seed");
        herb(RusticFarmBlocks.COHOSH, "seed");
        herb(RusticFarmBlocks.CORE_ROOT, "seed");
        herb(RusticFarmBlocks.DEATHSTALK_MUSHROOM, "spore");
        herb(RusticFarmBlocks.GINSENG, "seed");
        herb(RusticFarmBlocks.HORSETAIL, "seed");
        herb(RusticFarmBlocks.MARSH_MALLOW, "seed");
        herb(RusticFarmBlocks.MOONCAP_MUSHROOM, "spore");
        herb(RusticFarmBlocks.VANTA_LILY, "seed");
        herb(RusticFarmBlocks.WIND_THISTLE, "seed");
    }

    private void herb(DeferredBlock<? extends HerbBlock> holder, String seedSuffix) {
        String name = path(holder);
        ModelFile mature = models().cross(name, tex("herbs/" + name)).renderType("cutout");
        ModelFile seed = models().cross(name + "_" + seedSuffix, tex("herbs/" + name + "_" + seedSuffix)).renderType("cutout");
        IntegerProperty age = HerbBlock.AGE;
        getVariantBuilder(holder.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(state.getValue(age) >= 3 ? mature : seed).build());
    }

    private void grape() {
        IntegerProperty stemAge = GrapeStemBlock.AGE;
        getVariantBuilder(RusticFarmBlocks.GRAPE_STEM.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(existing("grape_stem_" + state.getValue(stemAge))).build());

        ModelFile leafCore = existing("grape_leaves_0");
        ModelFile leafArm = existing("grape_leaves_1");
        ModelFile stemCap = existing("grape_stem_top");
        ModelFile vine = existing("grape_vine");
        ModelFile grapes = existing("grapes");
        ModelFile horizontal = existing("rope_horizontal");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticFarmBlocks.GRAPE_LEAVES.get());

        builder.part().modelFile(leafCore).addModel()
                .condition(GrapeLeavesBlock.DIST, 0).end()
                .part().modelFile(stemCap).addModel()
                .condition(GrapeLeavesBlock.DIST, 0).end()
                .part().modelFile(leafArm).rotationY(90).addModel()
                .condition(GrapeLeavesBlock.DIST, 1).condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(leafArm).addModel()
                .condition(GrapeLeavesBlock.DIST, 1).condition(BlockRopeBase.AXIS, Direction.Axis.Z).end();

        builder.part().modelFile(vine).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(vine).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Z).end()
                .part().modelFile(horizontal).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(horizontal).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Z).end();

        builder.part().modelFile(grapes).addModel()
                .condition(GrapeLeavesBlock.GRAPES, true).end();
    }

    private void berry() {
        simpleBlockWithItem(RusticFarmBlocks.WILDBERRY_BUSH.get(), existing("wildberry_bush"));
    }

    private void worldgen() {
        ModelFile beehive = existing("beehive");
        horizontalBlock(RusticFarmBlocks.BEEHIVE.get(), beehive, WALL_ANGLE_NORTH);
        simpleBlockItem(RusticFarmBlocks.BEEHIVE.get(), beehive);

        ModelFile appleLeaves = models().leaves(path(RusticFarmBlocks.APPLE_LEAVES), vanillaTex("oak_leaves"));
        getVariantBuilder(RusticFarmBlocks.APPLE_LEAVES.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(appleLeaves).build());
        simpleBlockItem(RusticFarmBlocks.APPLE_LEAVES.get(), appleLeaves);

        ModelFile appleSapling = models().cross(path(RusticFarmBlocks.APPLE_SAPLING), vanillaTex("oak_sapling"))
                .renderType("cutout");
        getVariantBuilder(RusticFarmBlocks.APPLE_SAPLING.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(appleSapling).build());
        itemModels().withExistingParent(path(RusticFarmBlocks.APPLE_SAPLING), mcLoc("item/generated"))
                .texture("layer0", vanillaTex("oak_sapling"));
    }

    private void machines() {
        simpleBlockWithItem(RusticMachineBlocks.BARREL.get(), existing("barrel"));

        ModelFile cabinet = existing("cabinet");
        getVariantBuilder(RusticMachineBlocks.CABINET.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(cabinet).build());
        simpleBlockItem(RusticMachineBlocks.CABINET.get(), cabinet);

        IntegerProperty vaseVariant = VaseBlock.VARIANT;
        getVariantBuilder(RusticMachineBlocks.VASE.get()).forAllStates(state ->

                ConfiguredModel.builder().modelFile(existing(vaseModel(state.getValue(vaseVariant)))).build());

        simpleBlockWithItem(RusticMachineBlocks.CRUSHING_TUB.get(), existing("crushing_tub"));
        simpleBlockWithItem(RusticMachineBlocks.EVAPORATING_BASIN.get(), existing("evaporating_basin"));
        simpleBlockWithItem(RusticMachineBlocks.LIQUID_BARREL.get(), existing("liquid_barrel"));

        simpleBlockWithItem(RusticMachineBlocks.BREWING_BARREL.get(), existing("brewing_barrel"));

        orientableColumn(RusticMachineBlocks.CONDENSER.get(), "condenser", "condenser_top");
        orientableColumn(RusticMachineBlocks.CONDENSER_ADVANCED.get(), "condenser_adv", "condenser_adv_top");

        orientable(RusticMachineBlocks.RETORT.get(), "retort");
        orientable(RusticMachineBlocks.RETORT_ADVANCED.get(), "retort_adv");

        ModelFile apiary = existing("apiary");
        horizontalBlock(RusticMachineBlocks.APIARY.get(), apiary, WALL_ANGLE_NORTH);
        simpleBlockItem(RusticMachineBlocks.APIARY.get(), apiary);
    }

    private void orientableColumn(Block block, String body, String top) {
        getVariantBuilder(block).forAllStates(state -> {
            ModelFile model = state.getValue(CondenserAdvancedBlock.TOP) ? existing(top) : existing(body);
            return ConfiguredModel.builder().modelFile(model)
                    .rotationY(orientableY(state.getValue(CondenserBlock.FACING))).build();
        });
        simpleBlockItem(block, existing(body));
    }

    private void orientable(Block block, String model) {
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(existing(model))
                .rotationY(orientableY(state.getValue(RetortBlock.FACING))).build());
        simpleBlockItem(block, existing(model));
    }

    private static int orientableY(Direction facing) {
        return Math.floorMod((int) facing.toYRot() + 180, 360);
    }

    private void pillars() {

        pillar(RusticBlocks.STONE_PILLAR, tex("pillar_stone_side"), tex("pillar_stone_top"));
        pillar(RusticBlocks.ANDESITE_PILLAR, tex("pillar_andesite_side"), vanillaTex("polished_andesite"));
        pillar(RusticBlocks.DIORITE_PILLAR, tex("pillar_diorite_side"), vanillaTex("polished_diorite"));
        pillar(RusticBlocks.GRANITE_PILLAR, tex("pillar_granite_side"), vanillaTex("polished_granite"));
        pillar(RusticBlocks.SLATE_PILLAR, tex("slate_pillar"), tex("slate_tile"));
    }

    private void pillar(DeferredBlock<? extends RotatedPillarBlock> holder, ResourceLocation side, ResourceLocation end) {
        String name = path(holder);
        ModelFile vertical = models().cubeColumn(name, side, end);
        ModelFile horizontal = models().cubeColumnHorizontal(name + "_horizontal", side, end);
        axisBlock(holder.get(), vertical, horizontal);
        simpleBlockItem(holder.get(), vertical);
    }

    private void slate() {
        cubeWithItem(RusticBlocks.SLATE);
        cubeWithItem(RusticBlocks.SLATE_ROOF);
        cubeWithItem(RusticBlocks.SLATE_TILE);
        cubeWithItem(RusticBlocks.SLATE_BRICK);
        cubeWithItem(RusticBlocks.SLATE_CHISELED);

        stairs(RusticBlocks.SLATE_ROOF_STAIRS, tex(RusticBlocks.SLATE_ROOF));
        stairs(RusticBlocks.SLATE_BRICK_STAIRS, tex(RusticBlocks.SLATE_BRICK));
        slab(RusticBlocks.SLATE_ROOF_SLAB, tex(RusticBlocks.SLATE_ROOF));
        slab(RusticBlocks.SLATE_BRICK_SLAB, tex(RusticBlocks.SLATE_BRICK));
    }

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

    private void slab(DeferredBlock<? extends SlabBlock> holder, ResourceLocation texture) {
        String name = path(holder);
        ModelFile bottom = models().slab(name, texture, texture, texture);
        ModelFile top = models().slabTop(name + "_top", texture, texture, texture);
        ModelFile doubleSlab = models().cubeAll(name + "_double", texture);
        slabBlock(holder.get(), bottom, top, doubleSlab);
        simpleBlockItem(holder.get(), bottom);
    }

    private void clayWalls() {
        cubeWithItem(RusticBlocks.CLAY_WALL);
        cubeWithItem(RusticBlocks.CLAY_WALL_CROSS);

        ModelFile model = existing("clay_wall_diag");
        horizontalBlock(RusticBlocks.CLAY_WALL_DIAG.get(), model, 180);
        simpleBlockItem(RusticBlocks.CLAY_WALL_DIAG.get(), model);
    }

    private void paintedWood() {
        for (DyeColor color : DyeColor.values()) {
            cubeWithItem(RusticBlocks.PAINTED_WOOD.get(color));
        }
    }

    private void woodFamily() {
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            String name = wood.name();
            ResourceLocation planks = tex("planks_" + name);

            cubeWithItem(wood.planks());
            pillar(wood.log(), tex("log_" + name), tex("log_" + name + "_top"));

            ModelFile leaves = models().leaves(path(wood.leaves()), tex("leaves_" + name));
            simpleBlockWithItem(wood.leaves().get(), leaves);

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

            doorBlock(wood.door().get(), tex("door_" + name + "_lower"), tex("door_" + name + "_upper"));
            itemModels().withExistingParent(path(wood.door()), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/door_" + name));
        }
    }

    private void appleSeeds() {
        String name = path(RusticFarmBlocks.APPLE_SEEDS_CROP);
        ModelFile age0 = models().cross(name + "_0", tex("apple_seeds_0")).renderType("cutout");
        ModelFile age1 = models().cross(name + "_1", tex("apple_seeds_1")).renderType("cutout");
        IntegerProperty age = AppleSeedsBlock.AGE;
        getVariantBuilder(RusticFarmBlocks.APPLE_SEEDS_CROP.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(state.getValue(age) >= 1 ? age1 : age0).build());
    }

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

        itemModels().withExistingParent(path(holder), mcLoc("item/generated"))
                .texture("layer0", tex("chain" + suffix));
    }

    private void candle(DeferredBlock<BlockCandle> holder, String suffix) {
        wallOrFloor(getVariantBuilder(holder.get()), BlockCandleBase.FACING,
                existing("candle_wall" + suffix), existing("candle" + suffix), WALL_ANGLE_NORTH);
        simpleBlockItem(holder.get(), existing("candle" + suffix));
    }

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

        simpleBlockWithItem(holder.get(), existing("chandelier" + suffix));
    }

    private void ironTorch() {
        wallOrFloor(getVariantBuilder(RusticBlocks.IRON_TORCH.get()), BlockCandleBase.FACING,
                existing("iron_torch_wall"), existing("iron_torch_alt"), WALL_ANGLE_EAST);
        simpleBlockItem(RusticBlocks.IRON_TORCH.get(), existing("iron_torch_alt"));
    }

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

        horizontalBlock(RusticBlocks.GARGOYLE.get(), model, 0);
        simpleBlockItem(RusticBlocks.GARGOYLE.get(), model);
    }

    private void woodLantern() {
        wallOrFloor(getVariantBuilder(RusticBlocks.LANTERN_WOOD.get()), BlockWoodLantern.FACING,
                existing("lantern_wood_wall"), existing("lantern_wood"), WALL_ANGLE_NORTH);
        simpleBlockItem(RusticBlocks.LANTERN_WOOD.get(), existing("lantern_wood"));
    }

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

    private void rope() {
        ModelFile vertical = existing("rope");
        ModelFile horizontal = existing("rope_horizontal");
        ModelFile dangle = existing("rope_dangle");
        ModelFile knot = existing("rope_knot");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticBlocks.ROPE.get());

        builder.part().modelFile(horizontal).rotationY(90).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.X).end()
                .part().modelFile(vertical).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Y).end()
                .part().modelFile(horizontal).addModel()
                .condition(BlockRopeBase.AXIS, Direction.Axis.Z).end();

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

        itemModels().withExistingParent(path(RusticBlocks.ROPE), mcLoc("item/generated"))
                .texture("layer0", tex("rope"));
    }

    private void lattice() {
        ModelFile ironBase = existing("lattice_iron_base");
        ModelFile leavesBase = existing("lattice_leaves_base");
        ModelFile ironBar = existing("lattice_iron_bar");
        ModelFile leavesBar = existing("lattice_leaves_bar");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(RusticBlocks.IRON_LATTICE.get());

        builder.part().modelFile(ironBase).addModel().end()
                .part().modelFile(leavesBase).addModel().condition(BlockLattice.LEAVES, true).end();
        arm(builder, BlockLattice.DOWN, ironBar, leavesBar, 90, 0);
        arm(builder, BlockLattice.UP, ironBar, leavesBar, 270, 0);
        arm(builder, BlockLattice.NORTH, ironBar, leavesBar, 0, 0);
        arm(builder, BlockLattice.SOUTH, ironBar, leavesBar, 0, 180);
        arm(builder, BlockLattice.WEST, ironBar, leavesBar, 0, 270);
        arm(builder, BlockLattice.EAST, ironBar, leavesBar, 0, 90);

        itemModels().withExistingParent(path(RusticBlocks.IRON_LATTICE), modLoc("block/lattice_iron_inventory"));
    }

    private void arm(MultiPartBlockStateBuilder builder, BooleanProperty connection,
                    ModelFile ironBar, ModelFile leavesBar, int rotationX, int rotationY) {
        builder.part().modelFile(ironBar).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(connection, true).end()
                .part().modelFile(leavesBar).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(connection, true).condition(BlockLattice.LEAVES, true).end();
    }

    private void fertileSoil() {
        ResourceLocation soil = tex(RusticBlocks.FERTILE_SOIL);
        ModelFile model = models().withExistingParent(path(RusticBlocks.FERTILE_SOIL), mcLoc("block/cube_all"))
                .texture("all", soil)
                .texture("up", vanillaTex("farmland_moist"))
                .texture("particle", soil);
        simpleBlockWithItem(RusticBlocks.FERTILE_SOIL.get(), model);
    }

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

    private static int wallY(Direction facing, int wallAngle) {
        return Math.floorMod((int) facing.toYRot() + wallAngle, 360);
    }

    private static final int WALL_ANGLE_NORTH = 180;

    private static final int WALL_ANGLE_EAST = 90;

    private ModelFile existing(String name) {
        return models().getExistingFile(modLoc("block/" + name));
    }

    private static String vaseModel(int variant) {
        return "vase_" + variant;
    }

    private static String path(DeferredBlock<? extends Block> holder) {
        return holder.getId().getPath();
    }

    private ResourceLocation tex(String name) {
        return modLoc("block/" + name);
    }

    private ResourceLocation tex(DeferredBlock<? extends Block> holder) {
        return tex(path(holder));
    }

    private ResourceLocation vanillaTex(String name) {
        return mcLoc("block/" + name);
    }
}
