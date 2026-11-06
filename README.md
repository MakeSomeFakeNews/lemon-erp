<p align="center">
  <img src="lemon-web/public/favicon.svg" width="96" alt="柠檬ERP" />
</p>

<h1 align="center">柠檬ERP · Lemon ERP</h1>

<p align="center">
  <b>真正 100% 开源、无功能限制的 ERP 系统</b><br />
  让企业经营像柠檬一样清新高效：进销存、生产、仓储、质量、财务、人力、客户、项目、商务智能与 AI，一套系统全部打通
</p>

<p align="center">
  <a href="https://github.com/MakeSomeFakeNews/lemon-erp/stargazers"><img src="https://img.shields.io/github/stars/MakeSomeFakeNews/lemon-erp?style=flat&logo=github" alt="stars" /></a>
  <img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg" alt="license" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg" alt="spring boot" />
  <img src="https://img.shields.io/badge/Vue-3.5-42b883.svg" alt="vue" />
  <img src="https://img.shields.io/badge/Ant%20Design%20Vue-4.2-1677ff.svg" alt="antdv" />
  <img src="https://img.shields.io/badge/Flowable-8.0-ff6a00.svg" alt="flowable" />
</p>

<p align="center">
  <a href="https://erp.dqq.me"><b>在线演示</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/"><b>官网</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/manual/guide/"><b>用户手册</b></a> ·
  <a href="https://makesomefakenews.github.io/lemon-erp-website/dev/start/"><b>开发文档</b></a> ·
  <a href="#联系方式"><b>联系我们</b></a> ·
  <a href="#english"><b>English</b></a>
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.jpg" alt="柠檬ERP 工作台" />
</p>

## 项目简介

柠檬ERP（Lemon ERP）是一套面向中小制造、商贸企业的**真正 100% 开源、无功能限制的全功能 ERP 系统**，前后端分离，开箱即用；既可以直接作为企业的 ERP 软件使用，也适合作为 Java 企业级项目二次开发的基础。

- **业务全覆盖**：13 大模块、150+ 功能页面，从请购到收款、从 BOM 到完工入库、从凭证到报表，一套系统闭环
- **单据自动联动**：出入库自动更新库存、生成应收应付，业务单据一键生成会计凭证，账实一致
- **多端使用**：电脑浏览器、微信小程序、手机 / PDA 扫码作业、数据大屏投屏，支持中英文界面
- **技术栈新**：Spring Boot 4 + Vue 3 + Ant Design Vue 4，Gradle 多模块、单向依赖，代码结构清晰
- **真正 100% 开源**：全部功能、全部源码开放，没有商业版、没有隐藏模块、没有授权码，个人与企业均可免费商用

> 如果这个项目对你有帮助，请点一下右上角的 **Star**，这是对我们最大的鼓励！

## 在线演示

演示地址：**https://erp.dqq.me**（演示环境只能查看，不能新增、修改、删除数据）

| 租户 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 柠檬科技 | admin | admin123 | 超级管理员，可以浏览全部功能 |
| 柠檬科技 | test | admin123 | 普通用户（只读），数据权限为「本部门及以下」 |

## 功能一览

