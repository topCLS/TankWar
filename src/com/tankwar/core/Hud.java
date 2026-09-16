package com.tankwar.core;

import com.tankwar.constant.GameConfig;
import com.tankwar.constant.GameMode;
import com.tankwar.constant.PowerUpType;
import com.tankwar.model.PlayerTank;
import com.tankwar.prop.EffectManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 战场右侧信息栏（HUD）。
 * <p>展示关卡、模式、难度、剩余敌方坦克点阵、各玩家分数与剩余生命、
 * 当前生效道具及其剩余时间进度条、FPS 与操作提示。纯绘制类，
 * 每帧由游戏状态调用，不保存任何可变状态。</p>
 *
 * @author TankWar Team
 */
public class Hud {

    private static final int X = GameConfig.FIELD_W;
    private static final int W = GameConfig.HUD_W;
    private static final Font TITLE_FONT =
            new Font(Font.SANS_SERIF, Font.BOLD, 17);
    private static final Font HEAD_FONT =
            new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font BODY_FONT =
            new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /**
     * 绘制整列 HUD。
     *
     * @param g       图形上下文
     * @param world   当前世界
     * @param context 游戏上下文
     */
    public void draw(Graphics2D g, World world, GameContext context) {
        g.setColor(new Color(18, 20, 28));
        g.fillRect(X, 0, W, GameConfig.FIELD_H);
        g.setColor(new Color(48, 54, 70));
        g.drawLine(X, 0, X, GameConfig.FIELD_H);

        int y = 18;
        g.setColor(new Color(255, 210, 90));
        g.setFont(TITLE_FONT);
        g.drawString("TANK WAR", X + 14, y);
        y += 24;

        g.setFont(BODY_FONT);
        g.setColor(new Color(200, 206, 216));
        g.drawString("第 " + world.getLevelIndex() + " / "
                + com.tankwar.map.LevelManager.LEVEL_COUNT + " 关", X + 16, y);
        y += 16;
        g.drawString(modeName(world.getMode()), X + 16, y);
        y += 16;
        g.drawString("难度:" + difficultyName(world.getDifficulty()), X + 16, y);
        y += 24;

        y = drawEnemyDots(g, world, y);
        for (PlayerTank p : world.getPlayers()) {
            y = drawPlayerBlock(g, context, world, p, y);
        }
        drawFooter(g, context, world);
    }

    /** 剩余敌人点阵。 */
    private int drawEnemyDots(Graphics2D g, World world, int y) {
        g.setFont(HEAD_FONT);
        g.setColor(new Color(235, 120, 110));
        g.drawString("剩余敌人", X + 16, y);
        y += 8;
        int remaining = world.getEnemiesRemaining();
        int cols = 6;
        int dot = 12;
        int gap = 6;
        int startX = X + 16;
        for (int i = 0; i < GameConfig.ENEMY_TOTAL_PER_LEVEL; i++) {
            int r = i / cols;
            int c = i % cols;
            int dx = startX + c * (dot + gap);
            int dy = y + r * (dot + gap);
            if (i < remaining) {
                drawTankIcon(g, dx, dy, dot, new Color(220, 90, 80));
            } else {
                g.setColor(new Color(40, 44, 56));
                g.fillRect(dx, dy, dot, dot);
            }
        }
        int rows = (GameConfig.ENEMY_TOTAL_PER_LEVEL + cols - 1) / cols;
        return y + rows * (dot + gap) + 10;
    }

    /** 单个玩家信息块（分数/生命/道具）。 */
    private int drawPlayerBlock(Graphics2D g, GameContext context, World world,
                                PlayerTank p, int y0) {
        int y = y0;
        Color color = p.getColor();
        g.setColor(color);
        g.setFont(HEAD_FONT);
        g.drawString("玩家 " + p.getPlayerId(), X + 16, y);
        y += 16;

        g.setFont(BODY_FONT);
        g.setColor(new Color(220, 226, 236));
        g.drawString("分数 " + context.getScoreManager().getScore(
                p.getPlayerId()), X + 16, y);
        y += 15;

        g.setColor(new Color(180, 186, 196));
        g.drawString("生命", X + 16, y);
        for (int i = 0; i < p.getLives(); i++) {
            drawTankIcon(g, X + 52 + i * 16, y - 10, 12, color);
        }
        y += 18;

        EffectManager fx = world.getEffects();
        long nowMs = System.currentTimeMillis();
        for (PowerUpType type : fx.activeTypes(p.getPlayerId())) {
            long rem = fx.remainingMillis(p.getPlayerId(), type, nowMs);
            long dur = type.getDurationMs();
            g.setColor(new Color(170, 178, 192));
            g.drawString(type.getDisplayName(), X + 16, y);
            int barX = X + 64;
            int barW = 64;
            g.setColor(new Color(48, 52, 64));
            g.fillRect(barX, y - 9, barW, 6);
            g.setColor(type.getColor());
            int fillW = (int) (barW * Math.max(0, rem) * 1.0 / dur);
            g.fillRect(barX, y - 9, fillW, 6);
            y += 15;
        }
        return y + 8;
    }

    /** 绘制小坦克图标（炮塔朝上调色用）。 */
    private void drawTankIcon(Graphics2D g, int x, int y, int s, Color color) {
        g.setColor(color);
        g.fillRect(x + s / 2 - 1, y, 2, s / 2);
        g.fillRect(x + 1, y + s / 4, s - 2, s - 3);
        g.setColor(color.darker());
        g.fillRect(x, y + s / 2, 3, s / 2);
        g.fillRect(x + s - 3, y + s / 2, 3, s / 2);
    }

    /** 底部 FPS 与操作提示。 */
    private void drawFooter(Graphics2D g, GameContext context, World world) {
        int y = GameConfig.FIELD_H - 86;
        g.setColor(new Color(120, 128, 144));
        g.setFont(BODY_FONT);
        g.drawString("FPS " + context.getFps(), X + 16, y);
        y += 22;
        if (world.getMode() == GameMode.SOLO) {
            g.drawString("WASD 移动", X + 16, y);
            y += 14;
            g.drawString("J 开火", X + 16, y);
            y += 14;
        } else {
            g.drawString("P1 WASD/J", X + 16, y);
            y += 14;
            g.drawString("P2 方向/空格", X + 16, y);
            y += 14;
        }
        g.drawString("P 暂停  M 静音", X + 16, y);
        y += 14;
        g.drawString("R 重开  Q 菜单", X + 16, y);
    }

    private String modeName(GameMode mode) {
        switch (mode) {
            case COOP:
                return "双人合作";
            case VERSUS:
                return "双人对战";
            default:
                return "单人闯关";
        }
    }

    private String difficultyName(com.tankwar.constant.Difficulty d) {
        switch (d) {
            case EASY:
                return "简单";
            case HARD:
                return "困难";
            default:
                return "普通";
        }
    }
}
