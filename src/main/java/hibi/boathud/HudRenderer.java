package hibi.boathud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import java.util.HashMap;
import java.util.Map;

import static hibi.boathud.Config.minimapSize;

public class HudRenderer {

	private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {
		net.minecraft.world.entity.EquipmentSlot.FEET,
		net.minecraft.world.entity.EquipmentSlot.LEGS,
		net.minecraft.world.entity.EquipmentSlot.CHEST,
		net.minecraft.world.entity.EquipmentSlot.HEAD
	};

	private final Minecraft client;
	private int scaledWidth;
	private int scaledHeight;

	// Cache for minimap to avoid recalculating every frame
	// Make this non-static to avoid memory leaks when the HUD renderer is recreated
	private class MinimapCache {
		float smoothYaw = 0f; // Only keep smooth rotation value, no other unnecessary state
		// Pre-rendered minimap held as a GPU texture so it can be drawn in a single call
		DynamicTexture minimapTexture = null;
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

	// Used for lerping
	private double displayedSpeed = 0.0d;

	public HudRenderer(Minecraft client) {
		this.client = client;
	}

	public void render(GuiGraphicsExtractor graphics, DeltaTracker counter) {
		this.scaledWidth = graphics.guiWidth();
		this.scaledHeight = graphics.guiHeight();
		int i = this.scaledWidth / 2;
		
		// Get HUD data from Common instance
		HudData hudData = Common.getInstance().getHudData();
		if(hudData == null) return;
		
		int nameLen = this.client.font.width(hudData.name);
		
		// Lerping the displayed speed with the actual speed against how far we are into the tick not only is mostly accurate,
		// but gives the impression that it's being updated faster than 20 hz (which it isn't)
		this.displayedSpeed = Mth.lerp(counter.getGameTimeDeltaPartialTick(true), this.displayedSpeed, hudData.speed);

		// Whole-HUD transform: user-configurable position (hudX/hudY) and scale (hudScale).
		// Anchor is the top-left of the HUD overlay (i - 91, scaledHeight - 83); scaling pivots there.
		float hudScale = (float)Config.hudScale;
		int hudAnchorX = i - 91 + Config.hudX;
		int hudAnchorY = this.scaledHeight - 83 + Config.hudY;
		graphics.pose().pushMatrix();
		graphics.pose().translate(hudAnchorX, hudAnchorY);
		graphics.pose().scale(hudScale, hudScale);
		graphics.pose().translate(-(i - 91), -(this.scaledHeight - 83));

		if(Config.extended) {
				// Overlay texture and bar
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_EXTENDED, i - 91, this.scaledHeight - 83, 182, 33);
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);

