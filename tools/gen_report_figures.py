# -*- coding: utf-8 -*-
"""
生成课程设计报告所需的技术配图：
  fig_arch.png     功能架构分层图
  fig_package.png  包图
  fig_class.png    核心类图（UML）
  fig_state.png    游戏状态机图
  fig_gitlog.png   Git 提交记录图
全部使用中文字体、扁平化配色；本人负责全部模块，架构图整体浅黄底标注。
"""
import os
import subprocess

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "docs", "figures")
os.makedirs(OUT, exist_ok=True)

# 配色
INK = "#1f2d3d"
BLUE = "#dae8fc"
BLUE_B = "#6c8ebf"
GREEN = "#d5e8d4"
GREEN_B = "#82b366"
ORANGE = "#ffe6cc"
ORANGE_B = "#d79b00"
YELLOW = "#fff2cc"
YELLOW_B = "#d6b656"
PURPLE = "#e1d5e7"
PURPLE_B = "#9673a6"
GRAY = "#f5f5f5"
GRAY_B = "#999999"


def box(ax, x, y, w, h, text, fc, ec, fs=11, bold=False, tc=INK, rounded=0.02):
    p = FancyBboxPatch((x, y), w, h,
                       boxstyle=f"round,pad=0.004,rounding_size={rounded}",
                       linewidth=1.4, edgecolor=ec, facecolor=fc, zorder=2)
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            fontsize=fs, color=tc, zorder=3,
            fontweight="bold" if bold else "normal", linespacing=1.35)
    return (x + w / 2, y + h / 2)


def arrow(ax, p1, p2, color="#5b6b7b", style="-|>", lw=1.3, ls="-"):
    a = FancyArrowPatch(p1, p2, arrowstyle=style, mutation_scale=13,
                        linewidth=lw, color=color, linestyle=ls, zorder=1,
                        shrinkA=2, shrinkB=2)
    ax.add_patch(a)


def save(fig, name):
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print("saved", path)


# ---------------------------------------------------------------- 图1 架构图
def fig_arch():
    fig, ax = plt.subplots(figsize=(9.2, 7.4))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")

    layers = [
        ("输入层  input", 88, BLUE, BLUE_B,
         ["InputManager 键盘采集", "Intent 并发意图队列", "P1:WASD+J  P2:方向键+空格"]),
        ("表现层  render", 72, GREEN, GREEN_B,
         ["GamePanel 双缓冲渲染", "HUD 信息栏 / FPS", "Explosion 爆炸 · Spark 粒子 · SpawnMarker"]),
        ("框架层  core.state 状态模式", 56, YELLOW, YELLOW_B,
         ["ReadyState 主菜单", "RunningState 战斗", "PausedState 暂停",
          "LevelClearState 过关", "GameOverState 结算", "GameContext 状态拥有者"]),
        ("逻辑层  core", 40, ORANGE, ORANGE_B,
         ["World 世界规则（碰撞/刷怪/胜负）", "GameLoop 60TPS 固定步长累加器",
          "ScoreManager 计分与最高分持久化"]),
        ("智能层  ai", 24, PURPLE, PURPLE_B,
         ["AIBrain 巡逻/追踪/攻击 三态FSM", "PathFinder  BFS 追击 · A* 打基地", "VisionUtil 视野与草丛遮挡"]),
        ("对象 / 地图 / 道具 / 工具", 8, GRAY, GRAY_B,
         ["model: PlayerTank AITank Bullet Base", "map: GameMap LevelManager 文本关卡",
          "prop: 三类道具 + 注册工厂 + 限时效果", "util: 碰撞检测 · 子弹对象池 · 资源/音频"]),
    ]
    for name, y, fc, ec, items in layers:
        ax.add_patch(FancyBboxPatch((2, y), 96, 13.2,
                     boxstyle="round,pad=0.004,rounding_size=0.02",
                     linewidth=1.6, edgecolor=ec, facecolor=fc, zorder=2))
        ax.text(4, y + 9.6, name, ha="left", va="center", fontsize=11.5,
                fontweight="bold", color=INK)
        line = "    ".join(items)
        ax.text(4, y + 4.4, line, ha="left", va="center", fontsize=9.6, color=INK)

    for y in (88, 72, 56, 40, 24):
        arrow(ax, (50, y), (50, y - 2.8), color="#7a8794", lw=1.5)
        arrow(ax, (50, y - 2.8), (50, y), color="#b9c2cc", lw=1.2, ls=(0, (4, 3)))

    ax.text(50, 98.2, "坦克大战 功能架构图（分层）", ha="center", fontsize=15,
            fontweight="bold")
    ax.text(99, 1.5, "黄色框架层为核心设计：GoF 状态模式；全系统均由本人独立设计实现",
            ha="right", fontsize=8.6, color="#8a6d1a")
    save(fig, "fig_arch.png")


