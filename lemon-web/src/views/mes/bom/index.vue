<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="产品" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="物料编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'mes:bom:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增 BOM
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <a @click="openDetail(record)">{{ record.materialCode }} {{ record.materialName }}</a>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'version'">
            {{ record.version }}
            <a-tag v-if="record.isDefault === 1" color="gold" :bordered="false">默认</a-tag>
            <a-tooltip v-if="pending(record)" :title="`工程变更 ${record.ecnNo} 生效后启用`">
              <a-tag color="orange" :bordered="false">变更中</a-tag>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'leadDays'">{{ record.leadDays }} 天</template>
          <template v-else-if="column.key === 'lines'">{{ record.itemCount }} 个子件 · {{ record.opCount }} 道工序</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="`${record.materialName} ${record.version}`"
              :api="(s) => changeBomStatus(record.id, s)"
              :disabled="!userStore.hasPerm('mes:bom:edit') || pending(record)"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="editable(record)" v-perm="'mes:bom:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-button v-if="record.isDefault !== 1 && !pending(record)" v-perm="'mes:bom:edit'" type="link" size="small" @click="makeDefault(record)">
                设为默认
              </a-button>
              <a-button v-if="!pending(record)" v-perm="'mes:ecn:add'" type="link" size="small" @click="router.push({ path: '/mes/ecn', query: { bomId: record.id } })">
                发起变更
              </a-button>
              <span v-perm="'mes:bom:remove'">
                <a-popconfirm :title="`确定删除 BOM ${record.version}？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer page :drawer="drawer" create-title="新增 BOM" edit-title="编辑 BOM" width="min(1200px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-alert
          v-if="pending(form)"
          type="warning"
          show-icon
          class="block"
          :message="`该版本由工程变更 ${form.ecnNo} 创建：修改子件和工序后在工程变更中提交，生效后自动启用并接替默认`"
        />
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="产品" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :disabled="pending(form)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="版本号" name="version">
              <a-input v-model:value="form.version" :maxlength="16" placeholder="如 V1.0" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="生产提前期" name="leadDays">
              <a-input-number v-model:value="form.leadDays" :min="0" :max="365" addon-after="天" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="18">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col v-if="!pending(form)" :xs="24" :sm="6">
            <a-form-item label=" " name="isDefault">
              <a-checkbox v-model:checked="form.isDefault">设为默认 BOM</a-checkbox>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">子件（单位用量：生产 1 个产品的用量）</span>
          <a-button type="primary" ghost size="small" @click="form.items.push({ key: rowKey(), qty: 1, lossRate: 0 })">
            <template #icon><PlusOutlined /></template>添加子件
          </a-button>
        </div>
        <a-table row-key="key" size="small" class="items-table" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 1180 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择子件' }]" class="cell-item">
                <MaterialSelect
                  v-model:value="record.materialId"
                  :label="record.materialLabel"
                  @select="(m) => Object.assign(record, { materialLabel: m.label, spec: m.spec, unit: m.unit })"
                />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'spec'">
              <span class="cell-text">{{ record.spec || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: '请填写用量' }]" class="cell-item">
                <a-input-number v-model:value="record.qty" :min="0" :precision="6" :addon-after="record.unit || undefined" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'refDes'">
              <a-form-item
                :name="['items', index, 'refDes']"
                :rules="[{ validator: (_, v) => refDesValidator(record, v), trigger: 'blur' }]"
                class="cell-item"
              >
                <a-input v-model:value="record.refDes" :maxlength="1000" placeholder="如 R1,R2 或 C1-C8" />
                <template #extra>
                  <span :class="{ warn: refDesHint(record)?.warn }">{{ refDesHint(record)?.text }}</span>
                </template>
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'lossRate'">
              <a-input-number v-model:value="record.lossRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" placeholder="选填" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>

        <div class="items-head">
          <span class="section-title">工艺路线（工序按工序号执行，标准工时为每个产品的工时）</span>
          <a-button type="primary" ghost size="small" @click="addOp">
            <template #icon><PlusOutlined /></template>添加工序
          </a-button>
        </div>
        <a-table row-key="key" size="small" class="items-table" :columns="opColumns" :data-source="form.ops" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'seq'">
              <a-input-number v-model:value="record.seq" :min="1" class="full" />
            </template>
            <template v-else-if="column.key === 'name'">
              <a-form-item :name="['ops', index, 'name']" :rules="[{ required: true, message: '请填写工序名称' }]" class="cell-item">
                <a-input v-model:value="record.name" :maxlength="64" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'workCenterId'">
              <a-form-item :name="['ops', index, 'workCenterId']" :rules="[{ required: true, message: '请选择工作中心' }]" class="cell-item">
                <WorkCenterSelect v-model:value="record.workCenterId" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'stdHours'">
              <a-input-number v-model:value="record.stdHours" :min="0" :precision="4" addon-after="小时" class="full" />
            </template>
            <template v-else-if="column.key === 'pieceRate'">
              <a-input-number v-model:value="record.pieceRate" :min="0" :precision="4" addon-after="元" placeholder="不计件" class="full" />
            </template>
            <template v-else-if="column.key === 'firstArticle'">
              <a-checkbox v-model:checked="record.firstArticle">需首件检验</a-checkbox>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.ops.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <!-- 详情：BOM 树逐层展开 -->
    <SubPage
      :open="detailOpen"
      :title="detail ? `BOM ${detail.materialCode} ${detail.materialName} ${detail.version}` : 'BOM'"
      width="min(900px, 100vw)"
      @close="detailOpen = false"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="section-title">物料清单（点击展开自制子件的下层）</div>
          <a-table
            row-key="key"
            size="small"
            :columns="treeColumns"
            :data-source="tree"
            :pagination="false"
            @expand="onExpand"
            class="block"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                {{ record.materialCode }} {{ record.materialName }}
                <a-tag v-if="record.childBomId" color="cyan" :bordered="false">自制</a-tag>
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'refDes'">
                <a-tooltip v-if="record.refDes" :title="record.refDes">
                  <span class="ref-des">{{ record.refDes }}</span>
                </a-tooltip>
                <span v-else class="sub-text">-</span>
                <div v-if="record.refDes" class="sub-text">共 {{ refDesCount(record.refDes) }} 个</div>
              </template>
              <template v-else-if="column.key === 'lossRate'">{{ Number(record.lossRate) ? `${Number(record.lossRate)}%` : '-' }}</template>
              <template v-else-if="column.key === 'total'">{{ formatQty(record.total) }} {{ record.unit }}</template>
            </template>
          </a-table>
          <div class="section-title">工艺路线</div>
          <a-table row-key="id" size="small" :columns="opViewColumns" :data-source="detail.ops" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'stdHours'">{{ formatQty(record.stdHours) }} 小时</template>
              <template v-else-if="column.key === 'pieceRate'">{{ record.pieceRate != null ? `¥${formatQty(record.pieceRate)} / 件` : '-' }}</template>
              <template v-else-if="column.key === 'firstArticle'">
                <a-tag v-if="record.firstArticle === 1" color="purple" :bordered="false">首件检验</a-tag>
                <span v-else class="sub-text">-</span>
              </template>
            </template>
          </a-table>
          <template v-if="docs.length">
            <div class="section-title">技术文档（产品与子件的已发布文档）</div>
            <a-table row-key="id" size="small" :columns="docColumns" :data-source="docs" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
                <template v-else-if="column.key === 'doc'">
                  <a @click="router.push({ path: '/mes/doc', query: { id: record.id } })">{{ record.name }}</a>
                  <div class="sub-text">{{ record.docNo }} · {{ record.fileCount }} 个文件</div>
                </template>
                <template v-else-if="column.key === 'type'"><DictTag type="mes_doc_type" :value="record.docType" /></template>
              </template>
            </a-table>
          </template>
        </template>
      </a-spin>
    </SubPage>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addBom, changeBomStatus, deleteBom, getBom, pageBom, setDefaultBom, updateBom } from '@/api/mes/base'
