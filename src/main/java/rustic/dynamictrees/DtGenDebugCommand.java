package rustic.dynamictrees;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.LevelContext;
import com.dtteam.dynamictrees.deserialization.math.MathContext;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.worldgen.BiomeDatabase;
import com.dtteam.dynamictrees.worldgen.BiomeDatabases;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import com.dtteam.dynamictrees.worldgen.IDTBiomeHolderSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dev/debug tool for Phase 10 / S10.3 natural generation. Not shipped gameplay content - it exists so the
 * "why can't I find olive/ironwood in the wild?" question can be answered from the running game rather than by
 * guessing at the biome-populator JSON.
 *
 * <p>{@code /dtgen [samples]} reads the exact structure Dynamic Trees' world generator consults - the live
 * {@link BiomeDatabase} for the biome the player is standing in - and prints, for that biome: the forestness value,
 * a density sample, and a histogram of the species its {@link BiomePropertySelectors.SpeciesSelector} actually
 * returns when sampled {@code samples} times (the same call the tree feature makes when it decides what to plant).
 *
 * <p>How to read the output:
 * <ul>
 *   <li>olive/ironwood present in the histogram &rarr; the {@code world_gen/rustic.json} populator is assigned; if the
 *       percentage is small the trees are simply competing with oak/birch (raise their weight), and if it is large yet
 *       none are seen in the world the failure is in generation validation, not the database.</li>
 *   <li>olive/ironwood absent (only dynamictrees:* species, or every draw unhandled/none) &rarr; the populator is not
 *       being applied to this biome (wrong tag/select, or {@code method} semantics), which is a data fix.</li>
 * </ul>
 *
 * <p>Touches the {@code compileOnly} Dynamic Trees API, so it is reached only through
 * {@link RusticDynamicTreesCompat#registerCommands}, itself wired behind the {@code ModList.isLoaded("dynamictrees")}
 * guard in {@link rustic.Rustic}; the class is never loaded when Dynamic Trees is absent.
 */
public final class DtGenDebugCommand {

    /** Number of selector draws per run when the player does not pass an explicit count. */
    private static final int DEFAULT_SAMPLES = 2000;

    private DtGenDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dtgen")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context, DEFAULT_SAMPLES))
                .then(Commands.argument("samples", IntegerArgumentType.integer(1, 100000))
                        .executes(context -> run(context, IntegerArgumentType.getInteger(context, "samples")))));
    }

    private static int run(CommandContext<CommandSourceStack> context, int samples) {
        CommandSourceStack source = context.getSource();
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("dtgen must be run by a player - it reports the biome you are standing in."));
            return 0;
        }

        final Level level = player.level();
        final BlockPos pos = player.blockPosition();
        final Holder<Biome> biome = level.getBiome(pos);
        final BiomeDatabase db = BiomeDatabases.getDefault();

        final BiomePropertySelectors.SpeciesSelector selector = db.getSpecies(biome);
        if (selector == null) {
            source.sendSuccess(() -> Component.literal("dtgen: this biome has no Dynamic Trees species selector at all"
                    + " (Dynamic Trees is not managing it)."), false);
            return 0;
        }

        final float forestness = db.getForestness(biome);
        final BiomePropertySelectors.DensitySelector density = db.getDensitySelector(biome);
        final RandomSource rand = level.getRandom();
        final BlockState soil = level.getBlockState(pos.below());

        // Sample the species selector the way the tree feature does: at varied positions within a chunk, one draw
        // each. A weighted-random pool returns a fresh pick per call; position variance also covers selectors that
        // key their result off coordinates.
        final Map<String, Integer> counts = new TreeMap<>();
        int unhandled = 0;
        int none = 0;
        for (int i = 0; i < samples; i++) {
            BlockPos probe = pos.offset(rand.nextInt(16) - 8, 0, rand.nextInt(16) - 8);
            BiomePropertySelectors.SpeciesSelection selection = selector.getSpecies(probe, soil, rand);
            if (!selection.isHandled()) {
                unhandled++;
                continue;
            }
            Species species = selection.getSpecies();
            if (species == null || !species.isValid()) {
                none++;
                continue;
            }
            counts.merge(species.getRegistryName().toString(), 1, Integer::sum);
        }

        double densitySample = density == null ? Double.NaN : density.getDensity(new MathContext(pos, rand));
        String biomeId = biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        final int totalSamples = samples;
        final int totalUnhandled = unhandled;
        final int totalNone = none;

        final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed());

        source.sendSuccess(() -> Component.literal("=== dtgen: biome " + biomeId + " ==="), false);
        source.sendSuccess(() -> Component.literal("forestness=" + fmt(forestness) + "  density@you=" + fmt(densitySample)), false);
        source.sendSuccess(() -> Component.literal("samples=" + totalSamples + "  unhandled=" + totalUnhandled + "  none/invalid=" + totalNone), false);

        if (sorted.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No species assigned - the populator is NOT reaching this biome."
                    + " (olive/ironwood will never spawn here)"), false);
        } else {
            int shown = 0;
            for (Map.Entry<String, Integer> entry : sorted) {
                if (shown++ >= 12) {
                    break;
                }
                final String name = entry.getKey();
                final int count = entry.getValue();
                final double pct = 100.0 * count / samples;
                source.sendSuccess(() -> Component.literal(String.format("  %-28s %5d  %5.1f%%", name, count, pct)), false);
            }
        }

        // Prove at runtime whether Dynamic Trees can resolve our species by the exact lookup its populator parser
        // uses (Species.findSpeciesSloppy -> parseDTLocation -> REGISTRY.has). If these are valid, an empty rustic
        // pool is NOT the cause and the failure must be structural in how rustic.json is deserialized.
        probeRusticSpecies(source);

        // Reflectively dump Dynamic Trees' per-biome JsonEntry templates so we can SEE whether the rustic populator
        // is present for this biome and whether DT's own default template is applied after (and so overwrites) it.
        dumpMatchingTemplates(source, db, biome.unwrapKey().orElse(null));

        // Selection clearly resolves to olive/ironwood, yet a fresh world has no trees at all. The tree feature runs
        // on the LOGICAL SERVER, so replay its post-selection gates there: soil acceptance and a real world-gen
        // generate() attempt. This tells us whether the failure is the species builder (generate returns false /
        // throws) or is entirely outside the populator (species generate fine, so DT is not running its tree feature
        // for this biome at all - e.g. the AddDynamicTrees biome modifier / DT config toggle).
        probeGeneration(source, biome, pos);
        return 1;
    }

    /**
     * Replays the exact placement steps {@code DynamicTreeFeature.genTree} takes AFTER it has selected a species, so a
     * "selected in the histogram but nothing spawns in the world" case can be attributed to soil, to the species
     * builder, or ruled out entirely. Non-destructive for the soil check; the generate step is run on a scratch
     * position a few blocks away and is wrapped so a builder exception is reported instead of crashing the command.
     */
    private static void probeGeneration(CommandSourceStack source, Holder<Biome> biome, BlockPos playerPos) {
        source.sendSuccess(() -> Component.literal("--- generation probe (server-side, mirrors DynamicTreeFeature.genTree) ---"), false);
        final ServerLevel serverLevel;
        try {
            serverLevel = source.getServer().overworld();
        } catch (Throwable t) {
            source.sendSuccess(() -> Component.literal("  could not obtain a ServerLevel: " + t), false);
            return;
        }
        for (String id : new String[]{"rustic:olive", "rustic:ironwood"}) {
            Species species = Species.findSpeciesSloppy(id);
            if (species == null || !species.isValid()) {
                source.sendSuccess(() -> Component.literal("  " + id + ": not resolvable, skipping"), false);
                continue;
            }
            // Find a surface (soil) position a handful of blocks ahead of the player: walk down from +6 to -8 and take
            // the first solid block whose top neighbour is air, i.e. a place a tree could root.
            BlockPos soilPos = findSurface(serverLevel, playerPos);
            if (soilPos == null) {
                source.sendSuccess(() -> Component.literal("  " + id + ": could not locate a surface block near the player"), false);
                continue;
            }
            final BlockState soilState = serverLevel.getBlockState(soilPos);
            final String soilId = soilState.getBlock().getDescriptionId();
            boolean soilOk;
            try {
                soilOk = species.isAcceptableSoilForWorldgen(serverLevel, soilPos, soilState);
            } catch (Throwable t) {
                soilOk = false;
                source.sendSuccess(() -> Component.literal("  " + id + " isAcceptableSoilForWorldgen threw: " + t), false);
            }
            final boolean soilVerdict = soilOk;
            source.sendSuccess(() -> Component.literal("  " + id + " @" + soilPos.getX() + "," + soilPos.getY() + "," + soilPos.getZ()
                    + " soil=" + soilId + "  isAcceptableSoilForWorldgen=" + soilVerdict), false);
            if (!soilVerdict) {
                continue; // genTree would stop here with FAIL_SOIL, so a generate attempt would be misleading.
            }
            // Actually run world-gen generation. If this plants a tree, the species builder is fine and the reason the
            // world is empty is that DT's tree feature is not being invoked for this biome (data/config, not species).
            try {
                LevelContext ctx = LevelContext.create(serverLevel);
                DynamicTreeGenerationContext genCtx = new DynamicTreeGenerationContext(
                        ctx, species, soilPos, soilPos.mutable(), biome, Direction.NORTH, 6, true);
                boolean placed = species.generate(genCtx);
                source.sendSuccess(() -> Component.literal("      generate(worldGen=true) -> "
                        + (placed ? "TRUE (a tree WAS built here - species builder is healthy; the empty world means "
                        + "DT's tree feature is not running for this biome)" : "FALSE (the species builder refused to "
                        + "generate a tree in world-gen - this is why the world has none)")
                        + (placed ? " << a test tree should be visible; break it if you do not want it" : "")), false);
            } catch (Throwable t) {
                source.sendSuccess(() -> Component.literal("      generate(worldGen=true) threw: " + t), false);
            }
        }
    }

    /** Walks a short column near {@code origin} to find a solid block with air above (a plausible tree surface). */
    private static BlockPos findSurface(ServerLevel level, BlockPos origin) {
        BlockPos probe = origin.offset(4, 6, 4);
        for (int dy = 0; dy <= 14; dy++) {
            BlockPos p = probe.below(dy);
            BlockState s = level.getBlockState(p);
            if (s.isAir()) {
                continue;
            }
            if (s.isSolid() && level.getBlockState(p.above()).isAir()) {
                return p;
            }
        }
        return null;
    }

    /** Runs Dynamic Trees' own species lookup against the two ids our populator references and reports the verdict. */
    private static void probeRusticSpecies(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("--- findSpeciesSloppy probe (what the populator parser uses) ---"), false);
        for (String id : new String[]{"rustic:olive", "rustic:ironwood", "olive", "ironwood"}) {
            Species s = Species.findSpeciesSloppy(id);
            final boolean valid = s != null && s.isValid();
            final String reg = valid ? String.valueOf(s.getRegistryName()) : "<null/invalid>";
            source.sendSuccess(() -> Component.literal("  findSpeciesSloppy(\"" + id + "\") -> "
                    + (valid ? "VALID  " + reg : "NULL_SPECIES (would be skipped from the random pool)")), false);
        }
    }

    /**
     * Reads Dynamic Trees' private {@code BiomeDatabase.jsonEntries} (a {@link java.util.LinkedHashMap}, so its
     * iteration order is insertion order = populator read order) and prints every template that covers the given biome,
     * with its species pool, its merge {@code Operation}, and its position. A higher position is copied into the biome
     * later and therefore wins: if the {@code dynamictrees} default template sits at a higher index than the {@code rustic}
     * one and uses REPLACE, it wipes the rustic species, which is the natural-generation failure being diagnosed.
     *
     * <p>Reflection is used deliberately - this is a development diagnostic, not shipped logic, and the fields it reads
     * are Dynamic Trees' private world-gen state with no public accessor. Every reflective read is guarded so the command
     * still prints the histogram even if the DT internals differ from what is expected.
     */
    private static void dumpMatchingTemplates(CommandSourceStack source, BiomeDatabase db, ResourceKey<Biome> biomeKey) {
        source.sendSuccess(() -> Component.literal("--- JsonEntry templates (#N = apply order, higher wins) ---"), false);
        if (biomeKey == null) {
            source.sendSuccess(() -> Component.literal("  biome has no registry key; cannot match templates"), false);
            return;
        }
        try {
            Field jeField = BiomeDatabase.class.getDeclaredField("jsonEntries");
            jeField.setAccessible(true);
            Map<?, ?> jsonEntries = (Map<?, ?>) jeField.get(db);
            int index = 0;
            int matched = 0;
            int rusticRefs = 0;
            for (Map.Entry<?, ?> entry : jsonEntries.entrySet()) {
                index++;
                if (!(entry.getKey() instanceof IDTBiomeHolderSet holderSet)) {
                    continue;
                }
                final boolean match = holderSet.containsKey(biomeKey);
                if (match) {
                    matched++;
                }
                Object template = entry.getValue();
                final Object op = readField(template, "speciesSelectorOp");
                final boolean changed = Boolean.TRUE.equals(readField(template, "changedSpeciesSelector"));
                final Object selector = readField(template, "speciesSelector");
                final String pool = describeSelector(selector);
                final boolean splice = op != null && op.toString().startsWith("SPLICE");
                final boolean mentionsRustic = pool.contains("rustic:") || splice;
                if (mentionsRustic) {
                    rusticRefs++;
                }
                // How many biomes this template's holder-set resolves to, plus its include/exclude component count,
                // so a template that silently resolved to an empty/wrong set is visible even when it does not match.
                final long biomeCount = holderSet.stream().count();
                final int includes = holderSet.getIncludeComponents().size();
                final int excludes = holderSet.getExcludeComponents().size();
                final boolean detailed = match || mentionsRustic;
                final int pos = index;
                // Print the biome-matching templates and any that look like our splice in full; every other
                // species-changing template gets a terse line so the total picture (did our entries create ANY
                // template?) is visible without drowning the reader in DT's own pools.
                if (detailed) {
                    source.sendSuccess(() -> Component.literal("  #" + pos + (match ? " [MATCH]" : "        ")
                            + "  speciesChanged=" + changed + "  op=" + op
                            + "  biomes=" + biomeCount + " inc=" + includes + " exc=" + excludes
                            + "  pool=" + pool), false);
                } else if (changed) {
                    source.sendSuccess(() -> Component.literal("  #" + pos + "        speciesChanged=true op=" + op
                            + "  biomes=" + biomeCount + "  pool=" + pool), false);
                }
            }
            final int total = index;
            final int hits = matched;
            final int rustic = rusticRefs;
            source.sendSuccess(() -> Component.literal("  (" + hits + " of " + total + " templates target this biome; "
                    + rustic + " template(s) look like our rustic splice (rustic pool or SPLICE op))"), false);
            if (rustic == 0) {
                source.sendSuccess(() -> Component.literal("  => NO rustic splice anywhere in jsonEntries: the "
                        + "rustic.json apply block never registered a selector (parse/shape issue), NOT a select/tag issue."), false);
            }
        } catch (Throwable t) {
            source.sendSuccess(() -> Component.literal("  reflection into BiomeDatabase failed: " + t), false);
        }
    }

    /** Renders a species selector's pool when it is Dynamic Trees' random-weighted table; otherwise its class name. */
    private static String describeSelector(Object selector) {
        if (selector == null) {
            return "null";
        }
        Object decisionTable = readField(selector, "decisionTable");
        if (decisionTable instanceof List<?> rows) {
            StringBuilder sb = new StringBuilder("random[");
            for (Object row : rows) {
                Object weight = readField(row, "weight");
                Object decision = readField(row, "decision");
                Species species = decision instanceof BiomePropertySelectors.SpeciesSelection ss ? ss.getSpecies() : null;
                if (sb.length() > 7) {
                    sb.append(", ");
                }
                sb.append(species == null ? "<unhandled>" : species.getRegistryName())
                        .append(':').append(weight);
            }
            return sb.append(']').toString();
        }
        // A spliced chain is a composed lambda with no readable table; report the type so it is recognisable.
        return "<" + selector.getClass().getName() + ">";
    }

    /** Reads a (possibly private, possibly inherited) field reflectively, returning null when absent or inaccessible. */
    private static Object readField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private static String fmt(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format("%.3f", value);
    }
}
