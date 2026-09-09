# Changelog

本应用随 [token-monitor](https://github.com/Javis603/token-monitor) 上游同步迭代。版本号独立于上游。

## [Unreleased]

同步最新上游 **token-monitor**（v0.53 + v0.54，至 bda6ffc），对齐提供方目录重构与额度子系统重构。

### 新增

- 三个新提供方的支持与品牌图标：**Alibaba Cloud**（Token Plan）、**Unsloth Studio**、**Kilo**（合并自 Kilo Code，统一为 `kilo`；`kilocode` 仍保留别名）
- 额度数据模型补齐：窗口按上游归一化使用 `remainingPercent`，尊重 `showMeter: false`；`additional` 标记的 Codex 子桶默认折叠，与桌面端紧凑视图一致
- **余额型窗口的派生进度条**（对齐 `limitBalanceDisplay.creditsMeterPercent`）：充值型余额没有固定配额分母，改为按「当前余额 /(当前余额 + 本月已观测消耗)」可视化；该比例仅为展示层推导，永不写回协议
- 金额窗口的货币渲染：支持 `CREDITS` 点数余额（不带货币符号）、`¥`/`$`/`NT$`/`HK$`，大额自动紧凑显示
- Claude 的 `spend` 窗口（Usage credits）按上游显示「已用 / 上限」或「已用」
- MiMo 的 Token Plan 窗口在提供方未上报窗口时，从 `balance` 的 `planUsed`/`planLimit`/`planPercent` 合成；`planStatus = expired` 显示「套餐已过期」
- `BalanceBlock` 补齐 `planUsed`/`planLimit`/`planPercent`/`planStatus`/`giftBalance`/`cashBalance`/`expiresAt`/`trackingSince`/`monthSinceTracking`

### 变更

- **额度页布局重做**，参照桌面端 `limits-view`：
  - 提供方卡片头部加入工具品牌图标（`ToolIcon`），左侧名称 + 身份/更新时间，右侧状态标签
  - **窗口改为 1-或 2-列网格**：两个窗口左右各半，单个窗口独占一行，对齐上游 `grid-template-columns: 1fr 1fr`
  - 窗口顶部行采用「标签 + 已用百分比」配色按余量阈值（剩余 <20% 红 / <50% 橙 / 其余绿）；余量条按 `remainingPercent` 归一填充
  - 状态枚举扩充中文本地化（未配置 / 需登录 / 受限 / 不可用 / 已停用 / 异常）
- 新增 `Format.windowLabel`：窗口展示标签优先使用上游 `window.label`（如 "Session"/"Weekly"/"5-hour"），回退到本地化类型
- 新增 `Format.limitFillPercent`：余量填充归一助手，对齐桌面端 `limitFillPercent`
- 提供方名称对齐上游 `limitProviders.js`：`zai` → GLM、`trae` → Trae CN、新增 `zed`；客户端名称 `qwen` → Qwen、新增 `kilo`
- 品牌配色对齐上游 `usageCharts.js`：`alibaba` #615CED、`unsloth` #40B85A、`thirdparty` #8090A6

### 修复

- 余量条此前按 `usedPercent` 反向填充，与上游一致改为「剩余越多条越长」
- 余额型窗口（DeepSeek / MiMo / OpenRouter / 三方）此前完全不显示进度条，现在按派生比例显示
- **无 `resetsAt` 的窗口此前丢失重置信息**，现在回退到上游的 `resetDescription`（如 Zed 编辑预测窗口）
- 模型归类：`muse-spark` 归入 Meta（此前回退到哈希色）、`hy\d` 覆盖全部混元编号、Doubao 规则去掉会误伤的 `volc|ark`
- 首页额度卡片此前按「已用百分比」排序与着色，与额度页方向相反；统一为按剩余额度（越紧张越靠前）

---

## [0.2.0] — 2026-09-04

同步最新上游 **token-monitor**（至 v0.52），对齐最新 hub API 协议与品牌图标。

### 新增

- 新增 7 个工具的品牌图标与支持：**Cherry Studio**、**DeepSeek Harness**、**LM Studio**、**Qoder CN**、**Trae**、**Sub2API**、独立 **Third-party** 图标
- 数据模型对齐最新 hub 协议（`docs/API.md`）：
  - provider 新增 `adapterId`、`actionRequired`、`usageSummary` 字段
  - 额度窗口新增 `limitId`、`additional`、`detail` 字段，支持 `daily` 窗口类型
- 新工具品牌配色与名称标签；补齐上游模型归类规则（Kimi/K2d6/K3、MiniMax abab、Doubao Seed、混元 hy3、OpenCode big-pickle）
- 额度页：`daily` 窗口耗尽预估、账号验证提示（`actionRequired`）、额度窗口详情说明（如 Kimi 组成）

### 变更

- `commandcode` 品牌图标同步上游最新样式
- Third-party 额度改用专属 `thirdparty.svg`（不再复用 newapi 图标）
- 对旧版本 hub 保持容错解析，低版本返回字段自动忽略

### 修复

- 兼容上游新增的额度窗口类型与字段，避免新数据被丢弃

---

## [0.1.0] — 初始版本

首个版本：极光渐变 + Liquid Glass 玻璃质感，只读展示 Token Monitor hub 聚合数据。

- 首页 / 工具 / 模型 / 项目 / 会话 / 趋势 / 额度 / 设备 八大视图 + 设置
- SVG 品牌图标直用（38 个工具图标）
- GitHub 风格活动热力图、趋势面积图、工具排行
- 支持多种货币换算、深浅色主题、自定义 Hub URL 与鉴权