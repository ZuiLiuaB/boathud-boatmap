package hibi.boathud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
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
		float smoothYaw = 0f; // Only keep smooth rotation value, no other unnecessary state
		// Pre-rendered minimap held as a GPU texture so it can be drawn in a single call
		NativeImageBackedTexture minimapTexture = null;
		Identifier minimapTextureId = null;
		int minimapTextureSize = 0;
		long lastPreRenderTime = -1; // Game time of the last world scan, used to throttle recompute
		// Pseudo-3D extra throttling: only re-scan when the player has moved/turned enough.
		// NaN initial values force the very first frame to scan.
		double lastScanX = Double.NaN;
		double lastScanZ = Double.NaN;
		double lastScanYaw = Double.NaN;
	}
	private MinimapCache minimapCache = new MinimapCache();
	
	// Pre-rendered minimap texture cache
	// Only keep one pre-rendered array at a time to minimize memory usage
	private int[] preRenderedMinimap;

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
		
		// Get HUD data from Common instance
		HudData hudData = Common.getInstance().getHudData();
		if(hudData == null) return;
		
		int nameLen = this.client.textRenderer.getWidth(hudData.name);
		
		// Lerping the displayed speed with the actual speed against how far we are into the tick not only is mostly accurate,
		// but gives the impression that it's being updated faster than 20 hz (which it isn't)
		this.displayedSpeed = MathHelper.lerp(counter.getTickDelta(false), this.displayedSpeed, hudData.speed);

		if(Config.extended) {
				// Overlay texture and bar
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, BACKGROUND_EXTENDED, i - 91, this.scaledHeight - 83, 182, 33);
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);

				// Sprites
				if(Common.getInstance().getHudData() != null && Common.getInstance().getHudData().isDriver) {
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
				if(Common.getInstance().getHudData() != null) {
					this.typeCentered(graphics, String.format(Config.angleFormat, Common.getInstance().getHudData().driftAngle), i, this.scaledHeight - 76, 0xFFFFFF);
					this.typeCentered(graphics, String.format(Config.gFormat, Common.getInstance().getHudData().g), i + 58, this.scaledHeight - 76, 0xFFFFFF);
					// Second Row
					graphics.drawTextWithShadow(this.client.textRenderer, Common.getInstance().getHudData().name, i + 88 - nameLen, this.scaledHeight - 65, 0xFFFFFF);
				}
	
		} else { // Compact mode
				// Overlay texture and bar
				graphics.drawGuiTexture(RenderLayer::getGuiTextured, BACKGROUND_COMPACT, i - 91, this.scaledHeight - 83, 182, 20);
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);
				// Speed and drift angle
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, hudData.driftAngle), i + 58, this.scaledHeight - 76, 0xFFFFFF);
		}
	
		// Render minimap if enabled (it will be rendered separately by the mixin if not riding boat)
		if(Config.minimapEnabled) {
			this.renderMinimap(graphics);
		}
	}

	/**
	* Renders the minimap.
	*
	* The scan always produces a circular disc whose radius reaches the corners of the square panel
	* (see getMinimapTextureSize). A disc is rotation invariant, so the square viewport - which is
	* exactly its inscribed square - still has real data behind every pixel at any rotation. That is
	* what kills both classic square-minimap bugs: nothing ever pokes out of the frame, and no corner
	* ever goes blank when the map turns.
	*/
	public void renderMinimap(DrawContext graphics) {
		if(this.client.world == null) return;

		// Get camera entity (local player or spectated player)
		net.minecraft.entity.Entity cameraEntity = this.client.getCameraEntity();
		if(cameraEntity == null) return;

		// Get player position and rotation
		Vec3d playerPos = cameraEntity.getPos();
		float playerYaw = cameraEntity.getYaw();
		boolean pseudo3D = Config.minimapShape == 2;

		double scale = 1.0d; // Fixed scale, no longer configurable
		int viewSize = (int)(Config.minimapSize * scale);
		int half = viewSize / 2;
		int posX = Config.minimapX;
		int posY = Config.minimapY;
		int centerX = posX + half;
		int centerY = posY + half;
		boolean square = Config.minimapShape == 1;

		// Advance the smoothed rotation before anything reads it
		this.updateSmoothYaw(playerYaw);
		preRenderMinimap(playerPos, pseudo3D ? minimapCache.smoothYaw : 0.0f);

		// Pseudo-3D: render the map as a perspective-tilted square inside an upright square frame.
		if(pseudo3D) {
			renderMinimapPseudo3D(graphics, playerPos, scale, viewSize, half, posX, posY, centerX, centerY);
			return;
		}

		// Panel background
		if(square) {
			graphics.fill(posX, posY, posX + viewSize, posY + viewSize, 0x40000000);
		} else {
			drawCircleFill(graphics, centerX, centerY, half, 0x40000000);
		}

		// The map itself, in a single blit of the pre-rendered disc.
		if(minimapCache.minimapTexture != null) {
			int textureSize = minimapCache.minimapTextureSize;
			int texHalf = textureSize / 2;
			// Scissor lives in screen space, so it MUST be enabled before push/translate/rotate,
			// otherwise the clip rectangle gets rotated along with the map and clipping is wrong.
			if(square) graphics.enableScissor(posX, posY, posX + viewSize, posY + viewSize);
			graphics.getMatrices().push();
			graphics.getMatrices().translate(centerX, centerY, 0);
			if(!Config.minimapLockNorth) {
				// Minecraft yaw increases clockwise, OpenGL rotation is counter-clockwise, so invert
				float rotation = (float)Math.toRadians(-minimapCache.smoothYaw);
				graphics.getMatrices().multiply(new Quaternionf().rotateZ(rotation));
			}
			graphics.drawTexture(RenderLayer::getGuiTextured, minimapCache.minimapTextureId,
				-texHalf, -texHalf, 0, 0, textureSize, textureSize, textureSize, textureSize);
			graphics.getMatrices().pop();
			if(square) graphics.disableScissor();
		}

		// Player indicator at the centre - upward pointing triangle with customizable size
		int indicatorSize = (int)(Config.minimapPlayerIndicatorSize * scale);
		int triangleHeight = (int)(indicatorSize * 2.0); // Taller, more pointed triangle

		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY, 0);

		// Only rotate the player indicator when the map itself is locked to north
		if(Config.minimapLockNorth) {
			// Calculate player's actual direction (inverted to fix rotation direction)
			float playerRotation = (float)Math.toRadians(playerYaw + 180);
			graphics.getMatrices().multiply(new Quaternionf().rotateZ(playerRotation));
		}

		// Draw black border triangle (slightly larger)
		int borderSize = 1;
		drawTriangle(graphics,
			0, -triangleHeight - borderSize, // Top point
			-indicatorSize - borderSize, borderSize, // Bottom left
			indicatorSize + borderSize, borderSize, // Bottom right
			0xFF000000); // Black border

		// Draw filled triangle with color based on leather armor
		drawTriangle(graphics,
			0, -triangleHeight, // Top point
			-indicatorSize, 0, // Bottom left
			indicatorSize, 0, // Bottom right
			getLocalPlayerIndicatorColor());

		graphics.getMatrices().pop();

		// Direction indicator (north arrow) - always points north, so rotate opposite to the map
		graphics.getMatrices().push();
		graphics.getMatrices().translate(centerX, centerY - half + 10, 0);
		float arrowRotation = (float)Math.toRadians(-minimapCache.smoothYaw);
		graphics.getMatrices().multiply(new Quaternionf().rotateZ(arrowRotation));
		graphics.fill(-2, -8, 2, 0, 0xFFFFFF);
		graphics.fill(-3, 0, 3, 2, 0xFFFFFF);
		graphics.getMatrices().pop();

		// Draw other players in boats if enabled
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayers(graphics, centerX, centerY, playerPos, scale, minimapCache.smoothYaw);
		}

		// Panel border
		int borderThickness = 1;
		if(square) {
			int x0 = posX - borderThickness;
			int y0 = posY - borderThickness;
			int x1 = posX + viewSize + borderThickness;
			int y1 = posY + viewSize + borderThickness;
			graphics.fill(x0, y0, x1, y0 + borderThickness, 0xFF000000); // top
			graphics.fill(x0, y1 - borderThickness, x1, y1, 0xFF000000); // bottom
			graphics.fill(x0, y0, x0 + borderThickness, y1, 0xFF000000); // left
			graphics.fill(x1 - borderThickness, y0, x1, y1, 0xFF000000); // right
		} else {
			drawCircle(graphics, centerX, centerY, half + borderThickness, 0xFF000000);
		}
	}

	/** Advances the smoothed minimap rotation towards the player facing, always taking the short way round. */
	private void updateSmoothYaw(float playerYaw) {
		if(Config.minimapLockNorth) {
			minimapCache.smoothYaw = 0.0f; // Locked to north, so no rotation
			return;
		}
		float targetYaw = playerYaw + 180.0f; // Add 180 degrees to get correct forward direction
		float yawDiff = targetYaw - minimapCache.smoothYaw;
		while(yawDiff > 180.0f) {
			yawDiff -= 360.0f;
		}
		while(yawDiff < -180.0f) {
			yawDiff += 360.0f;
		}
		minimapCache.smoothYaw += yawDiff * 0.1f; // Smoothing factor, applied per frame
		// Keep normalized to [0, 360) so it never accumulates across full turns.
		// Rotation is periodic, so this does not cause any visual jump.
		minimapCache.smoothYaw = (minimapCache.smoothYaw % 360.0f + 360.0f) % 360.0f;
	}
	
	/**
	* Pre-renders the minimap into an int array and uploads it to the GPU texture.
	*
	* Circle mode scans a circular area. Square and pseudo-3D modes scan a square area whose
	* half-diagonal equals the viewport radius, so rotating the map can never leave the square
	* corners empty. Pseudo-3D additionally bakes the player yaw into the texture so the map can
	* be rendered with a perspective tilt without a second rotation.
	*/
	private void preRenderMinimap(Vec3d playerPos, float playerYaw) {
		if(this.client.world == null) return;

		int textureSize = getMinimapTextureSize();
		boolean pseudo3D = Config.minimapShape == 2;

		int arraySize = textureSize * textureSize;
		long now = this.client.world.getTime();

		// Circle/square only throttle by game tick. Pseudo-3D bakes the player yaw into the texture,
		// so it also throttles by position/yaw changes to avoid a full re-scan every frame.
		if(!pseudo3D) {
			if(now == minimapCache.lastPreRenderTime && preRenderedMinimap != null && preRenderedMinimap.length == arraySize) {
				return;
			}
		} else {
			if(preRenderedMinimap != null && preRenderedMinimap.length == arraySize
					&& now == minimapCache.lastPreRenderTime
					&& Math.abs(playerPos.x - minimapCache.lastScanX) < 0.5d
					&& Math.abs(playerPos.z - minimapCache.lastScanZ) < 0.5d
					&& Math.abs(MathHelper.wrapDegrees(playerYaw - (float)minimapCache.lastScanYaw)) < 0.5f) {
				return;
			}
		}
		minimapCache.lastPreRenderTime = now;
		minimapCache.lastScanX = playerPos.x;
		minimapCache.lastScanZ = playerPos.z;
		minimapCache.lastScanYaw = playerYaw;

		if(preRenderedMinimap == null || preRenderedMinimap.length != arraySize) {
			preRenderedMinimap = new int[arraySize];
		}

		int playerY = (int)playerPos.y;
		double halfTex = textureSize / 2.0d;
		double zoom = Config.minimapZoom;
		// The displayed plane is vertically foreshortened. Scan a wider forward/back range so
		// that the compressed plane still fills the complete square viewport.
		double pseudoVerticalScale = pseudo3D
				? Math.max(0.15d, Math.sin(Math.toRadians(Config.minimapTiltAngle))) : 1.0d;

		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		BlockPos.Mutable abovePos = new BlockPos.Mutable();

		double yawRad = 0.0d, sinYaw = 0.0d, cosYaw = 1.0d;
		if(pseudo3D) {
			yawRad = Math.toRadians(playerYaw);
			sinYaw = Math.sin(yawRad);
			cosYaw = Math.cos(yawRad);
		}

		double r2 = halfTex * halfTex;
		for(int x = 0; x < textureSize; x++) {
			for(int z = 0; z < textureSize; z++) {
				double offsetX = x - halfTex + 0.5d;
				double offsetZ = z - halfTex + 0.5d;

				if(Config.minimapShape == 0 && offsetX * offsetX + offsetZ * offsetZ > r2) {
					preRenderedMinimap[x + z * textureSize] = 0;
					continue;
				}

				int worldX, worldZ;
				if(pseudo3D) {
					// Match the normal map basis: +X is screen-right and +Z is screen-down.
					// Stretch only the sampled forward/back range; the final plane compresses it back.
					double planeZ = offsetZ / pseudoVerticalScale;
					worldX = (int)(playerPos.x + (offsetX * cosYaw - planeZ * sinYaw) * zoom);
					worldZ = (int)(playerPos.z + (offsetX * sinYaw + planeZ * cosYaw) * zoom);
				} else {
					worldX = (int)(playerPos.x + offsetX * zoom);
					worldZ = (int)(playerPos.z + offsetZ * zoom);
				}
				preRenderedMinimap[x + z * textureSize] = scanIceAt(worldX, worldZ, playerY, mutablePos, abovePos);
			}
		}

		ensureMinimapTexture(textureSize);
		NativeImage img = minimapCache.minimapTexture.getImage();
		int viewSize = Config.minimapSize;
		double halfView = viewSize / 2.0d;
		double r2View = halfView * halfView;

		for(int x = 0; x < textureSize; x++) {
			for(int z = 0; z < textureSize; z++) {
				double relX = x - halfTex + 0.5d;
				double relZ = z - halfTex + 0.5d;

				// Only the circle shape masks down to an inscribed circle.
				if(Config.minimapShape == 0 && relX * relX + relZ * relZ > r2View) {
					img.setColorArgb(x, z, 0);
					continue;
				}

				int color = preRenderedMinimap[x + z * textureSize];
				if((color >>> 24) == 0) {
					img.setColorArgb(x, z, 0);
					continue;
				}

				// Edge detection for circle/square only. Perspective strips in pseudo-3D look bad with black edges.
				if(Config.minimapShape != 2) {
					boolean isEdge = false;
					for(int dx = -1; dx <= 1 && !isEdge; dx++) {
						for(int dz = -1; dz <= 1; dz++) {
							if(dx == 0 && dz == 0) continue;
							int nx = x + dx, nz = z + dz;
							if(nx < 0 || nx >= textureSize || nz < 0 || nz >= textureSize) { isEdge = true; break; }
							if((preRenderedMinimap[nx + nz * textureSize] >>> 24) == 0) { isEdge = true; break; }
						}
					}
					img.setColorArgb(x, z, isEdge ? 0xFF000000 : color);
				} else {
					img.setColorArgb(x, z, color);
				}
			}
		}
		minimapCache.minimapTexture.upload();
	}

	/** Scans a single world column for a visible ice block and returns its minimap colour. */
	private int scanIceAt(int worldX, int worldZ, int playerY, BlockPos.Mutable mutablePos, BlockPos.Mutable abovePos) {
		// Check current Y level first (most common case)
		mutablePos.set(worldX, playerY + Config.minimapYOffset, worldZ);
		BlockState currentState = this.client.world.getBlockState(mutablePos);
		Block currentBlock = currentState.getBlock();

		abovePos.set(mutablePos.getX(), mutablePos.getY() + 1, mutablePos.getZ());
		if(isIceBlock(currentBlock) && hasTransparentAbove(abovePos)) {
			return calculateIceColor(0, playerY);
		}

		// If not found, check below player level (limit vertical check range)
		int maxBelow = Math.max(-Config.minimapIceDetectionRange, -10);
		for(int yOffset = -1; yOffset >= maxBelow; yOffset--) {
			mutablePos.set(worldX, playerY + Config.minimapYOffset + yOffset, worldZ);
			BlockState blockState = this.client.world.getBlockState(mutablePos);
			Block block = blockState.getBlock();

			abovePos.set(mutablePos.getX(), mutablePos.getY() + 1, mutablePos.getZ());
			if(isIceBlock(block) && hasTransparentAbove(abovePos)) {
				return calculateIceColor(yOffset, playerY);
			}
		}

		// If still not found, check above player level if enabled
		if(Config.minimapShowAllHeights) {
			int maxAbove = Math.min(Config.minimapIceDetectionRange, 10);
			for(int yOffset = 1; yOffset <= maxAbove; yOffset++) {
				mutablePos.set(worldX, playerY + Config.minimapYOffset + yOffset, worldZ);
				BlockState blockState = this.client.world.getBlockState(mutablePos);
				Block block = blockState.getBlock();

				abovePos.set(mutablePos.getX(), mutablePos.getY() + 1, mutablePos.getZ());
				if(isIceBlock(block) && hasTransparentAbove(abovePos)) {
					return calculateIceColor(yOffset, playerY);
				}
			}
		}
		return 0; // No ice -> transparent
	}

	/** Lazily (re)create the minimap DynamicTexture to match the requested texture size. */
	private void ensureMinimapTexture(int textureSize) {
		if(minimapCache.minimapTexture == null || minimapCache.minimapTextureSize != textureSize) {
			if(minimapCache.minimapTexture != null) {
				this.client.getTextureManager().destroyTexture(minimapCache.minimapTextureId);
				minimapCache.minimapTexture.close();
			}
			NativeImage img = new NativeImage(textureSize, textureSize, false);
			minimapCache.minimapTexture = new NativeImageBackedTexture(img);
			// registerTexture(Identifier, Texture) is stable across versions; registerDynamicTexture's
			// (String, NativeImageBackedTexture) overload does not exist at runtime in 1.21.4 and crashes.
			minimapCache.minimapTextureId = Identifier.of("boathud", "minimap");
			this.client.getTextureManager().registerTexture(minimapCache.minimapTextureId, minimapCache.minimapTexture);
			minimapCache.minimapTextureSize = textureSize;
		}
	}

	/**
	 * Texture size needed for the current shape.
	 *
	 * A square viewport that rotates has to be fed from a circular scan whose radius reaches its
	 * corners, i.e. half the diagonal. Scanning only the inscribed circle is what leaves the four
	 * corners empty once the map turns by 45 degrees. A circle is rotation invariant, so it only
	 * ever needs the viewport size.
	 */
	private int getMinimapTextureSize() {
		int viewSize = Config.minimapSize;
		if(Config.minimapShape != 1) return viewSize;
		int texSize = (int)(viewSize * Math.sqrt(2.0d)) + 2;
		if((texSize & 1) == 1) texSize++; // keep it even so the texture centre lands on a pixel centre
		return Math.max(viewSize, texSize);
	}
	
	/** Check if the block is an ice block we want to render */
	private boolean isIceBlock(Block block) {
		return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE;
	}
	
	/** Determine the indicator color based on player's leather armor color */
	private int getPlayerIndicatorColor(net.minecraft.entity.player.PlayerEntity player) {
		// Default color for other players: blue
		int defaultColor = 0xFF0000FF;
		
		// Check if player is wearing leather armor
		for (net.minecraft.item.ItemStack armorItem : player.getArmorItems()) {
			if (armorItem.getItem() instanceof ArmorItem) {
				// Get color from leather armor using DataComponentTypes
				var dyeColor = armorItem.get(DataComponentTypes.DYED_COLOR);
				if (dyeColor != null) {
					// Get the RGB color value from DyedColorComponent
					int color = dyeColor.rgb();
					
					// Convert RGB color to ARGB with full alpha
					int alpha = 0xFF;
					int red = (color >> 16) & 0xFF;
					int green = (color >> 8) & 0xFF;
					int blue = color & 0xFF;
					
					// Check if color is red or blue
					if (red > 150 && green < 100 && blue < 100) {
						// Red armor: use red color
						return (alpha << 24) | (0xFF << 16) | (0x00 << 8) | 0x00; // Red
					} else if (red < 100 && green < 100 && blue > 150) {
						// Blue armor: use blue color
						return (alpha << 24) | (0x00 << 16) | (0x00 << 8) | 0xFF; // Blue
					}
				}
			}
		}
		
		// Default color if no red/blue leather armor
		return defaultColor;
	}
	
	/** Get the indicator color for the local player */
	private int getLocalPlayerIndicatorColor() {
		if (this.client.player == null) return 0xFFFF0000; // Default red for local player
		
		// Check if local player is wearing leather armor
		for (net.minecraft.item.ItemStack armorItem : this.client.player.getArmorItems()) {
			if (armorItem.getItem() instanceof ArmorItem) {
				// Get color from leather armor using DataComponentTypes
				var dyeColor = armorItem.get(DataComponentTypes.DYED_COLOR);
				if (dyeColor != null) {
					// Get the RGB color value from DyedColorComponent
					int color = dyeColor.rgb();
					
					// Convert RGB color to ARGB with full alpha
					int alpha = 0xFF;
					int red = (color >> 16) & 0xFF;
					int green = (color >> 8) & 0xFF;
					int blue = color & 0xFF;
					
					// Check if color is red or blue
					if (red > 150 && green < 100 && blue < 100) {
						// Red armor: use red color
						return (alpha << 24) | (0xFF << 16) | (0x00 << 8) | 0x00; // Red
					} else if (red < 100 && green < 100 && blue > 150) {
						// Blue armor: use blue color
						return (alpha << 24) | (0x00 << 16) | (0x00 << 8) | 0xFF; // Blue
					}
				}
			}
		}
		
		// Default red color if no red/blue leather armor
		return 0xFFFF0000;
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
		
		// Calculate minimap radius
		int renderedSize = (int)(Config.minimapSize * scale);
		int radius = renderedSize / 2;
		
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
				
				// Limit player indicator within minimap bounds if enabled
				if(Config.minimapLimitPlayersToBounds) {
					double dx = screenX - centerX;
					double dy = screenY - centerY;
					if(Config.minimapShape == 1) {
						// Square panel: clamp each axis independently so the marker slides along the edge
						screenX = centerX + (int)Math.round(MathHelper.clamp(dx, -radius, radius));
						screenY = centerY + (int)Math.round(MathHelper.clamp(dy, -radius, radius));
					} else {
						// Circular panel: project onto the rim
						double distance = Math.sqrt(dx * dx + dy * dy);
						if(distance > radius) {
							double angle = Math.atan2(dy, dx);
							screenX = centerX + (int)(Math.cos(angle) * radius);
							screenY = centerY + (int)(Math.sin(angle) * radius);
						}
					}
				}
				
				// Draw small square for other player with customizable size
			int indicatorSize = (int)(Config.minimapOtherPlayersIndicatorSize * scale); // Use customizable size
			int borderSize = 1;
			
			// Get speed comparison color if enabled
			int borderColor = 0xFF000000; // Default black border
			if(Config.minimapShowSpeedComparison && this.client.player != null) {
				// Get local player speed
				Vec3d localVelocity = this.client.player.getVelocity();
				double localSpeed = Math.sqrt(localVelocity.x * localVelocity.x + localVelocity.z * localVelocity.z);
				
				// Get other player speed
				Vec3d otherVelocity = otherPlayer.getVelocity();
				double otherSpeed = Math.sqrt(otherVelocity.x * otherVelocity.x + otherVelocity.z * otherVelocity.z);
				
				// Compare speeds and set border color
				if(otherSpeed > localSpeed) {
					// Other player is faster: green border
					borderColor = 0xFF00FF00;
				} else if(otherSpeed < localSpeed) {
						// Other player is slower: orange border (more prominent than yellow)
						borderColor = 0xFFFF8000;
					}
				// If speeds are equal, keep black border
			}
			
			// Draw border square (slightly larger)
			graphics.fill(screenX - indicatorSize - borderSize, screenY - indicatorSize - borderSize, 
				screenX + indicatorSize + borderSize + 1, screenY + indicatorSize + borderSize + 1, borderColor);
			
			// Draw filled square with color based on leather armor
			int playerColor = getPlayerIndicatorColor(otherPlayer);
			graphics.fill(screenX - indicatorSize, screenY - indicatorSize, 
				screenX + indicatorSize + 1, screenY + indicatorSize + 1, playerColor);
				
				// Draw player name if enabled
				if(Config.minimapShowOtherPlayersNames) {
					// Get player name
					String playerName = otherPlayer.getName().getString();
					
					// Calculate name position (above the player indicator)
					int nameX = screenX;
					int nameY = screenY - indicatorSize - 2;
					
					// Set name scale
					float nameScale = (float)Config.minimapOtherPlayersNameSize;
					
					// Save current matrix state
					graphics.getMatrices().push();
					
					// Apply scale
					graphics.getMatrices().translate(nameX, nameY, 0);
					graphics.getMatrices().scale(nameScale, nameScale, 1.0f);
					
					// Calculate centered text position
					int textWidth = this.client.textRenderer.getWidth(playerName);
					int centeredX = -textWidth / 2;
					int textY = 0;
					
					// Draw text with shadow for better visibility
					graphics.drawTextWithShadow(this.client.textRenderer, playerName, centeredX, textY, 0xFFFFFF);
					
					// Restore matrix state
					graphics.getMatrices().pop();
				}
				
				playersRendered++;
			}
		}
	}


	/**
	 * Renders the pseudo-3D minimap: the panel frame stays a normal square, but the map layer is
	 * drawn with a perspective tilt (like a 3D navigation app). The player arrow stays fixed at
	 * the centre and always points up. The yaw is baked into the texture, so no second rotation is
	 * needed during rendering.
	 */
	private void renderMinimapPseudo3D(DrawContext graphics, Vec3d playerPos, double scale, int viewSize, int half, int posX, int posY, int centerX, int centerY) {
		int textureSize = minimapCache.minimapTextureSize;

		// The panel frame is always a square in screen space - no squash.
		// It acts as a clipping window, like the window in a 3D navigation app.
		graphics.enableScissor(posX, posY, posX + viewSize, posY + viewSize);

		// Square background (frame only, no tilt).
		graphics.fill(posX, posY, posX + viewSize, posY + viewSize, 0x40000000);

		if(minimapCache.minimapTexture != null) {
			float verticalScale = Math.max(0.15f,
				MathHelper.sin((float)Math.toRadians(Config.minimapTiltAngle)));
			int stretchedHeight = (int)Math.ceil(textureSize / verticalScale);
			int halfTex = textureSize / 2;
			graphics.getMatrices().push();
			graphics.getMatrices().translate(centerX, centerY, 0);
			graphics.getMatrices().scale(1.0f, verticalScale, 1.0f);
			graphics.drawTexture(RenderLayer::getGuiTextured, minimapCache.minimapTextureId,
				-halfTex, -stretchedHeight / 2, 0, 0, textureSize, stretchedHeight,
				textureSize, textureSize, textureSize, textureSize);
			graphics.getMatrices().pop();
		}

		// Player indicator at the panel centre, always up, not tilted.
		int indicatorSize = (int)(Config.minimapPlayerIndicatorSize * scale);
		int triangleHeight = (int)(indicatorSize * 2.0);
		drawTriangle(graphics,
			centerX, centerY - triangleHeight - 1,
			centerX - indicatorSize - 1, centerY + 1,
			centerX + indicatorSize + 1, centerY + 1,
			0xFF000000);
		drawTriangle(graphics,
			centerX, centerY - triangleHeight,
			centerX - indicatorSize, centerY,
			centerX + indicatorSize, centerY,
			getLocalPlayerIndicatorColor());

		// Other players projected onto the tilted plane.
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayersPseudo3D(graphics, playerPos, scale, centerX, centerY);
		}

		graphics.disableScissor();

		// Square border in screen space, unchanged size.
		int borderThickness = 1;
		int x0 = posX - borderThickness;
		int y0 = posY - borderThickness;
		int x1 = posX + viewSize + borderThickness;
		int y1 = posY + viewSize + borderThickness;
		graphics.fill(x0, y0, x1, y0 + borderThickness, 0xFF000000); // top
		graphics.fill(x0, y1 - borderThickness, x1, y1, 0xFF000000); // bottom
		graphics.fill(x0, y0, x0 + borderThickness, y1, 0xFF000000); // left
		graphics.fill(x1 - borderThickness, y0, x1, y1, 0xFF000000); // right
	}

	/** Draw other players on the perspective-tilted pseudo-3D minimap. */
	private void drawOtherPlayersPseudo3D(DrawContext graphics, Vec3d playerPos, double scale, int centerX, int centerY) {
		if(this.client.world == null) return;

		int maxPlayersToRender = 16;
		int playersRendered = 0;
		// Pseudo-3D bakes the player yaw INTO the texture with +smoothYaw, so a world-relative
		// point must be rotated by +smoothYaw (not negated) to land in the baked texture frame.
		float rotation = (float)Math.toRadians(minimapCache.smoothYaw);
		float sin = MathHelper.sin(rotation);
		float cos = MathHelper.cos(rotation);
		double zoom = Config.minimapZoom;
		int radius = Config.minimapSize / 2;

		double pseudoVerticalScale = Math.max(0.15d,
			Math.sin(Math.toRadians(Config.minimapTiltAngle)));

		for(net.minecraft.entity.player.PlayerEntity otherPlayer : this.client.world.getPlayers()) {
			if(playersRendered >= maxPlayersToRender) break;
			if(otherPlayer == this.client.player || otherPlayer == this.client.getCameraEntity()) continue;
			if(!(otherPlayer.hasVehicle() && otherPlayer.getVehicle() instanceof net.minecraft.entity.vehicle.AbstractBoatEntity)) continue;

			Vec3d otherPos = otherPlayer.getPos();
			double relX = otherPos.x - playerPos.x;
			double relZ = otherPos.z - playerPos.z;

			// Match the normal map basis: +X is screen-right and +Z is screen-down.
			double rx = relX * cos + relZ * sin;
			double rz = -relX * sin + relZ * cos;

			// Clamp within the square scan area before projection.
			if(Config.minimapLimitPlayersToBounds) {
				double limit = radius * zoom;
				rx = MathHelper.clamp(rx, -limit, limit);
				rz = MathHelper.clamp(rz, -limit / pseudoVerticalScale, limit / pseudoVerticalScale);
			}

			// Convert to texture/screen pixel offsets.
			double ox = rx / zoom;
			double oy = rz / zoom;

			// Use the same affine plane projection as the map texture: no depth scaling.
			int screenX = centerX + (int)Math.round(ox);
			int screenY = centerY + (int)Math.round(-oy * pseudoVerticalScale);

			int indicatorSize = (int)(Config.minimapOtherPlayersIndicatorSize * scale);
			int borderSize = 1;
			int borderColor = 0xFF000000;
			if(Config.minimapShowSpeedComparison && this.client.player != null) {
				Vec3d localVelocity = this.client.player.getVelocity();
				double localSpeed = Math.sqrt(localVelocity.x * localVelocity.x + localVelocity.z * localVelocity.z);
				Vec3d otherVelocity = otherPlayer.getVelocity();
				double otherSpeed = Math.sqrt(otherVelocity.x * otherVelocity.x + otherVelocity.z * otherVelocity.z);
				if(otherSpeed > localSpeed) {
					borderColor = 0xFF00FF00;
				} else if(otherSpeed < localSpeed) {
					borderColor = 0xFFFF8000;
				}
			}

			graphics.fill(screenX - indicatorSize - borderSize, screenY - indicatorSize - borderSize,
				screenX + indicatorSize + borderSize + 1, screenY + indicatorSize + borderSize + 1, borderColor);
			int playerColor = getPlayerIndicatorColor(otherPlayer);
			graphics.fill(screenX - indicatorSize, screenY - indicatorSize,
				screenX + indicatorSize + 1, screenY + indicatorSize + 1, playerColor);

			if(Config.minimapShowOtherPlayersNames) {
				String playerName = otherPlayer.getName().getString();
				float nameScale = (float)Config.minimapOtherPlayersNameSize;
				graphics.getMatrices().push();
				graphics.getMatrices().translate(screenX, screenY - indicatorSize - 2, 0);
				graphics.getMatrices().scale(nameScale, nameScale, 1.0f);
				int textWidth = this.client.textRenderer.getWidth(playerName);
				graphics.drawTextWithShadow(this.client.textRenderer, playerName, -textWidth / 2, 0, 0xFFFFFF);
				graphics.getMatrices().pop();
			}

			playersRendered++;
		}
	}

	/** Renders the speed bar atop the HUD, uses displayedSpeed to, well, diisplay the speed. */
	private void renderBar(DrawContext graphics, int x, int y) {
		graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_OFF[Config.barType], x, y, 182, 5);
		// Get HUD data from Common instance
		HudData hudData = Common.getInstance().getHudData();
		if(hudData == null) return;
		
		if(hudData.speed < MIN_V[Config.barType]) return;
		if(hudData.speed > MAX_V[Config.barType]) {
			if(this.client.world != null && this.client.world.getTime() % 2 == 0) return;
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_ON[Config.barType], x, y, 182, 5);
			return;
		}
		graphics.drawGuiTexture(RenderLayer::getGuiTextured, BAR_ON[Config.barType], 182, 5, 0, 0, x, y, (int)((this.displayedSpeed - MIN_V[Config.barType]) * SCALE_V[Config.barType]), 5);
	}

	/** Implementation is cloned from the notchian ping display in the tab player list. */
	private void renderPing(DrawContext graphics, int x, int y) {
		Identifier bar = PING_5;
		// Get HUD data from Common instance
		HudData hudData = Common.getInstance().getHudData();
		if(hudData == null) return;
		
		if(hudData.ping < 0) {
			bar = PING_UNKNOWN;
		}
		else if(hudData.ping < 150) {
			bar = PING_5;
		}
		else if(hudData.ping < 300) {
			bar = PING_4;
		}
		else if(hudData.ping < 600) {
			bar = PING_3;
		}
		else if(hudData.ping < 1000) {
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
		// Simple but reliable implementation using rectangle fills
		// Only draw pixels within the circle radius
		for (int y = -radius; y <= radius; y++) {
			// Precompute the horizontal range for this y level
			int xRange = (int) Math.sqrt(radius * radius - y * y);
			// Draw a horizontal line across the circle at this y level
			graphics.fill(centerX - xRange, centerY + y, centerX + xRange + 1, centerY + y + 1, color);
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
