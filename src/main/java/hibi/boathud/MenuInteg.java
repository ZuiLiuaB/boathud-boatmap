package hibi.boathud;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

public class MenuInteg implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(TITLE);
			ConfigEntryBuilder entryBuilder = builder.entryBuilder();
			builder.getOrCreateCategory(CAT)

				.addEntry(entryBuilder.startBooleanToggle(ENABLED, Config.enabled)
					.setDefaultValue(true)
					.setSaveConsumer(newVal -> Config.enabled = newVal)
					.build())

				.addEntry(entryBuilder.startBooleanToggle(EXTENDED, Config.extended)
					.setDefaultValue(true)
					.setTooltip(TIP_EXTENDED)
					.setSaveConsumer(newVal -> Config.extended = newVal)
					.build())

				.addEntry(entryBuilder.startEnumSelector(SPEED_FORMAT, SpeedFormat.class, SpeedFormat.values()[Config.configSpeedType])
					.setDefaultValue(SpeedFormat.KMPH)
					.setSaveConsumer(newVal -> Config.setUnit(newVal.ordinal()))
					.setEnumNameProvider(value -> Text.translatable("boathud.option.speed_format." + value.toString()))
					.build())

				.addEntry(entryBuilder.startEnumSelector(BAR_TYPE, BarType.class, BarType.values()[Config.barType])
					.setDefaultValue(BarType.PACKED)
					.setTooltip(TIP_BAR, TIP_BAR_PACKED, TIP_BAR_MIXED, TIP_BAR_BLUE)
					.setSaveConsumer(newVal -> Config.barType = newVal.ordinal())
					.setEnumNameProvider(value -> Text.translatable("boathud.option.bar_type." + value.toString()))
					.build())

				// Minimap settings
				.addEntry(entryBuilder.startBooleanToggle(MINIMAP_ENABLED, Config.minimapEnabled)
					.setDefaultValue(true)
					.setSaveConsumer(newVal -> Config.minimapEnabled = newVal)
					.build())

				.addEntry(entryBuilder.startIntField(MINIMAP_X, Config.minimapX)
					.setDefaultValue(10)
					.setMin(0)
					.setSaveConsumer(newVal -> Config.minimapX = newVal)
					.build())

				.addEntry(entryBuilder.startIntField(MINIMAP_Y, Config.minimapY)
					.setDefaultValue(10)
					.setMin(0)
					.setSaveConsumer(newVal -> Config.minimapY = newVal)
					.build())

				.addEntry(entryBuilder.startDoubleField(MINIMAP_SCALE, Config.minimapScale)
					.setDefaultValue(1.0d)
					.setMin(0.1d)
					.setMax(5.0d)
					.setSaveConsumer(newVal -> Config.minimapScale = newVal)
					.build())

				.addEntry(entryBuilder.startIntField(MINIMAP_Y_OFFSET, Config.minimapYOffset)
					.setDefaultValue(0)
					.setMin(-100)
					.setMax(100)
					.setSaveConsumer(newVal -> Config.minimapYOffset = newVal)
					.build())

				.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SQUARE, Config.minimapSquare)
					.setDefaultValue(false)
					.setSaveConsumer(newVal -> Config.minimapSquare = newVal)
					.build())

				.addEntry(entryBuilder.startBooleanToggle(MINIMAP_LOCK_NORTH, Config.minimapLockNorth)
					.setDefaultValue(true)
					.setTooltip(TIP_MINIMAP_LOCK_NORTH)
					.setSaveConsumer(newVal -> Config.minimapLockNorth = newVal)
					.build())

				.addEntry(entryBuilder.startEnumSelector(MINIMAP_PLAYER_SHAPE, PlayerShape.class, PlayerShape.values()[Config.minimapPlayerShape])
					.setDefaultValue(PlayerShape.CIRCLE)
					.setTooltip(TIP_MINIMAP_PLAYER_SHAPE)
					.setSaveConsumer(newVal -> Config.minimapPlayerShape = newVal.ordinal())
					.setEnumNameProvider(value -> Text.translatable("boathud.option.minimap_player_shape." + value.toString()))
					.build())

				.addEntry(entryBuilder.startBooleanToggle(SHOW_SPEED_BAR, Config.showSpeedBar)
					.setDefaultValue(true)
					.setSaveConsumer(newVal -> Config.showSpeedBar = newVal)
					.build());

			builder.setSavingRunnable(() -> Config.save());
			return builder.build();
		};
	}

	public enum BarType {
		PACKED, MIXED, BLUE
	}
		public enum SpeedFormat {
		MS, KMPH, MPH, KT
	}
	public enum PlayerShape {
		SQUARE, CIRCLE
	}

	private static final MutableText
		TITLE = Text.translatable("boathud.config.title"),
		CAT = Text.translatable("boathud.config.cat"),
		ENABLED = Text.translatable("boathud.option.enabled"),
		EXTENDED = Text.translatable("boathud.option.extended"),
		BAR_TYPE = Text.translatable("boathud.option.bar_type"),
		SPEED_FORMAT = Text.translatable("boathud.option.speed_format"),
		MINIMAP_ENABLED = Text.translatable("boathud.option.minimap_enabled"),
		MINIMAP_X = Text.translatable("boathud.option.minimap_x"),
		MINIMAP_Y = Text.translatable("boathud.option.minimap_y"),
		MINIMAP_SCALE = Text.translatable("boathud.option.minimap_scale"),
		MINIMAP_Y_OFFSET = Text.translatable("boathud.option.minimap_y_offset"),
		MINIMAP_SQUARE = Text.translatable("boathud.option.minimap_square"),
		MINIMAP_LOCK_NORTH = Text.translatable("boathud.option.minimap_lock_north"),
		TIP_MINIMAP_LOCK_NORTH = Text.translatable("boathud.tooltip.minimap_lock_north"),
		MINIMAP_PLAYER_SHAPE = Text.translatable("boathud.option.minimap_player_shape"),
		TIP_MINIMAP_PLAYER_SHAPE = Text.translatable("boathud.tooltip.minimap_player_shape"),
		SHOW_SPEED_BAR = Text.translatable("boathud.option.show_speed_bar"),
		TIP_EXTENDED = Text.translatable("boathud.tooltip.extended"),
		TIP_BAR = Text.translatable("boathud.tooltip.bar_type"),
		TIP_BAR_PACKED = Text.translatable("boathud.tooltip.bar_type.packed"),
		TIP_BAR_MIXED = Text.translatable("boathud.tooltip.bar_type.mixed"),
		TIP_BAR_BLUE = Text.translatable("boathud.tooltip.bar_type.blue");
}
