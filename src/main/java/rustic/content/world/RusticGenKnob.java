package rustic.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.Tags;
import rustic.config.RusticConfig;

import java.util.List;

public enum RusticGenKnob {

    SLATE_OVERWORLD(Routing.OVERWORLD_SLATE, null, RusticConfig.SLATE_VEINS_PER_CHUNK, Scatter.ORE_OVERWORLD,
            List.of(), List.of()),

    SLATE_NETHER(Routing.NETHER_SLATE, null, RusticConfig.SLATE_VEINS_PER_CHUNK, Scatter.ORE_NETHER,
            List.of(), List.of()),

    OLIVE(Routing.OVERWORLD, RusticConfig.OLIVE_GEN_CHANCE, RusticConfig.MAX_OLIVE_GEN_ATTEMPTS, Scatter.TREE,
            List.of(Tags.Biomes.IS_FOREST, Tags.Biomes.IS_PLAINS, Tags.Biomes.IS_MOUNTAIN),
            List.of(Tags.Biomes.IS_SNOWY)),

    IRONWOOD(Routing.OVERWORLD, RusticConfig.IRONWOOD_GEN_CHANCE, RusticConfig.MAX_IRONWOOD_GEN_ATTEMPTS, Scatter.TREE,
            List.of(Tags.Biomes.IS_FOREST, Tags.Biomes.IS_PLAINS, Tags.Biomes.IS_MOUNTAIN, Tags.Biomes.IS_SWAMP,
                    Tags.Biomes.IS_JUNGLE),
            List.of(Tags.Biomes.IS_DRY)),

    SURFACE_HERB(Routing.OVERWORLD, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.SURFACE,
            List.of(), List.of(Tags.Biomes.IS_SNOWY)),

    CAVE_HERB(Routing.OVERWORLD, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.CAVE,
            List.of(), List.of()),

    NETHER_HERB(Routing.NETHER, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.NETHER_HERB,
            List.of(), List.of()),

    WILDBERRY(Routing.OVERWORLD, RusticConfig.WILDBERRY_GEN_CHANCE, RusticConfig.MAX_WILDBERRY_ATTEMPTS, Scatter.WILDBERRY,
            List.of(),
            List.of(Tags.Biomes.IS_COLD, Tags.Biomes.IS_SNOWY, Tags.Biomes.IS_SANDY, Tags.Biomes.IS_SAVANNA,
                    Tags.Biomes.IS_BADLANDS, Tags.Biomes.IS_MUSHROOM, Tags.Biomes.IS_NETHER, Tags.Biomes.IS_END,
                    Tags.Biomes.IS_DEAD, Tags.Biomes.IS_WASTELAND)),

    BEEHIVE(Routing.OVERWORLD, RusticConfig.BEEHIVE_GEN_CHANCE, RusticConfig.MAX_BEEHIVE_ATTEMPTS, Scatter.BEEHIVE,
            List.of(), List.of(Tags.Biomes.IS_SNOWY));

    private enum Routing {
        OVERWORLD,
        NETHER,

        OVERWORLD_SLATE,

        NETHER_SLATE
    }

    private final Routing routing;
    private final ModConfigSpec.DoubleValue chance;
    private final ModConfigSpec.IntValue attempts;
    private final Scatter scatter;
    private final List<TagKey<Biome>> anyOf;
    private final List<TagKey<Biome>> noneOf;

    RusticGenKnob(Routing routing, ModConfigSpec.DoubleValue chance, ModConfigSpec.IntValue attempts, Scatter scatter,
            List<TagKey<Biome>> anyOf, List<TagKey<Biome>> noneOf) {
        this.routing = routing;
        this.chance = chance;
        this.attempts = attempts;
        this.scatter = scatter;
        this.anyOf = anyOf;
        this.noneOf = noneOf;
    }

    public boolean dimensionAllows(ResourceKey<Level> dimension) {
        return switch (routing) {
            case OVERWORLD -> whitelisted(RusticConfig.OVERWORLD_GENERATION_WHITELIST, dimension);
            case NETHER -> whitelisted(RusticConfig.NETHER_GENERATION_WHITELIST, dimension);
            case OVERWORLD_SLATE -> whitelisted(RusticConfig.OVERWORLD_GENERATION_WHITELIST, dimension)
                    && RusticConfig.ENABLE_SLATE.get() && !RusticConfig.NETHER_SLATE.get();
            case NETHER_SLATE -> whitelisted(RusticConfig.NETHER_GENERATION_WHITELIST, dimension)
                    && RusticConfig.ENABLE_SLATE.get() && RusticConfig.NETHER_SLATE.get();
        };
    }

