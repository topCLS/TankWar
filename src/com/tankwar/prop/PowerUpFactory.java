package com.tankwar.prop;

import com.tankwar.constant.PowerUpType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 道具工厂。
 * <p>用"类型 -&gt; 构造器"注册表替代 if-else / switch 链：新增道具只需
 * 注册一个构造器，加权随机逻辑无需改动（开闭原则）。</p>
 *
 * @author TankWar Team
 */
public final class PowerUpFactory {

    /** 具体道具构造器接口。 */
    public interface Creator {
        /**
         * 创建道具。
         *
         * @param col 格列
         * @param row 格行
         * @param now 生成时间
         * @return 道具实例
         */
        PowerUp create(int col, int row, long now);
    }

    /** 类型到构造器的注册表。 */
    private static final Map<PowerUpType, Creator> CREATORS =
            new HashMap<PowerUpType, Creator>();
    /** 权重总和。 */
    private static int totalWeight = 0;

    static {
        register(PowerUpType.SPEED_BOOST, new Creator() {
            @Override
            public PowerUp create(int col, int row, long now) {
                return new SpeedBoost(col, row, now);
            }
        });
        register(PowerUpType.POWER_BULLET, new Creator() {
            @Override
            public PowerUp create(int col, int row, long now) {
                return new PowerBullet(col, row, now);
            }
        });
        register(PowerUpType.SHIELD, new Creator() {
            @Override
            public PowerUp create(int col, int row, long now) {
                return new Shield(col, row, now);
            }
        });
    }

    private PowerUpFactory() {
    }

    /**
     * 注册构造器。
     *
     * @param type    类型
     * @param creator 构造器
     */
    private static void register(PowerUpType type, Creator creator) {
        CREATORS.put(type, creator);
        totalWeight += type.getWeight();
    }

    /**
     * 按权重随机创建一个道具。
     *
     * @param col    格列
     * @param row    格行
     * @param now    生成时间
     * @param random 随机数源
     * @return 道具实例
     */
    public static PowerUp randomCreate(int col, int row, long now, Random random) {
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (PowerUpType type : PowerUpType.values()) {
            cumulative += type.getWeight();
            if (roll < cumulative) {
                return CREATORS.get(type).create(col, row, now);
            }
        }
        PowerUpType fallback = PowerUpType.values()[0];
        return CREATORS.get(fallback).create(col, row, now);
    }

    /**
     * 按指定类型创建（测试/指定掉落用）。
     *
     * @param type 类型
     * @param col  格列
     * @param row  格行
     * @param now  时间
     * @return 道具
     */
    public static PowerUp create(PowerUpType type, int col, int row, long now) {
        return CREATORS.get(type).create(col, row, now);
    }
}
