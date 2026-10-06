import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// In development the API, WebSocket and uploaded files are proxied to the Spring Boot backend,
// so the browser only ever talks to the Vite origin.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080'
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      proxy: {
        '/api': { target: backend, changeOrigin: false },
        '/uploads': { target: backend, changeOrigin: false },
        '/ws': { target: backend.replace(/^http/, 'ws'), ws: true, changeOrigin: false },
      },
    },
  }
})
