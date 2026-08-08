import http from '../../../api/http'
import type { ApiResponse } from '../../../types/api'
import type { CurrentUser } from '../../auth/types/auth'

export const getCurrentUser = () => http.get<ApiResponse<CurrentUser>>('/users/me')

export const updateNickname = (nickname: string) =>
  http.patch<ApiResponse<CurrentUser>>('/users/me/nickname', { nickname })
