package com.tankwar.input;

import com.tankwar.constant.Direction;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 键盘输入管理器。
 * <p>采用 Swing Key Bindings（{@link InputMap}/{@link ActionMap}）而非
 * KeyListener，避免窗口失焦吞键；持续移动用"按下方向双端队列"表达，
 * 队尾即最后按下的方向（斜按跟随最后按键，支持双人同时操作）；开火与
 * 系统命令用边沿触发的 {@link Intent} 队列，事件在 EDT 产生、由逻辑线程
 * 消费，跨线程通过并发队列安全传递。</p>
 *
 * @author TankWar Team
 */
public class InputManager {

    /** 每个玩家当前按住的方向队列（队尾=最后按下）。 */
    private final ConcurrentLinkedDeque<Direction>[] heldDirs;
    /** 一次性意图队列。 */
    private final ConcurrentLinkedQueue<Intent> intents =
            new ConcurrentLinkedQueue<Intent>();

    /** 按住中的键集合（用于自动重复按下事件去重）。 */
    private final Map<Integer, Boolean> pressedKeys =
            new ConcurrentHashMap<Integer, Boolean>();

    /** 玩家编号在数组中的下标偏移。 */
    private static final int P1 = 0;
    private static final int P2 = 1;

    /**
     * 构造输入管理器。
     */
    @SuppressWarnings("unchecked")
    public InputManager() {
        this.heldDirs = new ConcurrentLinkedDeque[2];
        this.heldDirs[P1] = new ConcurrentLinkedDeque<Direction>();
        this.heldDirs[P2] = new ConcurrentLinkedDeque<Direction>();
    }

    /**
     * 把全部按键绑定到目标组件。
     *
     * @param component 游戏面板
     */
    public void attach(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        // 玩家 1：WASD 移动 + J 开火（方向键同时承担主菜单导航）
        bindDir(inputMap, actionMap, KeyEvent.VK_W, P1, Direction.UP,
                Intent.Kind.MENU_UP);
        bindDir(inputMap, actionMap, KeyEvent.VK_S, P1, Direction.DOWN,
                Intent.Kind.MENU_DOWN);
        bindDir(inputMap, actionMap, KeyEvent.VK_A, P1, Direction.LEFT,
                Intent.Kind.MENU_LEFT);
        bindDir(inputMap, actionMap, KeyEvent.VK_D, P1, Direction.RIGHT,
                Intent.Kind.MENU_RIGHT);
        bindFire(inputMap, actionMap, KeyEvent.VK_J, 1);

        // 玩家 2：方向键移动 + 空格开火（方向键在菜单中同样可导航）
        bindDir(inputMap, actionMap, KeyEvent.VK_UP, P2, Direction.UP,
                Intent.Kind.MENU_UP);
        bindDir(inputMap, actionMap, KeyEvent.VK_DOWN, P2, Direction.DOWN,
                Intent.Kind.MENU_DOWN);
        bindDir(inputMap, actionMap, KeyEvent.VK_LEFT, P2, Direction.LEFT,
                Intent.Kind.MENU_LEFT);
        bindDir(inputMap, actionMap, KeyEvent.VK_RIGHT, P2, Direction.RIGHT,
                Intent.Kind.MENU_RIGHT);
        bindFire(inputMap, actionMap, KeyEvent.VK_SPACE, 2);

        // 全局键
        bindCommand(inputMap, actionMap, KeyEvent.VK_P, Intent.Kind.PAUSE_TOGGLE);
        bindCommand(inputMap, actionMap, KeyEvent.VK_ESCAPE, Intent.Kind.PAUSE_TOGGLE);
        bindCommand(inputMap, actionMap, KeyEvent.VK_ENTER, Intent.Kind.CONFIRM);
        bindCommand(inputMap, actionMap, KeyEvent.VK_R, Intent.Kind.RESTART);
        bindCommand(inputMap, actionMap, KeyEvent.VK_M, Intent.Kind.MUTE_TOGGLE);
        bindCommand(inputMap, actionMap, KeyEvent.VK_Q, Intent.Kind.QUIT_TO_MENU);
        bindCommand(inputMap, actionMap, KeyEvent.VK_1, Intent.Kind.MODE_SOLO);
        bindCommand(inputMap, actionMap, KeyEvent.VK_2, Intent.Kind.MODE_COOP);
        bindCommand(inputMap, actionMap, KeyEvent.VK_3, Intent.Kind.MODE_VERSUS);
        bindCommand(inputMap, actionMap, KeyEvent.VK_E, Intent.Kind.MODE_SOLO);
    }

    /**
     * 绑定一个移动方向键（按下入队尾、释放移除）；按下边沿同时产生一条
     * 菜单导航意图，供主菜单复用方向键（游戏中该意图被忽略）。
     */
    private void bindDir(InputMap im, ActionMap am, int keyCode,
                         final int playerIndex, final Direction dir,
                         final Intent.Kind menuKind) {
        String press = "dir_p_" + keyCode;
        String release = "dir_r_" + keyCode;
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), press);
        im.put(KeyStroke.getKeyStroke(keyCode, 0, true), release);
        am.put(press, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pressedKeys.put(keyCode, Boolean.TRUE) == null) {
                    heldDirs[playerIndex].remove(dir);
                    heldDirs[playerIndex].addLast(dir);
                    intents.add(Intent.of(menuKind));
                }
            }
        });
        am.put(release, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pressedKeys.remove(keyCode);
                heldDirs[playerIndex].remove(dir);
            }
        });
    }

    /**
     * 绑定开火键（按下边沿触发）。
     */
    private void bindFire(InputMap im, ActionMap am, int keyCode,
                          final int playerId) {
        String press = "fire_" + keyCode;
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), press);
        am.put(press, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                intents.add(new Intent(Intent.Kind.FIRE, playerId));
            }
        });
    }

    /**
     * 绑定一次性系统命令键。
     */
    private void bindCommand(InputMap im, ActionMap am, int keyCode,
                             final Intent.Kind kind) {
        String key = "cmd_" + keyCode;
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), key);
        am.put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                intents.add(Intent.of(kind));
            }
        });
    }

    /**
     * 获取某玩家当前移动方向（最后按下者）。
     *
     * @param playerId 玩家编号 1/2
     * @return 方向；无按键返回 null
     */
    public Direction getMoveDirection(int playerId) {
        int index = playerId == 2 ? P2 : P1;
        return heldDirs[index].peekLast();
    }

    /**
     * 取出并清空当前全部一次性意图。
     *
     * @return 意图列表
     */
    public List<Intent> pollIntents() {
        List<Intent> result = new ArrayList<Intent>();
        Intent intent;
        while ((intent = intents.poll()) != null) {
            result.add(intent);
        }
        return result;
    }

    /** 清空所有按住方向与待处理意图（状态切换时调用，防止串键）。 */
    public void clear() {
        heldDirs[P1].clear();
        heldDirs[P2].clear();
        intents.clear();
        pressedKeys.clear();
    }
}
