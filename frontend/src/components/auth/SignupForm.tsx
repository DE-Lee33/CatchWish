import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { toApiError } from '../../api/client.ts'
import { checkLoginId, signup } from '../../api/members.ts'

type Field = 'loginId' | 'memberName' | 'email' | 'password' | 'passwordConfirm'
type Values = Record<Field, string>
type FieldErrors = Partial<Record<Field, string>>

// 칸 5개가 모양이 같아 목록으로 그린다
const FIELDS: { name: Field; label: string; type: string; placeholder: string; autoComplete: string }[] = [
  { name: 'loginId', label: '아이디', type: 'text', placeholder: '영문·숫자 4~20자', autoComplete: 'username' },
  { name: 'memberName', label: '이름', type: 'text', placeholder: '이름을 입력하세요', autoComplete: 'name' },
  { name: 'email', label: '이메일', type: 'email', placeholder: '알림을 받을 이메일', autoComplete: 'email' },
  { name: 'password', label: '비밀번호', type: 'password', placeholder: '8자 이상', autoComplete: 'new-password' },
  { name: 'passwordConfirm', label: '비밀번호 확인', type: 'password', placeholder: '비밀번호를 다시 입력하세요', autoComplete: 'new-password' },
]

// 서버 MemberManager와 같은 규칙 (DECISIONS "입력 규칙"). 여기는 빨리 알려 주는 용도이고 최종 검사는 서버가 한다
const LOGIN_ID_PATTERN = /^[A-Za-z0-9]{4,20}$/
const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
const LOGIN_ID_FORMAT_ERROR = '아이디는 영문·숫자 4~20자로 입력해주세요.'

// 중복 오류는 폼 아래가 아니라 해당 칸 아래에 띄운다
const FIELD_ERRORS: Record<string, [Field, string]> = {
  DUPLICATE_LOGIN_ID: ['loginId', '이미 사용 중인 아이디입니다.'],
  DUPLICATE_EMAIL: ['email', '이미 사용 중인 이메일입니다.'],
}

function validate(values: Values): FieldErrors {
  const errors: FieldErrors = {}
  if (!LOGIN_ID_PATTERN.test(values.loginId)) errors.loginId = LOGIN_ID_FORMAT_ERROR
  if (!values.memberName.trim()) errors.memberName = '이름을 입력해주세요.'
  if (!EMAIL_PATTERN.test(values.email)) errors.email = '이메일 형식이 올바르지 않습니다.'
  if (values.password.length < 8) errors.password = '비밀번호는 8자 이상 입력해주세요.'
  if (values.password !== values.passwordConfirm) errors.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  return errors
}

function SignupForm() {
  const navigate = useNavigate()
  const [values, setValues] = useState<Values>({
    loginId: '',
    memberName: '',
    email: '',
    password: '',
    passwordConfirm: '',
  })
  const [errors, setErrors] = useState<FieldErrors>({})
  // 확인한 아이디를 저장해 두고 지금 입력값과 비교한다. 아이디를 고치면 자동으로 "확인 안 됨"이 된다
  const [checkedLoginId, setCheckedLoginId] = useState<string | null>(null)
  const loginIdChecked = checkedLoginId === values.loginId
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // 노션 MVC 표2 M1: 사용 가능 → 가입 버튼 활성화 / 사용 중 → "이미 사용 중인 아이디"
  async function handleCheckLoginId() {
    const { loginId } = values
    if (!LOGIN_ID_PATTERN.test(loginId)) {
      setErrors({ ...errors, loginId: LOGIN_ID_FORMAT_ERROR })
      return
    }
    try {
      const available = await checkLoginId(loginId)
      setCheckedLoginId(available ? loginId : null)
      setErrors({ ...errors, loginId: available ? undefined : FIELD_ERRORS.DUPLICATE_LOGIN_ID[1] })
    } catch (caught) {
      setErrors({ ...errors, loginId: toApiError(caught).message })
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const found = validate(values)
    setErrors(found)
    setFormError('')
    if (Object.keys(found).length > 0) return

    setSubmitting(true)
    try {
      const { loginId, password, memberName, email } = values
      await signup({ loginId, password, memberName, email })
      navigate('/login')
    } catch (caught) {
      // 실패해도 values는 그대로라 입력값이 유지된다
      const { code, message } = toApiError(caught)
      const fieldError = FIELD_ERRORS[code]
      // 확인 뒤 남이 먼저 가입한 경우라 다시 확인받는다
      if (code === 'DUPLICATE_LOGIN_ID') setCheckedLoginId(null)
      if (fieldError) setErrors({ [fieldError[0]]: fieldError[1] })
      else setFormError(message)
    } finally {
      setSubmitting(false)
    }
  }

  // 브라우저 기본 검사 말풍선 대신 칸 아래 문구로 통일하려고 noValidate
  return (
    <form onSubmit={handleSubmit} noValidate>
      {FIELDS.map((field) => (
        <div key={field.name}>
          <label>
            {field.label}
            <input
              type={field.type}
              value={values[field.name]}
              onChange={(event) => setValues({ ...values, [field.name]: event.target.value })}
              placeholder={field.placeholder}
              autoComplete={field.autoComplete}
              aria-invalid={Boolean(errors[field.name])}
            />
          </label>
          {field.name === 'loginId' && (
            <button type="button" onClick={handleCheckLoginId} disabled={loginIdChecked}>
              중복확인
            </button>
          )}
          {errors[field.name] && <p role="alert">{errors[field.name]}</p>}
          {field.name === 'loginId' && loginIdChecked && <p>사용 가능한 아이디입니다.</p>}
        </div>
      ))}
      {formError && <p role="alert">{formError}</p>}
      {!loginIdChecked && <p>아이디 중복확인 후 가입할 수 있습니다.</p>}
      <button type="submit" disabled={submitting || !loginIdChecked}>
        {submitting ? '가입 중...' : '가입하기'}
      </button>
    </form>
  )
}

export default SignupForm
