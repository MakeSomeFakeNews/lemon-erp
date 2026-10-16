<template>
  <div ref="root" class="workbench">
    <!-- 欢迎条 -->
    <section class="page-card hero">
      <div class="hero-main">
        <UserAvatar :src="userStore.user?.avatar" :name="userStore.displayName" :size="52" />
        <div class="hero-text">
          <h2>{{ greeting() }}，{{ userStore.displayName }}</h2>
          <p>
            {{ today }}
            <template v-if="userStore.roles?.length">
              <a-divider type="vertical" />
              <a-tag v-for="r in userStore.roles" :key="r" :bordered="false" class="role-tag">{{ r }}</a-tag>
            </template>
          </p>
        </div>
      </div>
      <div class="hero-actions">
        <router-link v-for="a in quickActions" :key="a.path" :to="a.path" class="hero-action">
          <MenuIcon :name="a.icon" />
          <span>{{ a.title }}</span>
        </router-link>
      </div>
      <svg class="hero-deco" viewBox="0 0 200 200" aria-hidden="true">
        <circle cx="100" cy="100" r="96" fill="#ffd43b" />
        <circle cx="100" cy="100" r="84" fill="#fff9db" />
        <circle cx="100" cy="100" r="74" fill="#ffe066" />
        <g stroke="#fff9db" stroke-width="7" stroke-linecap="round">
          <path d="M100 26v148M26 100h148M47.7 47.7l104.6 104.6M152.3 47.7L47.7 152.3" />
        </g>
      </svg>
    </section>

    <!-- 经营数据：只显示有权限的项 -->
    <div v-if="cards.length" class="stats" :style="{ gridTemplateColumns: `repeat(${statColumns}, minmax(0, 1fr))` }">
      <router-link v-for="c in cards" :key="c.key" :to="c.link" class="page-card stat">
        <span class="stat-icon" :style="{ color: cardMeta[c.key].color, background: `${cardMeta[c.key].color}14` }">
          <MenuIcon :name="cardMeta[c.key].icon" />
        </span>
        <div class="stat-body">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value">
            <template v-if="c.kind === 'MONEY'"><span class="stat-unit">¥</span>{{ money(c.value) }}</template>
            <template v-else>{{ Number(c.value) }}<span class="stat-unit"> {{ cardMeta[c.key].unit }}</span></template>
          </div>
          <div v-if="c.previous != null" class="stat-sub">
            较上月同期
            <span :class="trend(c).cls">{{ trend(c).text }}</span>
          </div>
          <div v-else class="stat-sub">{{ cardMeta[c.key].sub }}</div>
        </div>
      </router-link>
    </div>

    <div class="grid">
      <div class="main-col">
        <!-- 功能导航 -->
        <a-card :bordered="false" class="page-card" title="功能导航">
          <div v-if="!modules.length" class="empty">暂无可访问的功能</div>
          <div class="modules">
            <div v-for="m in modules" :key="m.path" class="module">
              <div class="module-head">
                <span class="module-icon"><MenuIcon :name="m.icon || 'AppstoreOutlined'" /></span>
                <span class="module-title">{{ m.title }}</span>
              </div>
              <div class="module-links">
                <router-link v-for="p in m.pages" :key="p.path" :to="p.path" class="module-link">{{ p.title }}</router-link>
              </div>
            </div>
          </div>
        </a-card>
      </div>

      <div class="side-col">
        <!-- 待办事项 -->
        <a-card :bordered="false" class="page-card todo-card" title="待办事项">
          <template #extra>
            <a-button type="text" size="small" :loading="todoLoading" @click="loadTodos"><ReloadOutlined /></a-button>
          </template>
          <div class="card-scroll scroll-hover">
            <a-skeleton v-if="todoLoading && !todos.length" active :paragraph="{ rows: 3 }" />
            <div v-else-if="!todos.length" class="empty">暂无待办，一切顺利</div>
            <ul v-else class="list">
              <li v-for="t in todos" :key="t.module + t.title" class="list-item clickable" @click="go(t.link)">
                <span :class="['dot', t.level]" />
                <span class="list-title">{{ t.title }}</span>
                <span :class="['badge', t.level]">{{ t.count }}</span>
                <RightOutlined class="list-arrow" />
              </li>
            </ul>
          </div>
        </a-card>

        <!-- 审批 -->
        <a-card v-if="userStore.hasPerm('wf:task:todo')" :bordered="false" class="page-card task-card" title="待我审批">
          <template #extra><router-link to="/workflow/todo">全部</router-link></template>
          <div class="card-scroll scroll-hover">
            <a-skeleton v-if="taskLoading" active :paragraph="{ rows: 2 }" />
            <div v-else-if="!tasks.length" class="empty">没有待审批的事项</div>
            <ul v-else class="list">
              <li v-for="t in tasks" :key="t.taskId" class="list-item clickable" @click="go('/workflow/todo')">
                <div class="list-main">
                  <div class="list-title">{{ t.processName }}</div>
                  <div class="list-sub">{{ t.startUserName }} · {{ t.taskName }} · {{ shortTime(t.createTime) }}</div>
                </div>
                <RightOutlined class="list-arrow" />
              </li>
            </ul>
          </div>
        </a-card>

        <!-- 消息 -->
        <a-card :bordered="false" class="page-card message-card" title="消息通知">
          <template #extra><router-link to="/messages">全部</router-link></template>
          <div class="card-scroll scroll-hover">
            <a-skeleton v-if="msgLoading" active :paragraph="{ rows: 3 }" />
            <div v-else-if="!messages.length" class="empty">暂无消息</div>
            <ul v-else class="list">
              <li
                v-for="m in messages"
                :key="m.id"
                :class="['list-item', { clickable: !!m.link }]"
                @click="m.link && go(m.link)"
              >
                <span :class="['dot', m.readFlag === 1 ? 'read' : 'info']" />
                <div class="list-main">
                  <div class="list-title">{{ m.title }}</div>
                  <div class="list-sub">{{ shortTime(m.createTime) }}</div>
                </div>
              </li>
            </ul>
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined, RightOutlined } from '@ant-design/icons-vue'
import MenuIcon from '@/components/MenuIcon.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { getBusinessCards, getTodos } from '@/api/dashboard'
import { useElementWidth } from '@/composables/useElementWidth'
import { pageMessage } from '@/api/system/message'
import { pageTodoTask } from '@/api/workflow/task'
import { useUserStore } from '@/store/user'
import { greeting } from '@/utils/format'
import { flattenPages } from '@/utils/menu'
import { joinPath } from '@/utils/path'

