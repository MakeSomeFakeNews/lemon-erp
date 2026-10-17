<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-segmented v-model:value="tab" :options="[{ label: $t('hr.recruit.recruit'), value: 'recruit' }, { label: $t('hr.recruit.candidate'), value: 'candidate' }]" />
        <template v-if="tab === 'recruit'">
          <a-input v-model:value="rq.keyword" :placeholder="$t('hr.recruit.keywordPh')" allow-clear class="w200" @press-enter="rSearch" />
          <a-select v-model:value="rq.status" :placeholder="$t('hr.recruit.allStatus')" allow-clear class="w140" :options="toOptions(dict.hr_recruit_status)" @change="rSearch" />
          <a-button type="primary" @click="rSearch">{{ $t('common.action.query') }}</a-button>
        </template>
        <template v-else>
          <a-select
            v-model:value="cq.recruitId"
            :placeholder="$t('hr.recruit.allRecruits')"
            allow-clear
            show-search
            option-filter-prop="label"
            class="w240"
            :options="recruitOptions"
            @change="cSearch"
          />
          <a-input v-model:value="cq.keyword" :placeholder="$t('hr.recruit.candidateKeywordPh')" allow-clear class="w200" @press-enter="cSearch" />
          <a-button type="primary" @click="cSearch">{{ $t('common.action.query') }}</a-button>
        </template>
      </div>
      <div v-if="tab === 'candidate'" class="stages">
        <a-tag
          v-for="s in [{ value: undefined, label: $t('common.status.all') }, ...dict.hr_candidate_stage]"
          :key="s.value || 'all'"
          :color="cq.stage === s.value ? 'blue' : 'default'"
          class="stage-tag"
          @click="setStage(s.value)"
        >
          {{ s.label }}
        </a-tag>
      </div>
    </a-card>

    <!-- 招聘需求 -->
    <a-card v-show="tab === 'recruit'" :bordered="false" class="page-card table-card">
      <RecruitTab
        ref="recruitRef"
        :query="rq"
        @view-candidates="viewCandidates"
        @add-candidate="(recruitId) => candidateRef.openCreate({ recruitId })"
        @options-changed="loadRecruitOptions"
      />
    </a-card>

    <!-- 应聘者 -->
    <a-card v-show="tab === 'candidate'" :bordered="false" class="page-card table-card">
      <CandidateTab
        ref="candidateRef"
        :query="cq"
        :open-options="openOptions"
        @recruit-changed="rLoad"
        @options-changed="loadRecruitOptions"
      />
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { openRecruits, pageRecruit } from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import CandidateTab from './CandidateTab.vue'
import RecruitTab from './RecruitTab.vue'

const dict = useDict('hr_recruit_status', 'hr_candidate_stage', 'hr_candidate_source', 'hr_interview_result', 'hr_education', 'hr_emp_type')
const tab = ref('recruit')
const recruitRef = ref()
const candidateRef = ref()

// ---------------------------------------------------------------- 招聘需求
const rq = reactive({ keyword: undefined, status: undefined })
const rLoad = () => recruitRef.value.load()
const rSearch = () => recruitRef.value.search()
function viewCandidates(record) {
  cq.recruitId = record.id
  tab.value = 'candidate'
  cSearch()
}

// 招聘需求下拉：全部（筛选）与招聘中（录入简历）
const recruitOptions = ref([])
const openOptions = ref([])
async function loadRecruitOptions() {
  const [all, open] = await Promise.all([pageRecruit({ current: 1, size: 200 }), openRecruits()])
  recruitOptions.value = all.records.map((r) => ({ value: r.id, label: `${r.recruitNo} ${r.title}` }))
  openOptions.value = open.map((r) => ({ value: r.id, label: `${r.recruitNo} ${r.title}` }))
}

// ---------------------------------------------------------------- 应聘者
const cq = reactive({ recruitId: undefined, stage: undefined, keyword: undefined })
const cSearch = () => candidateRef.value.search()
function setStage(stage) {
  cq.stage = stage
  cSearch()
}

onMounted(() => {
  rLoad()
  candidateRef.value.load()
  loadRecruitOptions()
})
</script>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.stages {
  margin-top: 12px;
}
.stage-tag {
  cursor: pointer;
}
.w140 {
  width: 140px;
}
.w200 {
  width: 200px;
}
.w240 {
  width: 260px;
}
</style>
