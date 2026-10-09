package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.halfmasa.xaerobinding.compat.InputCompat;

public final class KeymapKeyboardLayout
{
    public static final int CELL_HEIGHT = 16;
    public static final int CELL_GAP = 2;
    public static final int ROWS = 6;
    public static final int HEIGHT = ROWS * (CELL_HEIGHT + CELL_GAP) + 8;
    public static final int HEIGHT_122 = (ROWS + 1) * (CELL_HEIGHT + CELL_GAP) + 8;

    private KeymapKeyboardLayout() {}

    public static List<Key> keys(int x, int y, int width)
    {
        List<Key> result = new ArrayList<>();
        int gap = 12;
        int mainWidth = (int) ((width - 2 * gap) * 0.635D);
        int clusterWidth = (int) ((width - 2 * gap) * 0.155D);
        int numpadWidth = width - 2 * gap - mainWidth - clusterWidth;
        int numpadX = x + width - numpadWidth;
        int navX = numpadX - gap - clusterWidth;
        int rowStep = CELL_HEIGHT + CELL_GAP;

        String[][] mainRows = {
                {"ESC:256:1", "_:0:1", "F1:290:1", "F2:291:1", "F3:292:1", "F4:293:1", "_:0:0.5",
                        "F5:294:1", "F6:295:1", "F7:296:1", "F8:297:1", "_:0:0.5",
                        "F9:298:1", "F10:299:1", "F11:300:1", "F12:301:1"},
                {"`:96:1", "1:49:1", "2:50:1", "3:51:1", "4:52:1", "5:53:1", "6:54:1", "7:55:1",
                        "8:56:1", "9:57:1", "0:48:1", "-:45:1", "=:61:1", "⌫:259:2"},
                {"TAB:258:1.5", "Q:81:1", "W:87:1", "E:69:1", "R:82:1", "T:84:1", "Y:89:1", "U:85:1",
                        "I:73:1", "O:79:1", "P:80:1", "[:91:1", "]:93:1", "\\:92:1.5"},
                {"CAPS:280:1.75", "A:65:1", "S:83:1", "D:68:1", "F:70:1", "G:71:1", "H:72:1", "J:74:1",
                        "K:75:1", "L:76:1", ";:59:1", "':39:1", "ENTER:257:2.25"},
                {"⇧:340:2.25", "Z:90:1", "X:88:1", "C:67:1", "V:86:1", "B:66:1", "N:78:1", "M:77:1",
                        ",:44:1", ".:46:1", "/:47:1", "⇧:344:2.75"},
                {"CTRL:341:1.25", "WIN:343:1.25", "ALT:342:1.25", "　:32:6.25", "ALT:346:1.25",
                        "WIN:347:1.25", "MENU:348:1.25", "CTRL:345:1.25"}
        };
        for (int row = 0; row < mainRows.length; row++)
        {
            addRow(result, mainRows[row], x, y + row * rowStep, mainWidth, 15.0F);
        }

        addRow(result, new String[] {"PRT:283:1", "SCR:281:1", "PAU:284:1"},
                navX, y, clusterWidth, 3.0F);
        String[][] navRows = {
                {"INS:260:1", "HOM:268:1", "PGU:266:1"},
                {"DEL:261:1", "END:269:1", "PGD:267:1"},
                {"_:0:1", "↑:264:1", "_:0:1"},
                {"←:263:1", "↓:265:1", "→:262:1"}
        };
        for (int row = 0; row < navRows.length; row++)
        {
            addRow(result, navRows[row], navX, y + (row + 2) * rowStep, clusterWidth, 3.0F);
        }

        String[][] numpadRows = {
                {"NUM:282:1", "/:331:1", "*:332:1", "-:333:1"},
                {"7:327:1", "8:328:1", "9:329:1", "+:334:1:2"},
                {"4:324:1", "5:325:1", "6:326:1", "_:0:1"},
                {"1:321:1", "2:322:1", "3:323:1", "⏎:335:1:2"},
                {"0:320:2", ".:330:1", "_:0:1"}
        };
        for (int row = 0; row < numpadRows.length; row++)
        {
            addRow(result, numpadRows[row], numpadX, y + row * rowStep, numpadWidth, 4.0F);
        }
        addRow(result, new String[] {"L:-1:1", "M:-3:1", "R:-2:1"},
                numpadX, y + 5 * rowStep, numpadWidth, 3.0F);
        return result;
    }

