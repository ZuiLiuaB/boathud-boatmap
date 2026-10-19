# -*- coding: utf-8 -*-
# Render the pseudo-3D kart minimap to PNG using ONLY the stdlib (zlib + hand-rolled PNG
# encoder), mirroring HudRenderer's exact perspective math. Lets us eyeball the orientation
# without launching Minecraft. Two tilts: 35 (default) and 12 (strong perspective).
import math, zlib, struct, os

def clamp_even(v):
    v = round(v)
    if v & 1: v += 1
    return max(64, min(1024, v))

def world_color(wx, wz):
    track = 2.2
    ad = abs(wx)
    if ad < track:
        on_dash = (math.floor(wz / 4) % 2) == 0
        if on_dash and ad < 0.35: return (255, 220, 120)
        return (60, 160, 255)
    gx = abs(wx - round(wx)); gz = abs(wz - round(wz))
    if gx < 0.06 or gz < 0.06: return (120, 150, 190)
    n = math.sin(wx*1.7)*math.cos(wz*1.3) + math.sin((wx+wz)*0.7)
    if n > 0.85: return (90, 200, 230)
    return (30, 40, 56)

def render(panel, view_dist, zoom, horizon_v, tilt_deg, yaw_deg, path):
    half = panel/2.0
    cx = int(round(half))
    T = clamp_even(max(panel, view_dist) * zoom)
    dFar = 0.9*T; dNear = -0.05*T
    tilt = math.radians(tilt_deg)
    k = 0.5*T*math.tan(tilt)
    thFar = k/(k+dFar); thNear = k/(k+dNear)
    horizonY = round(horizon_v*panel)
    mapHeight = panel - horizonY
    yR = math.radians(yaw_deg)
    fX = -math.sin(yR); fZ = math.cos(yR)
    rX = -math.cos(yR); rZ = -math.sin(yR)

    rgb = bytearray(panel*panel*3)
    for py in range(panel):
        sky = py < horizonY
        for px in range(panel):
            o = (py*panel+px)*3
            if sky:
                t = (py-0)/max(1, horizonY) if horizonY>0 else 0
                rgb[o]=0; rgb[o+1]=int(0x1E+t*0x10); rgb[o+2]=int(0x2A+t*0x20)
                continue
            t = (py-horizonY)/mapHeight
            th = t*(thNear-thFar)+thFar
            d = k*(1.0/th - 1.0)
            screenHalf = half*th
            worldLat = ((px-cx)/(screenHalf if screenHalf else 1.0))*(T/2.0)
            wx = worldLat*rX + d*fX
            wz = worldLat*rZ + d*fZ
            c = world_color(wx, wz)
            rgb[o]=c[0]; rgb[o+1]=c[1]; rgb[o+2]=c[2]

    # other players on centre line (lateral=0) -> must land on vertical centre axis
    others = [12,26,42,60,80,100]
    for od in others:
        thD = k/(k+od)
        t = (thD-thFar)/(thNear-thFar)
        sy = int(round(horizonY + t*mapHeight))
        sx = int(round(cx))
        for dy in (-2,-1,0,1,2):
            for dx in (-2,-1,0,1,2):
                yy=sy+dy; xx=sx+dx
                if 0<=yy<panel and 0<=xx<panel:
                    o=(yy*panel+xx)*3
                    rgb[o]=255; rgb[o+1]=210; rgb[o+2]=74

    # player arrow at tPlayer
    tPlayer = (1.0-thFar)/(thNear-thFar)
    pyA = int(round(horizonY + tPlayer*mapHeight))
    s = max(4, int(panel*0.03))
    def pxset(x,y,col):
        if 0<=y<panel and 0<=x<panel:
            o=(y*panel+x)*3; rgb[o]=col[0]; rgb[o+1]=col[1]; rgb[o+2]=col[2]
    for i in range(-s, s+1):
        pxset(cx, pyA-2*s+i, (0,0,0))
        pxset(cx-i, pyA+s, (0,0,0))
        pxset(cx+i, pyA+s, (0,0,0))
    for i in range(-int(0.7*s), int(0.7*s)+1):
        pxset(cx, pyA-int(1.4*s)+i, (255,77,77))
        pxset(cx-i, pyA+int(0.7*s), (255,77,77))
        pxset(cx+i, pyA+int(0.7*s), (255,77,77))

    # frame
    for x in range(panel):
        for y in (0,panel-1):
            o=(y*panel+x)*3; rgb[o]=0; rgb[o+1]=0; rgb[o+2]=0
    for y in range(panel):
        for x in (0,panel-1):
            o=(y*panel+x)*3; rgb[o]=0; rgb[o+1]=0; rgb[o+2]=0

    # ---- PNG encode (RGB, 8-bit) ----
    def chunk(typ, data):
        return struct.pack(">I", len(data)) + typ + data + struct.pack(">I", zlib.crc32(typ+data) & 0xffffffff)
    ihdr = struct.pack(">IIBBBBB", panel, panel, 8, 2, 0, 0, 0)
    raw = bytearray()
    for y in range(panel):
        raw.append(0)
        raw.extend(rgb[y*panel*3:(y+1)*panel*3])
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", idat))
        f.write(chunk(b"IEND", b""))
    print(f"wrote {path}  (T={T}, tPlayer={tPlayer:.3f})")

os.makedirs("tools", exist_ok=True)
render(256, 64, 1.0, 0.0, 35, 0, "tools/pseudo3d_render_t35.png")
render(256, 64, 1.0, 0.0, 12, 0, "tools/pseudo3d_render_t12.png")