/**
 * 首页工作台：按模块的功能导航、待办事项、待我审批、消息通知。
 * 经营数据、快捷入口、功能都只显示当前用户有权限的
 */
const router = useRouter()
const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const pages = computed(() => flattenPages(userStore.menus))
const pagePaths = computed(() => new Set(pages.value.map((p) => p.path)))
const canVisit = (path) => pagePaths.value.has(path)

const cardMeta = {
  sales: { icon: 'ShoppingOutlined', color: '#52c41a' },
  purchase: { icon: 'ShoppingCartOutlined', color: '#1677ff' },
  receivable: { icon: 'RiseOutlined', color: '#fa8c16', sub: '未核销的应收单' },
  payable: { icon: 'FallOutlined', color: '#722ed1', sub: '未核销的应付单' },
  toShip: { icon: 'SendOutlined', color: '#13c2c2', unit: '单', sub: '已审核、部分出库' },
  wip: { icon: 'ProfileOutlined', color: '#eb2f96', unit: '张', sub: '已下达、生产中' }
}
const cards = ref([])
// 经营数据卡片按内容区宽度均匀分行：每张至少 STAT_MIN_WIDTH，各行数量尽量相同（6 张为 6 / 3+3 / 2+2+2，不会出现 4+2）
const STAT_MIN_WIDTH = 150
const STAT_GAP = 16
const root = ref()
const width = useElementWidth(root)
const statColumns = computed(() => {
  const n = cards.value.length || 1
  const max = Math.max(1, Math.floor((width.value + STAT_GAP) / (STAT_MIN_WIDTH + STAT_GAP)))
  return Math.ceil(n / Math.ceil(n / max))
})
const money = (v) => Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
function trend(c) {
  const cur = Number(c.value || 0)
  const prev = Number(c.previous || 0)
  if (!prev) return { text: cur ? '新增' : '持平', cls: 'flat' }
  const pct = ((cur - prev) / prev) * 100
  return { text: `${pct >= 0 ? '↑' : '↓'} ${Math.abs(pct).toFixed(1)}%`, cls: pct >= 0 ? 'up' : 'down' }
}

