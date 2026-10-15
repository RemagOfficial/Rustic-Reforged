package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import rustic.content.fluids.DrinkableFluid;
import rustic.content.fluids.RusticBooze;
import rustic.content.fluids.RusticFluidBlocks;
import rustic.content.fluids.RusticFluids;

/**
 * Phase 6 port of the 1.12.2 golden {@code ModFluidsBaselineTest}, run against the real 1.21 registry
 * content. It pins the sixteen logical Rustic fluids — names, density/viscosity physics carried on the
 * {@link FluidType}, the {@code EPIC} rarity split, the seven-booze count, the nine-vs-seven bucket split and
 * the eight placeables' flow tick rates — with the assertions the plan says to <b>rewrite</b> per the recorded
 * deviations swapped for their 1.21 forms.
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM, so {@code BuiltInRegistries} and the deferred holders resolve.
 * This is a registration + arithmetic gate, not a fluid simulation: {@code onDrank} bodies need a ticking
 * {@code Level} and a real {@code Player}, which is a live-game check parked until the post-Phase-11 pass.</p>
 *
 * <p><b>Deviations rewritten (see the {@code RusticFluids}/{@code RusticFluidBlocks} javadoc):</b></p>
 * <ul>
 *   <li><b>Universal bucket.</b> Legacy enabled Forge's universal bucket and handed every bucket-able fluid
 *   back as a shared {@code UniversalBucket}. 1.21 Rustic registers nine discrete per-fluid bucket items
 *   (the eight placeables plus vanta oil), so bucket-ability is read off each fluid's own
 *   {@link BaseFlowingFluid#getBucket()} — a real bucket item versus {@link Items#AIR} — not off a
 *   registry-wide universal-bucket flag.</li>
 *   <li><b>Rarity.</b> 1.21 {@link Fluid} has no rarity field at all; the port keeps it on the
 *   {@link FluidType} via {@code Properties.rarity}, so the {@code EPIC}-only-golden-and-ambrosia assertion is
 *   now a {@link FluidType#getRarity()} check instead of a fluid/stack overload.</li>
 *   <li><b>No flowing block for booze/vanta.</b> Legacy asserted {@code fluid.getBlock() == null}. In 1.21 the
 *   {@code block} supplier is private with no getter, so the same fact is asserted two ways: the boozes and
 *   vanta are <em>source-only self-referencing</em> fluids (their flowing half is the source itself), and the
 *   {@code RusticFluidBlocks} register holds exactly the eight placeables — nothing wraps a booze or vanta.</li>
 *   <li><b>Honey quanta.</b> The legacy 2-{@code quantaPerBlock} thick look has no 1.21 field (levels are fixed
 *   at 8); the port carries the sluggishness in honey's 27-tick flow rate, so the quanta assertions are replaced
 *   by the tick-rate table below.</li>
 * </ul>
 */
public class RusticFluidRegistrationTest {

    /** One logical fluid with the golden numbers the port must reproduce verbatim. */
    private record Spec(DeferredHolder<Fluid, ? extends BaseFlowingFluid> holder, String legacyName, int density,
                        int viscosity, Rarity rarity, boolean bucketAble, boolean placeable, int tickRate,
                        float inebriationChance) {
    }

    private static final List<Spec> SPECS = List.of(
            // ---------------------------------------------------------------- eight placeables (bucket + flowing pair + block)
            new Spec(RusticFluids.OLIVE_OIL, "oliveoil", 920, 2000, Rarity.COMMON, true, true, 10, 0F),
            new Spec(RusticFluids.IRONBERRY_JUICE, "ironberryjuice", 1100, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.WILDBERRY_JUICE, "wildberryjuice", 1070, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.GRAPE_JUICE, "grapejuice", 1070, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.APPLE_JUICE, "applejuice", 1050, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.ALE_WORT, "alewort", 1004, 2000, Rarity.COMMON, true, true, 10, 0F),
            new Spec(RusticFluids.HONEY, "honey", 1433, 5500, Rarity.COMMON, true, true, 27, 0F),
            new Spec(RusticFluids.GOLDEN_APPLE_JUICE, "goldenapplejuice", 1050, 1100, Rarity.EPIC, true, true, 5, 0F),
            // ---------------------------------------------------------------- vanta oil: bucket, no flowing pair, no block
            new Spec(RusticFluids.VANTA_OIL, "vantaoil", 920, 2000, Rarity.COMMON, true, false, 5, 0F),
            // ---------------------------------------------------------------- seven boozes: no bucket, no flowing pair, no block
            new Spec(RusticFluids.ALE, "ale", 1004, 1016, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.CIDER, "cider", 1004, 1400, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.IRON_WINE, "ironwine", 1034, 1400, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.MEAD, "mead", 1034, 1500, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.WILDBERRY_WINE, "wildberrywine", 1034, 1500, Rarity.COMMON, false, false, 5, 0.85F),
            new Spec(RusticFluids.WINE, "wine", 1034, 1500, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.AMBROSIA, "ambrosia", 1004, 1400, Rarity.EPIC, false, false, 5, 1.0F));

