# 船只航向角度计算文档

## 一、功能概述

在BoatHud模组中，`HudData`类负责计算船只的当前速度和漂移角度。漂移角度（drift angle）是指船只实际移动方向与船头朝向之间的角度差，是衡量船只操控性的重要指标。

## 二、核心代码位置

**文件路径**：`src/main/java/hibi/boathud/HudData.java`

**主要方法**：`update()` 方法（第31-47行）

## 三、使用的Minecraft包和类

```java
import net.minecraft.entity.vehicle.AbstractBoatEntity;  // 船只实体类
import net.minecraft.util.math.Vec3d;                    // 三维向量类
```

### 主要方法调用链

| 方法名 | 说明 | 返回值类型 |
|--------|------|-----------|
| `AbstractBoatEntity.getVelocity()` | 获取船只速度向量 | `Vec3d` |
| `AbstractBoatEntity.getRotationVector()` | 获取船只朝向向量 | `Vec3d` |
| `Vec3d.multiply(double, double, double)` | 向量乘法 | `Vec3d` |
| `Vec3d.dotProduct(Vec3d)` | 向量点积 | `double` |
| `Vec3d.length()` | 向量长度计算 | `double` |
| `Math.acos(double)` | 反余弦函数 | `double` |
| `Math.toDegrees(double)` | 弧度转角度 | `double` |

## 四、核心计算逻辑

### 4.1 速度计算

**代码位置**：第34-36行

```java
// 忽略垂直速度，只考虑水平方向
Vec3d velocity = boat.getVelocity().multiply(1, 0, 1);

// 将速度转换为常用单位（从米/游戏刻转换为米/秒）
this.speed = velocity.length() * 20d;
```

**逻辑说明**：

- 船只速度向量包含三个分量（x, y, z）
- 通过乘法运算将y分量置零，保留x和z分量
- `velocity.length()` 计算向量的模长（即速度大小）
- 乘以20是因为Minecraft中速度单位是米/游戏刻，需要转换为米/秒

**计算公式**：

```
速度（m/s）= 向量模长 × 20
```

### 4.2 漂移角度计算

**代码位置**：第38-41行

```java
// 使用向量点积公式计算角度
this.driftAngle = Math.toDegrees(
    Math.acos(
        velocity.dotProduct(boat.getRotationVector()) 
        / (velocity.length() * boat.getRotationVector().length())
    )
);

// 处理除零情况（当速度为0时）
if(Double.isNaN(this.driftAngle)) {
    this.driftAngle = 0;
}
```

**数学原理**：

- **速度向量（velocity）**：表示船只实际移动的方向和速度
- **朝向向量（getRotationVector()）**：表示船头所指的方向
- **向量点积（dotProduct）**：计算两个向量的点积
- **反余弦（acos）**：根据点积结果计算夹角
- **转换为角度制（toDegrees）**：从弧度转换为角度

**计算步骤**：

1. 计算速度向量与朝向向量的点积：`velocity · boat.getRotationVector()`
2. 计算两个向量的模长乘积：`velocity.length() × boat.getRotationVector().length()`
3. 计算点积与模长乘积的比值：`cos(θ) = (velocity · rotationVector) / (velocity.length() × rotationVector.length())`
4. 使用反余弦函数计算角度：`θ = acos(cos(θ))`
5. 转换为角度制：`driftAngle = θ × (180/π)`

## 五、关键公式解析

### 向量点积公式

```
a̅•b̅ = |a̅||b̅|cos(θ)
```

**参数说明**：

| 参数 | 说明 |
|------|------|
| `a̅•b̅` | 向量a和b的点积 |
| `|a̅|` | 向量a的模长 |
| `|b̅|` | 向量b的模长 |
| `θ` | 两个向量之间的夹角 |

### 推导公式

```
θ = acos[(a̅•b̅) / (|a̅||b̅|)]
```

### 漂移角度的物理意义

| 角度值 | 含义 | 操控状态 |
|--------|------|--------|
| 0° | 船只完全沿船头方向移动 | 最佳状态，无漂移 |
| 0°-30° | 船只略微偏离船头方向 | 正常行驶，轻微漂移 |
| 30°-60° | 船只明显偏离船头方向 | 存在明显漂移 |
| 60°-90° | 船只严重偏离船头方向 | 失控边缘 |
| 90° | 船只完全侧向移动 | 完全失控 |
| 180° | 船只完全向后移动 | 倒退状态 |

## 六、完整代码示例

