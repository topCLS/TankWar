package com.tankwar.prop;

import com.tankwar.constant.PowerUpType;
import com.tankwar.model.PlayerTank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 道具效果管理器：按玩家维护生效中的增益与到期时间。
 * <p>同一效果再次拾取时持续时间取最大值（刷新而非叠加）；效果到期
 * 自动调用道具的 {@code expire} 还原坦克原始属性（如速度按基础值还原，
 * 而非"再除回去"，避免浮点误差累积）。</p>
 *
 * @author TankWar Team
 */
public class EffectManager {

    /** 单个生效记录。 */
    private static final class ActiveEffect {
        /** 道具（用于到期回调）。 */
        PowerUp powerUp;
        /** 到期时间。 */
        long expireAt;

        /**
         * 构造记录。
         *
         * @param powerUp  道具
         * @param expireAt 到期时间
         */
        ActiveEffect(PowerUp powerUp, long expireAt) {
            this.powerUp = powerUp;
            this.expireAt = expireAt;
        }
    }

    /** 玩家编号 -> (类型 -> 生效记录)。 */
    private final Map<Integer, Map<PowerUpType, ActiveEffect>> active =
            new HashMap<Integer, Map<PowerUpType, ActiveEffect>>();

    /**
     * 对玩家施加道具效果。
     *
     * @param tank    玩家
     * @param powerUp 道具
     * @param now     当前时间
     */
    public void apply(PlayerTank tank, PowerUp powerUp, long now) {
        Map<PowerUpType, ActiveEffect> map = active.get(tank.getPlayerId());
        if (map == null) {
            map = new HashMap<PowerUpType, ActiveEffect>();
            active.put(tank.getPlayerId(), map);
        }
        long expireAt = now + powerUp.getType().getDurationMs();
        ActiveEffect existing = map.get(powerUp.getType());
        if (existing != null) {
            // 已存在同类效果：仅刷新时间（幂等）
            existing.expireAt = Math.max(existing.expireAt, expireAt);
            return;
        }
        powerUp.apply(tank, now);
        map.put(powerUp.getType(), new ActiveEffect(powerUp, expireAt));
    }

    /**
     * 每逻辑步检查到期效果并还原。
     *
     * @param now 当前时间
     */
    public void update(long now) {
        for (Integer playerId : new ArrayList<Integer>(active.keySet())) {
            Map<PowerUpType, ActiveEffect> map = active.get(playerId);
            PlayerTank tank = tankHolder.get(playerId);
            List<PowerUpType> expired = new ArrayList<PowerUpType>();
            for (Map.Entry<PowerUpType, ActiveEffect> e : map.entrySet()) {
                if (now >= e.getValue().expireAt) {
                    expired.add(e.getKey());
                }
            }
            for (PowerUpType type : expired) {
                ActiveEffect record = map.remove(type);
                // 坦克实例存在时回调还原；仅移除记录也要保证不泄漏
                if (tank != null) {
                    record.powerUp.expire(tank);
                }
            }
        }
    }

    /** 玩家编号 -> 当前存活坦克的映射（由外部每帧更新）。 */
    private final Map<Integer, PlayerTank> tankHolder =
            new HashMap<Integer, PlayerTank>();

    /**
     * 登记玩家坦克（供到期回调使用）。
     *
     * @param tank 玩家坦克
     */
    public void registerTank(PlayerTank tank) {
        tankHolder.put(tank.getPlayerId(), tank);
    }

    /**
     * 按编号取玩家坦克。
     *
     * @param playerId 玩家编号
     * @return 坦克；不存在返回 null
     */
    private PlayerTank holderOf(int playerId) {
        return tankHolder.get(playerId);
    }

    /**
     * 清理指定玩家的全部效果并还原（复活/出局时调用）。
     *
     * @param tank 玩家坦克
     */
    public void clearPlayer(PlayerTank tank) {
        Map<PowerUpType, ActiveEffect> map = active.remove(tank.getPlayerId());
        if (map != null) {
            for (ActiveEffect record : map.values()) {
                record.powerUp.expire(tank);
            }
        }
    }

    /** 清空全部玩家效果（关卡重置）。 */
    public void clearAll() {
        for (Map.Entry<Integer, Map<PowerUpType, ActiveEffect>> entry
                : active.entrySet()) {
            PlayerTank tank = tankHolder.get(entry.getKey());
            if (tank != null) {
                for (ActiveEffect record : entry.getValue().values()) {
                    record.powerUp.expire(tank);
                }
            }
        }
        active.clear();
    }

    /**
     * 某玩家某效果剩余毫秒。
     *
     * @param playerId 玩家编号
     * @param type     类型
     * @param now      当前时间
     * @return 剩余毫秒；未生效返回 0
     */
    public long remainingMillis(int playerId, PowerUpType type, long now) {
        Map<PowerUpType, ActiveEffect> map = active.get(playerId);
        if (map == null) {
            return 0L;
        }
        ActiveEffect record = map.get(type);
        return record == null ? 0L : Math.max(0L, record.expireAt - now);
    }

    /**
     * 某玩家当前生效的效果类型列表。
     *
     * @param playerId 玩家编号
     * @return 不可变类型列表
     */
    public List<PowerUpType> activeTypes(int playerId) {
        Map<PowerUpType, ActiveEffect> map = active.get(playerId);
        if (map == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<PowerUpType>(map.keySet()));
    }
}
