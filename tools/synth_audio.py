# -*- coding: utf-8 -*-
"""
8-bit 风格音效 / BGM 程序化合成（构建期工具）。
纯标准库 wave + math 生成 16bit 单声道 22050Hz WAV，
避免任何第三方音频版权问题；输出到 resources/audio。
音色：方波（主旋律）、噪声（爆炸）、三角波（柔和底音）。
运行：python tools/synth_audio.py
"""
import math
import os
import struct
import wave

SR = 22050
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "resources", "audio")

# 音名 -> 频率（以 A4=440 十二平均律）
NOTE = {}
_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
for octv in range(2, 7):
    for i, n in enumerate(_NAMES):
        midi = (octv + 1) * 12 + i
        NOTE[f"{n}{octv}"] = 440.0 * 2 ** ((midi - 69) / 12.0)


def _envelope(i, n, attack=0.01, release=0.08):
    a = int(attack * SR)
    r = int(release * SR)
    if i < a:
        return i / max(a, 1)
    if i > n - r:
        return max(0.0, (n - i) / max(r, 1))
    return 1.0


def square(freq, t, duty=0.5):
    return 1.0 if (t * freq) % 1.0 < duty else -1.0


def triangle(freq, t):
    return 2.0 * abs(2.0 * ((t * freq) % 1.0) - 1.0) - 1.0


def noise(i):
    # 确定性伪噪声，避免 random 不可复现
    x = (i * 1103515245 + 12345) & 0x7FFFFFFF
    x ^= x >> 13
    return ((x & 0xFFFF) / 32768.0) - 1.0


def write_wav(name, samples, vol=0.5):
    data = bytearray()
    for i, s in enumerate(samples):
        v = int(max(-1.0, min(1.0, s * vol)) * 32767)
        data += struct.pack("<h", v)
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(data))
    print("audio ->", name, f"{len(samples)/SR:.2f}s")


def sweep(f0, f1, dur, wave_fn=square, vol=0.6):
    n = int(dur * SR)
    out = []
    for i in range(n):
        t = i / SR
        f = f0 + (f1 - f0) * (i / n)
        e = _envelope(i, n, 0.005, dur * 0.4)
        out.append(wave_fn(f, t) * e * vol)
    return out


def blast(dur, vol=0.8, lp=0.0):
    n = int(dur * SR)
    out = []
    last = 0.0
    for i in range(n):
        x = noise(i)
        last = last * lp + x * (1 - lp)
        t = i / n
        e = (1.0 - t) ** 1.5
        out.append(last * e * vol)
    return out


def ping(freq, dur, vol=0.5):
    n = int(dur * SR)
    out = []
    for i in range(n):
        t = i / SR
        e = math.exp(-t * 18)
        out.append((square(freq, t) * 0.6 + square(freq * 2.01, t) * 0.3) * e * vol)
    return out


def arp(notes, each=0.075, wave_fn=square, vol=0.55):
    out = []
    for name in notes:
        f = NOTE[name]
        n = int(each * SR)
        for i in range(n):
            t = i / SR
            e = _envelope(i, n, 0.01, 0.03)
            out.append(wave_fn(f, t) * e * vol)
    return out


def melody(score, lead_vol=0.42, bass_vol=0.22):
    """score: [(note_or_None, beats, wavefn)]，BPM=132，8 分=0.227s。"""
    b8 = 60.0 / 132 / 2
    out = []
    bass_step = 0
    for item in score:
        name, beats, wf = item
        dur = beats * b8
        n = int(dur * SR)
        f = NOTE.get(name) if name else None
        for i in range(n):
            t = i / SR
            e = _envelope(i, n, 0.008, 0.05)
            s = wf(f, t) * e * lead_vol if f else 0.0
            # 每拍一个低音根音
            if bass_step % 8 == 0:
                pass
            out.append(s)
        bass_step += int(beats * 2)
    return out


def build():
    os.makedirs(OUT, exist_ok=True)
    write_wav("fire.wav", sweep(420, 90, 0.13, square, 0.5))
    write_wav("brick.wav", blast(0.10, 0.7, 0.2))
    write_wav("steel.wav", ping(2600, 0.16, 0.4))
    write_wav("explode_small.wav", blast(0.28, 0.8, 0.35))
    write_wav("explode_big.wav",
              [0.7 * a + 0.4 * b for a, b in
               zip(blast(0.55, 0.9, 0.4), sweep(160, 40, 0.5, triangle, 0.5))])
    write_wav("powerup.wav", arp(["C5", "E5", "G5", "C6", "E6"], 0.07))
    write_wav("levelclear.wav",
              arp(["G4", "C5", "E5", "G5", "C6", "G5", "C6"], 0.11, triangle, 0.6))
    write_wav("gameover.wav",
              arp(["E4", "D4", "C4", "G3", "C4"], 0.16, triangle, 0.6))
    write_wav("start.wav",
              arp(["C4", "G4", "C5", "E5", "G5"], 0.09, square, 0.55))

    # 循环 BGM：雄壮进行曲风格 A 段 + B 段（方波主旋律 + 噪声军鼓）
    a1 = [("C5", 1, square), ("E5", 1, square), ("G5", 1, square),
          ("E5", 1, square), ("C5", 1, square), ("G4", 1, square),
          ("C5", 2, square)]
    a2 = [("D5", 1, square), ("F5", 1, square), ("A5", 1, square),
          ("F5", 1, square), ("D5", 1, square), ("A4", 1, square),
          ("D5", 2, square)]
    a3 = [("E5", 1, square), ("G5", 1, square), ("C6", 1, square),
          ("G5", 1, square), ("E5", 1, square), ("C5", 1, square),
          ("G5", 2, square)]
    a4 = [("C5", 1, square), ("E5", 1, square), ("G5", 2, square),
          ("E5", 1, square), ("C5", 1, square), ("C5", 2, square)]
    bgm = melody(a1 + a2 + a3 + a4)
    write_wav("bgm.wav", bgm, 0.7)
    print("AUDIO DONE")


if __name__ == "__main__":
    build()
