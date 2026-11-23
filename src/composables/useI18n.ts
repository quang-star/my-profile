import { computed, ref, watchEffect } from 'vue'
import type { Locale, LocalizedList, LocalizedText } from '@/types/profile'

const STORAGE_KEY = 'my-profile-locale'

function detectInitialLocale(): Locale {
  if (typeof window === 'undefined') return 'vi'
  const saved = window.localStorage.getItem(STORAGE_KEY)
  if (saved === 'vi' || saved === 'en') return saved
  return navigator.language.toLowerCase().startsWith('en') ? 'en' : 'vi'
}

const locale = ref<Locale>(detectInitialLocale())

export function useI18n() {
  watchEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_KEY, locale.value)
      document.documentElement.lang = locale.value
    }
  })

  const isVi = computed(() => locale.value === 'vi')

  function setLocale(next: Locale) {
    locale.value = next
  }

  function toggleLocale() {
    locale.value = locale.value === 'vi' ? 'en' : 'vi'
  }

  function t(text: LocalizedText): string {
    return text[locale.value]
  }

  function tList(list: LocalizedList): string[] {
    return list[locale.value]
  }

  return {
    locale,
    isVi,
    setLocale,
    toggleLocale,
    t,
    tList,
  }
}
