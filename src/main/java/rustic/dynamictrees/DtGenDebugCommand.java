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

public final class DtGenDebugCommand {

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

        probeRusticSpecies(source);

        dumpMatchingTemplates(source, db, biome.unwrapKey().orElse(null));

        probeGeneration(source, biome, pos);
        return 1;
    }

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
                continue;

            }

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

                final long biomeCount = holderSet.stream().count();
                final int includes = holderSet.getIncludeComponents().size();
                final int excludes = holderSet.getExcludeComponents().size();
                final boolean detailed = match || mentionsRustic;
                final int pos = index;

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

        return "<" + selector.getClass().getName() + ">";
    }

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
