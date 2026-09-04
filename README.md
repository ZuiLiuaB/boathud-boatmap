# BoatHud-map 1.21.4 版本日志
# BoatHud-map 1.21.4 Release Notes

⚠️ **重要警告：不建议与其他小地图模组一起安装，可能会导致游戏崩溃！**
⚠️ **Important Warning: Not recommended to install with other minimap mods, may cause game crashes!**

## 2026-09-04 更新
## September 4, 2026 Update

### 伪 3D 小地图
### Pseudo-3D Minimap
- 🗺️ 新增伪 3D 小地图模式，玩家固定在小地图正中心，地图方向跟随玩家视角
- 🗺️ Added a pseudo-3D minimap mode with the player fixed at the center and the map aligned to the player's view
- 🧭 修正左右方向，使伪 3D 模式与圆形、方形模式保持一致
- 🧭 Corrected left-right orientation to match the circle and square minimap modes
- 📐 赛道采用统一的倾斜平面投影，不产生近大远小、弯曲或扇形拉伸
- 📐 The track uses a uniform tilted-plane projection without depth scaling, bending, or fan-shaped distortion
- ↕️ 倾斜角减小时自动扩大地图前后方向的采样与绘制长度，在 0° 至 89° 范围内填满上下边缘
- ↕️ As the tilt angle decreases, the forward-back scan and draw length expand automatically to fill the top and bottom edges from 0° to 89°
- 👥 其他玩家标识使用与赛道相同的旋转、缩放和边界投影
- 👥 Other player markers use the same rotation, scaling, and boundary projection as the track

## 新增功能
## New Features

### 小地图功能
### Minimap Features
- ✨ 新增了冰道小地图，显示周围的蓝冰、浮冰和普通冰
- ✨ Added ice path minimap showing surrounding blue ice, packed ice, and regular ice
- 📍 玩家标识采用红色三角形，更加醒目和指向性更强
- 📍 Player indicator uses red triangle, more eye-catching and directional
- 👥 支持显示其他玩家，以蓝色正方形表示
- 👥 Supports displaying other players as blue squares
- 🎨 可配置的小地图样式和行为
- 🎨 Configurable minimap style and behavior