    public boolean biomeAllows(WorldGenLevel level, BlockPos origin) {
        var biome = level.getBiome(origin);
        if (!anyOf.isEmpty() && anyOf.stream().noneMatch(biome::is)) {
            return false;
        }
        for (TagKey<Biome> rejected : noneOf) {
            if (biome.is(rejected)) {
                return false;
            }
        }
        return true;
    }

    public boolean rollsChance() {
        return chance != null;
    }

    public float chance() {
        return chance == null ? 1.0F : chance.get().floatValue();
    }

    public int attempts() {
        return attempts.get();
    }

    public Scatter scatter() {
        return scatter;
    }

    public boolean naturalSpread() {
        return scatter == Scatter.TREE && RusticConfig.NATURAL_TREE_PLACEMENT.get();
    }

    private static boolean whitelisted(ModConfigSpec.ConfigValue<List<? extends Integer>> whitelist,
            ResourceKey<Level> dimension) {
        List<? extends Integer> ids = whitelist.get();
        return ids != null && ids.contains(legacyDimensionId(dimension));
    }

    private static int legacyDimensionId(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return 0;
        }
        if (dimension.equals(Level.NETHER)) {
            return -1;
        }
        if (dimension.equals(Level.END)) {
            return 1;
        }
        return Integer.MIN_VALUE;
    }

    public enum Scatter {

        BEEHIVE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                int y = canopySurface(level, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                if (!(level.getBlockState(below).getBlock() instanceof LeavesBlock)) {
                    return null;
                }
                BlockPos temp = below;
                while (level.getBlockState(temp).getBlock() instanceof LeavesBlock) {
                    temp = temp.below();
                }
                return level.isEmptyBlock(temp) ? temp : null;
            }
        },

        ORE_OVERWORLD {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return new BlockPos(origin.getX() + random.nextInt(16), random.nextInt(80) + 4,
                        origin.getZ() + random.nextInt(16));
            }
        },

        ORE_NETHER {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return new BlockPos(origin.getX() + random.nextInt(16), random.nextInt(112) + 8,
                        origin.getZ() + random.nextInt(16));
            }
        },

        TREE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                int y = vegetationSurface(level, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                BlockState belowState = level.getBlockState(below);

                return (belowState.is(BlockTags.DIRT) || belowState.is(Blocks.GRASS_BLOCK)) ? new BlockPos(x, y, z) : null;
            }
        },

        SURFACE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return surfaceScatter(random, level, origin);
            }
        },

        WILDBERRY {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                BlockPos pos = surfaceScatter(random, level, origin);
                return level.getFluidState(pos).isEmpty() ? pos : null;
            }
        },

        CAVE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                BlockPos pos = new BlockPos(x, random.nextInt(32) + 10, z);
                return level.isEmptyBlock(pos) ? dropToFloor(level, pos, 4, -1) : null;
            }
        },

        NETHER_HERB {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                BlockPos pos = new BlockPos(x, random.nextInt(96) + 32, z);
                return level.isEmptyBlock(pos) ? dropToFloor(level, pos, Integer.MIN_VALUE, 24) : null;
            }
        };

        public abstract BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin);

        static BlockPos surfaceScatter(RandomSource random, WorldGenLevel level, BlockPos origin) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            return new BlockPos(x, vegetationSurface(level, x, z), z);
        }

        static int vegetationSurface(WorldGenLevel level, int x, int z) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }

        static int canopySurface(WorldGenLevel level, int x, int z) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        }

        static BlockPos dropToFloor(WorldGenLevel level, BlockPos pos, int minFloorY, int maxDrop) {
            int i = 0;
            while (level.isEmptyBlock(pos.below(i)) && pos.getY() - i >= minFloorY && (maxDrop < 0 || i < maxDrop)) {
                i++;
            }
            return pos.below(Math.max(1, i) - 1);
        }
    }
}
