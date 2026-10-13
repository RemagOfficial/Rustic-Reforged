package rustic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import rustic.common.Config;
import rustic.common.blocks.fluids.FluidBooze;
import rustic.common.blocks.fluids.ModFluids;
import rustic.common.crafting.AdvancedCondenserRecipe;
import rustic.common.crafting.BasicCondenserRecipe;
import rustic.common.crafting.BrewingBarrelRecipe;
import rustic.common.crafting.CondenserRecipe;
import rustic.common.crafting.CrushingTubRecipe;
import rustic.common.crafting.EvaporatingBasinRecipe;
import rustic.testsupport.RusticContent;

/**
 * Phase 0 baseline for the four hand-written machine recipe types.
 *
 * Everything here is matching semantics that the 1.21 data-driven port has to reproduce (or
 * deliberately fix): what counts as an ingredient match, what the time/amount formulas are, and
 * which quirks are load-bearing. Known latent bugs are pinned as-is rather than "fixed", so the
 * port is an explicit decision instead of an accident.
 */
public class CustomRecipeMatchingBaselineTest {

	private static FluidBooze juice;
	private static FluidBooze booze;

	private int savedMinBrewQualityChange;
	private int savedMaxBrewQualityChange;

	@BeforeClass
	public static void bootstrapRustic() {
		RusticContent.init();
		juice = new FluidBooze("baseline_juice", new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow"));
		booze = new FluidBooze("baseline_booze2", new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow"));
		FluidRegistry.registerFluid(juice);
		FluidRegistry.registerFluid(booze);
	}

	@Before
	public void rememberBrewConfig() {
		savedMinBrewQualityChange = Config.MIN_BREW_QUALITY_CHANGE;
		savedMaxBrewQualityChange = Config.MAX_BREW_QUALITY_CHANGE;
	}

	@After
	public void restoreBrewConfig() {
		Config.MIN_BREW_QUALITY_CHANGE = savedMinBrewQualityChange;
		Config.MAX_BREW_QUALITY_CHANGE = savedMaxBrewQualityChange;
	}

	// ---------------------------------------------------------------- crushing tub

	@Test
	public void crushingTubMatchesOnItemAndMetadataOnly() {
		CrushingTubRecipe recipe = new CrushingTubRecipe(new FluidStack(FluidRegistry.WATER, 100),
				new ItemStack(Items.REEDS));

		assertTrue(recipe.matches(new ItemStack(Items.REEDS)));
		assertFalse(recipe.matches(new ItemStack(net.minecraft.init.Blocks.CACTUS)));
		assertFalse(recipe.matches(new ItemStack(Items.SUGAR)));

		// stack size and NBT are deliberately ignored by the tub
		ItemStack sixtyFour = new ItemStack(Items.REEDS, 64);
		assertTrue(recipe.matches(sixtyFour));
		ItemStack tagged = new ItemStack(Items.REEDS);
		tagged.setTagCompound(new NBTTagCompound());
		assertTrue("the tub never inspects the tag compound", recipe.matches(tagged));

		// metadata does matter, so a 0-damage input never matches a 1-damage candidate
		CrushingTubRecipe metaRecipe = new CrushingTubRecipe(new FluidStack(FluidRegistry.WATER, 100),
				new ItemStack(Items.FISH, 1, 1));
		assertTrue(metaRecipe.matches(new ItemStack(Items.FISH, 1, 1)));
		assertFalse(metaRecipe.matches(new ItemStack(Items.FISH, 1, 0)));
	}

	@Test
	public void crushingTubResultIsLookedUpFromTheFluidRegistryByName() {
		CrushingTubRecipe recipe = new CrushingTubRecipe(new FluidStack(juice, 250), new ItemStack(Items.REEDS));

		FluidStack result = recipe.getResult();
		assertNotNull(result);
		assertEquals("baseline_juice", result.getFluid().getName());
		assertEquals(250, result.amount);
		assertSame("result must be the registered instance, not the recipe's private copy",
				FluidRegistry.getFluid("baseline_juice"), result.getFluid());
	}

	@Test
	public void crushingTubByproductDefaultsToEmptyAndCopiesOnRead() {
		CrushingTubRecipe plain = new CrushingTubRecipe(new FluidStack(FluidRegistry.WATER, 100),
				new ItemStack(Items.REEDS));
		assertTrue(plain.getByproduct().isEmpty());

		ItemStack seed = new ItemStack(Items.WHEAT_SEEDS);
		CrushingTubRecipe withByproduct = new CrushingTubRecipe(new FluidStack(FluidRegistry.WATER, 100),
				new ItemStack(Items.REEDS), seed);
		assertEquals(ItemStack.class, withByproduct.getByproduct().getClass());
		assertEquals(Items.WHEAT_SEEDS, withByproduct.getByproduct().getItem());

		// getInput/getByproduct hand out copies, so machine code cannot corrupt the recipe
		withByproduct.getByproduct().grow(64);
		assertEquals(1, withByproduct.getByproduct().getCount());
		withByproduct.getInput().grow(64);
		assertEquals(1, withByproduct.getInput().getCount());
	}

	@Test
	public void fluidStackRefusesUnregisteredFluids() {
		// FluidStack's own constructor rejects fluids that are not in the registry, so the
		// "unregistered output" branch is only reachable through a null output.
		Fluid unregistered = new Fluid("baseline_unregistered", new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow"));
		try {
			new FluidStack(unregistered, 250);
			org.junit.Assert.fail("expected IllegalArgumentException for an unregistered fluid");
		} catch (IllegalArgumentException expected) {
			assertEquals("Cannot create a fluidstack from an unregistered fluid", expected.getMessage());
		}

		CrushingTubRecipe nullOutput = new CrushingTubRecipe(null, new ItemStack(Items.REEDS));
		assertNull(nullOutput.getResult());
	}

	// ---------------------------------------------------------------- evaporating basin

	@Test
	public void evaporatingBasinTimeEqualsInputAmount() {
		EvaporatingBasinRecipe recipe = new EvaporatingBasinRecipe(new ItemStack(Items.SUGAR),
				new FluidStack(FluidRegistry.WATER, 750));

		assertEquals(750, recipe.getTime());
		assertEquals(750, recipe.getAmount());
		assertEquals(FluidRegistry.WATER, recipe.getFluid());
		assertEquals(Items.SUGAR, recipe.getOutput().getItem());

		// the map is keyed by Fluid, and getTime() == amount is what makes 1000mb slow
		EvaporatingBasinRecipe quick = new EvaporatingBasinRecipe(new ItemStack(Items.SUGAR),
				new FluidStack(FluidRegistry.WATER, 100));
		assertEquals(100, quick.getTime());
	}

	@Test
	public void evaporatingBasinHandsOutCopies() {
		EvaporatingBasinRecipe recipe = new EvaporatingBasinRecipe(new ItemStack(Items.SUGAR, 2),
				new FluidStack(FluidRegistry.WATER, 500));

		recipe.getOutput().grow(10);
		assertEquals(2, recipe.getOutput().getCount());
		recipe.getInput().amount = 1;
		assertEquals(500, recipe.getAmount());
	}

	// ---------------------------------------------------------------- brewing barrel

	@Test
	public void brewingBarrelMatchesByFluidNameAndAuxMustBeTheOutputFluid() {
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		assertTrue(recipe.matches(new FluidStack(juice, 1)));
		assertFalse(recipe.matches(new FluidStack(FluidRegistry.WATER, 1000)));
		assertFalse(recipe.matches(null));

		// any amount of the right input fluid matches
		assertTrue(recipe.matches(new FluidStack(FluidRegistry.getFluid("baseline_juice"), 1)));

		// aux re-brewing requires the aux to be the *output* fluid instance
		assertTrue(recipe.matches(new FluidStack(juice, 1000), new FluidStack(booze, 100)));
		assertFalse(recipe.matches(new FluidStack(juice, 1000), new FluidStack(juice, 100)));
		assertFalse(recipe.matches(new FluidStack(FluidRegistry.WATER, 1000), new FluidStack(booze, 100)));
		// a null aux degenerates to the plain input check
		assertTrue(recipe.matches(new FluidStack(juice, 1000), null));
		assertFalse(recipe.matches(new FluidStack(FluidRegistry.WATER, 1000), null));
	}

	@Test
	public void freshBrewQualityIsFiveToSeventyFivePercent() {
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		FluidStack result = recipe.getResult(new FluidStack(juice, 1000));
		assertNotNull(result);
		assertNotNull("a fresh booze output always carries a Quality tag", result.tag);
		assertTrue(result.tag.hasKey(FluidBooze.QUALITY_NBT_KEY, 5));

		float min = 1F;
		float max = 0F;
		for (int i = 0; i < 20000; i++) {
			float quality = recipe.getResult(new FluidStack(juice, 1000)).tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
			assertTrue("quality must land on a 1% step, got " + quality,
					Math.abs(quality * 100F - Math.round(quality * 100F)) < 0.001F);
			min = Math.min(min, quality);
			max = Math.max(max, quality);
		}
		assertEquals(0.05F, min, 0.0001F);
		assertEquals(0.75F, max, 0.0001F);

		assertNull(recipe.getResult(new FluidStack(FluidRegistry.WATER, 1000)));
	}

	@Test
	public void reBrewShiftsQualityByTheConfiguredPercentRange() {
		Config.MIN_BREW_QUALITY_CHANGE = -1;
		Config.MAX_BREW_QUALITY_CHANGE = 4;
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		float min = 1F;
		float max = 0F;
		for (int i = 0; i < 20000; i++) {
			FluidStack result = recipe.getResult(new FluidStack(juice, 1000), taggedBooze(0.5F));
			float quality = result.tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
			min = Math.min(min, quality);
			max = Math.max(max, quality);
		}
		// (rand[-1..4] + (int)(100 * 0.5)) / 100
		assertEquals(0.49F, min, 0.0001F);
		assertEquals(0.54F, max, 0.0001F);
	}

	@Test
	public void reBrewQualityIsClampedAndCanSaturate() {
		Config.MIN_BREW_QUALITY_CHANGE = -1;
		Config.MAX_BREW_QUALITY_CHANGE = 4;
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		FluidStack fromZero = recipe.getResult(new FluidStack(juice, 1000), taggedBooze(0.0F));
		assertTrue(fromZero.tag.getFloat(FluidBooze.QUALITY_NBT_KEY) >= 0.0F);
		assertTrue("a dead brew can only crawl upwards by the configured change, got "
				+ fromZero.tag.getFloat(FluidBooze.QUALITY_NBT_KEY),
				fromZero.tag.getFloat(FluidBooze.QUALITY_NBT_KEY) <= 0.04F);

		FluidStack fromOne = recipe.getResult(new FluidStack(juice, 1000), taggedBooze(1.0F));
		assertTrue(fromOne.tag.getFloat(FluidBooze.QUALITY_NBT_KEY) <= 1.0F);
		assertTrue("perfect brew can only drop by the configured change, got "
				+ fromOne.tag.getFloat(FluidBooze.QUALITY_NBT_KEY),
				fromOne.tag.getFloat(FluidBooze.QUALITY_NBT_KEY) >= 0.99F);
	}

	@Test
	public void reBrewWithoutQualityTagOnAuxProducesUntaggedBooze() {
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		FluidStack noTag = recipe.getResult(new FluidStack(juice, 1000), new FluidStack(booze, 100));
		assertNotNull(noTag);
		assertNull("an aux with no tag at all yields output with no Quality -> reads as 0%", noTag.tag);
		assertEquals(0.0F, booze.getQuality(noTag), 0.0F);
	}

	@Test
	public void reBrewQualityPresenceCheckIgnoresTheNbtType() {
		// LATENT INCONSISTENCY, pinned: BrewingBarrelRecipe tests hasKey("Quality") WITHOUT a type
		// filter, while FluidBooze.getQuality only reads type 5. A non-float "Quality" therefore takes
		// the re-brew branch and is silently read as 0.0, so a perfect brew can be destroyed by it.
		Config.MIN_BREW_QUALITY_CHANGE = -1;
		Config.MAX_BREW_QUALITY_CHANGE = 4;
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		FluidStack wrongTypeTag = new FluidStack(booze, 100);
		NBTTagCompound stringTag = new NBTTagCompound();
		stringTag.setString(FluidBooze.QUALITY_NBT_KEY, "0.5");
		wrongTypeTag.tag = stringTag;

		FluidStack result = recipe.getResult(new FluidStack(juice, 1000), wrongTypeTag);
		assertNotNull("a string Quality tag still counts as \"has quality\"", result.tag);
		float quality = result.tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
		assertTrue("expected 0..0.04 from a 0.0 reading plus the -1..4 change, got " + quality,
				quality >= 0.0F && quality <= 0.04F);
	}

	@Test
	public void invertedBrewConfigSilentlyPinsMaxToMin() {
		// MAX < MIN: the recipe overwrites the static config field instead of failing.
		// This mutation outlives the call, so @After has to restore both values.
		Config.MIN_BREW_QUALITY_CHANGE = 5;
		Config.MAX_BREW_QUALITY_CHANGE = 1;
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 1000));

		FluidStack result = recipe.getResult(new FluidStack(juice, 1000), taggedBooze(0.5F));
		assertEquals(5, Config.MAX_BREW_QUALITY_CHANGE);
		assertEquals(0.55F, result.tag.getFloat(FluidBooze.QUALITY_NBT_KEY), 0.0001F);
	}

	@Test
	public void brewingBarrelInputAndOutputAreCopies() {
		BrewingBarrelRecipe recipe = new BrewingBarrelRecipe(new FluidStack(booze, 1000),
				new FluidStack(juice, 250));

		assertEquals(250, recipe.getInput().amount);
		assertEquals(1000, recipe.getOuput().amount);
		recipe.getInput().amount = 1;
		assertEquals(250, recipe.getInput().amount);
	}

	// ---------------------------------------------------------------- condenser

	@Test
	public void basicCondenserNeedsWaterGlassBottleAndAnEmptyModifierSlot() {
		BasicCondenserRecipe recipe = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				new ItemStack(Items.SUGAR), new ItemStack(Items.SUGAR));

		ItemStack[] inputs = { new ItemStack(Items.SUGAR), new ItemStack(Items.SUGAR), ItemStack.EMPTY };
		assertTrue(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE), inputs));

		assertFalse(recipe.matches(FluidRegistry.LAVA, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE), inputs));
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.REEDS), inputs));
		// a basic condenser refuses to run while something sits in the modifier slot
		assertFalse(recipe.matches(FluidRegistry.WATER, new ItemStack(Items.COAL), new ItemStack(Items.GLASS_BOTTLE),
				inputs));
		// no inputs at all
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[0]));
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY }));
	}

	@Test
	public void condenserMatchingIsOrderIndependentAndRejectsExtras() {
		BasicCondenserRecipe recipe = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));

		assertTrue(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { new ItemStack(Items.BEEF), new ItemStack(Items.SUGAR) }));
		// one ingredient missing
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { new ItemStack(Items.BEEF), ItemStack.EMPTY }));
		// duplicate instead of the second ingredient
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { new ItemStack(Items.BEEF), new ItemStack(Items.BEEF) }));
		// an unexpected third ingredient
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { new ItemStack(Items.BEEF), new ItemStack(Items.SUGAR), new ItemStack(Items.BREAD) }));
	}

	@Test
	public void condenserMatchingIgnoresStackButNotRecipeTagMismatch() {
		BasicCondenserRecipe recipe = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));

		ItemStack taggedBeef = new ItemStack(Items.BEEF);
		taggedBeef.setTagCompound(new NBTTagCompound());
		// recipe input is untagged: the tag on the candidate is ignored (the !temp.hasTagCompound() branch)
		assertTrue(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { taggedBeef, new ItemStack(Items.SUGAR) }));
	}

	@Test
	public void condenserWithTaggedRecipeInputsNeverMatches() {
		// LATENT BUG, pinned deliberately: the tagged branch calls tempInputs.remove(stack) with the
		// *candidate* stack, and ItemStack has no equals() override in 1.12.2, so the required entry is
		// never removed and matches() can never return true once the recipe input carries a tag.
		ItemStack taggedIngredient = new ItemStack(Items.SUGAR);
		taggedIngredient.setTagCompound(new NBTTagCompound());
		BasicCondenserRecipe recipe = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				taggedIngredient, new ItemStack(Items.BEEF));

		ItemStack candidateCopy = new ItemStack(Items.SUGAR);
		candidateCopy.setTagCompound(new NBTTagCompound());
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack[] { candidateCopy, new ItemStack(Items.BEEF) }));
	}

	@Test
	public void advancedCondenserChecksTheModifierFirst() {
		AdvancedCondenserRecipe recipe = new AdvancedCondenserRecipe(new PotionEffect(MobEffects.SPEED, 9600),
				new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));

		ItemStack[] inputs = { new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF) };
		assertTrue(recipe.matches(FluidRegistry.WATER, new ItemStack(Items.GLOWSTONE_DUST),
				new ItemStack(Items.GLASS_BOTTLE), inputs));
		assertFalse(recipe.matches(FluidRegistry.WATER, new ItemStack(Items.COAL), new ItemStack(Items.GLASS_BOTTLE),
				inputs));
		assertFalse(recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE), inputs));

		// modifier tag rules are stricter than the ingredient ones: a tagged modifier needs a tagged match
		ItemStack taggedModifier = new ItemStack(Items.GLOWSTONE_DUST);
		taggedModifier.setTagCompound(new NBTTagCompound());
		AdvancedCondenserRecipe taggedRecipe = new AdvancedCondenserRecipe(new PotionEffect(MobEffects.SPEED, 9600),
				taggedModifier, new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));
		ItemStack plainModifier = new ItemStack(Items.GLOWSTONE_DUST);
		assertFalse(taggedRecipe.matches(FluidRegistry.WATER, plainModifier, new ItemStack(Items.GLASS_BOTTLE), inputs));
		ItemStack matchingModifier = new ItemStack(Items.GLOWSTONE_DUST);
		matchingModifier.setTagCompound(new NBTTagCompound());
		assertTrue("an equal (here: both empty) modifier tag passes the strict comparison and the recipe matches",
				taggedRecipe.matches(FluidRegistry.WATER, matchingModifier, new ItemStack(Items.GLASS_BOTTLE), inputs));
	}

	@Test
	public void condenserTimingFluidAndConsumptionContracts() {
		BasicCondenserRecipe basic = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));
		AdvancedCondenserRecipe advanced = new AdvancedCondenserRecipe(new PotionEffect(MobEffects.SPEED, 9600),
				new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));

		assertEquals(400, basic.getTime());
		assertEquals(300, advanced.getTime());
		assertTrue(basic.isBasic());
		assertFalse(basic.isAdvanced());
		assertTrue(advanced.isAdvanced());
		assertFalse(advanced.isBasic());

		FluidStack water = basic.getFluid();
		assertEquals(FluidRegistry.WATER, water.getFluid());
		assertEquals(125, water.amount);
		assertEquals(1, basic.getBottleConsumption(new ItemStack(Items.GLASS_BOTTLE)));
		assertEquals(1, basic.getModifierConsumption(ItemStack.EMPTY));
		assertEquals(3, basic.getInputConsumption(new ItemStack[0]).length);
		assertEquals(1, basic.getBottles().size());
		assertNull("a basic recipe has no modifier slot to advertise", basic.getModifiers());

		// an elixir built from a PotionEffect keeps the effect in the standard ElixirEffects tag
		ItemStack result = basic.getResult();
		assertSame(rustic.common.items.ModItems.ELIXIR, result.getItem());
		assertTrue(result.hasTagCompound());
		assertTrue(result.getTagCompound().hasKey("ElixirEffects", 9));
	}

	@Test
	public void registeredCondenserTableKeepsItsShape() {
		int basicCount = 0;
		int advancedCount = 0;
		for (rustic.common.crafting.ICondenserRecipe recipe : rustic.common.crafting.Recipes.getCondenserRecipe()) {
			if (recipe.isBasic()) {
				basicCount++;
			}
			if (recipe.isAdvanced()) {
				advancedCount++;
			}
		}
		assertEquals(31, rustic.common.crafting.Recipes.getCondenserRecipe().size());
		assertEquals(31, basicCount + advancedCount);
		// the shipped table is 5 basic single-effect elixirs and 26 advanced ones
		assertEquals(5, basicCount);
		assertEquals(26, advancedCount);
	}

	@Test
	public void ambrosiaBrewingRecipeOverridesTheQualityRoll() {
		// golden_apple_juice -> ambrosia is not a plain BrewingBarrelRecipe: fresh brews start high
		// (49-74%) and re-brewing never lowers quality, it only ever adds 0-7%
		rustic.common.crafting.IBrewingBarrelRecipe ambrosia = null;
		for (rustic.common.crafting.IBrewingBarrelRecipe recipe : rustic.common.crafting.Recipes.getBrewingRecipes()) {
			if ("ambrosia".equals(recipe.getOuput().getFluid().getName())) {
				ambrosia = recipe;
			}
		}
		assertNotNull("the ambrosia recipe must stay registered", ambrosia);
		assertEquals("goldenapplejuice", ambrosia.getInput().getFluid().getName());

		float min = 1F;
		float max = 0F;
		for (int i = 0; i < 40000; i++) {
			float quality = ambrosia.getResult(new FluidStack(ModFluids.GOLDEN_APPLE_JUICE, 1000)).tag
					.getFloat(FluidBooze.QUALITY_NBT_KEY);
			min = Math.min(min, quality);
			max = Math.max(max, quality);
		}
		assertEquals(0.49F, min, 0.0001F);
		assertEquals(0.74F, max, 0.0001F);
	}

	@Test
	public void registeredCrushingAndEvaporatingTablesArePopulated() {
		assertEquals(10, rustic.common.crafting.Recipes.getCrushingRecipes().size());
		assertEquals(2, rustic.common.crafting.Recipes.getEvaporatingRecipes().size());
		assertEquals(7, rustic.common.crafting.Recipes.getBrewingRecipes().size());

		for (rustic.common.crafting.ICrushingTubRecipe recipe : rustic.common.crafting.Recipes.getCrushingRecipes()) {
			assertNotNull("crushing tub output must resolve against the fluid registry", recipe.getResult());
			assertNotNull(recipe.getInput());
		}
	}

	/**
	 * Seven of the shipped advanced recipes are built with an {@code ItemStack.EMPTY} modifier (fire
	 * resistance, health boost, haste, strength, iron skin, feather fall, blazing trail), so they run in
	 * the advanced condenser without needing a modifier item. The port must keep that empty-modifier path
	 * working, otherwise seven elixirs silently become uncraftable.
	 */
	@Test
	public void advancedRecipesWithAnEmptyModifierRunOnAnEmptyModifierSlot() {
		rustic.common.crafting.ICondenserRecipe sample = null;
		int emptyModifierCount = 0;
		for (rustic.common.crafting.ICondenserRecipe recipe : rustic.common.crafting.Recipes.getCondenserRecipe()) {
			java.util.List<ItemStack> modifiers = recipe.getModifiers();
			if (modifiers != null && modifiers.size() == 1 && modifiers.get(0).isEmpty()) {
				emptyModifierCount++;
				if (sample == null) {
					sample = recipe;
				}
			}
		}
		assertEquals(7, emptyModifierCount);
		assertNotNull(sample);
		assertTrue(sample.isAdvanced());
		assertFalse(sample.isBasic());
		assertEquals(300, sample.getTime());

		ItemStack[] candidates = new ItemStack[sample.getInputs().size()];
		for (int i = 0; i < candidates.length; i++) {
			candidates[i] = sample.getInputs().get(i).get(0).copy();
		}
		assertTrue("an empty-modifier advanced recipe must accept an empty modifier slot",
				sample.matches(FluidRegistry.WATER, ItemStack.EMPTY, new ItemStack(Items.GLASS_BOTTLE), candidates));
		assertFalse("...and must refuse once something is sitting in that slot",
				sample.matches(FluidRegistry.WATER, new ItemStack(Items.COAL), new ItemStack(Items.GLASS_BOTTLE),
						candidates));
	}

	private static FluidStack taggedBooze(float quality) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setFloat(FluidBooze.QUALITY_NBT_KEY, quality);
		return new FluidStack(booze, 1000, tag);
	}

	@Test
	public void condenserRecipeFieldTypesAreExposedThroughInterfacesOnly() {
		// guards against a port accidentally changing getInputs() into a flat list
		CondenserRecipe recipe = new BasicCondenserRecipe(new PotionEffect(MobEffects.SPEED, 3600),
				new ItemStack(Items.SUGAR), new ItemStack(Items.BEEF));
		assertEquals(2, recipe.getInputs().size());
		assertEquals(1, recipe.getInputs().get(0).size());
		assertEquals(Items.SUGAR, recipe.getInputs().get(0).get(0).getItem());
	}

}
