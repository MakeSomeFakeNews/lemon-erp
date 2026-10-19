<template>
  <view>
    <!-- 选择客户 -->
    <template v-if="picking">
      <view class="l-search">
        <input v-model="keyword" :placeholder="t('crm.followup.searchCustomer')" placeholder-class="l-placeholder" confirm-type="search" focus @confirm="customers.refresh()" />
      </view>
      <view v-for="c in customers.list" :key="c.customerId" class="l-item" @click="pick(c)">
        <view class="l-item__head">
          <text class="l-item__title">{{ c.name }}</text>
        </view>
        <view class="l-item__desc">{{ [c.code, c.contact, c.phone].filter(Boolean).join(' · ') }}</view>
      </view>
      <l-empty v-if="customers.loaded && !customers.list.length" />
      <l-load-more v-else-if="customers.list.length" :status="customers.status" />
    </template>

    <template v-else>
      <l-card>
        <l-field :label="t('crm.ticket.customer')" required>
          <view class="te__target" @click="startPick">
            <text v-if="customer.id">{{ customer.name }}</text>
            <text v-else class="l-placeholder">{{ t('crm.followup.selectCustomer') }}</text>
            <text class="te__link">{{ customer.id ? t('crm.followup.change') : t('common.pleaseSelect') }}</text>
          </view>
        </l-field>
        <l-field v-if="contacts.length" :label="t('crm.ticket.contact')">
          <picker :range="contacts" range-key="label" @change="(e) => (form.contactId = contacts[e.detail.value].id)">
            <view class="l-input" :class="{ 'l-placeholder': !form.contactId }">{{ contactLabel || t('common.pleaseSelect') }}</view>
          </picker>
        </l-field>
        <l-field :label="t('crm.ticket.type')" required>
          <picker :range="dict.crm_ticket_type" range-key="label" @change="(e) => (form.type = dict.crm_ticket_type[e.detail.value].value)">
            <view class="l-input" :class="{ 'l-placeholder': !form.type }">{{ form.type ? dictText(dict.crm_ticket_type, form.type) : t('common.pleaseSelect') }}</view>
          </picker>
        </l-field>
        <l-field :label="t('crm.ticket.priority')" required>
          <picker :range="dict.crm_ticket_priority" range-key="label" @change="(e) => (form.priority = dict.crm_ticket_priority[e.detail.value].value)">
            <view class="l-input">{{ dictText(dict.crm_ticket_priority, form.priority) }}</view>
          </picker>
        </l-field>
        <l-field :label="t('crm.ticket.titleLabel')" required>
          <input v-model="form.title" class="l-input" maxlength="128" :placeholder="t('crm.ticket.titlePh')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.ticket.description')">
          <textarea v-model="form.description" class="l-textarea" maxlength="2000" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </l-field>
        <l-field :label="t('crm.ticket.serialNo')">
          <view class="te__serial">
            <input v-model="form.serialNo" class="l-input" maxlength="64" :placeholder="t('crm.ticket.serialPh')" placeholder-class="l-placeholder" />
            <text class="te__link" @click="scanSerial">{{ t('common.scan') }}</text>
          </view>
        </l-field>
      </l-card>
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { TicketPriority } from '@web/constants/enums'
import { addTicket, listContacts, pageCustomers } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { scanCode } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'

const { t } = useI18n()
useTitle('crm.ticket.add')
const dict = useDict('crm_ticket_type', 'crm_ticket_priority')
const customer = reactive({ id: null, name: '' })
const contacts = ref([])
const picking = ref(false)
const keyword = ref('')
const busy = ref(false)
const form = reactive({ contactId: null, type: '', priority: TicketPriority.MEDIUM, title: '', description: '', serialNo: '' })
const customers = usePaged((p) => pageCustomers({ ...p, scope: 'team', keyword: keyword.value.trim() }))
const contactLabel = computed(() => contacts.value.find((c) => c.id === form.contactId)?.label || '')

onLoad((q) => {
  if (q.customerId) {
    selectCustomer(Number(q.customerId), decodeURIComponent(q.name || ''))
  } else {
    startPick()
  }
})
onReachBottom(() => picking.value && customers.more())

function startPick() {
  picking.value = true
  customers.refresh()
}

function pick(c) {
  selectCustomer(c.customerId, c.name)
  picking.value = false
}

/** 选择客户后加载联系人，默认首要联系人 */
async function selectCustomer(id, name) {
  Object.assign(customer, { id, name })
  form.contactId = null
  const list = (await listContacts(id).catch(() => [])) || []
  contacts.value = list.map((c) => ({ id: c.id, label: [c.name, c.phone].filter(Boolean).join(' '), primary: c.isPrimary === 1 }))
  form.contactId = contacts.value.find((c) => c.primary)?.id || null
}

async function scanSerial() {
  const code = await scanCode()
  if (code) form.serialNo = code
}

async function submit() {
  if (!customer.id) return toast(t('common.selectRequired', { name: t('crm.ticket.customer') }))
  if (!form.type) return toast(t('common.selectRequired', { name: t('crm.ticket.type') }))
  if (!form.title.trim()) return toast(t('common.required', { name: t('crm.ticket.titleLabel') }))
  busy.value = true
  let id
  try {
    id = await addTicket({
      customerId: customer.id,
      contactId: form.contactId,
      type: form.type,
      priority: form.priority,
      title: form.title.trim(),
      description: form.description.trim() || null,
      serialNo: form.serialNo.trim() || null
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('common.submitted'), icon: 'success' })
  setTimeout(() => uni.redirectTo({ url: `/pkg-crm/ticket/detail?id=${id}` }), 800)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.te {
  &__target {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 72rpx;
  }
  &__link {
    flex-shrink: 0;
    margin-left: 16rpx;
    font-size: 26rpx;
    color: $l-primary;
  }
  &__serial {
    display: flex;
    align-items: center;
  }
}
</style>
