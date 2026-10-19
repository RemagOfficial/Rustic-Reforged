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

public class RusticFluidRegistrationTest {

    private record Spec(DeferredHolder<Fluid, ? extends BaseFlowingFluid> holder, String legacyName, int density,
                        int viscosity, Rarity rarity, boolean bucketAble, boolean placeable, int tickRate,
                        float inebriationChance) {
    }

    private static final List<Spec> SPECS = List.of(

            new Spec(RusticFluids.OLIVE_OIL, "oliveoil", 920, 2000, Rarity.COMMON, true, true, 10, 0F),
            new Spec(RusticFluids.IRONBERRY_JUICE, "ironberryjuice", 1100, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.WILDBERRY_JUICE, "wildberryjuice", 1070, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.GRAPE_JUICE, "grapejuice", 1070, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.APPLE_JUICE, "applejuice", 1050, 1100, Rarity.COMMON, true, true, 5, 0F),
            new Spec(RusticFluids.ALE_WORT, "alewort", 1004, 2000, Rarity.COMMON, true, true, 10, 0F),
            new Spec(RusticFluids.HONEY, "honey", 1433, 5500, Rarity.COMMON, true, true, 27, 0F),
            new Spec(RusticFluids.GOLDEN_APPLE_JUICE, "goldenapplejuice", 1050, 1100, Rarity.EPIC, true, true, 5, 0F),

            new Spec(RusticFluids.VANTA_OIL, "vantaoil", 920, 2000, Rarity.COMMON, true, false, 5, 0F),

            new Spec(RusticFluids.ALE, "ale", 1004, 1016, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.CIDER, "cider", 1004, 1400, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.IRON_WINE, "ironwine", 1034, 1400, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.MEAD, "mead", 1034, 1500, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.WILDBERRY_WINE, "wildberrywine", 1034, 1500, Rarity.COMMON, false, false, 5, 0.85F),
            new Spec(RusticFluids.WINE, "wine", 1034, 1500, Rarity.COMMON, false, false, 5, 0.5F),
            new Spec(RusticFluids.AMBROSIA, "ambrosia", 1004, 1400, Rarity.EPIC, false, false, 5, 1.0F));

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

    @Test
    public void densityAndViscosityMatchTheShippedValues() {
        for (Spec spec : SPECS) {
            FluidType type = spec.holder().value().getFluidType();
            String id = spec.legacyName();
            assertEquals(spec.density(), type.getDensity(), id + " density");
            assertEquals(spec.viscosity(), type.getViscosity(), id + " viscosity");
        }
    }

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

    @Test
    public void onlyGoldenAppleJuiceAndAmbrosiaAreEpic() {
        for (Spec spec : SPECS) {
            FluidType type = spec.holder().value().getFluidType();
            assertSame(spec.rarity(), type.getRarity(), spec.legacyName() + " rarity");
        }
    }

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

    @Test
    public void fluidBlocksKeepTheirShippedFlowTickRate() {
        for (Spec spec : SPECS) {
            if (spec.placeable()) {

                assertEquals(spec.tickRate(), spec.holder().value().getTickDelay(null),
                        spec.legacyName() + " flow tick rate");
            }
        }
    }

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

    @Test
    public void everyRusticFluidBlockLinksBackToItsFluid() {
        assertSame(RusticFluidBlocks.OLIVE_OIL.value(), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("rustic:olive_oil")));

        for (var entry : RusticFluidBlocks.entries()) {
            assertTrue(entry.value() instanceof net.minecraft.world.level.block.LiquidBlock,
                    entry.getId() + " must be a LiquidBlock (water-like, non-opaque, model-render)");
        }
        assertEquals(8, RusticFluidBlocks.entries().size(), "eight placeable fluids, eight blocks");
    }

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
