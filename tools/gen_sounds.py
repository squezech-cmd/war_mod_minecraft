#!/usr/bin/env python3
"""Procedural PLACEHOLDER sound pack (OGG Vorbis). Every file is meant to be replaced
with a real recording - see README.md "Заменить" section."""
import os

import numpy as np
import soundfile as sf

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "warmod", "sounds")
os.makedirs(ROOT, exist_ok=True)
SR = 44100
rng = np.random.default_rng(1337)


def envelope(n, attack=0.004, release=0.25, power=2.0):
    env = np.ones(n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    env[:a] = np.linspace(0, 1, a)
    tail = np.linspace(1, 0, min(r, n)) ** power
    env[-len(tail):] = tail
    return env


def lowpass(x, window=8):
    kernel = np.ones(window) / window
    return np.convolve(x, kernel, mode="same")


def noise(n):
    return rng.normal(0, 1, n)


def gunshot(duration=0.42, thump=95.0, brightness=0.65, crack=0.5):
    n = int(duration * SR)
    t = np.arange(n) / SR
    body = noise(n) * np.exp(-t * 18.0)
    body = lowpass(body, 4) * crack + noise(n) * np.exp(-t * 60.0) * (1 - crack) * 0.6
    thump_wave = np.sin(2 * np.pi * thump * t) * np.exp(-t * 22.0) * 0.9
    tail = lowpass(noise(n), 24) * np.exp(-t * 5.0) * 0.35
    signal = body * brightness + thump_wave + tail
    return signal * envelope(n, 0.001, duration * 0.8, 1.6)


def click(duration=0.06, freq=1800.0):
    n = int(duration * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * freq * t) * 0.4 + noise(n) * 0.5) * np.exp(-t * 90.0)


def explosion(duration=2.2, power=1.0):
    n = int(duration * SR)
    t = np.arange(n) / SR
    rumble = lowpass(noise(n), 40) * np.exp(-t * 2.2) * 1.6
    crack = noise(n) * np.exp(-t * 26.0) * 0.9
    sub = np.sin(2 * np.pi * 42 * t) * np.exp(-t * 3.0) * 0.8
    return (rumble + crack + sub) * power * envelope(n, 0.002, duration * 0.9, 1.2)


def sweep(duration, f0, f1, amp=0.5):
    n = int(duration * SR)
    t = np.arange(n) / SR
    freq = np.linspace(f0, f1, n)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    return np.sin(phase) * amp * envelope(n, 0.02, duration * 0.5, 1.0)


def riffle_shots(count, spread=0.35, volume=0.5):
    total = int((count * spread + 0.8) * SR)
    out = np.zeros(total)
    for index in range(count):
        shot = gunshot(0.5, thump=rng.uniform(70, 110), brightness=0.4, crack=0.3) * volume
        start = int(index * spread * SR + rng.uniform(0, 0.05) * SR)
        end = min(total, start + len(shot))
        out[start:end] += shot[:end - start]
    return lowpass(out, 16)


def engine(duration=2.0, base=68.0, roughness=0.4, hum=0.3):
    n = int(duration * SR)
    t = np.arange(n) / SR
    wave = np.sin(2 * np.pi * base * t) * 0.5
    wave += np.sin(2 * np.pi * base * 2.02 * t) * 0.25
    wave += np.sin(2 * np.pi * base * 3.01 * t) * 0.12
    wave += lowpass(noise(n), 6) * roughness * 0.6
    wave += np.sin(2 * np.pi * base * 8.5 * t) * hum * 0.1
    # loop friendly fade
    fade = int(0.05 * SR)
    wave[:fade] *= np.linspace(0, 1, fade)
    wave[-fade:] *= np.linspace(1, 0, fade)
    return wave * 0.45


