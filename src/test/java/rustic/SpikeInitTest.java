package rustic;

import org.junit.Ignore;
import org.junit.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import rustic.client.util.ClientUtils;
import rustic.common.Config;
import rustic.common.blocks.fluids.FluidBooze;
import rustic.common.blocks.fluids.ModFluids;
import rustic.common.crafting.BrewingBarrelRecipe;
import rustic.common.crafting.CrushingTubRecipe;
import rustic.common.crafting.EvaporatingBasinRecipe;
import rustic.common.items.ModItems;
import rustic.common.util.ElixirUtils;

/**
 * Phase 0 environment probe.
 *
 * Not a real assertion test: it discovers which parts of the 1.12.2 stack can be
 * initialized headlessly (no game, no Forge lifecycle) so the characterization test
 * suite can be built on top of what actually works. Everything that fails here
 * becomes an in-game checklist entry in PORT_BASELINE.md instead of a unit test.
 */
@Ignore("build-time probe: kept for reference, the real coverage now lives in the *BaselineTest classes")
public class SpikeInitTest {

	/**
	 * Vanilla registries must be bootstrapped before any Blocks/Items/Sounds/FluidRegistry
	 * class is touched, otherwise 1.12.2 throws "Accessed X before Bootstrap!" and permanently
	 * poisons those classes for the rest of the JVM.
	 */
	@org.junit.BeforeClass
	public static void bootstrapMinecraft() {
		Bootstrap.register();
	}

	private static void probe(String label, Runnable body) {
		try {
			body.run();
			System.out.println("[PROBE-OK]   " + label);
		} catch (Throwable t) {
			System.out.println("[PROBE-FAIL] " + label + " -> " + t.getClass().getName() + ": " + t.getMessage());
			int limit = 0;
			for (StackTraceElement e : t.getStackTrace()) {
				if (limit++ >= 8) {
					break;
				}
				System.out.println("        at " + e);
			}
			for (Throwable c = t.getCause(); c != null; c = c.getCause()) {
				System.out.println("     caused by " + c.getClass().getName() + ": " + c.getMessage());
				int climit = 0;
				for (StackTraceElement e : c.getStackTrace()) {
					if (climit++ >= 6) {
						break;
					}
					System.out.println("        at " + e);
				}
			}
		}
	}

	@Test
	public void probeHeadlessInitialization() {
		final ItemStack[] holder = new ItemStack[1];

		probe("Bootstrap.register()", () -> Bootstrap.register());

		probe("vanilla ItemStack + registry name", () -> {
			holder[0] = new ItemStack(Items.IRON_INGOT);
			System.out.println("   iron_ingot registry=" + holder[0].getItem().getRegistryName()
					+ " maxStack=" + holder[0].getMaxStackSize());
		});

		probe("NBT round trip", () -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setFloat(FluidBooze.QUALITY_NBT_KEY, 0.42F);
			System.out.println("   float back=" + tag.getFloat(FluidBooze.QUALITY_NBT_KEY)
					+ " hasKey(type5)=" + tag.hasKey(FluidBooze.QUALITY_NBT_KEY, 5));
		});

		probe("FluidRegistry class init + WATER", () -> {
			System.out.println("   water=" + FluidRegistry.WATER + " density=" + FluidRegistry.WATER.getDensity()
					+ " viscosity=" + FluidRegistry.WATER.getViscosity());
		});

		probe("FluidStack of water", () -> {
			FluidStack fs = new FluidStack(FluidRegistry.WATER, 1000);
			System.out.println("   fluidstack name=" + fs.getFluid().getName() + " amount=" + fs.amount);
		});

		probe("vanilla PotionEffect registry name", () -> {
			PotionEffect pe = new PotionEffect(MobEffects.SPEED, 100, 1);
			System.out.println("   effect name=" + pe.getEffectName()
					+ " registry=" + pe.getPotion().getRegistryName()
					+ " liquidColor=" + Integer.toHexString(pe.getPotion().getLiquidColor()));
		});

		probe("ElixirUtils.addEffect/getEffects on vanilla effect", () -> {
			ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
			ElixirUtils.addEffect(new PotionEffect(MobEffects.SPEED, 100, 1), stack);
			ElixirUtils.addEffect(new PotionEffect(MobEffects.JUMP_BOOST, 200, 2), stack);
			System.out.println("   effects size=" + ElixirUtils.getEffects(stack).size()
					+ " color=" + Integer.toHexString(ElixirUtils.getColor(stack))
					+ " emptyColor=" + Integer.toHexString(ElixirUtils.getColor(new ItemStack(Items.GLASS_BOTTLE))));
		});

