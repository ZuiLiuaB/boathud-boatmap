package hibi.boathud;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

public class MenuInteg implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new ConfigScreen(parent);
	}

	/** Builds the full cloth-config settings screen (export/import live on the wrapper screen). */
	public static Screen buildClothScreen(Screen parent) {
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

				.addEntry(entryBuilder.startIntField(HUD_X_OFFSET, Config.hudX)
					.setDefaultValue(0)
					.setMin(-400).setMax(400)
					.setTooltip(TIP_HUD_X_OFFSET)
					.setSaveConsumer(newVal -> Config.hudX = newVal)
					.build())

				.addEntry(entryBuilder.startIntField(HUD_Y_OFFSET, Config.hudY)
					.setDefaultValue(0)
					.setMin(-400).setMax(400)
					.setTooltip(TIP_HUD_Y_OFFSET)
					.setSaveConsumer(newVal -> Config.hudY = newVal)
					.build())

				.addEntry(entryBuilder.startDoubleField(HUD_SCALE, Config.hudScale)
					.setDefaultValue(1.0d)
					.setMin(0.5d).setMax(2.0d)
					.setTooltip(TIP_HUD_SCALE)
					.setSaveConsumer(newVal -> Config.hudScale = newVal)
					.build())

				.addEntry(entryBuilder.startEnumSelector(SPEED_FORMAT, SpeedFormat.class, SpeedFormat.values()[Config.configSpeedType])
					.setDefaultValue(SpeedFormat.KMPH)
					.setSaveConsumer(newVal -> Config.setUnit(newVal.ordinal()))
					.setEnumNameProvider(value -> Component.translatable("boathud.option.speed_format." + value.toString()))
					.build())

				.addEntry(entryBuilder.startEnumSelector(BAR_TYPE, SpeedBar.class, Config.barType)
					.setDefaultValue(SpeedBar.PACKED)
					.setTooltip(TIP_BAR, TIP_BAR_PACKED, TIP_BAR_MIXED, TIP_BAR_BLUE, TIP_BAR_PROGRESSIVE)
					.setSaveConsumer(newVal -> Config.barType = newVal)
					.setEnumNameProvider(value -> Component.translatable("boathud.option.bar_type." + value.toString()))
					.build());

			builder.getOrCreateCategory(CAT_CAMERA)

				.addEntry(entryBuilder.startBooleanToggle(CAMERA_CONTROL, Config.cameraControl)
					.setDefaultValue(false)
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
					.build());

			builder.getOrCreateCategory(CAT_MINIMAP)

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

		.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SHOW_NORTH_INDICATOR, Config.minimapShowNorthIndicator)
				.setDefaultValue(false)
				.setTooltip(TIP_MINIMAP_SHOW_NORTH_INDICATOR)
				.setSaveConsumer(newVal -> Config.minimapShowNorthIndicator = newVal)
				.build())

			.addEntry(entryBuilder.startEnumSelector(MINIMAP_SHAPE, MinimapShape.class, MinimapShape.values()[Config.minimapShape])
				.setDefaultValue(MinimapShape.CIRCLE)
				.setTooltip(TIP_MINIMAP_SHAPE)
				.setSaveConsumer(newVal -> Config.minimapShape = newVal.ordinal())
				.setEnumNameProvider(value -> Component.translatable("boathud.option.minimap_shape." + value.toString()))
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_TILT_ANGLE, Config.minimapTiltAngle)
				.setDefaultValue(35.0d)
				.setMin(0.0d)
				.setMax(90.0d)
				.setTooltip(TIP_MINIMAP_TILT_ANGLE)
				.setSaveConsumer(newVal -> Config.minimapTiltAngle = newVal)
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

			.addEntry(entryBuilder.startDoubleField(MINIMAP_BORDER_SIZE, Config.minimapBorderSize)
				.setDefaultValue(1.0d)
				.setMin(0.0d)
				.setMax(8.0d)
				.setTooltip(TIP_MINIMAP_BORDER_SIZE)
				.setSaveConsumer(newVal -> Config.minimapBorderSize = newVal)
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

			.addEntry(entryBuilder.startDoubleField(MINIMAP_PLAYER_INDICATOR_BORDER_SIZE, Config.minimapPlayerIndicatorBorderSize)
				.setDefaultValue(1.0d)
				.setMin(0.0d)
				.setMax(6.0d)
				.setTooltip(TIP_MINIMAP_PLAYER_INDICATOR_BORDER_SIZE)
				.setSaveConsumer(newVal -> Config.minimapPlayerIndicatorBorderSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE, Config.minimapOtherPlayersIndicatorSize)
				.setDefaultValue(2.0d)
				.setMin(0.5d)
				.setMax(10.0d)
				.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE)
				.setSaveConsumer(newVal -> Config.minimapOtherPlayersIndicatorSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_HEAD_SIZE, Config.minimapOtherPlayersHeadSize)
				.setDefaultValue(6.0d)
				.setMin(2.0d)
				.setMax(16.0d)
				.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_HEAD_SIZE)
				.setSaveConsumer(newVal -> Config.minimapOtherPlayersHeadSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_HEAD_FRAME_SIZE, Config.minimapOtherPlayersHeadFrameSize)
				.setDefaultValue(1.0d)
				.setMin(0.0d)
				.setMax(6.0d)
				.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_HEAD_FRAME_SIZE)
				.setSaveConsumer(newVal -> Config.minimapOtherPlayersHeadFrameSize = newVal)
				.build())
			
			.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SHOW_OTHER_PLAYERS_NAMES, Config.minimapShowOtherPlayersNames)
				.setDefaultValue(false)
				.setTooltip(TIP_MINIMAP_SHOW_OTHER_PLAYERS_NAMES)
				.setSaveConsumer(newVal -> Config.minimapShowOtherPlayersNames = newVal)
				.build())
			
			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_NAME_SIZE, Config.minimapOtherPlayersNameSize)
				.setDefaultValue(1.0d)
				.setMin(0.1d)
				.setMax(3.0d)
				.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_NAME_SIZE)
				.setSaveConsumer(newVal -> Config.minimapOtherPlayersNameSize = newVal)
				.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_NAME_OFFSET_Y, Config.minimapOtherPlayersNameOffsetY)
					.setDefaultValue(10.0d)
					.setMin(-40.0d)
					.setMax(40.0d)
					.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_NAME_OFFSET_Y)
					.setSaveConsumer(newVal -> Config.minimapOtherPlayersNameOffsetY = newVal)
					.build())

			.addEntry(entryBuilder.startDoubleField(MINIMAP_OTHER_PLAYERS_NAME_OFFSET_X, Config.minimapOtherPlayersNameOffsetX)
					.setDefaultValue(0.0d)
					.setMin(-40.0d)
					.setMax(40.0d)
					.setTooltip(TIP_MINIMAP_OTHER_PLAYERS_NAME_OFFSET_X)
					.setSaveConsumer(newVal -> Config.minimapOtherPlayersNameOffsetX = newVal)
					.build())

			.addEntry(entryBuilder.startBooleanToggle(MINIMAP_SPECTATOR_LABEL, Config.minimapSpectatorLabel)
					.setDefaultValue(false)
					.setTooltip(TIP_MINIMAP_SPECTATOR_LABEL)
					.setSaveConsumer(newVal -> Config.minimapSpectatorLabel = newVal)
					.build())
			
			.addEntry(entryBuilder.startBooleanToggle(MINIMAP_LIMIT_PLAYERS_TO_BOUNDS, Config.minimapLimitPlayersToBounds)
					.setDefaultValue(true)
					.setTooltip(TIP_MINIMAP_LIMIT_PLAYERS_TO_BOUNDS)
					.setSaveConsumer(newVal -> Config.minimapLimitPlayersToBounds = newVal)
					.build());


				// .addEntry(entryBuilder.startBooleanToggle(SHOW_SPEED_BAR, Config.showSpeedBar)
				// 	.setDefaultValue(true)
					// 	.setSaveConsumer(newVal -> Config.showSpeedBar = newVal)
				// 	.build());

		builder.setSavingRunnable(() -> Config.save());
		return builder.build();
	}

	public enum MinimapShape {
		CIRCLE, SQUARE, PSEUDO3D
	}
		public enum SpeedFormat {
		MS, KMPH, MPH, KT
	}


	private static final MutableComponent
		TITLE = Component.translatable("boathud.config.title"),
		CAT = Component.translatable("boathud.config.cat"),
		CAT_CAMERA = Component.translatable("boathud.config.cat.camera"),
		CAT_MINIMAP = Component.translatable("boathud.config.cat.minimap"),
		ENABLED = Component.translatable("boathud.option.enabled"),
		EXTENDED = Component.translatable("boathud.option.extended"),
		HUD_X_OFFSET = Component.translatable("boathud.option.hud_x_offset"),
		TIP_HUD_X_OFFSET = Component.translatable("boathud.tooltip.hud_x_offset"),
		HUD_Y_OFFSET = Component.translatable("boathud.option.hud_y_offset"),
		TIP_HUD_Y_OFFSET = Component.translatable("boathud.tooltip.hud_y_offset"),
		HUD_SCALE = Component.translatable("boathud.option.hud_scale"),
		TIP_HUD_SCALE = Component.translatable("boathud.tooltip.hud_scale"),
		CAMERA_CONTROL = Component.translatable("boathud.option.camera_control"),
		CAMERA_AGGRESSIVENESS = Component.translatable("boathud.option.camera_aggressiveness"),
		CAMERA_SMOOTHING = Component.translatable("boathud.option.camera_smoothing"),
		BAR_TYPE = Component.translatable("boathud.option.bar_type"),
		SPEED_FORMAT = Component.translatable("boathud.option.speed_format"),
		MINIMAP_ENABLED = Component.translatable("boathud.option.minimap_enabled"),
		MINIMAP_X = Component.translatable("boathud.option.minimap_x"),
	MINIMAP_Y = Component.translatable("boathud.option.minimap_y"),
	MINIMAP_Y_OFFSET = Component.translatable("boathud.option.minimap_y_offset"),
		MINIMAP_SQUARE = Component.translatable("boathud.option.minimap_square"),
		MINIMAP_SHOW_ALL_HEIGHTS = Component.translatable("boathud.option.minimap_show_all_heights"),
		TIP_MINIMAP_SHOW_ALL_HEIGHTS = Component.translatable("boathud.tooltip.minimap_show_all_heights"),
		MINIMAP_LOCK_NORTH = Component.translatable("boathud.option.minimap_lock_north"),
		TIP_MINIMAP_LOCK_NORTH = Component.translatable("boathud.tooltip.minimap_lock_north"),
		MINIMAP_SHOW_NORTH_INDICATOR = Component.translatable("boathud.option.minimap_show_north_indicator"),
		TIP_MINIMAP_SHOW_NORTH_INDICATOR = Component.translatable("boathud.tooltip.minimap_show_north_indicator"),
		MINIMAP_SHAPE = Component.translatable("boathud.option.minimap_shape"),
		TIP_MINIMAP_SHAPE = Component.translatable("boathud.tooltip.minimap_shape"),
		MINIMAP_TILT_ANGLE = Component.translatable("boathud.option.minimap_tilt_angle"),
		TIP_MINIMAP_TILT_ANGLE = Component.translatable("boathud.tooltip.minimap_tilt_angle"),
	
	MINIMAP_ICE_DETECTION_RANGE = Component.translatable("boathud.option.minimap_ice_detection_range"),
	TIP_MINIMAP_ICE_DETECTION_RANGE = Component.translatable("boathud.tooltip.minimap_ice_detection_range"),
	MINIMAP_FLAT_ICE = Component.translatable("boathud.option.minimap_flat_ice"),
	TIP_MINIMAP_FLAT_ICE = Component.translatable("boathud.tooltip.minimap_flat_ice"),
	MINIMAP_SHOW_OTHER_PLAYERS = Component.translatable("boathud.option.minimap_show_other_players"),
	TIP_MINIMAP_SHOW_OTHER_PLAYERS = Component.translatable("boathud.tooltip.minimap_show_other_players"),
	MINIMAP_SIZE = Component.translatable("boathud.option.minimap_size"),
	TIP_MINIMAP_SIZE = Component.translatable("boathud.tooltip.minimap_size"),
	MINIMAP_BORDER_SIZE = Component.translatable("boathud.option.minimap_border_size"),
	TIP_MINIMAP_BORDER_SIZE = Component.translatable("boathud.tooltip.minimap_border_size"),
	MINIMAP_ZOOM = Component.translatable("boathud.option.minimap_zoom"),
	TIP_MINIMAP_ZOOM = Component.translatable("boathud.tooltip.minimap_zoom"),
	MINIMAP_PLAYER_INDICATOR_SIZE = Component.translatable("boathud.option.minimap_player_indicator_size"),
	TIP_MINIMAP_PLAYER_INDICATOR_SIZE = Component.translatable("boathud.tooltip.minimap_player_indicator_size"),
	MINIMAP_PLAYER_INDICATOR_BORDER_SIZE = Component.translatable("boathud.option.minimap_player_indicator_border_size"),
	TIP_MINIMAP_PLAYER_INDICATOR_BORDER_SIZE = Component.translatable("boathud.tooltip.minimap_player_indicator_border_size"),
		MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Component.translatable("boathud.option.minimap_other_players_indicator_size"),
		TIP_MINIMAP_OTHER_PLAYERS_INDICATOR_SIZE = Component.translatable("boathud.tooltip.minimap_other_players_indicator_size"),
		MINIMAP_OTHER_PLAYERS_HEAD_SIZE = Component.translatable("boathud.option.minimap_other_players_head_size"),
		TIP_MINIMAP_OTHER_PLAYERS_HEAD_SIZE = Component.translatable("boathud.tooltip.minimap_other_players_head_size"),
		MINIMAP_OTHER_PLAYERS_HEAD_FRAME_SIZE = Component.translatable("boathud.option.minimap_other_players_head_frame_size"),
		TIP_MINIMAP_OTHER_PLAYERS_HEAD_FRAME_SIZE = Component.translatable("boathud.tooltip.minimap_other_players_head_frame_size"),
	MINIMAP_SHOW_OTHER_PLAYERS_NAMES = Component.translatable("boathud.option.minimap_show_other_players_names"),