def music(duration=40.0):
    n = int(duration * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for freq, amp in ((55.0, 0.30), (82.5, 0.18), (110.0, 0.12), (164.8, 0.06)):
        out += np.sin(2 * np.pi * freq * t + np.sin(2 * np.pi * 0.07 * t) * 2.0) * amp
    out *= 0.55 + 0.45 * np.sin(2 * np.pi * 0.035 * t)
    # distant war drums / booms
    for start in (6.0, 13.5, 22.0, 31.0, 37.5):
        boom = explosion(1.8, 0.35)
        index = int(start * SR)
        end = min(n, index + len(boom))
        out[index:end] += boom[:end - index] * 0.5
    # air / wind layer
    out += lowpass(noise(n), 90) * 0.05
    fade = int(2.0 * SR)
    out[:fade] *= np.linspace(0, 1, fade)
    out[-fade:] *= np.linspace(1, 0, fade)
    peak = np.max(np.abs(out)) or 1.0
    return out / peak * 0.55


def mix(*parts):
    length = max(len(p) for p in parts)
    out = np.zeros(length)
    for part in parts:
        out[:len(part)] += part
    return out


def save(name, data):
    data = np.asarray(data, dtype=np.float64)
    peak = np.max(np.abs(data)) or 1.0
    data = data / peak * 0.85
    sf.write(os.path.join(ROOT, name + ".ogg"), data.astype(np.float32), SR, format="OGG", subtype="VORBIS")


def main():
    save("gunshot_pistol", gunshot(0.30, 130, 0.7, 0.55))
    save("gunshot_smg", gunshot(0.26, 145, 0.75, 0.6))
    save("gunshot_rifle", gunshot(0.40, 95, 0.65, 0.5))
    save("gunshot_mg", gunshot(0.38, 88, 0.6, 0.45))
    save("gunshot_sniper", gunshot(0.65, 70, 0.55, 0.4) * 1.1)
    save("gunshot_50cal", gunshot(0.8, 58, 0.5, 0.35) * 1.2)
    save("gunshot_shotgun", mix(gunshot(0.55, 78, 0.45, 0.3), explosion(0.35, 0.25)))
    save("gunshot_launcher", explosion(1.0, 0.7))

    clicks = np.concatenate([click(0.05, 1500), np.zeros(int(0.12 * SR)), click(0.06, 1100),
                             np.zeros(int(0.1 * SR)), click(0.07, 2100)])
    save("reload_rifle", clicks)
    save("reload_pistol", np.concatenate([click(0.05, 2000), np.zeros(int(0.09 * SR)), click(0.06, 1600)]))
    heavy = np.concatenate([click(0.08, 800), np.zeros(int(0.2 * SR)), click(0.09, 600),
                            np.zeros(int(0.15 * SR)), lowpass(noise(int(0.2 * SR)), 30) * 0.4])
    save("reload_heavy", heavy)

    save("dry_fire", click(0.07, 900) * 1.2)
    save("melee_swing", lowpass(noise(int(0.22 * SR)), 6) * envelope(int(0.22 * SR), 0.02, 0.2, 1.3))
    save("melee_hit", mix(lowpass(noise(int(0.18 * SR)), 12) * envelope(int(0.18 * SR), 0.001, 0.15, 2.0),
                          np.sin(2 * np.pi * 120 * np.arange(int(0.18 * SR)) / SR) * 0.4))
    save("bullet_impact", mix(click(0.05, 2600), lowpass(noise(int(0.05 * SR)), 4) * 0.5))
    save("headshot", mix(sweep(0.18, 2400, 1200, 0.5), click(0.05, 3000)))
    save("throw", lowpass(noise(int(0.3 * SR)), 10) * envelope(int(0.3 * SR), 0.03, 0.25, 1.2))

    save("explosion_small", explosion(1.2, 0.8))
    save("explosion_large", explosion(2.4, 1.3))
    save("grenade_bounce", np.concatenate([click(0.05, 1200), np.zeros(int(0.12 * SR)), click(0.05, 900)]))

    save("ambient_far_gunfire", riffle_shots(9, 0.5, 0.28))
    save("ambient_jet_pass", np.concatenate([lowpass(noise(int(1.2 * SR)), 200) * np.linspace(0, 0.6, int(1.2 * SR)),
                                             lowpass(noise(int(1.6 * SR)), 60) * np.linspace(0.6, 0, int(1.6 * SR))]))
    save("ambient_siren", sweep(3.4, 420, 780, 0.45) * (1 + np.sin(2 * np.pi * 0.35 * np.arange(int(3.4 * SR)) / SR)) * 0.5)
    save("ambient_explosion", explosion(3.0, 0.5))
    save("ambient_drone", mix(lowpass(noise(int(4.0 * SR)), 60) * 0.4,
                              np.sin(2 * np.pi * 58 * np.arange(int(4.0 * SR)) / SR) * 0.2))
    save("music_battlefield", music(40.0))

    save("engine_car", engine(2.0, 66, 0.35, 0.3))
    save("engine_plane", engine(2.0, 120, 0.55, 0.4))
    save("engine_heli", engine(2.0, 46, 0.6, 0.5))
    save("engine_tank", engine(2.0, 38, 0.5, 0.4))
    save("tank_cannon", explosion(2.6, 1.4))
    save("vehicle_hit", mix(click(0.12, 700), lowpass(noise(int(0.12 * SR)), 8) * 0.6))
    save("vehicle_destroy", explosion(3.2, 1.5))

    save("ui_click", click(0.05, 2400))
    save("deploy", np.concatenate([click(0.06, 900), np.zeros(int(0.1 * SR)), sweep(0.5, 300, 700, 0.35)]))

    print("sounds done:", len([f for f in os.listdir(ROOT) if f.endswith('.ogg')]))


if __name__ == "__main__":
    main()
