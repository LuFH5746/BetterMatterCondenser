# Better Molecular Assembler

## 环境

- Minecraft 1.21.1
- NeoForge
- AE2 19.2.0-beta

---

## 功能概述

基于 AE2 分子装配室（Molecular Assembler）的功能性模组，新增一个方块 `better_molecular_assembler`，具备以下能力：

1. 垃圾桶截取
2. 自动导出产物
3. 红石模式控制

---

## 方块与资源

- 方块贴图：复制 AE2 分子装配室贴图（`molecular_assembler.png`、`molecular_assembler_lights.png`）
- GUI 贴图：复制 AE2 分子装配室 GUI 贴图（`molecular_assembler.png`）
- 方块模型：复制 AE2 分子装配室模型
- 物品图标：继承方块模型
- 合成配方：自定义配方（见下方）

### 合成配方

```
ABA
CDE
ABA
```

- A: `ae2:quartz_glass`
- B: `ae2:engineering_processor`
- C: `ae2:crafting_unit`
- D: `minecraft:nether_star`
- E: `ae2:formation_core`

---

## 垃圾桶截取

### 目标

拦截无线终端的垃圾桶操作，将物品导入本模组的聚合器输入槽，而非直接进入 ME 网络存储。

### 支持的终端

- AE2 原版无线终端（`MEStorageMenu`）
- 其他模组终端：待调查

### 规则

1. 仅响应**同一 ME 网络**内的终端垃圾桶操作
2. 不同网络互不干扰
3. 绑定对象为**网络**，非玩家
4. 未连接到网络的聚合器不接收垃圾

### 选取逻辑

1. 获取当前网络内所有 `BetterMABlockEntity` 实例
2. 过滤条件：已连接网络 + 输入槽有空间
3. 排序：优先级降序 → 放置时间降序（`level.getGameTime()`）
4. 依次尝试插入，选择第一台接受成功的聚合器

### 满载回退

- 指定聚合器输入槽满时，尝试下一台
- 所有聚合器均满载：不截取，垃圾桶按原版逻辑销毁物品
- 网络中无可用聚合器：同上

### Mixin 目标

`appeng.menu.me.common.MEStorageMenu#putCarriedItemIntoNetwork`

---

## 自动导出产物

### 目标

合成完成后，输出槽物品可自动导出到 ME 网络存储。

### 导出方式

仅支持有线连接（ME 线缆）。

无线导出（通过 ME 无线访问点）暂不实现，作为未来功能保留。

### 速率限制

- 每 tick 导出物品数量上限
- 默认值：256
- 配置项：`exportRateLimit`（Config 文件）
- GUI 不显示此配置

### 实现参考

向网络注入物品的逻辑参考 AE2 ME 接口（`appliedenergistics2:interface`）。

---

## 红石模式

控制自动导出的触发条件。

| 模式 | 值 | 行为 |
|------|----|------|
| IGNORE | 0 | 不自动导出（默认） |
| NORMAL | 1 | 红石信号激活时导出 |
| INVERTED | 2 | 红石信号未激活时导出 |

- 切换方式：GUI 按钮循环切换
- 未连接网络时，红石模式无效

---

## 优先级

### 范围

-999 ~ 999

### 作用

决定垃圾桶截取时的选取顺序。高优先级优先接收。

### GUI

- 文本输入框
- Top 按钮：设置为 999
- Bottom 按钮：设置为 -999

---

## GUI 布局

基于 AE2 分子装配室 GUI 贴图。

### 槽位

| 类型 | 数量 | 起始坐标 (x, y) | 排列 |
|------|------|-----------------|------|
| 输入槽 | 9 (3x3) | (29, 31) | 间距 18px |
| 输出槽 | 9 (3x3) | (116, 31) | 间距 18px |
| 样板槽 | 3 | (29, 85) | 水平间距 18px |
| 玩家背包 | 27 (9x3) | (8, 111) | 间距 18px |
| 玩家快捷栏 | 9 | (8, 169) | 水平间距 18px |

### 新增控件

| 控件 | 坐标 (相对 GUI 左上角) | 尺寸 |
|------|------------------------|------|
| 优先级输入框 | (7, 75) | 40x12 |
| Top 按钮 | (49, 75) | 12x12 |
| Bottom 按钮 | (63, 75) | 12x12 |
| 红石模式按钮 | (118, 75) | 50x12 |

---

## Config 配置

| 配置项 | 类型 | 默认值 | 范围 | 说明 |
|--------|------|--------|------|------|
| `exportRateLimit` | int | 256 | 1 ~ 10000 | 每 tick 导出物品数量上限 |

配置文件路径：`config/bettermolecularassembler-common.toml`

---

## 未连接网络时的行为

| 功能 | 状态 |
|------|------|
| 垃圾桶截取 | 不生效 |
| 自动导出 | 不生效 |
| 红石按钮 | 无效 |
| 优先级设置 | 可设置但无实际效果 |
| 物品输入 | 原版行为（漏斗/接口推送） |

---

## BlockEntity 数据持久化

| 字段 | NBT 键 | 类型 | 说明 |
|------|--------|------|------|
| 红石模式 | `RedstoneMode` | String | IGNORE/NORMAL/INVERTED |
| 优先级 | `Priority` | Int | -999 ~ 999 |
| 放置时间 | `PlacementTime` | Long | `level.getGameTime()` |
| 物品栏 | `Inventory` | Compound | 21 个槽位（9输入+9输出+3样板） |

---

## 待完成

- [ ] 禁用原版 AE2 分子装配室配方
- [ ] 调查非 AE2 终端的 Mixin 支持
- [ ] 测试与调试