TIP_MINIMAP_SHOW_OTHER_PLAYERS_NAMES = Component.translatable("boathud.tooltip.minimap_show_other_players_names"),
		MINIMAP_OTHER_PLAYERS_NAME_SIZE = Component.translatable("boathud.option.minimap_other_players_name_size"),
		TIP_MINIMAP_OTHER_PLAYERS_NAME_SIZE = Component.translatable("boathud.tooltip.minimap_other_players_name_size"),
		MINIMAP_OTHER_PLAYERS_NAME_OFFSET_Y = Component.translatable("boathud.option.minimap_other_players_name_offset_y"),
		TIP_MINIMAP_OTHER_PLAYERS_NAME_OFFSET_Y = Component.translatable("boathud.tooltip.minimap_other_players_name_offset_y"),
		MINIMAP_OTHER_PLAYERS_NAME_OFFSET_X = Component.translatable("boathud.option.minimap_other_players_name_offset_x"),
		TIP_MINIMAP_OTHER_PLAYERS_NAME_OFFSET_X = Component.translatable("boathud.tooltip.minimap_other_players_name_offset_x"),
		MINIMAP_SPECTATOR_LABEL = Component.translatable("boathud.option.minimap_spectator_label"),
		TIP_MINIMAP_SPECTATOR_LABEL = Component.translatable("boathud.tooltip.minimap_spectator_label"),
