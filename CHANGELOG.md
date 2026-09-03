# Changelog

本应用随 [token-monitor](https://github.com/Javis603/token-monitor) 上游同步迭代。版本号独立于上游。

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