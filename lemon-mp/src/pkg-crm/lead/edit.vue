<template>
  <view>
    <l-card>
      <l-field :label="t('crm.lead.company')" required>
        <input v-model="form.company" class="l-input" maxlength="128" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.contactName')">
        <input v-model="form.contactName" class="l-input" maxlength="64" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.position')">
        <input v-model="form.title" class="l-input" maxlength="64" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.phone')">
        <input v-model="form.phone" class="l-input" type="number" maxlength="32" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.email')">
        <input v-model="form.email" class="l-input" maxlength="128" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.source')">
        <picker :range="dict.crm_source" range-key="label" @change="(e) => (form.source = dict.crm_source[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.source }">{{ form.source ? dictText(dict.crm_source, form.source) : t('common.pleaseSelect') }}</view>
        </picker>
      </l-field>
      <l-field :label="t('crm.lead.industry')">
        <picker :range="dict.crm_industry" range-key="label" @change="(e) => (form.industry = dict.crm_industry[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.industry }">{{ form.industry ? dictText(dict.crm_industry, form.industry) : t('common.pleaseSelect') }}</view>
        </picker>
      </l-field>
      <l-field :label="t('crm.lead.region')">
        <input v-model="form.region" class="l-input" maxlength="64" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </l-field>
      <l-field :label="t('crm.lead.requirement')">
        <textarea v-model="form.requirement" class="l-textarea" maxlength="500" :placeholder="t('crm.lead.requirementPh')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.save') }}</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { addLead, getLead, updateLead } from '@/api/crm'
import { dictText, useDict } from '@/store/dict'

const { t } = useI18n()
const dict = useDict('crm_source', 'crm_industry')
const FIELDS = ['company', 'contactName', 'title', 'phone', 'email', 'industry', 'source', 'region', 'requirement']
const form = reactive(Object.fromEntries(FIELDS.map((f) => [f, ''])))
const id = ref(null)
const busy = ref(false)
/** 修改时保留负责人、市场活动（表单中不改） */
let keep = {}

onLoad(async (q) => {
  if (q.id) {
    id.value = Number(q.id)
    uni.setNavigationBarTitle({ title: t('crm.lead.editTitle') })
    const lead = await getLead(id.value)
    FIELDS.forEach((f) => (form[f] = lead[f] || ''))
    keep = { ownerId: lead.ownerId, campaignId: lead.campaignId }
  } else {
    uni.setNavigationBarTitle({ title: t('crm.lead.add') })
  }
})

async function submit() {
  if (!form.company.trim()) return uni.showToast({ title: t('common.required', { name: t('crm.lead.company') }), icon: 'none' })
  const data = { ...keep, ...Object.fromEntries(FIELDS.map((f) => [f, form[f].trim() || null])) }
  busy.value = true
  try {
    if (id.value) await updateLead(id.value, data)
    else await addLead(data)
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('common.saved'), icon: 'success' })
  setTimeout(() => uni.navigateBack(), 800)
}
</script>
