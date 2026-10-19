package rustic.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigPreread {

    private static final Logger LOGGER = LoggerFactory.getLogger("rustic");

    private static final String COMMON_FILE = "rustic-common.toml";

    public static boolean flag(String section, String key, boolean fallback) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(COMMON_FILE);
        if (!Files.isRegularFile(path)) {

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
