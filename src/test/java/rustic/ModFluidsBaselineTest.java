package rustic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.registry.GameRegistry;
import rustic.common.blocks.fluids.BlockFluidRustic;
import rustic.common.blocks.fluids.FluidBooze;
import rustic.common.blocks.fluids.FluidDrinkable;
import rustic.common.blocks.fluids.ModFluids;
import rustic.testsupport.RusticContent;

/**
 * Phase 0 baseline for the fluid registry content: names, physical properties, rarity, bucket
 * availability and the eight flowing-block definitions. These are the numbers a 1.21 port has to
 * reproduce exactly (JEI tooltips, mod interactions like theTinkers-style material stats and any
 * density-driven flow behaviour all read them straight off the Fluid instance).
 */
public class ModFluidsBaselineTest {

	@BeforeClass
	public static void bootstrapRustic() {
		RusticContent.init();
	}

	// ---------------------------------------------------------------- identity

	@Test
	public void everyFluidIsRegisteredUnderItsExactName() {
		String[] names = { "oliveoil", "ironberryjuice", "wildberryjuice", "grapejuice", "applejuice", "alewort",
				"honey", "goldenapplejuice", "vantaoil", "ale", "cider", "ironwine", "mead", "wildberrywine", "wine",
				"ambrosia" };
		Fluid[] fields = { ModFluids.OLIVE_OIL, ModFluids.IRONBERRY_JUICE, ModFluids.WILDBERRY_JUICE,
				ModFluids.GRAPE_JUICE, ModFluids.APPLE_JUICE, ModFluids.ALE_WORT, ModFluids.HONEY,
				ModFluids.GOLDEN_APPLE_JUICE, ModFluids.VANTA_OIL, ModFluids.ALE, ModFluids.CIDER,
				ModFluids.IRON_WINE, ModFluids.MEAD, ModFluids.WILDBERRY_WINE, ModFluids.WINE, ModFluids.AMBROSIA };

		assertEquals(names.length, fields.length);
		for (int i = 0; i < names.length; i++) {
			assertNotNull(fields[i] + " must exist", fields[i]);
			assertEquals("field name mismatch at index " + i, names[i], fields[i].getName());
			// recipes and saved NBT resolve fluids by name, so the registry instance must be identical
			assertSame("registry lookup by name must return the same instance for " + names[i], fields[i],
					FluidRegistry.getFluid(names[i]));
		}

		List<Fluid> tracked = ModFluids.getFluids();
		assertEquals(16, tracked.size());
		assertSame(fields[0], tracked.get(0));
		assertSame(fields[names.length - 1], tracked.get(tracked.size() - 1));
	}

	@Test
	public void getFluidsHandsOutACopy() {
		List<Fluid> first = ModFluids.getFluids();
		int size = first.size();
		first.clear();
		assertEquals("clearing the returned list must not shrink the internal one", size, ModFluids.getFluids().size());
	}

	@Test
	public void everyRusticFluidIsDrinkableAndOnlySevenAreBooze() {
		int boozeCount = 0;
		for (Fluid fluid : ModFluids.getFluids()) {
			assertTrue(fluid.getName() + " must extend FluidDrinkable so the bottle item can call onDrank",
					fluid instanceof FluidDrinkable);
			if (fluid instanceof FluidBooze) {
				boozeCount++;
			}
		}
		assertEquals("ale, cider, ironwine, mead, wildberrywine, wine, ambrosia", 7, boozeCount);
	}

	// ---------------------------------------------------------------- physical properties

	@Test
	public void densityAndViscosityMatchTheShippedValues() {
		assertPhysics(ModFluids.OLIVE_OIL, 920, 2000);
		assertPhysics(ModFluids.IRONBERRY_JUICE, 1100, 1100);
		assertPhysics(ModFluids.WILDBERRY_JUICE, 1070, 1100);
		assertPhysics(ModFluids.GRAPE_JUICE, 1070, 1100);
		assertPhysics(ModFluids.APPLE_JUICE, 1050, 1100);
		assertPhysics(ModFluids.ALE_WORT, 1004, 2000);
		assertPhysics(ModFluids.HONEY, 1433, 5500);
		assertPhysics(ModFluids.GOLDEN_APPLE_JUICE, 1050, 1100);
		assertPhysics(ModFluids.VANTA_OIL, 920, 2000);
		assertPhysics(ModFluids.ALE, 1004, 1016);
		assertPhysics(ModFluids.CIDER, 1004, 1400);
		assertPhysics(ModFluids.IRON_WINE, 1034, 1400);
		assertPhysics(ModFluids.MEAD, 1034, 1500);
		assertPhysics(ModFluids.WILDBERRY_WINE, 1034, 1500);
		assertPhysics(ModFluids.WINE, 1034, 1500);
		assertPhysics(ModFluids.AMBROSIA, 1004, 1400);
	}

