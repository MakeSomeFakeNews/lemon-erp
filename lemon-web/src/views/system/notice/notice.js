import dayjs from 'dayjs'

/** 已过期：有效期（含当天）早于今天，员工不再看到 */
export const expired = (notice) => !!notice?.expireDate && dayjs(notice.expireDate).isBefore(dayjs(), 'day')