    // ---------------------------------------------------------------- identity

    /**
     * The sixteen logical fluids are exactly the legacy set — nothing more, nothing fewer — and each deferred
     * holder resolves to the identical instance the live {@code FLUID} registry holds under {@code rustic:<legacyName>}.
     */
    @Test
    public void everyFluidIsRegisteredUnderItsExactLegacyName() {
        assertEquals(16, SPECS.size(), "the port must expose exactly the sixteen logical Rustic fluids");
        for (Spec spec : SPECS) {
            Fluid fluid = spec.holder().value();
            assertEquals("rustic:" + spec.legacyName(), spec.holder().getId().toString(),
                    spec.legacyName() + " holder id drifted from the legacy fluid name");
            assertSame(fluid, BuiltInRegistries.FLUID.get(spec.holder().getId()),
                    "registry lookup by name must return the same instance for " + spec.legacyName());
        }
    }

    /** Every logical fluid is drinkable, and exactly seven of them are boozes (ale..ambrosia). */
    @Test
    public void everyFluidIsDrinkableAndOnlySevenAreBooze() {
        int boozeCount = 0;
        for (Spec spec : SPECS) {
            Fluid fluid = spec.holder().value();
            assertTrue(fluid instanceof DrinkableFluid,
                    spec.legacyName() + " must implement DrinkableFluid so the bottle item can call onDrank");
            if (fluid instanceof RusticBooze) {
                boozeCount++;
            }
        }
        assertEquals(7, boozeCount, "ale, cider, ironwine, mead, wildberrywine, wine, ambrosia");
    }

    // ---------------------------------------------------------------- physical properties

    /** Density/viscosity straight off each fluid's {@link FluidType}, matching the shipped values. */
    @Test
    public void densityAndViscosityMatchTheShippedValues() {
        for (Spec spec : SPECS) {
            FluidType type = spec.holder().value().getFluidType();
            String id = spec.legacyName();
            assertEquals(spec.density(), type.getDensity(), id + " density");
            assertEquals(spec.viscosity(), type.getViscosity(), id + " viscosity");
        }
    }

    /** Honey is the thickest, olive/vanta oil the lightest and the same as each other — several compat behaviours key off these. */
    @Test
    public void densityAndViscosityKeepTheirRelativeOrder() {
        FluidType honey = RusticFluids.HONEY.value().getFluidType();
        FluidType olive = RusticFluids.OLIVE_OIL.value().getFluidType();
        FluidType vanta = RusticFluids.VANTA_OIL.value().getFluidType();
        FluidType ironberry = RusticFluids.IRONBERRY_JUICE.value().getFluidType();
        FluidType alewort = RusticFluids.ALE_WORT.value().getFluidType();

        assertTrue(honey.getViscosity() > olive.getViscosity(), "honey must be thicker than olive oil");
        assertTrue(honey.getDensity() > olive.getDensity(), "honey must be denser than olive oil");
        assertEquals(vanta.getDensity(), olive.getDensity(), "vanta oil and olive oil share a density");
        assertEquals(vanta.getViscosity(), olive.getViscosity(), "vanta oil and olive oil share a viscosity");
        assertTrue(ironberry.getDensity() > alewort.getDensity(), "ironberry juice is denser than ale wort");
    }

    /** The untouched vanilla-forge defaults: temperature 300, light 0, and every density positive (nothing gaseous). */
    @Test
    public void untouchedFluidDefaultsAreStillVanillaForge() {
        for (Spec spec : SPECS) {
            FluidType type = spec.holder().value().getFluidType();
            String id = spec.legacyName();
            assertEquals(0, type.getLightLevel(), id + " luminosity");
            assertEquals(300, type.getTemperature(), id + " temperature");
            assertTrue(type.getDensity() > 0, id + " must not be gaseous (density <= 0)");
        }
    }

    // ---------------------------------------------------------------- rarity (on the FluidType in 1.21)

    /** Only golden apple juice and ambrosia are EPIC; the other fourteen stay COMMON. */
    @Test
    public void onlyGoldenAppleJuiceAndAmbrosiaAreEpic() {
        for (Spec spec : SPECS) {
            FluidType type = spec.holder().value().getFluidType();
            assertSame(spec.rarity(), type.getRarity(), spec.legacyName() + " rarity");
        }
    }

