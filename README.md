<p align="center">
  <img src="lemon-web/public/favicon.svg" width="96" alt="柠檬ERP" />
</p>

<h1 align="center">柠檬ERP · Lemon ERP</h1>

<p align="center">
  <b>让企业经营像柠檬一样清新高效</b><br />
  一套系统打通采购、仓储、生产、质量、销售、财务、人力、客户、项目与商务智能
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg" alt="license" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg" alt="spring boot" />
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5-blue.svg" alt="mybatis-plus" />
  <img src="https://img.shields.io/badge/Flowable-8.0-ff6a00.svg" alt="flowable" />
  <img src="https://img.shields.io/badge/Vue-3.5-42b883.svg" alt="vue" />
  <img src="https://img.shields.io/badge/Ant%20Design%20Vue-4.2-1677ff.svg" alt="antdv" />
</p>

<p align="center">
  <a href="https://erp.dqq.me"><b>在线演示</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/"><b>官网</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/manual/guide/"><b>用户手册</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/dev/start/"><b>开发文档</b></a>
</p>

<p align="center">
  <a href="#项目简介">项目简介</a> ·
  <a href="#在线演示">在线演示</a> ·
  <a href="#核心亮点">核心亮点</a> ·
  <a href="#功能模块">功能模块</a> ·
  <a href="#系统架构">系统架构</a> ·
  <a href="#系统截图">系统截图</a> ·
  <a href="#快速开始">快速开始</a>
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.jpg" alt="柠檬ERP 工作台" />
</p>

## 项目简介

柠檬ERP 是一套面向中小制造、商贸企业的**全功能开源 ERP**，前后端分离，开箱即用。

- **业务全覆盖**：13 大模块、140+ 功能页面，从请购到收款、从 BOM 到完工入库、从凭证到报表，一套系统闭环
- **单据自动联动**：出入库自动更新库存、生成应收应付，业务单据一键生成会计凭证，账实一致
- **技术栈新**：Spring Boot 4 + Vue 3 + Ant Design Vue 4，Gradle 多模块、单向依赖，代码结构清晰，便于二次开发
- **完全开源**：Apache-2.0 协议，个人与企业均可免费商用

**开发初衷**：为中小企业提供开源、好用、完整的 ERP，降低企业信息化成本；让开发者和软件公司可以基于柠檬ERP 快速交付项目；也为初学者提供一个学习企业级 Java 开发的完整范例。

> 如果这个项目对你有帮助，请点一下右上角的 **Star**，这是对我们最大的鼓励！

## 在线演示

演示地址：**https://erp.dqq.me**

| 租户 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 柠檬科技 | admin | admin123 | 超级管理员，可以浏览全部功能 |
| 柠檬科技 | test | admin123 | 普通用户（只读），数据权限为「本部门及以下」 |

