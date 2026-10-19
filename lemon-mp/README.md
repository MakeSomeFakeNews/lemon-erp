# lemon-mp 小程序端

柠檬ERP 的微信小程序（uni-app + Vue 3），面向随时随地、操作轻的场景：审批、消息、工作台、员工自助、销售外勤、仓库扫码作业与报工。后端接口与 Web 端共用，权限、数据权限、多租户都由后端控制。

## 功能取舍

| 放进小程序 | 留在电脑端 |
| --- | --- |
| 审批：待办 / 已办 / 我发起的 / 抄送我的，同意、拒绝、转交、撤回；发起申请（流程中心设计的表单）；审批提醒（微信订阅消息） | 系统配置：菜单、角色、字典、参数、租户、打印模板、编码规则、第三方集成 |
| 工作台（待办汇总、经营概览）、站内消息 | 流程设计器、表单设计、BI 多维分析、自定义报表、数据大屏 |
| 员工自助：打卡、考勤日历、假期余额、请假加班申请与撤销、工资条、绩效自评与评分；项目工时填报与审核 | 字段多、明细行多的单据录入：采购 / 销售订单、BOM、工艺路线、凭证、工资表、MRP、成本核算、结账 |
| 销售与服务：客户、联系人拨号、写跟进（拍照）、今日待跟进、线索录入与转化、商机推进、服务工单受理与处理、销售订单进度（按订单号或客户查询） | 打印、Excel 导入导出、供应商 / 经销商门户 |
| 仓储生产：库存查询、扫码收货、上架、拣货、盘点、生产报工 | 工单指派、配件领用、不合格品评审处置、检验方案维护 |
| 设备与质量：设备台账（扫码）、保养维修登记与处理、来料 / 过程 / 成品 / 首件检验录入、不合格品登记 | |

首页待办、消息中的链接按 `src/config/entries` 中登记的 Web 端路由前缀转到小程序页面，没有对应页面的提示「请在电脑端处理」。

## 开发

```bash
cd lemon-mp && npm install
npm run dev:mp-weixin      # 编译到 dist/dev/mp-weixin，用微信开发者工具导入该目录
npm run build:mp-weixin    # 发布包 dist/build/mp-weixin
npm run dev:h5             # 浏览器调试（没有微信登录，直接用账号密码登录）
npm run i18n:check         # 多语言检查
```

- 接口地址：复制 `.env.example` 为 `.env`，设置 `VITE_API_BASE`（含 `/api`）。正式版必须是在微信公众平台「开发管理 - 服务器域名」中配置的 HTTPS 域名；开发时可在开发者工具中勾选「不校验合法域名」。H5 调试时设为 `/api`，开发服务器代理到 `VITE_PROXY_TARGET`（默认 `http://localhost:8080`，写在 `.env` 或命令行中都可以，命令行优先）。
- AppID：填写 `src/manifest.json` 的 `mp-weixin.appid`；后端设置同一个小程序的 `LEMON_MINIAPP_APP_ID` / `LEMON_MINIAPP_APP_SECRET`（`lemon.miniapp.*`）。

## 登录

`wx.login` 的 code 交给后端 `POST /auth/miniapp/login` 换取 openid（openid 不返回前端）：

1. 已绑定一个账号：直接登录；
2. 同一个微信绑定了多个租户的账号（SaaS 模式）：选择租户（`/auth/miniapp/choose`）；
3. 未绑定：输入账号密码（SaaS 模式还要填租户），校验与 Web 端登录相同（验证码、失败锁定、登录日志），成功后绑定当前微信（`/auth/miniapp/bind`）。

**审批提醒**：后端配置订阅消息模板后（见 docs/development.md「小程序」），小程序启动时读取模板 ID（`/auth/miniapp/config`），在用户点击时申请授权（`utils/subscribe.js` 的 `requestSubscribe`，微信要求在点击事件中同步调用）：微信登录按钮、工作台待办、审批的提交、发起申请、「我的 - 审批提醒」。每次授权可推送一条，用户勾选「总是保持」后不再弹窗。

「使用其他账号登录」（`switchAccount`）不自动登录，用于绑定另一个账号；「我的 - 解除微信绑定」删除绑定。门户账号、停用账号不能登录。密码由管理员设置或已过期时，登录后先进入修改密码页。

## 约定

- 目录：`pages` 是主包（底部导航四个页面、登录、修改密码），功能区的页面放在分包 `pkg-approval`、`pkg-hr`、`pkg-pm`、`pkg-crm`、`pkg-wms`、`pkg-mes`（分包内可以有只给本区用的组件和工具函数，如 `pkg-crm/photos.js`）；接口、语言包、工作台入口按功能区分文件，但统一放在 `src` 下：`src/api/<区>.js`、`src/locales/<语言>/<区>.js`、`src/config/entries/<区>.js`。
- 新功能入口在 `src/config/entries/<区>.js` 登记：`perm`（任一权限，空为登录即可）、`permAll`（还必须同时具备的权限）、`links`（对应的 Web 端路由前缀）。
- 通用组件 `components/l-*`（easycom 自动引入）：`l-card`、`l-cell`、`l-field`、`l-tag`、`l-empty`、`l-load-more`、`l-segmented`、`l-scan-input`；组合函数 `usePaged`（下拉刷新 + 触底加载）、`runAction`（确认 → 调接口 → 提示 → 回调）、`useTitle`（按语言设置标题）、`scanCode` / `vibrate`。
- 多语言：小程序不能运行 vue-i18n 的消息编译，用 `src/locales/index.js` 中的最小实现（`t('a.b', { n })`）；代码中不写中文；字典英文译文、后端枚举常量与 Web 端共用（`@web/locales/en-US/dict.js`、`@web/constants/enums`）。`pages.json` 只能写中文，页面标题和底部导航运行时按语言设置。
- 小程序中没有 DOM、`window`、`localStorage`，用 `uni.*` API；平台差异用条件编译（`#ifdef MP-WEIXIN`）。
