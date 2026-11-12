package com.domclient.client.module.hud;

import com.domclient.client.gui.Theme;
import com.domclient.client.module.Module;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.HudSettingsModule;
import com.domclient.client.module.setting.BooleanSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The classic "array list": every module that's on, longest name first. */
public class ModuleListModule extends HudModule {
	private final BooleanSetting backgrounds = add(new BooleanSetting("Background", "Dark box behind each name.", true));
	private final BooleanSetting sidebar = add(new BooleanSetting("Sidebar", "Coloured bar next to each name.", true));
	private final List<String> entries = new ArrayList<>();
	private final List<Integer> widths = new ArrayList<>();

	public ModuleListModule() {
		super("Module List", "Lists the modules you have turned on.", Corner.TOP_RIGHT, true);
		addBindSetting();
	}

	private static String entry(Module module) {
		String info = module.getInfo();
		return info.isEmpty() ? module.getName() : module.getName() + " §7" + info;
	}

	@Override
	public boolean measure(Font font) {
		entries.clear();
		widths.clear();
		List<Module> active = new ArrayList<>();
		for (Module module : ModuleManager.all()) {
			if (module.showInModuleList() && module.isEnabled()) {
				active.add(module);
			}
		}
		active.sort(Comparator.comparingInt((Module m) -> font.width(entry(m))).reversed());
		int w = 0;
		for (Module module : active) {
			String text = entry(module);
			entries.add(text);
			int tw = font.width(text);
			widths.add(tw);
			w = Math.max(w, tw);
		}
		if (entries.isEmpty()) {
			width = height = 0;
			return false;
		}
		width = w + 4 + (sidebar.isOn() ? 2 : 0);
		height = entries.size() * 11;
		return true;
	}

	@Override
	public boolean measurePreview(Font font) {
		if (measure(font)) {
			return true;
		}
		entries.add("Module List");
		widths.add(font.width("Module List"));
		width = widths.getFirst() + 4 + (sidebar.isOn() ? 2 : 0);
		height = 11;
		return true;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y) {
		boolean shadow = ModuleManager.get(HudSettingsModule.class).textShadow();
		boolean rightSide = x + width / 2 > g.guiWidth() / 2;
		int bar = sidebar.isOn() ? 2 : 0;
		for (int i = 0; i < entries.size(); i++) {
			int tw = widths.get(i);
			int rowW = tw + 4 + bar;
			int rowX = rightSide ? x + width - rowW : x;
			int rowY = y + i * 11;
			int color = Theme.accent(i * 0.06f);
			if (backgrounds.isOn()) {
				g.fill(rowX, rowY, rowX + rowW, rowY + 11, 0x90000000);
			}
			if (bar > 0) {
				int barX = rightSide ? rowX + rowW - bar : rowX;
				g.fill(barX, rowY, barX + bar, rowY + 11, color);
			}
			int textX = rightSide ? rowX + 2 : rowX + bar + 2;
			g.text(font, entries.get(i), textX, rowY + 2, color, shadow);
		}
	}
}