		probe("ElixirUtils vanta oil tag round trip", () -> {
			ItemStack stack = new ItemStack(Items.IRON_SWORD);
			ElixirUtils.setVantaOilEffect(stack, new PotionEffect(MobEffects.STRENGTH, 600, 1));
			PotionEffect read = ElixirUtils.getVantaOilEffect(stack);
			System.out.println("   read back=" + (read == null ? "null" : read.getDuration() + "/" + read.getAmplifier()));
			ElixirUtils.setVantaOilEffect(stack, null);
			System.out.println("   after clear hasTag=" + stack.hasTagCompound());
		});

		probe("ClientUtils.getQualityLabelColor boundaries", () -> {
			System.out.println("   1.00=" + Integer.toHexString(ClientUtils.getQualityLabelColor(1.00F))
					+ " 0.90=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.90F))
					+ " 0.89=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.89F))
					+ " 0.70=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.70F))
					+ " 0.50=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.50F))
					+ " 0.35=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.35F))
					+ " 0.20=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.20F))
					+ " 0.00=" + Integer.toHexString(ClientUtils.getQualityLabelColor(0.00F)));
		});

		probe("ClientUtils.getQualityTooltip (needs Minecraft instance)", () -> {
			System.out.println("   tooltip=" + ClientUtils.getQualityTooltip(0.75F));
		});

		probe("FluidBooze.getQuality clamp", () -> {
			FluidBooze boozy = new FluidBooze("probe_booze", new ResourceLocation("minecraft:blocks/water_still"),
					new ResourceLocation("minecraft:blocks/water_flow"));
			NBTTagCompound tag = new NBTTagCompound();
			tag.setFloat(FluidBooze.QUALITY_NBT_KEY, 1.7F);
			System.out.println("   over=1 clamp=" + boozy.getQuality(new FluidStack(boozy, 100, tag))
					+ " negative clamp=" + boozy.getQuality(fluidWithQuality(boozy, -0.5F))
					+ " no tag=" + boozy.getQuality(new FluidStack(boozy, 100))
					+ " wrong type=" + boozy.getQuality(fluidWithWrongType(boozy)));
		});

		probe("FluidBooze registered in FluidRegistry", () -> {
			FluidBooze boozy = new FluidBooze("probe_booze2", new ResourceLocation("minecraft:blocks/water_still"),
					new ResourceLocation("minecraft:blocks/water_flow"));
			System.out.println("   register=" + FluidRegistry.registerFluid(boozy)
					+ " lookup=" + FluidRegistry.getFluid("probe_booze2"));
		});

