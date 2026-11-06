# 数据库升级脚本

`sql/init.sql` 始终是最新版本的完整建表与初始数据脚本，**全新安装只执行 init.sql**，不需要执行这里的脚本。

已经在使用的数据库升级到新版本时，按文件名（日期）顺序执行**部署之后**新增的脚本。所有脚本都可以重复执行，已经执行过的部分会自动跳过，不确定是否执行过时直接再执行一次即可。

| 脚本 | 说明 |
| --- | --- |
| `20261106_v1.0.0.sql` | 开源首版（2026-10-16）升级到 v1.0.0。这段时间内任一版本的代码部署的数据库都可以直接执行 |

之后的变更按 `YYYYMMDD_名称.sql`（变更当天的日期）新增，并在上表中登记。

## 升级步骤

```bash
# 1. 备份数据库
mysqldump -uroot -p lemon_erp > lemon_erp_backup.sql

# 2. 按日期顺序执行新增的升级脚本
mysql -uroot -p lemon_erp < sql/upgrade/20261106_v1.0.0.sql

# Docker 部署时
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" lemon_erp' < ../sql/upgrade/20261106_v1.0.0.sql
```

3. 部署新版本的后端和前端并重启；
4. 在「系统监控 → 缓存监控」中清空「用户权限」「字典数据」「系统参数」缓存（或删除 Redis 中 `login:perms:*`、`dict:*` 开头的键和 `settings:all`），使新的菜单、权限、字典立即生效。

Flowable 的 `ACT_*` 表不需要处理，后端启动时自动升级。

## 编写约定

- 可重复执行：建表用 `CREATE TABLE IF NOT EXISTS`；加列、加索引前查询 `information_schema` 判断是否已存在；插入数据用 `INSERT ... SELECT ... WHERE NOT EXISTS`；数据换算要能判断是否已经换算过
- 不依赖自增 ID：按编码、路由地址、权限标识、角色编码关联
- 多租户：业务字典等租户数据写平台租户（`tenant_id` = 1），并给已有租户各复制一份
- 同时修改 `sql/init.sql`，保证「旧版本 init.sql + 升级脚本」与「新版本 init.sql」得到相同的表结构和初始数据
