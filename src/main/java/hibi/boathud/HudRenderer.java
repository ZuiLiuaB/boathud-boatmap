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

public class HudRenderer {

	private final MinecraftClient client;
	private int scaledWidth;
	private int scaledHeight;

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

	/** Renders the minimap showing ice blocks */
	public void renderMinimap(DrawContext graphics) {
		if(this.client.player == null || this.client.world == null) return;

		int minimapSize = 128;
		int halfSize = minimapSize / 2;
		int renderDistance = 16; // Blocks to render around the player

		// Get player position and rotation
		Vec3d playerPos = this.client.player.getPos();
		BlockPos playerBlockPos = this.client.player.getBlockPos();
		float playerYaw = this.client.player.getYaw();

		// Calculate minimap position
		int posX = Config.minimapX;
		int posY = Config.minimapY;
		double scale = Config.minimapScale;

		// Calculate actual rendered size
		int renderedSize = (int)(minimapSize * scale);
		int renderedHalfSize = renderedSize / 2;

		// Draw minimap background
		graphics.fill(posX, posY, posX + renderedSize, posY + renderedSize, 0x40000000);

		// Calculate rotation angle (0 if locked to north, -yaw if following player)
		double rotationRad = Config.minimapLockNorth ? 0 : Math.toRadians(-playerYaw);
		double cos = Config.minimapLockNorth ? 1 : Math.cos(rotationRad);
		double sin = Config.minimapLockNorth ? 0 : Math.sin(rotationRad);

		// Iterate through nearby blocks
		for(int x = -renderDistance; x <= renderDistance; x++) {
			for(int z = -renderDistance; z <= renderDistance; z++) {
				BlockPos blockPos = playerBlockPos.add(x, Config.minimapYOffset, z);
				BlockState blockState = this.client.world.getBlockState(blockPos);
				Block block = blockState.getBlock();

				// Check if it's an ice block we want to render
				if(block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) {
					// Calculate relative position with sub-block precision
					double relX = (x + 0.5) + (playerPos.x - playerBlockPos.getX());
					double relZ = (z + 0.5) + (playerPos.z - playerBlockPos.getZ());

					// Apply rotation if not locked to north
					double rotatedX = relX * cos - relZ * sin;
					double rotatedZ = relX * sin + relZ * cos;

					// Calculate distance from player
					double distance = Math.sqrt(rotatedX * rotatedX + rotatedZ * rotatedZ);
					if(distance > renderDistance) continue;

					// Calculate screen position with double precision
					double screenX = posX + renderedHalfSize + (rotatedX * (renderedHalfSize / (double)renderDistance));
					double screenZ = posY + renderedHalfSize + (rotatedZ * (renderedHalfSize / (double)renderDistance));

					// Calculate Y difference for transparency
					int yDiff = Math.abs(blockPos.getY() - playerBlockPos.getY());
					float alpha = 1.0f - (yDiff / 10.0f);
					alpha = MathHelper.clamp(alpha, 0.1f, 1.0f);

					// Set color with transparency
					int color = (int)(alpha * 255) << 24 | 0xFFFFFF;

					// Draw the block as a filled rectangle connecting to neighbors
					double blockSize = scale; // Use scale directly for better size control
					int blockPixelSize = (int)Math.ceil(blockSize);
					graphics.fill((int)(screenX - blockPixelSize / 2.0), (int)(screenZ - blockPixelSize / 2.0), 
					              (int)(screenX + blockPixelSize / 2.0), (int)(screenZ + blockPixelSize / 2.0), color);
				}
			}
		}

		// Draw player indicator
		graphics.fill(posX + renderedHalfSize - 2, posY + renderedHalfSize - 2, posX + renderedHalfSize + 2, posY + renderedHalfSize + 2, 0xFFFF0000);

		// Draw minimap border
		graphics.fill(posX, posY, posX + renderedSize, posY + 1, 0xFFFFFFFF); // Top
		graphics.fill(posX, posY + renderedSize - 1, posX + renderedSize, posY + renderedSize, 0xFFFFFFFF); // Bottom
		graphics.fill(posX, posY, posX + 1, posY + renderedSize, 0xFFFFFFFF); // Left
		graphics.fill(posX + renderedSize - 1, posY, posX + renderedSize, posY + renderedSize, 0xFFFFFFFF); // Right
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
