import { onUnmounted, watch, type Ref } from 'vue'

export function useModalEffects(isOpen: Ref<boolean>, onClose: () => void) {
  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && isOpen.value) {
      onClose()
    }
  }

  function unlockScroll() {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = ''
    }
  }

  watch(
    isOpen,
    (open) => {
      if (typeof window === 'undefined' || typeof document === 'undefined') return

      if (open) {
        document.body.style.overflow = 'hidden'
        window.addEventListener('keydown', handleKeydown)
      } else {
        unlockScroll()
        window.removeEventListener('keydown', handleKeydown)
      }
    },
    { immediate: true },
  )

  onUnmounted(() => {
    unlockScroll()
    if (typeof window !== 'undefined') {
      window.removeEventListener('keydown', handleKeydown)
    }
  })
}
