import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    // Firebase Hosting deploy root (configured in /firebase.json as "public": "hosting/public")
    outDir: path.resolve(__dirname, '../hosting/public'),
    // REQUIRED: outDir is outside project root, so Vite disables auto-empty; force it on
    emptyOutDir: true,
    sourcemap: true,
  },
  server: {
    port: 5173,
    strictPort: false,
  },
})
