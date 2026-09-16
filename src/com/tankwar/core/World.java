package com.tankwar.core;

import com.tankwar.ai.AIBrain;
import com.tankwar.constant.Difficulty;
import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.GameMode;
import com.tankwar.constant.ScoreReason;
import com.tankwar.constant.TileType;
import com.tankwar.constant.TankType;
import com.tankwar.map.GameMap;
import com.tankwar.map.LevelData;
import com.tankwar.map.LevelManager;
import com.tankwar.map.MapLoader;
import com.tankwar.map.TileHitResult;
import com.tankwar.model.AITank;
import com.tankwar.model.Base;
import com.tankwar.model.Bullet;
import com.tankwar.model.PlayerTank;
import com.tankwar.model.Tank;
import com.tankwar.prop.EffectManager;
import com.tankwar.prop.PowerUp;
import com.tankwar.prop.PowerUpFactory;
import com.tankwar.render.Explosion;
import com.tankwar.render.SpawnMarker;
import com.tankwar.render.Spark;
import com.tankwar.util.AudioPlayer;
import com.tankwar.util.CollisionDetector;
import com.tankwar.util.ObjectPool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 一局游戏的世界（核心系统）。
 * <p>统一持有地图、双方坦克、子弹、道具、特效与基地，在固定逻辑步中
 * 调度移动碰撞、分步扫掠开火、刷怪、掉落拾取、增益到期、死亡结算与
 * 胜负判定；渲染时按 z-order 分层绘制战场并施加屏幕震动。本类是模型
 * 实体与各算法模块之间的"中介/门面"，实体本身不互相直接修改。</p>
 *
 * @author TankWar Team
 */
public class World {

    private final GameContext context;
    private final int levelIndex;
    private final GameMode mode;
    private final Difficulty difficulty;
    private final GameMap map;
    private final LevelData levelData;
    private final Base base;

    private final List<PlayerTank> players = new CopyOnWriteArrayList<PlayerTank>();
    private final List<AITank> enemies = new CopyOnWriteArrayList<AITank>();
    private final List<Bullet> bullets = new CopyOnWriteArrayList<Bullet>();
    private final List<PowerUp> powerups = new CopyOnWriteArrayList<PowerUp>();
    private final List<Spark> sparks = new CopyOnWriteArrayList<Spark>();
    private final List<Explosion> explosions = new CopyOnWriteArrayList<Explosion>();
    private final List<PendingSpawn> pendingSpawns =
            new CopyOnWriteArrayList<PendingSpawn>();

    /** 敌方坦克 -> AI 大脑（决策线程与游戏线程共享）。 */
    private final Map<AITank, AIBrain> brains =
            new ConcurrentHashMap<AITank, AIBrain>();

    private final EffectManager effects = new EffectManager();
    private final ObjectPool<Bullet> bulletPool = new ObjectPool<Bullet>(
            new ObjectPool.ObjectFactory<Bullet>() {
                @Override
                public Bullet create() {
                    return new Bullet();
                }
            });

    /** 尚未出场的敌方坦克队列。 */
    private final Deque<TankType> spawnQueue = new ArrayDeque<TankType>();
    private final Random random;
    private long nextSpawnAt;

    /** 已结算击毁奖励的敌人。 */
    private final Set<AITank> rewarded = new HashSet<AITank>();
    /** 被复活清场、不给予奖励的敌人。 */
    private final Set<AITank> despawned = new HashSet<AITank>();
    /** 已耗尽生命淘汰的玩家。 */
    private final Set<PlayerTank> eliminated = new HashSet<PlayerTank>();

    private long shakeUntil = 0L;
    private int shakeMagnitude = 0;

    private boolean levelCleared = false;
    private boolean failed = false;
    /** 对战胜方：-1 未决，0 平局，1/2 玩家。 */
    private int versusWinner = -1;