import { releasedDocs } from '@/api/mes/plm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatQty, rowKey } from '@/components/wms/wms'

const userStore = useUserStore()
const dict = useDict('sys_common_status')

const columns = [
  { title: '产品', key: 'product', width: 260 },
  { title: '版本', key: 'version', width: 130 },
  { title: '提前期', key: 'leadDays', width: 90 },
  { title: '结构', key: 'lines', width: 170 },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-bom')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageBom, query)

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  search()
}

const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从工程变更「修改变更后的 BOM」进入
  if (route.query.edit) drawer.openEdit({ id: Number(route.query.edit) })
})

/** 工程变更创建、还没有生效的版本：只在变更草稿时可以修改 */
const pending = (bom) => !!bom?.ecnNo && bom.ecnStatus !== 'EFFECTIVE'
const editable = (bom) => !pending(bom) || bom.ecnStatus === 'DRAFT'

// ---------------------------------------------------------------- 表单
const itemColumns = [
  { title: '子件', key: 'material', width: 260 },
  { title: '规格', key: 'spec', width: 160 },
  { title: '单位用量', key: 'qty', width: 170 },
  { title: '损耗率', key: 'lossRate', width: 110 },
  { title: '位号', key: 'refDes', width: 220 },
  { title: '备注', key: 'remark', width: 160 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]
const opColumns = [
  { title: '工序号', key: 'seq', width: 100 },
  { title: '工序名称', key: 'name', width: 200 },
  { title: '工作中心', key: 'workCenterId', width: 220 },
  { title: '标准工时', key: 'stdHours', width: 170 },
  { title: '计件单价', key: 'pieceRate', width: 150 },
  { title: '首件检验', key: 'firstArticle', width: 130 },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  materialId: [{ required: true, message: '请选择产品' }],
  version: [{ required: true, whitespace: true, message: '请填写版本号' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, materialId: undefined, version: 'V1.0', leadDays: 0, isDefault: true, remark: '', items: [], ops: [] }),
  fetch: async (record) => {
    const bom = await getBom(record.id)
    return {
      ...bom,
      materialLabel: `${bom.materialCode} ${bom.materialName}`,
      isDefault: bom.isDefault === 1,
      items: bom.items.map((i) => ({
        ...i,
        key: rowKey(),
        qty: Number(i.qty),
        lossRate: Number(i.lossRate),
        materialLabel: `${i.materialCode} ${i.materialName}`
      })),
      ops: bom.ops.map((o) => ({ ...o, key: rowKey(), stdHours: Number(o.stdHours), pieceRate: o.pieceRate == null ? undefined : Number(o.pieceRate), firstArticle: o.firstArticle === 1 }))
    }
  },
  create: addBom,
  update: updateBom,
  toPayload: (f) => ({
    materialId: f.materialId,
    version: f.version,
    leadDays: f.leadDays ?? 0,
    isDefault: f.isDefault ? 1 : 0,
    remark: f.remark || null,
    items: f.items.map((i) => ({ materialId: i.materialId, qty: i.qty, lossRate: i.lossRate ?? 0, refDes: i.refDes || null, remark: i.remark || null })),
    ops: f.ops.map((o) => ({ seq: o.seq, name: o.name, workCenterId: o.workCenterId, stdHours: o.stdHours ?? 0, pieceRate: o.pieceRate ?? null,
      firstArticle: !!o.firstArticle }))
  }),
  onSuccess: load
})
const { formRef, form } = drawer

