/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

// Detect if we're building for Tauri (env var set in build:tauri script)
const isTauriBuild = process.env.TAURI_BUILD === 'true';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],

  // Prevent vite from obscuring Rust errors
  clearScreen: false,

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  // Vitest configuration
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    css: true,
  },

  build: {
    // Tauri uses a custom protocol for loading files, not relative paths
    assetsDir: '',

    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          charts: ['recharts'],
          ui: ['lucide-react', 'sonner', 'class-variance-authority', 'clsx', 'tailwind-merge'],
          forms: ['react-hook-form', '@hookform/resolvers', 'zod'],
          stomp: ['@stomp/stompjs', 'sockjs-client'],
          state: ['zustand'],
        },
      },
    },
    sourcemap: isTauriBuild, // Enable sourcemaps for Tauri debugging
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: !isTauriBuild,
        drop_debugger: !isTauriBuild,
      },
    },
  },

  server: {
    port: 3000,
    // Tauri dev server needs to be strict about the host
    strictPort: true,

    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },

  // Tauri-specific env vars are handled via the build script, not client envPrefix
  envPrefix: ['VITE_'],
});