    /**
     * 创建一局世界并加载指定关卡。
     *
     * @param context    游戏上下文
     * @param levelIndex 关卡序号（从 1 开始）
     */
    public World(GameContext context, int levelIndex) {
        this.context = context;
        this.levelIndex = levelIndex;
        this.mode = context.getMode();
        this.difficulty = context.getDifficulty();
        this.random = new Random(0x5A44L + levelIndex * 131L);

        this.levelData = MapLoader.load(levelIndex);
        this.map = levelData.getMap();
        this.base = mode.isVersus() ? null
                : new Base(levelData.getBaseCell().x, levelData.getBaseCell().y);

        long now = System.currentTimeMillis();
        PlayerTank p1 = new PlayerTank(1, levelData.getPlayer1Spawn().x,
                levelData.getPlayer1Spawn().y);
        players.add(p1);
        effects.registerTank(p1);
        if (mode.getPlayerCount() >= 2) {
            PlayerTank p2 = new PlayerTank(2, levelData.getPlayer2Spawn().x,
                    levelData.getPlayer2Spawn().y);
            players.add(p2);
            effects.registerTank(p2);
        }

        spawnQueue.addAll(context.getLevelManager().enemyPlan(levelIndex));
        this.nextSpawnAt = now + 1500L;
    }

    /* ============================== 主更新 ============================== */

    /**
     * 推进一个逻辑步。
     *
     * @param dtMs 步长（固定 60TPS）
     * @param now  当前时间
     */
    public void update(long dtMs, long now) {
        if (levelCleared || failed || versusWinner >= 0) {
            updateEffectsOnly(dtMs);
            return;
        }
        updateSpawner(now);
        updatePlayerMovement(dtMs);
        updateEnemyMovement(dtMs, now);
        updateBullets(now);
        updatePowerups(now);
        effects.update(now);
        resolveDeaths(now);
        resolveOutcome();
        updateEffectsOnly(dtMs);
        cleanup();
    }

    /** 仅推进粒子/爆炸（结算画面也保留动画）。 */
    private void updateEffectsOnly(long dtMs) {
        for (Explosion e : explosions) {
            e.update(dtMs);
        }
        for (Spark s : sparks) {
            s.update(dtMs);
        }
    }

    /* ------------------------------ 刷怪 ------------------------------ */

    private void updateSpawner(long now) {
        if (mode.isVersus()) {
            return;
        }
        // 已到点的出生标记：位置空闲则真正生成
        List<PendingSpawn> due = new ArrayList<PendingSpawn>();
        for (PendingSpawn ps : pendingSpawns) {
            if (now - ps.marker.getStartAt() >= GameConfig.SPAWN_FLASH_MS) {
                due.add(ps);
            }
        }
        for (PendingSpawn ps : due) {
            if (cellFreeForTank(ps.marker.getCellRow(), ps.marker.getCellCol())) {
                spawnEnemy(ps.type, ps.marker.getCellCol(),
                        ps.marker.getCellRow(), now);
                pendingSpawns.remove(ps);
            }
            // 位置被占则保留标记，下一拍重试
        }
        // 安排新出生
        if (!spawnQueue.isEmpty()
                && enemies.size() + pendingSpawns.size()
                < GameConfig.MAX_ENEMIES_ON_FIELD
                && now >= nextSpawnAt) {
            Point cell = chooseFreeSpawnCell(now);
            if (cell != null) {
                TankType type = spawnQueue.poll();
                pendingSpawns.add(new PendingSpawn(
                        new SpawnMarker(cell.x, cell.y, now), type));
                nextSpawnAt = now + difficulty.getSpawnIntervalMs();
            }
        }
    }

    /** 选择一个空闲敌方出生点（无标记、无坦克占据 2x2）。 */
    private Point chooseFreeSpawnCell(long now) {
        List<Point> candidates = new ArrayList<Point>(levelData.getEnemySpawns());
        Collections.shuffle(candidates, random);
        for (Point p : candidates) {
            boolean markerBusy = false;
            for (PendingSpawn ps : pendingSpawns) {
                if (ps.marker.getCellCol() == p.x
                        && ps.marker.getCellRow() == p.y) {
                    markerBusy = true;
                    break;
                }
            }
            if (!markerBusy && cellFreeForTank(p.y, p.x)) {
                return new Point(p.x, p.y);
            }
        }
        return null;
    }

    /** 2x2 出生区域是否没有任何坦克占据。 */
    private boolean cellFreeForTank(int row, int col) {
        Rectangle area = new Rectangle(col * GameConfig.TILE_SIZE,
                row * GameConfig.TILE_SIZE, GameConfig.TILE_SIZE * 2,
                GameConfig.TILE_SIZE * 2);
        for (Tank t : allTanks()) {
            if (t.isAlive() && area.intersects(t.getBounds())) {
                return false;
            }
        }
        return true;
    }

