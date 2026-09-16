package com.tankwar.core;

import com.tankwar.constant.ScoreReason;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * 计分与最高分管理器。
 * <p>所有分数变更必须经 {@link #addScore}（携带原因枚举，便于日志与结算）；
 * 最高分通过 {@link Preferences} 持久化到系统用户目录，重启游戏依然保留。</p>
 *
 * @author TankWar Team
 */
public class ScoreManager {

    /** 持久化节点。 */
    private final Preferences prefs = Preferences.userRoot().node("com/tankwar");
    /** 最高分键。 */
    private static final String KEY_HIGH_SCORE = "highScore";

    /** 各玩家当前分数。 */
    private final Map<Integer, Integer> scores =
            new ConcurrentHashMap<Integer, Integer>();

    /** 清零全部玩家分数（新一局开始）。 */
    public void reset() {
        scores.clear();
    }

    /**
     * 增加分数。
     *
     * @param playerId 玩家编号
     * @param delta    增量
     * @param reason   计分原因
     */
    public void addScore(int playerId, int delta, ScoreReason reason) {
        Integer old = scores.get(playerId);
        int value = (old == null ? 0 : old) + delta;
        scores.put(playerId, value);
        System.out.println("[SCORE] 玩家" + playerId + " +" + delta
                + "（" + reason.getText() + "） => " + value);
    }

    /**
     * 查询分数。
     *
     * @param playerId 玩家编号
     * @return 分数
     */
    public int getScore(int playerId) {
        Integer value = scores.get(playerId);
        return value == null ? 0 : value;
    }

    /**
     * 读取历史最高分。
     *
     * @return 最高分
     */
    public int getHighScore() {
        return prefs.getInt(KEY_HIGH_SCORE, 0);
    }

    /**
     * 结算最高分：若本局总分超过纪录则更新。
     *
     * @param totalScore 本局总分
     * @return 是否刷新了纪录
     */
    public boolean commitHighScore(int totalScore) {
        if (totalScore > getHighScore()) {
            prefs.putInt(KEY_HIGH_SCORE, totalScore);
            return true;
        }
        return false;
    }
}