		probe("Config defaults via reflection on CommonProxy.config", () -> {
			try {
				java.io.File tmp = java.io.File.createTempFile("rustic-probe", ".cfg");
				tmp.deleteOnExit();
				net.minecraftforge.common.config.Configuration cfg =
						new net.minecraftforge.common.config.Configuration(tmp);
				java.lang.reflect.Field f = rustic.core.CommonProxy.class.getField("config");
				f.set(null, cfg);
				Config.readConfig();
				System.out.println("   MAX_BREW_TIME=" + Config.MAX_BREW_TIME
						+ " MIN/MAX_BREW_QUALITY_CHANGE=" + Config.MIN_BREW_QUALITY_CHANGE + "/" + Config.MAX_BREW_QUALITY_CHANGE
						+ " ENABLE_SLATE=" + Config.ENABLE_SLATE
						+ " OVERWORLD_DIMS=" + Config.OVERWORLD_GENERATION_WHITELIST
						+ " NETHER_DIMS=" + Config.NETHER_GENERATION_WHITELIST
						+ " IRONSKIN_BLACKLIST=" + Config.IRONSKIN_RENDER_ENTITY_BLACKLIST
						+ " GRAPE_TOOLS=" + Config.GRAPE_TOOL_WHITELIST);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		});

		probe("BrewingBarrelRecipe fresh-brew quality range", () -> {
			Fluid juice = new FluidBooze("probe_juice", new ResourceLocation("minecraft:blocks/water_still"),
					new ResourceLocation("minecraft:blocks/water_flow"));
			Fluid booze = new FluidBooze("probe_booze3", new ResourceLocation("minecraft:blocks/water_still"),
					new ResourceLocation("minecraft:blocks/water_flow"));
			FluidRegistry.registerFluid(juice);
			FluidRegistry.registerFluid(booze);
			BrewingBarrelRecipe r = new BrewingBarrelRecipe(new FluidStack(booze, 1000), new FluidStack(juice, 1000));
			float min = 1F, max = 0F;
			for (int i = 0; i < 20000; i++) {
				float q = r.getResult(new FluidStack(juice, 1000)).tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
				min = Math.min(min, q);
				max = Math.max(max, q);
			}
			System.out.println("   fresh quality min=" + min + " max=" + max
					+ " matchesWrongFluid=" + r.matches(new FluidStack(FluidRegistry.WATER, 100)));
		});

		probe("BrewingBarrelRecipe re-brew quality formula", () -> {
			Fluid juice = FluidRegistry.getFluid("probe_juice");
			Fluid booze = FluidRegistry.getFluid("probe_booze3");
			Config.MIN_BREW_QUALITY_CHANGE = -1;
			Config.MAX_BREW_QUALITY_CHANGE = 4;
			BrewingBarrelRecipe r = new BrewingBarrelRecipe(new FluidStack(booze, 1000), new FluidStack(juice, 1000));
			FluidStack aux = new FluidStack(booze, 1000);
			NBTTagCompound auxTag = new NBTTagCompound();
			auxTag.setFloat(FluidBooze.QUALITY_NBT_KEY, 0.5F);
			aux.tag = auxTag;
			float min = 1F, max = 0F;
			for (int i = 0; i < 20000; i++) {
				float q = r.getResult(new FluidStack(juice, 1000), aux).tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
				min = Math.min(min, q);
				max = Math.max(max, q);
			}
			System.out.println("   rebrew from 0.50 quality min=" + min + " max=" + max);
		});

		probe("CrushingTubRecipe match/result", () -> {
			CrushingTubRecipe r = new CrushingTubRecipe(new FluidStack(FluidRegistry.WATER, 100),
					new ItemStack(Items.REEDS));
			System.out.println("   match reeds=" + r.matches(new ItemStack(Items.REEDS))
					+ " match cactus=" + r.matches(new ItemStack(net.minecraft.init.Blocks.CACTUS))
					+ " result=" + r.getResult()
					+ " byproduct empty=" + r.getByproduct().isEmpty());
		});

		probe("EvaporatingBasinRecipe time/output", () -> {
			EvaporatingBasinRecipe r = new EvaporatingBasinRecipe(new ItemStack(Items.SUGAR),
					new FluidStack(FluidRegistry.WATER, 750));
			System.out.println("   time=" + r.getTime() + " amount=" + r.getAmount()
					+ " output=" + r.getOutput().getUnlocalizedName());
		});

		probe("ModFluids.init() registers rustic fluids", () -> {
			ModFluids.init();
			System.out.println("   oliveoil=" + FluidRegistry.getFluid("oliveoil")
					+ " count=" + ModFluids.getFluids().size());
		});

		probe("ModBlocks.init() then ModItems.init() headless", () -> {
			rustic.common.blocks.ModBlocks.init();
			System.out.println("   VASE=" + new ItemStack(rustic.common.blocks.ModBlocks.VASE).getUnlocalizedName());
			ModItems.init();
			System.out.println("   olives=" + new ItemStack(ModItems.OLIVES).getUnlocalizedName());
		});
	}

	/** Third probe: does the shared RusticContent harness bootstrap everything? */
	@Test
	public void probeRusticContentBootstrap() {
		probe("RusticContent.init() (all blocks/items/recipes)", () -> rustic.testsupport.RusticContent.init());

		probe("registered rustic content summary", () -> {
			System.out.println("   fluids=" + ModFluids.getFluids().size());
			System.out.println("   crushingTub=" + rustic.common.crafting.Recipes.getCrushingRecipes().size());
			System.out.println("   evaporating=" + rustic.common.crafting.Recipes.getEvaporatingRecipes().size());
			System.out.println("   condenser=" + rustic.common.crafting.Recipes.getCondenserRecipe().size());
			System.out.println("   brewing=" + rustic.common.crafting.Recipes.getBrewingRecipes().size());
			System.out.println("   config MAX_BREW_TIME=" + Config.MAX_BREW_TIME
					+ " qualityChange=" + Config.MIN_BREW_QUALITY_CHANGE + "/" + Config.MAX_BREW_QUALITY_CHANGE);
		});
	}

