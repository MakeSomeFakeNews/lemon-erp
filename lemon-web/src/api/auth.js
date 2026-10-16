import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const getInfo = () => request.get('/auth/info')
export const getMenus = () => request.get('/auth/menus')
/** 公开配置：{ tenantEnabled, systemName, showDemoAccount, watermark, captchaMode, passwordMinLength, passwordComplexity, attachmentMaxMb } */
export const getAuthConfig = () => request.get('/auth/config')
/** 登录验证码：{ captchaId, image } */
export const getCaptcha = () => request.get('/auth/captcha')
