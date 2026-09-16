# -*- coding: utf-8 -*-
"""
关卡生成器（构建期工具）：程序化构建 26x26 对称战场，保证
  - 尺寸严格 26x26；
  - 三个敌方出生点（左上/中上/右上）、两个玩家出生点（底部）2x2 区域清空；
  - 基地位于底部中央并由砖墙 U 形护卫；
  - 左/右镜像对称、通道连通，避免出现不可能到达的封闭区域。
符号：. 空地  1 砖墙  2 钢墙  3 草丛  4 河流  9 基地
      P 玩家1出生点  Q 玩家2出生点  E 敌方出生点
同时渲染 level_*_preview.png 便于人工检查。
运行：python tools/gen_levels.py
"""
import os

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LV = os.path.join(ROOT, "resources", "levels")
DOC = os.path.join(ROOT, "docs")
R, C = 26, 26

COLORS = {
    ".": (18, 20, 30), "1": (176, 92, 48), "2": (150, 158, 170),
    "3": (54, 128, 48), "4": (48, 96, 200), "9": (230, 200, 80),
    "P": (240, 200, 40), "Q": (90, 210, 70), "E": (210, 80, 70),
}


def blank():
    return [["." for _ in range(C)] for _ in range(R)]


def stamp(g, ch, r, c, h, w):
    """在 (r,c) 放置 h x w 的实心块，并自动镜像到右侧（c 为左半边时）。"""
    for rr in range(r, r + h):
        for cc in range(c, c + w):
            if 0 <= rr < R and 0 <= cc < C:
                g[rr][cc] = ch
    # 镜像块
    c2 = 25 - (c + w - 1)
    if c2 != c:
        for rr in range(r, r + h):
            for cc in range(c2, c2 + w):
                if 0 <= rr < R and 0 <= cc < C:
                    g[rr][cc] = ch


def checker(g, ch, r, c, h, w):
    """棋盘格块（半砖掩体风），同样镜像。"""
    for rr in range(r, r + h):
        for cc in range(c, c + w):
            if (rr + cc) % 2 == 0:
                g[rr][cc] = ch
    c2 = 25 - (c + w - 1)
    if c2 != c:
        for rr in range(r, r + h):
            for cc in range(c2, c2 + w):
                if (rr + cc) % 2 == 0 and 0 <= rr < R and 0 <= cc < C:
                    g[rr][cc] = ch


def center(g, ch, r, c, h=1, w=1):
    """中轴线附近的不镜像块。"""
    stamp_one(g, ch, r, c, h, w)


def stamp_one(g, ch, r, c, h, w):
    for rr in range(r, r + h):
        for cc in range(c, c + w):
            if 0 <= rr < R and 0 <= cc < C:
                g[rr][cc] = ch


def finalize(g):
    """清理出生区域、放置基地与出生点。"""
    # 敌方 3 个出生点（顶部），各 2x2 清空
    for c0, c1 in ((0, 1), (12, 13), (23, 24)):
        for rr in (0, 1):
            for cc in range(c0, c1 + 1):
                g[rr][cc] = "."
    for ec in (0, 12, 24):
        g[0][ec] = "E"
    # 玩家出生点（底部）
    for (pr, pc, tag) in ((24, 8, "P"), (24, 9, "."), (25, 8, "."), (25, 9, "."),
                          (24, 16, "Q"), (24, 17, "."), (25, 16, "."),
                          (25, 17, ".")):
        g[pr][pc] = tag
    # 基地 + U 形砖墙护卫
    stamp_one(g, ".", 23, 11, 3, 3)
    g[24][12] = "9"
    for cc in (11, 12, 13):
        g[23][cc] = "1"
    g[24][11] = "1"
    g[24][13] = "1"
    return g


def level1():
    g = blank()
    # 顶部砖柱
    stamp(g, "1", 3, 3, 4, 2)
    stamp(g, "1", 3, 8, 4, 2)
    # 中部横墙（留缺口）
    stamp(g, "1", 9, 1, 1, 5)
    stamp(g, "1", 9, 8, 1, 3)
    # 钢墙据点
    stamp_one(g, "2", 5, 12, 1, 2)
    stamp(g, "2", 13, 6, 2, 1)
    # 河流
    stamp(g, "4", 16, 3, 2, 4)
    # 草丛
    stamp(g, "3", 11, 10, 2, 6)
    # 底部砖阵
    checker(g, "1", 19, 2, 2, 4)
    checker(g, "1", 19, 8, 2, 3)
    stamp(g, "1", 21, 2, 1, 2)
    stamp(g, "1", 21, 22, 1, 2)
    return finalize(g)


