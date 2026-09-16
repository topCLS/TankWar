package com.tankwar.constant;

/**
 * 游戏模式枚举。
 *
 * @author TankWar Team
 */
public enum GameMode {
    /** 单人闯关：1 名玩家协作守卫基地、对抗 AI。 */
    SOLO("单人闯关", 1, false),
    /** 双人合作：2 名玩家共同守卫基地、对抗 AI。 */
    COOP("双人合作", 2, false),
    /** 双人对战：2 名玩家同屏竞技，无 AI，先击毁对方全部生命者胜。 */
    VERSUS("双人对战", 2, true);

    /** 中文名。 */
    private final String displayName;
    /** 参与玩家数量。 */
    private final int playerCount;
    /** 是否为玩家对战模式。 */
    private final boolean versus;

    GameMode(String displayName, int playerCount, boolean versus) {
        this.displayName = displayName;
        this.playerCount = playerCount;
        this.versus = versus;
    }

    /** @return 中文名。 */
    public String getDisplayName() {
        return displayName;
    }

    /** @return 参与玩家数。 */
    public int getPlayerCount() {
        return playerCount;
    }

    /** @return 是否对战模式。 */
    public boolean isVersus() {
        return versus;
    }
}
