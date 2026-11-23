<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { FileText, Languages, Menu, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'

const emit = defineEmits<{
  openCv: []
}>()

const { locale, toggleLocale, isVi } = useI18n()
const { portfolio } = usePortfolio()
const mobileOpen = ref(false)
const activeSection = ref('about')

const hasExperiences = computed(() => portfolio.value.experiences.length > 0)

const navLinks = computed(() => [
  { id: 'about', href: '#about', label: { vi: 'Giới thiệu', en: 'About' } },
  { id: 'skills', href: '#skills', label: { vi: 'Kỹ năng', en: 'Skills' } },
  {
    id: 'experience',
    href: '#experience',
    label: hasExperiences.value
      ? { vi: 'Kinh nghiệm', en: 'Experience' }
      : { vi: 'Học vấn', en: 'Education' },
  },
  { id: 'projects', href: '#projects', label: { vi: 'Dự án', en: 'Projects' } },
  { id: 'contact', href: '#contact', label: { vi: 'Liên hệ', en: 'Contact' } },
])

let observer: IntersectionObserver | null = null

onMounted(() => {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) return

  const sectionIds = ['about', 'skills', 'experience', 'projects', 'contact']
  const elements = sectionIds
    .map((id) => document.getElementById(id))
    .filter((el): el is HTMLElement => el !== null)

  observer = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)

      if (visible[0]?.target.id) {
        activeSection.value = visible[0].target.id
      }
    },
    {
      rootMargin: '-25% 0px -55% 0px',
      threshold: [0.1, 0.25, 0.5],
    },
  )

  elements.forEach((el) => observer?.observe(el))
})

onUnmounted(() => {
  observer?.disconnect()
})

function closeMobile() {
  mobileOpen.value = false
}

function handleNavClick(id: string) {
  activeSection.value = id
  closeMobile()
}

function handleOpenCv() {
  closeMobile()
  emit('openCv')
}
</script>

<template>
  <header class="no-print sticky top-0 z-40 border-b border-zinc-200 bg-white/95 backdrop-blur-sm">
    <div class="mx-auto flex h-14 max-w-4xl items-center justify-between px-4 sm:px-6">
      <a
        href="#about"
        class="font-mono text-sm font-medium tracking-tight text-zinc-900 transition-colors hover:text-emerald-700"
        @click="activeSection = 'about'"
      >
        quang<span class="text-emerald-700">.dev</span>
      </a>

      <nav class="hidden items-center gap-5 md:flex" aria-label="Primary">
        <a
          v-for="link in navLinks"
          :key="link.href"
          :href="link.href"
          class="text-xs font-medium transition-colors"
          :class="
            activeSection === link.id
              ? 'text-emerald-700 font-semibold'
              : 'text-zinc-600 hover:text-zinc-900'
          "
          @click="activeSection = link.id"
        >
          {{ link.label[locale] }}
        </a>
      </nav>

      <div class="flex items-center gap-2">
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded border border-zinc-200 bg-zinc-50 px-2.5 py-1 font-mono text-xs font-medium text-zinc-700 transition-colors hover:border-zinc-300 hover:bg-zinc-100"
          :aria-label="isVi ? 'Switch to English' : 'Chuyển sang tiếng Việt'"
          @click="toggleLocale"
        >
          <Languages class="h-3.5 w-3.5 text-zinc-500" />
          <span>{{ locale.toUpperCase() }}</span>
        </button>

        <button
          type="button"
          class="hidden items-center gap-1.5 rounded border border-zinc-900 bg-zinc-900 px-3 py-1 text-xs font-medium text-white transition-colors hover:bg-zinc-800 sm:inline-flex"
          @click="handleOpenCv"
        >
          <FileText class="h-3.5 w-3.5" />
          <span>{{ isVi ? 'Xem CV' : 'View CV' }}</span>
        </button>

        <button
          type="button"
          class="inline-flex h-8 w-8 items-center justify-center rounded border border-zinc-200 bg-white text-zinc-700 md:hidden"
          :aria-label="mobileOpen ? 'Close menu' : 'Open menu'"
          @click="mobileOpen = !mobileOpen"
        >
          <X v-if="mobileOpen" class="h-4 w-4" />
          <Menu v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <div
      v-if="mobileOpen"
      class="border-t border-zinc-200 bg-white px-4 py-3 md:hidden"
    >
      <nav class="flex flex-col gap-1">
        <a
          v-for="link in navLinks"
          :key="link.href"
          :href="link.href"
          class="rounded px-3 py-2 text-xs transition-colors"
          :class="
            activeSection === link.id
              ? 'bg-zinc-100 font-semibold text-zinc-900'
              : 'font-medium text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900'
          "
          @click="handleNavClick(link.id)"
        >
          {{ link.label[locale] }}
        </a>
        <button
          type="button"
          class="mt-2 inline-flex items-center justify-center gap-2 rounded bg-zinc-900 px-4 py-2 text-xs font-semibold text-white"
          @click="handleOpenCv"
        >
          <FileText class="h-3.5 w-3.5" />
          <span>{{ isVi ? 'Xem & tải CV' : 'View & Download CV' }}</span>
        </button>
      </nav>
    </div>
  </header>
</template>
