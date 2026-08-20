/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url'
import { readFileSync, existsSync } from 'node:fs'
import { resolve, extname } from 'node:path'
import { defineConfig, type Plugin } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

/**
 * 介绍页中间件（grill-me 共识方案 A）
 *
 * 设计依据：前端页面设计方案 §12.6（/ → 静态 HTML，/app → Vue 应用）
 * - 拦截 GET /：有 refresh_token cookie → 302 跳 /app/dashboard（已登录直达工作台）；
 *   无 cookie → 返回 public/landing/index.html（介绍页）
 * - 拦截 /css/ /js/ /images/：从 public/landing/ 下读取（保持介绍页相对路径不变）
 * - 不拦截 /app/（base=/app/ 天然隔离）、/api/（proxy 处理）、/@vite/（HMR 内部路径）
 *
 * 生产环境由 Nginx 处理：location / { root .../landing; } location /app { alias .../dist; }
 */
function landingMiddleware(): Plugin {
  return {
    name: 'landing-middleware',
    configureServer(server) {
      const landingDir = resolve(server.config.publicDir, 'landing')
      const indexHtmlPath = resolve(landingDir, 'index.html')

      const mime: Record<string, string> = {
        '.css': 'text/css',
        '.js': 'application/javascript',
        '.html': 'text/html',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.webp': 'image/webp',
        '.svg': 'image/svg+xml',
        '.ico': 'image/x-icon',
      }

      server.middlewares.use((req, res, next) => {
        if (req.method !== 'GET') return next()
        const url = req.url || ''

        // 1. 根路径 → 介绍页（未登录）或跳工作台（已登录）
        if (url === '/' || url.startsWith('/?')) {
          // HttpOnly cookie 服务端可读，仅粗判登录态（精确校验交给 Vue 路由守卫）
          const hasRefresh = /refresh_token=[^;]+/.test(req.headers.cookie || '')
          if (hasRefresh) {
            res.statusCode = 302
            res.setHeader('Location', '/app/dashboard')
            res.end()
            return
          }
          if (existsSync(indexHtmlPath)) {
            res.setHeader('Content-Type', 'text/html; charset=utf-8')
            res.setHeader('Cache-Control', 'no-cache')
            res.end(readFileSync(indexHtmlPath, 'utf-8'))
            return
          }
          return next()
        }

        // 2. 介绍页相对路径资源 → 从 public/landing/ 下读取
        //    浏览器从 / 解析相对路径，变成 /css/style.css，需映射回 landing 目录
        if (['/css/', '/js/', '/images/'].some((p) => url.startsWith(p))) {
          const cleanPath = url.split('?')[0].slice(1) // 去掉前导 / 和 query
          const filePath = resolve(landingDir, cleanPath)
          if (existsSync(filePath)) {
            const ext = extname(filePath).toLowerCase()
            res.setHeader('Content-Type', mime[ext] || 'application/octet-stream')
            res.end(readFileSync(filePath))
            return
          }
        }

        // 其他请求交给 Vite 默认处理（/app/* SPA、/api/* proxy、/@vite/* HMR）
        next()
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  // Vue 应用挂载在 /app 下，/ 留给介绍页静态页（Nginx 路由见设计文档 §12.6）
  base: '/app/',
  plugins: [vue(), landingMiddleware()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 启动自动打开根路径，由中间件返回介绍页（grill-me 共识方案）
    open: '/',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/tests/**/*.spec.ts'],
  },
})