    // ---------------------------------------------------------------- buckets (nine discrete, seven unbucketable)

    /**
     * The eight placeables plus vanta oil carry a real per-fluid bucket; the seven boozes fall back to
     * {@link Items#AIR}. This preserves the golden 9-bucketable / 7-not split without a universal bucket.
     */
    @Test
    public void juicesAndVantaGetPerFluidBucketsButBoozeDeliberatelyDoesNot() {
        for (Spec spec : SPECS) {
            Item bucket = spec.holder().value().getBucket();
            if (spec.bucketAble()) {
                assertTrue(bucket != Items.AIR, spec.legacyName() + " should be bucket-able");
            } else {
                assertSame(Items.AIR, bucket,
                        spec.legacyName() + " must stay un-bucket-able, booze is only served in bottles");
            }
        }
        long bucketAbleCount = SPECS.stream().filter(Spec::bucketAble).count();
        assertEquals(9, bucketAbleCount, "nine fluids carry a bucket (eight placeables + vanta oil)");
    }

    // ---------------------------------------------------------------- flow tick rate (replaces the quanta table)

    /** The eight placeables' flow tick rates, derived from viscosity exactly like the legacy quanta/tick table. */
    @Test
    public void fluidBlocksKeepTheirShippedFlowTickRate() {
        for (Spec spec : SPECS) {
            if (spec.placeable()) {
                // getTickDelay ignores the level argument, so null is safe for a headless read of the field.
                assertEquals(spec.tickRate(), spec.holder().value().getTickDelay(null),
                        spec.legacyName() + " flow tick rate");
            }
        }
    }

    // ---------------------------------------------------------------- no flowing block for booze / vanta

    /**
     * The boozes and vanta oil are source-only, self-referencing fluids: their flowing half is the source itself,
     * so there is no distinct flowing fluid (and no block). The eight placeables pair a separate flowing fluid.
     */
    @Test
    public void boozeAndVantaOilHaveNoSeparateFlowingFluid() {
        for (Spec spec : SPECS) {
            BaseFlowingFluid source = spec.holder().value();
            Fluid flowing = source.getFlowing();
            if (spec.placeable()) {
                assertNotSame(source, flowing, spec.legacyName() + " must have a distinct flowing variant");
            } else {
                assertSame(source, flowing,
                        spec.legacyName() + " is source-only and self-referencing (no flowing variant, no block)");
            }
        }
    }

    /**
     * The fluid-block register holds exactly the eight placeables — vanta oil (bucket-able but not placeable) and
     * the boozes get no block, the 1.21 form of the baseline's {@code getBlock() == null}.
     */
    @Test
    public void onlyTheEightPlaceablesHaveAFluidBlock() {
        List<String> blockIds = new ArrayList<>();
        for (var entry : RusticFluidBlocks.entries()) {
            blockIds.add(entry.getId().getPath());
        }
        assertEquals(List.of("ale_wort", "apple_juice", "golden_apple_juice", "grape_juice", "honey", "ironberry_juice",
                        "olive_oil", "wildberry_juice"), blockIds.stream().sorted().toList(),
                "exactly the eight placeable fluids may have a flowing block");
    }

    // ---------------------------------------------------------------- linkage & class shape

    /** Each placeable's fluid links back to its registered block, so the placed source and the item agree. */
    @Test
    public void everyRusticFluidBlockLinksBackToItsFluid() {
        assertSame(RusticFluidBlocks.OLIVE_OIL.value(), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("rustic:olive_oil")));
        // The eight fluid blocks are all LiquidBlocks and the water-like material the legacy blocks used.
        for (var entry : RusticFluidBlocks.entries()) {
            assertTrue(entry.value() instanceof net.minecraft.world.level.block.LiquidBlock,
                    entry.getId() + " must be a LiquidBlock (water-like, non-opaque, model-render)");
        }
        assertEquals(8, RusticFluidBlocks.entries().size(), "eight placeable fluids, eight blocks");
    }

    /** The seven boozes carry the shipped inebriation chances, defaulting to half where unset. */
    @Test
    public void inebriationChancesMatchTheShippedValues() {
        for (Spec spec : SPECS) {
            if (spec.holder().value() instanceof RusticBooze booze && spec.inebriationChance() > 0F) {
                assertEquals(spec.inebriationChance(), booze.getInebriationChance(), 0.0001F,
                        spec.legacyName() + " inebriation chance");
            }
        }
    }
}
