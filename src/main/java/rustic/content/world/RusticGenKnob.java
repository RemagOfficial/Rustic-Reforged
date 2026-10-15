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

/**
 * The live, code-side half of Phase 8's hybrid world generation.
 *
 * <p>1.12.2's {@code WorldGeneratorRustic} read {@code rustic.common.Config} on every chunk and, for each of its
 * nine generators, decided in code <em>whether</em> to run (a dimension whitelist plus a per-chunk chance roll),
 * <em>how often</em> (a {@code MAX_*_ATTEMPTS} scatter loop), <em>where</em> (a legacy scatter formula that turns a
 * chunk origin into a candidate position) and <em>in which biome</em> (a {@code BiomeDictionary} type test). The
 * {@code RusticConfigGatedFeature} reuses one {@link RusticGenKnob} to reproduce all four decisions for whichever
 * generator wraps it, while the {@code ConfiguredFeature} the knob wraps carries only the <em>what</em> - the block
 * set or tree shape - as data.
 *
 * <p>Every value here is read through the {@link ModConfigSpec} {@code Value} reference at {@code place()} time,
 * never cached, so flipping {@code rustic-common.toml} affects newly generated chunks without regenerating any JSON.
 * This is the reason the dimension whitelist and the {@code ENABLE_SLATE}/{@code NETHER_SLATE} routing cannot live
 * in the biome-tag predicates of a {@code neoforge:add_features} modifier: a biome tag answers "what kind of biome is
 * this", not "which dimension is generating" nor "did the user disable slate".
 *
 * <p>The {@link Tags.Biomes} references replace the legacy {@code BiomeDictionary.Type}s: {@code FOREST}-{@code
 * PLAINS}-{@code MOUNTAIN}-{@code SNOWY}-{@code DRY}-{@code SWAMP}-{@code JUNGLE}-{@code COLD}-{@code SANDY}-{@code
 * SAVANNA}-{@code MESA}-{@code MUSHROOM}-{@code NETHER}-{@code END}-{@code DEAD}-{@code WASTELAND} all have a NeoForge
 * {@code is_*} equivalent, with legacy {@code MESA} mapping to {@code IS_BADLANDS}.
 */
public enum RusticGenKnob {

    /** Slate in the overworld: no chance roll, {@code SLATE_VEINS_PER_CHUNK} full-chunk attempts, y 4..83. */
    SLATE_OVERWORLD(Routing.OVERWORLD_SLATE, null, RusticConfig.SLATE_VEINS_PER_CHUNK, Scatter.ORE_OVERWORLD,
            List.of(), List.of()),

    /** Slate relocated to the nether by {@code NETHER_SLATE}: same loop, y 8..119. */
    SLATE_NETHER(Routing.NETHER_SLATE, null, RusticConfig.SLATE_VEINS_PER_CHUNK, Scatter.ORE_NETHER,
            List.of(), List.of()),

    /** Olive tree: forest/plains/mountain but not snowy, legacy scatter with a motion-blocking surface height. */
    OLIVE(Routing.OVERWORLD, RusticConfig.OLIVE_GEN_CHANCE, RusticConfig.MAX_OLIVE_GEN_ATTEMPTS, Scatter.TREE,
            List.of(Tags.Biomes.IS_FOREST, Tags.Biomes.IS_PLAINS, Tags.Biomes.IS_MOUNTAIN),
            List.of(Tags.Biomes.IS_SNOWY)),

    /** Ironwood tree: forest/plains/mountain/swamp/jungle but not dry. */
    IRONWOOD(Routing.OVERWORLD, RusticConfig.IRONWOOD_GEN_CHANCE, RusticConfig.MAX_IRONWOOD_GEN_ATTEMPTS, Scatter.TREE,
            List.of(Tags.Biomes.IS_FOREST, Tags.Biomes.IS_PLAINS, Tags.Biomes.IS_MOUNTAIN, Tags.Biomes.IS_SWAMP,
                    Tags.Biomes.IS_JUNGLE),
            List.of(Tags.Biomes.IS_DRY)),

    /**
     * Surface herbs: rejects snowy at the chunk origin (the legacy {@code WorldGenSurfaceHerbs} guard). The
     * per-biome-category herb set is chosen by which {@code ConfiguredFeature} a biome modifier wraps this knob
     * around, so the knob itself needs no any-of list.
     */
    SURFACE_HERB(Routing.OVERWORLD, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.SURFACE,
            List.of(), List.of(Tags.Biomes.IS_SNOWY)),

