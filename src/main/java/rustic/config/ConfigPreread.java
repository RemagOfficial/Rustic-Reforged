package rustic.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads one boolean out of the mod's own TOML file before {@link RusticConfig} is loadable.
 *
 * <p>NeoForge loads a {@code ModConfigSpec} after the registry events have run, so a
 * {@code BooleanValue} cannot be consulted from a block's registration. 1.12.2 did exactly that for
 * {@code enable_silver_decoration_blocks}: the six silver decor blocks were registered inside an
 * {@code if (Config.ENABLE_SILVER_DECOR)} and did not exist at all when the flag was off. Keeping that
 * behaviour - rather than registering the blocks and hiding them from the creative tab, which is what
 * every other {@code enable_*} flag now does - means reading the file directly. The values NeoForge
 * writes into it are not live yet at that point, so this sees the file from the previous run: turning
 * the flag off takes one restart to take effect, which is also exactly what 1.12.2 did, and the first
 * run simply has no file and falls through to the default.
 *
 * <p>NightConfig is the parser {@code ModConfigSpec} itself is built on, so the file is read the same
 * way the game will read it later: sections, quoting and commented-out lines all behave identically.
 */
public final class ConfigPreread {

    private static final Logger LOGGER = LoggerFactory.getLogger("rustic");

    /** {@code ModConfig.Type.COMMON} for mod id {@code rustic}, which is where NeoForge puts it. */
    private static final String COMMON_FILE = "rustic-common.toml";

    /**
     * The value of {@code [section] key} in the config file, or {@code fallback} when the file, the
     * section or the key is missing, or when anything at all goes wrong - registration must not fail
     * because a hand-edited config file is broken, and {@link RusticConfig} will report it properly.
     */
    public static boolean flag(String section, String key, boolean fallback) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(COMMON_FILE);
        if (!Files.isRegularFile(path)) {
            // A first run: NeoForge has not written the file yet, so the spec's own default is the answer.
            return fallback;
        }
        try (FileConfig config = FileConfig.of(path)) {
            config.load();
            Boolean value = config.get(List.of(section, key));
            return value == null ? fallback : value;
        } catch (Exception exception) {
            LOGGER.warn("Could not pre-read {} from {}, falling back to {}", key, path, fallback, exception);
            return fallback;
        }
    }

    private ConfigPreread() {
    }
}
