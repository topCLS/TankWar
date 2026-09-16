package com.tankwar.prop;

import com.tankwar.constant.PowerUpType;
import com.tankwar.model.PlayerTank;

/**
 * 穿甲弹道具：子弹威力提升到 2，可击穿钢墙，到期还原。
 *
 * @author TankWar Team
 */
public class PowerBullet extends PowerUp {

    /**
     * 构造穿甲弹道具。
     *
     * @param cellCol 格列
     * @param cellRow 格行
     * @param now     生成时间
     */
    public PowerBullet(int cellCol, int cellRow, long now) {
        super(PowerUpType.POWER_BULLET, cellCol, cellRow, now);
    }

    /** 威力提升为 2（可破钢墙）。 */
    @Override
    public void apply(PlayerTank tank, long now) {
        tank.setBulletPower(2);
    }

    /** 到期威力还原为 1。 */
    @Override
    public void expire(PlayerTank tank) {
        tank.setBulletPower(1);
    }
}