# ---------------------------------------------------------------- 图2 包图
def fig_package():
    fig, ax = plt.subplots(figsize=(9.0, 6.6))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    ax.text(50, 96, "com.tankwar 包结构与依赖关系", ha="center",
            fontsize=15, fontweight="bold")

    cx, cy = 50, 50
    box(ax, 40, 44, 20, 12, "com.tankwar\n(Main 入口)", ORANGE, ORANGE_B, 11, True)

    mods = [
        ("constant\n配置与枚举", 8, 78, BLUE, BLUE_B),
        ("model\n游戏对象", 38, 80, GREEN, GREEN_B),
        ("map\n地图关卡", 68, 78, GREEN, GREEN_B),
        ("ai\n寻路与敌控AI", 88, 52, PURPLE, PURPLE_B),
        ("prop\n道具系统", 82, 20, YELLOW, YELLOW_B),
        ("core(.state)\n循环/世界/状态", 46, 10, ORANGE, ORANGE_B),
        ("render\n渲染表现", 10, 22, BLUE, BLUE_B),
        ("input\n键盘输入", 2, 52, BLUE, BLUE_B),
        ("util\n资源/碰撞/池/音频", 24, 58, GRAY, GRAY_B),
    ]
    centers = {}
    for name, x, y, fc, ec in mods:
        centers[name.split("\n")[0]] = box(ax, x - 9, y - 6, 18, 12, name, fc, ec, 10.2, True)

    core = centers["core(.state)"]
    for key in ["input", "render", "ai", "prop", "map"]:
        arrow(ax, centers[key], core, color=BLUE_B, lw=1.2)
    for key in ["model", "util", "constant"]:
        arrow(ax, centers[key], (cx, cy + 6), color="#9aa7b4", lw=1.1, ls=(0, (4, 3)))
    arrow(ax, centers["ai"], centers["model"], color=PURPLE_B, lw=1.1)
    ax.text(2, 3, "实线：直接调用/驱动    虚线：被广泛依赖的基础包", fontsize=8.8,
            color="#666")
    save(fig, "fig_package.png")


# ---------------------------------------------------------------- 图3 类图
def uml(ax, x, y, w, title, attrs, methods, fc, ec, title_fs=10.5, body_fs=8.8):
    n = len(attrs) + len(methods) + 2
    h = 3.0 + 1.9 * n
    ax.add_patch(FancyBboxPatch((x, y - h), w, h,
                 boxstyle="round,pad=0.004,rounding_size=0.015",
                 linewidth=1.4, edgecolor=ec, facecolor="white", zorder=2))
    ax.add_patch(plt.Rectangle((x, y - 3.4), w, 3.4, facecolor=fc,
                 edgecolor=ec, linewidth=1.4, zorder=3))
    ax.text(x + w / 2, y - 1.7, title, ha="center", va="center",
            fontsize=title_fs, fontweight="bold", zorder=4)
    yy = y - 5.0
    for a in attrs:
        ax.text(x + 0.5, yy, a, ha="left", va="center", fontsize=body_fs, zorder=4)
        yy -= 1.9
    ax.plot([x, x + w], [yy + 0.55, yy + 0.55], color=ec, lw=1.0, zorder=4)
    for m in methods:
        ax.text(x + 0.5, yy, m, ha="left", va="center", fontsize=body_fs, zorder=4)
        yy -= 1.9
    return (x, y, w, h)


def inherit(ax, child_xy, parent_xy, color="#5b6b7b"):
    # UML 泛化：空心三角箭头指向父类
    a = FancyArrowPatch(child_xy, parent_xy, arrowstyle="-|>",
                        mutation_scale=15, linewidth=1.3, color=color,
                        shrinkA=0, shrinkB=0, zorder=1)
    ax.add_patch(a)


