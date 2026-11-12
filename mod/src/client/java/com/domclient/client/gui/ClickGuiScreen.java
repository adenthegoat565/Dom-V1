package com.domclient.client.gui;

import com.domclient.client.DomClient;
import com.domclient.client.config.ConfigManager;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.ClickGuiModule;
import com.domclient.client.module.setting.ActionSetting;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.module.setting.KeybindSetting;
import com.domclient.client.module.setting.ModeSetting;
import com.domclient.client.module.setting.NumberSetting;
import com.domclient.client.module.setting.Setting;
import com.domclient.client.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The click GUI: one draggable panel per category. Click a panel's header to drop it down or fold it up,
 * left-click a module to turn it on/off and right-click it to open its settings.
 */
public class ClickGuiScreen extends Screen {
	static final int PANEL_WIDTH = 118;
	static final int HEADER_HEIGHT = 18;
	static final int MODULE_HEIGHT = 15;
	static final int SETTING_HEIGHT = 14;
	static final int SLIDER_HEIGHT = 18;
	private static final int MARGIN = 8;

	private final @Nullable Screen parent;
	private final List<Panel> panels = new ArrayList<>();
	private @Nullable Panel dragging;
	private double dragOffsetX;
	private double dragOffsetY;
	private double dragStartX;
	private double dragStartY;
	private boolean dragMoved;
	private @Nullable SliderDrag sliderDrag;
	private @Nullable KeybindSetting listening;
	private String search = "";
	/** Mouse-wheel scroll of the whole panel area. Only for viewing; never saved. */
	private float scrollOffset;
	private long lastFrame = Util.getMillis();
	private @Nullable String hoverDescription;