<table>
  <tr><th width="120">模块</th><th>主要功能</th></tr>
  <tr><td><b>供应链</b></td><td>请购、询价比价、招标、采购合同、采购订单、送货通知、采购入库 / 退货；销售报价、价目表与商品多价格、客户价格等级、促销、返利、销售提成、销售订单、发货拣货、销售出库 / 退货、物流发运；入库出库可直接开单；供应商 / 经销商门户</td></tr>
  <tr><td><b>进销存报表</b></td><td>销售报表、进货报表（多维汇总，汇总 → 明细 → 单据穿透）、经营状况、资产状况、商品分析（畅销、滞销、积压、缺货、周转）、客户 RFM 分群</td></tr>
  <tr><td><b>仓库</b></td><td>入库、出库、调拨、盘点、组装拆卸；批次与序列号追踪、库存预留与冻结、收发存汇总；多规格物料（SKU）、库位与上架策略；手机 / PDA 扫码作业</td></tr>
  <tr><td><b>生产制造</b></td><td>主生产计划、MRP 运算、产能排程、生产看板；工单、齐套分析、领料超领控制、补料、报工、完工入库；BOM 与工艺路线、工程变更、设备维护与 OEE、模具工装</td></tr>
  <tr><td><b>质量</b></td><td>检验方案（AQL 抽样）、来料 / 过程 / 成品 / 首件检验；不合格品 MRB、纠正预防（8D）、供应商索赔；合格率趋势、不良柏拉图、SPC 与 Cp / Cpk</td></tr>
  <tr><td><b>财务</b></td><td>凭证、账簿、资产负债表 / 利润表 / 现金流量表（小企业会计准则）、期末结账；应收应付与核销、账龄、数电票、往来对账；出纳、费用报销、票据、资金计划；固定资产、成本核算、标准成本、预算</td></tr>
  <tr><td><b>客户关系</b></td><td>客户公海、线索、商机看板、跟进记录、合同、服务工单、市场活动、客户分析</td></tr>
  <tr><td><b>项目管理</b></td><td>WBS 计划与甘特图、资源负荷、工时填报审批、成本控制、项目核算与挣值分析</td></tr>
  <tr><td><b>人力资源</b></td><td>员工档案与异动、劳动合同、招聘培训、考勤打卡、请假加班、绩效考核、薪酬核算（社保、个税、计件、提成）、员工自助</td></tr>
  <tr><td><b>商务智能</b></td><td>经营驾驶舱、多维分析、自定义报表与仪表盘、决策支持与预警、数据大屏（可投屏到电视）</td></tr>
  <tr><td><b>智能（AI）</b></td><td>接入 DeepSeek、通义千问、智谱 GLM、Kimi、豆包等大模型：AI 问数、智能录入（识别图片 / 文件生成单据）、数据整理、智能报告</td></tr>
  <tr><td><b>流程中心</b></td><td>仿钉钉流程设计器（条件分支、并行分支、会签 / 或签），业务单据一键接入审批</td></tr>
  <tr><td><b>系统平台</b></td><td>用户、部门、角色、菜单 / 按钮 / 数据权限；SaaS 多租户与行业模板；编码规则、打印模板（PDF、套打）、业务参数、自定义字段；钉钉 / 企业微信 / 飞书集成；中英文切换；系统监控与定时任务</td></tr>
</table>

