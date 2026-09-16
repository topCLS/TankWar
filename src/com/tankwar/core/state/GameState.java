package com.tankwar.core.state;

import com.tankwar.core.GameContext;
import com.tankwar.input.Intent;

import java.awt.Graphics2D;
import java.util.List;

/**
 * 游戏状态接口（GoF 状态模式）。
 * <p>菜单/进行/暂停/过关/结束各自实现本接口，状态间迁移只能通过
 * {@link GameContext#setState} 触发，任何状态类内部都不允许出现
 * switch(stateEnum) 式分支——新增状态只需增加实现类，符合开闭原则。</p>
 *
 * @author TankWar Team
 */
public interface GameState {

    /** 进入状态时的初始化。 */
    void onEnter(GameContext context);

    /** 处理本逻辑步收到的一次性意图。 */
    void handleInput(GameContext context, List<Intent> intents);

    /** 推进一个固定逻辑步。 */
    void update(GameContext context, long dtMs);

    /** 渲染本状态画面。 */
    void render(GameContext context, Graphics2D g);

    /** 离开状态时的清理。 */
    void onExit(GameContext context);
}