/** 快捷操作：常用业务的列表页，按权限显示前 5 个 */
const quickActions = computed(() =>
  [
    { path: '/scm/sales', title: '销售订单', icon: 'ShoppingOutlined' },
    { path: '/scm/purchase', title: '采购订单', icon: 'ShoppingCartOutlined' },
    { path: '/mes/wo', title: '生产工单', icon: 'ProfileOutlined' },
    { path: '/wms/stock', title: '库存查询', icon: 'DatabaseOutlined' },
    { path: '/fin/voucher', title: '凭证', icon: 'FileTextOutlined' },
    { path: '/fin/claim', title: '费用报销', icon: 'AccountBookOutlined' },
    { path: '/hr/self', title: '员工自助', icon: 'SmileOutlined' }
  ].filter((a) => canVisit(a.path)).slice(0, 5)
)

/** 功能导航：一级菜单为模块，列出其下可访问的页面（不含首页、流程中心、系统管理和监控） */
const EXCLUDED = new Set(['/workflow', '/system', '/monitor'])
const modules = computed(() =>
  (userStore.menus || [])
    .filter((m) => m.type === 'M' && m.visible === 1 && !EXCLUDED.has(joinPath('', m.path)))
    .map((m) => ({
      path: joinPath('', m.path),
      title: m.name,
      icon: m.icon,
      pages: flattenPages(m.children || [], joinPath('', m.path))
    }))
    .filter((m) => m.pages.length)
)

const go = (link) => link && router.push(link).catch(() => {})
const shortTime = (t) => (t ? String(t).slice(5, 16) : '')

const todos = ref([])
const todoLoading = ref(false)
async function loadTodos() {
  todoLoading.value = true
  try {
    todos.value = await getTodos()
  } finally {
    todoLoading.value = false
  }
}

const tasks = ref([])
const taskLoading = ref(false)
const messages = ref([])
const msgLoading = ref(false)

onMounted(async () => {
  getBusinessCards().then((r) => (cards.value = r))
  loadTodos()
  if (userStore.hasPerm('wf:task:todo')) {
    taskLoading.value = true
    pageTodoTask({ current: 1, size: 5 })
      .then((r) => (tasks.value = r.records || []))
      .finally(() => (taskLoading.value = false))
  }
  msgLoading.value = true
  pageMessage({ current: 1, size: 6 })
    .then((r) => (messages.value = r.records || []))
    .finally(() => (msgLoading.value = false))
})
</script>

<style scoped>
.workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  /* 按内容区宽度（而不是窗口宽度）排版：侧边栏展开、收起时都能正确响应 */
  container-type: inline-size;
}
.workbench .page-card + .page-card {
  margin-top: 0;
}

/* 欢迎条 */
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(100deg, rgba(255, 212, 59, 0.16), rgba(255, 255, 255, 0) 55%), var(--app-card);
}
.hero-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.hero-text h2 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text);
}
.hero-text p {
  margin: 0;
  color: var(--app-text-3);
}
.role-tag {
  color: #ad6800;
  background: rgba(255, 212, 59, 0.22);
}
.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hero-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  color: var(--app-text-2);
  background: var(--app-card);
  border: 1px solid var(--app-border);
  transition: all 0.2s;
}
.hero-action:hover {
  color: var(--app-primary);
  border-color: var(--app-primary);
}
.hero-deco {
  position: absolute;
  width: 150px;
  right: 34%;
  top: -70px;
  opacity: 0.18;
  transform: rotate(16deg);
  pointer-events: none;
}

