package hibi.boathud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class HudRenderer {

	private final Minecraft client;
	private int scaledWidth;
	private int scaledHeight;

	// Cache for minimap to avoid recalculating every frame
	private float smoothYaw = 0f; // Simplified cache - just keep smooth rotation value
	
	// Pre-rendered minimap texture cache with size tracking to avoid unnecessary reallocations
	private int[] preRenderedMinimap;
	private int lastMinimapSize = -1;
	
	// Cache for minimap rendering optimization
	private Vec3 lastPlayerPos = Vec3.ZERO;
	private long lastRenderTime = 0;

	// The index to be used in these scales is the bar type (stored internally as an integer, defined in Config)
	//                                        Pack     Mixed      Blue
	private static final double[] MIN_V =   {   0d,       8d,      40d}; // Minimum display speed (m/s)
	private static final double[] MAX_V =   {  40d,      70d,      70d}; // Maximum display speed (m/s)
	private static final double[] SCALE_V = {4.55d, 182d/62d, 182d/30d}; // Pixels for 1 unit of speed (px*s/m) (BarWidth / (VMax - VMin))

	// Used for lerping
	private double displayedSpeed = 0.0d;

	public HudRenderer(Minecraft client) {
		this.client = client;
	}

	public void render(GuiGraphics graphics, DeltaTracker counter) {
		this.scaledWidth = graphics.guiWidth();
		this.scaledHeight = graphics.guiHeight();
		int i = this.scaledWidth / 2;
		int nameLen = this.client.font.width(Common.hudData.name);

		// Lerping the displayed speed with the actual speed against how far we are into the tick not only is mostly accurate,
		// but gives the impression that it's being updated faster than 20 hz (which it isn't)
		this.displayedSpeed = Mth.lerp(counter.getGameTimeDeltaPartialTick(true), this.displayedSpeed, Common.hudData.speed);

		if(Config.extended) {
			// Overlay texture and bar
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_EXTENDED, i - 91, this.scaledHeight - 83, 182, 33);
			this.renderBar(graphics, i - 91, this.scaledHeight - 83);

			// Sprites
			if(Common.hudData.isDriver) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.client.options.keyLeft.isDown()? LEFT_LIT : LEFT_UNLIT, i - 86, this.scaledHeight - 65, 17, 8);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.client.options.keyRight.isDown()? RIGHT_LIT : RIGHT_UNLIT, i - 63, this.scaledHeight - 65, 17, 8);
				// Brake-throttle bar
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.client.options.keyUp.isDown()? FORWARD_LIT : FORWARD_UNLIT, i, this.scaledHeight - 55, 61, 5);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.client.options.keyDown.isDown()? BACKWARD_LIT : BACKWARD_UNLIT, i - 61, this.scaledHeight - 55, 61, 5);
			}

			// Ping
			this.renderPing(graphics, i + 75 - nameLen, this.scaledHeight - 65);
			
			// Text
			// First Row
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i, this.scaledHeight - 76, 0xFFFFFFFF);
			this.typeCentered(graphics, String.format(Config.gFormat, Common.hudData.g), i + 58, this.scaledHeight - 76, 0xFFFFFFFF);
			// Second Row
			graphics.drawString(this.client.font, Common.hudData.name, i + 88 - nameLen, this.scaledHeight - 65, 0xFFFFFFFF);

		} else { // Compact mode
		// Overlay texture and bar
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_COMPACT, i - 91, this.scaledHeight - 83, 182, 20);
		this.renderBar(graphics, i - 91, this.scaledHeight - 83);
		// Speed and drift angle
		this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFFFF);
		this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i + 58, this.scaledHeight - 76, 0xFFFFFFFF);
	}

		// Render minimap if enabled (it will be rendered separately by the mixin if not riding boat)
		if(Config.minimapEnabled) {
			this.renderMinimap(graphics);
		}
	}

	/** Renders the minimap showing ice blocks with optimized caching and proper rotation */
	public void renderMinimap(GuiGraphics graphics) {
		if(this.client.level == null) return;

		// Get camera entity (local player or spectated player)
		Entity cameraEntity = this.client.getCameraEntity();
		if(cameraEntity == null) return;

		// Get player position and rotation
		Vec3 playerPos = cameraEntity.position();
		float playerYaw = cameraEntity.getYRot();

		// Only pre-render the map if player has moved significantly or time has passed
		long currentTime = System.currentTimeMillis();
		if(playerPos.distanceToSqr(lastPlayerPos) > 0.5 || currentTime - lastRenderTime > 50) {
			preRenderMinimap(playerPos);
			lastPlayerPos = playerPos;
			lastRenderTime = currentTime;
		}

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
		graphics.pose().pushMatrix();
		graphics.pose().translate((float)centerX, (float)centerY);

		// Apply rotation only if not locked to north
		if(!Config.minimapLockNorth) {
			// Calculate target rotation angle to match player's forward direction
			// When player looks forward, map should show forward direction correctly
			// Fixing direction by adjusting rotation calculation
			float targetYaw = playerYaw + 180.0f; // Add 180 degrees to get correct forward direction
			float smoothFactor = 0.1f; // Adjust for smoother rotation
			
			// Calculate shortest rotation path
			float yawDiff = targetYaw - smoothYaw;
			if(yawDiff > 180.0f) {
				yawDiff -= 360.0f;
			} else if(yawDiff < -180.0f) {
				yawDiff += 360.0f;
			}
			
			// Apply lerp to get smooth yaw
			smoothYaw += yawDiff * smoothFactor;
			
			// Rotate map to match player's view direction with smooth rotation
			// Minecraft yaw increases clockwise, OpenGL rotation is counter-clockwise, so invert
			float rotation = (float)Math.toRadians(-smoothYaw);
			// Use rotate for 2D rotation in Matrix3x2fStack
			graphics.pose().rotate(rotation);
		} else {
			// Locked to north, so no rotation needed
			smoothYaw = 0.0f; // Reset smooth yaw to north
		}

		// Draw ice blocks with rotation applied, only within circular area
		if(preRenderedMinimap != null) {
			// Get player's fractional position for smooth scrolling
			final double playerXFrac = playerPos.x() - Math.floor(playerPos.x());
			final double playerZFrac = playerPos.z() - Math.floor(playerPos.z());
			
			// Pre-calculate values for performance
			final int mapSize = Config.minimapSize;
			final double halfSize = mapSize / 2.0;
			final double radiusSq = radius * radius;
			final int[] minimapData = preRenderedMinimap;
			
			// Combine edge detection and rendering into a single pass for better performance
			for(int x = 0; x < mapSize; x++) {
				// Calculate x position with sub-pixel precision using player's fractional offset
				// Apply player's X fractional offset for smooth scrolling
				final double pixelX = (x - halfSize + 0.5 - playerXFrac) * scale;
				final double pixelXSq = pixelX * pixelX;
				
				for(int z = 0; z < mapSize; z++) {
					// Calculate array index
					final int index = x + z * mapSize;
					final int color = minimapData[index];
					
					if((color >>> 24) == 0) continue; // Skip transparent pixels
					
					// Calculate z position with sub-pixel precision using player's fractional offset
					final double pixelZ = (z - halfSize + 0.5 - playerZFrac) * scale;
					
					// Check if pixel is within circle radius
					if(pixelXSq + pixelZ * pixelZ > radiusSq) continue;
					
					// Optimized edge detection - only check 4 cardinal directions
					boolean isEdge = false;
					
					// Check left neighbor
					if(x == 0 || (minimapData[(x - 1) + z * mapSize] >>> 24) == 0) {
						isEdge = true;
					} 
					// Check right neighbor
					else if(x == mapSize - 1 || (minimapData[(x + 1) + z * mapSize] >>> 24) == 0) {
						isEdge = true;
					} 
					// Check top neighbor
					else if(z == 0 || (minimapData[x + (z - 1) * mapSize] >>> 24) == 0) {
						isEdge = true;
					} 
					// Check bottom neighbor
					else if(z == mapSize - 1 || (minimapData[x + (z + 1) * mapSize] >>> 24) == 0) {
						isEdge = true;
					}
					
					// Convert to integer coordinates for drawing, maintaining sub-pixel smoothness
					final int drawX = (int)pixelX;
					final int drawZ = (int)pixelZ;
					
					// Draw edge pixel with black border first, then regular pixel on top
					if(isEdge) {
						// Draw black border pixel
						graphics.fill(drawX, drawZ, drawX + 1, drawZ + 1, 0xFF000000);
					}
					// Draw the regular pixel
					graphics.fill(drawX, drawZ, drawX + 1, drawZ + 1, color);
				}
			}
		}

		// Pop the matrix stack to reset rotation for player indicator
		graphics.pose().popMatrix();

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
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY - radius + 10);
		// Direction indicator should always point north, so rotate opposite to map rotation
		float arrowRotation = (float)Math.toRadians(-smoothYaw);
		graphics.pose().rotate(arrowRotation);
		// Draw north arrow
		graphics.fill(-2, -8, 2, 0, 0xFFFFFF);
		graphics.fill(-3, 0, 3, 2, 0xFFFFFF);
		graphics.pose().popMatrix();

		// Draw other players in boats if enabled
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayers(graphics, centerX, centerY, playerPos, scale, smoothYaw);
		}

		// Draw circular minimap border - black outer stroke
		int borderThickness = 1;
		drawCircle(graphics, centerX, centerY, radius + borderThickness, 0xFF000000);
	}
	
	/** Pre-renders the minimap to an integer array with performance optimizations */
	private void preRenderMinimap(Vec3 playerPos) {
		if(this.client.level == null) return;
		
		int currentSize = Config.minimapSize;
		int arraySize = currentSize * currentSize;
		
		// Only reallocate the array if the minimap size has changed
		if(preRenderedMinimap == null || lastMinimapSize != currentSize) {
			// Reallocate array with new size
			preRenderedMinimap = new int[arraySize];
			lastMinimapSize = currentSize;
		}
		
		// Early return if minimap is disabled or size is 0
		if(currentSize <= 0) return;
		
		// Get player position with fractional precision
		final double playerX = playerPos.x();
		final double playerZ = playerPos.z();
		final int playerY = (int)playerPos.y();
		
		// Calculate player position fractional offset for smooth minimap scrolling
		final double playerXFrac = playerX - Math.floor(playerX);
		final double playerZFrac = playerZ - Math.floor(playerZ);
		
		// Pre-calculate all values that don't change in loops
		final int centerX = currentSize / 2;
		final int centerZ = currentSize / 2;
		final double zoom = Config.minimapZoom;
		final int yOffset = Config.minimapYOffset;
		final int minYOffset = -Config.minimapIceDetectionRange;
		final int maxYOffset = Config.minimapShowAllHeights ? Config.minimapIceDetectionRange : 0;
		final int[] minimapData = preRenderedMinimap;
		final var level = this.client.level;
		
		// Reuse BlockPos objects to reduce garbage collection
		final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		final BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();
		
		// Loop through all pixels in the minimap
		for(int x = 0; x < currentSize; x++) {
			// Calculate world coordinates with sub-block precision
			// Apply player's fractional offset to get smooth scrolling
			final double offsetX = (x - centerX) * zoom;
			final double worldX = playerX + offsetX;
			
			for(int z = 0; z < currentSize; z++) {
				// Calculate world coordinates with sub-block precision
				final double offsetZ = (z - centerZ) * zoom;
				final double worldZ = playerZ + offsetZ;
				final int arrayIndex = x + z * currentSize;
				
				int bestColor = 0;
				boolean foundIce = false;
				
				// Set block position with integer coordinates (required for getBlockState)
				blockPos.set((int)worldX, playerY + yOffset, (int)worldZ);
				
				// Check block state
				final BlockState currentState = level.getBlockState(blockPos);
				final Block block = currentState.getBlock();
				
				if(isIceBlock(block)) {
					// Check if above is air - only do this if we found ice
					abovePos.set(blockPos).move(0, 1, 0);
					final BlockState aboveState = level.getBlockState(abovePos);
					if(aboveState.isAir() || aboveState.canBeReplaced()) {
						bestColor = calculateIceColor(0, playerY);
						foundIce = true;
					}
				}
				
				// If not found, check other Y levels with early exit
				if(!foundIce) {
					// Check below player level first
					for(int yOff = -1; yOff >= minYOffset && !foundIce; yOff--) {
						blockPos.set((int)worldX, playerY + yOffset + yOff, (int)worldZ);
						final BlockState state = level.getBlockState(blockPos);
						if(isIceBlock(state.getBlock())) {
							abovePos.set(blockPos).move(0, 1, 0);
							final BlockState aboveState = level.getBlockState(abovePos);
							if(aboveState.isAir() || aboveState.canBeReplaced()) {
								bestColor = calculateIceColor(yOff, playerY);
								foundIce = true;
							}
						}
					}
					
					// If still not found, check above player level if enabled
					if(!foundIce && maxYOffset > 0) {
						for(int yOff = 1; yOff <= maxYOffset && !foundIce; yOff++) {
							blockPos.set((int)worldX, playerY + yOffset + yOff, (int)worldZ);
							final BlockState state = level.getBlockState(blockPos);
							if(isIceBlock(state.getBlock())) {
								abovePos.set(blockPos).move(0, 1, 0);
								final BlockState aboveState = level.getBlockState(abovePos);
								if(aboveState.isAir() || aboveState.canBeReplaced()) {
									bestColor = calculateIceColor(yOff, playerY);
									foundIce = true;
								}
							}
						}
					}
				}
				
				// Store the color for this position
				minimapData[arrayIndex] = bestColor;
			}
		}
	}
	
	/** Check if the block is an ice block we want to render */
	private boolean isIceBlock(Block block) {
		return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE;
	}
	
	/** Check if the block above is transparent */
	private boolean hasTransparentAbove(BlockPos pos) {
		if(this.client.level == null) return false;
		BlockState state = this.client.level.getBlockState(pos);
		// In newer Minecraft versions, use isAir or canBeReplaced instead of isReplaceable
		return state.isAir() || state.canBeReplaced();
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
		alphaFactor = Mth.clamp(alphaFactor, 0.2f, 1.0f);
		
		// Calculate brightness based on height difference (lower = darker)
		float brightness;
		if(yDiff <= 0) {
			// Ice below or at player level: brighter at player level, darker as it gets lower
			brightness = 1.0f - (Math.abs(yDiff) / 20.0f);
		} else {
			// Ice above player level: less bright
			brightness = 0.7f - (yDiff / 30.0f);
		}
		brightness = Mth.clamp(brightness, 0.3f, 1.0f);
		
		// Combine brightness with alpha factor
		int alpha = (int)(255 * alphaFactor);
		int gray = (int)(brightness * 255);
		
		// Calculate final color
		return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
	}
	
	/** Draw other players in boats on the minimap as blue small squares */
	private void drawOtherPlayers(GuiGraphics graphics, int centerX, int centerY, Vec3 playerPos, double scale, float currentYaw) {
		if(this.client.level == null) return;
		
		// Get all players in the world (limit to improve performance)
		int maxPlayersToRender = 16;
		int playersRendered = 0;
		
		for(Player otherPlayer : this.client.level.players()) {
			// Limit the number of players rendered to improve performance
			if(playersRendered >= maxPlayersToRender) break;
			
			// Skip the local player (or the player we're spectating)
			if(otherPlayer == this.client.player || otherPlayer == this.client.getCameraEntity()) continue;
			
			// Check if player is in a boat
				if(otherPlayer.getVehicle() != null && otherPlayer.getVehicle() instanceof AbstractBoat) {
				// Calculate player's position relative to the local player
				Vec3 otherPos = otherPlayer.position();
				
				// Calculate relative coordinates with precise floating-point values
				double relX = otherPos.x() - playerPos.x();
				double relZ = otherPos.z() - playerPos.z();
				
				// Apply rotation to match map rotation (invert rotation to match map direction)
				float rotation = (float)Math.toRadians(-currentYaw);
				float sin = Mth.sin(rotation);
				float cos = Mth.cos(rotation);
				
				// Rotate the relative coordinates
				float rotatedX = (float)relX * cos - (float)relZ * sin;
				float rotatedZ = (float)relX * sin + (float)relZ * cos;
				
				// Calculate screen coordinates with scale and zoom - use float for precise positioning
					// Apply zoom factor to relative coordinates to match minimap zoom level
					float screenX = centerX + (float)(rotatedX * scale / Config.minimapZoom);
					float screenY = centerY + (float)(rotatedZ * scale / Config.minimapZoom);
				
				// Draw blue small square for other player with customizable size
					int indicatorSize = (int)(Config.minimapOtherPlayersIndicatorSize * scale); // Use customizable size
					int borderSize = 1;
					
					// Round coordinates to nearest pixel for smooth movement
					int roundedX = Math.round(screenX);
					int roundedY = Math.round(screenY);
					
					// Draw black border square (slightly larger)
					graphics.fill(roundedX - indicatorSize - borderSize, roundedY - indicatorSize - borderSize, 
						roundedX + indicatorSize + borderSize + 1, roundedY + indicatorSize + borderSize + 1, 0xFF000000);
					
					// Draw blue filled square
					graphics.fill(roundedX - indicatorSize, roundedY - indicatorSize, 
						roundedX + indicatorSize + 1, roundedY + indicatorSize + 1, 0xFF0000FF);
				
				playersRendered++;
			}
		}
	}

	/** Custom triangle drawing method since fillTriangle is not available in GuiGraphics */
	private void drawTriangle(GuiGraphics graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
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

	/** Custom circle drawing methods since fillEllipse and drawEllipse are not available in GuiGraphics */
	
	/** Draw a filled circle with optimized Bresenham's algorithm */
	private void drawCircleFill(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		// Use a simple and efficient method for filled circles in 2D GUI
		// This avoids the horizontal line artifacts by using a single fillRect call per scanline
		for (int y = -radius; y <= radius; y++) {
			int xRange = (int) Math.sqrt(radius * radius - y * y);
			graphics.fill(centerX - xRange, centerY + y, centerX + xRange + 1, centerY + y + 1, color);
		}
	}

	/** Draw a circle outline with optimized Bresenham's algorithm */
	private void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
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

	/** Renders the speed bar atop the HUD, uses displayedSpeed to, well, diisplay the speed. */
	private void renderBar(GuiGraphics graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_OFF[Config.barType], x, y, 182, 5);
		if(Common.hudData.speed < MIN_V[Config.barType]) return;
		if(Common.hudData.speed > MAX_V[Config.barType]) {
			if(this.client.level.getGameTime() % 2 == 0) return;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_ON[Config.barType], x, y, 182, 5);
			return;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_ON[Config.barType], 182, 5, 0, 0, x, y, (int)((this.displayedSpeed - MIN_V[Config.barType]) * SCALE_V[Config.barType]), 5);
	}

	/** Implementation is cloned from the notchian ping display in the tab player list.	 */
	private void renderPing(GuiGraphics graphics, int x, int y) {
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bar, x, y, 10, 8);
	}

	/** Renders a piece of text centered horizontally on an X coordinate. */
	private void typeCentered(GuiGraphics graphics, String text, int centerX, int y, int color) {
		graphics.drawString(this.client.font, text, centerX - this.client.font.width(text) / 2, y, color);
	}

	private static final Identifier
		BACKGROUND_EXTENDED = Identifier.fromNamespaceAndPath("boathud", "background_extended"),
		BACKGROUND_COMPACT = Identifier.fromNamespaceAndPath("boathud", "background_compact"),
		LEFT_UNLIT = Identifier.fromNamespaceAndPath("boathud", "left_unlit"),
		LEFT_LIT = Identifier.fromNamespaceAndPath("boathud", "left_lit"),
		RIGHT_UNLIT = Identifier.fromNamespaceAndPath("boathud", "right_unlit"),
		RIGHT_LIT = Identifier.fromNamespaceAndPath("boathud", "right_lit"),
		FORWARD_UNLIT = Identifier.fromNamespaceAndPath("boathud", "forward_unlit"),
		FORWARD_LIT = Identifier.fromNamespaceAndPath("boathud", "forward_lit"),
		BACKWARD_UNLIT = Identifier.fromNamespaceAndPath("boathud", "backward_unlit"),
		BACKWARD_LIT = Identifier.fromNamespaceAndPath("boathud", "backward_lit"),
		BAR_1_UNLIT = Identifier.fromNamespaceAndPath("boathud", "bar_1_unlit"),
		BAR_1_LIT = Identifier.fromNamespaceAndPath("boathud", "bar_1_lit"),
		BAR_2_UNLIT = Identifier.fromNamespaceAndPath("boathud", "bar_2_unlit"),
		BAR_2_LIT = Identifier.fromNamespaceAndPath("boathud", "bar_2_lit"),
		BAR_3_UNLIT = Identifier.fromNamespaceAndPath("boathud", "bar_3_unlit"),
		BAR_3_LIT = Identifier.fromNamespaceAndPath("boathud", "bar_3_lit"),
		PING_5 = Identifier.fromNamespaceAndPath("boathud", "ping_5"),
		PING_4 = Identifier.fromNamespaceAndPath("boathud", "ping_4"),
		PING_3 = Identifier.fromNamespaceAndPath("boathud", "ping_3"),
		PING_2 = Identifier.fromNamespaceAndPath("boathud", "ping_2"),
		PING_1 = Identifier.fromNamespaceAndPath("boathud", "ping_1"),
		PING_UNKNOWN = Identifier.fromNamespaceAndPath("boathud", "ping_unknown")
	;
	private static final Identifier[] BAR_OFF = {BAR_1_UNLIT, BAR_2_UNLIT, BAR_3_UNLIT};
	private static final Identifier[] BAR_ON = {BAR_1_LIT, BAR_2_LIT, BAR_3_LIT};
}