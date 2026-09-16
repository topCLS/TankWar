package com.tankwar.constant;

/**
 * 全局配置常量。
 * <p>所有可调参数集中于此，杜绝魔法数字；地图基于 26&times;26 网格，
 * 每格 32 像素，战场 832&times;832，右侧固定 144 像素信息栏。</p>
 *
 * @author TankWar Team
 */
public final class GameConfig {

    private GameConfig() {
    }

    /* ---------------- 网格与窗口 ---------------- */
    /** 单个地形格边长（像素）。 */
    public static final int TILE_SIZE = 32;
    /** 地图列数。 */
    public static final int MAP_COLS = 26;
    /** 地图行数。 */
    public static final int MAP_ROWS = 26;
    /** 战场像素宽度。 */
    public static final int FIELD_W = TILE_SIZE * MAP_COLS;
    /** 战场像素高度。 */
    public static final int FIELD_H = TILE_SIZE * MAP_ROWS;
    /** 右侧信息栏宽度。 */
    public static final int HUD_W = 144;
    /** 窗口客户区宽度。 */
    public static final int WINDOW_W = FIELD_W + HUD_W;
    /** 窗口客户区高度。 */
    public static final int WINDOW_H = FIELD_H;

    /* ---------------- 实体尺寸 ---------------- */
    /** 坦克边长（略小于一格，留出转向容差）。 */
    public static final int TANK_SIZE = 28;
    /** 子弹边长。 */
    public static final int BULLET_SIZE = 8;
    /** 道具边长。 */
    public static final int POWERUP_SIZE = 26;

    /* ---------------- 速度与节奏 ---------------- */
    /** 逻辑帧率（每秒逻辑步数）。 */
    public static final int TICK_RATE = 60;
    /** 单个逻辑步长（毫秒）。 */
    public static final long STEP_MS = 1000L / TICK_RATE;
    /** 单帧最大真实耗时（毫秒），防止卡顿后"死亡螺旋"。 */
    public static final long MAX_FRAME_MS = 250L;
    /** 玩家坦克速度（像素/逻辑步）。 */
    public static final int PLAYER_SPEED = 2;
    /** 普通子弹速度（像素/逻辑步）。 */
    public static final int BULLET_SPEED = 6;
    /** 强化子弹速度。 */
    public static final int POWER_BULLET_SPEED = 8;
    /** 子弹扫掠时单个子步的最大长度（像素），保证任何速度都不穿透格子。 */
    public static final int BULLET_SUBSTEP = 8;

    /* ---------------- 生命 / 开火 ---------------- */
    /** 玩家初始生命数。 */
    public static final int PLAYER_LIVES = 3;
    /** 玩家同时存在的最大子弹数。 */
    public static final int MAX_PLAYER_BULLETS = 2;
    /** 敌方坦克同时存在的最大子弹数（每辆）。 */
    public static final int MAX_AI_BULLETS = 1;
    /** 玩家开火冷却（毫秒）。 */
    public static final long PLAYER_FIRE_COOLDOWN_MS = 320L;
    /** 复活 / 出生后的无敌时间（毫秒）。 */
    public static final long RESPAWN_INVINCIBLE_MS = 3000L;
    /** 敌方坦克出生保护时间（毫秒）。 */
    public static final long AI_SPAWN_SHIELD_MS = 1500L;

    /* ---------------- 刷怪 ---------------- */
    /** 每关敌方坦克总数。 */
    public static final int ENEMY_TOTAL_PER_LEVEL = 20;
    /** 场上同时存在的敌方坦克上限。 */
    public static final int MAX_ENEMIES_ON_FIELD = 4;
    /** 出生传送闪光持续时间（毫秒）。 */
    public static final long SPAWN_FLASH_MS = 800L;
    /** 每关奖励（必掉道具）坦克数量。 */
    public static final int BONUS_TANKS_PER_LEVEL = 4;

    /* ---------------- 道具 ---------------- */
    /** 普通敌人被击毁后掉落道具的概率。 */
    public static final double POWERUP_DROP_RATE = 0.35;
    /** 道具在战场上的存活时间（毫秒），超时消失。 */
    public static final long POWERUP_LIFETIME_MS = 12000L;

    /* ---------------- 砖墙 ---------------- */
    /** 单格砖墙的耐久度。 */
    public static final int BRICK_HP = 4;

    /* ---------------- 资源路径（classpath 根） ---------------- */
    public static final String IMAGE_DIR = "/images/";
    public static final String LEVEL_DIR = "/levels/";
    public static final String AUDIO_DIR = "/audio/";

    /** 窗口标题。 */
    public static final String WINDOW_TITLE = "坦克大战 Tank War — 面向对象程序设计课程设计";
}
