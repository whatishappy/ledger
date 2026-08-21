/* ============================================
   Ledger Landing Page · Vanilla JS Interactions
   ============================================ */

(function () {
  'use strict';

  /* ---------- 1. IntersectionObserver · 滚动淡入 ---------- */
  function initRevealOnScroll() {
    var reveals = document.querySelectorAll('.reveal');
    if (!reveals.length) return;

    if (!('IntersectionObserver' in window)) {
      reveals.forEach(function (el) { el.classList.add('fadeIn'); });
      return;
    }

    var observer = new IntersectionObserver(
      function (entries, obs) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add('fadeIn');
            obs.unobserve(entry.target);
          }
        });
      },
      {
        root: null,
        rootMargin: '0px 0px -60px 0px',
        threshold: 0.2
      }
    );

    reveals.forEach(function (el) { observer.observe(el); });
  }

  /* ---------- 2. FAQ 手风琴 ---------- */
  function initAccordion() {
    var items = document.querySelectorAll('.accordion-item');
    if (!items.length) return;

    items.forEach(function (item) {
      var btn    = item.querySelector('.accordion-btn');
      var panel  = item.querySelector('.accordion-panel');
      if (!btn || !panel) return;

      btn.addEventListener('click', function () {
        var isActive = item.classList.contains('active');

        items.forEach(function (other) {
          if (other === item) return;
          other.classList.remove('active');
          var op = other.querySelector('.accordion-panel');
          if (op) op.style.maxHeight = null;
        });

        if (isActive) {
          item.classList.remove('active');
          panel.style.maxHeight = null;
        } else {
          item.classList.add('active');
          panel.style.maxHeight = panel.scrollHeight + 'px';
        }
      });
    });
  }

  /* ---------- 3. 平滑滚动 · 锚点链接 ---------- */
  function initSmoothScroll() {
    var anchors = document.querySelectorAll('a[href^="#"]');
    anchors.forEach(function (a) {
      a.addEventListener('click', function (e) {
        var href = a.getAttribute('href');
        if (!href || href === '#') return;
        var target = document.querySelector(href);
        if (!target) return;
        e.preventDefault();

        var headerOffset = document.getElementById('header')
          ? document.getElementById('header').offsetHeight
          : 80;
        var top = target.getBoundingClientRect().top + window.pageYOffset - headerOffset - 12;

        if ('scrollBehavior' in document.documentElement.style) {
          window.scrollTo({ top: top, behavior: 'smooth' });
        } else {
          smoothScrollTo(top, 600);
        }
      });
    });
  }

  function smoothScrollTo(targetTop, duration) {
    var startTop = window.pageYOffset;
    var diff = targetTop - startTop;
    var start;
    function step(timestamp) {
      if (!start) start = timestamp;
      var progress = Math.min((timestamp - start) / duration, 1);
      var eased = progress < 0.5
        ? 2 * progress * progress
        : 1 - Math.pow(-2 * progress + 2, 2) / 2;
      window.scrollTo(0, startTop + diff * eased);
      if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  /* ---------- 4. Header 滚动阴影 ---------- */
  function initHeaderScroll() {
    var header = document.getElementById('header');
    if (!header) return;
    var onScroll = function () {
      if (window.scrollY > 12) header.classList.add('scrolled');
      else header.classList.remove('scrolled');
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  /* ---------- 5. 移动端汉堡菜单 ---------- */
  function initMobileMenu() {
    var btn  = document.getElementById('menuBtn');
    var menu = document.getElementById('mobileMenu');
    if (!btn || !menu) return;

    btn.addEventListener('click', function () {
      var hidden = menu.classList.contains('hidden');
      if (hidden) {
        menu.classList.remove('hidden');
        requestAnimationFrame(function () {
          menu.style.opacity = '1';
          menu.style.transform = 'translateY(0)';
        });
      } else {
        menu.classList.add('hidden');
      }
    });

    menu.querySelectorAll('a').forEach(function (link) {
      link.addEventListener('click', function () { menu.classList.add('hidden'); });
    });
  }

  /* ---------- 6. FAQ 手风琴首项默认展开 ---------- */
  function openFirstAccordion() {
    var first = document.querySelector('.accordion-item');
    if (!first) return;
    var panel = first.querySelector('.accordion-panel');
    first.classList.add('active');
    if (panel) panel.style.maxHeight = panel.scrollHeight + 'px';
  }

  /* ---------- 启动 ---------- */
  function boot() {
    initHeaderScroll();
    initMobileMenu();
    initSmoothScroll();
    initAccordion();
    initRevealOnScroll();
    if (document.readyState === 'complete') {
      setTimeout(openFirstAccordion, 120);
    } else {
      window.addEventListener('load', function () { setTimeout(openFirstAccordion, 120); });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
