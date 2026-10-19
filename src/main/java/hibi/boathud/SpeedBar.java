package hibi.boathud;

import net.minecraft.resources.Identifier;

public enum SpeedBar {
	PACKED(0, 40, 4.55, Identifier.fromNamespaceAndPath("boathud", "bar_1_lit"), Identifier.fromNamespaceAndPath("boathud", "bar_1_unlit")),
	MIXED(8, 70, 182d/62d, Identifier.fromNamespaceAndPath("boathud", "bar_2_lit"), Identifier.fromNamespaceAndPath("boathud", "bar_2_unlit")),
	BLUE(40, 70, 182d/30d, Identifier.fromNamespaceAndPath("boathud", "bar_3_lit"), Identifier.fromNamespaceAndPath("boathud", "bar_3_unlit")),
	PROGRESSIVE(0, 70, 0, Identifier.fromNamespaceAndPath("boathud", "bar_4_lit"), Identifier.fromNamespaceAndPath("boathud", "bar_4_unlit")) {
		@Override
		public int getProgress(double displayedSpeed) {
			if (displayedSpeed < this.minV) return 0;
			if (displayedSpeed > this.maxV) return -1;
			if (displayedSpeed < 8) return (int)(displayedSpeed * 22.75);
			if (displayedSpeed < 40) return (int)((displayedSpeed - 8) * 5.6875);
			return (int)((displayedSpeed - 40) * 6.0666);
		}
	};

	public final double minV, maxV, scaleV;
	public final Identifier litBar, unlitBar;

	public int getProgress(double displayedSpeed) {
		if (displayedSpeed < this.minV) return 0;
		if (displayedSpeed > this.maxV) return -1;
		return (int)((displayedSpeed - this.minV) * this.scaleV);
	}

	private SpeedBar(double minV, double maxV, double scaleV, Identifier litBar, Identifier unlitBar) {
		this.minV = minV;
		this.maxV = maxV;
		this.scaleV = scaleV;
		this.litBar = litBar;
		this.unlitBar = unlitBar;
	}
}
