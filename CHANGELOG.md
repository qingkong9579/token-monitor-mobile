# Changelog

本应用随 [token-monitor](https://github.com/Javis603/token-monitor) 上游同步迭代。版本号独立于上游。

## [Unreleased]

### 修复

- **首页额度卡片对齐上游 home limits 模块**：改为纯文本列表（账户行 = 图标 + 名称，逐窗口「标签 + 剩余百分比」+ 重置时间行），去掉此前自创的进度条、按余量阈值的健康配色与 "stale" 字样——上游首页默认不画条、不标 stale；设备预览区去掉多余的分隔线
- **修复 hub 返回较大 payload（约 >16KB）时同步必然失败的问题**：响应体此前在主线程读取，触发 `NetworkOnMainThreadException`（只有小于 OkHttp 内部缓冲的响应侥幸可用）；现请求与响应体读取都在 IO 线程执行
- 额度页与首页对「无内容 provider」的过滤改用**展示层窗口**判断：MiMo/DeepSeek 这类仅有余额数据的 provider 现在与桌面端一致地显示（合成 Token Plan、Balance/Spend 备注行），不再因 wire 窗口为空被整卡隐藏
- 首页额度列表改用上游 `limitProviderCompactWindows` 语义取紧凑窗口（Codex 去掉 `additional` 子桶、Antigravity 每模型组取最紧窗口最多 2 组、MiMo 合成 Token Plan）
- 离线错误横幅现在附带具体错误信息，网络/解析失败不再只显示一句「网络异常」

## [Unreleased — v0.53/v0.54 同步]

同步最新上游 **token-monitor**（v0.54.0，main 分支），对齐提供方目录重构与额度子系统重构。

### 新增

- 三个新提供方的支持与品牌图标：**Alibaba Cloud**（Token Plan）、**Unsloth Studio**、**Kilo**（合并自 Kilo Code，统一为 `kilo`；`kilocode` 仍保留别名）
- 额度数据模型补齐：窗口按上游归一化使用 `remainingPercent`，尊重 `showMeter: false`；Codex 的 `additional` 子桶按桌面端默认展示（tone 0.78、独占一行，`gpt-reserve` 显示为 "Luna Reserve"）
- **余额型窗口的派生进度条**（对齐 `limitBalanceDisplay.creditsMeterPercent`）：充值型余额没有固定配额分母，改为按「当前余额 /(当前余额 + 本月已观测消耗)」可视化；该比例仅为展示层推导，永不写回协议
- 金额窗口的货币渲染：支持 `CREDITS` 点数余额（不带货币符号）、`¥`/`$`/`NT$`/`HK$`，大额自动紧凑显示
- Claude 的 `spend` 窗口（Usage credits）按上游显示「已用 / 上限」或「已用」，设了月度上限时带进度条
- MiMo 的 Token Plan 窗口在提供方未上报窗口时，从 `balance` 的 `planUsed`/`planLimit`/`planPercent` 合成；`planStatus = expired` 显示「套餐已过期」
- `BalanceBlock` 补齐 `planUsed`/`planLimit`/`planPercent`/`planStatus`/`giftBalance`/`cashBalance`/`expiresAt`/`trackingSince`/`monthSinceTracking`
- Balance / Spend / Usage credits 等备注行（`limit-window-note`）：仅标签 + 金额，无进度条
- Antigravity 按「模型 5-hour / weekly」分组展示（`limit-window-group`），组内仅一个窗口时独占整行

### 变更

- **额度页布局按桌面端 `renderProviderWindows` 逐 provider 重做**：
  - 卡片头部对齐 `.limit-head`：左侧图标 + 名称 + 更新时间（多账号才显示身份），右侧 `.limit-plan` 计划列（非 ok 状态显示中文状态标签，且不再在 meta 行重复）；stale 行整体淡化
  - **窗口网格语义对齐 `.limit-windows`（1fr 1fr）**：普通窗口两两并排，上游标记 `limit-window-wide` 的窗口独占整行（Codex Monthly、Cursor 全部、MiMo/Grok/Copilot/Kiro/Qoder/Zed、Zai MCP、volcengine 奇数尾行等），仅一个窗口时独占整行（`:only-child`）
  - **进度条 tone 逐窗口对齐上游**：session 0.95 / weekly·monthly 0.68 / daily 0.78 / 支出与月度资金 0.5 等；轨道按品牌色 16% 染色（`colorWithAlpha(color, 0.16)`），填充为品牌色 × tone，不再按余量阈值变色（首页卡片保留健康色数值）
  - 无上游 `label` 的窗口回退到各分支默认英文标签（Session / Weekly / Monthly / 5-hour / Credits 等）
  - Kiro/Qoder/Zed/Command Code 在重置行右侧显示绝对用量（`12/225`、`$8.78 / $10.00`）
- 新增 `Format.windowLabel`：窗口展示标签优先使用上游 `window.label`（如 "Session"/"Weekly"/"5-hour"），回退到本地化类型
- 新增 `Format.limitFillPercent`：余量填充归一助手，对齐桌面端 `limitFillPercent`
- 提供方名称对齐上游 `limitProviders.js`：`zai` → GLM、`trae` → Trae CN、新增 `zed`；客户端名称 `qwen` → Qwen、新增 `kilo`
- 品牌配色对齐上游 `usageCharts.js`：`alibaba` #615CED、`unsloth` #40B85A、`thirdparty` #8090A6；MiMo 额度行使用 Xiaomi 品牌色、三方按 `adapterId` 取色（newapi #C738FB / sub2api / custom）

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