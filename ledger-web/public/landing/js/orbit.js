/* ============================================================
   Ledger · 技术栈环绕动画（辅助逻辑）
   核心旋转由 CSS @keyframes orbit-spin 驱动；本文件负责：
   页面隐藏时暂停、窄屏调速、触屏 hover 替代
   ============================================================ */
(() => {
  'use strict'

  const container = document.getElementById('orbit')
  if (!container) return

  const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const SLOW = '42s' // 桌面 / 平板
  const FAST = '28s' // 手机

  function applySpeed() {
    const fast = window.matchMedia('(max-width: 768px)').matches
    container.style.setProperty('--orbit-duration', fast ? FAST : SLOW)
  }

  /* 标签页隐藏时暂停，节省资源 */
  document.addEventListener('visibilitychange', () => {
    container.classList.toggle('paused', document.hidden)
  })

  /* 触屏：按住暂停，方便查看图标 */
  if (window.matchMedia('(hover: none)').matches) {
    container.addEventListener('touchstart', () => container.classList.add('paused'), {
      passive: true,
    })
    container.addEventListener('touchend', () => container.classList.remove('paused'))
    container.addEventListener('touchcancel', () => container.classList.remove('paused'))
  }

  if (REDUCED_MOTION) {
    container.classList.add('paused')
  } else {
    applySpeed()
    window.addEventListener('resize', applySpeed, { passive: true })
  }
})()