    /** Cave herbs: any biome, dropped from y 10..41 onto the first cave floor. */
    CAVE_HERB(Routing.OVERWORLD, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.CAVE,
            List.of(), List.of()),

    /** Nether herbs (always deathstalk): nether routing, dropped from y 32..127 with a 24-block walk. */
    NETHER_HERB(Routing.NETHER, RusticConfig.HERB_GEN_CHANCE, RusticConfig.MAX_HERB_ATTEMPTS, Scatter.NETHER_HERB,
            List.of(), List.of()),

    /**
     * Wildberries: the legacy blacklist minus cold/sandy/savanna/mesa/mushroom/nether/end/dead/wasteland. Uses
     * {@link Scatter#WILDBERRY}, the herb surface scatter plus the legacy liquid-cell rejection, and rejects a chunk
     * whose origin biome is on that list.
     */
    WILDBERRY(Routing.OVERWORLD, RusticConfig.WILDBERRY_GEN_CHANCE, RusticConfig.MAX_WILDBERRY_ATTEMPTS, Scatter.WILDBERRY,
            List.of(),
            List.of(Tags.Biomes.IS_COLD, Tags.Biomes.IS_SNOWY, Tags.Biomes.IS_SANDY, Tags.Biomes.IS_SAVANNA,
                    Tags.Biomes.IS_BADLANDS, Tags.Biomes.IS_MUSHROOM, Tags.Biomes.IS_NETHER, Tags.Biomes.IS_END,
                    Tags.Biomes.IS_DEAD, Tags.Biomes.IS_WASTELAND)),

    /**
     * Beehive: the overworld gate, the {@code !snowy} chunk-origin test and the {@code BEEHIVE_GEN_CHANCE}/
     * {@code MAX_BEEHIVE_ATTEMPTS} loop the dispatcher ran before delegating to {@code WorldGenBeehive}. The
     * leaves walk-down itself is the {@link Scatter#BEEHIVE} scatter, and {@link RusticBeehiveFeature} reads this
     * constant directly rather than wrapping a data feature (there is no block set to declare - it places the hive).
     */
    BEEHIVE(Routing.OVERWORLD, RusticConfig.BEEHIVE_GEN_CHANCE, RusticConfig.MAX_BEEHIVE_ATTEMPTS, Scatter.BEEHIVE,
            List.of(), List.of(Tags.Biomes.IS_SNOWY));

    /** Which dimension whitelist gates the knob, and whether the slate toggles additionally route it. */
    private enum Routing {
        OVERWORLD,
        NETHER,
        /** overworld whitelist AND {@code ENABLE_SLATE && !NETHER_SLATE}. */
        OVERWORLD_SLATE,
        /** nether whitelist AND {@code ENABLE_SLATE && NETHER_SLATE}. */
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

    /**
     * Whether this generator runs in {@code dimension} at all, combining the whitelist membership with the slate
     * routing. Custom (non-vanilla) dimensions resolve to no legacy id and so are never whitelisted, exactly as the
     * 1.12.2 numeric-id whitelist behaved for a dimension it did not recognise.
     */
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

    /** Whether the chunk-origin biome satisfies the any-of / none-of type tests. */
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

    /** {@code true} when a per-chunk {@link #chance()} roll guards this generator (false for the ore loop). */
    public boolean rollsChance() {
        return chance != null;
    }

    /** The live per-chunk chance, or {@code 1.0f} when {@link #rollsChance()} is false. */
    public float chance() {
        return chance == null ? 1.0F : chance.get().floatValue();
    }

    /** The live scatter-loop attempt count for one chunk. */
    public int attempts() {
        return attempts.get();
    }

    /** This generator's legacy scatter formula. */
    public Scatter scatter() {
        return scatter;
    }

    /**
     * Whether this generator honours {@link RusticConfig#NATURAL_TREE_PLACEMENT}. Only the tree scatter does. When the
     * toggle is on, {@code RusticConfigGatedFeature} rolls {@link #chance()} once per scatter attempt instead of once
     * per chunk, so the same average density spreads evenly across the biome rather than clustering in the handful of
     * chunks that win the single legacy roll. Every non-tree generator returns {@code false} and keeps the legacy path.
     */
    public boolean naturalSpread() {
        return scatter == Scatter.TREE && RusticConfig.NATURAL_TREE_PLACEMENT.get();
    }