def fig_class():
    fig, ax = plt.subplots(figsize=(9.6, 7.4))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    ax.text(50, 97, "核心类图（节选关键类与关系）", ha="center",
            fontsize=15, fontweight="bold")

    # —— 左：坦克对象继承树（单列，无交叉）——
    go = uml(ax, 6, 84, 24, "GameObject", ["#x,y,w,h : int", "#alive : boolean"],
             ["getBounds()", "draw(Graphics2D)"], GRAY, GRAY_B, 10, 8.6)
    tk = uml(ax, 6, 60, 24, "Tank", ["#dir,#speed,#hp", "#invincibleUntilMs"],
             ["move()/takeDamage()", "fire() : Bullet"], GREEN, GREEN_B, 10, 8.6)
    uml(ax, 1.5, 30, 13.5, "PlayerTank", ["#playerId,#lives"],
        ["loseLife()", "respawn()"], BLUE, BLUE_B, 9.5, 8.2)
    uml(ax, 17.5, 30, 13.5, "AITank", ["#type,#difficulty", "#state,#killerId"],
        ["setKiller()"], PURPLE, PURPLE_B, 9.5, 8.2)
    # GameObject <- Tank；Tank <- Player / AI（箭头指向父类）
    inherit(ax, (18, 60), (18, 63.0))
    inherit(ax, (8.2, 30), (13, 49.6))
    inherit(ax, (24.2, 30), (23, 49.6))
    ax.text(18, 61.6, "", fontsize=1)
    # Bullet / Base 同为 GameObject 子类，置于右上对象区，用汇流母线连接
    uml(ax, 38, 84, 13, "Bullet", ["#vx,#vy,#power", "#owner"],
        ["step()", "recycle()"], GRAY, GRAY_B, 9.3, 8.0)
    uml(ax, 53, 84, 13, "Base", ["#destroyed"],
        ["destroy()"], GRAY, GRAY_B, 9.3, 8.0)
    # 母线：两子类顶部 -> 水平母线 -> GameObject 右侧
    ax.plot([44.5, 47.0], [84, 84], color="#5b6b7b", lw=1.2, zorder=1)
    ax.plot([59.5, 47.0], [84, 84], color="#5b6b7b", lw=1.2, zorder=1)
    ax.plot([47.0, 47.0], [84, 86.5], color="#5b6b7b", lw=1.2, zorder=1)
    inherit(ax, (47, 86.5), (30, 82.0))

    # —— 中：World ——
    uml(ax, 38, 60, 28, "World", ["#players,#enemies,#bullets,#base",
        "#map,#effects,#scoreMgr", "#nextSpawnAt"], [
        "update(dt,now)", "runAiDecisions(now)", "requestFire(playerId)",
        "resolveDeaths(now)", "isFailed()/isLevelCleared()"], ORANGE, ORANGE_B,
        9.8, 8.4)
    ax.annotate("", xy=(38, 47), xytext=(30, 40),
                arrowprops=dict(arrowstyle="->", color="#5b6b7b", lw=1.2))
    ax.text(30.5, 45.5, "聚合", fontsize=8, color="#5b6b7b", rotation=58)
    ax.text(38.5, 36.5, "玩家/敌人/子弹/基地/地图", fontsize=7.8, color="#8a5a1a")

    # —— 右上：状态模式（汇流母线，线不穿框）——
    uml(ax, 70, 80, 28, "«interface» GameState", [],
        ["enter(ctx)", "update(ctx,dt)", "render(g)", "handleInput(ctx)"],
        YELLOW, YELLOW_B, 10, 8.6)
    state_mids = []
    for i, nm in enumerate(["ReadyState", "RunningState", "PausedState",
                            "LevelClearState", "GameOverState"]):
        top = 65 - i * 7.0
        ax.add_patch(FancyBboxPatch((72, top - 4.6), 20, 4.8,
                     boxstyle="round,pad=0.004,rounding_size=0.02",
                     linewidth=1.2, edgecolor=YELLOW_B, facecolor="white", zorder=2))
        ax.text(82, top - 2.2, nm, ha="center", va="center", fontsize=9.0, zorder=3)
        state_mids.append(top - 2.3)
    bus_x = 95.0
    bot_state_y = state_mids[-1]
    for my in state_mids:
        ax.plot([92, bus_x], [my, my], color="#5b6b7b", lw=1.1, zorder=1)
    inherit(ax, (bus_x, bot_state_y), (bus_x, 65.7))
    ax.text(82, bot_state_y - 4.2, "五状态实现接口（语义为实现）",
            ha="center", fontsize=7.8, color="#8a6d1a")

    # —— 下：工厂 + 池 ——
    uml(ax, 8, 17, 26, "PowerUpFactory", ["#registry : Map<Type,Supplier>"],
        ["register(type,creator)", "create(type,...)", "randomCreate(...)"],
        BLUE, BLUE_B, 9.6, 8.4)
    uml(ax, 40, 17, 26, "ObjectPool<T>", ["#idle : Deque<T>", "#factory"],
        ["borrow() : T", "recycle(T)"], GRAY, GRAY_B, 9.6, 8.4)
    ax.text(84, 11.5, "# 表示 protected\n三角箭头=继承\n普通箭头=调用/聚合",
            ha="left", va="center", fontsize=8.0, color="#666", linespacing=1.5)
    save(fig, "fig_class.png")


