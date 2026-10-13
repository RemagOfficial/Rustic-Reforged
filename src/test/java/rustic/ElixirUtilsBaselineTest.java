package rustic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import rustic.common.util.ElixirUtils;
import rustic.testsupport.RusticContent;

/**
 * Phase 0 baseline for the elixir / vanta oil NBT format.
 *
 * These tests pin the exact on-disk tag layout, because a port that renames a key silently
 * breaks every existing elixir in every existing save. They also pin the "uses" arithmetic that
 * drives the vanta oil tooltip and the durability-like consumption of an oil charge.
 */
public class ElixirUtilsBaselineTest {

	@BeforeClass
	public static void bootstrapRustic() {
		RusticContent.init();
	}

	@Test
	public void elixirEffectsAreStoredAsTaggedCompoundList() {
		ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.SPEED, 100, 1), stack);

		NBTTagCompound tag = stack.getTagCompound();
		assertNotNull("addEffect must create a tag compound", tag);
		assertTrue("effects live under the ElixirEffects list", tag.hasKey("ElixirEffects", 9));

		NBTTagList list = tag.getTagList("ElixirEffects", 10);
		assertEquals(1, list.tagCount());

		NBTTagCompound first = list.getCompoundTagAt(0);
		assertEquals("minecraft:speed", first.getString("Effect"));
		assertEquals(100, first.getInteger("Duration"));
		assertEquals(1, first.getInteger("Amplifier"));
	}

	@Test
	public void effectsAreAppendedInOrderAndReadBackInOrder() {
		ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.SPEED, 100, 1), stack);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.JUMP_BOOST, 200, 2), stack);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.POISON, 300, 0), stack);

		List<PotionEffect> effects = ElixirUtils.getEffects(stack);
		assertEquals(3, effects.size());
		assertEquals(MobEffects.SPEED, effects.get(0).getPotion());
		assertEquals(100, effects.get(0).getDuration());
		assertEquals(1, effects.get(0).getAmplifier());
		assertEquals(MobEffects.JUMP_BOOST, effects.get(1).getPotion());
		assertEquals(200, effects.get(1).getDuration());
		assertEquals(2, effects.get(1).getAmplifier());
		assertEquals(MobEffects.POISON, effects.get(2).getPotion());
	}

	@Test
	public void stackWithoutTagHasNoEffects() {
		List<PotionEffect> effects = ElixirUtils.getEffects(new ItemStack(Items.GLASS_BOTTLE));
		assertTrue(effects.isEmpty());
	}

	@Test
	public void unknownPotionIdsAreSkippedInsteadOfThrowing() {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		list.appendTag(effectTag("rustic:not_a_potion", 200, 0));
		list.appendTag(effectTag("minecraft:speed", 200, 1));
		tag.setTag("ElixirEffects", list);

		List<PotionEffect> effects = ElixirUtils.deserializeEffects(tag);
		assertEquals(1, effects.size());
		assertEquals(MobEffects.SPEED, effects.get(0).getPotion());
	}

	@Test
	public void deserializeToleratesNullAndMissingList() {
		assertTrue(ElixirUtils.deserializeEffects(null).isEmpty());
		assertTrue(ElixirUtils.deserializeEffects(new NBTTagCompound()).isEmpty());
	}

	@Test
	public void colorComesFromFirstEffectLiquidColor() {
		ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.SPEED, 100, 1), stack);
		ElixirUtils.addEffect(new PotionEffect(MobEffects.POISON, 200, 0), stack);

		assertEquals(ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft:speed")).getLiquidColor(),
				ElixirUtils.getColor(stack));
	}

	@Test
	public void colorFallsBackToVanillaPinkWater() {
		assertEquals(16253176, ElixirUtils.getColor(new ItemStack(Items.GLASS_BOTTLE)));

		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("ElixirEffects", new NBTTagList());
		ItemStack emptyList = new ItemStack(Items.GLASS_BOTTLE);
		emptyList.setTagCompound(tag);
		assertEquals(16253176, ElixirUtils.getColor(emptyList));

		ItemStack unknownPotion = new ItemStack(Items.GLASS_BOTTLE);
		NBTTagCompound unknownTag = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		list.appendTag(effectTag("rustic:not_a_potion", 200, 0));
		unknownTag.setTag("ElixirEffects", list);
		unknownPotion.setTagCompound(unknownTag);
		assertEquals(16253176, ElixirUtils.getColor(unknownPotion));
	}

	@Test
	public void vantaOilEffectRoundTripsThroughSubCompound() {
		ItemStack stack = new ItemStack(Items.IRON_SWORD);
		ElixirUtils.setVantaOilEffect(stack, new PotionEffect(MobEffects.STRENGTH, 600, 1));

		NBTTagCompound vantaTag = ElixirUtils.getVantaOilTag(stack);
		assertNotNull(vantaTag);
		assertEquals("minecraft:strength", vantaTag.getString("Effect"));
		assertEquals(600, vantaTag.getInteger("Duration"));
		assertEquals(1, vantaTag.getInteger("Amplifier"));

		PotionEffect read = ElixirUtils.getVantaOilEffect(stack);
		assertNotNull(read);
		assertEquals(MobEffects.STRENGTH, read.getPotion());
		assertEquals(600, read.getDuration());
		assertEquals(1, read.getAmplifier());
	}

	@Test
	public void settingVantaOilOnStackWithOtherTagsKeepsThem() {
		ItemStack stack = new ItemStack(Items.IRON_SWORD);
		stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString("rusticTestMarker", "keep me");

		ElixirUtils.setVantaOilEffect(stack, new PotionEffect(MobEffects.STRENGTH, 600, 1));
		assertEquals(600, ElixirUtils.getVantaOilEffect(stack).getDuration());

		ElixirUtils.setVantaOilEffect(stack, null);

		assertTrue("the untouched marker tag must survive the vanta oil removal",
				stack.getTagCompound().hasKey("rusticTestMarker", 8));
		assertEquals("keep me", stack.getTagCompound().getString("rusticTestMarker"));
		assertFalse(stack.getTagCompound().hasKey("vanta_oil", 10));
		assertNull(ElixirUtils.getVantaOilEffect(stack));
	}

	@Test
	public void clearingVantaOilOnCleanStackDropsTagCompoundEntirely() {
		ItemStack stack = new ItemStack(Items.IRON_SWORD);
		ElixirUtils.setVantaOilEffect(stack, new PotionEffect(MobEffects.STRENGTH, 600, 1));
		assertTrue(stack.hasTagCompound());

		ElixirUtils.setVantaOilEffect(stack, null);
		assertFalse("an empty tag compound must not linger on the item", stack.hasTagCompound());
	}

	@Test
	public void setVantaOilEffectRejectsUslessEffects() {
		assertNull(ElixirUtils.setVantaOilEffect((NBTTagCompound) null, null));
		assertNull(ElixirUtils.setVantaOilEffect((NBTTagCompound) null, new PotionEffect(MobEffects.STRENGTH, 0, 0)));

		// an expired effect also refuses to overwrite a live one; the caller then clears the tag
		NBTTagCompound live = ElixirUtils.setVantaOilEffect((NBTTagCompound) null, new PotionEffect(MobEffects.STRENGTH, 600, 1));
		assertNull(ElixirUtils.setVantaOilEffect(live, new PotionEffect(MobEffects.STRENGTH, 0, 1)));
		assertEquals(600, live.getInteger("Duration"));

		// one tick is still a valid charge, amplifier 0 is the lowest legal amplifier
		NBTTagCompound minimal = ElixirUtils.setVantaOilEffect((NBTTagCompound) null, new PotionEffect(MobEffects.STRENGTH, 1, 0));
		assertNotNull(minimal);
		assertEquals(1, minimal.getInteger("Duration"));
		assertEquals(0, minimal.getInteger("Amplifier"));
	}

	@Test
	public void invalidStoredVantaOilTagsReadAsNull() {
		assertNull(ElixirUtils.getVantaOilEffect((NBTTagCompound) null));

		NBTTagCompound noKeys = new NBTTagCompound();
		assertNull(ElixirUtils.getVantaOilEffect(noKeys));

		assertNull(ElixirUtils.getVantaOilEffect(vantaTag("minecraft:strength", 0, 1)));
		assertNull(ElixirUtils.getVantaOilEffect(vantaTag("minecraft:strength", 300, -1)));
		assertNull(ElixirUtils.getVantaOilEffect(vantaTag("rustic:not_a_potion", 300, 0)));
		// Duration/Amplifier must be present as ints; a bare Effect string is not an effect
		NBTTagCompound onlyEffect = new NBTTagCompound();
		onlyEffect.setString("Effect", "minecraft:strength");
		assertNull(ElixirUtils.getVantaOilEffect(onlyEffect));
	}

	@Test
	public void vantaOilTagIsNotWrittenForEmptyStacks() {
		ItemStack empty = ItemStack.EMPTY;
		ElixirUtils.setVantaOilEffect(empty, new PotionEffect(MobEffects.STRENGTH, 600, 1));
		assertFalse(empty.hasTagCompound());
		assertNull(ElixirUtils.getVantaOilTag(empty));
	}

	@Test
	public void remainingVantaUsesGroupsDurationIntoThirtySecondCharges() {
		// VANTA_OIL_EFFECT_DURATION = 15 * 20 = 300 ticks, max = 400 ticks
		assertEquals(300, ElixirUtils.VANTA_OIL_EFFECT_DURATION);
		assertEquals(400, ElixirUtils.VANTA_OIL_EFFECT_MAX_DURATION);

		assertEquals(0, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 0, 0)));
		assertEquals(1, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 1, 0)));
		assertEquals(1, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 250, 0)));
		assertEquals(1, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 300, 0)));
		assertEquals(1, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 350, 0)));
		assertEquals(2, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 401, 0)));
		assertEquals(2, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 600, 0)));
		assertEquals(2, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 700, 0)));
		assertEquals(3, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.STRENGTH, 701, 0)));
	}

	@Test
	public void instantPotionDurationIsCountedAsOneUsePerTick() {
		// instant effects take the "duration" shortcut: the raw duration IS the use count
		assertEquals(12, ElixirUtils.getRemainingVantaUses(new PotionEffect(MobEffects.INSTANT_HEALTH, 12, 0)));
	}

	@Test
	public void nextVantaHitDurationSplitsOnlyOversizedStashes() {
		assertEquals(250, ElixirUtils.getNextVantaHitDuration(250));
		assertEquals(400, ElixirUtils.getNextVantaHitDuration(400));
		assertEquals(300, ElixirUtils.getNextVantaHitDuration(401));
		assertEquals(300, ElixirUtils.getNextVantaHitDuration(600));
	}

	private static NBTTagCompound effectTag(String effectId, int duration, int amplifier) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("Effect", effectId);
		tag.setInteger("Duration", duration);
		tag.setInteger("Amplifier", amplifier);
		return tag;
	}

	private static NBTTagCompound vantaTag(String effectId, int duration, int amplifier) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("Effect", effectId);
		tag.setInteger("Duration", duration);
		tag.setInteger("Amplifier", amplifier);
		return tag;
	}

}
