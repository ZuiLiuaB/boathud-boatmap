package hibi.boathud;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
					.setSaveConsumer(newVal -> Config.setUnit(newVal.ordinal()))
					.setEnumNameProvider(value -> Component.translatable("boathud.option.speed_format." + value.toString()))
					.build())

				.addEntry(entryBuilder.startEnumSelector(BAR_TYPE, BarType.class, BarType.values()[Config.barType])
					.setTooltip(TIP_BAR, TIP_BAR_PACKED, TIP_BAR_MIXED, TIP_BAR_BLUE)
					.setSaveConsumer(newVal -> Config.barType = newVal.ordinal())
					.setEnumNameProvider(value -> Component.translatable("boathud.option.bar_type." + value.toString()))
					.build())

				.addEntry(entryBuilder.startBooleanToggle(CAMERA_CONTROL, Config.cameraControl)
					.setDefaultValue(true)
					.setTooltip(TIP_CAMERA_CONTROL)
					.setSaveConsumer(newVal -> Config.cameraControl = newVal)
					.build())
				
				.addEntry(entryBuilder.startFloatField(CAMERA_AGGRESSIVENESS, Config.cameraAggressiveness * 20)
					.setDefaultValue(60)
					.setTooltip(TIP_CAMERA_AGGRESSIVENESS)
					.setMin(4).setMax(70)
					.setSaveConsumer(newVal -> Config.cameraAggressiveness = newVal / 20)
					.build())
				
				.addEntry(entryBuilder.startFloatField(CAMERA_SMOOTHING, Config.cameraSmoothing / 0.009f)
					.setDefaultValue(50)
					.setTooltip(TIP_CAMERA_SMOOTHING)
					.setMin(0).setMax(100)
					.setSaveConsumer(newVal -> Config.cameraSmoothing = newVal * 0.009f)
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
					.setDefaultValue(3)
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

	private static final MutableComponent
		TITLE = Component.translatable("boathud.config.title"),
		CAT = Component.translatable("boathud.config.cat"),
		ENABLED = Component.translatable("boathud.option.enabled"),
		EXTENDED = Component.translatable("boathud.option.extended"),
		SPEED_FORMAT = Component.translatable("boathud.option.speed_format"),
		BAR_TYPE = Component.translatable("boathud.option.bar_type"),
		CAMERA_CONTROL = Component.translatable("boathud.option.camera_control"),
		CAMERA_AGGRESSIVENESS = Component.translatable("boathud.option.camera_aggressiveness"),
		CAMERA_SMOOTHING = Component.translatable("boathud.option.camera_smoothing"),
		MINIMAP_ENABLED = Component.translatable("boathud.option.minimap_enabled"),
		MINIMAP_X = Component.translatable("boathud.option.minimap_x"),
		MINIMAP_Y = Component.translatable("boathud.option.minimap_y"),
		MINIMAP_Y_OFFSET = Component.translatable("boathud.option.minimap_y_offset"),
		MINIMAP_SHOW_ALL_HEIGHTS = Component.translatable("boathud.option.minimap_show_all_heights"),
		MINIMAP_LOCK_NORTH = Component.translatable("boathud.option.minimap_lock_north"),
		MINIMAP_ICE_DETECTION_RANGE = Component.translatable("boathud.option.minimap_ice_detection_range"),
		MINIMAP_FLAT_ICE = Component.translatable("boathud.option.minimap_flat_ice"),
		MINIMAP_SHOW_OTHER_PLAYERS = Component.translatable("boathud.option.minimap_show_other_players"),
		MINIMAP_SIZE = Component.translatable("boathud.option.minimap_size"),
		MINIMAP_ZOOM = Component.translatable("boathud.option.minimap_zoom"),
		MINIMAP_PLAYER_INDICATOR_SIZE = Component.translatable("boathud.option.minimap_player_indicator_size"),
		MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Component.translatable("boathud.option.minimap_other_players_indicator_size"),
		TIP_EXTENDED = Component.translatable("boathud.tooltip.extended"),
		TIP_CAMERA_CONTROL = Component.translatable("boathud.tooltip.camera_control"),
		TIP_CAMERA_AGGRESSIVENESS = Component.translatable("boathud.tooltip.camera_aggressiveness"),
		TIP_CAMERA_SMOOTHING = Component.translatable("boathud.tooltip.camera_smoothing"),
		TIP_MINIMAP_SHOW_ALL_HEIGHTS = Component.translatable("boathud.tooltip.minimap_show_all_heights"),
		TIP_MINIMAP_LOCK_NORTH = Component.translatable("boathud.tooltip.minimap_lock_north"),
		TIP_MINIMAP_ICE_DETECTION_RANGE = Component.translatable("boathud.tooltip.minimap_ice_detection_range"),
		TIP_MINIMAP_FLAT_ICE = Component.translatable("boathud.tooltip.minimap_flat_ice"),
		TIP_MINIMAP_SHOW_OTHER_PLAYERS = Component.translatable("boathud.tooltip.minimap_show_other_players"),
		TIP_MINIMAP_SIZE = Component.translatable("boathud.tooltip.minimap_size"),
		TIP_MINIMAP_ZOOM = Component.translatable("boathud.tooltip.minimap_zoom"),
		TIP_MINIMAP_PLAYER_INDICATOR_SIZE = Component.translatable("boathud.tooltip.minimap_player_indicator_size"),
		TIP_MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Component.translatable("boathud.tooltip.minimap_other_players_indicator_size"),
		TIP_BAR = Component.translatable("boathud.tooltip.bar_type"),
		TIP_BAR_PACKED = Component.translatable("boathud.tooltip.bar_type.packed"),
		TIP_BAR_MIXED = Component.translatable("boathud.tooltip.bar_type.mixed"),
		TIP_BAR_BLUE = Component.translatable("boathud.tooltip.bar_type.blue");
}
