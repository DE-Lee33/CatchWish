import { Link } from 'react-router'
import SignupForm from '../components/auth/SignupForm.tsx'

function SignupPage() {
  return (
    <section>
      <h1>회원가입</h1>
      <p>가입한 회원은 모두 일반회원이며, 모든 회원은 공연 등록이 가능합니다.</p>
      <SignupForm />
      <Link to="/login">로그인하기</Link>
    </section>
  )
}

export default SignupPage
