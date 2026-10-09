package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
/** Shared colors and list geometry for both conflict-selection layouts. */
public final class ConflictSelectionAppearance
{
    private ConflictSelectionAppearance() {}

    public record Palette(int base, int selected, int highlight, int alpha,
            boolean alternating, int alternateLighten)
    {
        public int color(Integer custom, int index, boolean isSelected, boolean isPressed)
        {
            int rgb = custom == null ? this.base : custom;
            if (isSelected) rgb = isPressed ? this.highlight : this.selected;
            else if (this.alternating && (index & 1) == 1) rgb = lighten(rgb, this.alternateLighten);
            return this.withAlpha(rgb);
        }

        public int withAlpha(int rgb)
        {
            return (Math.clamp(this.alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
        }

        public static int textColor(int background)
        {
            int brightness = (((background >> 16) & 255) * 299 +
                    ((background >> 8) & 255) * 587 + (background & 255) * 114) / 1000;
            return brightness >= 160 ? 0xFF181818 : 0xFFF0F0F0;
        }

        private static int lighten(int rgb, int amount)
        {
            int red = Math.clamp(((rgb >> 16) & 255) + amount, 0, 255);
            int green = Math.clamp(((rgb >> 8) & 255) + amount, 0, 255);
            int blue = Math.clamp((rgb & 255) + amount, 0, 255);
            return (red << 16) | (green << 8) | blue;
        }
    }

    public record ListLayout(int left, int top, int width, int height, int rowHeight,
            int rowStep, int padding, int visibleRows, int expansion, float textScale)
    {
        public static ListLayout create(int screenWidth, int screenHeight, int count,
                double configuredScale, double selectedExpansion, double animation)
        {
            double scale = Math.clamp(configuredScale, 0.2D, 1.0D) / 0.6D;
            double expansion = Math.clamp(selectedExpansion, 1.0D, 2.0D);
            int baseWidth = Math.max(220, screenWidth / 3);
            // Reserve enough space for the selected row to expand without clipping.
            scale = Math.min(scale, Math.max(1, screenWidth - 24) / (baseWidth * expansion));
            int finalRowStep = Math.max(2, (int) Math.round(25 * scale));
            int finalPadding = Math.max(1, (int) Math.round(4 * scale));
            int visible = Math.min(count, Math.max(1,
                    (screenHeight - 24 - 2 * finalPadding) / finalRowStep));
            double displayScale = scale * Math.clamp(animation, 0.05D, 1.0D);
            int width = Math.max(1, (int) Math.round(baseWidth * displayScale));
            int padding = Math.max(1, (int) Math.round(4 * displayScale));
            int rowHeight = Math.max(1, (int) Math.round(22 * displayScale));
            int rowStep = rowHeight + Math.max(1, (int) Math.round(3 * displayScale));
            int height = 2 * padding + visible * rowHeight + Math.max(0, visible - 1) * (rowStep - rowHeight);
            int extra = Math.max(0, (int) Math.round(width * (expansion - 1) / 2));
            return new ListLayout((screenWidth - width) / 2, (screenHeight - height) / 2,
                    width, height, rowHeight, rowStep, padding, visible, extra, (float) displayScale);
        }

        public int rowTop(int row)
        {
            return this.top + this.padding + row * this.rowStep;
        }

        public int indexAt(double mouseX, double mouseY, int scroll, int selected)
        {
            if (mouseY < this.rowTop(0)) return -1;
            int row = (int) ((mouseY - this.rowTop(0)) / this.rowStep);
            if (row >= this.visibleRows || mouseY >= this.rowTop(row) + this.rowHeight) return -1;
            int index = scroll + row;
            int extra = index == selected ? this.expansion : 0;
            return mouseX >= this.left - extra && mouseX < this.left + this.width + extra ? index : -1;
        }
    }
}
//#endif
