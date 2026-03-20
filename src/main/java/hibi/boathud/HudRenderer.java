package hibi.boathud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import org.joml.Quaternionf;
import java.util.HashMap;
import java.util.Map;

public class HudRenderer {

	private final MinecraftClient client;
	private int scaledWidth;
	private int scaledHeight;

	// Cache for minimap to avoid recalculating every frame
	private static class MinimapCache {
		BlockPos lastPlayerPos = null;
		int lastPlayerY = 0;
		float lastYaw = 0f;
		float smoothYaw = 0f; // For smooth rotation
		Map<BlockPos, Integer> iceCache = new HashMap<>();
		boolean needsUpdate = true;
	}
	private MinimapCache minimapCache = new MinimapCache();
	
	// Pre-rendered minimap texture cache
	private int[] preRenderedMinimap;
	private int minimapSize = 256;
	private BlockPos lastRenderedPos = null;
	private int lastRenderedY = 0;

	// The index to be used in these scales is the bar type (stored internally as an integer, defined in Config)
	//                                        Pack     Mixed      Blue
	private static final double[] MIN_V =   {   0d,       8d,      40d}; // Minimum display speed (m/s)
	private static final double[] MAX_V =   {  40d,      70d,      70d}; // Maximum display speed (m/s)
	private static final double[] SCALE_V = {4.55d, 182d/62d, 182d/30d}; // Pixels for 1 unit of speed (px*s/m) (BarWidth / (VMax - VMin))

	// Used for lerping
	private double displayedSpeed = 0.0d;

	public HudRenderer(MinecraftClient client) {
		this.client = client;
	}

	public void render(DrawContext graphics, RenderTickCounter counter) {
		this.scaledWidth = this.client.getWindow().getScaledWidth();
		this.scaledHeight = this.client.getWindow().getScaledHeight();
		int i = this.scaledWidth / 2;
		int nameLen = this.client.textRenderer.getWidth(Common.hudData.name);
	
		// Lerping the displayed speed with the actual speed against how far we are into the tick not only is mostly accurate,
		// but gives the impression that it's being updated faster than 20 hz (which it isn't)
		this.displayedSpeed = MathHelper.lerp(counter.getTickDelta(false), this.displayedSpeed, Common.hudData.speed);
	
		if(Config.extended) {
			// Overlay texture and bar
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, BACKGROUND_EXTENDED, i - 91, this.scaledHeight - 83, 182, 33);
			if(Config.showSpeedBar) {
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);
			}
	
