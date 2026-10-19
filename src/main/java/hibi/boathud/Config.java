package hibi.boathud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import java.io.StringReader;
import java.io.StringWriter;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class Config {
	
	/** Format string for speed display on the HUD. Should not be modified directly, use setUnit(). */
	public static String speedFormat = "%03.0f km/h";

	/** Format string for the drift angle display on the HUD. */
	public static final  String angleFormat = "%03.0f °";

	/** Format string for the acceleration display on the HUD. */
	public static final String gFormat = "%+.1f g";

	/** Controls whether or not the HUD should be displayed. */
	public static boolean enabled = true;
	/** Controls whether or not to show all the available details on the HUD. */
	public static boolean extended = true;

	/** Horizontal offset of the main HUD in pixels (positive = right). */
	public static int hudX = 0;
	/** Vertical offset of the main HUD in pixels (positive = down). */
	public static int hudY = 0;
	/** Overall scale of the main HUD (1.0 = default size). */
	public static double hudScale = 1.0d;

	/** Conversion rate between speed unit and m/s. Should not be modified directly, use setUnit(). */
	public static double speedRate = 3.6d;
	/** Speed unit, used for tracking. Should not be modified directly, use setUnit(). */
	public static int configSpeedType = 1;

	// The speed bar type is one of four values in the config file:
	// 0: (Pack) Water and Packed Ice speeds (0 ~ 40 m/s)
	// 1: (Mix) Packed and Blue Ice speeds (10 ~ 70 m/s)
	// 2: (Blue) Blue Ice type speeds (40 ~ 70 m/s)
	// 3: (Prog) All speeds, adapting to the category of speed (0 ~ 70 m/s)
	/** The value in here is a member of the SpeedBar enum. The numbers described above are indexes into this enum. */
	public static SpeedBar barType = SpeedBar.PACKED;

	/** Enables speed-based camera control, akin to Boat Cam. */
	public static boolean cameraControl = false;

	/** Controls from which speed the camera is the most aggressive in look-ahead, or when it completely ignores the boat's rotation (m/t). */
	public static float cameraAggressiveness = 3f;

	/** Controls how strongly the camera is moved (higher means more inertia). */
	public static float cameraSmoothing = 0.45f;

	// Minimap settings
	/** Controls whether or not the minimap should be displayed. */
	public static boolean minimapEnabled = true;
	/** X position of the minimap. */
	public static int minimapX = 10;
	/** Y position of the minimap. */
	public static int minimapY = 10;
	/** Y-axis offset for the minimap. */
	public static int minimapYOffset = 0;
	/** Whether to show all ice heights or only ice at or below player level. */
	public static boolean minimapShowAllHeights = false;
	/** Whether the minimap should lock to north or follow player rotation. */
	public static boolean minimapLockNorth = true;
	/** Whether to show the north direction arrow on the minimap (circle/square shapes only). */
	public static boolean minimapShowNorthIndicator = false;
	/** Minimap panel shape. 0 = circle, 1 = square, 2 = pseudo-3D (tilted top-down). Any other value is reset to circle on load. */
	public static int minimapShape = 0;
	/** Pseudo-3D tilt angle in degrees. 90 = flat (no tilt), 0 = side-on. Only used when minimapShape == 2. */
	public static double minimapTiltAngle = 35.0d;
	// /** Whether the minimap should be square. */
	// public static boolean minimapSquare = false;
	// /** Whether to show the speed bar. */
	// public static boolean showSpeedBar = true;
	/** Ice detection range (blocks above and below player). */
	public static int minimapIceDetectionRange = 3;
	/** Whether to show all ice with the same brightness, ignoring height differences. */
	public static boolean minimapFlatIce = false;
	/** Whether to show other players in boats on the minimap. */
	public static boolean minimapShowOtherPlayers = true;
	/** Size of the minimap in pixels (higher = more detailed but more resource intensive). */
	public static int minimapSize = 128;
	/** Thickness of the minimap panel border in pixels. 0 = no border. */
	public static double minimapBorderSize = 1.0d;
	/** Zoom level for the minimap (controls how much world area is shown). Higher values = more zoomed out. */
	public static double minimapZoom = 1.0d;
	/** Size of the local player indicator on the minimap. */
	public static double minimapPlayerIndicatorSize = 3.0d;
	/** Thickness of the black outline around the local player arrow, in pixels. 0 = no outline. */
	public static double minimapPlayerIndicatorBorderSize = 1.0d;
	/** Size of other players' indicators on the minimap. */
	public static double minimapOtherPlayersIndicatorSize = 2.0d;
	/** Size of other players' head icons on the minimap. The team leather armour colour is used as the icon border. */
	public static double minimapOtherPlayersHeadSize = 6.0d;
	/** Thickness of the team-colour frame around other players' head icons, in pixels. 0 = no frame. */
	public static double minimapOtherPlayersHeadFrameSize = 1.0d;
	/** Whether to show other players' names on the minimap. */
	public static boolean minimapShowOtherPlayersNames = true;
	/** Size of other players' names on the minimap. */
	public static double minimapOtherPlayersNameSize = 1.0d;
	/** Vertical offset of other players' names from the head icon centre, in screen pixels. Negative = above, positive = below. */
	public static double minimapOtherPlayersNameOffsetY = 10.0d;
	/** Horizontal offset of other players' names from the head icon centre, in screen pixels. Negative = left, positive = right. */
	public static double minimapOtherPlayersNameOffsetX = 0.0d;
	/** In spectator mode, hide the local player's own arrow and label the spectated player instead. */
	public static boolean minimapSpectatorLabel = false;
	/** Whether to limit other players' indicators within the minimap boundaries. */
	public static boolean minimapLimitPlayersToBounds = true;

	private Config() {}

	/**
	 * Load the config from disk and into memory. Ideally should be run only once. Wrong and missing settings are silently reset to defaults.
	 */
	public static void load() {
		File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "boathud.properties");
		if(!file.exists()) {
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			Properties prop = new Properties();
			prop.load(br);
			br.close();
			applyProperties(prop);
		}
		catch (Exception e) {
			// Empty catch block
		}
		cameraAggressiveness = Mth.clamp(cameraAggressiveness, 0.2f, 3.5f);
		cameraSmoothing = Mth.clamp(cameraSmoothing, 0f, 0.9f);
		if(hudScale < 0.5d) {
			hudScale = 0.5d;
		} else if(hudScale > 2.0d) {
			hudScale = 2.0d;
		}
		if(minimapShape < 0 || minimapShape > 2) {
			minimapShape = 0;
		}
		if(minimapTiltAngle < 0.0d || minimapTiltAngle > 90.0d) {
			minimapTiltAngle = 35.0d;
		}
	}

	/** Applies a parsed properties set onto the static fields. Shared by load() and clipboard import. */
	public static void applyProperties(Properties prop) {
		int barIndex = 0;
		try {
			if(prop.get("enabled") instanceof String val) {
				enabled = Boolean.parseBoolean(val);
			}
			if(prop.get("extended") instanceof String val) {
				extended = Boolean.parseBoolean(val);
			}
			if(prop.get("hudX") instanceof String val) {
				hudX = Integer.parseInt(val);
			}
			if(prop.get("hudY") instanceof String val) {
				hudY = Integer.parseInt(val);
			}
			if(prop.get("hudScale") instanceof String val) {
				hudScale = Double.parseDouble(val);
			}
			if(prop.get("barType") instanceof String val) {
				barIndex = Integer.parseInt(val);
			}
			if(prop.get("speedUnit") instanceof String val) {
				setUnit(Integer.parseInt(val));
			}
			if(prop.get("cameraControl") instanceof String val) {
				cameraControl = Boolean.parseBoolean(val);
			}
			if(prop.get("cameraAggressiveness") instanceof String val) {
				cameraAggressiveness = Float.parseFloat(val);
			}
			if(prop.get("cameraSmoothing") instanceof String val) {
				cameraSmoothing = Float.parseFloat(val);
			}
			// Minimap settings
	if(prop.get("minimapEnabled") instanceof String val) {
		minimapEnabled = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapX") instanceof String val) {
		minimapX = Integer.parseInt(val);
	}
	if(prop.get("minimapY") instanceof String val) {
		minimapY = Integer.parseInt(val);
	}
	if(prop.get("minimapYOffset") instanceof String val) {
			minimapYOffset = Integer.parseInt(val);
		}
		// if(prop.get("minimapSquare") instanceof String val) {
		// 	minimapSquare = Boolean.parseBoolean(val);
		// }
		if(prop.get("minimapShowAllHeights") instanceof String val) {
			minimapShowAllHeights = Boolean.parseBoolean(val);
		}
	if(prop.get("minimapLockNorth") instanceof String val) {
		minimapLockNorth = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapShowNorthIndicator") instanceof String val) {
		minimapShowNorthIndicator = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapShape") instanceof String val) {
		minimapShape = Integer.parseInt(val);
	}
	if(prop.get("minimapTiltAngle") instanceof String val) {
		minimapTiltAngle = Double.parseDouble(val);
	}
	if(prop.get("minimapIceDetectionRange") instanceof String val) {
		minimapIceDetectionRange = Integer.parseInt(val);
	}
	if(prop.get("minimapFlatIce") instanceof String val) {
		minimapFlatIce = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapShowOtherPlayers") instanceof String val) {
		minimapShowOtherPlayers = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapSize") instanceof String val) {
		minimapSize = Integer.parseInt(val);
	}
	if(prop.get("minimapBorderSize") instanceof String val) {
		minimapBorderSize = Double.parseDouble(val);
	}
	if(prop.get("minimapZoom") instanceof String val) {
		minimapZoom = Double.parseDouble(val);
	}
	if(prop.get("minimapPlayerIndicatorSize") instanceof String val) {
			minimapPlayerIndicatorSize = Double.parseDouble(val);
		}
		if(prop.get("minimapPlayerIndicatorBorderSize") instanceof String val) {
			minimapPlayerIndicatorBorderSize = Double.parseDouble(val);
		}
		if(prop.get("minimapOtherPlayersIndicatorSize") instanceof String val) {
			minimapOtherPlayersIndicatorSize = Double.parseDouble(val);
		}
		if(prop.get("minimapOtherPlayersHeadSize") instanceof String val) {
			minimapOtherPlayersHeadSize = Double.parseDouble(val);
		}
		if(prop.get("minimapOtherPlayersHeadFrameSize") instanceof String val) {
			minimapOtherPlayersHeadFrameSize = Double.parseDouble(val);
		}
		if(prop.get("minimapShowOtherPlayersNames") instanceof String val) {
			minimapShowOtherPlayersNames = Boolean.parseBoolean(val);
		}
		if(prop.get("minimapOtherPlayersNameSize") instanceof String val) {
				minimapOtherPlayersNameSize = Double.parseDouble(val);
			}
			// Legacy keys (above/gap) migrated into the unified Y offset: text top relative to the
			// head centre. Head size default 6, font line height 9.
			if(prop.get("minimapOtherPlayersNameAbove") instanceof String val) {
				boolean legacyAbove = Boolean.parseBoolean(val);
				double legacyGap = 4.0d;
				if(prop.get("minimapOtherPlayersNameOffset") instanceof String gapVal) {
					legacyGap = Double.parseDouble(gapVal);
				}
				minimapOtherPlayersNameOffsetY = legacyAbove ? -(6.0d + legacyGap + 9.0d) : (6.0d + legacyGap);
			}
			if(prop.get("minimapOtherPlayersNameOffsetX") instanceof String val) {
				minimapOtherPlayersNameOffsetX = Double.parseDouble(val);
			}
			if(prop.get("minimapOtherPlayersNameOffsetY") instanceof String val) {
				minimapOtherPlayersNameOffsetY = Double.parseDouble(val);
			}
			if(prop.get("minimapSpectatorLabel") instanceof String val) {
				minimapSpectatorLabel = Boolean.parseBoolean(val);
			}
			if(prop.get("minimapLimitPlayersToBounds") instanceof String val) {
					minimapLimitPlayersToBounds = Boolean.parseBoolean(val);
				}
				// if(prop.get("showSpeedBar") instanceof String val) {
				// showSpeedBar = Boolean.parseBoolean(val);
				// }
		}
		catch (Exception e) {
			// Empty catch block
		}
		// Sanity check
		if(barIndex > 3 || barIndex < 0) {
			barIndex = 0;
		}
		barType = SpeedBar.values()[barIndex];
	}

	/**
	 * Save the config from memory and onto disk. Ideally, should only be run when the settings are changed.
	 */
	public static void save() {
		File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "boathud.properties");
		try {
			FileWriter writer = new FileWriter(file);
			writer.write(toPropertiesString());
			// Minimap settings
			writer.close();
		}
		catch (Exception ignored) {
		}
	}

	/** Serialises the current config into the properties text format used on disk. */
	public static String toPropertiesString() {
		StringWriter sw = new StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		pw.println("enabled " + Boolean.toString(enabled) + "\n");
		pw.println("extended " + Boolean.toString(extended) + "\n");
		pw.println("hudX " + Integer.toString(hudX) + "\n");
		pw.println("hudY " + Integer.toString(hudY) + "\n");
		pw.println("hudScale " + Double.toString(hudScale) + "\n");
		pw.println("barType " + Integer.toString(barType.ordinal()) + "\n");
		pw.println("speedUnit " + Integer.toString(configSpeedType) + "\n");
		pw.println("cameraControl " + Boolean.toString(cameraControl) + "\n");
		pw.println("cameraAggressiveness " + Float.toString(cameraAggressiveness) + "\n");
		pw.println("cameraSmoothing " + Float.toString(cameraSmoothing) + "\n");
		pw.println("minimapEnabled " + Boolean.toString(minimapEnabled) + "\n");
		pw.println("minimapX " + Integer.toString(minimapX) + "\n");
		pw.println("minimapY " + Integer.toString(minimapY) + "\n");
		pw.println("minimapYOffset " + Integer.toString(minimapYOffset) + "\n");
		pw.println("minimapShowAllHeights " + Boolean.toString(minimapShowAllHeights) + "\n");
		pw.println("minimapLockNorth " + Boolean.toString(minimapLockNorth) + "\n");
		pw.println("minimapShowNorthIndicator " + Boolean.toString(minimapShowNorthIndicator) + "\n");
		pw.println("minimapShape " + Integer.toString(minimapShape) + "\n");
		pw.println("minimapTiltAngle " + Double.toString(minimapTiltAngle) + "\n");
		pw.println("minimapIceDetectionRange " + Integer.toString(minimapIceDetectionRange) + "\n");
		pw.println("minimapFlatIce " + Boolean.toString(minimapFlatIce) + "\n");
		pw.println("minimapShowOtherPlayers " + Boolean.toString(minimapShowOtherPlayers) + "\n");
		pw.println("minimapSize " + Integer.toString(minimapSize) + "\n");
		pw.println("minimapBorderSize " + Double.toString(minimapBorderSize) + "\n");
		pw.println("minimapZoom " + Double.toString(minimapZoom) + "\n");
		pw.println("minimapPlayerIndicatorSize " + Double.toString(minimapPlayerIndicatorSize) + "\n");
		pw.println("minimapPlayerIndicatorBorderSize " + Double.toString(minimapPlayerIndicatorBorderSize) + "\n");
		pw.println("minimapOtherPlayersIndicatorSize " + Double.toString(minimapOtherPlayersIndicatorSize) + "\n");
		pw.println("minimapOtherPlayersHeadSize " + Double.toString(minimapOtherPlayersHeadSize) + "\n");
		pw.println("minimapOtherPlayersHeadFrameSize " + Double.toString(minimapOtherPlayersHeadFrameSize) + "\n");
		pw.println("minimapShowOtherPlayersNames " + Boolean.toString(minimapShowOtherPlayersNames) + "\n");
		pw.println("minimapOtherPlayersNameSize " + Double.toString(minimapOtherPlayersNameSize) + "\n");
		pw.println("minimapOtherPlayersNameOffsetX " + Double.toString(minimapOtherPlayersNameOffsetX) + "\n");
		pw.println("minimapOtherPlayersNameOffsetY " + Double.toString(minimapOtherPlayersNameOffsetY) + "\n");
		pw.println("minimapSpectatorLabel " + Boolean.toString(minimapSpectatorLabel) + "\n");
		pw.println("minimapLimitPlayersToBounds " + Boolean.toString(minimapLimitPlayersToBounds) + "\n");
		pw.close();
		return sw.toString();
	}

	/** Copies the current config to the system clipboard. Returns true on success. */
	public static boolean exportToClipboard() {
		try {
			Minecraft.getInstance().keyboardHandler.setClipboard(toPropertiesString());
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/** Reads config text from the system clipboard, applies it and saves to disk. Returns true on success. */
	public static boolean importFromClipboard() {
		String text = Minecraft.getInstance().keyboardHandler.getClipboard();
		if(text == null || text.isBlank()) {
			return false;
		}
		try {
			Properties prop = new Properties();
			prop.load(new StringReader(text));
			if(!prop.containsKey("enabled") && !prop.containsKey("minimapEnabled")) {
				return false; // Not a boathud config
			}
			applyProperties(prop);
			save();
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Sets the speed unit.
	 * @param type 0 for m/s, 1 for km/h (default), 2 for mph, 3 for knots.
	 */
	public static void setUnit(int type) {
		switch(type) {
		case 0:
			Config.speedRate = 1d;
			Config.speedFormat = "%03.0f m/s";
			Config.configSpeedType = 0;
			break;
		case 2:
			Config.speedRate = 2.236936d;
			Config.speedFormat = "%03.0f mph";
			Config.configSpeedType = 2;
			break;
		case 3:
			Config.speedRate = 1.943844d;
			Config.speedFormat = "%03.0f kt";
			Config.configSpeedType = 3;
			break;
		case 1:
			default:
			Config.speedRate = 3.6d;
			Config.speedFormat = "%03.0f km/h";
			Config.configSpeedType = 1;
			break;
		}
	}
}
