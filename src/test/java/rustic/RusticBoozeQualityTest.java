package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.fluids.RusticBooze;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.fluids.RusticFluids;

/**
 * Phase 6 port of the 1.12.2 golden {@code BoozeQualityBaselineTest}: the quality contract on a
 * {@link FluidStack} and the quality tier -&gt; label colour mapping.
 *
 * <p>Legacy stored quality as a loose {@code "Quality"} FLOAT NBT tag (type 5) and only accepted type 5, so a
 * port that wrote a double/int silently turned every existing booze into 0% sludge. In 1.21 quality is the typed
 * {@code rustic:quality} {@link net.minecraft.core.component.DataComponentType DataComponentType&lt;Float&gt;}
 * (see {@link RusticDataComponents#QUALITY}); the whole "wrong NBT type" half of the legacy baseline has no
 * analogue — the type system rejects a non-{@code Float} at compile time now, which is the improvement that half
 * of the old test was guarding. What stays pinned verbatim: absent reads as {@code 0.0F} (the golden default,
 * deliberately <em>not</em> {@code 0.5F}), every read is clamped to {@code [0, 1]}, the read keys off the
 * component and not the fluid identity, the six {@code 0.89999/0.69999/0.5/0.35/0.2} tier bounds and their exact
 * colours.</p>
 *
 * <p><b>Deviation from the legacy baseline.</b> The old test built an anonymous {@code new FluidBooze(...) { }}
 * to prove the {@code 0.5F} inebriation default and the setter's returns-this contract. In 1.21 that is
 * impossible in a test JVM: the vanilla {@link net.minecraft.world.level.material.Fluid} constructor calls
 * {@code BuiltInRegistries.FLUID.createIntrusiveHolder(this)}, which throws {@code "Registry is already frozen"}
 * once bootstrap has run, so no fresh fluid subclass can be instantiated here. The {@code 0.5F} default and the
 * per-booze chances are instead pinned on the real registered boozes by
 * {@code RusticFluidRegistrationTest#inebriationChancesMatchTheShippedValues}.</p>
 */
public class RusticBoozeQualityTest {

    /** getQuality ignores fluid identity, so an ordinary registered booze is a fine reader for every stack. */
    private static RusticBooze reader() {
        return RusticFluids.ALE.value();
    }

    private static FluidStack boozeStack(float quality) {
        FluidStack stack = new FluidStack(RusticFluids.AMBROSIA.value(), 1000);
        stack.set(RusticDataComponents.QUALITY.get(), quality);
        return stack;
    }

    // ---------------------------------------------------------------- the read contract

    /** Absent quality reads as {@code 0.0F} — the pinned default, not {@code 0.5F}. */
    @Test
    public void missingQualityReadsAsZero() {
        FluidStack untouched = new FluidStack(RusticFluids.AMBROSIA.value(), 100);
        assertEquals(0.0F, reader().getQuality(untouched), 0.0F);
    }

    /** A stored quality round-trips through the component with no drift. */
    @Test
    public void storedQualitySurvivesComponentRoundTrip() {
        assertEquals(0.6875F, reader().getQuality(boozeStack(0.6875F)), 0.0F);
    }

    /** Every read is clamped to {@code [0, 1]}. */
    @Test
    public void qualityIsClampedToZeroOne() {
        assertEquals(1.0F, reader().getQuality(boozeStack(1.7F)), 0.0F);
        assertEquals(1.0F, reader().getQuality(boozeStack(1.0F)), 0.0F);
        assertEquals(0.0F, reader().getQuality(boozeStack(0.0F)), 0.0F);
        assertEquals(0.0F, reader().getQuality(boozeStack(-0.5F)), 0.0F);
    }

    /** getQuality keys off the component only, so any fluid's stack works as a carrier (legacy parity). */
    @Test
    public void qualityCanReadFromAnyFluidStack() {
        FluidStack otherFluid = new FluidStack(RusticFluids.OLIVE_OIL.value(), 100);
        otherFluid.set(RusticDataComponents.QUALITY.get(), 0.25F);
        assertEquals(0.25F, reader().getQuality(otherFluid), 0.0F);
    }

    // ---------------------------------------------------------------- tier colours (exact bounds)

    /**
     * The tier-&gt;colour mapping, with the literal {@code 0.89999}/{@code 0.69999}/{@code 0.5}/{@code 0.35}/{@code 0.2}
     * bounds and their neighbours pinned so a rounded threshold can never slip in.
     */
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

    /** Out-of-range qualities still land on the two extreme tiers. */
    @Test
    public void labelColorHandlesOutOfRangeQuality() {
        assertEquals(0xC29311, RusticBooze.getQualityLabelColor(5.0F));
        assertEquals(0x222222, RusticBooze.getQualityLabelColor(-1.0F));
    }

}
