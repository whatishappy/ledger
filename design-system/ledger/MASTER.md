# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** Ledger
**Generated:** 2026-08-18 22:31:52
**Category:** Personal Finance Tracker
**Design Dials:** Density 7/10 (Standard)

---

## Global Rules

### Color Palette

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#1E40AF` | `--color-primary` |
| On Primary | `#FFFFFF` | `--color-on-primary` |
| Secondary | `#3B82F6` | `--color-secondary` |
| On Secondary | `#FFFFFF` | `--color-on-secondary` |
| Accent/CTA | `#059669` | `--color-accent` |
| On Accent/CTA | `#FFFFFF` | `--color-on-accent` |
| Background | `#0F172A` | `--color-background` |
| Foreground | `#FFFFFF` | `--color-foreground` |
| Card | `#192134` | `--color-card` |
| Card Foreground | `#FFFFFF` | `--color-card-foreground` |
| Muted | `#101A34` | `--color-muted` |
| Muted Foreground | `#94A3B8` | `--color-muted-foreground` |
| Border | `rgba(255,255,255,0.08)` | `--color-border` |
| Destructive | `#DC2626` | `--color-destructive` |
| On Destructive | `#FFFFFF` | `--color-on-destructive` |
| Ring | `#FFFFFF` | `--color-ring` |

**Color Notes:** Trust blue + profit green on dark（暗色优先）

**Light 模式（Element Plus 亮色默认覆盖）：**

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#1E40AF` | `--el-color-primary` |
| Background | `#F8FAFC` | `--el-bg-color` |
| Card / Overlay | `#FFFFFF` | `--el-bg-color-overlay` |
| Border | `#E2E8F0` | `--el-border-color` |
| Foreground | `#0F172A` | `--el-text-color-primary` |
| Muted | `#64748B` | `--el-text-color-secondary` |
| Accent/Income | `#059669` | `--color-income` |
| Destructive/Expense | `#DC2626` | `--color-expense` |

**财务语义色（两模式一致）：**

| 语义 | Light | Dark | 用途 |
|------|-------|------|------|
| 收入 `type=1` | `#059669` | `#22C55E` | 收入 Tag、收入金额 |
| 支出 `type=0` | `#DC2626` | `#F87171` | 支出金额（负数红）、删除动作 |
| 预算 <60% | `#059669` | `#22C55E` | 进度条正常 |
| 预算 <90% | `#EAB308` | `#FBBF24` | 进度条预警 |
| 预算 ≥100% | `#DC2626` | `#F87171` | 进度条超支 + 卡片红条 |

> **对比度约束**：正文对背景 ≥4.5:1；Accent 绿在按钮上只用白字（`#FFFFFF` on `#059669` ≈ 4.6:1），禁用黑字。

### Typography