    /** 实例化敌方坦克并挂载 AI 大脑。 */
    private void spawnEnemy(TankType type, int col, int row, long now) {
        AITank enemy = new AITank(type, col, row, difficulty, now);
        enemies.add(enemy);
        brains.put(enemy, new AIBrain(enemy));
    }

    /**
     * 由调度器线程周期调用：为全部敌方坦克做一次决策（只读世界）。
     *
     * @param now 当前时间
     */
    public void runAiDecisions(long now) {
        if (mode.isVersus()) {
            return;
        }
        int chasers = 0;
        for (AITank enemy : enemies) {
            AIBrain brain = brains.get(enemy);
            if (brain != null && enemy.isAlive()
                    && (brain.getState() == com.tankwar.ai.AIState.CHASE
                    || brain.getState() == com.tankwar.ai.AIState.ATTACK)) {
                chasers++;
            }
        }
        boolean allowChase = chasers < difficulty.getMaxChasers();
        for (AITank enemy : enemies) {
            AIBrain brain = brains.get(enemy);
            if (brain != null && enemy.isAlive()) {
                brain.decide(map, players, base, now, allowChase);
            }
        }
    }

    /* ---------------------------- 移动 / 开火 ---------------------------- */

    private void updatePlayerMovement(long dtMs) {
        for (PlayerTank p : players) {
            if (!p.isAlive()) {
                continue;
            }
            Direction dir = context.getInput().getMoveDirection(p.getPlayerId());
            if (dir != null) {
                moveTank(p, dir);
            } else {
                p.setMoving(false);
            }
            p.update(dtMs);
        }
    }

    private void updateEnemyMovement(long dtMs, long now) {
        for (AITank enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            int oldX = enemy.getX();
            int oldY = enemy.getY();
            moveTank(enemy, enemy.getAiMoveDir());
            // 沿主轴没动 -> 撞墙/被挡，反馈给决策线程换向
            boolean axialBlocked = enemy.getDir().isHorizontal()
                    ? enemy.getX() == oldX : enemy.getY() == oldY;
            enemy.setMoveBlocked(axialBlocked);
            if (enemy.consumeFireRequest()
                    && enemy.canFire(now, countBulletsOf(enemy))) {
                fire(enemy, false, -1);
            }
            enemy.update(dtMs);
        }
    }

    /**
     * 移动坦克：先做地形碰撞与格线吸附，再做坦克间碰撞（重叠则整步取消）。
     *
     * @param tank 坦克
     * @param dir  方向
     */
    private void moveTank(Tank tank, Direction dir) {
        int oldX = tank.getX();
        int oldY = tank.getY();
        tank.setDir(dir);
        int[] next = CollisionDetector.tryMoveAxis(map, oldX, oldY,
                GameConfig.TANK_SIZE, dir, tank.getSpeed());
        if (!hitsOtherTank(next[0], next[1], tank)) {
            tank.setX(next[0]);
            tank.setY(next[1]);
            tank.setMoving(next[0] != oldX || next[1] != oldY);
        } else {
            tank.setMoving(false);
        }
    }