每个功能的详细说明见 [用户手册](https://makesomefakenews.github.io/lemon-erp-website/manual/guide/)。

## 核心亮点

<table>
  <tr>
    <td width="33%" valign="top">
      <h4>可视化审批流</h4>
      仿钉钉流程设计器，审批人、抄送、条件分支、并行分支，会签 / 或签；采购、销售、请假、报销等单据一键接入审批
    </td>
    <td width="33%" valign="top">
      <h4>业财一体化</h4>
      出入库自动生成应收应付，业务单据一键生成凭证；三大报表、成本核算、预算执行分析开箱即用
    </td>
    <td width="33%" valign="top">
      <h4>MRP 与生产执行</h4>
      主生产计划驱动 MRP 多级展开，一键下达工单与采购；齐套分析、报工、生产看板全流程管控
    </td>
  </tr>
  <tr>
    <td valign="top">
      <h4>AI 智能助手</h4>
      用自然语言查询业务数据，上传报价单、发票自动生成单据，资料查重整理，自动撰写经营报告
    </td>
    <td valign="top">
      <h4>SaaS 多租户</h4>
      共享库按租户隔离，按行业模板开通功能，到期与账号数控制，新租户自动初始化基础数据
    </td>
    <td valign="top">
      <h4>精细权限</h4>
      菜单 / 按钮 / 接口三级权限；数据权限支持全部、自定义部门、本部门、本部门及以下、仅本人
    </td>
  </tr>
  <tr>
    <td valign="top">
      <h4>多端协同</h4>
      微信小程序处理审批和外勤，手机 / PDA 扫码收发货，数据大屏投屏车间，钉钉、企业微信、飞书消息推送
    </td>
    <td valign="top">
      <h4>单据打印</h4>
      后端生成 PDF，可视化配置模板或上传 JRXML，支持预印单据纸套打；单据编号规则可配置
    </td>
    <td valign="top">
      <h4>部署简单</h4>
      Docker 一键启动 MySQL、Redis、后端和前端；演示模式、中英文界面、深色模式开箱即用
    </td>
  </tr>
</table>

## 系统截图

**采购订单详情**

![采购订单详情](docs/screenshots/scm-purchase-detail.jpg)

**MRP 运算**

![MRP 运算](docs/screenshots/mes-mrp.jpg)

**财务报表（资产负债表）**

![资产负债表](docs/screenshots/fin-report.jpg)

**项目计划（甘特图）**

![计划进度（甘特图）](docs/screenshots/pm-gantt.jpg)

**经营驾驶舱**

![经营驾驶舱](docs/screenshots/bi-cockpit.jpg)

**流程设计器**

![流程设计器](docs/screenshots/wf-designer.jpg)

**数据大屏（生产车间）**

![生产车间](docs/screenshots/screen-factory.jpg)

**微信小程序**

![微信小程序](docs/screenshots/miniapp.jpg)

<details>
<summary><b>更多截图：登录与首页、供应链、仓库</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/login.jpg" alt="登录页" /><br />登录页</td>
    <td width="50%"><img src="docs/screenshots/dashboard-dark.jpg" alt="工作台（深色模式）" /><br />工作台（深色模式）</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/scm-purchase.jpg" alt="采购订单" /><br />采购订单</td>
    <td><img src="docs/screenshots/scm-sales-detail.jpg" alt="销售订单详情" /><br />销售订单详情</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/scm-supplier-eval.jpg" alt="供应商评估" /><br />供应商评估</td>
    <td><img src="docs/screenshots/wms-stock.jpg" alt="库存查询" /><br />库存查询</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wms-trace.jpg" alt="批次与序列号追踪" /><br />批次与序列号追踪</td>
    <td><img src="docs/screenshots/wms-material.jpg" alt="物料管理" /><br />物料管理</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wms-location.jpg" alt="库位管理" /><br />库位管理</td>
    <td><img src="docs/screenshots/mobile.jpg" alt="手机 / PDA 移动作业" /><br />手机 / PDA 移动作业</td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：生产、质量</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/mes-plan.jpg" alt="生产计划" /><br />生产计划</td>
    <td width="50%"><img src="docs/screenshots/mes-capacity.jpg" alt="产能排程" /><br />产能排程</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mes-kit.jpg" alt="齐套分析" /><br />齐套分析</td>
    <td><img src="docs/screenshots/mes-wo-detail.jpg" alt="生产工单详情" /><br />生产工单详情</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/mes-bom-detail.jpg" alt="物料清单" /><br />物料清单（BOM）</td>
    <td><img src="docs/screenshots/qms-inspection.jpg" alt="质量检验" /><br />质量检验</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/qms-ncr.jpg" alt="不合格品处理" /><br />不合格品处理</td>
    <td><img src="docs/screenshots/qms-spc.jpg" alt="SPC 控制图" /><br />SPC 控制图</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/qms-analysis.jpg" alt="质量分析" /><br />质量分析</td>
    <td><img src="docs/screenshots/screen-quality.jpg" alt="质量大屏" /><br />数据大屏（质量管理）</td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：财务</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/fin-voucher.jpg" alt="凭证管理" /><br />凭证管理</td>
    <td width="50%"><img src="docs/screenshots/fin-voucher-detail.jpg" alt="记账凭证" /><br />记账凭证</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/fin-book.jpg" alt="科目余额表" /><br />科目余额表</td>
    <td><img src="docs/screenshots/fin-ar.jpg" alt="应收管理" /><br />应收管理</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/fin-asset.jpg" alt="固定资产" /><br />固定资产</td>
    <td><img src="docs/screenshots/fin-cost.jpg" alt="成本核算" /><br />成本核算</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/fin-budget.jpg" alt="预算管理" /><br />预算管理</td>
    <td><img src="docs/screenshots/fin-analysis.jpg" alt="经营分析" /><br />经营分析</td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：客户关系、项目、人力资源</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/crm-opportunity.jpg" alt="销售机会看板" /><br />销售机会看板</td>
    <td width="50%"><img src="docs/screenshots/crm-analysis.jpg" alt="客户分析" /><br />客户分析</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/crm-customer.jpg" alt="客户管理" /><br />客户管理</td>
    <td><img src="docs/screenshots/crm-ticket.jpg" alt="服务工单" /><br />服务工单</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/pm-project.jpg" alt="项目列表" /><br />项目列表</td>
    <td><img src="docs/screenshots/pm-resource.jpg" alt="资源分配" /><br />资源分配</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/pm-accounting.jpg" alt="项目核算" /><br />项目核算</td>
    <td><img src="docs/screenshots/hr-employee.jpg" alt="员工档案" /><br />员工档案</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/hr-attendance.jpg" alt="考勤管理" /><br />考勤管理</td>
    <td><img src="docs/screenshots/hr-payroll.jpg" alt="薪酬核算" /><br />薪酬核算</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/hr-perf.jpg" alt="绩效考核" /><br />绩效考核</td>
    <td><img src="docs/screenshots/hr-recruit.jpg" alt="招聘管理" /><br />招聘管理</td>
  </tr>
</table>

</details>

<details>
<summary><b>更多截图：商务智能、数据大屏、流程与系统</b></summary>

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/bi-olap.jpg" alt="多维分析" /><br />多维分析</td>
    <td width="50%"><img src="docs/screenshots/bi-board.jpg" alt="自定义仪表盘" /><br />自定义仪表盘</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/screen-warehouse.jpg" alt="仓储物流大屏" /><br />数据大屏（仓储物流）</td>
    <td><img src="docs/screenshots/screen-sales.jpg" alt="销售经营大屏" /><br />数据大屏（销售经营）</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/wf-instance.jpg" alt="流程实例" /><br />流程实例</td>
    <td><img src="docs/screenshots/sys-print-template.jpg" alt="打印模板设计" /><br />打印模板设计</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/sys-user.jpg" alt="用户管理" /><br />用户管理</td>
    <td><img src="docs/screenshots/sys-role.jpg" alt="角色管理" /><br />角色管理</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/sys-dept.jpg" alt="部门管理" /><br />部门管理</td>
    <td><img src="docs/screenshots/monitor-server.jpg" alt="服务监控" /><br />服务监控</td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/monitor-cache.jpg" alt="缓存监控" /><br />缓存监控</td>
    <td><img src="docs/screenshots/monitor-job.jpg" alt="定时任务" /><br />定时任务</td>
  </tr>
</table>

</details>

## 系统架构

**业务架构**：蓝色虚线是模块内的业务流程，灰色虚线是跨模块的自动联动（生成下游单据、更新库存、生成应收应付与凭证）。

![柠檬ERP 业务架构图](docs/architecture/business.png)

**技术架构**

![柠檬ERP 技术架构图](docs/architecture/tech.png)

## 快速开始

### 方式一：Docker 一键部署（推荐）

安装了 Docker（含 Compose v2）时，一条命令启动 MySQL、Redis、后端和前端：

```bash
cd docker
cp .env.example .env        # 修改其中的密码和 JWT_SECRET
docker compose up -d --build
```

启动完成后访问 http://localhost（端口见 `.env` 中的 `WEB_PORT`）。构建、推送镜像使用 `./docker/build.sh`，详见 [Docker 部署](https://makesomefakenews.github.io/lemon-erp-website/dev/deploy/docker)。

### 方式二：源码运行

```bash
# 1. 初始化数据库（会删除并重建全部表）
mysql -uroot -p -e "CREATE DATABASE lemon_erp DEFAULT CHARSET utf8mb4"
mysql -uroot -p lemon_erp < sql/init.sql

# 2. 启动后端（项目根目录执行，默认连接本机 MySQL root / root 和本机 Redis，
#    可用环境变量 DB_URL、DB_USERNAME、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD 覆盖）
DB_PASSWORD=你的密码 ./gradlew bootRun

# 3. 启动前端，访问 http://localhost:5173（/api 自动代理到 http://localhost:8080）
cd lemon-web && npm install && npm run dev
```

<details>
<summary>打包部署与 Nginx 参考配置</summary>

```bash
./gradlew build                     # 生成 lemon-admin/build/libs/lemon-erp.jar
cd lemon-web && npm run build       # 生成 lemon-web/dist，交给 Nginx 托管

# 生产环境配置全部取环境变量，JWT_SECRET 至少 32 字节
java -jar lemon-erp.jar --spring.profiles.active=prod
```

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

使用 AI 问数时还需要为 `/api/ai/` 关闭代理缓冲，见 [Nginx 配置](https://makesomefakenews.github.io/lemon-erp-website/dev/deploy/nginx)。

</details>

**默认账号**：`admin` / `admin123`（超级管理员）、`test` / `admin123`（只读用户）。开启 SaaS 多租户（`LEMON_TENANT_ENABLED=true`）后登录需要填写租户「柠檬科技」；设置 `LEMON_DEMO_ENABLED=true` 开启演示模式（只能查看，不能修改数据）。

## 技术栈

| 分类 | 技术 | 环境要求 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 4.1、Spring Security + JWT、MyBatis-Plus 3.5、Flowable 8、JasperReports 7 | JDK 17 |
| 数据与缓存 | MySQL、Redis（可切换为内存缓存） | MySQL 8.0+、Redis 6.2+ |
| 前端 | Vue 3.5、Vite 7、Ant Design Vue 4.2、Pinia、Vue Router、ECharts 5、vue-i18n | Node 20+ |
| 小程序 | uni-app（Vue 3），微信小程序 | 微信开发者工具 |
| 构建部署 | Gradle 多模块、Docker / Docker Compose、Nginx | Docker 20.10+、Nginx 1.20+ |

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
│   ├── lemon-wms       仓库管理        ├── lemon-bi    商务智能、进销存报表
│   ├── lemon-scm       供应链管理      └── lemon-ai    智能（AI）
│   └── lemon-mes       生产制造 + 质量管理
├── lemon-admin         启动模块：组装全部业务模块，打包为可执行 jar
├── lemon-web           前端（Vite + Vue 3 + Ant Design Vue）
├── lemon-mp            微信小程序（uni-app + Vue 3）
├── sql/init.sql        建表与初始数据
├── docker              Dockerfile、docker-compose 与镜像构建脚本
└── docs                开发说明、架构图与截图
```

## 联系方式

商务合作、定制开发、部署实施或使用中遇到问题，欢迎联系：

- 电话：17701123202
- 微信：BG7LIF
- 邮箱：zmissu@163.com

也欢迎在 [Issues](https://github.com/MakeSomeFakeNews/lemon-erp/issues) 中反馈问题和建议，或提交 Pull Request 一起完善。

## 真正 100% 开源

柠檬ERP 不是「开源版 + 商业版」的模式，演示环境和文档中看到的全部功能，都在本仓库中完整开源：

- **全部功能开源**：供应链、仓储、生产、质量、财务、人力、客户、项目、商务智能、AI，没有任何模块收费或闭源
- **没有功能限制**：没有授权码和激活，系统本身不限制用户数、租户数和单据量，也没有隐藏的高级功能
- **免费商用**：遵守 [Apache-2.0](LICENSE) 协议，个人与企业都可以免费使用、修改、二次开发和商业部署，使用时保留原作者版权声明与许可证文件即可
- **文档同样开放**：200 多页 [用户手册](https://makesomefakenews.github.io/lemon-erp-website/manual/guide/) 和 [开发文档](https://makesomefakenews.github.io/lemon-erp-website/dev/start/)、Docker 一键部署脚本全部公开

如果觉得柠檬ERP 不错，请点一个 **Star**，也欢迎推荐给身边做生意、办工厂、做软件的朋友。

## English

**Lemon ERP** is a truly 100% open-source, full-featured **ERP system** with no paid edition and no feature restrictions, for small and medium-sized manufacturers and distributors, built with **Spring Boot 4 + Vue 3 + Ant Design Vue 4**. It covers purchasing, sales, inventory and warehouse management (**WMS**), manufacturing execution (**MES**: BOM, MRP, work orders, shop-floor reporting), quality management (**QMS**), accounting and finance, **CRM**, human resources (**HRM**, payroll, attendance), project management, business intelligence (**BI**) and **AI** assistants. Documents flow between modules automatically: stock movements update inventory and generate receivables/payables, and business documents generate accounting vouchers.

- Visual approval workflow (Flowable), SaaS multi-tenancy, menu / button / API permissions and department-level data permissions
- AI assistants with DeepSeek, Qwen, GLM, Kimi or Doubao: natural-language data queries, document recognition, data cleansing and business reports
- WeChat mini program, mobile / PDA barcode scanning, data screens, DingTalk / WeCom / Feishu login and notifications
- Chinese and English UI, demo mode, Docker one-click deployment
- Every feature and every line of source code is open: no paid edition, no hidden modules, no license keys; Apache-2.0 licensed, free for commercial use

Online demo: https://erp.dqq.me (tenant「柠檬科技」, `admin` / `admin123`) · Documentation: https://makesomefakenews.github.io/lemon-erp-website/ · Contact: zmissu@163.com

---

<p align="center">Apache License 2.0 © 2026 柠檬ERP</p>
