package com.domclient.client.module.hud;

import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.HudSettingsModule;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.module.setting.ModeSetting;
import com.domclient.client.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatusModule extends HudModule {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private final BooleanSetting heldItem = add(new BooleanSetting("Held Item", "Also show the item in your hand.", true));
	private final ModeSetting display = add(new ModeSetting("Durability", "How durability is written.", "Number", "Number", "Percent", "Hidden"));
	private final List<ItemStack> items = new ArrayList<>();

	public ArmorStatusModule() {
		super("Armor Status", "Shows your armor and how much durability is left.", Corner.BOTTOM_RIGHT, true);
		addBindSetting();
	}

	@Override
	public boolean measure(Font font) {
		items.clear();
		if (mc.player == null) {
			return false;
		}
		if (heldItem.isOn()) {
			ItemStack main = mc.player.getMainHandItem();
			if (!main.isEmpty()) {
				items.add(main);
			}
		}
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				items.add(stack);
			}
		}
		return layout(font);
	}

	@Override
	public boolean measurePreview(Font font) {
		measure(font);
		if (items.isEmpty()) {
			items.add(new ItemStack(Items.DIAMOND_HELMET));
			items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
			items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
			items.add(new ItemStack(Items.DIAMOND_BOOTS));
		}
		return layout(font);
	}

	private boolean layout(Font font) {
		if (items.isEmpty()) {
			width = height = 0;
			return false;
		}
		int textWidth = 0;
		for (ItemStack stack : items) {
			textWidth = Math.max(textWidth, font.width(label(stack)));
		}
		width = 16 + (textWidth > 0 ? 3 + textWidth : 0);
		height = items.size() * 17 - 1;
		return true;
	}

	private String label(ItemStack stack) {
		if (display.is("Hidden")) {
			return "";
		}
		if (stack.isDamageableItem()) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			return display.is("Percent") ? Math.round(left * 100f / stack.getMaxDamage()) + "%" : Integer.toString(left);
		}
		return stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "";
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y) {
		boolean shadow = ModuleManager.get(HudSettingsModule.class).textShadow();
		boolean rightSide = x + width / 2 > g.guiWidth() / 2;
		int row = y;
		for (ItemStack stack : items) {
			String text = label(stack);
			int textWidth = font.width(text);
			int itemX = rightSide ? x + width - 16 : x;
			int textX = rightSide ? itemX - 3 - textWidth : x + 19;
			g.item(stack, itemX, row);
			if (!text.isEmpty()) {
				int color = 0xFFFFFFFF;
				if (stack.isDamageableItem()) {
					float left = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
					color = ColorUtil.lerp(0xFFF87171, 0xFF4ADE80, left);
				}
				g.text(font, text, textX, row + 4, color, shadow);
			}
			row += 17;
		}
	}
}