### 配置选项
### Configuration Options
- 📐 小地图位置调整（X/Y坐标）
- 📐 Minimap position adjustment (X/Y coordinates)
- 📏 小地图大小和缩放级别
- 📏 Minimap size and zoom level
- 🔄 旋转锁定选项（锁定北方或跟随玩家视角）
- 🔄 Rotation lock option (lock north or follow player view)
- 🔍 冰道检测范围调整
- 🔍 Ice path detection range adjustment
- 🎯 玩家标识和其他玩家标识大小调整
- 🎯 Player indicator and other players' indicator size adjustment
- 👤 其他玩家名字显示开关与大小调整
- 👤 Other players' name display toggle and size adjustment
- 🌈 冰道显示选项（显示所有高度或仅显示玩家所在高度及以下）
- 🌈 Ice path display options (show all heights or only player's height and below)
- 🔲 小地图形状切换（圆形 / 方形 / 伪 3D）
- 🔲 Minimap shape switch (circle / square / pseudo-3D)

### 小地图形状
### Minimap Shape
- 🔲 新增「小地图形状」选项，可在圆形、方形与伪 3D 之间自由切换
- 🔲 Added a "Minimap Shape" option to switch between circle, square, and pseudo-3D
- 🛡️ 方形采用「圆形扫描 + 矩形遮罩」架构：扫描区域始终是圆盘，方形视口是它的内接正方形，因此任意旋转角下都不会出现空白角落，内容也不会戳出边框
- 🛡️ Square mode uses a "circular scan + rectangular mask" architecture: the scanned region is always a disc and the square viewport is its inscribed square, so at any rotation angle there are no blank corners and nothing ever pokes out of the frame
- 🔁 两种形状的缩放手感一致，1 屏幕像素始终对应相同的世界格数
- 🔁 Both shapes share the same zoom feel: one screen pixel always corresponds to the same number of world blocks
- 🧭 其他玩家标识的边界钳制按形状分别处理（方形沿边滑动，圆形贴圆周）
- 🧭 Other players' indicator clamping is handled per shape (slides along the edge on square, hugs the rim on circle)
- 🗺️ 支持伪 3D 形状，并提供可调节的地图倾斜角
- 🗺️ Added the pseudo-3D shape with an adjustable minimap tilt angle

## 优化改进
## Optimizations and Improvements

### 性能优化
### Performance Optimizations
- 🚀 修复了内存泄漏问题，显著减少了内存占用
- 🚀 Fixed memory leak issues, significantly reducing memory usage
- 💨 优化了渲染逻辑，提高了游戏流畅度
- 💨 Optimized rendering logic, improving game smoothness
- 🧹 改进了缓存机制，减少了GC压力
- 🧹 Improved caching mechanism, reducing GC pressure
- ⚡ 优化了冰道检测算法，提高了检测效率
- ⚡ Optimized ice path detection algorithm, improving detection efficiency

### 视觉改进
### Visual Improvements
- 🎨 小地图背景采用半透明黑色圆形
- 🎨 Minimap background uses semi-transparent black circle
- 🔴 玩家标识改为更尖高的三角形，指向性更强
- 🔴 Player indicator changed to taller, more pointed triangle with better directionality
- 🔵 其他玩家标识采用蓝色正方形，易于区分
- 🔵 Other players' indicators use blue squares, easy to distinguish
- 📏 标识大小可根据需要调整
- 📏 Indicator size can be adjusted as needed

### 稳定性改进
### Stability Improvements
- 🛡️ 修复了小地图移动卡顿问题
- 🛡️ Fixed minimap movement stuttering issue
- 🔧 修复了多人游戏时的内存爆炸问题
- 🔧 Fixed memory explosion issue in multiplayer games
- 🔄 修复了缩放时其他玩家位置不正确的问题
- 🔄 Fixed incorrect position of other players when zooming
- 🧭 修复了旋转方向问题
- 🧭 Fixed rotation direction issue
- ✅ 修复了玩家标识形状问题
- ✅ Fixed player indicator shape issue

## 代码优化
## Code Optimizations

### 架构改进
### Architecture Improvements
- 💡 实现了单例模式，避免了静态内存泄漏
- 💡 Implemented singleton pattern, avoiding static memory leaks
- 🔄 改进了生命周期管理，确保资源正确释放
- 🔄 Improved lifecycle management, ensuring proper resource release
- 📦 优化了访问权限，提高了代码的可维护性
- 📦 Optimized access permissions, improving code maintainability

### 错误处理
### Error Handling
- 🛠️ 添加了空指针检查，提高了代码的健壮性
- 🛠️ Added null pointer checks, improving code robustness
- 🚨 改进了错误处理机制，减少了崩溃风险
- 🚨 Improved error handling mechanism, reducing crash risks
- 📋 优化了日志记录，便于调试和问题定位
- 📋 Optimized logging, facilitating debugging and issue localization

## 使用说明
## Usage Instructions

1. 在游戏中打开ModMenu配置界面
1. Open the ModMenu configuration interface in the game
2. 找到"BoatHud选项"
2. Find "BoatHud Options"
3. 调整小地图的各项设置
3. Adjust the minimap settings
4. 实时预览效果，找到最适合您的配置
4. Preview the effect in real-time, find the most suitable configuration for you

## 兼容性
## Compatibility

- 🎮 支持Minecraft 1.21.3
- 🎮 Supports Minecraft 1.21.3
- 📦 基于Fabric API开发
- 📦 Developed based on Fabric API

## 已知问题
## Known Issues

- ⚠️ 方形模式的扫描面积约为圆形模式的 2 倍；小地图大小设到 512 时可能出现掉帧，建议 256 及以下
- ⚠️ Square mode scans about twice the area of circle mode; setting the minimap size to 512 may cause frame drops, 256 or below is recommended
- ⚠️ 其余暂无已知严重问题
- ⚠️ No other known serious issues
- 📝 如有问题，请在GitHub Issues中报告
- 📝 If you encounter any issues, please report them on GitHub Issues