	/**
	 * Honey is the thickest and olive oil / vanta oil the lightest: several compat behaviours
	 * (flow speed, the honey entity-slowdown, barrel fill rates) key off these extremes.
	 */
	@Test
	public void densityAndViscosityKeepTheirRelativeOrder() {
		assertTrue(ModFluids.HONEY.getViscosity() > ModFluids.OLIVE_OIL.getViscosity());
		assertTrue(ModFluids.HONEY.getDensity() > ModFluids.OLIVE_OIL.getDensity());
		assertTrue(ModFluids.VANTA_OIL.getDensity() == ModFluids.OLIVE_OIL.getDensity());
		assertTrue(ModFluids.VANTA_OIL.getViscosity() == ModFluids.OLIVE_OIL.getViscosity());
		assertTrue(ModFluids.IRONBERRY_JUICE.getDensity() > ModFluids.ALE_WORT.getDensity());
	}

	@Test
	public void untouchedFluidDefaultsAreStillVanillaForge() {
		// the port must not accidentally inherit 1.21 defaults instead of these
		for (Fluid fluid : ModFluids.getFluids()) {
			assertEquals(fluid.getName() + " luminosity", 0, fluid.getLuminosity());
			assertEquals(fluid.getName() + " temperature", 300, fluid.getTemperature());
			assertFalse(fluid.getName() + " isGaseous", fluid.isGaseous());
			assertNull(fluid.getName() + " must not override the still texture lookup", fluid.getOverlay());
		}
	}

	@Test
	public void stillAndFlowingTexturesUseThePortedAssetLayout() {
		assertTexture(ModFluids.OLIVE_OIL, "rustic:blocks/fluids/olive_oil_still", "rustic:blocks/fluids/olive_oil_flow");
		assertTexture(ModFluids.IRONBERRY_JUICE, "rustic:blocks/fluids/ironberry_juice_still",
				"rustic:blocks/fluids/ironberry_juice_flow");
		assertTexture(ModFluids.WILDBERRY_JUICE, "rustic:blocks/fluids/wildberry_juice_still",
				"rustic:blocks/fluids/wildberry_juice_flow");
		assertTexture(ModFluids.GRAPE_JUICE, "rustic:blocks/fluids/grape_juice_still",
				"rustic:blocks/fluids/grape_juice_flow");
		assertTexture(ModFluids.APPLE_JUICE, "rustic:blocks/fluids/apple_juice_still",
				"rustic:blocks/fluids/apple_juice_flow");
		assertTexture(ModFluids.ALE_WORT, "rustic:blocks/fluids/ale_wort_still", "rustic:blocks/fluids/ale_wort_flow");
		assertTexture(ModFluids.HONEY, "rustic:blocks/fluids/honey_still", "rustic:blocks/fluids/honey_flow");
		assertTexture(ModFluids.GOLDEN_APPLE_JUICE, "rustic:blocks/fluids/golden_apple_juice_still",
				"rustic:blocks/fluids/golden_apple_juice_flow");
		assertTexture(ModFluids.VANTA_OIL, "rustic:blocks/fluids/vanta_oil_still", "rustic:blocks/fluids/vanta_oil_flow");
		assertTexture(ModFluids.ALE, "rustic:blocks/fluids/booze/ale_still", "rustic:blocks/fluids/booze/ale_flow");
		assertTexture(ModFluids.CIDER, "rustic:blocks/fluids/booze/cider_still",
				"rustic:blocks/fluids/booze/cider_flow");
		assertTexture(ModFluids.IRON_WINE, "rustic:blocks/fluids/booze/iron_wine_still",
				"rustic:blocks/fluids/booze/iron_wine_flow");
		assertTexture(ModFluids.MEAD, "rustic:blocks/fluids/booze/mead_still", "rustic:blocks/fluids/booze/mead_flow");
		assertTexture(ModFluids.WILDBERRY_WINE, "rustic:blocks/fluids/booze/wildberry_wine_still",
				"rustic:blocks/fluids/booze/wildberry_wine_flow");
		assertTexture(ModFluids.WINE, "rustic:blocks/fluids/booze/wine_still", "rustic:blocks/fluids/booze/wine_flow");
		assertTexture(ModFluids.AMBROSIA, "rustic:blocks/fluids/booze/ambrosia_still",
				"rustic:blocks/fluids/booze/ambrosia_flow");
	}

