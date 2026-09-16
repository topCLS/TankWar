package com.tankwar.ai;

/**
 * 敌方坦克行为状态。
 *
 * @author TankWar Team
 */
public enum AIState {
    /** 巡逻：沿随机方向直线行进，周期性或撞墙时换向。 */
    PATROL,
    /** 追踪：发现目标后沿网格路径逼近。 */
    CHASE,
    /** 攻击：与目标同行/同列且无遮挡，转向开火。 */
    ATTACK
}