			// Sprites
			if(Common.hudData.isDriver) {
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, this.client.options.leftKey.isPressed()? LEFT_LIT : LEFT_UNLIT, i - 86, this.scaledHeight - 65, 17, 8);
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, this.client.options.rightKey.isPressed()? RIGHT_LIT : RIGHT_UNLIT, i - 63, this.scaledHeight - 65, 17, 8);
				// Brake-throttle bar
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, this.client.options.forwardKey.isPressed()? FORWARD_LIT : FORWARD_UNLIT, i, this.scaledHeight - 55, 61, 5);
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, this.client.options.backKey.isPressed()? BACKWARD_LIT : BACKWARD_UNLIT, i - 61, this.scaledHeight - 55, 61, 5);
			}
	
			// Ping
			this.renderPing(graphics, i + 75 - nameLen, this.scaledHeight - 65);
					
			// Text
			// First Row
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i, this.scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.gFormat, Common.hudData.g), i + 58, this.scaledHeight - 76, 0xFFFFFF);
			// Second Row
			graphics.drawTextWithShadow(this.client.textRenderer, Common.hudData.name, i + 88 - nameLen, this.scaledHeight - 65, 0xFFFFFF);
	
		} else { // Compact mode
			// Overlay texture and bar
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, BACKGROUND_COMPACT, i - 91, this.scaledHeight - 83, 182, 20);
			if(Config.showSpeedBar) {
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);
			}
			// Speed and drift angle
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i + 58, this.scaledHeight - 76, 0xFFFFFF);
		}
	
		// Render minimap if enabled (it will be rendered separately by the mixin if not riding boat)
		if(Config.minimapEnabled) {
			this.renderMinimap(graphics);
		}
	}

	/** Renders the minimap showing ice blocks with optimized caching and proper rotation */
	public void renderMinimap(DrawContext graphics) {
		if(this.client.player == null || this.client.world == null) return;

		// Get player position and rotation
		Vec3d playerPos = this.client.player.getPos();
		BlockPos playerBlockPos = this.client.player.getBlockPos();
		float playerYaw = this.client.player.getYaw();

		// Check if cache needs update (player moved)
		boolean positionChanged = lastRenderedPos == null || 
			!lastRenderedPos.equals(playerBlockPos) ||
			lastRenderedY != playerBlockPos.getY();

		// Pre-render map to array if position changed
		if(positionChanged) {
			preRenderMinimap(playerBlockPos);
			lastRenderedPos = playerBlockPos;
			lastRenderedY = playerBlockPos.getY();
			minimapCache.needsUpdate = true;
		}

		// Calculate minimap position
		int posX = Config.minimapX;
		int posY = Config.minimapY;
		double scale = Config.minimapScale;

		// Calculate actual rendered size
		int renderedSize = (int)(minimapSize * scale);
		int renderedHalfSize = renderedSize / 2;

		// Draw minimap background
		graphics.fill(posX, posY, posX + renderedSize, posY + renderedSize, 0x40000000);

		// Calculate center position
		int centerX = posX + renderedHalfSize;
		int centerY = posY + renderedHalfSize;

		// Apply rotation using matrix stack
		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY, 0);

		// Smooth rotation using lerp
		float targetYaw = -playerYaw;
		float smoothFactor = 0.1f; // Adjust for smoother rotation
		
		// Calculate shortest rotation path
		float yawDiff = targetYaw - minimapCache.smoothYaw;
		if(yawDiff > 180.0f) {
			yawDiff -= 360.0f;
		} else if(yawDiff < -180.0f) {
			yawDiff += 360.0f;
		}
		
		// Apply lerp to get smooth yaw
		minimapCache.smoothYaw += yawDiff * smoothFactor;
		
		// Rotate map to match player's view direction with smooth rotation
		float rotation = (float)Math.toRadians(minimapCache.smoothYaw);
		// Use Quaternionf for rotation in Minecraft 1.21.3
		Quaternionf quaternion = new Quaternionf().rotateZ(rotation);
		graphics.getMatrices().multiply(quaternion);

		// Draw ice blocks with rotation applied
		if(preRenderedMinimap != null) {
			for(int x = 0; x < minimapSize; x++) {
				for(int z = 0; z < minimapSize; z++) {
					int color = preRenderedMinimap[x + z * minimapSize];
					if((color >>> 24) == 0) continue; // Skip transparent pixels

					// Calculate relative position
					double relX = (x - (double) minimapSize / 2 + 0.5) * scale;
					double relZ = (z - (double) minimapSize / 2 + 0.5) * scale;

					// Draw the pixel with rotation applied
					graphics.fill((int)relX, (int)relZ, (int)relX + 1, (int)relZ + 1, color);
				}
			}
		}

		// Pop the matrix stack to reset rotation for player indicator
		graphics.getMatrices().pop();

		// Clear cache after each render to prevent memory leaks
		minimapCache.iceCache.clear();
		minimapCache.needsUpdate = false;

		// Draw player indicator at center with configurable shape
		int indicatorSize = (int)(5 * scale);
		
		// Draw player indicator
		if(Config.minimapSquare) {
			// Square shape with black border
			// Draw black border
			graphics.fill(centerX - indicatorSize - 1, centerY - indicatorSize - 1, 
			              centerX + indicatorSize + 2, centerY + indicatorSize + 2, 0xFF000000);
			// Draw red square
			graphics.fill(centerX - indicatorSize, centerY - indicatorSize, 
			              centerX + indicatorSize + 1, centerY + indicatorSize + 1, 0xFFFF0000);
		} else {
			// Circle shape with black border (default)
			// Draw black border circle
			for(int dx = -indicatorSize - 1; dx <= indicatorSize + 1; dx++) {
				for(int dy = -indicatorSize - 1; dy <= indicatorSize + 1; dy++) {
					if(dx * dx + dy * dy <= (indicatorSize + 1) * (indicatorSize + 1)) {
						graphics.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, 0xFF000000);
					}
				}
			}
			// Draw red circle
			for(int dx = -indicatorSize; dx <= indicatorSize; dx++) {
				for(int dy = -indicatorSize; dy <= indicatorSize; dy++) {
					if(dx * dx + dy * dy <= indicatorSize * indicatorSize) {
						graphics.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, 0xFFFF0000);
					}
				}
			}
		}

		// Draw direction indicator (north arrow)
		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY - renderedHalfSize + 10, 0);
		// Use smooth yaw for the direction indicator as well
		// Use Quaternionf for rotation in Minecraft 1.21.3
		float arrowRotation = (float)Math.toRadians(-minimapCache.smoothYaw);
		Quaternionf arrowQuaternion = new Quaternionf().rotateZ(arrowRotation);
		graphics.getMatrices().multiply(arrowQuaternion);
		// Draw north arrow
		graphics.fill(-2, -8, 2, 0, 0xFFFFFF);
		graphics.fill(-3, 0, 3, 2, 0xFFFFFF);
		graphics.getMatrices().pop();

		// Draw minimap border
		graphics.fill(posX, posY, posX + renderedSize, posY + 1, 0xFFFFFFFF); // Top
		graphics.fill(posX, posY + renderedSize - 1, posX + renderedSize, posY + renderedSize, 0xFFFFFFFF); // Bottom
		graphics.fill(posX, posY, posX + 1, posY + renderedSize, 0xFFFFFFFF); // Left
		graphics.fill(posX + renderedSize - 1, posY, posX + renderedSize, posY + renderedSize, 0xFFFFFFFF); // Right
	}
	
	/** Pre-renders the minimap to an integer array with circular mask (from NFS mod) */
	private void preRenderMinimap(BlockPos playerBlockPos) {
		if(this.client.world == null) return;
		
		preRenderedMinimap = new int[minimapSize * minimapSize];
		int centerX = minimapSize / 2;
		int centerZ = minimapSize / 2;
		float radius = ((float) minimapSize / 2) - 1; // Circular mask radius
		int renderDistance = minimapSize / 2; // Blocks to render around the player
		
		// First pass: Render ice blocks to array with improved detection
		for(int x = 0; x < minimapSize; x++) {
			for(int z = 0; z < minimapSize; z++) {
				// Calculate world coordinates
				int worldX = playerBlockPos.getX() + (x - centerX);
				int worldZ = playerBlockPos.getZ() + (z - centerZ);
				
				// Check distance from player (circular mask during pre-render)
				double dx = x - centerX;
				double dz = z - centerZ;
				double distance = Math.sqrt(dx * dx + dz * dz);
				
				if(distance > radius) {
					preRenderedMinimap[x + z * minimapSize] = 0; // Transparent outside circle
					continue;
				}
				
				// Check blocks at different Y levels for better coverage
				int bestAlpha = 0;
				int bestColor = 0;
				
				// Check current Y level and nearby levels
				for(int yOffset = -2; yOffset <= 2; yOffset++) {
					BlockPos blockPos = new BlockPos(worldX, playerBlockPos.getY() + Config.minimapYOffset + yOffset, worldZ);
					BlockState blockState = this.client.world.getBlockState(blockPos);
					Block block = blockState.getBlock();
					
					// Check if it's an ice block we want to render
					if(block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) {
						// Calculate Y difference for transparency
						int yDiff = Math.abs(blockPos.getY() - playerBlockPos.getY());
						float alphaFactor = 1.0f - (yDiff / 10.0f);
						alphaFactor = MathHelper.clamp(alphaFactor, 0.2f, 1.0f);
						
						// Different ice types have different brightness
						float brightness;
						if(block == Blocks.BLUE_ICE) {
							brightness = 1.0f; // Blue ice is brightest
						} else if(block == Blocks.PACKED_ICE) {
							brightness = 0.9f; // Packed ice is slightly less bright
						} else {
							brightness = 0.8f; // Regular ice is least bright
						}
						
						// Combine brightness with alpha factor
						int alpha = (int)(255 * alphaFactor);
						int gray = (int)(brightness * 255);
						
						// Calculate final color
						int color = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
						
						// Keep the most visible ice block (highest alpha)
						if(alpha > bestAlpha) {
							bestAlpha = alpha;
							bestColor = color;
						}
					}
				}
				
				preRenderedMinimap[x + z * minimapSize] = bestColor;
			}
		}
		
		// Second pass: Apply smooth edges to the circular mask
		for(int x = 0; x < minimapSize; x++) {
			for(int z = 0; z < minimapSize; z++) {
				double dx = x - centerX;
				double dz = z - centerZ;
				double distance = Math.sqrt(dx * dx + dz * dz);
				
				int index = x + z * minimapSize;
				int color = preRenderedMinimap[index];
				int alpha = (color >>> 24) & 0xFF;
				
				// Apply smooth circular edge
				if(alpha > 0 && distance > radius - 3) {
					float edgeAlpha = (float)((radius - distance) / 3.0);
					edgeAlpha = MathHelper.clamp(edgeAlpha, 0.0f, 1.0f);
					int newAlpha = (int)(alpha * edgeAlpha);
					if(newAlpha < 5) newAlpha = 0;
					int gray = color & 0xFF;
					preRenderedMinimap[index] = (newAlpha << 24) | (gray << 16) | (gray << 8) | gray;
				}
			}
		}
	}

	/** Renders the speed bar atop the HUD, uses displayedSpeed to, well, diisplay the speed. */
	private void renderBar(DrawContext graphics, int x, int y) {
		graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_OFF[Config.barType], x, y, 182, 5);
		if(Common.hudData.speed < MIN_V[Config.barType]) return;
		if(Common.hudData.speed > MAX_V[Config.barType]) {
			if(this.client.world != null && this.client.world.getTime() % 2 == 0) return;
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_ON[Config.barType], x, y, 182, 5);
			return;
		}
		graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_ON[Config.barType], 182, 5, 0, 0, x, y, (int)((this.displayedSpeed - MIN_V[Config.barType]) * SCALE_V[Config.barType]), 5);
	}

	/** Implementation is cloned from the notchian ping display in the tab player list.	 */
	private void renderPing(DrawContext graphics, int x, int y) {
		Identifier bar = PING_5;
		if(Common.hudData.ping < 0) {
			bar = PING_UNKNOWN;
		}
		else if(Common.hudData.ping < 150) {
			bar = PING_5;
		}
		else if(Common.hudData.ping < 300) {
			bar = PING_4;
		}
		else if(Common.hudData.ping < 600) {
			bar = PING_3;
		}
		else if(Common.hudData.ping < 1000) {
			bar = PING_2;
		}
		else {
			bar = PING_1;
		}
		graphics.drawGuiTexture(RenderLayer::getGuiTextured, bar, x, y, 10, 8);
	}

	/** Renders a piece of text centered horizontally on an X coordinate. */
	private void typeCentered(DrawContext graphics, String text, int centerX, int y, int color) {
		graphics.drawTextWithShadow(this.client.textRenderer, text, centerX - this.client.textRenderer.getWidth(text) / 2, y, color);
	}

	private static final Identifier
		BACKGROUND_EXTENDED = Identifier.of("boathud", "background_extended"),
		BACKGROUND_COMPACT = Identifier.of("boathud", "background_compact"),
		LEFT_UNLIT = Identifier.of("boathud", "left_unlit"),
		LEFT_LIT = Identifier.of("boathud", "left_lit"),
		RIGHT_UNLIT = Identifier.of("boathud", "right_unlit"),
		RIGHT_LIT = Identifier.of("boathud", "right_lit"),
		FORWARD_UNLIT = Identifier.of("boathud", "forward_unlit"),
		FORWARD_LIT = Identifier.of("boathud", "forward_lit"),
		BACKWARD_UNLIT = Identifier.of("boathud", "backward_unlit"),
		BACKWARD_LIT = Identifier.of("boathud", "backward_lit"),
		BAR_1_UNLIT = Identifier.of("boathud", "bar_1_unlit"),
		BAR_1_LIT = Identifier.of("boathud", "bar_1_lit"),
		BAR_2_UNLIT = Identifier.of("boathud", "bar_2_unlit"),
		BAR_2_LIT = Identifier.of("boathud", "bar_2_lit"),
		BAR_3_UNLIT = Identifier.of("boathud", "bar_3_unlit"),
		BAR_3_LIT = Identifier.of("boathud", "bar_3_lit"),
		PING_5 = Identifier.of("boathud", "ping_5"),
		PING_4 = Identifier.of("boathud", "ping_4"),
		PING_3 = Identifier.of("boathud", "ping_3"),
		PING_2 = Identifier.of("boathud", "ping_2"),
		PING_1 = Identifier.of("boathud", "ping_1"),
		PING_UNKNOWN = Identifier.of("boathud", "ping_unknown")
	;
	private static final Identifier[] BAR_OFF = {BAR_1_UNLIT, BAR_2_UNLIT, BAR_3_UNLIT};
	private static final Identifier[] BAR_ON = {BAR_1_LIT, BAR_2_LIT, BAR_3_LIT};
}