	/**
	 * Not a single Rustic fluid sets custom fill/empty sounds, so all sixteen fall back to the vanilla
	 * water bucket sounds. The port should keep this unless the sound mapping changes in 1.21.
	 */
	@Test
	public void noFluidCustomizesTheBucketSounds() {
		Fluid water = FluidRegistry.WATER;
		for (Fluid fluid : ModFluids.getFluids()) {
			assertSame(fluid.getName() + " fill sound", water.getFillSound(), fluid.getFillSound());
			assertSame(fluid.getName() + " empty sound", water.getEmptySound(), fluid.getEmptySound());
			assertEquals(ModFluids.ALE.getFillSound(), fluid.getFillSound());
			assertEquals(ModFluids.ALE.getEmptySound(), fluid.getEmptySound());
		}
		// lava proves the fallback really is material sensitive and the assertion above is not vacuous
		assertNotEquals(FluidRegistry.LAVA.getFillSound(), water.getFillSound());
	}

	// ---------------------------------------------------------------- rarity

	@Test
	public void onlyGoldenAppleJuiceAndAmbrosiaAreEpic() {
		for (Fluid fluid : ModFluids.getFluids()) {
			EnumRarity expected = EnumRarity.COMMON;
			if (fluid == ModFluids.GOLDEN_APPLE_JUICE || fluid == ModFluids.AMBROSIA) {
				expected = EnumRarity.EPIC;
			}
			assertEquals(fluid.getName() + " rarity", expected, fluid.getRarity());
			assertEquals(fluid.getName() + " rarity via stack overload", expected,
					fluid.getRarity(new FluidStack(fluid, 1)));
		}
	}

	// ---------------------------------------------------------------- buckets

	/**
	 * Rustic calls {@code FluidRegistry.enableUniversalBucket()} in preInit, so every bucket-able fluid is
	 * handed back as a Forge universal bucket rather than a per-fluid ItemBucket. The machine containers
	 * rely on that (they all special-case {@code UniversalBucket}).
	 */
	@Test
	public void juicesGetUniversalBucketsButBoozeDeliberatelyDoesNot() {
		assertTrue("Rustic enables the universal bucket in Rustic.preInit", FluidRegistry.isUniversalBucketEnabled());
		Fluid[] withBucket = { ModFluids.OLIVE_OIL, ModFluids.IRONBERRY_JUICE, ModFluids.WILDBERRY_JUICE,
				ModFluids.GRAPE_JUICE, ModFluids.APPLE_JUICE, ModFluids.ALE_WORT, ModFluids.HONEY,
				ModFluids.GOLDEN_APPLE_JUICE, ModFluids.VANTA_OIL };
		for (Fluid fluid : withBucket) {
			assertTrue(fluid.getName() + " should be bucket-able", FluidRegistry.hasBucket(fluid));
			ItemStack bucket = FluidUtil.getFilledBucket(new FluidStack(fluid, 1000));
			assertNotNull(fluid.getName() + " filled bucket", bucket);
			assertTrue(fluid.getName() + " comes back as a universal bucket, not a vanilla one",
					bucket.getItem() instanceof UniversalBucket);
			assertNotEquals(Items.BUCKET, bucket.getItem());
			// read the contents off the item itself: FluidUtil.getFluidContained needs the fluid handler
			// capability, which a headless JVM never registers
			FluidStack contained = ((UniversalBucket) bucket.getItem()).getFluid(bucket);
			assertSame(fluid, contained.getFluid());
			assertEquals(1000, contained.amount);
			assertEquals(1000, ((UniversalBucket) bucket.getItem()).getCapacity());
		}

		for (Fluid fluid : ModFluids.getFluids()) {
			if (fluid instanceof FluidBooze) {
				assertFalse(fluid.getName() + " must stay un-bucket-able, booze is only served in bottles",
						FluidRegistry.hasBucket(fluid));
				ItemStack rejected = FluidUtil.getFilledBucket(new FluidStack(fluid, 1000));
				assertTrue(fluid.getName() + " getFilledBucket hands back an empty (air) stack, not null",
						rejected == null || rejected.isEmpty());
			}
		}
	}

