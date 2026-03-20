package hibi.boathud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

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

	/** Conversion rate between speed unit and m/s. Should not be modified directly, use setUnit(). */
	public static double speedRate = 3.6d;
	/** Speed unit, used for tracking. Should not be modified directly, use setUnit(). */
	public static int configSpeedType = 1;

	// The speed bar type is one of three values:
	// 0: (Pack) Water and Packed Ice speeds (0 ~ 40 m/s)
	// 1: (Mix) Packed and Blue Ice speeds (10 ~ 70 m/s)
	// 2: (Blue) Blue Ice type speeds (40 ~ 70 m/s)
	/** Setting a value that's not between 0 and 2 *will* cause an IndexOutOfBounds */
	public static int barType = 0;

	// Minimap settings
	/** Controls whether or not the minimap should be displayed. */
	public static boolean minimapEnabled = true;
	/** X position of the minimap. */
	public static int minimapX = 10;
	/** Y position of the minimap. */
	public static int minimapY = 10;
	/** Y-axis offset for the minimap. */
	public static int minimapYOffset = 0;
	/** Whether the minimap should be square. */
	public static boolean minimapSquare = false;
	/** Whether to show all ice heights or only ice at or below player level. */
	public static boolean minimapShowAllHeights = false;
	/** Whether the minimap should lock to north or follow player rotation. */
	public static boolean minimapLockNorth = true;
	/** Whether to show the speed bar. */
	public static boolean showSpeedBar = true;
	/** Ice detection range (blocks above and below player). */
	public static int minimapIceDetectionRange = 3;
	/** Whether to show all ice with the same brightness, ignoring height differences. */
	public static boolean minimapFlatIce = false;
	/** Whether to show other players in boats on the minimap. */
	public static boolean minimapShowOtherPlayers = true;
	/** Size of the minimap in pixels (higher = more detailed but more resource intensive). */
	public static int minimapSize = 128;
	/** Zoom level for the minimap (controls how much world area is shown). Higher values = more zoomed out. */
	public static double minimapZoom = 1.0d;

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
			if(prop.get("enabled") instanceof String val) {
				enabled = Boolean.parseBoolean(val);
			}
			if(prop.get("extended") instanceof String val) {
				extended = Boolean.parseBoolean(val);
			}
			if(prop.get("barType") instanceof String val) {
				barType = Integer.parseInt(val);
			}
			if(prop.get("speedUnit") instanceof String val) {
				setUnit(Integer.parseInt(val));
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
	if(prop.get("minimapSquare") instanceof String val) {
		minimapSquare = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapShowAllHeights") instanceof String val) {
		minimapShowAllHeights = Boolean.parseBoolean(val);
	}
	if(prop.get("minimapLockNorth") instanceof String val) {
		minimapLockNorth = Boolean.parseBoolean(val);
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
	if(prop.get("minimapZoom") instanceof String val) {
		minimapZoom = Double.parseDouble(val);
	}
			if(prop.get("showSpeedBar") instanceof String val) {
				showSpeedBar = Boolean.parseBoolean(val);
			}
		}
		catch (Exception e) {
			// Empty catch block
		}
		// Sanity check
		if(barType > 2 || barType < 0) {
			barType = 0;
		}
	}

	/**
	 * Save the config from memory and onto disk. Ideally, should only be run when the settings are changed.
	 */
	public static void save() {
		File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "boathud.properties");
		try {
			FileWriter writer = new FileWriter(file);
			writer.write("enabled " + Boolean.toString(enabled) + "\n");
			writer.write("extended " + Boolean.toString(extended) + "\n");
			writer.write("barType " + Integer.toString(barType) + "\n");
			writer.write("speedUnit " + Integer.toString(configSpeedType) + "\n");
			// Minimap settings
		writer.write("minimapEnabled " + Boolean.toString(minimapEnabled) + "\n");
		writer.write("minimapX " + Integer.toString(minimapX) + "\n");
	writer.write("minimapY " + Integer.toString(minimapY) + "\n");
	writer.write("minimapYOffset " + Integer.toString(minimapYOffset) + "\n");
		writer.write("minimapSquare " + Boolean.toString(minimapSquare) + "\n");
		writer.write("minimapShowAllHeights " + Boolean.toString(minimapShowAllHeights) + "\n");
		writer.write("minimapLockNorth " + Boolean.toString(minimapLockNorth) + "\n");
		writer.write("minimapIceDetectionRange " + Integer.toString(minimapIceDetectionRange) + "\n");
		writer.write("minimapFlatIce " + Boolean.toString(minimapFlatIce) + "\n");
		writer.write("minimapShowOtherPlayers " + Boolean.toString(minimapShowOtherPlayers) + "\n");
	writer.write("minimapSize " + Integer.toString(minimapSize) + "\n");
	writer.write("minimapZoom " + Double.toString(minimapZoom) + "\n");
	writer.write("showSpeedBar " + Boolean.toString(showSpeedBar) + "\n");
			writer.close();
		}
		catch (Exception ignored) {
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
