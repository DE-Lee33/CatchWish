import client from './client.ts'

// 서버 common.auth.LoginMember record와 같은 모양. 비밀번호 해시는 서버가 애초에 보내지 않는다
export type LoginMember = {
  memberId: number
  loginId: string
  memberName: string
  role: string
  admin: boolean
}

export async function fetchMe(): Promise<LoginMember> {
  const response = await client.get<LoginMember>('/members/me')
  return response.data
}

export async function login(loginId: string, password: string): Promise<LoginMember> {
  const response = await client.post<LoginMember>('/auth/login', { loginId, password })
  return response.data
}