	// ---------------------------------------------------------------- booze tuning

	@Test
	public void inebriationChancesMatchTheShippedValues() {
		assertEquals(0.5F, ((FluidBooze) ModFluids.ALE).getInebriationChance(), 0.0001F);
		assertEquals(0.5F, ((FluidBooze) ModFluids.CIDER).getInebriationChance(), 0.0001F);
		assertEquals(0.5F, ((FluidBooze) ModFluids.IRON_WINE).getInebriationChance(), 0.0001F);
		assertEquals(0.5F, ((FluidBooze) ModFluids.MEAD).getInebriationChance(), 0.0001F);
		assertEquals("wildberry wine gets you drunk far more easily", 0.85F,
				((FluidBooze) ModFluids.WILDBERRY_WINE).getInebriationChance(), 0.0001F);
		assertEquals(0.5F, ((FluidBooze) ModFluids.WINE).getInebriationChance(), 0.0001F);
		assertEquals("ambrosia always gets you drunk", 1.0F,
				((FluidBooze) ModFluids.AMBROSIA).getInebriationChance(), 0.0001F);
	}

	@Test
	public void boozeWithoutAnExplicitChanceDefaultsToHalf() {
		FluidBooze anonymous = new FluidBooze("baseline_chance_default",
				new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow")) {
		};
		assertEquals(0.5F, anonymous.getInebriationChance(), 0.0001F);
		assertSame(anonymous, anonymous.setInebriationChance(0.1F));
		assertEquals(0.1F, anonymous.getInebriationChance(), 0.0001F);
	}

	// ---------------------------------------------------------------- flowing blocks

	@Test
	public void fluidBlocksKeepTheirShippedQuantaPerBlock() {
		assertEquals(4, quanta(ModFluids.BLOCK_OLIVE_OIL));
		assertEquals(6, quanta(ModFluids.BLOCK_IRONBERRY_JUICE));
		assertEquals(6, quanta(ModFluids.BLOCK_WILDBERRY_JUICE));
		assertEquals(6, quanta(ModFluids.BLOCK_GRAPE_JUICE));
		assertEquals(6, quanta(ModFluids.BLOCK_APPLE_JUICE));
		assertEquals(4, quanta(ModFluids.BLOCK_ALE_WORT));
		assertEquals("honey only ever shows two levels, which is what makes it look thick", 2,
				quanta(ModFluids.BLOCK_HONEY));
		assertEquals(6, quanta(ModFluids.BLOCK_GOLDEN_APPLE_JUICE));
	}

	@Test
	public void fluidBlocksAreRegisteredAndLinkBackToTheirFluid() {
		BlockFluidRustic[] blocks = { ModFluids.BLOCK_OLIVE_OIL, ModFluids.BLOCK_IRONBERRY_JUICE,
				ModFluids.BLOCK_WILDBERRY_JUICE, ModFluids.BLOCK_GRAPE_JUICE, ModFluids.BLOCK_APPLE_JUICE,
				ModFluids.BLOCK_ALE_WORT, ModFluids.BLOCK_HONEY, ModFluids.BLOCK_GOLDEN_APPLE_JUICE };
		String[] names = { "olive_oil", "ironberry_juice", "wildberry_juice", "grape_juice", "apple_juice", "ale_wort",
				"honey", "golden_apple_juice" };

		for (int i = 0; i < blocks.length; i++) {
			BlockFluidRustic block = blocks[i];
			assertEquals(names[i], block.getRegistryName().getResourcePath());
			assertEquals("tile.rustic." + names[i], block.getUnlocalizedName());
			assertSame("fluid.getBlock() must point back at the block", block, block.getFluid().getBlock());
			assertSame(block.getFluid(), FluidRegistry.getFluid(block.getFluid().getName()));
			assertEquals("every rustic fluid block is water-based", Material.WATER,
					block.getDefaultState().getMaterial());
			assertFalse(block.isOpaqueCube(block.getDefaultState()));
			assertFalse(block.isFullCube(block.getDefaultState()));
			assertEquals(EnumBlockRenderType.MODEL, block.getRenderType(block.getDefaultState()));

			Item item = GameRegistry.findRegistry(Item.class).getValue(block.getRegistryName());
			assertTrue(names[i] + " needs an ItemBlock or the fluid block cannot be given to the player",
					item instanceof ItemBlock);
			assertSame(block, ((ItemBlock) item).getBlock());
		}
	}