function addOp() {
  const next = form.value.ops.reduce((max, o) => Math.max(max, o.seq || 0), 0) + 10
  form.value.ops.push({ key: rowKey(), seq: next, name: '', workCenterId: undefined, stdHours: 0, firstArticle: false })
}

async function makeDefault(record) {
  await setDefaultBom(record.id)
  message.success('已设为默认 BOM')
  load()
}

async function remove(id) {
  await deleteBom(id)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 位号：R1,R2 或区间 C1-C8，与后端 RefDes 的规则一致
const REF_TOKEN = /^([A-Z]+)(\d+)(?:-([A-Z]*)(\d+))?$/

/** 展开后的位号列表；格式不正确时返回错误信息 */
function parseRefDes(text) {
  const list = []
  for (const raw of (text || '').trim().toUpperCase().split(/[,，;；、\s]+/)) {
    if (!raw) continue
    const t = raw.replace(/[－~～]/g, '-')
    const m = REF_TOKEN.exec(t)
    if (!m) return { error: `位号「${raw}」格式不正确，应为字母加数字，如 R1，连续的写成 R1-R8` }
    if (m[4] == null) {
      list.push(t)
      continue
    }
    if (m[3] && m[3] !== m[1]) return { error: `位号区间「${raw}」的前缀不一致` }
    const start = Number(m[2])
    const end = Number(m[4])
    if (end < start) return { error: `位号区间「${raw}」的结束编号小于起始编号` }
    if (end - start + 1 > 5000) return { error: `位号区间「${raw}」超过 5000 个` }
    for (let i = start; i <= end; i++) list.push(m[1] + i)
  }
  return { list }
}

const refDesCount = (text) => parseRefDes(text).list?.length ?? 0

function refDesValidator(record, value) {
  const { error, list } = parseRefDes(value)
  if (error) return Promise.reject(error)
  // 与其他子件的位号重复
  const others = new Set(
    form.value.items.filter((i) => i !== record).flatMap((i) => parseRefDes(i.refDes).list || [])
  )
  const dup = list.find((r) => others.has(r))
  return dup ? Promise.reject(`位号 ${dup} 与其他子件重复`) : Promise.resolve()
}

/** 位号数与整数用量不一致时提示（按盘、按 kg 计量的物料用量与位号数无关，不拦截） */
function refDesHint(record) {
  const { list } = parseRefDes(record.refDes)
  if (!list?.length) return null
  const qty = Number(record.qty)
  const mismatch = Number.isInteger(qty) && qty > 0 && qty !== list.length
  return { text: mismatch ? `共 ${list.length} 个，与用量 ${qty} 不一致` : `共 ${list.length} 个`, warn: mismatch }
}

// ---------------------------------------------------------------- 详情：BOM 树
const treeColumns = [
  { title: '物料', key: 'material', width: 280 },
  { title: '规格', dataIndex: 'spec', width: 160, ellipsis: true },
  { title: '单位用量', key: 'qty', width: 110, align: 'right' },
  { title: '损耗率', key: 'lossRate', width: 80, align: 'right' },
  { title: '累计用量（每个产品）', key: 'total', width: 160, align: 'right' },
  { title: '位号', key: 'refDes', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true }
]
const opViewColumns = [
  { title: '工序号', dataIndex: 'seq', width: 90 },
  { title: '工序名称', dataIndex: 'name', width: 180 },
  { title: '工作中心', dataIndex: 'workCenterName', width: 180 },
  { title: '标准工时', key: 'stdHours', width: 120 },
  { title: '计件单价', key: 'pieceRate', width: 120 },
  { title: '首件检验', key: 'firstArticle', width: 100 }
]
const docColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '文档', key: 'doc', width: 240 },
  { title: '类型', key: 'type', width: 110 },
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '发布时间', dataIndex: 'releaseTime', width: 160 }
]
const docs = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()
const tree = ref([])

/** 子件行：累计用量 = 上层累计用量 × 用量 × (1 + 损耗率)；自制件可展开 */
const toNodes = (items, parentTotal) =>
  items.map((i) => {
    const total = parentTotal * Number(i.qty) * (1 + Number(i.lossRate) / 100)
    return { ...i, key: rowKey(), total, children: i.childBomId ? [] : undefined }
  })

async function openDetail(record) {
  detailOpen.value = true
  detail.value = undefined
  detailLoading.value = true
  try {
    detail.value = await getBom(record.id)
    tree.value = toNodes(detail.value.items, 1)
    docs.value = userStore.hasPerm('mes:doc:list')
      ? await releasedDocs([detail.value.materialId, ...detail.value.items.map((i) => i.materialId)])
      : []
  } finally {
    detailLoading.value = false
  }
}

async function onExpand(expanded, record) {
  if (!expanded || !record.childBomId || record.children?.length) return
  const child = await getBom(record.childBomId)
  record.children = toNodes(child.items, record.total)
  tree.value = [...tree.value]
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 8px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.cell-text {
  display: inline-block;
  padding-top: 5px;
}
.warn {
  color: var(--status-warning);
}
.ref-des {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.block {
  margin-bottom: 24px;
}
</style>