    private static boolean whitelisted(ModConfigSpec.ConfigValue<List<? extends Integer>> whitelist,
            ResourceKey<Level> dimension) {
        List<? extends Integer> ids = whitelist.get();
        return ids != null && ids.contains(legacyDimensionId(dimension));
    }

    /**
     * The 1.12.2 numeric dimension id for the three vanilla dimensions (overworld {@code 0}, nether {@code -1}, the
     * end {@code 1}), or {@link Integer#MIN_VALUE} for anything else so it never matches a whitelist entry. The
     * shipped config defaults are {@code [0]} and {@code [-1]}, so Rustic gen lands in vanilla dimensions only.
     */
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

    /**
     * The per-generator scatter formula: given the chunk {@code origin} a biome decoration hands the feature, produce
     * the candidate position for one attempt, or {@code null} to skip that attempt (matching a legacy branch where
     * the generator declined to try at all). Every formula is ported from its 1.12.2 {@code WorldGen*} class verbatim.
     */
    public enum Scatter {

        /**
         * {@code WorldGenBeehive}: one candidate anywhere in the chunk, then a downward walk through {@link
         * LeavesBlock} to the air cell the hive hangs in. Returns {@code null} unless the cell one below the canopy
         * surface is a leaf and the walk-down lands on a replaceable (empty) cell - exactly the two guards the legacy
         * generator required before placing. This is the one scatter that must read the {@code MOTION_BLOCKING}
         * surface (canopy included, not leaf-free ground): the hive hangs <em>under leaves</em>, so it has to start
         * at the treetop and walk down through them.
         */
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
        /** {@code WorldGeneratorRustic}: {@code x/z = corner + rand(16)}, {@code y = rand(80) + 4}. */
        ORE_OVERWORLD {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return new BlockPos(origin.getX() + random.nextInt(16), random.nextInt(80) + 4,
                        origin.getZ() + random.nextInt(16));
            }
        },

        /** {@code WorldGeneratorRustic} nether branch: {@code y = rand(112) + 8}. */
        ORE_NETHER {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return new BlockPos(origin.getX() + random.nextInt(16), random.nextInt(112) + 8,
                        origin.getZ() + random.nextInt(16));
            }
        },

