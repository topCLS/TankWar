package com.tankwar.render;

import com.tankwar.constant.GameConfig;
import com.tankwar.model.interfaces.Drawable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 敌方出生传送标记：在出生点播放约 0.8 秒的旋转星环，播完后由
 * {@code core.World} 真正生成坦克，给玩家反应时间。
 *
 * @author TankWar Team
 */
public class SpawnMarker implements Drawable {

    /** 出生格列。 */
    private final int cellCol;
    /** 出生格行。 */
    private final int cellRow;
    /** 开始时间。 */
    private final long startAt;
    /** 持续时间。 */
    private final long durationMs;
    /** 是否存活。 */
    private boolean alive = true;

    /**
     * 构造出生标记。
     *
     * @param cellCol 格列
     * @param cellRow 格行
     * @param startAt 开始时间
     */
    public SpawnMarker(int cellCol, int cellRow, long startAt) {
        this.cellCol = cellCol;
        this.cellRow = cellRow;
        this.startAt = startAt;
        this.durationMs = GameConfig.SPAWN_FLASH_MS;
    }

    /**
     * 是否到了真正生成坦克的时刻。
     *
     * @param now 当前时间
     * @return 到期返回 true，并标记为结束
     */
    public boolean isDue(long now) {
        if (now - startAt >= durationMs) {
            alive = false;
            return true;
        }
        return false;
    }

    /** @return 是否存活。 */
    public boolean isAlive() {
        return alive;
    }

    /** @return 开始时间。 */
    public long getStartAt() {
        return startAt;
    }

    /** @return 格列。 */
    public int getCellCol() {
        return cellCol;
    }

    /** @return 格行。 */
    public int getCellRow() {
        return cellRow;
    }

    /** 绘制旋转星环（随时间快速闪烁、四角射线旋转）。 */
    @Override
    public void draw(Graphics2D g) {
        long now = System.currentTimeMillis();
        long t = now - startAt;
        if (t < 0 || t > durationMs) {
            return;
        }
        int cx = cellCol * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        int cy = cellRow * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        int radius = 10;
        double angle = (t / 60.0);
        g.setColor((t / 90) % 2 == 0 ? Color.WHITE : new Color(255, 120, 60));
        g.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < 4; i++) {
            double a = angle + i * Math.PI / 2.0;
            int x1 = cx + (int) (Math.cos(a) * 3);
            int y1 = cy + (int) (Math.sin(a) * 3);
            int x2 = cx + (int) (Math.cos(a) * radius);
            int y2 = cy + (int) (Math.sin(a) * radius);
            g.drawLine(x1, y1, x2, y2);
        }
        g.setColor(new Color(255, 255, 255, 120));
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }
}