	@Test
	public void probeFullContentRegistration() {
		probe("Config statics set to documented defaults", () -> {
			setConfigDefaults();
			System.out.println("   ENABLE_SLATE=" + Config.ENABLE_SLATE + " ENABLE_CHAIRS=" + Config.ENABLE_CHAIRS);
		});

		probe("ModFluids.init()", () -> ModFluids.init());

		probe("ModBlocks.init()", () -> {
			rustic.common.blocks.ModBlocks.init();
			System.out.println("   VASE=" + rustic.common.blocks.ModBlocks.VASE
					+ " registry=" + rustic.common.blocks.ModBlocks.VASE.getRegistryName());
		});

		probe("ModItems.init()", () -> {
			ModItems.init();
			System.out.println("   OLIVES registry=" + ModItems.OLIVES.getRegistryName()
					+ " ELIXIR=" + ModItems.ELIXIR.getRegistryName());
		});

		probe("PotionsRustic.init()", () -> rustic.common.potions.PotionsRustic.init());

		probe("Recipes.initOres()", () -> {
			rustic.common.crafting.Recipes.initOres();
			System.out.println("   cropOlive=" + net.minecraftforge.oredict.OreDictionary
					.getOreName(net.minecraftforge.oredict.OreDictionary.getOreID("cropOlive"))
					+ " size=" + net.minecraftforge.oredict.OreDictionary.getOres("cropOlive").size());
		});

		probe("Recipes.init() (all crafting/smelting/custom)", () -> {
			rustic.common.crafting.Recipes.init();
			System.out.println("   crushingTub=" + rustic.common.crafting.Recipes.crushingTubRecipes.size()
					+ " evaporatingMap=" + rustic.common.crafting.Recipes.evaporatingRecipesMap.size()
					+ " condenser=" + rustic.common.crafting.Recipes.condenserRecipes.size()
					+ " brewing=" + rustic.common.crafting.Recipes.brewingRecipes.size()
					+ " craftingRegistry=" + rusticRecipeCount()
					+ " smelting=" + net.minecraft.item.crafting.FurnaceRecipes.instance()
							.getSmeltingList().size());
		});

		probe("postInit silver recipes + injectEvaporatingRecipes", () -> {
			rustic.common.crafting.Recipes.postInit();
			rustic.common.crafting.Recipes.injectEvaporatingRecipes();
			System.out.println("   rustic recipes after postInit=" + rusticRecipeCount());
		});
	}

	private static int rusticRecipeCount() {
		int count = 0;
		for (net.minecraft.util.ResourceLocation key : net.minecraft.item.crafting.CraftingManager.REGISTRY.getKeys()) {
			if ("rustic".equals(key.getResourceDomain())) {
				count++;
			}
		}
		return count;
	}

	private static void setConfigDefaults() {
		Config.ENABLE_SLATE = true;
		Config.ENABLE_PILLARS = true;
		Config.ENABLE_CLAY_WALLS = true;
		Config.ENABLE_PAINTED_WOOD = true;
		Config.ENABLE_TABLES = true;
		Config.ENABLE_CHAIRS = true;
		Config.ENABLE_LATTICE = true;
		Config.ENABLE_SILVER_DECOR = true;
		Config.ENABLE_SEED_DROPS = true;
		Config.SEED_DROP_RATE = 7;
		Config.ENABLE_OLIVE_OILING = true;
		Config.OLIVE_OIL_USE_WHITELIST = false;
		Config.FLESH_SMELTING = true;
		Config.NETHER_SLATE = false;
		Config.MIN_BREW_QUALITY_CHANGE = -1;
		Config.MAX_BREW_QUALITY_CHANGE = 4;
		Config.MAX_BREW_TIME = 12000;
	}

	private static FluidStack fluidWithQuality(Fluid fluid, float quality) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setFloat(FluidBooze.QUALITY_NBT_KEY, quality);
		return new FluidStack(fluid, 100, tag);
	}

	private static FluidStack fluidWithWrongType(Fluid fluid) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(FluidBooze.QUALITY_NBT_KEY, "not a float");
		return new FluidStack(fluid, 100, tag);
	}

}
