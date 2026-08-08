import http from '../../../api/http'
import type { ApiResponse } from '../../../types/api'
import type { AuthResponse, LoginPayload, RegisterPayload } from '../types/auth'

export const register = (payload: RegisterPayload) =>
  http.post<ApiResponse<AuthResponse>>('/auth/register', payload)

export const login = (payload: LoginPayload) =>
  http.post<ApiResponse<AuthResponse>>('/auth/login', payload)