```java
package hibi.boathud;

import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 船只航向角度计算工具类
 * 
 * 用于计算船只的漂移角度，即实际移动方向与船头朝向之间的夹角
 */
public class AngleCalculator {
    
    /**
     * 计算船只的漂移角度
     * 
     * @param boat 船只实体
     * @return 漂移角度（度），范围0-180
     */
    public static double calculateDriftAngle(AbstractBoatEntity boat) {
        // 获取船只速度向量（忽略垂直分量）
        Vec3d velocity = boat.getVelocity().multiply(1, 0, 1);
        
        // 获取船只朝向向量
        Vec3d rotationVector = boat.getRotationVector();
        
        // 计算点积
        double dotProduct = velocity.dotProduct(rotationVector);
        
        // 计算向量模长
        double velocityLength = velocity.length();
        double rotationLength = rotationVector.length();
        
        // 避免除零错误
        if (velocityLength < 0.0001 || rotationLength < 0.0001) {
            return 0.0;
        }
        
        // 计算夹角的余弦值
        double cosAngle = dotProduct / (velocityLength * rotationLength);
        
        // 确保cosAngle在[-1, 1]范围内，避免NaN
        cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));
        
        // 计算角度并转换为角度制
        double angle = Math.toDegrees(Math.acos(cosAngle));
        
        return angle;
    }
    
    /**
     * 计算船只的移动速度
     * 
     * @param boat 船只实体
     * @return 速度（米/秒）
     */
    public static double calculateSpeed(AbstractBoatEntity boat) {
        // 获取速度向量并忽略垂直分量
        Vec3d velocity = boat.getVelocity().multiply(1, 0, 1);
        
        // 转换为常用单位
        return velocity.length() * 20.0;
    }
}
```

## 七、类完整实现

```java
package hibi.boathud;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.Vec3d;

public class HudData {
    /** 当前速度（米/秒） */
    public double speed;
    
    /** 当前加速度（g） */
    public double g;
    
    /** 当前漂移角度（度），即速度方向与船头朝向之间的角度差 */
    public double driftAngle;
    
    /** 当前ping值 */
    public int ping;
    
    /** 玩家名称 */
    public final String name;
    
    /** 是否为驾驶员 */
    public boolean isDriver;
    
    private double oldSpeed;
    private final PlayerListEntry listEntry;
    
    public HudData(){
        this.name = Common.client.player.getName().getString();
        this.listEntry = Common.client.getNetworkHandler().getPlayerListEntry(Common.client.player.getUuid());
    }
    
    /**
     * 更新数据，假设玩家在船上
     * 警告：除非确定玩家在船上，否则不要调用此方法
     */
    public void update() {
        AbstractBoatEntity boat = (AbstractBoatEntity)Common.client.player.getVehicle();
        
        // 忽略垂直速度
        Vec3d velocity = boat.getVelocity().multiply(1, 0, 1);
        this.oldSpeed = this.speed;
        this.speed = velocity.length() * 20d; // Minecraft引擎速度单位是米/游戏刻
        
        // 使用向量点积公式计算角度
        // 公式：a̅•b̅ = |a̅||b̅|cos(θ)
        // 推导：θ = acos[(a̅•b̅) / (|a̅||b̅|)]
        this.driftAngle = Math.toDegrees(
            Math.acos(
                velocity.dotProduct(boat.getRotationVector()) 
                / (velocity.length() * boat.getRotationVector().length())
            )
        );
        
        // 处理除零情况（当速度为0时）
        if(Double.isNaN(this.driftAngle)) {
            this.driftAngle = 0;
        }
        
        // 计算加速度
        // 2.040816327 = 20 tps / 9.8 m/s²
        this.g = (this.speed - this.oldSpeed) * 2.040816327d;
        
        // 更新其他数据
        this.ping = this.listEntry.getLatency();
        this.isDriver = boat.getControllingPassenger() == Common.client.player;
    }
}
```

## 八、性能优化建议

### 8.1 避免重复计算

```java
// 优化前：每次都重新计算
this.driftAngle = Math.toDegrees(
    Math.acos(
        velocity.dotProduct(boat.getRotationVector()) 
        / (velocity.length() * boat.getRotationVector().length())
    )
);

// 优化后：预先计算并缓存
double velocityLength = velocity.length();
double rotationLength = boat.getRotationVector().length();
double product = velocityLength * rotationLength;

if (product > 0.0001) {
    this.driftAngle = Math.toDegrees(
        Math.acos(velocity.dotProduct(boat.getRotationVector()) / product)
    );
}
```

### 8.2 除零保护

```java
// 确保向量的模长不为零
if (velocityLength < 0.0001 || rotationLength < 0.0001) {
    return 0.0;
}

// 确保cosAngle在[-1, 1]范围内
double cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));
```

### 8.3 快速判断

```java
// 对于静止状态，直接返回
if (velocityLength < 0.0001) {
    this.driftAngle = 0.0;
    return;
}
```

## 九、应用场景

### 9.1 竞速比赛

- 监测船只是否保持最佳行驶角度
- 实时反馈当前操控状态
- 帮助玩家找到最佳行驶路线

### 9.2 操控反馈

- 为玩家提供直观的操控反馈
- 显示当前漂移程度
- 指导玩家进行方向调整

### 9.3 性能分析

- 分析不同冰面上的操控特性
- 对比不同配置的船只性能
- 优化行驶策略

### 9.4 教学指导

- 帮助新手学习最佳的划船技巧
- 展示最佳角度范围
- 提供实时改进建议

## 十、相关资源

- Minecraft官方文档：[Entity Velocity](https://minecraft.fandom.com/wiki/Entity#Velocity)
- 向量数学：[Vec3d Class](https://minecraft.fandom.com/wiki/Java_Edition_vector)
- 游戏刻度系统：1秒 = 20游戏刻

---

**文档版本**：1.0

**最后更新**：2026-04-20

**作者**：BoatHud Development Team

**许可协议**：MIT License
