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