    /**
     * 候选位置是否与其他坦克重叠。
     */
    private boolean hitsOtherTank(int x, int y, Tank self) {
        Rectangle rect = new Rectangle(x, y,
                GameConfig.TANK_SIZE, GameConfig.TANK_SIZE);
        for (Tank t : allTanks()) {
            if (t != self && t.isAlive() && rect.intersects(t.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private List<Tank> allTanks() {
        List<Tank> all = new ArrayList<Tank>(players.size() + enemies.size());
        all.addAll(players);
        all.addAll(enemies);
        return all;
    }

    /**
     * 玩家请求开火（边沿触发，由状态分发调用）。
     *
     * @param playerId 玩家编号
     */
    public void requestFire(int playerId) {
        for (PlayerTank p : players) {
            if (p.getPlayerId() == playerId && p.isAlive()) {
                long now = System.currentTimeMillis();
                if (p.canFire(now, countBulletsOf(p))) {
                    fire(p, true, playerId);
                }
                return;
            }
        }
    }

    /** 统计某坦克当前在场子弹数。 */
    private int countBulletsOf(Tank tank) {
        int count = 0;
        for (Bullet b : bullets) {
            if (b.isAlive() && b.getOwner() == tank) {
                count++;
            }
        }
        return count;
    }

    /**
     * 真正发射：炮口生成子弹（对象池获取），受冷却与数量限制。
     */
    private void fire(Tank tank, boolean fromPlayer, int ownerPlayerId) {
        long now = System.currentTimeMillis();
        if (!tank.canFire(now, countBulletsOf(tank))) {
            return;
        }
        int bulletSize = GameConfig.BULLET_SIZE;
        int startX = tank.getCenterX()
                + tank.getDir().getDx() * (GameConfig.TANK_SIZE / 2)
                - bulletSize / 2;
        int startY = tank.getCenterY()
                + tank.getDir().getDy() * (GameConfig.TANK_SIZE / 2)
                - bulletSize / 2;
        int power = tank.getBulletPower();
        int speed = power >= 2 ? GameConfig.POWER_BULLET_SPEED
                : GameConfig.BULLET_SPEED;
        Bullet bullet = bulletPool.acquire();
        bullet.reset(tank, fromPlayer, ownerPlayerId, startX, startY,
                tank.getDir(), speed, power);
        bullets.add(bullet);
        tank.markFired(now);
        context.getAudio().playOnce("fire.wav");
    }

    /* ------------------------------ 子弹 ------------------------------ */

    private void updateBullets(long now) {
        for (Bullet b : bullets) {
            if (!b.isAlive()) {
                continue;
            }
            int subSteps = Math.max(1,
                    (int) Math.ceil(b.getSpeed() * 1.0
                            / GameConfig.BULLET_SUBSTEP));
            double dx = b.getDir().getDx() * b.getSpeed() * 1.0 / subSteps;
            double dy = b.getDir().getDy() * b.getSpeed() * 1.0 / subSteps;
            for (int i = 0; i < subSteps && b.isAlive(); i++) {
                b.markPrev();
                b.moveBy(dx, dy);
                if (outOfField(b)) {
                    b.setAlive(false);
                    break;
                }
                if (bulletHitsBullet(b)) {
                    break;
                }
                Tank victim = pickTankVictim(b);
                if (victim != null) {
                    onBulletHitTank(b, victim, now);
                    break;
                }
                int col = GameMap.pixelToCol((int) b.getCurCenterX());
                int row = GameMap.pixelToRow((int) b.getCurCenterY());
                TileType type = map.getTileType(row, col);
                if (type == TileType.BRICK || type == TileType.STEEL
                        || type == TileType.BASE) {
                    onBulletHitTile(b, row, col, now);
                }
            }
        }
    }

    private boolean outOfField(Bullet b) {
        return b.getX() < 0 || b.getY() < 0
                || b.getX() + b.getWidth() > GameConfig.FIELD_W
                || b.getY() + b.getHeight() > GameConfig.FIELD_H;
    }

    /** 玩家子弹与敌方子弹相遇互相抵消。 */
    private boolean bulletHitsBullet(Bullet b) {
        for (Bullet other : bullets) {
            if (other != b && other.isAlive()
                    && other.isFromPlayer() != b.isFromPlayer()
                    && other.getBounds().intersects(b.getBounds())) {
                b.setAlive(false);
                other.setAlive(false);
                burst(other.getCenterX(), other.getCenterY(),
                        Color.WHITE, 4, 1.5, 180L);
                return true;
            }
        }
        return false;
    }

    /** 用线段扫掠选择被子弹击中的坦克（高速不穿透）。 */
    private Tank pickTankVictim(Bullet b) {
        if (b.isFromPlayer()) {
            if (mode.isVersus()) {
                for (PlayerTank p : players) {
                    if (p.getPlayerId() != b.getOwnerPlayerId() && p.isAlive()
                            && CollisionDetector.sweptAABB(p.getBounds(),
                            b.getPrevCenterX(), b.getPrevCenterY(),
                            b.getCurCenterX(), b.getCurCenterY(),
                            GameConfig.BULLET_SIZE)) {
                        return p;
                    }
                }
            } else {
                for (AITank e : enemies) {
                    if (e.isAlive() && CollisionDetector.sweptAABB(e.getBounds(),
                            b.getPrevCenterX(), b.getPrevCenterY(),
                            b.getCurCenterX(), b.getCurCenterY(),
                            GameConfig.BULLET_SIZE)) {
                        return e;
                    }
                }
            }
        } else {
            for (PlayerTank p : players) {
                if (p.isAlive() && CollisionDetector.sweptAABB(p.getBounds(),
                        b.getPrevCenterX(), b.getPrevCenterY(),
                        b.getCurCenterX(), b.getCurCenterY(),
                        GameConfig.BULLET_SIZE)) {
                    return p;
                }
            }
        }
        return null;
    }

    /** 子弹命中坦克：结算受伤与火花，死亡交给统一死亡结算。 */
    private void onBulletHitTank(Bullet b, Tank tank, long now) {
        boolean damaged = tank.takeDamage(b.getPower(), now);
        if (tank instanceof AITank) {
            ((AITank) tank).setKillerPlayerId(b.getOwnerPlayerId());
        }
        if (damaged && !tank.isAlive()) {
            addExplosion(tank.getCenterX(), tank.getCenterY(), Explosion.Scale.MEDIUM);
            context.getAudio().playOnce("explode_big.wav");
        } else {
            context.getAudio().playOnce("steel.wav");
            Color sparkColor = damaged ? new Color(255, 200, 90)
                    : new Color(120, 190, 255);
            burst(b.getCurCenterX(), b.getCurCenterY(), sparkColor, 6, 2.0, 220L);
        }
        b.setAlive(false);
    }

    /** 子弹命中地形：按伤害结果触发爆炸/火花/音效与基地摧毁。 */
    private void onBulletHitTile(Bullet b, int row, int col, long now) {
        TileType type = map.getTileType(row, col);
        if (type == TileType.BASE) {
            if (!b.isFromPlayer() && base != null && !base.isDestroyed()) {
                map.damageTile(row, col, b.getPower());
                base.destroy();
                addExplosion(base.getCenterX(), base.getCenterY(),
                        Explosion.Scale.LARGE);
                shake(450L, 6);
                context.getAudio().playOnce("explode_big.wav");
            } else {
                burst(b.getCurCenterX(), b.getCurCenterY(),
                        new Color(255, 220, 120), 5, 1.5, 200L);
            }
            b.setAlive(false);
            return;
        }
        TileHitResult result = map.damageTile(row, col, b.getPower());
        switch (result) {
            case BRICK_DESTROYED:
                addExplosion((int) b.getCurCenterX(), (int) b.getCurCenterY(),
                        Explosion.Scale.SMALL);
                context.getAudio().playOnce("brick.wav");
                burst(b.getCurCenterX(), b.getCurCenterY(),
                        new Color(220, 130, 70), 6, 2.0, 240L);
                break;
            case BRICK_DAMAGED:
                context.getAudio().playOnce("brick.wav");
                burst(b.getCurCenterX(), b.getCurCenterY(),
                        new Color(220, 130, 70), 4, 1.4, 180L);
                break;
            case STEEL_DESTROYED:
                addExplosion((int) b.getCurCenterX(), (int) b.getCurCenterY(),
                        Explosion.Scale.SMALL);
                context.getAudio().playOnce("brick.wav");
                burst(b.getCurCenterX(), b.getCurCenterY(),
                        new Color(220, 220, 230), 8, 2.4, 280L);
                break;
            case STEEL_BLOCKED:
                context.getAudio().playOnce("steel.wav");
                burst(b.getCurCenterX(), b.getCurCenterY(),
                        new Color(230, 230, 240), 6, 2.0, 220L);
                break;
            default:
                break;
        }
        b.setAlive(false);
    }

    /* ----------------------------- 道具 ----------------------------- */

    private void updatePowerups(long now) {
        List<PowerUp> expired = new ArrayList<PowerUp>();
        for (PowerUp p : powerups) {
            if (p.isExpiredFromField(now)) {
                expired.add(p);
                continue;
            }
            for (PlayerTank tank : players) {
                if (tank.isAlive() && tank.getBounds().intersects(p.getBounds())) {
                    effects.apply(tank, p, now);
                    context.getScoreManager().addScore(tank.getPlayerId(),
                            50, ScoreReason.PICKUP_POWERUP);
                    context.getAudio().playOnce("powerup.wav");
                    expired.add(p);
                    break;
                }
            }
        }
        powerups.removeAll(expired);
    }

    /* ----------------------------- 死亡结算 ----------------------------- */

    private void resolveDeaths(long now) {
        // 先结算玩家（可能触发复活清场），再结算敌人
        List<PlayerTank> newlyDead = new ArrayList<PlayerTank>();
        for (PlayerTank p : players) {
            if (!p.isAlive() && !eliminated.contains(p)) {
                newlyDead.add(p);
            }
        }
        for (PlayerTank p : newlyDead) {
            int left = p.loseLife();
            effects.clearPlayer(p);
            addExplosion(p.getCenterX(), p.getCenterY(), Explosion.Scale.MEDIUM);
            context.getAudio().playOnce("explode_big.wav");
            if (left > 0) {
                clearSpawnArea(p);
                p.respawn(now);
            } else {
                eliminated.add(p);
            }
        }
        for (AITank enemy : enemies) {
            if (enemy.isAlive() || rewarded.contains(enemy)) {
                continue;
            }
            rewarded.add(enemy);
            if (!despawned.contains(enemy)) {
                int killer = enemy.getKillerPlayerId();
                context.getScoreManager().addScore(killer,
                        enemy.getType().getScoreValue(), ScoreReason.KILL_ENEMY);
                maybeDropPowerUp(enemy, now);
            }
        }
    }

    /** 复活前清开出生区域内的敌方坦克（不给分、不掉道具），避免卡死重叠。 */
    private void clearSpawnArea(PlayerTank p) {
        Rectangle area = new Rectangle(
                (p.getSpawnCol() - 1) * GameConfig.TILE_SIZE,
                (p.getSpawnRow() - 1) * GameConfig.TILE_SIZE,
                GameConfig.TILE_SIZE * 3, GameConfig.TILE_SIZE * 3);
        for (AITank enemy : enemies) {
            if (enemy.isAlive() && area.intersects(enemy.getBounds())) {
                enemy.setAlive(false);
                despawned.add(enemy);
            }
        }
    }

    /** 奖励坦克必掉，普通坦克按概率掉落。 */
    private void maybeDropPowerUp(AITank enemy, long now) {
        boolean drop = enemy.isBonus()
                || random.nextDouble() < GameConfig.POWERUP_DROP_RATE;
        if (!drop) {
            return;
        }
        int col = GameMap.pixelToCol(enemy.getCenterX());
        int row = GameMap.pixelToRow(enemy.getCenterY());
        powerups.add(PowerUpFactory.randomCreate(col, row, now, random));
    }

    /* ----------------------------- 胜负判定 ----------------------------- */

    private void resolveOutcome() {
        if (mode.isVersus()) {
            boolean p1Out = isEliminated(1);
            boolean p2Out = isEliminated(2);
            if (p1Out && p2Out) {
                versusWinner = 0;
            } else if (p1Out) {
                versusWinner = 2;
            } else if (p2Out) {
                versusWinner = 1;
            }
            return;
        }
        if (base != null && base.isDestroyed()) {
            failed = true;
        } else if (eliminated.size() >= players.size()) {
            failed = true;
        } else if (spawnQueue.isEmpty() && pendingSpawns.isEmpty()
                && countAliveEnemies() == 0) {
            levelCleared = true;
        }
    }

    private boolean isEliminated(int playerId) {
        for (PlayerTank p : players) {
            if (p.getPlayerId() == playerId) {
                return eliminated.contains(p);
            }
        }
        return true;
    }

    private int countAliveEnemies() {
        int count = 0;
        for (AITank e : enemies) {
            if (e.isAlive()) {
                count++;
            }
        }
        return count;
    }

    /* ----------------------------- 清理回收 ----------------------------- */

    private void cleanup() {
        List<Bullet> deadBullets = new ArrayList<Bullet>();
        for (Bullet b : bullets) {
            if (!b.isAlive()) {
                deadBullets.add(b);
            }
        }
        for (Bullet b : deadBullets) {
            bulletPool.release(b);
        }
        bullets.removeAll(deadBullets);

        List<AITank> deadEnemies = new ArrayList<AITank>();
        for (AITank e : enemies) {
            if (!e.isAlive()) {
                deadEnemies.add(e);
            }
        }
        enemies.removeAll(deadEnemies);
        for (AITank e : deadEnemies) {
            brains.remove(e);
        }

        List<Explosion> doneFx = new ArrayList<Explosion>();
        for (Explosion e : explosions) {
            if (!e.isAlive()) {
                doneFx.add(e);
            }
        }
        explosions.removeAll(doneFx);

        List<Spark> doneSparks = new ArrayList<Spark>();
        for (Spark s : sparks) {
            if (!s.isAlive()) {
                doneSparks.add(s);
            }
        }
        sparks.removeAll(doneSparks);
    }

    /* ----------------------------- 特效辅助 ----------------------------- */

    /** 添加爆炸。 */
    public void addExplosion(int cx, int cy, Explosion.Scale scale) {
        explosions.add(new Explosion(cx, cy, scale));
    }

    /** 迸发一团火花粒子。 */
    public void burst(double cx, double cy, Color color, int count,
                     double speed, long lifeMs) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double v = speed * (0.4 + random.nextDouble() * 0.8);
            sparks.add(new Spark(cx, cy, Math.cos(angle) * v,
                    Math.sin(angle) * v, color, lifeMs));
        }
    }

    /** 触发屏幕震动。 */
    public void shake(long durationMs, int magnitude) {
        this.shakeUntil = System.currentTimeMillis() + durationMs;
        this.shakeMagnitude = magnitude;
    }

    /* ============================== 渲染 ============================== */

    /**
     * 绘制整个战场（含屏幕震动），HUD 由外部另行绘制。
     *
     * @param g 图形上下文
     */
    public void draw(Graphics2D g) {
        Graphics2D ctx2 = (Graphics2D) g.create();
        long now = System.currentTimeMillis();
        if (now < shakeUntil) {
            int ox = random.nextInt(shakeMagnitude * 2 + 1) - shakeMagnitude;
            int oy = random.nextInt(shakeMagnitude * 2 + 1) - shakeMagnitude;
            ctx2.translate(ox, oy);
        }
        // 底色
        ctx2.setColor(new Color(14, 16, 24));
        ctx2.fillRect(-8, -8, GameConfig.FIELD_W + 16, GameConfig.FIELD_H + 16);

        drawTilesLayer(ctx2, false);
        for (PendingSpawn ps : pendingSpawns) {
            ps.marker.draw(ctx2);
        }
        if (base != null) {
            base.draw(ctx2);
        }
        for (PowerUp p : powerups) {
            p.draw(ctx2);
        }
        for (PlayerTank p : players) {
            if (p.isAlive()) {
                p.draw(ctx2);
            }
        }
        for (AITank e : enemies) {
            if (e.isAlive()) {
                e.draw(ctx2);
            }
        }
        for (Bullet b : bullets) {
            if (b.isAlive()) {
                b.draw(ctx2);
            }
        }
        // 草丛覆盖在坦克之上
        drawTilesLayer(ctx2, true);
        for (Spark s : sparks) {
            s.draw(ctx2);
        }
        for (Explosion e : explosions) {
            e.draw(ctx2);
        }
        ctx2.dispose();
    }

    /** 绘制地形层：grassOnly=false 画普通地形，true 只画草丛覆盖。 */
    private void drawTilesLayer(Graphics2D g, boolean grassOnly) {
        int tile = GameConfig.TILE_SIZE;
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                TileType type = map.getTileType(r, c);
                int px = c * tile;
                int py = r * tile;
                if (type == TileType.GRASS) {
                    if (grassOnly) {
                        drawGrass(g, px, py);
                    }
                    continue;
                }
                if (grassOnly) {
                    continue;
                }
                switch (type) {
                    case BRICK:
                        drawBrick(g, px, py, r, c);
                        break;
                    case STEEL:
                        drawSteel(g, px, py);
                        break;
                    case WATER:
                        drawWater(g, px, py, r, c);
                        break;
                    case BASE:
                        // 基地空地底
                        g.setColor(new Color(20, 22, 30));
                        g.fillRect(px, py, tile, tile);
                        break;
                    default:
                        drawEmpty(g, px, py, r, c);
                        break;
                }
            }
        }
    }

