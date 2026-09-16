package com.tankwar.input;

/**
 * 离散操作意图。
 * <p>持续移动不通过本类（由 {@link InputManager} 的按住方向队列表达），
 * 本类只承载"边沿触发"的一次性操作：开火、暂停、确认、菜单导航等。
 * 意图在 EDT 产生、入队后由游戏逻辑线程消费，实现线程解耦。</p>
 *
 * @author TankWar Team
 */
public class Intent {

    /** 意图种类。 */
    public enum Kind {
        /** 玩家开火（playerId 区分 1/2）。 */
        FIRE,
        /** 暂停 / 继续。 */
        PAUSE_TOGGLE,
        /** 菜单确认 / 开始。 */
        CONFIRM,
        /** 重开当前关。 */
        RESTART,
        /** 静音切换。 */
        MUTE_TOGGLE,
        /** 菜单光标上。 */
        MENU_UP,
        /** 菜单光标下。 */
        MENU_DOWN,
        /** 菜单选项左切。 */
        MENU_LEFT,
        /** 菜单选项右切。 */
        MENU_RIGHT,
        /** 快捷选择单人模式。 */
        MODE_SOLO,
        /** 快捷选择双人合作。 */
        MODE_COOP,
        /** 快捷选择双人对战。 */
        MODE_VERSUS,
        /** 返回主菜单。 */
        QUIT_TO_MENU
    }

    /** 种类。 */
    private final Kind kind;
    /** 关联玩家（0 表示无、1/2 为玩家编号）。 */
    private final int playerId;

    /**
     * 构造意图。
     *
     * @param kind     种类
     * @param playerId 关联玩家
     */
    public Intent(Kind kind, int playerId) {
        this.kind = kind;
        this.playerId = playerId;
    }

    /**
     * 便捷工厂。
     *
     * @param kind 种类
     * @return 意图
     */
    public static Intent of(Kind kind) {
        return new Intent(kind, 0);
    }

    /** @return 种类。 */
    public Kind getKind() {
        return kind;
    }

    /** @return 关联玩家。 */
    public int getPlayerId() {
        return playerId;
    }
}
