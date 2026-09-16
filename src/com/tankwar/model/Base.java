package com.tankwar.model;

import com.tankwar.constant.GameConfig;
import com.tankwar.util.SpriteCache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 基地（鹰巢）：玩家方必须守护的目标，被任意敌方子弹命中即游戏失败。
 *
 * @author TankWar Team
 */
public class Base extends GameObject {

    /** 是否已被摧毁。 */
    private boolean destroyed = false;
    /** 所在格列。 */
    private final int cellCol;
    /** 所在格行。 */
    private final int cellRow;

    /**
     * 在指定格子上构造基地（居中放置 28px 图像）。
     *
     * @param col 格列
     * @param row 格行
     */
    public Base(int col, int row) {
        super(col * GameConfig.TILE_SIZE
                        + (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2,
                row * GameConfig.TILE_SIZE
                        + (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2,
                GameConfig.TANK_SIZE, GameConfig.TANK_SIZE);
        this.cellCol = col;
        this.cellRow = row;
    }

    /** 标记基地被摧毁。 */
    public void destroy() {
        this.destroyed = true;
        this.alive = false;
    }

    /** @return 是否已被摧毁。 */
    public boolean isDestroyed() {
        return destroyed;
    }

    /** @return 所在格列。 */
    public int getCellCol() {
        return cellCol;
    }

    /** @return 所在格行。 */
    public int getCellRow() {
        return cellRow;
    }

    /**
     * 绘制：存活时绘制金色鹰徽；被摧毁后绘制焦黑废墟。
     */
    @Override
    public void draw(Graphics2D g) {
        BufferedImage eagle = SpriteCache.getInstance().get("base_eagle.png");
        if (!destroyed) {
            if (eagle != null) {
                g.drawImage(eagle, x, y, width, height, null);
            } else {
                g.setColor(new Color(230, 190, 70));
                g.fillOval(x + 2, y + 2, width - 4, height - 4);
                g.setColor(new Color(120, 80, 20));
                g.drawOval(x + 2, y + 2, width - 4, height - 4);
            }
        } else {
            drawRubble(g);
        }
    }

    /** 矢量绘制被毁后的废墟。 */
    private void drawRubble(Graphics2D g) {
        g.setColor(new Color(45, 40, 40));
        g.fillRect(x + 2, y + 8, width - 4, height - 10);
        g.setColor(new Color(80, 50, 40));
        g.fillRect(x + 5, y + 4, 7, 12);
        g.fillRect(x + width - 12, y + 6, 7, 10);
        g.setColor(new Color(30, 28, 28));
        g.fillRect(x + 10, y + 12, 8, 12);
        g.setColor(new Color(200, 70, 30));
        g.fillRect(x + 12, y + 16, 3, 3);
    }
}