    private void drawEmpty(Graphics2D g, int px, int py, int r, int c) {
        g.setColor(new Color(16, 18, 26));
        g.fillRect(px, py, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        if ((r + c) % 2 == 0) {
            g.setColor(new Color(20, 23, 33));
            g.fillRect(px + 1, py + 1, 2, 2);
        }
    }

    private void drawBrick(Graphics2D g, int px, int py, int r, int c) {
        g.setColor(new Color(132, 64, 36));
        g.fillRect(px, py, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        g.setColor(new Color(176, 96, 52));
        int half = GameConfig.TILE_SIZE / 2;
        for (int row = 0; row < 2; row++) {
            int offset = row == 0 ? 0 : half;
            for (int k = -1; k < 2; k++) {
                int bx = px + offset + k * GameConfig.TILE_SIZE;
                int by = py + row * half;
                g.fillRect(bx + 1, by + 1, half - 2, half - 2);
            }
        }
        // 耐久降低后的焦黑裂痕
        int hp = map.getBrickHp(r, c);
        if (hp <= GameConfig.BRICK_HP / 2) {
            g.setColor(new Color(60, 30, 20, 180));
            g.fillRect(px + 6, py + 4, 4, 12);
            g.fillRect(px + 16, py + 14, 5, 12);
        }
    }

    private void drawSteel(Graphics2D g, int px, int py) {
        g.setColor(new Color(120, 128, 140));
        g.fillRect(px, py, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        g.setColor(new Color(168, 176, 188));
        g.fillRect(px + 2, py + 2, GameConfig.TILE_SIZE - 4,
                GameConfig.TILE_SIZE / 2 - 2);
        g.setColor(new Color(84, 90, 100));
        g.drawRect(px + 1, py + 1, GameConfig.TILE_SIZE - 3,
                GameConfig.TILE_SIZE - 3);
        g.setColor(new Color(210, 216, 224));
        g.fillRect(px + 4, py + 4, 3, 3);
        g.fillRect(px + GameConfig.TILE_SIZE - 7, py + 4, 3, 3);
        g.fillRect(px + 4, py + GameConfig.TILE_SIZE - 7, 3, 3);
        g.fillRect(px + GameConfig.TILE_SIZE - 7,
                py + GameConfig.TILE_SIZE - 7, 3, 3);
    }

    private void drawWater(Graphics2D g, int px, int py, int r, int c) {
        g.setColor(new Color(32, 70, 168));
        g.fillRect(px, py, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        g.setColor(new Color(70, 120, 220));
        int shift = (int) (System.currentTimeMillis() / 300 % 8);
        g.drawLine(px + 2 + ((c + shift) % 4), py + 9,
                px + GameConfig.TILE_SIZE - 4, py + 9);
        g.drawLine(px + 2, py + 22, px + GameConfig.TILE_SIZE - 6, py + 22);
    }

    private void drawGrass(Graphics2D g, int px, int py) {
        g.setColor(new Color(42, 108, 40, 235));
        g.fillRect(px, py, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        g.setColor(new Color(74, 150, 58, 235));
        g.fillRect(px + 3, py + 3, 8, 8);
        g.fillRect(px + 18, py + 6, 10, 9);
        g.fillRect(px + 8, py + 19, 10, 9);
        g.fillRect(px + 21, py + 21, 7, 7);
    }

    /* ============================== 访问器 ============================== */

    /** @return 当前关卡序号。 */
    public int getLevelIndex() {
        return levelIndex;
    }

    /** @return 游戏模式。 */
    public GameMode getMode() {
        return mode;
    }

    /** @return 难度。 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return 地图。 */
    public GameMap getMap() {
        return map;
    }

    /** @return 玩家列表。 */
    public List<PlayerTank> getPlayers() {
        return players;
    }

    /** @return 敌方坦克列表。 */
    public List<AITank> getEnemies() {
        return enemies;
    }

    /** @return 道具效果管理器。 */
    public EffectManager getEffects() {
        return effects;
    }

    /** @return 基地（对战模式为 null）。 */
    public Base getBase() {
        return base;
    }

    /** @return 剩余敌人总数（待出场 + 在场）。 */
    public int getEnemiesRemaining() {
        return spawnQueue.size() + pendingSpawns.size() + countAliveEnemies();
    }

    /** @return 当前在场子弹数（性能监控用）。 */
    public int getBulletCount() {
        return bullets.size();
    }

    /** @return 是否单关通过。 */
    public boolean isLevelCleared() {
        return levelCleared;
    }

    /** @return 是否失败。 */
    public boolean isFailed() {
        return failed;
    }

    /** @return 对战胜方（-1 未决，0 平局，1/2 玩家）。 */
    public int getVersusWinner() {
        return versusWinner;
    }

    /**
     * 出生队列条目：出生标记 + 待生成的坦克类型。
     */
    private static final class PendingSpawn {
        private final SpawnMarker marker;
        private final TankType type;

        PendingSpawn(SpawnMarker marker, TankType type) {
            this.marker = marker;
            this.type = type;
        }
    }
}
