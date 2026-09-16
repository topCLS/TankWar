# -*- coding: utf-8 -*-
"""
美术资源后处理脚本（构建期工具，不属于游戏运行时代码）
v2 键控策略：
  AI 生成的品红背景带明暗噪点，阈值法残留严重。改为：
  1. 归一化色相差 t = (min(R,B) - G) / (min(R,B) + G + 1) 标记"品红相"候选；
  2. 从画布四边做 flood fill，仅在候选像素间扩散——只会删掉与边缘连通的
     品红背景，红色装甲暗部即使色相偏品，只要不与背景连通就不会被误抠；
  3. 前景腐蚀 1px 收掉紫边；
  4. 包围盒裁剪、居中正方形、缩放；爆炸精灵表四等分切帧；标题图 cover 裁剪。
运行：python tools/process_assets.py
"""
import os
from collections import deque

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "resources", "images", "_raw")
OUT = os.path.join(ROOT, "resources", "images")

# (源文件, 输出文件, 目标边长)
SPRITES = [
    ("tank_player1.png", "tank_player1.png", 96),
    ("tank_player2.png", "tank_player2.png", 96),
    ("tank_basic.png",   "enemy_basic.png",  96),
    ("tank_fast.png",    "enemy_fast.png",   96),
    ("tank_armored.png", "enemy_armored.png",96),
    ("tank_bonus.png",   "enemy_bonus.png",  96),
    ("power_speed.png",  "power_speed.png",  64),
    ("power_star.png",   "power_star.png",   64),
    ("power_shield.png", "power_shield.png", 64),
    ("eagle.png",        "base_eagle.png",   64),
]

WORK = 384          # 键控工作分辨率（长边像素）
T_HUE = 0.18        # 品红色相差阈值
V_FLOOR = 25        # min(R,B) 极低的像素不参与候选（保护近黑中性色）
PAD_RATIO = 0.04    # 主体包围盒外留白比例


def _flood_background(rgb: np.ndarray) -> np.ndarray:
    """返回与画布边缘连通的品红背景布尔掩膜。"""
    r = rgb[:, :, 0].astype(np.float32)
    g = rgb[:, :, 1].astype(np.float32)
    b = rgb[:, :, 2].astype(np.float32)
    v = np.minimum(r, b)
    hue = (v - g) / (v + g + 1.0)
    cand = (hue > T_HUE) & (v > V_FLOOR)

    h, w = cand.shape
    visited = bytearray(h * w)
    dq = deque()
    flat = cand.reshape(-1)
    for x in range(w):
        for y in (0, h - 1):
            i = y * w + x
            if flat[i] and not visited[i]:
                visited[i] = 1
                dq.append(i)
    for y in range(h):
        for x in (0, w - 1):
            i = y * w + x
            if flat[i] and not visited[i]:
                visited[i] = 1
                dq.append(i)
    while dq:
        i = dq.popleft()
        y, x = divmod(i, w)
        if y > 0:
            j = i - w
            if flat[j] and not visited[j]:
                visited[j] = 1
                dq.append(j)
        if y < h - 1:
            j = i + w
            if flat[j] and not visited[j]:
                visited[j] = 1
                dq.append(j)
        if x > 0:
            j = i - 1
            if flat[j] and not visited[j]:
                visited[j] = 1
                dq.append(j)
        if x < w - 1:
            j = i + 1
            if flat[j] and not visited[j]:
                visited[j] = 1
                dq.append(j)
    return np.asarray(visited, dtype=bool).reshape(h, w)


def chroma_key(im: Image.Image) -> Image.Image:
    """flood-fill 品红键控，输出硬边透明 PNG。"""
    w0, h0 = im.size
    scale = WORK / max(w0, h0)
    im = im.convert("RGBA").resize(
        (max(1, int(w0 * scale)), max(1, int(h0 * scale))), Image.LANCZOS)
    arr = np.asarray(im)
    rgb = arr[:, :, :3]
    bg = _flood_background(rgb)
    alpha = np.where(bg, 0, 255).astype(np.uint8)
    # 前景腐蚀 1px：收掉紧贴背景的紫/亮边（黑描边在工作尺度有 2~4px）
    a = alpha
    er = a.copy()
    er[1:-1, 1:-1] = np.minimum(np.minimum(a[:-2, 1:-1], a[2:, 1:-1]),
                                np.minimum(a[1:-1, :-2], a[1:-1, 2:]))
    alpha = er
    out = np.dstack([rgb, alpha])
    return Image.fromarray(out, "RGBA")


def cut_to_square(im: Image.Image, size: int) -> Image.Image:
    """按 alpha 包围盒裁剪，居中放入正方形画布后缩放。"""
    bbox = im.getbbox()
    if bbox is None:
        raise ValueError("抠图后主体为空")
    sub = im.crop(bbox)
    w, h = sub.size
    pad = int(max(w, h) * PAD_RATIO)
    side = max(w, h) + pad * 2
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(sub, ((side - w) // 2, (side - h) // 2), sub)
    return canvas.resize((size, size), Image.LANCZOS)


def process_sprites():
    for src, dst, size in SPRITES:
        im = chroma_key(Image.open(os.path.join(RAW, src)))
        im = cut_to_square(im, size)
        im.save(os.path.join(OUT, dst))
        print("sprite ->", dst, im.size)


def process_explosion():
    """4096x1024 精灵表 -> 4 帧 160x160。"""
    sheet = Image.open(os.path.join(RAW, "explosion_sheet.png")).convert("RGBA")
    w, h = sheet.size
    fw = w // 4
    for i in range(4):
        frame = sheet.crop((i * fw, 0, (i + 1) * fw, h))
        frame = chroma_key(frame)
        bbox = frame.getbbox()
        if bbox:
            cx = (bbox[0] + bbox[2]) // 2
            cy = (bbox[1] + bbox[3]) // 2
            side = max(bbox[2] - bbox[0], bbox[3] - bbox[1])
            pad = int(side * PAD_RATIO)
            side += pad * 2
            half = side // 2
            fw2, fh2 = frame.size
            l = min(max(cx - half, 0), max(fw2 - side, 0))
            t = min(max(cy - half, 0), max(fh2 - side, 0))
            frame = frame.crop((l, t, min(l + side, fw2), min(t + side, fh2)))
            canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
            canvas.paste(frame, (0, 0), frame)
            frame = canvas
        frame = frame.resize((160, 160), Image.LANCZOS)
        frame.save(os.path.join(OUT, f"explosion_{i}.png"))
        print("explosion frame ->", i + 1)


def process_title():
    """标题图 cover 裁剪到 976x832。"""
    tw, th = 976, 832
    im = Image.open(os.path.join(RAW, "title_bg.png")).convert("RGB")
    w, h = im.size
    s = max(tw / w, th / h)
    nw, nh = int(w * s + 0.5), int(h * s + 0.5)
    im = im.resize((nw, nh), Image.LANCZOS)
    l, t = (nw - tw) // 2, (nh - th) // 2
    im.crop((l, t, l + tw, t + th)).save(os.path.join(OUT, "title_bg.png"))
    print("title -> title_bg.png", (tw, th))


if __name__ == "__main__":
    process_sprites()
    process_explosion()
    process_title()
    print("ALL ASSETS DONE")