	/**
	 * The boozes and vanta oil have no BlockFluidRustic at all: they can only exist inside
	 * barrels/bottles, never as a placed fluid. A port that "helpfully" registers flowing booze
	 * blocks would change this baseline.
	 */
	@Test
	public void boozeAndVantaOilHaveNoFlowingBlock() {
		assertNull(ModFluids.ALE.getBlock());
		assertNull(ModFluids.CIDER.getBlock());
		assertNull(ModFluids.IRON_WINE.getBlock());
		assertNull(ModFluids.MEAD.getBlock());
		assertNull(ModFluids.WILDBERRY_WINE.getBlock());
		assertNull(ModFluids.WINE.getBlock());
		assertNull(ModFluids.AMBROSIA.getBlock());
		assertNull(ModFluids.VANTA_OIL.getBlock());
		for (Fluid fluid : ModFluids.getFluids()) {
			if (fluid instanceof FluidBooze || fluid == ModFluids.VANTA_OIL) {
				assertNull(fluid.getName() + " must have no flowing block", fluid.getBlock());
			}
		}
	}

	/** BlockFluidBase keeps tickRate protected with no stable getter, so the baseline reads the field. */
	@Test
	public void flowTickRateIsDerivedFromViscosity()
	{
		assertEquals(10, tickRate(ModFluids.BLOCK_OLIVE_OIL));
		assertEquals(5, tickRate(ModFluids.BLOCK_IRONBERRY_JUICE));
		assertEquals(5, tickRate(ModFluids.BLOCK_WILDBERRY_JUICE));
		assertEquals(5, tickRate(ModFluids.BLOCK_GRAPE_JUICE));
		assertEquals(5, tickRate(ModFluids.BLOCK_APPLE_JUICE));
		assertEquals("ale wort is as sluggish as olive oil", 10, tickRate(ModFluids.BLOCK_ALE_WORT));
		assertEquals("honey flows at a third of the speed of the juices", 27, tickRate(ModFluids.BLOCK_HONEY));
		assertEquals(5, tickRate(ModFluids.BLOCK_GOLDEN_APPLE_JUICE));
	}

	// ---------------------------------------------------------------- helpers

	private static int tickRate(BlockFluidBase block) {
		try {
			Field field = BlockFluidBase.class.getDeclaredField("tickRate");
			field.setAccessible(true);
			return field.getInt(block);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not read tickRate from " + block, e);
		}
	}

	private static void assertPhysics(Fluid fluid, int density, int viscosity) {
		assertEquals(fluid.getName() + " density", density, fluid.getDensity());
		assertEquals(fluid.getName() + " viscosity", viscosity, fluid.getViscosity());
	}

	private static void assertTexture(Fluid fluid, String still, String flowing) {
		assertEquals(new ResourceLocation(still), fluid.getStill());
		assertEquals(new ResourceLocation(flowing), fluid.getFlowing());
	}

	/** BlockFluidBase keeps quantaPerBlock protected with no getter, so the baseline reads the field. */
	private static int quanta(BlockFluidBase block) {
		try {
			Field field = BlockFluidBase.class.getDeclaredField("quantaPerBlock");
			field.setAccessible(true);
			return field.getInt(block);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not read quantaPerBlock from " + block, e);
		}
	}

}