MINIMAP_LIMIT_PLAYERS_TO_BOUNDS = Component.translatable("boathud.option.minimap_limit_players_to_bounds"),
TIP_MINIMAP_LIMIT_PLAYERS_TO_BOUNDS = Component.translatable("boathud.tooltip.minimap_limit_players_to_bounds"),
	SHOW_SPEED_BAR = Component.translatable("boathud.option.show_speed_bar"),
		TIP_EXTENDED = Component.translatable("boathud.tooltip.extended"),
		TIP_CAMERA_CONTROL = Component.translatable("boathud.tooltip.camera_control"),
		TIP_CAMERA_AGGRESSIVENESS = Component.translatable("boathud.tooltip.camera_aggressiveness"),
		TIP_CAMERA_SMOOTHING = Component.translatable("boathud.tooltip.camera_smoothing"),
		TIP_BAR = Component.translatable("boathud.tooltip.bar_type"),
		TIP_BAR_PACKED = Component.translatable("boathud.tooltip.bar_type.packed"),
		TIP_BAR_MIXED = Component.translatable("boathud.tooltip.bar_type.mixed"),
		TIP_BAR_BLUE = Component.translatable("boathud.tooltip.bar_type.blue"),
		TIP_BAR_PROGRESSIVE = Component.translatable("boathud.tooltip.bar_type.progressive");
}
