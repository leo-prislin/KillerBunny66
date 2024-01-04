package de.wattestaebchen.killerbunny66.mixin;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
	
	@Final
	@Shadow
	private Property levelCost;
	@Shadow
	private int repairItemUsage;
	@Shadow
	private String newItemName;
	
	public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(type, syncId, playerInventory, context);
	}
	
	@Unique
	private final Map<Enchantment, Integer> enchantmentBaseValues = new HashMap<>() {{
		put(Enchantments.PROTECTION,1);
		put(Enchantments.FIRE_PROTECTION,1);
		put(Enchantments.BLAST_PROTECTION,1);
		put(Enchantments.PROJECTILE_PROTECTION,1);
		put(Enchantments.THORNS,0);
		put(Enchantments.AQUA_AFFINITY,2);
		put(Enchantments.RESPIRATION,1);
		put(Enchantments.DEPTH_STRIDER,1);
		put(Enchantments.FROST_WALKER,1);
		put(Enchantments.SOUL_SPEED,0);
		put(Enchantments.FEATHER_FALLING,1);
		put(Enchantments.SWIFT_SNEAK,0);
		put(Enchantments.SHARPNESS,0);
		put(Enchantments.SMITE,0);
		put(Enchantments.BANE_OF_ARTHROPODS,0);
		put(Enchantments.KNOCKBACK,1);
		put(Enchantments.FIRE_ASPECT,1);
		put(Enchantments.LOOTING,1);
		put(Enchantments.SWEEPING,0);
		put(Enchantments.EFFICIENCY,0);
		put(Enchantments.SILK_TOUCH,3);
		put(Enchantments.FORTUNE,2);
		put(Enchantments.POWER,0);
		put(Enchantments.PUNCH,1);
		put(Enchantments.FLAME,3);
		put(Enchantments.INFINITY,3);
		put(Enchantments.MULTISHOT,2);
		put(Enchantments.PIERCING,0);
		put(Enchantments.QUICK_CHARGE,0);
		put(Enchantments.IMPALING,0);
		put(Enchantments.RIPTIDE,2);
		put(Enchantments.LOYALTY,1);
		put(Enchantments.CHANNELING,2);
		put(Enchantments.LUCK_OF_THE_SEA,0);
		put(Enchantments.LURE,0);
		put(Enchantments.UNBREAKING,2);
		put(Enchantments.MENDING,4);
		put(Enchantments.BINDING_CURSE,4);
		put(Enchantments.VANISHING_CURSE,4);
	}};
	
	@Unique
	private int getMaterialValue(ItemStack itemStack) {
		if(itemStack.getItem() instanceof ToolItem item) {
			if(item.getMaterial().getMiningLevel() <= 0) return 2;
			else if(item.getMaterial().getMiningLevel() <= 2) return 4;
			else if(item.getMaterial().getMiningLevel() <= 4) return 8;
			else return 16;
		}
		else throw new IllegalArgumentException("ItemStack must be of type ToolItem!");
	}
	@Unique
	private int getEnchantmentValue(ItemStack itemStack) {
		int enchantmentValue = 0;
		Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
		for(Enchantment e : enchantments.keySet()) {
			int lvl = enchantments.get(e) + enchantmentBaseValues.getOrDefault(e, 0);
			enchantmentValue += (int) Math.pow(2, lvl)-1;
		}
		return enchantmentValue;
	}
	
	
	@Override
	public void updateResult() {
		this.output.setStack(0, ItemStack.EMPTY);
		this.levelCost.set(0);
		this.repairItemUsage = 0;
		
		ItemStack input1 = input.getStack(0);
		if (input1.isEmpty()) {
			return;
		}
		
		ItemStack output = input1.copy();
		int levelCost = 0;
		
		if(!Util.isBlank(newItemName)) {
			// Rename
			output.setCustomName(Text.literal(newItemName));
			levelCost++;
		}
		
		ItemStack input2 = input.getStack(1);
		if(input2.isEmpty()) {
			if(Util.isBlank(newItemName)) {
				return;
			}
		}
		else if(input1.isDamaged() && input1.getItem().canRepair(input1, input2)) {
			// Repair with Ingredient
			int requiredIngredients = (input1.getItem() instanceof ArmorItem) ? 3 : 2;
			int repairAmount = (input1.getMaxDamage()+1)/requiredIngredients;
			int damage = input1.getDamage();
			for(int i = 0; i < input2.getCount(); i++) {
				damage -= repairAmount;
				repairItemUsage++;
				if(damage <= 0) {
					damage = 0;
					break;
				}
			}
			output.setDamage(damage);
			levelCost += repairItemUsage;
		}
		else if(input2.isOf(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantmentNbt(input2).isEmpty()) {
			// Combine with Enchanted Book
			Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.get(input1));
			boolean success = false;
			for(Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.get(input2).entrySet()) {
				if(entry.getKey().isAcceptableItem(input1)) {
					success = true;
					Integer value = enchantments.get(entry.getKey());
					if(value != null && value.equals(entry.getValue()))
						enchantments.put(entry.getKey(), Math.min(value+1, entry.getKey().getMaxLevel()));
					else if(value == null || value < entry.getValue())
						enchantments.put(entry.getKey(), entry.getValue());
				}
			}
			if(success) {
				EnchantmentHelper.set(enchantments, output);
				levelCost += getEnchantmentValue(output) - getEnchantmentValue(input1);
			} else return;
		}
		else if(input1.getItem().isDamageable() && input1.getItem().equals(input2.getItem())) {
			// Combine with Tool
			Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.get(input1));
			for(Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.get(input2).entrySet()) {
				Integer value = enchantments.get(entry.getKey());
				if(value != null && value.equals(entry.getValue()))
					enchantments.put(entry.getKey(), Math.min(value+1, entry.getKey().getMaxLevel()));
				else if(value == null || value < entry.getValue())
					enchantments.put(entry.getKey(), entry.getValue());
			}
			EnchantmentHelper.set(enchantments, output);
			output.setDamage(input1.getDamage() + input2.getDamage() - input1.getMaxDamage());
			levelCost += getEnchantmentValue(output) - getEnchantmentValue(input1) - getEnchantmentValue(input2) + getMaterialValue(input1);
		}
		else return;
		
		this.output.setStack(0, output);
		this.levelCost.set(levelCost);
		sendContentUpdates();
	}
	
	@Override
	public void onTakeOutput(PlayerEntity player, ItemStack stack) {
		if (!player.getAbilities().creativeMode) {
			player.addExperienceLevels(-this.levelCost.get());
		}
		if (this.repairItemUsage > 0) {
			ItemStack itemStack = input.getStack(1);
			if (!itemStack.isEmpty() && itemStack.getCount() > this.repairItemUsage) {
				itemStack.decrement(this.repairItemUsage);
				input.setStack(1, itemStack);
			} else {
				input.setStack(1, ItemStack.EMPTY);
			}
		} else {
			input.setStack(1, ItemStack.EMPTY);
		}
		input.setStack(0, ItemStack.EMPTY);
		this.levelCost.set(0);
		this.context.run((world, pos) -> {
			BlockState blockState = world.getBlockState(pos);
			if (!player.getAbilities().creativeMode && blockState.isIn(BlockTags.ANVIL) && player.getRandom().nextFloat() < 0.12f) {
				BlockState blockState2 = AnvilBlock.getLandingState(blockState);
				if (blockState2 == null) {
					world.removeBlock(pos, false);
					world.syncWorldEvent(WorldEvents.ANVIL_DESTROYED, pos, 0);
				} else {
					world.setBlockState(pos, blockState2, Block.NOTIFY_LISTENERS);
					world.syncWorldEvent(WorldEvents.ANVIL_USED, pos, 0);
				}
			} else {
				world.syncWorldEvent(WorldEvents.ANVIL_USED, pos, 0);
			}
		});
	}
	
}