# ---------------------------------------------------------------- 图4 状态机
def fig_state():
    fig, ax = plt.subplots(figsize=(9.2, 5.4))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    ax.text(50, 95, "游戏状态机（GoF State 模式）", ha="center",
            fontsize=15, fontweight="bold")

    pos = {
        "ReadyState\n主菜单": (12, 62, BLUE, BLUE_B),
        "RunningState\n战斗进行": (44, 62, GREEN, GREEN_B),
        "PausedState\n暂停": (44, 20, YELLOW, YELLOW_B),
        "LevelClearState\n过关": (78, 62, ORANGE, ORANGE_B),
        "GameOverState\n失败/胜利结算": (44, 88, PURPLE, PURPLE_B),
    }
    c = {}
    for name, (x, y, fc, ec) in pos.items():
        c[name] = box(ax, x, y - 7, 20, 14, name, fc, ec, 10.6, True)

    def mid(name):
        return c[name]

    arrow(ax, (32, 62), (44, 62)); ax.text(38, 65, "选择模式\nEnter", ha="center", fontsize=8.6)
    arrow(ax, (54, 55), (54, 34)); ax.text(57, 45, "P / Esc", fontsize=8.8)
    arrow(ax, (50, 34), (50, 55)); ax.text(42, 45, "P / Esc", fontsize=8.8, ha="right")
    arrow(ax, (64, 66), (78, 66)); ax.text(71, 70, "全歼且有下一关", ha="center", fontsize=8.4)
    arrow(ax, (78, 58), (58, 58)); ax.text(70, 53.5, "Enter 进入下一关", ha="center", fontsize=8.4)
    arrow(ax, (54, 69), (54, 81)); ax.text(62, 76, "基地毁/命尽 或 通关", fontsize=8.6)
    arrow(ax, (44, 84), (26, 69)); ax.text(26, 84, "Enter 返回菜单", fontsize=8.6)
    arrow(ax, (78, 69), (58, 86)); ax.text(74, 84, "最后一关全歼(胜利)", fontsize=8.2)
    ax.text(50, 6, "状态迁移由 GameContext.setState 统一驱动，逻辑层无 switch(state) 分支",
            ha="center", fontsize=8.8, color="#666")
    save(fig, "fig_state.png")


# ---------------------------------------------------------------- 图5 Git 记录
def fig_git():
    try:
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        out = subprocess.run(["git", "-C", root, "log",
                              "--pretty=format:%h  %ad  %s", "--date=short"],
                             capture_output=True, text=True, encoding="utf-8")
        lines = [ln for ln in out.stdout.splitlines() if ln.strip()]
        # git log 是最新在上，反转为时间从早到晚展示
        lines = lines[::-1]
    except Exception:
        lines = []
    if not lines:
        lines = ["(git 不可用)"]

    n = len(lines)
    fig_h = max(3.2, 0.42 * n + 1.4)
    fig, ax = plt.subplots(figsize=(9.6, fig_h))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    ax.add_patch(plt.Rectangle((0, 0), 100, 100, facecolor="#1e1e2e", zorder=0))
    ax.text(1.5, 96, "TankWar  Git 提交记录（git log，main 分支，共 %d 次提交）" % n,
            ha="left", va="top", fontsize=10.5, color="#cdd6f4",
            family=["Consolas", "Microsoft YaHei"], zorder=2)
    y = 88
    for ln in lines:
        ax.text(1.5, y, "$ " + ln, ha="left", va="top", fontsize=9.4,
                color="#a6e3a1", family=["Consolas", "Microsoft YaHei"], zorder=2)
        y -= 88.0 / max(n, 1)
    save(fig, "fig_gitlog.png")


if __name__ == "__main__":
    fig_arch()
    fig_package()
    fig_class()
    fig_state()
    fig_git()
    print("ALL FIGURES DONE ->", OUT)
