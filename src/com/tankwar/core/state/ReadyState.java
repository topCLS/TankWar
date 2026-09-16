package com.tankwar.core.state;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.GameMode;
import com.tankwar.core.GameContext;
import com.tankwar.input.Intent;
import com.tankwar.util.SpriteCache;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 主菜单 / 准备状态：选择游戏模式与难度，确认后开局。
 * <p>背景使用 AI 生成的战场夜景图，叠加像素风标题与选项面板；
 * 支持方向键/WASD 导航、Enter 确认、数字键 1/2/3 快捷选模式。</p>
 *
 * @author TankWar Team
 */
public class ReadyState implements GameState {

    private static final GameMode[] MODES = GameMode.values();
    private static final Difficulty[] DIFFICULTIES = Difficulty.values();

    /** 当前选中行（0 模式、1 难度）。 */
    private int selectedRow = 0;
    /** 模式下标。 */
    private int modeIndex;
    /** 难度下标。 */
    private int difficultyIndex;

    @Override
    public void onEnter(GameContext context) {
        this.modeIndex = context.getMode().ordinal();
        this.difficultyIndex = context.getDifficulty().ordinal();
    }

    @Override
    public void handleInput(GameContext context, List<Intent> intents) {
        for (Intent intent : intents) {
            switch (intent.getKind()) {
                case MENU_UP:
                    selectedRow = Math.floorMod(selectedRow - 1, 2);
                    break;
                case MENU_DOWN:
                    selectedRow = Math.floorMod(selectedRow + 1, 2);
                    break;
                case MENU_LEFT:
                    shift(-1);
                    break;
                case MENU_RIGHT:
                    shift(1);
                    break;
                case CONFIRM:
                    start(context);
                    break;
                case MODE_SOLO:
                    modeIndex = 0;
                    start(context);
                    break;
                case MODE_COOP:
                    modeIndex = 1;
                    start(context);
                    break;
                case MODE_VERSUS:
                    modeIndex = 2;
                    start(context);
                    break;
                default:
                    break;
            }
        }
    }

    /** 当前行左右切换选项。 */
    private void shift(int delta) {
        if (selectedRow == 0) {
            modeIndex = Math.floorMod(modeIndex + delta, MODES.length);
        } else {
            difficultyIndex = Math.floorMod(
                    difficultyIndex + delta, DIFFICULTIES.length);
        }
    }

    /** 按当前选择开局。 */
    private void start(GameContext context) {
        context.configureAndStart(MODES[modeIndex], DIFFICULTIES[difficultyIndex]);
    }

    @Override
    public void update(GameContext context, long dtMs) {
        // 纯静态菜单，无逻辑推进
    }

    @Override
    public void render(GameContext context, Graphics2D g) {
        BufferedImage bg = SpriteCache.getInstance().get("title_bg.png");
        if (bg != null) {
            g.drawImage(bg, 0, 0, GameConfig.WINDOW_W,
                    GameConfig.WINDOW_H, null);
        } else {
            g.setColor(new Color(18, 20, 34));
            g.fillRect(0, 0, GameConfig.WINDOW_W, GameConfig.WINDOW_H);
        }
        g.setColor(new Color(6, 8, 16, 188));
        g.fillRect(0, 0, GameConfig.WINDOW_W, GameConfig.WINDOW_H);

        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 62);
        Font subFont = new Font(Font.MONOSPACED, Font.BOLD, 24);
        g.setFont(titleFont);
        drawCentered(g, "坦 克 大 战", GameConfig.WINDOW_W / 2, 150,
                new Color(255, 206, 70), new Color(60, 40, 0));
        g.setFont(subFont);
        drawCentered(g, "T A N K   W A R", GameConfig.WINDOW_W / 2, 196,
                new Color(235, 240, 250), null);

        // 选项面板
        int panelX = 238;
        int panelY = 250;
        int panelW = 500;
        int panelH = 300;
        g.setColor(new Color(14, 18, 30, 220));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g.setColor(new Color(90, 110, 150));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 22);
        Font optionFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);

        drawOptionRow(g, panelX, panelY + 56, "模式", labelFont, optionFont,
                names(MODES), modeIndex, selectedRow == 0);
        drawOptionRow(g, panelX, panelY + 126, "难度", labelFont, optionFont,
                names(DIFFICULTIES), difficultyIndex, selectedRow == 1);

        Font btnFont = new Font(Font.SANS_SERIF, Font.BOLD, 24);
        g.setFont(btnFont);
        drawCentered(g, "▶ 按 Enter 开始游戏 ◀", GameConfig.WINDOW_W / 2,
                panelY + 215, new Color(120, 230, 140), null);

        Font small = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        g.setFont(small);
        drawCentered(g, "方向键/WASD 选择    数字键 1/2/3 快捷选模式",
                GameConfig.WINDOW_W / 2, panelY + 258,
                new Color(170, 180, 200), null);

        // 最高分与操作说明
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        drawCentered(g, "最高分  " + context.getScoreManager().getHighScore(),
                GameConfig.WINDOW_W / 2, 600, new Color(255, 220, 120), null);

        g.setFont(small);
        drawCentered(g, "玩家1：W A S D 移动  J 开火      玩家2：方向键移动  空格开火",
                GameConfig.WINDOW_W / 2, 668, new Color(190, 198, 214), null);
        drawCentered(g, "P 暂停    M 静音    R 重开本关    Q 返回菜单",
                GameConfig.WINDOW_W / 2, 696, new Color(150, 158, 178), null);
        drawCentered(g, "守卫金鹰基地，击毁全部 20 辆敌方坦克即可过关",
                GameConfig.WINDOW_W / 2, 736, new Color(140, 160, 190), null);
    }

    /** 绘制一行（标签 + 多个横排选项，当前行高亮）。 */
    private void drawOptionRow(Graphics2D g, int panelX, int y, String label,
                               Font labelFont, Font optionFont,
                               String[] optionNames, int selected, boolean active) {
        if (active) {
            g.setColor(new Color(255, 210, 90, 40));
            g.fillRoundRect(panelX + 16, y - 24, 468, 40, 8, 8);
        }
        g.setFont(labelFont);
        g.setColor(active ? new Color(255, 214, 90) : new Color(180, 188, 204));
        g.drawString(label, panelX + 44, y);
        g.setFont(optionFont);
        int[] xs = {panelX + 190, panelX + 300, panelX + 410};
        for (int i = 0; i < optionNames.length; i++) {
            boolean chosen = i == selected;
            Color color = chosen ? new Color(110, 220, 140)
                    : new Color(150, 158, 176);
            String text = (chosen && active ? "[" : " ") + optionNames[i]
                    + (chosen && active ? "]" : " ");
            g.setColor(color);
            g.drawString(text, xs[i], y);
        }
    }

    /** 枚举显示名数组。 */
    private String[] names(Enum<?>[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof GameMode) {
                result[i] = ((GameMode) values[i]).getDisplayName();
            } else {
                result[i] = ((Difficulty) values[i]).getDisplayName();
            }
        }
        return result;
    }

    /** 居中绘制文字（可选深色描边）。 */
    static void drawCentered(Graphics2D g, String text, int centerX, int y,
                             Color color, Color outline) {
        FontMetrics fm = g.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        if (outline != null) {
            g.setColor(outline);
            g.drawString(text, x - 2, y);
            g.drawString(text, x + 2, y);
            g.drawString(text, x, y - 2);
            g.drawString(text, x, y + 2);
        }
        g.setColor(color);
        g.drawString(text, x, y);
    }

    @Override
    public void onExit(GameContext context) {
        // 无需清理
    }
}
