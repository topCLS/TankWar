package com.tankwar.constant;

/**
 * 四方向枚举（项目最核心的枚举）。
 * <p>每个方向携带单位像素增量 {@code dx, dy}，把"数据 + 行为"绑定，
 * 替代裸数字 0/1/2/3，编译期类型安全。</p>
 *
 * @author TankWar Team
 */
public enum Direction {
    /** 上：y 递减。 */
    UP(0, -1),
    /** 下：y 递增。 */
    DOWN(0, 1),
    /** 左：x 递减。 */
    LEFT(-1, 0),
    /** 右：x 递增。 */
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** @return 水平方向像素增量（-1/0/1）。 */
    public int getDx() {
        return dx;
    }

    /** @return 垂直方向像素增量（-1/0/1）。 */
    public int getDy() {
        return dy;
    }

    /** @return 相反方向。 */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            default:
                return LEFT;
        }
    }

    /** @return 是否为水平方向（左/右）。 */
    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }

    /**
     * 由像素增量反推方向。
     *
     * @param dx 水平增量
     * @param dy 垂直增量
     * @return 对应方向；无法识别时回退为 UP
     */
    public static Direction fromDelta(int dx, int dy) {
        if (dx > 0) {
            return RIGHT;
        }
        if (dx < 0) {
            return LEFT;
        }
        if (dy > 0) {
            return DOWN;
        }
        return UP;
    }
}
