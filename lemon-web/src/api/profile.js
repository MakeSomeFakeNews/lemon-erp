import request from '@/utils/request'

export const getProfile = () => request.get('/profile')
export const updateProfile = (data) => request.put('/profile', data)
export const updatePassword = (data) => request.put('/profile/password', data)
/** 头像上传地址（a-upload 直传） */
export const AVATAR_UPLOAD_URL = '/api/profile/avatar'
/** 保存当前用户的界面设置（主题、布局等，见 utils/uiSettings.js）；读取随登录后的 /auth/info（uiSettings）返回 */
export const saveUiSettings = (data) => request.put('/profile/settings/ui', data)