def level2():
    g = blank()
    # 迷宫式砖墙
    stamp(g, "1", 2, 1, 2, 1)
    stamp(g, "1", 2, 4, 1, 4)
    stamp(g, "1", 4, 2, 4, 1)
    stamp(g, "1", 4, 7, 3, 1)
    # 中轴钢墙堡垒
    stamp_one(g, "2", 8, 12, 2, 2)
    # 交错钢墙
    stamp(g, "2", 10, 3, 1, 2)
    stamp(g, "2", 14, 3, 1, 2)
    # 横向河流（中轴留旱路）
    stamp_one(g, "4", 12, 0, 2, 11)
    stamp_one(g, "4", 12, 15, 2, 11)
    # 草丛迷宫
    stamp(g, "3", 6, 10, 2, 2)
    stamp(g, "3", 16, 9, 2, 2)
    # 下方砖阵
    checker(g, "1", 18, 1, 2, 5)
    stamp(g, "1", 18, 9, 2, 2)
    stamp(g, "2", 20, 4, 2, 1)
    stamp(g, "1", 21, 21, 2, 2)
    return finalize(g)


def level3():
    g = blank()
    # 重火力要塞
    stamp(g, "2", 2, 1, 2, 1)
    stamp(g, "1", 2, 3, 1, 3)
    stamp(g, "2", 2, 8, 2, 1)
    stamp(g, "1", 4, 2, 1, 5)
    # 中央钢十字
    stamp_one(g, "2", 7, 12, 4, 2)
    stamp_one(g, "2", 8, 10, 2, 6)
    # 砖堡环绕
    stamp(g, "1", 6, 4, 3, 2)
    stamp(g, "1", 6, 20, 3, 2)
    # 河流护城
    stamp_one(g, "4", 13, 1, 2, 6)
    stamp_one(g, "4", 13, 19, 2, 6)
    stamp_one(g, "4", 13, 11, 2, 1)
    stamp_one(g, "4", 13, 14, 2, 1)
    # 草丛通道
    stamp(g, "3", 12, 8, 2, 2)
    stamp(g, "3", 16, 12, 2, 2)
    # 钢墙 + 砖混合下段
    stamp(g, "2", 18, 2, 1, 2)
    checker(g, "1", 19, 5, 2, 4)
    stamp(g, "2", 18, 23, 2, 1)
    stamp(g, "1", 21, 8, 1, 3)
    stamp(g, "1", 21, 15, 1, 3)
    return finalize(g)


def validate(g, name):
    assert len(g) == R, f"{name} 行数错误"
    for i, row in enumerate(g):
        assert len(row) == C, f"{name} 第{i}行宽 {len(row)}"
    flat = "".join("".join(r) for r in g)
    assert flat.count("9") == 1, f"{name} 基地数量 {flat.count('9')}"
    assert flat.count("P") == 1 and flat.count("Q") == 1
    assert flat.count("E") == 3, f"{name} 敌方出生点 {flat.count('E')}"


def render(g, path):
    scale = 20
    img = Image.new("RGB", (C * scale, R * scale), (18, 20, 30))
    arr = np.asarray(img).copy()
    for r in range(R):
        for c in range(C):
            col = COLORS[g[r][c]]
            arr[r * scale:(r + 1) * scale, c * scale:(c + 1) * scale] = col
    Image.fromarray(arr).save(path)


def main():
    os.makedirs(LV, exist_ok=True)
    os.makedirs(DOC, exist_ok=True)
    for i, fn in enumerate((level1, level2, level3), start=1):
        g = fn()
        validate(g, f"level{i}")
        text = "\n".join("".join(row) for row in g)
        with open(os.path.join(LV, f"level_{i}.txt"), "w",
                  encoding="utf-8") as f:
            f.write(text + "\n")
        render(g, os.path.join(DOC, f"level_{i}_preview.png"))
        print(f"level_{i}.txt ok, preview rendered")
    print("LEVELS DONE")


if __name__ == "__main__":
    main()
