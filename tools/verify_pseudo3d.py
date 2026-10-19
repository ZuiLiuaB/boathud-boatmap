# -*- coding: utf-8 -*-
# Numerical verification of the pseudo-3D "true perspective floor" math,
# mirroring HudRenderer.renderMinimapPseudo3D / preRenderMinimap(shape==2) /
# drawOtherPlayersPseudo3D exactly. Catches NaN / non-monotonic / degenerate cases
# across the full parameter ranges without needing to launch Minecraft.
import math

def get_texture_size(panel, view_dist, zoom):
    t = round(max(panel, view_dist) * zoom)
    if t & 1:
        t += 1
    return max(64, min(1024, t))

def check(panel, view_dist, zoom, horizon_v, tilt_deg):
    T = get_texture_size(panel, view_dist, zoom)
    dFar = 0.9 * T
    dNear = -0.05 * T
    tilt = math.radians(tilt_deg)
    k = 0.5 * T * math.tan(tilt)
    thFar = k / (k + dFar)
    thNear = k / (k + dNear)
    half = panel / 2.0
    horizonY = horizon_v * panel
    mapHeight = panel - horizonY
    # player t
    tPlayer = (1.0 - thFar) / (thNear - thFar)
    # inverse mapping d(t)
    def d_of_t(t):
        th = t * (thNear - thFar) + thFar
        return k * (1.0 / th - 1.0)
    # checks
    problems = []
    if not (0.0 < tPlayer < 1.0):
        problems.append(f"tPlayer={tPlayer:.4f} not in (0,1)")
    # monotonic decreasing d over t in [0,1]
    prev = None
    for i in range(101):
        t = i / 100.0
        d = d_of_t(t)
        if prev is not None and d > prev + 1e-9:
            problems.append(f"d not monotonic at t={t:.2f}")
            break
        prev = d
    # t at far / near edges
    t_far = (thFar - thFar) / (thNear - thFar)  # = 0
    t_near = (thNear - thFar) / (thNear - thFar)  # = 1
    if abs(t_far) > 1e-9:
        problems.append(f"t(dFar)!=0 ({t_far})")
    if abs(t_near - 1.0) > 1e-9:
        problems.append(f"t(dNear)!=1 ({t_near})")
    # screenHalf at far edge and at player
    th_dFar = thFar
    th_d0 = 1.0
    halfFar = half * th_dFar
    halfPlayer = half * th_d0
    if not (0.0 < halfFar < halfPlayer + 1e-9):
        problems.append(f"halfFar={halfFar:.2f} not in (0, half={half:.1f})")
    if abs(halfPlayer - half) > 1e-9:
        problems.append("half at player != half")
    # strip regionH sanity over nStrips
    nStrips = min(max(16, panel), T)
    min_regionH = 10**9
    max_regionH = -1
    for i in range(nStrips):
        tTop = i / nStrips
        tBot = (i + 1) / nStrips
        dTop = d_of_t(tTop)
        dBot = d_of_t(tBot)
        zTop = round((dFar - dTop) / T * (T - 1))
        zBot = round((dFar - dBot) / T * (T - 1))
        regionH = abs(zBot - zTop)
        if regionH <= 0:
            regionH = 1
        min_regionH = min(min_regionH, regionH)
        max_regionH = max(max_regionH, regionH)
    if min_regionH < 0:
        problems.append("negative regionH")
    # NaN sweep already covered by the float ops above; check explicit NaN
    for name, val in [("k", k), ("thFar", thFar), ("thNear", thNear), ("tPlayer", tPlayer)]:
        if math.isnan(val) or math.isinf(val):
            problems.append(f"{name} is {val}")
    return {
        "T": T, "dFar": round(dFar, 1), "dNear": round(dNear, 1), "k": round(k, 1),
        "tPlayer": round(tPlayer, 4), "halfFar": round(halfFar, 1),
        "minRH": min_regionH, "maxRH": max_regionH, "problems": problems,
    }

print("=== default + tilt sweep (panel=128, viewDist=64, zoom=1, horizon=0) ===")
for tilt in [12, 20, 35, 50, 70, 89]:
    r = check(128, 64, 1.0, 0.0, tilt)
    print(f"tilt={tilt:>2}  T={r['T']:<4} dFar={r['dFar']:>7} dNear={r['dNear']:>7} "
          f"k={r['k']:>7} tPlayer={r['tPlayer']:.3f} halfFar={r['halfFar']:>5} "
          f"regionH[{r['minRH']}..{r['maxRH']}]  {'OK' if not r['problems'] else r['problems']}")

print("\n=== parameter-range sweep (look for any problem) ===")
bad = 0
for panel in [64, 128, 256]:
    for vd in [4, 16, 64, 128, 256]:
        for zoom in [0.25, 1.0, 2.0, 4.0]:
            for hor in [0.0, 0.4, 0.8]:
                for tilt in [12, 35, 60, 89]:
                    r = check(panel, vd, zoom, hor, tilt)
                    if r["problems"]:
                        bad += 1
                        if bad <= 10:
                            print(f"  BAD panel={panel} vd={vd} zoom={zoom} hor={hor} tilt={tilt}: {r['problems']}")
print(f"\nTotal parameter combos checked: {len([0])*0 + 3*5*4*3*4}  problems found: {bad}")
