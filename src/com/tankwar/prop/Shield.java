package com.tankwar.prop;

import com.tankwar.constant.PowerUpType;
import com.tankwar.model.PlayerTank;

/**
 * 护盾道具：持续期间免疫一切伤害（蓝色光环），到期解除。
 *
 * @author TankWar Team
 */
public class Shield extends PowerUp {

    /**
     * 构造护盾道具。
     *
     * @param cellCol 格列
     * @param cellRow 格行
     * @param now     生成时间
     */
    public Shield(int cellCol, int cellRow, long now) {
        super(PowerUpType.SHIELD, cellCol, cellRow, now);
    }

    /** 开启护盾。 */
    @Override
    public void apply(PlayerTank tank, long now) {
        tank.setShield(true);
    }

    /** 到期关闭护盾。 */
    @Override
    public void expire(PlayerTank tank) {
        tank.setShield(false);
    }
}
