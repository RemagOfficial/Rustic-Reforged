package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.fluids.RusticBooze;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.fluids.RusticFluids;

public class RusticBoozeQualityTest {

    private static RusticBooze reader() {
        return RusticFluids.ALE.value();
    }

    private static FluidStack boozeStack(float quality) {
        FluidStack stack = new FluidStack(RusticFluids.AMBROSIA.value(), 1000);
        stack.set(RusticDataComponents.QUALITY.get(), quality);
        return stack;
    }

    @Test
    public void missingQualityReadsAsZero() {
        FluidStack untouched = new FluidStack(RusticFluids.AMBROSIA.value(), 100);
        assertEquals(0.0F, reader().getQuality(untouched), 0.0F);
    }

    @Test
    public void storedQualitySurvivesComponentRoundTrip() {
        assertEquals(0.6875F, reader().getQuality(boozeStack(0.6875F)), 0.0F);
    }

    @Test
    public void qualityIsClampedToZeroOne() {
        assertEquals(1.0F, reader().getQuality(boozeStack(1.7F)), 0.0F);
        assertEquals(1.0F, reader().getQuality(boozeStack(1.0F)), 0.0F);
        assertEquals(0.0F, reader().getQuality(boozeStack(0.0F)), 0.0F);
        assertEquals(0.0F, reader().getQuality(boozeStack(-0.5F)), 0.0F);
    }

    @Test
    public void qualityCanReadFromAnyFluidStack() {
        FluidStack otherFluid = new FluidStack(RusticFluids.OLIVE_OIL.value(), 100);
        otherFluid.set(RusticDataComponents.QUALITY.get(), 0.25F);
        assertEquals(0.25F, reader().getQuality(otherFluid), 0.0F);
    }

    @Test
    public void labelColorTierBoundariesAreExact() {
        assertEquals(0xC29311, RusticBooze.getQualityLabelColor(1.0F));
        assertEquals(0xC29311, RusticBooze.getQualityLabelColor(0.9F));
        assertEquals(0xC29311, RusticBooze.getQualityLabelColor(0.89999F));
        assertEquals(0x4A7ABD, RusticBooze.getQualityLabelColor(0.8999F));
        assertEquals(0x4A7ABD, RusticBooze.getQualityLabelColor(0.7F));
        assertEquals(0x4A7ABD, RusticBooze.getQualityLabelColor(0.69999F));
        assertEquals(0xDBD3BD, RusticBooze.getQualityLabelColor(0.6999F));
        assertEquals(0xDBD3BD, RusticBooze.getQualityLabelColor(0.5F));
        assertEquals(0xDBD3BD, RusticBooze.getQualityLabelColor(0.4999F));
        assertEquals(0xDBD3BD, RusticBooze.getQualityLabelColor(0.35F));
        assertEquals(0xBAA911, RusticBooze.getQualityLabelColor(0.3499F));
        assertEquals(0xBAA911, RusticBooze.getQualityLabelColor(0.2F));
        assertEquals(0x222222, RusticBooze.getQualityLabelColor(0.1999F));
        assertEquals(0x222222, RusticBooze.getQualityLabelColor(0.0F));
    }

    @Test
    public void labelColorHandlesOutOfRangeQuality() {
        assertEquals(0xC29311, RusticBooze.getQualityLabelColor(5.0F));
        assertEquals(0x222222, RusticBooze.getQualityLabelColor(-1.0F));
    }

}