/* 经营数据卡片 */
.stats {
  display: grid;
  gap: 16px;
}
.stat {
  position: relative;
  display: block;
  padding: 16px 18px;
  background: var(--app-card);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.stat-icon {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}
.stat-body {
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: var(--app-text-3);
  padding-right: 40px;
}
.stat-value {
  margin: 10px 0 4px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--app-text-3);
}
.stat-sub {
  font-size: 12px;
  color: var(--app-text-3);
  white-space: nowrap;
}
.stat-sub .up {
  color: #52c41a;
}
.stat-sub .down {
  color: var(--status-critical);
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: stretch;
}
.main-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
/*
 * 右侧栏高度跟随左侧：height: 0 使它不参与撑高网格行，min-height: 100% 再拉伸到左侧的高度。
 * 待办占用剩余高度，审批保持自身高度，消息在空间不够时才压缩；超出的在卡片内滚动，不把页面撑高
 */
.main-col {
  align-self: start;
}
.side-col {
  height: 0;
  min-height: 100%;
}
.side-col > .page-card {
  display: flex;
  flex-direction: column;
}
.side-col > .todo-card {
  flex: 1 1 0;
  min-height: 200px;
}
.side-col > .task-card {
  flex: none;
}
.side-col > .message-card {
  flex: 0 1 auto;
  min-height: 200px;
}
.side-col > .page-card :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  /* 内边距移到滚动区域内，滚动条贴着卡片右边缘 */
  padding: 0;
}
.card-scroll {
  flex: 1;
  min-height: 0;
  padding: 24px;
}
.card-hint {
  font-size: 12px;
  color: var(--app-text-3);
}

/* 功能导航 */
.modules {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px 28px;
}
.module-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.module-icon {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.module-title {
  font-weight: 600;
  color: var(--app-text);
}
.module-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 4px;
}
.module-link {
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--app-text-2);
  background: var(--app-hover);
  transition: all 0.2s;
}
.module-link:hover {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}

/* 右侧列表 */
.empty {
  color: var(--app-text-3);
  padding: 4px 0;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--app-border);
}
.list-item:last-child {
  border-bottom: none;
}
.list-item.clickable {
  cursor: pointer;
  border-radius: 6px;
}
.list-item.clickable:hover {
  background: var(--app-hover);
}
.list-main {
  flex: 1;
  min-width: 0;
}
.list-title {
  flex: 1;
  min-width: 0;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.list-sub {
  font-size: 12px;
  color: var(--app-text-3);
  margin-top: 2px;
}
.list-arrow {
  color: var(--app-text-3);
  font-size: 11px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--app-primary);
}
.dot.danger {
  background: var(--status-critical);
}
.dot.warning {
  background: var(--status-warning);
}
.dot.read {
  background: var(--app-border);
}
.badge {
  min-width: 22px;
  padding: 0 7px;
  border-radius: 11px;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.badge.danger {
  color: var(--status-critical);
  background: rgba(208, 59, 59, 0.1);
}
.badge.warning {
  color: #ad6800;
  background: rgba(250, 178, 25, 0.16);
}

/* 内容区较窄：右侧栏移到下方，三张卡片并排或换行 */
@container (max-width: 1000px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .side-col {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    align-items: start;
    height: auto;
    min-height: 0;
  }
  .side-col > .page-card {
    min-height: 0;
    flex: none;
  }
  .card-scroll {
    overflow-y: visible;
  }
}
@container (max-width: 576px) {
  .hero {
    padding: 16px;
  }
  .hero-main {
    align-items: flex-start;
  }
  .hero-text h2 {
    font-size: 18px;
  }
  .hero-actions {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  }
  .hero-action {
    justify-content: center;
  }
  .hero-deco {
    display: none;
  }
  .stat {
    padding: 14px;
  }
  .stat-value {
    font-size: 18px;
  }
  .side-col {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

<style>
/* 暗色：欢迎条的柠檬黄渐变调淡（需要匹配根元素的 data-theme，不能放在 scoped 中） */
html[data-theme='dark'] .workbench .hero {
  background: linear-gradient(100deg, rgba(255, 212, 59, 0.07), rgba(0, 0, 0, 0) 55%), var(--app-card);
}
html[data-theme='dark'] .workbench .hero-deco {
  opacity: 0.08;
}
html[data-theme='dark'] .workbench .role-tag {
  color: #ffd666;
  background: rgba(255, 212, 59, 0.1);
}
</style>
