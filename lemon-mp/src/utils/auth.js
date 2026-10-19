const TOKEN_KEY = 'lemon_mp_token'

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || ''
export const setToken = (token) => uni.setStorageSync(TOKEN_KEY, token)
export const removeToken = () => uni.removeStorageSync(TOKEN_KEY)
