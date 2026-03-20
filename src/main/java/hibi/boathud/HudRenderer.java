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

import static hibi.boathud.Config.minimapSize;

public class HudRenderer {

	private final MinecraftClient client;
	private int scaledWidth;
	private int scaledHeight;

	// Cache for minimap to avoid recalculating every frame
	// Make this non-static to avoid memory leaks when the HUD renderer is recreated
	private class MinimapCache {
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
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);

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
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);
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
		if(this.client.world == null) return;

		// Get camera entity (local player or spectated player)
		net.minecraft.entity.Entity cameraEntity = this.client.getCameraEntity();
		if(cameraEntity == null) return;

		// Get player position and rotation
		Vec3d playerPos = cameraEntity.getPos();
		BlockPos playerBlockPos = cameraEntity.getBlockPos();
		float playerYaw = cameraEntity.getYaw();

		// Always pre-render the map every frame to avoid race conditions and array out of bounds errors
		preRenderMinimap(playerPos);

		// Calculate minimap position
		int posX = Config.minimapX;
		int posY = Config.minimapY;
		double scale = 1.0d; // Fixed scale, no longer configurable

		// Calculate actual rendered size
		int renderedSize = (int)(Config.minimapSize * scale);
		int renderedHalfSize = renderedSize / 2;

		// Calculate center position
		int centerX = posX + renderedHalfSize;
		int centerY = posY + renderedHalfSize;

		// Draw circular minimap background
		int radius = renderedSize / 2;
		// Use custom circle drawing since fillEllipse is not available
		drawCircleFill(graphics, centerX, centerY, radius, 0x40000000);

		// Apply rotation using matrix stack
		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY, 0);

		// Apply rotation only if not locked to north
		if(!Config.minimapLockNorth) {
			// Calculate target rotation angle to match player's forward direction
			// When player looks forward, map should show forward direction correctly
			// Fixing direction by adjusting rotation calculation
			float targetYaw = playerYaw + 180.0f; // Add 180 degrees to get correct forward direction
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
			// Minecraft yaw increases clockwise, OpenGL rotation is counter-clockwise, so invert
			float rotation = (float)Math.toRadians(-minimapCache.smoothYaw);
			// Use Quaternionf for rotation in Minecraft 1.21.3
			Quaternionf quaternion = new Quaternionf().rotateZ(rotation);
			graphics.getMatrices().multiply(quaternion);
		} else {
			// Locked to north, so no rotation needed
			minimapCache.smoothYaw = 0.0f; // Reset smooth yaw to north
		}

		// Draw ice blocks with rotation applied, only within circular area
		if(preRenderedMinimap != null) {
			// First draw black borders for ice edges
			for(int x = 0; x < Config.minimapSize; x++) {
				for(int z = 0; z < Config.minimapSize; z++) {
					int color = preRenderedMinimap[x + z * Config.minimapSize];
					if((color >>> 24) == 0) continue; // Skip transparent pixels
					
					// Check if this is an edge pixel by looking at adjacent pixels
					boolean isEdge = false;
					for(int dx = -1; dx <= 1; dx++) {
						for(int dz = -1; dz <= 1; dz++) {
							if(dx == 0 && dz == 0) continue; // Skip current pixel
							int nx = x + dx;
							int nz = z + dz;
							if(nx >= 0 && nx < Config.minimapSize && nz >= 0 && nz < Config.minimapSize) {
								int neighborColor = preRenderedMinimap[nx + nz * Config.minimapSize];
								if((neighborColor >>> 24) == 0) {
									isEdge = true;
									break;
								}
							} else {
								// Edge of the map is considered an edge
								isEdge = true;
								break;
							}
						}
						if(isEdge) break;
					}
					
					if(isEdge) {
				// Calculate relative position for border pixel
				double relX = (x - (double) Config.minimapSize / 2 + 0.5) * scale;
				double relZ = (z - (double) Config.minimapSize / 2 + 0.5) * scale;
				
				// Check if pixel is within circle radius
				if(relX * relX + relZ * relZ <= radius * radius) {
					// Draw black border pixel
					graphics.fill((int)relX, (int)relZ, (int)relX + 1, (int)relZ + 1, 0xFF000000);
				}
			}
		}
	}
	
	// Then draw the regular ice blocks on top
	for(int x = 0; x < Config.minimapSize; x++) {
		for(int z = 0; z < Config.minimapSize; z++) {
			int color = preRenderedMinimap[x + z * Config.minimapSize];
			if((color >>> 24) == 0) continue; // Skip transparent pixels

			// Calculate relative position
			double relX = (x - (double) Config.minimapSize / 2 + 0.5) * scale;
			double relZ = (z - (double) Config.minimapSize / 2 + 0.5) * scale;

					// Check if pixel is within circle radius
					if(relX * relX + relZ * relZ <= radius * radius) {
						// Draw the pixel with rotation applied
						graphics.fill((int)relX, (int)relZ, (int)relX + 1, (int)relZ + 1, color);
					}
				}
			}
		}

		// Pop the matrix stack to reset rotation for player indicator
		graphics.getMatrices().pop();

		// Draw player indicator at center - upward pointing triangle with customizable size
		// Make triangle taller and more pointed (height multiplier increased from 1.5 to 2.0)
		int indicatorSize = (int)(Config.minimapPlayerIndicatorSize * scale); // Use customizable size
		int triangleHeight = (int)(indicatorSize * 2.0); // Taller, more pointed triangle
		
		// Draw black border triangle (slightly larger)
		int borderSize = 1;
		// Custom triangle drawing since fillTriangle is not available
		drawTriangle(graphics, 
			centerX, centerY - triangleHeight - borderSize, // Top point
			centerX - indicatorSize - borderSize, centerY + borderSize, // Bottom left
			centerX + indicatorSize + borderSize, centerY + borderSize, // Bottom right
			0xFF000000); // Black border
		
		// Draw red filled triangle
		drawTriangle(graphics, 
			centerX, centerY - triangleHeight, // Top point
			centerX - indicatorSize, centerY, // Bottom left
			centerX + indicatorSize, centerY, // Bottom right
			0xFFFF0000); // Red color

		// Draw direction indicator (north arrow)
		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY - radius + 10, 0);
		// Direction indicator should always point north, so rotate opposite to map rotation
		float arrowRotation = (float)Math.toRadians(-minimapCache.smoothYaw);
		Quaternionf arrowQuaternion = new Quaternionf().rotateZ(arrowRotation);
		graphics.getMatrices().multiply(arrowQuaternion);
		// Draw north arrow
		graphics.fill(-2, -8, 2, 0, 0xFFFFFF);
		graphics.fill(-3, 0, 3, 2, 0xFFFFFF);
		graphics.getMatrices().pop();

		// Draw other players in boats if enabled
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayers(graphics, centerX, centerY, playerPos, scale, minimapCache.smoothYaw);
		}

		// Draw circular minimap border - black outer stroke
		int borderThickness = 1;
		drawCircle(graphics, centerX, centerY, radius + borderThickness, 0xFF000000);
	}
	
	/** Pre-renders the minimap to an integer array with performance optimizations */
	private void preRenderMinimap(Vec3d playerPos) {
		if(this.client.world == null) return;
		
		// Release old pre-rendered array to free memory before creating new one
		if(preRenderedMinimap != null) {
			// Set to null to allow garbage collection
			preRenderedMinimap = null;
		}
		
		// Create new pre-rendered array with current size
		preRenderedMinimap = new int[Config.minimapSize * Config.minimapSize];
		int centerX = Config.minimapSize / 2;
		int centerZ = Config.minimapSize / 2;
		int playerY = (int)playerPos.y;
		
		// Render ice blocks to array with performance optimizations
		// Ensure we render the entire rectangular area without circular mask
		for(int x = 0; x < Config.minimapSize; x++) {
			for(int z = 0; z < Config.minimapSize; z++) {
				// Calculate world coordinates with zoom
				// More intuitive zoom calculation: zoom = 1.0 shows normal area, higher values show larger area
				// Scale the offset by zoom factor to control the area shown
				double offsetX = (x - centerX) * Config.minimapZoom;
				double offsetZ = (z - centerZ) * Config.minimapZoom;
				// Use precise floating-point player position for smooth movement
				int worldX = (int)(playerPos.x + offsetX);
				int worldZ = (int)(playerPos.z + offsetZ);
				
				int bestColor = 0;
				boolean foundIce = false;
				
				// Check current Y level first (most common case)
				BlockPos currentPos = new BlockPos(worldX, playerY + Config.minimapYOffset, worldZ);
				BlockState currentState = this.client.world.getBlockState(currentPos);
				Block currentBlock = currentState.getBlock();
				
				// Check if it's an ice block and has transparent block above
				if(isIceBlock(currentBlock) && hasTransparentAbove(currentPos.up())) {
					bestColor = calculateIceColor(0, playerY);
					foundIce = true;
				}
				
				// If not found, check below player level
				if(!foundIce) {
					for(int yOffset = -1; yOffset >= -Config.minimapIceDetectionRange; yOffset--) {
						BlockPos blockPos = new BlockPos(worldX, playerY + Config.minimapYOffset + yOffset, worldZ);
						BlockState blockState = this.client.world.getBlockState(blockPos);
						Block block = blockState.getBlock();
						
						if(isIceBlock(block) && hasTransparentAbove(blockPos.up())) {
							bestColor = calculateIceColor(yOffset, playerY);
							foundIce = true;
							break; // Found ice, no need to check further
						}
					}
				}
				
				// If still not found, check above player level if enabled
				if(!foundIce && Config.minimapShowAllHeights) {
					for(int yOffset = 1; yOffset <= Config.minimapIceDetectionRange; yOffset++) {
						BlockPos blockPos = new BlockPos(worldX, playerY + Config.minimapYOffset + yOffset, worldZ);
						BlockState blockState = this.client.world.getBlockState(blockPos);
						Block block = blockState.getBlock();
						
						if(isIceBlock(block) && hasTransparentAbove(blockPos.up())) {
							bestColor = calculateIceColor(yOffset, playerY);
							foundIce = true;
							break; // Found ice, no need to check further
						}
					}
				}
				
				// Store the color for this position (will be transparent if no ice found)
				preRenderedMinimap[x + z * Config.minimapSize] = bestColor;
			}
		}
		
		// Clear the ice cache after each render to free memory
		minimapCache.iceCache.clear();
	}
	
	/** Check if the block is an ice block we want to render */
	private boolean isIceBlock(Block block) {
		return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE;
	}
	
	/** Check if the block above is transparent */
	private boolean hasTransparentAbove(BlockPos pos) {
		if(this.client.world == null) return false;
		BlockState state = this.client.world.getBlockState(pos);
		// In newer Minecraft versions, use isReplaceable instead of getMaterial
		return state.isAir() || state.isReplaceable();
	}
	
	/** Calculate the color of ice based on its Y offset from player */
	private int calculateIceColor(int yOffset, int playerY) {
		// Check if flat ice mode is enabled
		if(Config.minimapFlatIce) {
			// Show all ice with the same brightness, ignoring height differences
			return 0xFFCCCCCC; // Solid white color for flat ice mode
		}
		
		// Calculate Y difference from player's current height (signed, for height-based brightness)
		int yDiff = yOffset;
		
		// Calculate alpha based on vertical distance
		float alphaFactor;
		if(yDiff <= 0) {
			// Ice below or at player level: more visible
			alphaFactor = 1.0f - (Math.abs(yDiff) / 15.0f);
		} else {
			// Ice above player level: less visible
			alphaFactor = 0.6f - (yDiff / 25.0f);
		}
		alphaFactor = MathHelper.clamp(alphaFactor, 0.2f, 1.0f);
		
		// Calculate brightness based on height difference (lower = darker)
		float brightness;
		if(yDiff <= 0) {
			// Ice below or at player level: brighter at player level, darker as it gets lower
			brightness = 1.0f - (Math.abs(yDiff) / 20.0f);
		} else {
			// Ice above player level: less bright
			brightness = 0.7f - (yDiff / 30.0f);
		}
		brightness = MathHelper.clamp(brightness, 0.3f, 1.0f);
		
		// Combine brightness with alpha factor
		int alpha = (int)(255 * alphaFactor);
		int gray = (int)(brightness * 255);
		
		// Calculate final color
		return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
	}
	
	/** Draw other players in boats on the minimap as blue small squares */
	private void drawOtherPlayers(DrawContext graphics, int centerX, int centerY, Vec3d playerPos, double scale, float currentYaw) {
		if(this.client.world == null) return;
		
		// Get all players in the world (limit to improve performance)
		int maxPlayersToRender = 16;
		int playersRendered = 0;
		
		for(net.minecraft.entity.player.PlayerEntity otherPlayer : this.client.world.getPlayers()) {
			// Limit the number of players rendered to improve performance
			if(playersRendered >= maxPlayersToRender) break;
			
			// Skip the local player (or the player we're spectating)
			if(otherPlayer == this.client.player || otherPlayer == this.client.getCameraEntity()) continue;
			
			// Check if player is in a boat
			if(otherPlayer.hasVehicle() && otherPlayer.getVehicle() instanceof net.minecraft.entity.vehicle.AbstractBoatEntity) {
				// Calculate player's position relative to the local player
				Vec3d otherPos = otherPlayer.getPos();
				
				// Calculate relative coordinates with precise floating-point values
				double relX = otherPos.x - playerPos.x;
				double relZ = otherPos.z - playerPos.z;
				
				// Apply rotation to match map rotation (invert rotation to match map direction)
				float rotation = (float)Math.toRadians(-currentYaw);
				float sin = MathHelper.sin(rotation);
				float cos = MathHelper.cos(rotation);
				
				// Rotate the relative coordinates
				float rotatedX = (float)relX * cos - (float)relZ * sin;
				float rotatedZ = (float)relX * sin + (float)relZ * cos;
				
				// Calculate screen coordinates with scale and zoom
				// Apply zoom factor to relative coordinates to match minimap zoom level
				int screenX = centerX + (int)(rotatedX * scale / Config.minimapZoom);
				int screenY = centerY + (int)(rotatedZ * scale / Config.minimapZoom);
				
				// Draw blue small square for other player with customizable size
				int indicatorSize = (int)(Config.minimapOtherPlayersIndicatorSize * scale); // Use customizable size
				int borderSize = 1;
				
				// Draw black border square (slightly larger)
				graphics.fill(screenX - indicatorSize - borderSize, screenY - indicatorSize - borderSize, 
					screenX + indicatorSize + borderSize + 1, screenY + indicatorSize + borderSize + 1, 0xFF000000);
				
				// Draw blue filled square
				graphics.fill(screenX - indicatorSize, screenY - indicatorSize, 
					screenX + indicatorSize + 1, screenY + indicatorSize + 1, 0xFF0000FF);
				
				playersRendered++;
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
	
	/** Custom triangle drawing method since fillTriangle is not available in DrawContext */
	private void drawTriangle(DrawContext graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
		// Sort vertices by y-coordinate
		if (y1 > y2) { int temp = y1; y1 = y2; y2 = temp; temp = x1; x1 = x2; x2 = temp; }
		if (y1 > y3) { int temp = y1; y1 = y3; y3 = temp; temp = x1; x1 = x3; x3 = temp; }
		if (y2 > y3) { int temp = y2; y2 = y3; y3 = temp; temp = x2; x2 = x3; x3 = temp; }
		
		// Calculate slopes
		float slope1 = (y2 - y1) == 0 ? 0 : (float)(x2 - x1) / (y2 - y1);
		float slope2 = (y3 - y1) == 0 ? 0 : (float)(x3 - x1) / (y3 - y1);
		float slope3 = (y3 - y2) == 0 ? 0 : (float)(x3 - x2) / (y3 - y2);
		
		// Draw top half
		float xLeft = x1;
		float xRight = x1;
		for (int y = y1; y <= y2; y++) {
			int left = Math.round(xLeft);
			int right = Math.round(xRight);
			if (left <= right) {
				graphics.fill(left, y, right + 1, y + 1, color);
			}
			xLeft += slope1;
			xRight += slope2;
		}
		
		// Draw bottom half
		xLeft = x2;
		for (int y = y2; y <= y3; y++) {
			int left = Math.round(xLeft);
			int right = Math.round(xRight);
			if (left <= right) {
				graphics.fill(left, y, right + 1, y + 1, color);
			}
			xLeft += slope3;
			xRight += slope2;
		}
	}

	/** Custom circle drawing methods since fillEllipse and drawEllipse are not available in DrawContext */
	
	/** Draw a filled circle */
	private void drawCircleFill(DrawContext graphics, int centerX, int centerY, int radius, int color) {
		// Simple implementation using rectangle fills
		// This will draw a solid circle without horizontal lines
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				if (x * x + y * y <= radius * radius) {
					graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
				}
			}
		}
	}

	/** Draw a circle outline */
	private void drawCircle(DrawContext graphics, int centerX, int centerY, int radius, int color) {
		int x = radius;
		int y = 0;
		int radiusError = 1 - x;

		while (x >= y) {
			// Draw pixels in all octants
			graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
			graphics.fill(centerX + y, centerY + x, centerX + y + 1, centerY + x + 1, color);
			graphics.fill(centerX - x, centerY + y, centerX - x + 1, centerY + y + 1, color);
			graphics.fill(centerX - y, centerY + x, centerX - y + 1, centerY + x + 1, color);
			graphics.fill(centerX - x, centerY - y, centerX - x + 1, centerY - y + 1, color);
			graphics.fill(centerX - y, centerY - x, centerX - y + 1, centerY - x + 1, color);
			graphics.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
			graphics.fill(centerX + y, centerY - x, centerX + y + 1, centerY - x + 1, color);

			y++;
			if (radiusError < 0) {
				radiusError += 2 * y + 1;
			} else {
				x--;
				radiusError += 2 * (y - x + 1);
			}
		}
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
