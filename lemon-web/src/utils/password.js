import { message } from 'ant-design-vue'
import { t } from '@/locales'
import { useConfigStore } from '@/store/config'

/** 去掉了易混淆字符（0/O、1/l/I）的随机密码字符集 */
const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
const SYMBOLS = '@#$%&*+-=?'

/** 密码复杂度，与后端 PasswordComplexity 一致 */
const COMPLEXITY = {
  LETTER_DIGIT: { text: () => t('components.password.letterDigit'), test: (v) => /[A-Za-z]/.test(v) && /\d/.test(v) },
  MIXED_CASE_DIGIT: { text: () => t('components.password.mixedCaseDigit'), test: (v) => /[A-Z]/.test(v) && /[a-z]/.test(v) && /\d/.test(v) },
  LETTER_DIGIT_SYMBOL: { text: () => t('components.password.letterDigitSymbol'), symbols: true, test: (v) => /[A-Za-z]/.test(v) && /\d/.test(v) && /[^A-Za-z\d]/.test(v) }
}

/** 当前的密码策略（参数设置）：{ minLength, text, symbols, test } */
function policy() {
  const config = useConfigStore()
  return { minLength: config.passwordMinLength, ...(COMPLEXITY[config.passwordComplexity] || COMPLEXITY.LETTER_DIGIT) }
}

/** 密码要求的提示文字，如「8-32 位，包含字母和数字」 */
export function passwordHint() {
  const p = policy()
  return t('components.password.hint', { min: p.minLength, rule: p.text() })
}

/** 新密码的表单校验规则（按参数设置中的密码策略） */
export function passwordRules(requiredMessage = t('login.inputPassword')) {
  return [
    { required: true, message: requiredMessage },
    {
      validator: (_, value) => {
        if (!value) return Promise.resolve()
        const p = policy()
        return value.length >= p.minLength && value.length <= 32 && p.test(value) ? Promise.resolve() : Promise.reject(new Error(passwordHint()))
      }
    }
  ]
}

/** 随机密码：长度不少于 12 位和最小长度，并满足密码复杂度 */
export function randomPassword() {
  const p = policy()
  const length = Math.min(Math.max(12, p.minLength), 32)
  const chars = p.symbols ? CHARS + SYMBOLS : CHARS
  for (;;) {
    const bytes = crypto.getRandomValues(new Uint32Array(length))
    const password = Array.from(bytes, (b) => chars[b % chars.length]).join('')
    if (p.test(password)) return password
  }
}

/** 生成随机密码并尝试复制到剪贴板，返回生成的密码 */
export async function generateAndCopyPassword() {
  const password = randomPassword()
  try {
    await navigator.clipboard.writeText(password)
    message.success(t('components.password.copied'))
  } catch {
    message.info(t('components.password.copyManually'))
  }
  return password
}