- **Latin/Display:** Figtree（干净、专业的无衬线）— 来源：数据库最近匹配 "Medical Clean"（Figtree + Noto Sans），按中文财务场景适配
- **中文/正文：** 系统栈 `PingFang SC, Microsoft YaHei, Noto Sans SC, sans-serif`（不额外加载 CJK webfont，避免体积与闪烁）
- **金额/数字：** `font-variant-numeric: tabular-nums`（表格与仪表盘数字等宽对齐）
- **Mood:** clean, precise, trustworthy（财务场景，**禁止**手写/休闲风格）
- **字号：** 正文 14px（Element Plus 默认），登录页大留白场景 base 16px，仪表盘金额 24-28px 用 tabular-nums
- **Google Fonts（仅拉丁）：** [Figtree](https://fonts.googleapis.com/css2?family=Figtree:wght@300;400;500;600;700&display=swap)

**CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Figtree:wght@300;400;500;600;700&display=swap');
```

### Spacing Variables

*Density: 7/10 — Standard*

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs

### Buttons

```css
/* Primary Button — 信任蓝（主 CTA），非绿色；绿色仅表收入/正向 */
.btn-primary {
  background: var(--color-primary, #1E40AF);
  color: #FFFFFF;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: background 200ms ease;
  cursor: pointer;
}

.btn-primary:hover {
  background: #1E3A8A;
}

/* 收入确认类按钮可用绿色（记账弹窗的「收入」提交态） */
.btn-success {
  background: #059669;
  color: #FFFFFF;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: background 200ms ease;
  cursor: pointer;
}

/* Secondary Button — 描边蓝 */
.btn-secondary {
  background: transparent;
  color: #1E40AF;
  border: 1px solid #1E40AF;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}
```

### Cards

```css
.card {
  background: var(--color-card, #FFFFFF);
  border: 1px solid var(--el-border-color, #E2E8F0);
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 200ms ease;
}

.card:hover {
  box-shadow: var(--shadow-md);
}
```

### Inputs

```css
.input {
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: #1E40AF;
  outline: none;
  box-shadow: 0 0 0 3px #1E40AF20;
}
```

### Modals

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

---

## Style Guidelines

**Style:** Glassmorphism

**Keywords:** Frosted glass, transparent, blurred background, layered, vibrant background, light source, depth, multi-layer

**Best For:** Modern SaaS, financial dashboards, high-end corporate, lifestyle apps, modal overlays, navigation

**Key Effects:** Backdrop blur (10-20px), subtle border (1px solid rgba white 0.2), light reflection, Z-depth

### Page Pattern

**N/A — 本产品为功能性记账应用，非落地页。** 数据库返回的 "Product Demo + Features" 是营销落地页模式（Hero/Video/CTA），不适用于本应用。页面结构以《前端页面设计方案.md》为准：

- 登录页（品牌面板 + 表单）→ 仪表盘 → 账目表 → 预算
- 通用骨架：左侧菜单 + 顶栏（面包屑/主题/头像下拉）+ 内容区
- 数据密集区（表格/图表）用**实心卡片**承载，玻璃拟态仅限登录页品牌面板与顶栏

### Style 落地约束

**Style:** Glassmorphism（选择性使用）

- 登录页左侧品牌面板：`backdrop-filter: blur(12px)` + 渐变背景 + 半透明卡片
- 顶栏/弹窗遮罩：`backdrop-filter: blur(4px)`（如 MASTER 中 modal-overlay 实现）
- **数据表面（表格/图表/预算卡片）禁用玻璃**——实心卡片 + 1px 描边 + 轻阴影，保证可读性与渲染性能

### Element Plus 主题映射（Vue 栈）

> **stack 检索 0 结果**：`element plus theming dark mode --stack vue` 无数据库匹配，以下为通用最佳实践回退。前端设计文档 §8 已预留 `isDark + config-provider + dark/css-vars.css`，此处填充具体 token。

```css
/* 亮色（默认） */
:root {
  --el-color-primary: #1E40AF;
  --el-color-primary-light-3: #6D8AD4;
  --el-color-primary-light-5: #A0B4E2;
  --el-color-primary-light-7: #D3DDF1;
  --el-color-primary-light-8: #E9EFF9;
  --el-color-primary-light-9: #F4F7FC;
  --el-bg-color: #F8FAFC;
  --el-bg-color-overlay: #FFFFFF;
  --el-fill-color-blank: #FFFFFF;
  --el-border-color: #E2E8F0;
  --el-border-color-light: #EDF1F7;
  --el-text-color-primary: #0F172A;
  --el-text-color-regular: #334155;
  --el-text-color-secondary: #64748B;
  --el-border-radius-base: 8px;
}

/* 暗色：html.dark + element-plus/theme-chalk/dark/css-vars.css */
html.dark {
  --el-color-primary: #3B82F6;
  --el-color-primary-light-3: #60A5FA;
  --el-color-primary-light-5: #93C5FD;
  --el-color-primary-light-7: #C7DCFE;
  --el-color-primary-light-8: #DCEAFE;
  --el-color-primary-light-9: #EFF6FF;
  --el-bg-color: #0F172A;
  --el-bg-color-overlay: #192134;
  --el-fill-color-blank: #192134;
  --el-border-color: rgba(255,255,255,0.12);
  --el-border-color-light: rgba(255,255,255,0.08);
  --el-text-color-primary: #F8FAFC;
  --el-text-color-regular: #CBD5E1;
  --el-text-color-secondary: #94A3B8;
}
```

> 切换逻辑：`isDark` store → `document.documentElement.classList.toggle('dark', isDark)` → `el-config-provider` 包整棵 App。金额/正文配 `font-variant-numeric: tabular-nums`。

### ECharts 主题

```js
// 与 Element Plus 亮/暗联动：chart.setOption 时读取 isDark 决定 textColor/axisLine
const CHART_PALETTE = ['#1E40AF', '#059669', '#3B82F6', '#EAB308', '#F59E0B', '#8B5CF6', '#DC2626'];
```

- **7 日支出折线**：平滑 + 区域渐变（主色 10% 透明度）+ tooltip 显示数值；X 轴 7 个点，无数据日期补 0
- **分类支出环形**：`legend` + 百分比 label + tooltip（**不以颜色为唯一信息载体**）；非前 N 项折叠进「其他」
- **预算进度条**：色值按 <60% 绿 / <90% 黄 / ≥100% 红（见财务语义色表）
- 无障碍：折线/环形均带数值 tooltip，鼠标 focus 与 hover 展示一致值；禁用纯色编码

---

## Anti-Patterns (Do NOT Use)

- ❌ Pure white backgrounds

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