    /** A PC/5250-style layout: the usual 104 keys, F13-F24, and six terminal keys. */
    public static List<Key> keys122(int x, int y, int width)
    {
        int rowStep = CELL_HEIGHT + CELL_GAP;
        int terminalWidth = Math.max(30, width / 20);
        int mainX = x + terminalWidth + 6;
        int remainingWidth = width - terminalWidth - 6;
        List<Key> result = keys(mainX, y + rowStep, remainingWidth);

        int gap = 12;
        int mainWidth = (int) ((remainingWidth - 2 * gap) * 0.635D);
        String[] extraFunctions = new String[12];
        for (int index = 0; index < extraFunctions.length; index++)
        {
            int number = index + 13;
            extraFunctions[index] = "F" + number + ":" + (302 + index) + ":1";
        }
        addRow(result, extraFunctions, mainX, y, mainWidth, 12.0F);

        addTerminalKey(result, "SYSR", x, y + rowStep, terminalWidth, 340, 256);
        addTerminalKey(result, "REC", x, y + 2 * rowStep, terminalWidth, 341, 340, 51);
        addTerminalKey(result, "PLAY", x, y + 3 * rowStep, terminalWidth, 341, 340, 52);
        addTerminalKey(result, "HELP", x, y + 4 * rowStep, terminalWidth, 342, 290);
        addTerminalKey(result, "ZOOM", x, y + 5 * rowStep, terminalWidth, 342, 260);
        addTerminalKey(result, "RULE", x, y + 6 * rowStep, terminalWidth, 342, 267);
        return result;
    }

    private static void addTerminalKey(List<Key> result, String label, int x, int y, int width,
            int... legacyCodes)
    {
        List<Integer> codes = new ArrayList<>(legacyCodes.length);
        for (int code : legacyCodes)
        {
            codes.add(InputCompat.layoutKeyCode(code));
        }
        result.add(new Key(codes.get(codes.size() - 1), label, x, y, width, CELL_HEIGHT, false,
                List.copyOf(codes)));
    }

    private static void addRow(List<Key> result, String[] cells, int x, int y, int width, float totalUnits)
    {
        float unit = width / totalUnits;
        //#if MC >= 26.3
        float usedUnits = 0.0F;
        //#else
        //$$ float cellX = x;
        //#endif
        for (String cell : cells)
        {
            String[] parts = cell.split(":");
            int code = InputCompat.layoutKeyCode(Integer.parseInt(parts[1]));
            float units = Float.parseFloat(parts[2]);
            //#if MC >= 26.3
            int cellX = x + Math.round(unit * usedUnits);
            int cellRight = x + Math.round(unit * (usedUnits + units)) - CELL_GAP;
            int cellWidth = Math.max(1, cellRight - cellX);
            //#else
            //$$ int cellWidth = (int) (unit * units) - CELL_GAP;
            //#endif
            int cellHeight = parts.length > 3 && "2".equals(parts[3])
                    ? 2 * CELL_HEIGHT + CELL_GAP : CELL_HEIGHT;
            if (code != 0)
            {
                //#if MC >= 26.3
                result.add(new Key(code, parts[0], cellX, y, cellWidth, cellHeight, code < 0));
                //#else
                //$$ result.add(new Key(code, parts[0], (int) cellX, y, cellWidth, cellHeight, code < 0));
                //#endif
            }
            //#if MC >= 26.3
            usedUnits += units;
            //#else
            //$$ cellX += cellWidth + CELL_GAP;
            //#endif
        }
    }

    public record Key(int code, String label, int x, int y, int width, int height, boolean mouse,
            List<Integer> codes)
    {
        //#if MC >= 26.3
        public boolean contains(double mouseX, double mouseY)
        {
            return mouseX >= this.x && mouseX < this.x + this.width &&
                    mouseY >= this.y && mouseY < this.y + this.height;
        }
        //#endif
        public Key(int code, String label, int x, int y, int width, int height, boolean mouse)
        {
            this(code, label, x, y, width, height, mouse, List.of(code));
        }
    }
}
