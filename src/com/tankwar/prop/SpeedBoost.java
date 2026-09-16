package com.tankwar.prop;

import com.tankwar.constant.GameConfig;
import com.tankwar.constant.PowerUpType;
import com.tankwar.model.PlayerTank;

/**
 * 加速道具：移动速度提升至 1.6 倍（向上取整），到期恢复初始速度。
 *
 * @author TankWar Team
 */
public class SpeedBoost extends PowerUp {

    /**
     * 构造加速道具。
     *
     * @param cellCol 格列
     * @param cellRow 格行
     * @param now     生成时间
     */
    public SpeedBoost(int cellCol, int cellRow, long now) {
        super(PowerUpType.SPEED_BOOST, cellCol, cellRow, now);
    }

    /** 速度提升到 1.6 倍（2 -> 3 像素/步）。 */
    @Override
    public void apply(PlayerTank tank, long now) {
        int boosted = (int) Math.round(GameConfig.PLAYER_SPEED * 1.6);
        tank.setSpeed(boosted);
    }

    /** 到期恢复玩家基础速度。 */
    @Override
    public void expire(PlayerTank tank) {
        tank.setSpeed(GameConfig.PLAYER_SPEED);
    }
}
