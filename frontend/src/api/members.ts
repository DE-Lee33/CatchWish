import client from './client.ts'

// 서버 model.dto.SignupRequest record와 같은 모양
export type SignupRequest = {
  loginId: string
  password: string
  memberName: string
  email: string
}

// 가입 후 자동 로그인은 하지 않는다. 화면이 /login으로 보낸다
export async function signup(request: SignupRequest): Promise<void> {
  await client.post('/members', request)
}

// 가입 전 안내용. true여도 가입 순간 남이 먼저 가져가면 signup이 DUPLICATE_LOGIN_ID로 실패할 수 있다
export async function checkLoginId(loginId: string): Promise<boolean> {
  const response = await client.get<{ available: boolean }>('/members/check-id', { params: { loginId } })
  return response.data.available
}
