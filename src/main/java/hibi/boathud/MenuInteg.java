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

			.addEntry(entryBuilder.startIntField(MINIMAP_Y_OFFSET, Config.minimapYOffset)
			.setDefaultValue(0)
			.setMin(-100)
			.setMax(100)
			.setSaveConsumer(newVal -> Config.minimapYOffset = newVal)
			.build())

		// .addEntry(entryBuilder.startBooleanToggle(MINIMAP_SQUARE, Config.minimapSquare)
		//	.setDefaultValue(false)
		//	.setSaveConsumer(newVal -> Config.minimapSquare = newVal)
		//	.build())

		.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SHOW_ALL_HEIGHTS, Config.minimapShowAllHeights)
			.setDefaultValue(false)
			.setTooltip(TIP_MINIMAP_SHOW_ALL_HEIGHTS)
			.setSaveConsumer(newVal -> Config.minimapShowAllHeights = newVal)
			.build())

		.addEntry(entryBuilder.startBooleanToggle(MINIMAP_LOCK_NORTH, Config.minimapLockNorth)
				.setDefaultValue(true)
				.setTooltip(TIP_MINIMAP_LOCK_NORTH)
				.setSaveConsumer(newVal -> Config.minimapLockNorth = newVal)
				.build())

			.addEntry(entryBuilder.startIntField(MINIMAP_ICE_DETECTION_RANGE, Config.minimapIceDetectionRange)
				.setDefaultValue(15)
				.setMin(1)
				.setMax(100)
				.setTooltip(TIP_MINIMAP_ICE_DETECTION_RANGE)
				.setSaveConsumer(newVal -> Config.minimapIceDetectionRange = newVal)
				.build())

			.addEntry(entryBuilder.startBooleanToggle(MINIMAP_FLAT_ICE, Config.minimapFlatIce)
				.setDefaultValue(false)
				.setTooltip(TIP_MINIMAP_FLAT_ICE)
				.setSaveConsumer(newVal -> Config.minimapFlatIce = newVal)
				.build())

			.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SHOW_OTHER_PLAYERS, Config.minimapShowOtherPlayers)
				.setDefaultValue(true)
				.setTooltip(TIP_MINIMAP_SHOW_OTHER_PLAYERS)
				.setSaveConsumer(newVal -> Config.minimapShowOtherPlayers = newVal)
				.build())

			.addEntry(entryBuilder.startIntField(MINIMAP_SIZE, Config.minimapSize)
				.setDefaultValue(128)
				.setMin(32)
				.setMax(512)
				.setTooltip(TIP_MINIMAP_SIZE)
				.setSaveConsumer(newVal -> Config.minimapSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_ZOOM, Config.minimapZoom)
				.setDefaultValue(1.0d)
				.setMin(0.5d)
				.setMax(5.0d)
				.setTooltip(TIP_MINIMAP_ZOOM)
				.setSaveConsumer(newVal -> Config.minimapZoom = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_PLAYER_INDICATOR_SIZE, Config.minimapPlayerIndicatorSize)
				.setDefaultValue(3.0d)
				.setMin(0.5d)
				.setMax(10.0d)
				.setTooltip(TIP_MINIMAP_PLAYER_INDICATOR_SIZE)
				.setSaveConsumer(newVal -> Config.minimapPlayerIndicatorSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE, Config.minimapOtherPlayersIndicatorSize)
				.setDefaultValue(2.0d)
				.setMin(0.5d)
				.setMax(10.0d)
				.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE)
				.setSaveConsumer(newVal -> Config.minimapOtherPlayersIndicatorSize = newVal)
				.build());

				// .addEntry(entryBuilder.startBooleanToggle(SHOW_SPEED_BAR, Config.showSpeedBar)
				// 	.setDefaultValue(true)
				// 	.setSaveConsumer(newVal -> Config.showSpeedBar = newVal)
				// 	.build());

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
	MINIMAP_Y_OFFSET = Text.translatable("boathud.option.minimap_y_offset"),
		MINIMAP_SQUARE = Text.translatable("boathud.option.minimap_square"),
		MINIMAP_SHOW_ALL_HEIGHTS = Text.translatable("boathud.option.minimap_show_all_heights"),
		TIP_MINIMAP_SHOW_ALL_HEIGHTS = Text.translatable("boathud.tooltip.minimap_show_all_heights"),
		MINIMAP_LOCK_NORTH = Text.translatable("boathud.option.minimap_lock_north"),
		TIP_MINIMAP_LOCK_NORTH = Text.translatable("boathud.tooltip.minimap_lock_north"),
	
	MINIMAP_ICE_DETECTION_RANGE = Text.translatable("boathud.option.minimap_ice_detection_range"),
	TIP_MINIMAP_ICE_DETECTION_RANGE = Text.translatable("boathud.tooltip.minimap_ice_detection_range"),
	MINIMAP_FLAT_ICE = Text.translatable("boathud.option.minimap_flat_ice"),
	TIP_MINIMAP_FLAT_ICE = Text.translatable("boathud.tooltip.minimap_flat_ice"),
	MINIMAP_SHOW_OTHER_PLAYERS = Text.translatable("boathud.option.minimap_show_other_players"),
	TIP_MINIMAP_SHOW_OTHER_PLAYERS = Text.translatable("boathud.tooltip.minimap_show_other_players"),
	MINIMAP_SIZE = Text.translatable("boathud.option.minimap_size"),
	TIP_MINIMAP_SIZE = Text.translatable("boathud.tooltip.minimap_size"),
	MINIMAP_ZOOM = Text.translatable("boathud.option.minimap_zoom"),
	TIP_MINIMAP_ZOOM = Text.translatable("boathud.tooltip.minimap_zoom"),
	MINIMAP_PLAYER_INDICATOR_SIZE = Text.translatable("boathud.option.minimap_player_indicator_size"),
	TIP_MINIMAP_PLAYER_INDICATOR_SIZE = Text.translatable("boathud.tooltip.minimap_player_indicator_size"),
	MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Text.translatable("boathud.option.minimap_other_players_indicator_size"),
	TIP_MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Text.translatable("boathud.tooltip.minimap_other_players_indicator_size"),
	SHOW_SPEED_BAR = Text.translatable("boathud.option.show_speed_bar"),
		TIP_EXTENDED = Text.translatable("boathud.tooltip.extended"),
		TIP_BAR = Text.translatable("boathud.tooltip.bar_type"),
		TIP_BAR_PACKED = Text.translatable("boathud.tooltip.bar_type.packed"),
		TIP_BAR_MIXED = Text.translatable("boathud.tooltip.bar_type.mixed"),
		TIP_BAR_BLUE = Text.translatable("boathud.tooltip.bar_type.blue");
}
