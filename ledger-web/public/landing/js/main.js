/* ============================================================
   Ledger · 介绍页主逻辑
   滚动 Header / Parallax / Count-up / Scroll Reveal / FAQ / 汉堡菜单
   ============================================================ */
(() => {
  'use strict'

  const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  /* ---------- 1. Header 滚动背景 + Hero Parallax ---------- */
  const header = document.getElementById('site-header')
  const heroVisual = document.querySelector('.parallax-float')

  function onScroll() {
    const y = window.scrollY || window.pageYOffset

    if (header) {
      header.classList.toggle('scrolled', y > 24)
    }

    // Parallax：仅 Hero 在视口附近时生效，位移上限 140px
    if (heroVisual && y < window.innerHeight * 1.1) {
      const pf = Math.min(y * 0.18, 140)
      heroVisual.style.setProperty('--pf', `${pf}px`)
    }
  }

  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()

  /* ---------- 2. Proof Ribbon 数字 Count-up ---------- */
  const nums = document.querySelectorAll('.ribbon-num[data-count]')

  function animateCount(el) {
    const target = Number(el.dataset.count) || 0
    const suffix = el.dataset.suffix || ''
    if (REDUCED_MOTION) {
      el.textContent = `${target}${suffix}`
      return
    }

    const duration = 1400
    const start = performance.now()

    function tick(now) {
      const p = Math.min((now - start) / duration, 1)
      // easeOutExpo：开始快、结尾缓
      const eased = p === 1 ? 1 : 1 - Math.pow(2, -10 * p)
      el.textContent = `${Math.round(target * eased)}${suffix}`
      if (p < 1) requestAnimationFrame(tick)
    }

    requestAnimationFrame(tick)
  }

  if ('IntersectionObserver' in window && nums.length) {
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            animateCount(entry.target)
            io.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.4 }
    )
    nums.forEach((el) => io.observe(el))
  } else {
    nums.forEach((el) => {
      el.textContent = `${el.dataset.count || 0}${el.dataset.suffix || ''}`
    })
  }

  /* ---------- 3. Scroll Reveal（板块 / 卡片进入视口淡入） ---------- */
  const revealEls = document.querySelectorAll('.reveal')

  if ('IntersectionObserver' in window && revealEls.length) {
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible')
            io.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    revealEls.forEach((el) => io.observe(el))
  } else {
    revealEls.forEach((el) => el.classList.add('visible'))
  }

  /* ---------- 4. FAQ 手风琴（同时只展开一条） ---------- */
  const faqItems = document.querySelectorAll('.faq-item')

  faqItems.forEach((item) => {
    const btn = item.querySelector('.faq-q')
    if (!btn) return

    btn.addEventListener('click', () => {
      const isOpen = item.classList.contains('open')

      faqItems.forEach((other) => {
        other.classList.remove('open')
        const q = other.querySelector('.faq-q')
        if (q) q.setAttribute('aria-expanded', 'false')
      })

      if (!isOpen) {
        item.classList.add('open')
        btn.setAttribute('aria-expanded', 'true')
      }
    })
  })

  /* ---------- 5. 移动端汉堡菜单 ---------- */
  const burger = document.getElementById('nav-burger')
  const navLinks = document.getElementById('nav-links')

  function closeMenu() {
    if (!burger || !navLinks) return
    burger.setAttribute('aria-expanded', 'false')
    navLinks.classList.remove('open')
  }

  if (burger && navLinks) {
    burger.addEventListener('click', () => {
      const isOpen = navLinks.classList.toggle('open')
      burger.setAttribute('aria-expanded', String(isOpen))
    })

    navLinks.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', closeMenu)
    })

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeMenu()
    })
  }
})()
