package rustic.testsupport;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;

import net.minecraft.init.Bootstrap;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import rustic.core.CommonProxy;
import rustic.common.Config;
import rustic.common.blocks.ModBlocks;
import rustic.common.blocks.fluids.ModFluids;
import rustic.common.crafting.Recipes;
import rustic.common.items.ModItems;
import rustic.common.potions.PotionsRustic;

/**
 * Shared headless bootstrap for the Phase 0 characterization tests.
 *
 * It reproduces, without a game or the FML launch wrapper, the pieces of the 1.12.2 startup
 * sequence that Rustic's static registration code depends on:
 *
 * <ul>
 * <li>{@link Bootstrap#register()} - vanilla registries, must run before any Block/Item access;</li>
 * <li>{@link Loader#setupTestHarness(net.minecraftforge.fml.common.ModContainer...)} - gives Forge an
 * active mod container, which {@code ForgeRegistry.add} needs to resolve unqualified registry names
 * ("planks" -> "rustic:planks" is impossible without one);</li>
 * <li>a fake mc home + config dir, because {@code Configuration} reads
 * {@code FMLInjectionData.data()[6]} in its constructor;</li>
 * <li>the universal bucket, which {@code FluidUtil.getFilledBucket} (used by the ale wort recipe)
 * takes from the {@code ForgeModContainer} singleton that only the real mod loader creates;</li>
 * <li>{@code Config.readConfig()} against a fresh, empty rustic.cfg, so every config-driven recipe
 * sees the shipped defaults.</li>
 * </ul>
 *
 * Registration order mirrors {@code CommonProxy.preInit} plus the recipe registry events from
 * {@code Rustic}, because several behaviours (condenser/brewing recipe lists, ore dictionary
 * wiring, smelting and fuel tables) only exist once those have run.
 */
public final class RusticContent {

	private static boolean ready;

	private RusticContent() {
	}

	/** Idempotent: safe to call from every test class' setup. */
	public static synchronized void init() {
		if (ready) {
			return;
		}
		try {
			File mcHome = Files.createTempDirectory("rustic-test-mchome").toFile();
			File configDir = new File(mcHome, "config");
			configDir.mkdirs();
			// Configuration#runConfiguration resolves paths relative to FMLInjectionData.data()[6]
			setStatic(FMLInjectionData.class, "minecraftHome", mcHome);

			Bootstrap.register();

			Loader loader = Loader.instance();
			loader.setupTestHarness(new StubModContainer("rustic"));
			setField(loader, "canonicalConfigDir", configDir);

			FluidRegistry.enableUniversalBucket();
			createForgeModContainer();

			CommonProxy.config = new Configuration(new File(configDir, "rustic.cfg"));
			Config.readConfig();
			// readConfig swallows every exception, so verify it actually populated the defaults
			if (Config.MAX_BREW_TIME <= 0 || !Config.ENABLE_SLATE || Config.VANTA_OIL_WHITELIST.isEmpty()) {
				throw new IllegalStateException("Config.readConfig() failed headlessly: "
						+ "MAX_BREW_TIME=" + Config.MAX_BREW_TIME + " ENABLE_SLATE=" + Config.ENABLE_SLATE
						+ " VANTA_OIL_WHITELIST=" + Config.VANTA_OIL_WHITELIST);
			}

			ModFluids.init();
			ModBlocks.init();
			ModItems.init();
			PotionsRustic.init();

			Recipes.initOres();
			Recipes.init();
			Recipes.postInit();
			// Rustic.postInit: re-injects evaporating recipes added straight into the data structure
			Recipes.injectEvaporatingRecipes();

			ready = true;
		} catch (ReflectiveOperationException | java.io.IOException e) {
			throw new IllegalStateException("Could not bootstrap Rustic headlessly", e);
		}
	}

	/**
	 * {@code ForgeModContainer} is only instantiated by FML, and {@code universalBucket} is only
	 * assigned while it handles the item registry event. Both are needed by
	 * {@code FluidUtil.getFilledBucket}; the bucket item itself never enters a registry here.
	 */
	private static void createForgeModContainer() throws ReflectiveOperationException {
		Constructor<ForgeModContainer> ctor = ForgeModContainer.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		ForgeModContainer container = ctor.newInstance();
		setStatic(ForgeModContainer.class, "INSTANCE", container);
		setField(container, "universalBucket", new UniversalBucket());
	}

	private static void setStatic(Class<?> type, String name, Object value) throws ReflectiveOperationException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

}