> 演示环境开启了演示模式，只能查看，不能新增、修改、删除数据。完整体验请参考 [快速开始](#快速开始) 自行部署。

## 核心亮点

<table>
  <tr>
    <td width="33%" valign="top">
      <h4>可视化审批流</h4>
      仿钉钉流程设计器，支持审批人、抄送、条件分支、并行分支，会签 / 或签；采购、销售、请假、报销等业务单据一键接入审批
    </td>
    <td width="33%" valign="top">
      <h4>SaaS 多租户</h4>
      共享库按租户隔离，租户套餐限定可用功能，到期与账号数控制；新租户自动开通管理员并初始化基础数据
    </td>
    <td width="33%" valign="top">
      <h4>精细权限</h4>
      菜单 / 按钮 / 接口三级权限；数据权限支持全部、自定义部门、本部门、本部门及以下、仅本人，多角色取并集
    </td>
  </tr>
  <tr>
    <td valign="top">
      <h4>MRP 与生产执行</h4>
      主生产计划 → MRP 多级展开计算净需求 → 一键下达工单与采购；齐套分析、超领控制、报工、完工入库全流程
    </td>
    <td valign="top">
      <h4>业财一体化</h4>
      应收应付由出入库自动生成，单据一键生成凭证；三大报表、成本核算、标准成本差异、预算执行分析
    </td>
    <td valign="top">
      <h4>移动作业与门户</h4>
      手机 / PDA 扫码收货、上架、拣货、盘点；供应商门户（订单确认、送货、对账、投标）与经销商门户
    </td>
  </tr>
  <tr>
    <td valign="top">
      <h4>单据打印与编码规则</h4>
      JasperReports 生成 PDF，自动分页、多种纸张，可视化配置或上传 JRXML，支持预印单据纸套打（拖放定位）；单据编号规则可配置（前缀、日期、流水号）
    </td>
    <td valign="top">
      <h4>现代化界面</h4>
      顶部模块导航 + 侧边栏分组、多标签页、深色模式、全局菜单搜索、固定表头、列设置
    </td>
    <td valign="top">
      <h4>安全与监控</h4>
      JWT + 服务端会话可强制下线，登录验证码、密码策略、水印；操作 / 登录日志、服务与缓存监控、定时任务
    </td>
  </tr>
</table>

## 功能模块

### 供应链管理

| 分组 | 功能 |
| --- | --- |
| 采购 | 请购单、采购订单、送货通知、采购入库、采购退货、补货建议 |
| 采购寻源 | 询价比价、招标采购（密封投标、开标比价、定标）、采购合同 / 框架协议 |
| 销售 | 销售报价、价目表（客户专属价、等级、数量阶梯）、价格控制（最低售价 / 最高采购价）、历史成交价、促销活动、销售返利、销售提成（销售额 / 毛利 / 回款，阶梯规则，计入工资）、渠道库存、销售订单、发货通知、波次拣货、销售出库、销售退货、物流发运、欠交清单、寄售结算 |
| 往来单位 | 供应商（供货价格、绩效评估）、客户（信用额度、账期） |
| 门户 | 供应商门户（订单确认、送货通知、对账、投标、质量索赔）、经销商门户（订货、渠道库存上报） |

### 仓库管理

| 分组 | 功能 |
| --- | --- |
| 库存作业 | 入库单、出库单、调拨单、盘点单（草稿 → 确认后更新库存）、组装拆卸单（按 BOM 带出子件，成本自动结转） |
| 库存查询 | 库存查询（明细 / 汇总、安全库存预警、临期提示）、收发存汇总、库存流水、批次与序列号追踪 |
| 库存控制 | 库存预留、库存冻结 |
| 基础资料 | 物料（批次 / 序列号管理、计划属性、多规格 SKU 与尺码矩阵录入）、规格属性、仓库、库位（区域与上架策略） |
| 移动作业 | 手机 / PDA 扫码收货、上架、拣货、盘点 |

### 生产制造

| 分组 | 功能 |
| --- | --- |
| 生产计划 | 主生产计划（可由销售订单生成）、MRP 运算（多级展开、低层码、净需求）、产能排程 |
| 车间执行 | 生产工单、齐套分析与缺料下单、领料 / 退料（超领控制）、补料申请、报工、完工入库 |
| 工艺与设备 | 物料清单与工艺路线（多版本）、工程变更、技术文档、工作中心、设备台账、设备维护、设备 OEE、模具工装 |

### 质量管理

| 分组 | 功能 |
| --- | --- |
| 检验管理 | 检验方案（定性 / 定量、GB/T 2828.1 AQL 抽样）、来料 / 过程 / 成品 / 首件检验，自动判定 |
| 不良处理 | 不合格品处理（MRB，联动冻结、报废、返工）、纠正预防（8D）、供应商索赔 |
| 质量分析 | 合格率趋势、不良柏拉图、供应商质量排名、SPC 控制图与 Cp / Cpk |

### 财务管理

| 分组 | 功能 |
| --- | --- |
| 总账 | 凭证管理（审核、记账、冲销）、账簿查询（余额表、明细账）、财务报表（资产负债表、利润表、现金流量表） |
| 应收应付 | 应收 / 应付（出入库自动生成）、收款单、付款单、预收预付、发票管理（数电票 XML / 发票清单导入、自动勾稽）、供应商对账、往来对账单（客户 / 供应商，可打印）、账龄分析 |
| 资金与费用 | 出纳管理（现金 / 银行日记账、资金日报）、费用报销、员工借款、银行对账、票据管理、账户调拨、资金计划 |
| 资产与成本 | 固定资产与折旧、资产盘点、低值易耗品、成本核算（月末一次加权平均 / 移动加权平均 / 先进先出，工单料工费）、标准成本与差异分析 |
| 预算与分析 | 预算编制与执行、经营分析（毛利汇总与单据明细、存货对账、库龄分析） |
| 期末与设置 | 期末结账（自动结转损益）、会计科目、凭证科目设置（自动凭证科目可配置，可按物料分类 / 职位类别区分）、期初余额与启用向导（期初库存、期初往来 Excel 导入，往来与科目期初核对）、币种汇率 |

### 客户关系

| 分组 | 功能 |
| --- | --- |
| 客户经营 | 客户（我的 / 团队 / 公海，领取、分配、回收）、线索、销售机会（阶段看板、赢单转销售订单）、跟进记录、合同管理 |
| 服务与营销 | 服务工单（处理时限、保修判断、满意度）、市场活动（成本、ROI）、客户分析（漏斗、RFM、销售排行） |

### 项目管理

| 分组 | 功能 |
| --- | --- |
| 项目与计划 | 项目列表与成员、计划进度（WBS 任务树、前置任务、甘特图）、资源分配（负荷分析） |
| 工时与成本 | 工时填报与审批、成本控制（预算、费用、领料） |
| 项目核算 | 收款计划、开票生成应收、完工百分比确认收入、挣值分析 |

### 人力资源

| 分组 | 功能 |
| --- | --- |
| 员工管理 | 员工自助、员工档案（入职 / 转正 / 调岗 / 离职）、劳动合同、职位管理 |
| 招聘培训 | 招聘需求 → 候选人 → 面试 → 录用入职、培训计划与记录 |
| 考勤绩效 | 班次、打卡与补卡、考勤汇总、请假 / 加班审批、假期余额、绩效考核 |
| 薪酬福利 | 薪资档案、薪酬核算（考勤扣款、加班费、计件工资、销售提成、社保公积金、个税）、社保方案、专项附加扣除 |

### 商务智能

| 分组 | 功能 |
| --- | --- |
| 分析展示 | 经营驾驶舱、多维分析（透视、筛选、下钻）、自定义报表、自定义仪表盘（可共享） |
| 决策支持 | 销售预测、ABC 分析、呆滞物料、预警规则 |
| 数据仓库 | 销售 / 采购 / 库存 / 生产 / 财务事实表，定时与手动同步 |

### 流程中心

| 分组 | 功能 |
| --- | --- |
| 我的事务 | 我的待办、发起申请、我的申请、我的已办、抄送我的 |
| 流程管理 | 流程模型（可视化设计器 + 表单设计，一键发布）、流程实例（进度图、审批记录、终止） |

### 系统管理与监控

| 分组 | 功能 |
| --- | --- |
| 系统管理 | 用户、部门（组织架构树）、角色（菜单 / 数据权限）、菜单、字典、租户、租户套餐、编码规则、打印模板、参数设置、业务参数（超收超发、信用控制等）、自定义字段 |
| 系统监控 | 在线用户（强制下线）、操作日志、登录日志（IP 归属地）、服务监控、缓存监控、定时任务 |
| 平台功能 | 站内消息、Excel 导入导出、单据附件、字段变更历史、自定义字段（不改表结构扩展单据和主数据的字段） |

> 每个功能的详细操作说明请阅读 [用户手册](https://makesomefakenews.github.io/lemon-erp-website/manual/guide/)，二次开发请阅读 [开发文档](https://makesomefakenews.github.io/lemon-erp-website/dev/start/)。

## 系统架构

### 业务架构

蓝色虚线是模块内的业务流程，灰色虚线是跨模块的自动联动（生成下游单据、更新库存、生成应收应付与凭证）。

![柠檬ERP 业务架构图](docs/architecture/business.png)

### 技术架构

![柠檬ERP 技术架构图](docs/architecture/tech.png)

## 系统截图

### 登录与首页

**登录页**

![登录页](docs/screenshots/login.jpg)

**登录页（深色模式）**

![登录页（深色模式）](docs/screenshots/login-dark.jpg)

**工作台首页**

![工作台首页](docs/screenshots/dashboard.jpg)

**工作台首页（深色模式）**

![工作台首页（深色模式）](docs/screenshots/dashboard-dark.jpg)

### 供应链管理

**采购订单**

![采购订单](docs/screenshots/scm-purchase.jpg)

**采购订单详情**

![采购订单详情](docs/screenshots/scm-purchase-detail.jpg)

**销售订单详情**

![销售订单详情](docs/screenshots/scm-sales-detail.jpg)

**供应商评估**

![供应商评估](docs/screenshots/scm-supplier-eval.jpg)

### 仓库管理

**库存查询**

![库存查询](docs/screenshots/wms-stock.jpg)

**批次与序列号追踪**

![批次与序列号追踪](docs/screenshots/wms-trace.jpg)

**物料管理**

![物料管理](docs/screenshots/wms-material.jpg)

**库位管理**

![库位管理](docs/screenshots/wms-location.jpg)

**手机 / PDA 移动作业**

![手机 / PDA 移动作业](docs/screenshots/mobile.jpg)

### 生产制造

**生产计划**

![生产计划](docs/screenshots/mes-plan.jpg)

**MRP 运算**

![MRP 运算](docs/screenshots/mes-mrp.jpg)

**产能排程**

![产能排程](docs/screenshots/mes-capacity.jpg)

**齐套分析**

![齐套分析](docs/screenshots/mes-kit.jpg)

**生产工单详情**

![生产工单详情](docs/screenshots/mes-wo-detail.jpg)

**物料清单（BOM）**

![物料清单（BOM）](docs/screenshots/mes-bom-detail.jpg)

### 质量管理

**质量检验**

![质量检验](docs/screenshots/qms-inspection.jpg)

**不合格品处理**

![不合格品处理](docs/screenshots/qms-ncr.jpg)

**SPC 控制图**

![SPC 控制图](docs/screenshots/qms-spc.jpg)

**质量分析**

![质量分析](docs/screenshots/qms-analysis.jpg)

### 财务管理

**凭证管理**

![凭证管理](docs/screenshots/fin-voucher.jpg)

**记账凭证**

![记账凭证](docs/screenshots/fin-voucher-detail.jpg)

**科目余额表**

![科目余额表](docs/screenshots/fin-book.jpg)

**资产负债表**

![资产负债表](docs/screenshots/fin-report.jpg)

**应收管理**

![应收管理](docs/screenshots/fin-ar.jpg)

**固定资产**

![固定资产](docs/screenshots/fin-asset.jpg)

**成本核算**

![成本核算](docs/screenshots/fin-cost.jpg)

**预算管理**

![预算管理](docs/screenshots/fin-budget.jpg)

**经营分析**

![经营分析](docs/screenshots/fin-analysis.jpg)

### 客户关系

**销售机会看板**

![销售机会看板](docs/screenshots/crm-opportunity.jpg)

**客户分析**

![客户分析](docs/screenshots/crm-analysis.jpg)

**客户管理**

![客户管理](docs/screenshots/crm-customer.jpg)

**服务工单**

![服务工单](docs/screenshots/crm-ticket.jpg)

### 项目管理

**计划进度（甘特图）**

![计划进度（甘特图）](docs/screenshots/pm-gantt.jpg)

**项目列表**

![项目列表](docs/screenshots/pm-project.jpg)

**资源分配**

![资源分配](docs/screenshots/pm-resource.jpg)

**项目核算**

![项目核算](docs/screenshots/pm-accounting.jpg)

### 人力资源

**员工档案**

![员工档案](docs/screenshots/hr-employee.jpg)

**考勤管理**

![考勤管理](docs/screenshots/hr-attendance.jpg)

**薪酬核算**

![薪酬核算](docs/screenshots/hr-payroll.jpg)

**绩效考核**

![绩效考核](docs/screenshots/hr-perf.jpg)

**招聘管理**

![招聘管理](docs/screenshots/hr-recruit.jpg)

### 商务智能

**经营驾驶舱**

![经营驾驶舱](docs/screenshots/bi-cockpit.jpg)

**多维分析**

![多维分析](docs/screenshots/bi-olap.jpg)

**自定义仪表盘**

![自定义仪表盘](docs/screenshots/bi-board.jpg)

### 流程中心

**流程设计器**

![流程设计器](docs/screenshots/wf-designer.jpg)

**流程实例**

![流程实例](docs/screenshots/wf-instance.jpg)

### 系统管理与监控

**用户管理**

![用户管理](docs/screenshots/sys-user.jpg)

**部门管理**

![部门管理](docs/screenshots/sys-dept.jpg)

**角色管理**

![角色管理](docs/screenshots/sys-role.jpg)

**打印模板设计**

![打印模板设计](docs/screenshots/sys-print-template.jpg)

**服务监控**

![服务监控](docs/screenshots/monitor-server.jpg)

**缓存监控**

![缓存监控](docs/screenshots/monitor-cache.jpg)

**定时任务**

![定时任务](docs/screenshots/monitor-job.jpg)

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 4.1、Spring Security、JWT |
| 持久层 | MyBatis-Plus 3.5（多租户、数据权限、分页插件） |
| 工作流 | Flowable 8.0 |
| 缓存 | Redis（可切换为内存缓存） |
| 构建 | Gradle 多模块 |
| 前端框架 | Vue 3.5、Vite 7、Pinia 3、Vue Router 4 |
| UI 组件 | Ant Design Vue 4.2 |
| 图表 | ECharts 5 |

## 环境要求

| | 开发环境 | 服务器环境 |
| --- | --- | --- |
| Java | JDK 17 | JRE 17 |
| 数据库 | MySQL 8.0+ | MySQL 8.0+ |
| 缓存 | Redis 6.2+ | Redis 6.2+ |
| 前端 | Node 20+ | Nginx 1.20+ |
| 其他 | IntelliJ IDEA 2025+，Gradle 使用项目自带 Wrapper | Windows / Linux / macOS |

## 快速开始

**1. 初始化数据库**

```bash
mysql -uroot -p -e "CREATE DATABASE lemon_erp DEFAULT CHARSET utf8mb4"
mysql -uroot -p lemon_erp < sql/init.sql   # 注意：会删除并重建全部表
```

**2. 启动后端**（项目根目录执行，默认连接本机 MySQL `root / root` 和本机 Redis）

```bash
# 可通过环境变量覆盖：DB_URL、DB_USERNAME、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD
DB_PASSWORD=你的密码 ./gradlew bootRun
```

**3. 启动前端**

```bash
cd lemon-web
npm install
npm run dev
```

访问 http://localhost:5173 即可（`/api` 自动代理到 http://localhost:8080）。

**4. 打包部署**

```bash
./gradlew build                     # 生成 lemon-admin/build/libs/lemon-erp.jar
cd lemon-web && npm run build       # 生成 lemon-web/dist，交给 Nginx 托管

# 生产环境配置全部取环境变量，JWT_SECRET 至少 32 字节
java -jar lemon-erp.jar --spring.profiles.active=prod
```

<details>
<summary>Nginx 参考配置</summary>

```nginx
location / {
    root /path/to/lemon-web/dist;
    try_files $uri $uri/ /index.html;
}
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

</details>

### 默认账号

| 租户 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 柠檬科技 | admin | admin123 | 超级管理员，拥有全部权限 |
| 柠檬科技 | test | admin123 | 普通用户，只读权限，数据权限为「本部门及以下」 |

> 租户输入框只在开启 SaaS 多租户（`LEMON_TENANT_ENABLED=true`）时显示，默认关闭。
>
> 搭建对外演示环境时设置 `LEMON_DEMO_ENABLED=true` 开启演示模式：所有用户只能查看，不能新增、修改、删除数据。

## 项目结构

```
lemon-erp
├── lemon-common        通用层：统一返回、异常、基础实体、工具类
├── lemon-framework     框架层：安全、数据权限、多租户、缓存、日志、模块间契约接口
├── lemon-modules       业务模块
│   ├── lemon-system    系统管理        ├── lemon-fin   财务管理
│   ├── lemon-monitor   系统监控        ├── lemon-hr    人力资源
│   ├── lemon-workflow  流程中心        ├── lemon-crm   客户关系
│   ├── lemon-dashboard 工作台          ├── lemon-pm    项目管理
│   ├── lemon-wms       仓库管理        └── lemon-bi    商务智能
│   ├── lemon-scm       供应链管理
│   └── lemon-mes       生产制造 + 质量管理
├── lemon-admin         启动模块：组装全部业务模块，打包为可执行 jar
├── lemon-web           前端（Vite + Vue 3 + Ant Design Vue）
├── sql/init.sql        建表与初始数据
└── docs                开发文档与截图
```

## 开源说明

- 本系统 **100% 开源**，遵守 [Apache-2.0](LICENSE) 协议，个人与企业均可免费商用
- 使用时请保留原作者版权声明与许可证文件
- 欢迎提交 [Issue](https://github.com/MakeSomeFakeNews/lemon-erp/issues) 反馈问题，也欢迎提交 Pull Request 一起完善

## 如何支持

- 开源不易，坚持更难！如果您觉得柠檬ERP 不错，请点一个 **Star** 支持一下
- 把柠檬ERP 推荐给身边做生意、办工厂、做软件的朋友
- 遇到问题或有好的建议，欢迎在 Issues 中交流

---

<p align="center">Apache License 2.0 © 2026 柠檬ERP</p>
