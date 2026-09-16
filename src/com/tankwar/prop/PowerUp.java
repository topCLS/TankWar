package com.tankwar.prop;

import com.tankwar.constant.GameConfig;
import com.tankwar.constant.PowerUpType;
import com.tankwar.model.GameObject;
import com.tankwar.model.PlayerTank;
import com.tankwar.util.SpriteCache;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 战场道具抽象基类。
 * <p>道具停留在地面等待玩家拾取，接近生命周期末尾时闪烁提示；
 * 子类通过模板方法 {@link #apply(PlayerTank, long)} /
 * {@link #expire(PlayerTank)} 实现各自的增益与到期还原，体现继承多态。</p>
 *
 * @author TankWar Team
 */
public abstract class PowerUp extends GameObject {

    /** 道具类型。 */
    protected final PowerUpType type;
    /** 生成时间。 */
    protected final long bornAt;
    /** 场上到期时间（超时消失）。 */
    protected final long expireAt;
    /** 所在格列。 */
    protected final int cellCol;
    /** 所在格行。 */
    protected final int cellRow;

    /**
     * 构造道具。
     *
     * @param type    类型
     * @param cellCol 格列
     * @param cellRow 格行
     * @param now     生成时间
     */
    protected PowerUp(PowerUpType type, int cellCol, int cellRow, long now) {
        super(cellCol * GameConfig.TILE_SIZE
                        + (GameConfig.TILE_SIZE - GameConfig.POWERUP_SIZE) / 2,
                cellRow * GameConfig.TILE_SIZE
                        + (GameConfig.TILE_SIZE - GameConfig.POWERUP_SIZE) / 2,
                GameConfig.POWERUP_SIZE, GameConfig.POWERUP_SIZE);
        this.type = type;
        this.cellCol = cellCol;
        this.cellRow = cellRow;
        this.bornAt = now;
        this.expireAt = now + GameConfig.POWERUP_LIFETIME_MS;
    }

    /** @return 类型。 */
    public PowerUpType getType() {
        return type;
    }

    /** @return 到期时间。 */
    public long getExpireAt() {
        return expireAt;
    }

    /**
     * 是否已经超时消失。
     *
     * @param now 当前时间
     * @return 超时返回 true
     */
    public boolean isExpiredFromField(long now) {
        return now >= expireAt;
    }

    /**
     * 对拾取坦克施加效果（模板方法，子类实现）。
     *
     * @param tank 拾取的玩家坦克
     * @param now  当前时间
     */
    public abstract void apply(PlayerTank tank, long now);

    /**
     * 效果到期时还原坦克状态（模板方法，子类实现）。
     *
     * @param tank 被还原的坦克
     */
    public abstract void expire(PlayerTank tank);

    /**
     * 统一绘制：图标 + 主题色光圈 + 临期闪烁。
     */
    @Override
    public void draw(Graphics2D g) {
        long now = System.currentTimeMillis();
        long remain = expireAt - now;
        // 最后 2.5 秒快速闪烁
        if (remain < 2500L && (now / 120) % 2 == 0) {
            return;
        }
        BufferedImage icon = SpriteCache.getInstance().get(type.getSprite());
        // 光圈
        g.setColor(new Color(type.getColor().getRed(), type.getColor().getGreen(),
                type.getColor().getBlue(), 70));
        g.fillOval(x - 4, y - 4, width + 8, height + 8);
        if (icon != null) {
            g.drawImage(icon, x, y, width, height, null);
        } else {
            g.setColor(type.getColor());
            g.fillRect(x, y, width, height);
        }
        // 边框
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width - 1, height - 1);
    }

    /** 兼容透明合成占位（防止个别环境残留 alpha）。 */
    protected static Graphics2D withAlpha(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        return g;
    }
}
