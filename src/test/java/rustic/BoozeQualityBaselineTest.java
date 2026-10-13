package rustic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import rustic.client.util.ClientUtils;
import rustic.common.blocks.fluids.FluidBooze;
import rustic.testsupport.RusticContent;

/**
 * Phase 0 baseline for booze quality: the NBT contract on a {@link FluidStack} and the quality
 * tier -> label colour mapping.
 *
 * Quality is stored as a FLOAT tag ("Quality", NBT type 5). The reader only accepts type 5, so a
 * port that writes a double/int silently turns every existing booze into 0% quality sludge. The
 * tier thresholds use the literal 0.89999F / 0.69999F bounds, not rounded values, so they are
 * pinned exactly here.
 */
public class BoozeQualityBaselineTest {

	private static FluidBooze booze;

	@BeforeClass
	public static void bootstrapRustic() {
		RusticContent.init();
		booze = new FluidBooze("baseline_booze", new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow"));
		FluidRegistry.registerFluid(booze);
	}

	@Test
	public void qualityKeyIsFloatNBT() {
		assertEquals("Quality", FluidBooze.QUALITY_NBT_KEY);

		NBTTagCompound tag = new NBTTagCompound();
		tag.setFloat(FluidBooze.QUALITY_NBT_KEY, 0.42F);
		assertTrue(tag.hasKey(FluidBooze.QUALITY_NBT_KEY, 5));
		assertFalse("quality must not be stored as a double", tag.hasKey(FluidBooze.QUALITY_NBT_KEY, 6));
		assertFalse("quality must not be stored as an int", tag.hasKey(FluidBooze.QUALITY_NBT_KEY, 3));
	}

	@Test
	public void storedQualitySurvivesNbtRoundTrip() {
		FluidStack original = stackWithTag(qualityTag(0.6875F));
		NBTTagCompound root = new NBTTagCompound();
		original.writeToNBT(root);

		FluidStack readBack = FluidStack.loadFluidStackFromNBT(root);
		assertEquals(0.6875F, booze.getQuality(readBack), 0.0F);
	}

	@Test
	public void qualityIsClampedToZeroOne() {
		assertEquals(1.0F, booze.getQuality(stackWithTag(qualityTag(1.7F))), 0.0F);
		assertEquals(1.0F, booze.getQuality(stackWithTag(qualityTag(1.0F))), 0.0F);
		assertEquals(0.0F, booze.getQuality(stackWithTag(qualityTag(0.0F))), 0.0F);
		assertEquals(0.0F, booze.getQuality(stackWithTag(qualityTag(-0.5F))), 0.0F);
	}

	@Test
	public void missingOrEmptyQualityDataReadsAsZero() {
		assertEquals(0.0F, booze.getQuality(new FluidStack(booze, 100)), 0.0F);

		NBTTagCompound wrongTypeDouble = new NBTTagCompound();
		wrongTypeDouble.setDouble(FluidBooze.QUALITY_NBT_KEY, 0.9D);
		assertEquals("only a FLOAT tag is a quality", 0.0F, booze.getQuality(stackWithTag(wrongTypeDouble)), 0.0F);

		NBTTagCompound wrongTypeInt = new NBTTagCompound();
		wrongTypeInt.setInteger(FluidBooze.QUALITY_NBT_KEY, 90);
		assertEquals(0.0F, booze.getQuality(stackWithTag(wrongTypeInt)), 0.0F);

		NBTTagCompound wrongTypeString = new NBTTagCompound();
		wrongTypeString.setString(FluidBooze.QUALITY_NBT_KEY, "0.9");
		assertEquals(0.0F, booze.getQuality(stackWithTag(wrongTypeString)), 0.0F);

		NBTTagCompound unrelated = new NBTTagCompound();
		unrelated.setFloat("SomethingElse", 0.9F);
		assertEquals(0.0F, booze.getQuality(stackWithTag(unrelated)), 0.0F);
	}

	@Test
	public void qualityCanReadFromAnyFluidStackTag() {
		// getQuality only looks at the tag, the fluid identity does not have to match
		Fluid other = new Fluid("baseline_other", new ResourceLocation("minecraft:blocks/water_still"),
				new ResourceLocation("minecraft:blocks/water_flow"));
		FluidRegistry.registerFluid(other);
		FluidStack tagged = new FluidStack(other, 100, qualityTag(0.25F));
		assertEquals(0.25F, booze.getQuality(tagged), 0.0F);
	}

	@Test
	public void labelColorTierBoundariesAreExact() {
		// >= 0.89999F gold, >= 0.69999F blue, >= 0.5F parchment, >= 0.35F parchment,
		// >= 0.2F yellow, else black
		assertEquals(0xC29311, ClientUtils.getQualityLabelColor(1.0F));
		assertEquals(0xC29311, ClientUtils.getQualityLabelColor(0.9F));
		assertEquals(0xC29311, ClientUtils.getQualityLabelColor(0.89999F));
		assertEquals(0x4A7ABD, ClientUtils.getQualityLabelColor(0.8999F));
		assertEquals(0x4A7ABD, ClientUtils.getQualityLabelColor(0.7F));
		assertEquals(0x4A7ABD, ClientUtils.getQualityLabelColor(0.69999F));
		assertEquals(0xDBD3BD, ClientUtils.getQualityLabelColor(0.6999F));
		assertEquals(0xDBD3BD, ClientUtils.getQualityLabelColor(0.5F));
		assertEquals(0xDBD3BD, ClientUtils.getQualityLabelColor(0.4999F));
		assertEquals(0xDBD3BD, ClientUtils.getQualityLabelColor(0.35F));
		assertEquals(0xBAA911, ClientUtils.getQualityLabelColor(0.3499F));
		assertEquals(0xBAA911, ClientUtils.getQualityLabelColor(0.2F));
		assertEquals(0x222222, ClientUtils.getQualityLabelColor(0.1999F));
		assertEquals(0x222222, ClientUtils.getQualityLabelColor(0.0F));
	}

	@Test
	public void labelColorHandlesOutOfRangeQuality() {
		assertEquals(0xC29311, ClientUtils.getQualityLabelColor(5.0F));
		assertEquals(0x222222, ClientUtils.getQualityLabelColor(-1.0F));
	}

	@Test
	public void inebriationChanceDefaultsToHalf() {
		assertEquals(0.5F, booze.getInebriationChance(), 0.0F);
		assertEquals(0.85F, booze.setInebriationChance(0.85F).getInebriationChance(), 0.0F);
	}

	private static NBTTagCompound qualityTag(float quality) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setFloat(FluidBooze.QUALITY_NBT_KEY, quality);
		return tag;
	}

	private static FluidStack stackWithTag(NBTTagCompound tag) {
		return new FluidStack(booze, 1000, tag);
	}

}
