import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Tomcat context path가 정해져 주소가 바뀌면 이 값만 고친다
const BACKEND_URL = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // /api 요청을 Vite가 Tomcat으로 대신 넘겨 브라우저가 같은 출처로 보게 한다 (CORS·세션 쿠키 문제 회피)
    proxy: {
      '/api': BACKEND_URL,
    },
  },
})
