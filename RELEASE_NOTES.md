# BoatHud & Map v3.2 — Minecraft 26.2

产物：`build/libs/boathud&map-v3.2-26.2.jar`（同步副本：`E:/1=冰船小地图/dist/`）
内部版本：`v3.2-26.2`
构建：JDK 25 + Fabric Loom 1.17.20 + Gradle 9.6.1

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.153.0+26.2 |
| Mod Menu | 20.0.0-beta.4 |
| Cloth Config | 26.2.155 |

与上游 [hibiii/BoatHud v1.3.1](https://github.com/hibiii/BoatHud)（官方 26.2 版，**主要参考**）完全一致。
本分支仓库：[ZuiLiuaB/boathud-boatmap](https://github.com/ZuiLiuaB/boathud-boatmap)。

## 本版相对上游的增量

在上游基础上自研**冰道小地图**（上游全工程无 minimap），并补齐上游 1.3.1 的两块新功能。

### v3.2 新增

- **可穿透方块不再挡冰**：`hasTransparentAbove` 由 `isAir() || canBeReplaced()` 改为 `getCollisionShape().isEmpty()`——绊线、火把、告示牌、按钮、红石线、光源方块等无碰撞方块不再遮挡下方冰道
- **其他玩家改为头像渲染**：
  - 抠玩家皮肤头（头 8,8 + 帽层 40,8 两次叠加），黑底
  - 外框 = 皮革盔甲实际染色 rgb（无皮革默认蓝）；队伍配色直接可用任意颜色
  - 速度比较色改为最外圈 2px 框，与皮革框不冲突
  - 新增 `头像大小` 配置（默认 6.0，范围 2–16）
- **名字位置自定义**（修复名字与头像重叠）：
  - `名字垂直偏移`：−40 ~ +40 像素，默认 +10（头像下方）；负 = 上方，正 = 下方，相对头像中心连续可调
  - `名字水平偏移`：−40 ~ +40 像素，默认 0；负 = 左，正 = 右
  - 旧配置 `名字在头像上方` + `名字间距` 自动迁移为等价的 Y 偏移
- **亚像素定位**：扫描中心 `(int)` → `MathHelper.floor`/`Mth.floor`（负坐标原来整体偏移一格）；其他玩家全链路 double，仅最后 `Math.round` 落像素
- 其他玩家名字默认大小 0.5 → 1.0（原版字号），菜单上限 2.0 → 3.0
- **旁观标签**（默认关，ModMenu → 小地图 → 旁观标签）：旁观模式下不画自己的中心箭头，把当前旁观玩家（相机实体）当作其他玩家渲染出头像 + 皮革色框 + 名字标签；平面与伪3D 均支持
- **一键导出/导入配置**（ModMenu 入口页）：
  - `导出配置到剪贴板`：把当前全部配置序列化为 properties 文本写入系统剪贴板
  - `从剪贴板导入配置`：读取剪贴板文本 → 校验是 BoatHud 配置 → 应用到内存并写盘，actionbar 反馈结果
  - `打开详细设置`：进入原 cloth-config 完整设置界面
  - 校验：剪贴板内容不含 `enabled`/`minimapEnabled` 键则拒绝导入并提示
- **移除速度比较**：删除 `minimapShowSpeedComparison` 配置、菜单开关及其他玩家头像外圈绿/橙速度框（皮革色框保留）；旧配置文件中的该键被静默忽略
- **三处边框粗细可调**（ModMenu → 小地图，单位像素，范围 0–8/0–6，默认 1.0，设为 0 即隐藏）：
  - `小地图边框大小`：面板外框（方形四边 / 圆形描边 / 伪3D 方框统一生效）
  - `玩家箭头描边大小`：自身中心箭头的黑色外描边
  - `其他玩家头像边框大小`：头像图标的队伍皮革色外框
  - 修复：ModMenu 入口页（ConfigScreen）在 26.2 下打开即崩 `Can only blur once per frame`——extractRenderState override 不再调 extractBackground（背景由 vanilla final 包装方法统一提取）
- **人多掉帧优化**：其他玩家外观数据（皮革染色、名字、名字宽度）此前每帧 × 每玩家重算——满服时每帧几十次盔甲数据组件查找 + 字符串拼接 + 字形宽度测量，是小地图最重的 CPU 开销；现改为约 4 Hz 缓存（每 5 tick 整表刷新一次防离线玩家泄漏，新玩家按需即时填充），平面路径每玩家的旋转 sin/cos 外提到循环外（每帧一次而非每玩家一次），`hasVehicle() && getVehicle() instanceof` 双次调用合并为单次 `getVehicle()` 判断；平面与伪3D 两条渲染路径均生效

### v3 新增

- **主 HUD 位置/大小可调**（ModMenu → 通用）：
  - `HUD X 偏移` / `HUD Y 偏移`：±400 像素，正 = 右/下
  - `HUD 大小`：0.5–2.0 倍缩放，1.0 = 默认；缩放以 HUD 左上角为锚点
  - 只作用于主 HUD（速度条/文字/输入显示），小地图不受影响
- **小地图指北箭头**（默认关闭，ModMenu → 小地图 → 显示指北箭头）：
  - 白色箭头 + 黑色描边，固定在面板顶部居中，始终指向世界北
  - 圆形 / 方形 / 伪3D 三种形状统一支持；伪3D 下屏幕空间直立、随 smoothYaw 反向旋转

### 此前已并入（1.1.8-mc26.2 阶段）

- **速度条 PROGRESSIVE（第 4 种）**：`SpeedBar` 枚举驱动，按速度自动分段量程；新增贴图 `bar_4_lit/unlit.png`
- **镜头 look-ahead（Boat Cam 式）**：`cameraControl`（默认关）/ `cameraAggressiveness` / `cameraSmoothing`
- **修复 mixin 漏船型**：`instanceof Boat` → `AbstractBoat`（否则坐运输船/竹筏 HUD 不显示）
- **修复依赖声明**：`fabric` → `fabric-api`；`minecraft >=26.2`；`cloth-config2 >=26.2.0`
- **修复 26.2 严格 ARGB 导致的文字全透明**：`0xFFFFFF` → `0xFFFFFFFF`（HUD 文字 + 指北针）
- 其他玩家名字默认显示（`minimapShowOtherPlayersNames`）

### 行为变更

- `Config.barType` 由 `int` 改为 `SpeedBar` 枚举；`boathud.properties` 中仍为 0–3 整数，旧存档自动映射，越界回落 PACKED。
- 新增配置键（hudX/hudY/hudScale、minimapShowNorthIndicator）在旧配置文件缺失时走默认值，升级无感。
- `minimapShowOtherPlayersNames` 默认值由 false 改为 true；旧配置若已显式写 false 则保持 false。

## 发布信息（ModMenu 页面）

- 名称：Boathud and Map　版本：v3.2-26.2　作者：hibi、ZuiLiu_ab
- 链接：网站 → hibiii/BoatHud（主要参考）；源码/问题 → ZuiLiuaB/boathud-boatmap；GitHub 发行版 → releases
- 已移除 Modrinth / CurseForge 链接；描述无测试标记

## 配置项一览

- 通用：启用、拓展 HUD、HUD X/Y 偏移、HUD 大小、速度单位、速度栏类型
- 镜头：启用视角前瞻、前瞻强度、前瞻平滑度
- 小地图：开关、位置、形状（圆形/方形/伪3D）、倾斜角、缩放、分辨率、冰道检测范围、平板冰、显示指北箭头、显示其他玩家及其名字/标识大小、头像大小、头像边框、名字垂直偏移、名字水平偏移、旁观标签、限制边界、边框大小、箭头描边大小

## 已知待办

- 部分历史遗留键未被代码使用（`minimap_player_shape*`、`minimap_scale`）
- 1.21.3 工程不回灌 v3 功能（用户决定，仅保留漏船型修复）