				// Sprites
				if(Common.getInstance().getHudData() != null && Common.getInstance().getHudData().isDriver) {
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
				if(Common.getInstance().getHudData() != null) {
					this.typeCentered(graphics, String.format(Config.angleFormat, Common.getInstance().getHudData().driftAngle), i, this.scaledHeight - 76, 0xFFFFFFFF);
					this.typeCentered(graphics, String.format(Config.gFormat, Common.getInstance().getHudData().g), i + 58, this.scaledHeight - 76, 0xFFFFFFFF);
					// Second Row
					graphics.text(this.client.font, Common.getInstance().getHudData().name, i + 88 - nameLen, this.scaledHeight - 65, 0xFFFFFFFF);
				}
	
		} else { // Compact mode
				// Overlay texture and bar
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_COMPACT, i - 91, this.scaledHeight - 83, 182, 20);
				// Always render speed bar, showSpeedBar setting removed
				this.renderBar(graphics, i - 91, this.scaledHeight - 83);
				// Speed and drift angle
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, hudData.driftAngle), i + 58, this.scaledHeight - 76, 0xFFFFFFFF);
		}

		graphics.pose().popMatrix();

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
	public void renderMinimap(GuiGraphicsExtractor graphics) {
		if(this.client.level == null) return;

		// Get camera entity (local player or spectated player)
		net.minecraft.world.entity.Entity cameraEntity = this.client.getCameraEntity();
		if(cameraEntity == null) return;

		// Get player position and rotation
		Vec3 playerPos = cameraEntity.position();
		float playerYaw = cameraEntity.getYRot();
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
			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY);
			if(!Config.minimapLockNorth) {
				// Minecraft yaw increases clockwise, OpenGL rotation is counter-clockwise, so invert
				float rotation = (float)Math.toRadians(-minimapCache.smoothYaw);
				graphics.pose().rotate(rotation);
			}
			graphics.blit(RenderPipelines.GUI_TEXTURED, minimapCache.minimapTextureId,
				-texHalf, -texHalf, 0, 0, textureSize, textureSize, textureSize, textureSize, textureSize, textureSize);
			graphics.pose().popMatrix();
			if(square) graphics.disableScissor();
		}

		// Own indicator: hidden in spectator-label mode (the spectated player is drawn as a
		// labelled head in drawOtherPlayers instead).
		if(!spectatorLabelActive()) {
			// Player indicator at the centre - upward pointing triangle with customizable size
			int indicatorSize = (int)(Config.minimapPlayerIndicatorSize * scale);
			int triangleHeight = (int)(indicatorSize * 2.0); // Taller, more pointed triangle

			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY);

			// Only rotate the player indicator when the map itself is locked to north
			if(Config.minimapLockNorth) {
				// Calculate player's actual direction (inverted to fix rotation direction)
				float playerRotation = (float)Math.toRadians(playerYaw + 180);
				graphics.pose().rotate(playerRotation);
			}

			// Draw black border triangle (slightly larger), outline thickness configurable (0 = none)
			int borderSize = Math.max(0, (int)Math.round(Config.minimapPlayerIndicatorBorderSize));
			if(borderSize > 0) {
				drawTriangle(graphics,
					0, -triangleHeight - borderSize, // Top point
					-indicatorSize - borderSize, borderSize, // Bottom left
					indicatorSize + borderSize, borderSize, // Bottom right
					0xFF000000); // Black border
			}

			// Draw filled triangle with color based on leather armor
			drawTriangle(graphics,
				0, -triangleHeight, // Top point
				-indicatorSize, 0, // Bottom left
				indicatorSize, 0, // Bottom right
				getLocalPlayerIndicatorColor());

			graphics.pose().popMatrix();
		}

		// Direction indicator (north arrow) - always points north, so rotate opposite to the map
		if(Config.minimapShowNorthIndicator) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY - half + 14);
			float arrowRotation = (float)Math.toRadians(-minimapCache.smoothYaw);
			graphics.pose().rotate(arrowRotation);
			// Black outline (arrowhead + shaft)
			drawTriangle(graphics, 0, -14, -4, -6, 4, -6, 0xFF000000);
			graphics.fill(-2, -7, 2, 1, 0xFF000000);
			// White arrowhead + shaft
			drawTriangle(graphics, 0, -13, -3, -6, 3, -6, 0xFFFFFFFF);
			graphics.fill(-1, -7, 1, 0, 0xFFFFFFFF);
			graphics.pose().popMatrix();
		}

		// Draw other players in boats if enabled
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayers(graphics, centerX, centerY, playerPos, scale, minimapCache.smoothYaw);
		}

		// Panel border (thickness configurable, 0 = none)
		int borderThickness = Math.max(0, (int)Math.round(Config.minimapBorderSize));
		if(borderThickness > 0) {
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
	private void preRenderMinimap(Vec3 playerPos, float playerYaw) {
		if(this.client.level == null) return;

		int textureSize = getMinimapTextureSize();
		boolean pseudo3D = Config.minimapShape == 2;

		int arraySize = textureSize * textureSize;
		long now = this.client.level.getGameTime();

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
					&& Math.abs(Mth.wrapDegrees(playerYaw - (float)minimapCache.lastScanYaw)) < 0.5f) {
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

		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

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
				// floor (not cast): a cast truncates towards zero and shifts the whole
				// scan by one block at negative coordinates.
				worldX = Mth.floor(playerPos.x + (offsetX * cosYaw - planeZ * sinYaw) * zoom);
				worldZ = Mth.floor(playerPos.z + (offsetX * sinYaw + planeZ * cosYaw) * zoom);
			} else {
				worldX = Mth.floor(playerPos.x + offsetX * zoom);
				worldZ = Mth.floor(playerPos.z + offsetZ * zoom);
			}
				preRenderedMinimap[x + z * textureSize] = scanIceAt(worldX, worldZ, playerY, mutablePos, abovePos);
			}
		}

		ensureMinimapTexture(textureSize);
		NativeImage img = minimapCache.minimapTexture.getPixels();
		int viewSize = Config.minimapSize;
		double halfView = viewSize / 2.0d;
		double r2View = halfView * halfView;

		for(int x = 0; x < textureSize; x++) {
			for(int z = 0; z < textureSize; z++) {
				double relX = x - halfTex + 0.5d;
				double relZ = z - halfTex + 0.5d;

				// Only the circle shape masks down to an inscribed circle.
				if(Config.minimapShape == 0 && relX * relX + relZ * relZ > r2View) {
					img.setPixel(x, z, 0);
					continue;
				}

				int color = preRenderedMinimap[x + z * textureSize];
				if((color >>> 24) == 0) {
					img.setPixel(x, z, 0);
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
					img.setPixel(x, z, isEdge ? 0xFF000000 : color);
				} else {
					img.setPixel(x, z, color);
				}
			}
		}
		minimapCache.minimapTexture.upload();
	}

	/** Scans a single world column for a visible ice block and returns its minimap colour. */
	private int scanIceAt(int worldX, int worldZ, int playerY, BlockPos.MutableBlockPos mutablePos, BlockPos.MutableBlockPos abovePos) {
		// Check current Y level first (most common case)
		mutablePos.set(worldX, playerY + Config.minimapYOffset, worldZ);
		BlockState currentState = this.client.level.getBlockState(mutablePos);
		Block currentBlock = currentState.getBlock();

		abovePos.set(mutablePos.getX(), mutablePos.getY() + 1, mutablePos.getZ());
		if(isIceBlock(currentBlock) && hasTransparentAbove(abovePos)) {
			return calculateIceColor(0, playerY);
		}

		// If not found, check below player level (limit vertical check range)
		int maxBelow = Math.max(-Config.minimapIceDetectionRange, -10);
		for(int yOffset = -1; yOffset >= maxBelow; yOffset--) {
			mutablePos.set(worldX, playerY + Config.minimapYOffset + yOffset, worldZ);
			BlockState blockState = this.client.level.getBlockState(mutablePos);
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
				BlockState blockState = this.client.level.getBlockState(mutablePos);
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
				this.client.getTextureManager().release(minimapCache.minimapTextureId);
				minimapCache.minimapTexture.close();
			}
			NativeImage img = new NativeImage(textureSize, textureSize, false);
			minimapCache.minimapTexture = new DynamicTexture(() -> "boathud:minimap", img);
			minimapCache.minimapTexture.upload();
			// registerTexture(Identifier, Texture) is stable across versions; registerDynamicTexture's
			// (String, NativeImageBackedTexture) overload does not exist at runtime in 1.21.4 and crashes.
			minimapCache.minimapTextureId = Identifier.fromNamespaceAndPath("boathud", "minimap");
			this.client.getTextureManager().register(minimapCache.minimapTextureId, minimapCache.minimapTexture);
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
	
	/** Returns the dye colour of the first dyed leather armour piece, or null when the player wears none. */
	private static Integer getLeatherArmorColor(net.minecraft.world.entity.player.Player player) {
		// No ArmorItem type check needed: the DYED_COLOR component only exists on dyed
		// leather armour - every other armour piece returns null for it.
		for(net.minecraft.world.entity.EquipmentSlot slot : ARMOR_SLOTS) {
			net.minecraft.world.item.ItemStack armorItem = player.getItemBySlot(slot);
			var dyeColor = armorItem.get(DataComponents.DYED_COLOR);
			if(dyeColor != null) {
				return 0xFF000000 | (dyeColor.rgb() & 0xFFFFFF);
			}
		}
		return null;
	}

	/**
	 * Frame colour for another player's head icon: their leather armour dye colour in team
	 * matches (any dyed leather piece wins), falling back to blue when none is worn.
	 */
	private int getPlayerIndicatorColor(net.minecraft.world.entity.player.Player player) {
		Integer leather = getLeatherArmorColor(player);
		return leather != null ? leather : 0xFF0000FF;
	}

	/**
	 * Cached per-player appearance for the minimap: indicator colour, name and the measured
	 * name width. These used to be recomputed for every player on every frame - armour
	 * component lookups, name strings and glyph measurements - which dominated the HUD frame
	 * cost on a crowded server. They only change a few times per second, so the whole cache
	 * is dropped every 5 ticks (players who log off cannot leak) and misses fill on demand.
	 */
	private static final class PlayerAppearance {
		int indicatorColor;
		String name;
		int nameWidth;
	}

	private final java.util.Map<java.util.UUID, PlayerAppearance> appearanceCache = new java.util.HashMap<>();
	private long appearanceCacheTick = Long.MIN_VALUE;

	private PlayerAppearance getPlayerAppearance(net.minecraft.world.entity.player.Player player) {
		long now = this.client.level != null ? this.client.level.getGameTime() : 0L;
		if(now != appearanceCacheTick) {
			appearanceCacheTick = now;
			if(now % 5 == 0) appearanceCache.clear();
		}
		java.util.UUID id = player.getUUID();
		PlayerAppearance appearance = appearanceCache.get(id);
		if(appearance == null) {
			appearance = new PlayerAppearance();
			appearance.indicatorColor = getPlayerIndicatorColor(player);
			appearance.name = player.getName().getString();
			appearance.nameWidth = this.client.font.width(appearance.name);
			appearanceCache.put(id, appearance);
		}
		return appearance;
	}
	
	/** Get the indicator color for the local player */
	private int getLocalPlayerIndicatorColor() {
		if (this.client.player == null) return 0xFFFF0000; // Default red for local player
		
		// Check if local player is wearing leather armor
		for (net.minecraft.world.entity.EquipmentSlot slot : ARMOR_SLOTS) {
			net.minecraft.world.item.ItemStack armorItem = this.client.player.getItemBySlot(slot);
			if (true) {
				// Get color from leather armor using DataComponentTypes
				var dyeColor = armorItem.get(DataComponents.DYED_COLOR);
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
	
	/**
	 * Draws another player's head (their skin texture) centred on (centerX, centerY), framed by
	 * their leather armour colour (or blue). When speed comparison is active, a second outer
	 * frame in the speed colour is drawn around it.
	 */
	/**
	 * Draws another player's head (their skin texture) centred on (centerX, centerY), framed by
	 * their leather armour colour (or blue).
	 */
	private void drawPlayerHead(GuiGraphicsExtractor graphics, net.minecraft.client.player.AbstractClientPlayer player,
			int centerX, int centerY, int size, int frameColor) {
		int half = size / 2;
		int x0 = centerX - half;
		int y0 = centerY - half;
		int x1 = x0 + size;
		int y1 = y0 + size;

		// Team frame: leather armour dye colour, or blue when none is worn (0 = no frame)
		int frameSize = Math.max(0, (int)Math.round(Config.minimapOtherPlayersHeadFrameSize));
		if(frameSize > 0) {
			graphics.fill(x0 - frameSize, y0 - frameSize, x1 + frameSize, y1 + frameSize, frameColor);
		}
		// Black backing so skin transparency still reads as an icon
		graphics.fill(x0, y0, x1, y1, 0xFF000000);

		// Skin texture via PlayerSkin -> ClientAsset.Texture -> Identifier
		net.minecraft.world.entity.player.PlayerSkin skin = player.getSkin();
		if(skin == null) return;
		net.minecraft.core.ClientAsset.Texture body = skin.body();
		if(body == null) return;
		Identifier skinId = body.texturePath();
		if(skinId == null) return;
		// On a 64x64 skin the head is the 8x8 region at (8,8) and the hat overlay at (40,8).
		graphics.blit(RenderPipelines.GUI_TEXTURED, skinId, x0, y0, 8f, 8f, size, size, 8, 8, 64, 64);
		graphics.blit(RenderPipelines.GUI_TEXTURED, skinId, x0, y0, 40f, 8f, size, size, 8, 8, 64, 64);
	}

	/** Check if the block above is transparent */
	private boolean hasTransparentAbove(BlockPos pos) {
		if(this.client.level == null) return false;
		BlockState state = this.client.level.getBlockState(pos);
		if(state.isAir()) return true;
		// Blocks you can walk straight through (tripwire, torches, signs, buttons, levers,
		// redstone wire, flowers, grass, snow layers, vines, light blocks...) mostly fail
		// canBeReplaced(), yet they must NOT hide the ice below them. The reliable test is
		// whether the block has any collision shape at all - pass-through blocks have none.
		return state.getCollisionShape(this.client.level, pos).isEmpty();
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
	
	/** True when spectator-label mode is on and the local player is currently spectating. */
	private boolean spectatorLabelActive() {
		return Config.minimapSpectatorLabel && this.client.player != null && this.client.player.isSpectator();
	}

	/** Draw other players in boats on the minimap as head icons framed by their team colour */
	private void drawOtherPlayers(GuiGraphicsExtractor graphics, int centerX, int centerY, Vec3 playerPos, double scale, float currentYaw) {
		if(this.client.level == null) return;

		// Calculate minimap radius
		int renderedSize = (int)(Config.minimapSize * scale);
		int radius = renderedSize / 2;

		// Get all players in the world (limit to improve performance)
		int maxPlayersToRender = 16;
		int playersRendered = 0;
		double zoom = Config.minimapZoom;

		// In spectator-label mode the spectated player (the camera entity) must still be drawn
		// as a labelled head, so only skip it when the mode is off.
		boolean spectatorLabel = spectatorLabelActive();

		// Loop-invariant rotation basis - hoisted out of the per-player loop so the trig
		// runs once per frame instead of once per player.
		double rotation = Math.toRadians(-currentYaw);
		double sin = Math.sin(rotation);
		double cos = Math.cos(rotation);

		for(net.minecraft.client.player.AbstractClientPlayer otherPlayer : this.client.level.players()) {
			// Limit the number of players rendered to improve performance
			if(playersRendered >= maxPlayersToRender) break;

			// Skip the local player (or the player we're spectating)
			if(otherPlayer == this.client.player) continue;
			if(!spectatorLabel && otherPlayer == this.client.getCameraEntity()) continue;

			// Check if player is in a boat (one getVehicle() call - it returns null when the player has none)
			if(otherPlayer.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat) {
				// Relative coordinates stay in full double precision all the way to the final
				// pixel rounding - no (int) truncation - so sub-block motion maps smoothly.
				Vec3 otherPos = otherPlayer.position();

				// Calculate relative coordinates with precise floating-point values
				double relX = otherPos.x - playerPos.x;
				double relZ = otherPos.z - playerPos.z;

				// Rotate the relative coordinates (basis hoisted above the loop)
				double rotatedX = relX * cos - relZ * sin;
				double rotatedZ = relX * sin + relZ * cos;

				// Calculate screen coordinates with scale and zoom
				// Apply zoom factor to relative coordinates to match minimap zoom level
				double screenXd = centerX + rotatedX * scale / zoom;
				double screenYd = centerY + rotatedZ * scale / zoom;

				// Limit player indicator within minimap bounds if enabled
				if(Config.minimapLimitPlayersToBounds) {
					double dx = screenXd - centerX;
					double dy = screenYd - centerY;
					if(Config.minimapShape == 1) {
						// Square panel: clamp each axis independently so the marker slides along the edge
						screenXd = centerX + Mth.clamp(dx, -radius, radius);
						screenYd = centerY + Mth.clamp(dy, -radius, radius);
					} else {
						// Circular panel: project onto the rim
						double distance = Math.sqrt(dx * dx + dy * dy);
						if(distance > radius) {
							double angle = Math.atan2(dy, dx);
							screenXd = centerX + Math.cos(angle) * radius;
							screenYd = centerY + Math.sin(angle) * radius;
						}
					}
				}
				int screenX = (int)Math.round(screenXd);
				int screenY = (int)Math.round(screenYd);

				// Head icon size from its own setting
				int headSize = Math.max(2, (int)Math.round(Config.minimapOtherPlayersHeadSize * scale));

			// Head icon framed by the team leather colour (or blue when none is worn).
			// Colour/name/name width come from the ~4 Hz appearance cache.
			PlayerAppearance appearance = getPlayerAppearance(otherPlayer);
			drawPlayerHead(graphics, otherPlayer, screenX, screenY, headSize, appearance.indicatorColor);

			// Draw player name if enabled
			if(Config.minimapShowOtherPlayersNames) {
				String playerName = appearance.name;

				// Set name scale
				float nameScale = (float)Config.minimapOtherPlayersNameSize;

				// Manual offsets from the head icon centre: Y negative = above, positive = below;
				// X negative = left, positive = right. Both in screen pixels.
				double nameY = screenY + Config.minimapOtherPlayersNameOffsetY;
				double nameX = screenX + Config.minimapOtherPlayersNameOffsetX;

				// Save current matrix state
				graphics.pose().pushMatrix();

				// Apply translation and scale
				graphics.pose().translate((float)nameX, (float)nameY);
				graphics.pose().scale(nameScale, nameScale);

				// Calculate centered text position
				int textWidth = appearance.nameWidth;
				int centeredX = -textWidth / 2;
				int textY = 0;

				// Draw text for better visibility
				graphics.text(this.client.font, playerName, centeredX, textY, 0xFFFFFFFF);

				// Restore matrix state
				graphics.pose().popMatrix();
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
	private void renderMinimapPseudo3D(GuiGraphicsExtractor graphics, Vec3 playerPos, double scale, int viewSize, int half, int posX, int posY, int centerX, int centerY) {
		int textureSize = minimapCache.minimapTextureSize;

		// The panel frame is always a square in screen space - no squash.
		// It acts as a clipping window, like the window in a 3D navigation app.
		graphics.enableScissor(posX, posY, posX + viewSize, posY + viewSize);

		// Square background (frame only, no tilt).
		graphics.fill(posX, posY, posX + viewSize, posY + viewSize, 0x40000000);

		if(minimapCache.minimapTexture != null) {
			float verticalScale = Math.max(0.15f,
				Mth.sin((float)Math.toRadians(Config.minimapTiltAngle)));
			int stretchedHeight = (int)Math.ceil(textureSize / verticalScale);
			int halfTex = textureSize / 2;
			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY);
			graphics.pose().scale(1.0f, verticalScale);
			graphics.blit(RenderPipelines.GUI_TEXTURED, minimapCache.minimapTextureId,
				-halfTex, -stretchedHeight / 2, 0f, 0f, textureSize, stretchedHeight,
				textureSize, textureSize, textureSize, textureSize);
			graphics.pose().popMatrix();
		}

		// Player indicator at the panel centre, always up, not tilted.
		// Hidden in spectator-label mode (the spectated player is drawn as a labelled head instead).
		if(!spectatorLabelActive()) {
			int indicatorSize = (int)(Config.minimapPlayerIndicatorSize * scale);
			int triangleHeight = (int)(indicatorSize * 2.0);
			// Black outline thickness configurable (0 = no outline)
			int arrowBorder = Math.max(0, (int)Math.round(Config.minimapPlayerIndicatorBorderSize));
			if(arrowBorder > 0) {
				drawTriangle(graphics,
					centerX, centerY - triangleHeight - arrowBorder,
					centerX - indicatorSize - arrowBorder, centerY + arrowBorder,
					centerX + indicatorSize + arrowBorder, centerY + arrowBorder,
					0xFF000000);
			}
			drawTriangle(graphics,
				centerX, centerY - triangleHeight,
				centerX - indicatorSize, centerY,
				centerX + indicatorSize, centerY,
				getLocalPlayerIndicatorColor());
		}

		// Other players projected onto the tilted plane.
		if(Config.minimapShowOtherPlayers) {
			drawOtherPlayersPseudo3D(graphics, playerPos, scale, centerX, centerY);
		}

		// North direction arrow (optional), rotated opposite to the map.
		if(Config.minimapShowNorthIndicator) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, posY + 14);
			float arrowRotation = (float)Math.toRadians(-minimapCache.smoothYaw);
			graphics.pose().rotate(arrowRotation);
			// Black outline (arrowhead + shaft)
			drawTriangle(graphics, 0, -14, -4, -6, 4, -6, 0xFF000000);
			graphics.fill(-2, -7, 2, 1, 0xFF000000);
			// White arrowhead + shaft
			drawTriangle(graphics, 0, -13, -3, -6, 3, -6, 0xFFFFFFFF);
			graphics.fill(-1, -7, 1, 0, 0xFFFFFFFF);
			graphics.pose().popMatrix();
		}

		graphics.disableScissor();

		// Square border in screen space, unchanged size (thickness configurable, 0 = none).
		int borderThickness = Math.max(0, (int)Math.round(Config.minimapBorderSize));
		if(borderThickness > 0) {
			int x0 = posX - borderThickness;
			int y0 = posY - borderThickness;
			int x1 = posX + viewSize + borderThickness;
			int y1 = posY + viewSize + borderThickness;
			graphics.fill(x0, y0, x1, y0 + borderThickness, 0xFF000000); // top
			graphics.fill(x0, y1 - borderThickness, x1, y1, 0xFF000000); // bottom
			graphics.fill(x0, y0, x0 + borderThickness, y1, 0xFF000000); // left
			graphics.fill(x1 - borderThickness, y0, x1, y1, 0xFF000000); // right
		}
	}

	/** Draw other players on the perspective-tilted pseudo-3D minimap. */
	private void drawOtherPlayersPseudo3D(GuiGraphicsExtractor graphics, Vec3 playerPos, double scale, int centerX, int centerY) {
		if(this.client.level == null) return;

		int maxPlayersToRender = 16;
		int playersRendered = 0;
		// Pseudo-3D bakes the player yaw INTO the texture with +smoothYaw, so a world-relative
		// point must be rotated by +smoothYaw (not negated) to land in the baked texture frame.
		float rotation = (float)Math.toRadians(minimapCache.smoothYaw);
		float sin = Mth.sin(rotation);
		float cos = Mth.cos(rotation);
		double zoom = Config.minimapZoom;
		int radius = Config.minimapSize / 2;

		// In spectator-label mode the spectated player (the camera entity) must still be drawn
		// as a labelled head, so only skip it when the mode is off.
		boolean spectatorLabel = spectatorLabelActive();

		double pseudoVerticalScale = Math.max(0.15d,
			Math.sin(Math.toRadians(Config.minimapTiltAngle)));

		for(net.minecraft.client.player.AbstractClientPlayer otherPlayer : this.client.level.players()) {
			if(playersRendered >= maxPlayersToRender) break;
			if(otherPlayer == this.client.player) continue;
			if(!spectatorLabel && otherPlayer == this.client.getCameraEntity()) continue;
			if(!(otherPlayer.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)) continue;

			Vec3 otherPos = otherPlayer.position();
			double relX = otherPos.x - playerPos.x;
			double relZ = otherPos.z - playerPos.z;

			// Match the normal map basis: +X is screen-right and +Z is screen-down.
			double rx = relX * cos + relZ * sin;
			double rz = -relX * sin + relZ * cos;

			// Clamp within the square scan area before projection.
			if(Config.minimapLimitPlayersToBounds) {
				double limit = radius * zoom;
				rx = Mth.clamp(rx, -limit, limit);
				rz = Mth.clamp(rz, -limit / pseudoVerticalScale, limit / pseudoVerticalScale);
			}

			// Convert to texture/screen pixel offsets.
			double ox = rx / zoom;
			double oy = rz / zoom;

			// Use the same affine plane projection as the map texture: no depth scaling.
			int screenX = centerX + (int)Math.round(ox);
			int screenY = centerY + (int)Math.round(-oy * pseudoVerticalScale);

			int headSize = Math.max(2, (int)Math.round(Config.minimapOtherPlayersHeadSize * scale));

			// Head icon framed by the team leather colour (or blue when none is worn).
			// Colour/name/name width come from the ~4 Hz appearance cache.
			PlayerAppearance appearance = getPlayerAppearance(otherPlayer);
			drawPlayerHead(graphics, otherPlayer, screenX, screenY, headSize, appearance.indicatorColor);

			if(Config.minimapShowOtherPlayersNames) {
				String playerName = appearance.name;
				float nameScale = (float)Config.minimapOtherPlayersNameSize;
				// Manual offsets from the head icon centre: Y negative = above, positive = below;
				// X negative = left, positive = right. Both in screen pixels.
				double nameY = screenY + Config.minimapOtherPlayersNameOffsetY;
				double nameX = screenX + Config.minimapOtherPlayersNameOffsetX;
				graphics.pose().pushMatrix();
				graphics.pose().translate((float)nameX, (float)nameY);
				graphics.pose().scale(nameScale, nameScale);
				int textWidth = appearance.nameWidth;
				graphics.text(this.client.font, playerName, -textWidth / 2, 0, 0xFFFFFFFF);
				graphics.pose().popMatrix();
			}

			playersRendered++;
		}
	}

	/** Renders the speed bar atop the HUD, uses displayedSpeed to, well, diisplay the speed. */
	private void renderBar(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Config.barType.unlitBar, x, y, 182, 5);
		int progress = Config.barType.getProgress(this.displayedSpeed);
		if(progress == 0) return;
		if(progress == -1) {
			if(this.client.level != null && this.client.level.getGameTime() % 2 == 0) return;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Config.barType.litBar, x, y, 182, 5);
			return;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Config.barType.litBar, 182, 5, 0, 0, x, y, progress, 5);
	}

	/** Implementation is cloned from the notchian ping display in the tab player list. */
	private void renderPing(GuiGraphicsExtractor graphics, int x, int y) {
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bar, x, y, 10, 8);
	}

	/** Renders a piece of text centered horizontally on an X coordinate. */
	private void typeCentered(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
		graphics.text(this.client.font, text, centerX - this.client.font.width(text) / 2, y, color);
	}

	/** Custom triangle drawing method since fillTriangle is not available in GuiGraphicsExtractor */
	private void drawTriangle(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
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

	/** Custom circle drawing methods since fillEllipse and drawEllipse are not available */

	/** Draw a filled circle */
	private void drawCircleFill(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
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
	private void drawCircle(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
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
		PING_5 = Identifier.fromNamespaceAndPath("boathud", "ping_5"),
		PING_4 = Identifier.fromNamespaceAndPath("boathud", "ping_4"),
		PING_3 = Identifier.fromNamespaceAndPath("boathud", "ping_3"),
		PING_2 = Identifier.fromNamespaceAndPath("boathud", "ping_2"),
		PING_1 = Identifier.fromNamespaceAndPath("boathud", "ping_1"),
		PING_UNKNOWN = Identifier.fromNamespaceAndPath("boathud", "ping_unknown")
	;
}
