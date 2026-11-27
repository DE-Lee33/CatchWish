import { createContext } from 'react'
import type { LoginMember } from '../../api/auth.ts'

// undefined = /me 응답 전(확인 중), null = 비로그인. 확인 중을 비로그인과 구분해야 로그인 버튼이 잠깐 보였다 사라지지 않는다
export type AuthState = LoginMember | null | undefined

// Provider 컴포넌트와 파일을 나눈 이유: 한 파일에서 컴포넌트와 다른 값을 같이 export하면 저장 시 화면 즉시 반영(HMR)이 깨진다
export const AuthContext = createContext<AuthState>(undefined)