	public ClickGuiScreen() {
		this(null);
	}

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal("Dom Client"));
		this.parent = parent;
	}

	private ClickGuiModule settings() {
		return ModuleManager.get(ClickGuiModule.class);
	}

	@Override
	protected void init() {
		if (panels.isEmpty()) {
			for (Category category : Category.values()) {
				panels.add(new Panel(category));
			}
		}
		placePanels(false);
	}

	/** Called by the "Reset Panels" button. */
	public void resetLayout() {
		placePanels(true);
	}

	/**
	 * Puts panels that have no saved spot into columns. Each one goes under the shortest column so
	 * nothing overlaps, even on small windows where the panels don't fit side by side.
	 */
	private void placePanels(boolean force) {
		int columns = Math.max(1, (width - MARGIN) / (PANEL_WIDTH + MARGIN));
		int[] bottoms = new int[columns];
		java.util.Arrays.fill(bottoms, MARGIN);
		for (Panel panel : panels) {
			ConfigManager.PanelState state = ConfigManager.panel(panel.category);
			if (force || state.x < 0 || state.y < 0) {
				int column = 0;
				for (int c = 1; c < columns; c++) {
					if (bottoms[c] < bottoms[column]) {
						column = c;
					}
				}
				state.x = MARGIN + column * (PANEL_WIDTH + MARGIN);
				state.y = bottoms[column];
				int panelHeight = HEADER_HEIGHT + (state.open ? panel.contentHeight() : 0) + 1;
				bottoms[column] += panelHeight + MARGIN;
			}
			panel.openAnim = state.open ? 1f : 0f;
			clamp(state);
		}
	}

	private void clamp(ConfigManager.PanelState state) {
		state.x = Math.max(0, Math.min(state.x, width - PANEL_WIDTH));
		state.y = Math.max(0, Math.min(state.y, height - HEADER_HEIGHT));
	}

	@Override
	public boolean isPauseScreen() {
		return settings().pauseGame();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (minecraft.level == null) {
			super.extractBackground(graphics, mouseX, mouseY, partialTick);
			return;
		}
		if (settings().blur()) {
			graphics.blurBeforeThisStratum();
		}
		int alpha = Math.round(settings().darkness() * 255);
		if (alpha > 0) {
			graphics.fillGradient(0, 0, width, height, ColorUtil.withAlpha(0x000000, alpha), ColorUtil.withAlpha(0x0B0B12, Math.min(255, alpha + 30)));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		float dt = Math.max(0f, Math.min(0.1f, (now - lastFrame) / 1000f));
		lastFrame = now;
		hoverDescription = null;

		Panel topHovered = panelAt(mouseX, mouseY);
		for (int i = 0; i < panels.size(); i++) {
			Panel panel = panels.get(i);
			panel.animate(dt, !search.isEmpty());
			panel.render(graphics, mouseX, mouseY, panel == topHovered, i);
		}

		extractFooter(graphics);
		if (hoverDescription != null && settings().descriptions()) {
			int w = font.width(hoverDescription) + 8;
			int x = (width - w) / 2;
			int y = height - 34;
			graphics.fill(x, y, x + w, y + 14, 0xE0101016);
			graphics.outline(x, y, w, 14, ColorUtil.withAlpha(Theme.accent(), 200));
			graphics.text(font, hoverDescription, x + 4, y + 3, Theme.TEXT, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void extractFooter(GuiGraphicsExtractor graphics) {
		String hint;
		if (listening != null) {
			hint = "Press a key to bind it  •  Esc to clear";
		} else if (!search.isEmpty()) {
			hint = "Search: " + search + "_";
		} else {
			hint = "Left-click: on/off  •  Right-click: settings  •  Type to search";
		}
		String brand = "Dom Client v" + DomClient.VERSION;
		int brandWidth = font.width(brand);
		int w = font.width(hint);
		int hintX = (width - w) / 2;
		boolean roomForBrand = hintX + w + 12 < width - brandWidth - 4;
		if (!roomForBrand && w + brandWidth + 16 < width) {
			// Not enough space to centre both: slide the hint left instead of overlapping.
			hintX = 4;
			roomForBrand = true;
		}
		int color = !search.isEmpty() ? Theme.accent() : Theme.TEXT_DIM;
		graphics.text(font, hint, hintX, height - 14, color, true);
		if (roomForBrand) {
			graphics.text(font, brand, width - brandWidth - 4, height - 14, ColorUtil.withAlpha(Theme.accent(), 220), true);
		}
	}

	private @Nullable Panel panelAt(double mouseX, double mouseY) {
		for (int i = panels.size() - 1; i >= 0; i--) {
			if (panels.get(i).contains(mouseX, mouseY)) {
				return panels.get(i);
			}
		}
		return null;
	}

	private void bringToFront(Panel panel) {
		panels.remove(panel);
		panels.add(panel);
	}

	void playClick() {
		if (settings().sounds()) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.35F));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		if (listening != null) {
			// Mouse side buttons can't be module binds; any click just stops listening.
			listening = null;
			return true;
		}
		Panel panel = panelAt(mx, my);
		if (panel == null) {
			return super.mouseClicked(event, doubleClick);
		}
		bringToFront(panel);
		if (panel.headerContains(mx, my)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				dragging = panel;
				dragOffsetX = mx - panel.x();
				dragOffsetY = my - panel.y();
				dragStartX = mx;
				dragStartY = my;
				dragMoved = false;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				panel.toggleOpen();
				playClick();
			}
			return true;
		}
		panel.clickContent(mx, my, button);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			if (Math.abs(event.x() - dragStartX) > 2 || Math.abs(event.y() - dragStartY) > 2) {
				dragMoved = true;
			}
			if (dragMoved) {
				ConfigManager.PanelState state = dragging.state();
				state.x = (float) (event.x() - dragOffsetX);
				state.y = (float) (event.y() - dragOffsetY - scrollOffset);
				clamp(state);
				state.y = Math.max(-scrollOffset, Math.min(state.y, height - HEADER_HEIGHT - scrollOffset));
			}
			return true;
		}
		if (sliderDrag != null) {
			sliderDrag.update(event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			if (!dragMoved && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				dragging.toggleOpen();
				playClick();
			}
			dragging = null;
			return true;
		}
		if (sliderDrag != null) {
			sliderDrag = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Panel panel = panelAt(mouseX, mouseY);
		if (panel != null && panel.scrollSlider(mouseX, mouseY, scrollY)) {
			return true;
		}
		// Scroll the whole view when tall panels don't fit on a small screen.
		int lowest = 0;
		for (Panel p : panels) {
			lowest = Math.max(lowest, Math.round(p.state().y) + HEADER_HEIGHT + p.visibleContentHeight() + 1);
		}
		float minOffset = Math.min(0, height - 20 - lowest);
		scrollOffset = (float) Math.max(minOffset, Math.min(0, scrollOffset + scrollY * 14));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening != null) {
			int key = event.key();
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
				listening.set(KeybindSetting.NONE);
			} else {
				listening.set(key);
			}
			listening = null;
			playClick();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			return true;
		}
		if (event.isEscape() && !search.isEmpty()) {
			search = "";
			return true;
		}
		if (DomClient.CLICK_GUI_KEY.matches(event) && search.isEmpty()) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (listening == null && event.isAllowedChatCharacter()) {
			String typed = event.codepointAsString();
			if (!(search.isEmpty() && typed.isBlank()) && search.length() < 24) {
				search += typed;
			}
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public void removed() {
		ConfigManager.save();
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	/** Centre of a module's row in GUI pixels, or null if it's folded away. Used by the automated in-game test. */
	public int @Nullable [] locateModule(String name) {
		for (Panel panel : panels) {
			int rowY = panel.y() + HEADER_HEIGHT;
			int limit = rowY + panel.visibleContentHeight();
			for (ModuleRow row : panel.visibleRows()) {
				if (row.module.getName().equalsIgnoreCase(name)) {
					return rowY + MODULE_HEIGHT > limit ? null : new int[]{panel.x() + PANEL_WIDTH / 2, rowY + MODULE_HEIGHT / 2};
				}
				rowY += row.height();
			}
		}
		return null;
	}

	/** Centre of a category header in GUI pixels. Used by the automated in-game test. */
	public int[] locatePanel(Category category) {
		ConfigManager.PanelState state = ConfigManager.panel(category);
		return new int[]{Math.round(state.x) + PANEL_WIDTH / 2, Math.round(state.y + scrollOffset) + HEADER_HEIGHT / 2};
	}

	private boolean matchesSearch(Module module) {
		return search.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
	}

	private static float ease(float t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	private static float approach(float current, float target, float dt) {
		if (!Float.isFinite(current)) {
			return target;
		}
		float next = current + (target - current) * (1f - (float) Math.exp(-dt * 16f));
		return Math.abs(next - target) < 0.004f ? target : next;
	}

	/** Remembers which slider is being dragged and where its track is. */
	private record SliderDrag(NumberSetting setting, int trackX, int trackWidth) {
		void update(double mouseX) {
			setting.setFromProgress((mouseX - trackX) / trackWidth);
		}
	}

	/** One category column. */
	private final class Panel {
		final Category category;
		final List<ModuleRow> rows = new ArrayList<>();
		float openAnim = 1f;

		Panel(Category category) {
			this.category = category;
			for (Module module : ModuleManager.byCategory(category)) {
				rows.add(new ModuleRow(module));
			}
		}

		ConfigManager.PanelState state() {
			return ConfigManager.panel(category);
		}

		void toggleOpen() {
			state().open = !state().open;
		}

		boolean effectiveOpen(boolean searching) {
			return searching ? visibleRows().size() > 0 : state().open;
		}

		List<ModuleRow> visibleRows() {
			List<ModuleRow> list = new ArrayList<>();
			for (ModuleRow row : rows) {
				if (matchesSearch(row.module)) {
					list.add(row);
				}
			}
			return list;
		}

		void animate(float dt, boolean searching) {
			openAnim = approach(openAnim, effectiveOpen(searching) ? 1f : 0f, dt);
			for (ModuleRow row : rows) {
				row.expandAnim = approach(row.expandAnim, row.expanded ? 1f : 0f, dt);
			}
		}

		int contentHeight() {
			int h = 0;
			for (ModuleRow row : visibleRows()) {
				h += row.height();
			}
			return h;
		}

		int visibleContentHeight() {
			return Math.round(contentHeight() * ease(openAnim));
		}

		int x() {
			return Math.round(state().x);
		}

		int y() {
			return Math.round(state().y + scrollOffset);
		}

		boolean headerContains(double mx, double my) {
			return mx >= x() && mx < x() + PANEL_WIDTH && my >= y() && my < y() + HEADER_HEIGHT;
		}

		boolean contains(double mx, double my) {
			return mx >= x() && mx < x() + PANEL_WIDTH && my >= y() && my < y() + HEADER_HEIGHT + visibleContentHeight() + 1;
		}

		void render(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean topMost, int index) {
			int x = x();
			int y = y();
			int accent = Theme.accent(index * 0.12f);
			int content = visibleContentHeight();
			boolean dimmed = !search.isEmpty() && visibleRows().isEmpty();

			// Soft shadow and outline.
			g.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + HEADER_HEIGHT + content + 2, 0x55000000);
			// Header with the accent gradient.
			int top = dimmed ? 0xFF3F3F46 : accent;
			g.fillGradient(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, top, ColorUtil.darker(top, 0.62f));
			Component title = Component.literal(category.getDisplayName()).withStyle(ChatFormatting.BOLD);
			g.text(font, title, x + 6, y + 5, Theme.TEXT, true);
			String count = Integer.toString(rows.size());
			String arrow = state().open ? "−" : "+";
			int arrowX = x + PANEL_WIDTH - 6 - font.width(arrow);
			g.text(font, arrow, arrowX, y + 5, Theme.TEXT, true);
			g.text(font, count, arrowX - 5 - font.width(count), y + 5, 0xB0FFFFFF, false);

			if (content > 0) {
				int cy = y + HEADER_HEIGHT;
				g.enableScissor(x, cy, x + PANEL_WIDTH, cy + content);
				g.fill(x, cy, x + PANEL_WIDTH, cy + content, Theme.PANEL_BG);
				int rowY = cy;
				int rowIndex = 0;
				for (ModuleRow row : visibleRows()) {
					boolean inside = topMost && mouseY >= cy && mouseY < cy + content;
					row.render(g, x, rowY, mouseX, mouseY, inside, index * 0.12f + rowIndex * 0.04f);
					rowY += row.height();
					rowIndex++;
				}
				g.disableScissor();
			}
			g.fill(x, y + HEADER_HEIGHT + content, x + PANEL_WIDTH, y + HEADER_HEIGHT + content + 1, accent);
		}

		void clickContent(double mx, double my, int button) {
			int rowY = y() + HEADER_HEIGHT;
			int limit = rowY + visibleContentHeight();
			if (my >= limit) {
				return;
			}
			for (ModuleRow row : visibleRows()) {
				int h = row.height();
				if (my >= rowY && my < rowY + h) {
					row.click(x(), rowY, mx, my, button);
					return;
				}
				rowY += h;
			}
		}

		boolean scrollSlider(double mx, double my, double amount) {
			int rowY = y() + HEADER_HEIGHT;
			int limit = rowY + visibleContentHeight();
			if (my < rowY || my >= limit) {
				return false;
			}
			for (ModuleRow row : visibleRows()) {
				int h = row.height();
				if (my >= rowY && my < rowY + h) {
					return row.scroll(rowY, my, amount);
				}
				rowY += h;
			}
			return false;
		}
	}

	/** A module inside a panel, plus its settings dropdown. */
	private final class ModuleRow {
		final Module module;
		boolean expanded;
		float expandAnim;

		ModuleRow(Module module) {
			this.module = module;
		}

		List<Setting<?>> visibleSettings() {
			List<Setting<?>> list = new ArrayList<>();
			for (Setting<?> setting : module.getSettings()) {
				if (setting.isVisible()) {
					list.add(setting);
				}
			}
			return list;
		}

		boolean hasSettings() {
			return !module.getSettings().isEmpty();
		}

		int settingsHeight() {
			int h = 0;
			for (Setting<?> setting : visibleSettings()) {
				h += settingHeight(setting);
			}
			return h == 0 ? 0 : h + 2;
		}

		int height() {
			return MODULE_HEIGHT + Math.round(settingsHeight() * ease(expandAnim));
		}

		void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY, boolean mouseInPanel, float hueOffset) {
			boolean available = module.isAvailable();
			boolean on = module.isEnabled() && module.getKind() == Module.Kind.TOGGLE;
			boolean hovered = mouseInPanel && mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= y && mouseY < y + MODULE_HEIGHT;
			int accent = Theme.accent(hueOffset);

			if (on) {
				g.fill(x, y, x + PANEL_WIDTH, y + MODULE_HEIGHT, ColorUtil.withAlpha(accent, 0x55));
				g.fill(x, y, x + 2, y + MODULE_HEIGHT, accent);
			}
			if (hovered) {
				g.fill(x, y, x + PANEL_WIDTH, y + MODULE_HEIGHT, Theme.ROW_HOVER);
				hoverDescription = available ? module.getDescription() : module.getUnavailableReason();
			}
			int textColor = !available ? Theme.TEXT_DISABLED : on ? Theme.TEXT : Theme.TEXT_DIM;
			if (module.getKind() != Module.Kind.TOGGLE && available) {
				textColor = hovered ? Theme.TEXT : 0xFFD4D4D8;
			}
			g.text(font, module.getName(), x + 7, y + 4, textColor, on);

			String marker;
			if (module.getKind() == Module.Kind.ACTION) {
				marker = "›";
			} else if (hasSettings()) {
				marker = expanded ? "▾" : "▸";
			} else {
				marker = "";
			}
			if (!marker.isEmpty()) {
				g.text(font, marker, x + PANEL_WIDTH - 6 - font.width(marker), y + 4, available ? Theme.TEXT_DIM : Theme.TEXT_DISABLED, false);
			}

			int settingsH = Math.round(settingsHeight() * ease(expandAnim));
			if (settingsH > 0) {
				int sy = y + MODULE_HEIGHT;
				g.fill(x, sy, x + PANEL_WIDTH, sy + settingsH, Theme.SETTINGS_BG);
				g.fill(x + 3, sy + 1, x + 4, sy + settingsH - 1, ColorUtil.withAlpha(accent, 0xAA));
				int rowY = sy + 1;
				for (Setting<?> setting : visibleSettings()) {
					renderSetting(g, setting, x + 6, rowY, PANEL_WIDTH - 10, mouseX, mouseY, mouseInPanel, accent);
					rowY += settingHeight(setting);
				}
			}
		}

		void click(int x, int y, double mx, double my, int button) {
			if (my < y + MODULE_HEIGHT) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					if (!module.isAvailable()) {
						return;
					}
					if (module.getKind() == Module.Kind.SETTINGS) {
						expanded = !expanded;
					} else {
						module.activate();
					}
					playClick();
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && hasSettings()) {
					expanded = !expanded;
					playClick();
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					listening = module.getBind();
					expanded = true;
					playClick();
				}
				return;
			}
			if (expandAnim < 0.99f) {
				return;
			}
			int rowY = y + MODULE_HEIGHT + 1;
			for (Setting<?> setting : visibleSettings()) {
				int h = settingHeight(setting);
				if (my >= rowY && my < rowY + h) {
					clickSetting(setting, x + 6, PANEL_WIDTH - 10, mx, button);
					return;
				}
				rowY += h;
			}
		}

		boolean scroll(int y, double my, double amount) {
			if (my < y + MODULE_HEIGHT || expandAnim < 0.99f) {
				return false;
			}
			int rowY = y + MODULE_HEIGHT + 1;
			for (Setting<?> setting : visibleSettings()) {
				int h = settingHeight(setting);
				if (my >= rowY && my < rowY + h) {
					if (setting instanceof NumberSetting number) {
						number.set(number.get() + Math.signum(amount) * number.getStep());
						return true;
					}
					if (setting instanceof ModeSetting mode) {
						mode.cycle(amount < 0);
						return true;
					}
					return false;
				}
				rowY += h;
			}
			return false;
		}
	}

	private static int settingHeight(Setting<?> setting) {
		return setting instanceof NumberSetting ? SLIDER_HEIGHT : SETTING_HEIGHT;
	}

	private void renderSetting(GuiGraphicsExtractor g, Setting<?> setting, int x, int y, int w, int mouseX, int mouseY, boolean mouseInPanel, int accent) {
		int h = settingHeight(setting);
		boolean hovered = mouseInPanel && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		if (hovered) {
			g.fill(x - 2, y, x + w + 2, y + h, 0x18FFFFFF);
			hoverDescription = setting.getDescription();
		}
		int textY = y + 3;
		if (setting instanceof BooleanSetting bool) {
			g.text(font, setting.getName(), x + 2, textY, Theme.TEXT_DIM, false);
			int bx = x + w - 9;
			int by = y + 3;
			g.fill(bx, by, bx + 8, by + 8, 0xFF2A2A33);
			g.outline(bx, by, 8, 8, bool.isOn() ? accent : 0xFF4B4B57);
			if (bool.isOn()) {
				g.fill(bx + 2, by + 2, bx + 6, by + 6, accent);
			}
		} else if (setting instanceof NumberSetting number) {
			g.text(font, setting.getName(), x + 2, textY, Theme.TEXT_DIM, false);
			String value = number.getDisplayValue();
			g.text(font, value, x + w - 1 - font.width(value), textY, Theme.TEXT, false);
			int trackY = y + 13;
			g.fill(x + 2, trackY, x + w - 1, trackY + 2, 0xFF2A2A33);
			int filled = (int) Math.round((w - 3) * number.getProgress());
			g.fill(x + 2, trackY, x + 2 + filled, trackY + 2, accent);
			int knobX = x + 2 + filled;
			g.fill(knobX - 1, trackY - 1, knobX + 2, trackY + 3, Theme.TEXT);
		} else if (setting instanceof ModeSetting mode) {
			g.text(font, setting.getName(), x + 2, textY, Theme.TEXT_DIM, false);
			String value = mode.getDisplayValue();
			g.text(font, value, x + w - 1 - font.width(value), textY, accent, false);
		} else if (setting instanceof KeybindSetting bind) {
			g.text(font, setting.getName(), x + 2, textY, Theme.TEXT_DIM, false);
			String value = listening == bind ? ((Util.getMillis() / 400) % 2 == 0 ? "..." : "") : bind.getDisplayValue();
			int vw = font.width(value);
			int bx = x + w - vw - 5;
			g.fill(bx, y + 1, x + w, y + h - 1, listening == bind ? ColorUtil.withAlpha(accent, 0x66) : 0xFF22222A);
			g.text(font, value, bx + 3, textY, Theme.TEXT, false);
		} else if (setting instanceof ActionSetting) {
			int color = hovered ? accent : 0xFF2A2A33;
			g.fill(x + 2, y + 1, x + w - 1, y + h - 1, ColorUtil.withAlpha(color, hovered ? 0x99 : 0xFF));
			String name = setting.getName();
			g.text(font, name, x + (w - font.width(name)) / 2, textY, Theme.TEXT, false);
		}
	}

	private void clickSetting(Setting<?> setting, int x, int w, double mx, int button) {
		if (setting instanceof BooleanSetting bool) {
			bool.toggle();
			playClick();
		} else if (setting instanceof NumberSetting number) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				number.reset();
				playClick();
			} else {
				sliderDrag = new SliderDrag(number, x + 2, w - 3);
				sliderDrag.update(mx);
			}
		} else if (setting instanceof ModeSetting mode) {
			mode.cycle(button != GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			playClick();
		} else if (setting instanceof KeybindSetting bind) {
			listening = bind;
			playClick();
		} else if (setting instanceof ActionSetting action) {
			playClick();
			action.run();
		}
	}
}
