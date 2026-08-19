/* ============================================================
   Ledger · 功能轮播
   左右箭头滚动 + 边界状态更新（横向 / 移动端纵向自适应）
   ============================================================ */
(() => {
  'use strict'

  const track = document.getElementById('carousel-track')
  const prev = document.getElementById('carousel-prev')
  const next = document.getElementById('carousel-next')
  if (!track || !prev || !next) return

  const isVertical = () => track.scrollHeight > track.clientHeight + 8

  function scrollAmount() {
    // 步进为"一张卡片 + 间距"，横向取第一张卡片宽度
    const firstCard = track.querySelector('.feature-card')
    if (!firstCard) return 300
    return firstCard.offsetWidth + 24
  }

  function step(direction) {
    const amount = scrollAmount()
    const isVert = isVertical()
    const delta = direction === 'next' ? amount : -amount

    track.scrollBy({
      left: isVert ? 0 : delta,
      top: isVert ? delta : 0,
      behavior: 'smooth',
    })
  }

  function updateArrows() {
    const isVert = isVertical()
    const max = isVert ? track.scrollHeight - track.clientHeight : track.scrollWidth - track.clientWidth
    const cur = isVert ? track.scrollTop : track.scrollLeft
    const tolerance = 4

    prev.disabled = cur <= tolerance
    next.disabled = max - cur <= tolerance
  }

  prev.addEventListener('click', () => step('prev'))
  next.addEventListener('click', () => step('next'))

  let scrollRaf = null
  track.addEventListener(
    'scroll',
    () => {
      if (scrollRaf) return
      scrollRaf = requestAnimationFrame(() => {
        updateArrows()
        scrollRaf = null
      })
    },
    { passive: true }
  )

  window.addEventListener('resize', updateArrows, { passive: true })
  updateArrows()
})()
