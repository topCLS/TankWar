package com.tankwar.constant;

/**
 * 游戏宏观状态标识。
 * <p>状态切换的真正逻辑由 {@code core.state} 包中的状态模式实现，
 * 此枚举仅用于标识与日志，状态类内部不会出现 switch(state) 分支。</p>
 *
 * @author TankWar Team
 */
public enum GameStateEnum {
    /** 主菜单（选择模式 / 难度）。 */
    READY,
    /** 对战进行中。 */
    RUNNING,
    /** 暂停。 */
    PAUSED,
    /** 单关通过。 */
    LEVEL_CLEAR,
    /** 全局结束（胜利或失败）。 */
    GAME_OVER
}
