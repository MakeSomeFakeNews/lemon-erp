import request from '@/utils/request'

export const getProfile = () => request.get('/profile')
export const updateProfile = (data) => request.put('/profile', data)
export const updatePassword = (data) => request.put('/profile/password', data)
/** 头像上传地址（a-upload 直传） */
export const AVATAR_UPLOAD_URL = '/api/profile/avatar'
