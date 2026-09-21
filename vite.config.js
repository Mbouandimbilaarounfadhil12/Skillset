import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Pin env file resolution to this directory regardless of the process's
  // working directory (e.g. `npm --prefix` launchers don't actually chdir).
  envDir: fileURLToPath(new URL('.', import.meta.url)),
})