        /**
         * {@code WorldGenAllTrees}: one candidate anywhere in the chunk, planted on the leaf-free surface.
         *
         * <p>Three faithful departures from the legacy {@code origin + (5 - rand(11))} around the chunk centre:
         * (1) the chunk generator hands {@code rustic:config_gated} the chunk's minimum corner, not its centre, so
         * the legacy {@code \u00b15} band would hug the chunk seam and read as a patch - {@code rand(16)} spreads the
         * same {@code MAX_*_GEN_ATTEMPTS} trees over the whole cell instead; (2) the surface lookup is
         * {@code MOTION_BLOCKING_NO_LEAVES}, so a second tree scattered under an existing canopy still finds real
         * ground rather than growing on top of the leaves (the legacy {@code World.getHeight} counted leaves as
         * solid, which is exactly how the 1.12 version put trunks in mid-air); (3) a soil check on {@code
         * pos.below()}. Vanilla {@code TreeFeature} has no ground test of its own - its {@code doPlace} only walks
         * upward looking for air - so without this scatter-level guard trees root on stone cliffs, sand beaches and
         * shallow-water surfaces, which reads as "flowers, not trees". The accepted blocks are
         * {@link BlockTags#DIRT} (dirt, coarse_dirt, rooted_dirt, podzol, mycelium, moss_block) and
         * {@code grass_block}, matching the legacy {@code BlockSapling.canPlaceBlockAt -> isSoil} set.
         */
        TREE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                int y = vegetationSurface(level, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                BlockState belowState = level.getBlockState(below);
                // Legacy: BlockSapling.canPlaceBlockAt -> isSoil. Vanilla TreeFeature has no soil test of its own
                // (doPlace only walks upward looking for air), so without this scatter-level guard trees root on
                // stone cliffs, sand beaches and shallow water surfaces, which reads as "flowers, not trees".
                // BlockTags.DIRT already covers dirt / coarse_dirt / rooted_dirt / podzol / mycelium / moss_block;
                // grass_block is the common surface block that sits on top of the DIRT tag.
                return (belowState.is(BlockTags.DIRT) || belowState.is(Blocks.GRASS_BLOCK)) ? new BlockPos(x, y, z) : null;
            }
        },

        /** {@code WorldGenSurfaceHerbs}/{@code WorldGenWildberries}: {@code x/z = origin + rand(7) - rand(7)}, top block. */
        SURFACE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                return surfaceScatter(random, level, origin);
            }
        },

        /**
         * {@code WorldGenWildberries} only: the same surface scatter as {@link #SURFACE}, but a candidate whose top
         * block is a liquid is skipped - the legacy {@code generateBush} opened with
         * {@code if (state.getMaterial().isLiquid()) return false;}. Without it the motion-blocking height lookup
         * hands back a water cell and {@code minecraft:simple_block} would overwrite it, so a bush could spawn
         * underwater. The herb generators had no such test, which is why they keep sharing plain {@link #SURFACE}.
         */
        WILDBERRY {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                BlockPos pos = surfaceScatter(random, level, origin);
                return level.getFluidState(pos).isEmpty() ? pos : null;
            }
        },

        /** {@code WorldGenCaveHerbs}: one candidate anywhere in the chunk, {@code y = rand(32) + 10}, drop to floor (min y 4). */
        CAVE {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                BlockPos pos = new BlockPos(x, random.nextInt(32) + 10, z);
                return level.isEmptyBlock(pos) ? dropToFloor(level, pos, 4, -1) : null;
            }
        },

        /** {@code WorldGenNetherHerbs}: one candidate anywhere in the chunk, {@code y = rand(96) + 32}, drop with a 24-block walk. */
        NETHER_HERB {
            @Override
            public BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin) {
                int x = origin.getX() + random.nextInt(16);
                int z = origin.getZ() + random.nextInt(16);
                BlockPos pos = new BlockPos(x, random.nextInt(96) + 32, z);
                return level.isEmptyBlock(pos) ? dropToFloor(level, pos, Integer.MIN_VALUE, 24) : null;
            }
        };

        /** Produce one candidate position for an attempt, or {@code null} to skip it. */
        public abstract BlockPos apply(RandomSource random, WorldGenLevel level, BlockPos origin);

        /** {@code x/z} anywhere in the chunk, planted on the leaf-free surface - the herb and berry scatter. */
        static BlockPos surfaceScatter(RandomSource random, WorldGenLevel level, BlockPos origin) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            return new BlockPos(x, vegetationSurface(level, x, z), z);
        }

        /**
         * The surface a plant is rooted on: {@code MOTION_BLOCKING_NO_LEAVES}, the first free cell above the ground
         * ignoring leaf canopies - the same heightmap vanilla hands {@code TREE}, {@code FLOWER} and the burry
         * placements. Leaves are excluded so a candidate landing under an existing tree still reaches soil rather
         * than sprouting on top of the canopy. Water is counted as motion-blocking so the free cell is <em>above</em>
         * the water surface; the caller (the {@link Scatter#TREE} soil check, or {@link Scatter#WILDBERRY}'s fluid
         * test) is then responsible for rejecting that cell when it is not the intended surface.
         */
        static int vegetationSurface(WorldGenLevel level, int x, int z) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }

        /**
         * The top of anything that blocks motion, {@code LeavesBlock} included: {@code MOTION_BLOCKING}. Used only
         * by {@link #BEEHIVE}, which must first find the canopy to walk down through. 1.12.2's
         * {@code World.getHeight(x, z)} returned this same canopy-inclusive height.
         */
        static int canopySurface(WorldGenLevel level, int x, int z) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        }

        /**
         * Walk down from {@code pos} through air to the first floor block, mirroring the legacy herb drop loops. The
         * walk stops at {@code minFloorY} (cave floor) or after {@code maxDrop} blocks (nether), whichever the caller
         * set; a non-positive {@code maxDrop} means the drop is bounded by {@code minFloorY} alone. The returned cell
         * is the first air block above the floor, exactly where the herb was then placed.
         */
        static BlockPos dropToFloor(WorldGenLevel level, BlockPos pos, int minFloorY, int maxDrop) {
            int i = 0;
            while (level.isEmptyBlock(pos.below(i)) && pos.getY() - i >= minFloorY && (maxDrop < 0 || i < maxDrop)) {
                i++;
            }
            return pos.below(Math.max(1, i) - 1);
        }
    }
}
