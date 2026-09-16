package com.tankwar.map;

import com.tankwar.constant.TankType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 关卡管理器：维护关卡序号、关卡数据与每关敌方坦克编成。
 *
 * @author TankWar Team
 */
public class LevelManager {

    /** 总关卡数。 */
    public static final int LEVEL_COUNT = 3;

    /** 当前关卡序号（从 1 开始）。 */
    private int currentLevel = 1;

    /** @return 当前关卡序号。 */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /** @return 是否还有下一关。 */
    public boolean hasNext() {
        return currentLevel < LEVEL_COUNT;
    }

    /**
     * 推进到下一关。
     *
     * @return 推进后的关卡序号
     */
    public int nextLevel() {
        if (hasNext()) {
            currentLevel++;
        }
        return currentLevel;
    }

    /** 重置到第一关。 */
    public void reset() {
        currentLevel = 1;
    }

    /**
     * 直接设置关卡序号（重开当前关时使用）。
     *
     * @param level 关卡序号
     */
    public void setLevel(int level) {
        this.currentLevel = Math.max(1, Math.min(LEVEL_COUNT, level));
    }

    /**
     * 加载当前关卡数据。
     *
     * @return 关卡数据
     */
    public LevelData loadCurrent() {
        return MapLoader.load(currentLevel);
    }

    /**
     * 生成指定关卡的敌方坦克出场队列（共 20 辆，含 4 辆必掉道具的奖励坦克）。
     * <p>使用以关卡号为种子的伪随机，保证同一关每次排布一致、便于测试复现；
     * 关卡越高，快速坦克与重装甲坦克比例越大。</p>
     *
     * @param level 关卡序号
     * @return 不可修改的坦克类型队列
     */
    public List<TankType> enemyPlan(int level) {
        List<TankType> plan = new ArrayList<TankType>();
        int basic;
        int fast;
        int armored;
        if (level <= 1) {
            basic = 10;
            fast = 4;
            armored = 2;
        } else if (level == 2) {
            basic = 6;
            fast = 7;
            armored = 3;
        } else {
            basic = 4;
            fast = 6;
            armored = 6;
        }
        add(plan, TankType.AI_BASIC, basic);
        add(plan, TankType.AI_FAST, fast);
        add(plan, TankType.AI_ARMORED, armored);
        add(plan, TankType.AI_BONUS,
                com.tankwar.constant.GameConfig.BONUS_TANKS_PER_LEVEL);
        Collections.shuffle(plan, new Random(level * 1009L + 7L));
        return Collections.unmodifiableList(plan);
    }

    /** 批量添加坦克类型。 */
    private void add(List<TankType> plan, TankType type, int count) {
        for (int i = 0; i < count; i++) {
            plan.add(type);
        }
    }
}
