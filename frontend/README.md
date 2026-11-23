# frontend

CatchWish 화면. React + TypeScript + Vite.

## 실행

**필요:** Node.js 20.19 이상 또는 22.12 이상 (`node -v`로 확인)

```bash
cd frontend
npm install      # 처음 한 번, package.json이 바뀌었을 때
npm run dev      # http://localhost:5173
```

| 명령 | 하는 일 |
|---|---|
| `npm run dev` | 개발 서버 실행. 저장하면 화면에 바로 반영 |
| `npm run build` | 타입 검사 후 배포용 파일을 `dist/`에 생성. **PR 전에 통과 확인** |
| `npm run lint` | 코드 규칙 검사 (oxlint) |

> `npm run dev`는 타입 오류가 있어도 화면이 뜹니다. 타입 검사는 `npm run build`가 합니다.

PR을 올리면 GitHub Actions(`frontend-ci`)가 `npm ci → lint → build`를 자동으로 돌립니다. 실패하면 PR에 ❌가 뜨니 고친 뒤 다시 push하세요.

## 백엔드 연결

- 화면 코드는 API를 항상 `/api/...` 경로로 부릅니다 (`src/api/client.ts` 사용)
- 개발 중에는 Vite가 `/api` 요청을 Tomcat(`http://localhost:8080`)으로 넘깁니다. 브라우저 입장에선 같은 주소라 CORS 설정이 필요 없습니다
- Tomcat 주소가 바뀌면 `vite.config.ts`의 `BACKEND_URL`만 고칩니다
- 백엔드가 꺼져 있으면 터미널에 `http proxy error`가 찍힙니다 (정상)

## 폴더 구조

```
src/
├─ main.tsx           시작점: 라우터를 화면에 연결
├─ router.tsx         라우트표: 주소 → 페이지
├─ api/client.ts      공통 axios 인스턴스
├─ pages/             주소가 있는 화면. 페이지 1개 = 파일 1개
└─ components/        페이지 안에 들어가는 부품
   ├─ layout/         RootLayout (공통 틀, Header 자리)
   ├─ mypage/         마이페이지 탭 내용
   └─ PagePlaceholder.tsx   초기세팅용 빈 화면 (임시)
```

## 작업 규칙

- **라우트:** 주소는 노션 자료실 "프론트 라우트 명세"와 `router.tsx`가 1:1입니다. 주소를 추가·변경하면 **두 곳 모두** 고칩니다
- **내 페이지 구현:** `pages/내페이지.tsx`의 `PagePlaceholder` 사용을 지우고 화면을 만듭니다. 다른 사람 페이지 파일은 수정하지 않습니다
- **부품:** 노션 MVC 표의 Component 이름 그대로 `components/<영역>/`에 만듭니다 (예: `components/performance/PresaleCodeModal.tsx`). 파일명 = 컴포넌트명
- **API:** axios를 직접 만들지 말고 `src/api/client.ts`를 가져다 씁니다. 기능별 API 함수는 `src/api/`에 파일로 둡니다
- **용어:** 회원은 `member` (`user` 사용 X), "주최자" 대신 "공연 등록자"
