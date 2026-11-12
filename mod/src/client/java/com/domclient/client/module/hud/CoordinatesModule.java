package com.domclient.client.module.hud;

import com.domclient.client.module.setting.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

public class CoordinatesModule extends TextHudModule {
	private final BooleanSetting facing = add(new BooleanSetting("Facing", "Show which way you're looking.", true));
	private final BooleanSetting biome = add(new BooleanSetting("Biome", "Show the biome you're standing in.", false));
	private final BooleanSetting otherDimension = add(new BooleanSetting("Nether Coords", "Show matching Nether/Overworld coordinates.", false));

	public CoordinatesModule() {
		super("Coordinates", "Shows your X Y Z position.", Corner.TOP_LEFT, true);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		int x = Mth.floor(mc.player.getX());
		int y = Mth.floor(mc.player.getY());
		int z = Mth.floor(mc.player.getZ());
		out.add(new Line("XYZ", x + ", " + y + ", " + z));
		if (otherDimension.isOn()) {
			if (mc.level.dimension() == Level.NETHER) {
				out.add(new Line("Overworld", (x * 8) + ", " + (z * 8)));
			} else if (mc.level.dimension() == Level.OVERWORLD) {
				out.add(new Line("Nether", Math.floorDiv(x, 8) + ", " + Math.floorDiv(z, 8)));
			}
		}
		if (facing.isOn()) {
			Direction direction = mc.player.getDirection();
			String axis = switch (direction) {
				case NORTH -> "-Z";
				case SOUTH -> "+Z";
				case EAST -> "+X";
				case WEST -> "-X";
				default -> "";
			};
			out.add(new Line("Facing", capitalize(direction.getName()) + " (" + axis + ")"));
		}
		if (biome.isOn()) {
			BlockPos pos = mc.player.blockPosition();
			String name = mc.level.getBiome(pos).unwrapKey()
				.map(key -> prettify(key.identifier().getPath()))
				.orElse("Unknown");
			out.add(new Line("Biome", name));
		}
	}

	@Override
	protected void collectPreviewLines(List<Line> out) {
		collectLines(out);
		if (out.isEmpty()) {
			out.add(new Line("XYZ", "128, 64, -256"));
		}
	}

	private static String capitalize(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String prettify(String path) {
		StringBuilder out = new StringBuilder();
		for (String part : path.split("_")) {
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(capitalize(part.toLowerCase(Locale.ROOT)));
		}
		return out.toString();
	}
}
