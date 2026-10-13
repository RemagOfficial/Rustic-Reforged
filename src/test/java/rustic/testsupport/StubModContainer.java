package rustic.testsupport;

import java.io.File;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.eventbus.EventBus;

import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionRange;

/**
 * Minimal {@link ModContainer} used only by the Phase 0 characterization tests.
 *
 * Forge 1.12.2 resolves a bare registry name ("planks") against
 * {@code Loader.instance().activeModContainer()}, and refuses to register anything when no
 * mod container is active ("Could not determine owner for the override on minecraft:planks").
 * A headless JUnit run never goes through the FML mod-loading pipeline, so this stub supplies
 * the mod id the registration code expects.
 */
public class StubModContainer implements ModContainer {

	private final String modId;
	private final ModMetadata metadata = new ModMetadata();

	public StubModContainer(String modId) {
		this.modId = modId;
		this.metadata.modId = modId;
		this.metadata.name = modId;
	}

	@Override
	public String getModId() {
		return modId;
	}

	@Override
	public String getName() {
		return modId;
	}

	@Override
	public String getVersion() {
		return "1.3.0";
	}

	@Override
	public File getSource() {
		return new File(".");
	}

	@Override
	public ModMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void bindMetadata(MetadataCollection mc) {
	}

	@Override
	public void setEnabledState(boolean enabled) {
	}

	@Override
	public Set<ArtifactVersion> getRequirements() {
		return Collections.emptySet();
	}

	@Override
	public List<ArtifactVersion> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public List<ArtifactVersion> getDependants() {
		return Collections.emptyList();
	}

	@Override
	public String getSortingRules() {
		return "";
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		return true;
	}

	@Override
	public boolean matches(Object mod) {
		return false;
	}

	@Override
	public Object getMod() {
		return null;
	}

	@Override
	public ArtifactVersion getProcessedVersion() {
		return null;
	}

	@Override
	public boolean isImmutable() {
		return false;
	}

	@Override
	public String getDisplayVersion() {
		return "1.3.0";
	}

	@Override
	public VersionRange acceptableMinecraftVersionRange() {
		return null;
	}

	@Override
	public Certificate getSigningCertificate() {
		return null;
	}

	@Override
	public Map<String, String> getCustomModProperties() {
		return EMPTY_PROPERTIES;
	}

	@Override
	public Class<?> getCustomResourcePackClass() {
		return null;
	}

	@Override
	public Map<String, String> getSharedModDescriptor() {
		return null;
	}

	@Override
	public Disableable canBeDisabled() {
		return Disableable.YES;
	}

	@Override
	public String getGuiClassName() {
		return null;
	}

	@Override
	public List<String> getOwnedPackages() {
		return Collections.singletonList("rustic");
	}

	@Override
	public boolean shouldLoadInEnvironment() {
		return true;
	}

	@Override
	public URL getUpdateUrl() {
		return null;
	}

	@Override
	public void setClassVersion(int classVersion) {
	}

	@Override
	public int getClassVersion() {
		return 52;
	}

}
