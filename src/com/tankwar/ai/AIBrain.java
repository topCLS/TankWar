package com.tankwar.ai;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.map.GameMap;
import com.tankwar.model.AITank;
import com.tankwar.model.Base;
import com.tankwar.model.PlayerTank;
import com.tankwar.util.CollisionDetector;

import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 敌方坦克 AI 决策大脑（巡逻 / 追踪 / 攻击三态有限状态机）。
 * <p>运行于调度器线程，{@link #decide} 只读地图与玩家位置，把
 * "下一步移动方向 / 是否开火"写入 {@link AITank} 的 volatile 字段，
 * 由游戏主线程在逻辑步中消费——决策与执行分离，重算按
 * {@link Difficulty#getRepathIntervalMs()} 节流，不会拖慢帧率。</p>
 * <pre>
 *  PATROL ──(视野内/受击)──▶ CHASE ──(同行列且无遮挡)──▶ ATTACK
 *    ▲                          │                          │
 *    └────(丢失目标 2 秒/无路)───┴────────(丢失目标 2s)─────┘
 * </pre>
 *
 * @author TankWar Team
 */
public class AIBrain {

    /** 丢失目标后继续向最后位置追击的宽限时间。 */
    private static final long LOST_GRACE_MS = 2000L;
    /** 巡逻换向最小间隔。 */
    private static final long PATROL_TURN_MIN = 800L;
    /** 巡逻换向最大间隔。 */
    private static final long PATROL_TURN_MAX = 1500L;

    /** 所属坦克。 */
    private final AITank ai;
    /** 状态机当前状态。 */
    private AIState state = AIState.PATROL;
    /** 随机数（每个 AI 独立实例，线程安全）。 */
    private final Random random = new Random();

    /** 当前缓存路径（仅本 brain 线程访问）。 */
    private List<Point> path = Collections.emptyList();
    /** 路径下标。 */
    private int pathIndex = 0;
    /** 下次允许重算路径的时间。 */
    private long nextRepathAt = 0L;
    /** 下次巡逻换向时间。 */
    private long nextPatrolTurnAt = 0L;
    /** 巡逻方向。 */
    private Direction patrolDir = Direction.DOWN;
    /** 最后看到目标的格子。 */
    private Point lastSeenCell = null;
    /** 最后看到目标的时间。 */
    private long lastSeenAt = 0L;

    /**
     * 构造决策大脑。
     *
     * @param ai 所属敌方坦克
     */
    public AIBrain(AITank ai) {
        this.ai = ai;
        this.patrolDir = Direction.values()[random.nextInt(4)];
    }

    /** @return 当前状态（监控/调试用）。 */
    public AIState getState() {
        return state;
    }

    /**
     * 执行一次决策（按难度间隔由外部节流）。
     *
     * @param map        地图（只读）
     * @param players    存活玩家列表
     * @param base       玩家基地（对战模式可为 null）
     * @param now        当前时间
     * @param allowChase 难度是否仍允许新的追击者（同时追击者上限）
     */
    public void decide(GameMap map, List<PlayerTank> players, Base base,
                       long now, boolean allowChase) {
        if (!ai.isAlive()) {
            return;
        }
        Difficulty dif = ai.getDifficulty();

        // 撞墙立即作废缓存路径
        if (ai.isMoveBlocked()) {
            ai.setMoveBlocked(false);
            path = Collections.emptyList();
        }

        PlayerTank target = nearestPlayer(players);
        boolean see = target != null
                && VisionUtil.canSee(map, ai, target, dif.getVisionRange());

        if (see) {
            Direction face = VisionUtil.cardinalDir(ai, target);
            boolean clearShot = face != null && !CollisionDetector.isLineBlocked(map,
                    ai.getCenterX(), ai.getCenterY(),
                    target.getCenterX(), target.getCenterY(),
                    GameConfig.BULLET_SUBSTEP);
            if (clearShot) {
                // 同行列且无遮挡：遭遇攻击永远允许
                attack(face, dif);
                return;
            }
            if (allowChase) {
                // 追踪：沿 BFS 路径逼近
                state = AIState.CHASE;
                lastSeenCell = new Point(
                        GameMap.pixelToCol(target.getCenterX()),
                        GameMap.pixelToRow(target.getCenterY()));
                lastSeenAt = now;
                Point goal = lastSeenCell;
                if (!followPath(map, aiCell(), goal, now, dif, false)) {
                    ai.setAiMoveDir(VisionUtil.roughDir(
                            ai.getCenterX(), ai.getCenterY(),
                            target.getCenterX(), target.getCenterY()));
                }
                return;
            }
            // 追击名额已满：暂退巡逻 / 推进基地
            state = AIState.PATROL;
            patrolOrAttackBase(map, players, base, now, dif);
            return;
        }

        if (lastSeenCell != null && (state == AIState.CHASE || state == AIState.ATTACK)
                && now - lastSeenAt < LOST_GRACE_MS) {
            // 短暂记忆：向最后目击位置追击
            state = AIState.CHASE;
            if (!followPath(map, aiCell(), lastSeenCell, now, dif, false)) {
                state = AIState.PATROL;
            }
            return;
        }
        state = AIState.PATROL;
        patrolOrAttackBase(map, players, base, now, dif);
    }

    /**
     * 进入攻击态：转向并按命中率开火。
     */
    private void attack(Direction face, Difficulty dif) {
        state = AIState.ATTACK;
        ai.setAiMoveDir(face);
        if (random.nextDouble() > dif.getAimError()) {
            ai.setAiWantFire(true);
        }
    }

    /**
     * 巡逻，并周期性地向基地发起进攻（看不到玩家时仍有战术目标）。
     */
    private void patrolOrAttackBase(GameMap map, List<PlayerTank> players,
                                    Base base, long now, Difficulty dif) {
        double baseRushProbability;
        switch (dif) {
            case HARD:
                baseRushProbability = 0.5;
                break;
            case NORMAL:
                baseRushProbability = 0.3;
                break;
            default:
                baseRushProbability = 0.15;
        }

        if (base != null && !base.isDestroyed()
                && random.nextDouble() < baseRushProbability) {
            Point baseCell = new Point(base.getCellCol(), base.getCellRow());
            if (followPath(map, aiCell(), baseCell, now, dif, true)) {
                Direction face = faceToPoint(base.getCenterX(), base.getCenterY());
                if (face != null && !CollisionDetector.isLineBlocked(map,
                        ai.getCenterX(), ai.getCenterY(),
                        base.getCenterX(), base.getCenterY(),
                        GameConfig.BULLET_SUBSTEP)
                        && random.nextDouble() > dif.getAimError()) {
                    ai.setAiMoveDir(face);
                    ai.setAiWantFire(true);
                }
                return;
            }
        }

        // 纯巡逻：撞墙或到时换向，30% 概率朝最近玩家大致方向偏转
        if (now >= nextPatrolTurnAt) {
            PlayerTank nearest = nearestPlayer(players);
            if (random.nextDouble() < 0.30 && nearest != null) {
                patrolDir = VisionUtil.roughDir(
                        ai.getCenterX(), ai.getCenterY(),
                        nearest.getCenterX(), nearest.getCenterY());
            } else {
                patrolDir = Direction.values()[random.nextInt(4)];
            }
            nextPatrolTurnAt = now + PATROL_TURN_MIN
                    + random.nextInt((int) (PATROL_TURN_MAX - PATROL_TURN_MIN));
        }
        ai.setAiMoveDir(patrolDir);
        // 巡逻时偶尔盲射
        if (random.nextDouble() < 0.04) {
            ai.setAiWantFire(true);
        }
    }

    /**
     * 沿缓存路径前进；到节流时间点则用 BFS/A* 重算。
     *
     * @param useAStar 是否用 A*（打基地，允许穿砖墙）
     * @return 成功取得下一步方向返回 true；无路径返回 false
     */
    private boolean followPath(GameMap map, Point start, Point goal, long now,
                               Difficulty dif, boolean useAStar) {
        if (now >= nextRepathAt || path.isEmpty()) {
            path = useAStar
                    ? PathFinder.findPathAStar(map, start, goal)
                    : PathFinder.findPathBfs(map, start, goal);
            pathIndex = 0;
            nextRepathAt = now + dif.getRepathIntervalMs();
        }
        Point cur = aiCell();
        while (pathIndex < path.size() && cur.equals(path.get(pathIndex))) {
            pathIndex++;
        }
        if (pathIndex >= path.size()) {
            path = Collections.emptyList();
            return false;
        }
        Point step = path.get(pathIndex);
        int targetX = step.x * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        int targetY = step.y * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
        ai.setAiMoveDir(VisionUtil.roughDir(
                ai.getCenterX(), ai.getCenterY(), targetX, targetY));
        return true;
    }

    /**
     * 与某点严格同行/同列时的朝向。
     *
     * @return 朝向；不对齐返回 null
     */
    private Direction faceToPoint(int px, int py) {
        int row = GameMap.pixelToRow(ai.getCenterY());
        int col = GameMap.pixelToCol(ai.getCenterX());
        int targetRow = GameMap.pixelToRow(py);
        int targetCol = GameMap.pixelToCol(px);
        if (col == targetCol) {
            return py < ai.getCenterY() ? Direction.UP : Direction.DOWN;
        }
        if (row == targetRow) {
            return px < ai.getCenterX() ? Direction.LEFT : Direction.RIGHT;
        }
        return null;
    }

    /** @return 坦克当前所在格。 */
    private Point aiCell() {
        return new Point(GameMap.pixelToCol(ai.getCenterX()),
                GameMap.pixelToRow(ai.getCenterY()));
    }

    /**
     * 选取距离最近的存活玩家。
     *
     * @param players 玩家列表
     * @return 最近玩家；无存活玩家返回 null
     */
    private PlayerTank nearestPlayer(List<PlayerTank> players) {
        PlayerTank best = null;
        int bestDist = Integer.MAX_VALUE;
        for (PlayerTank p : players) {
            if (!p.isAlive()) {
                continue;
            }
            int dist = Math.abs(p.getCenterX() - ai.getCenterX())
                    + Math.abs(p.getCenterY() - ai.getCenterY());
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }
}
