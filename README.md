# BoatHud-map  版本日志
# BoatHud-map  Release Notes

⚠️ **重要警告：不建议与其他小地图模组一起安装，可能会导致游戏崩溃！**
⚠️ **Important Warning: Not recommended to install with other minimap mods, may cause game crashes!**

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
- 🌈 冰道显示选项（显示所有高度或仅显示玩家所在高度及以下）
- 🌈 Ice path display options (show all heights or only player's height and below)

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
- 🏗️ 参考Xaero Minimap的优秀设计，改进了代码结构
- 🏗️ Improved code structure by referencing Xaero Minimap's excellent design
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

- ⚠️ 暂无已知严重问题
- ⚠️ No known serious issues
- 📝 如有问题，请在GitHub Issues中报告
- 📝 If you encounter any issues, please report them on GitHub Issues

