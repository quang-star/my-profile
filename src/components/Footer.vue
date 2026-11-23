<script setup lang="ts">
import { ArrowUp, Lock } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi } = useI18n()
const { portfolio, isAuthenticated, loginModalOpen, editMode } = usePortfolio()
const year = new Date().getFullYear()

function handleAdminClick() {
  if (isAuthenticated.value) {
    editMode.value = !editMode.value
  } else {
    loginModalOpen.value = true
  }
}
</script>

<template>
  <footer class="no-print border-t border-zinc-200 py-6">
    <div class="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-4 px-4 text-xs text-zinc-500 sm:px-6">
      <p>
        © {{ year }} <span class="font-medium text-zinc-800">{{ portfolio.name }}</span>
        <span class="mx-1.5 text-zinc-300">·</span>
        <span class="font-mono text-zinc-400">Ha Noi, VN</span>
      </p>

      <div class="flex items-center gap-2">
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded border border-zinc-200 bg-white px-2.5 py-1 font-medium text-zinc-600 transition-colors hover:border-zinc-300 hover:text-zinc-900"
          :title="isVi ? 'Quản trị nội dung' : 'Admin edit mode'"
          @click="handleAdminClick"
        >
          <Lock class="h-3 w-3" />
          <span>{{ isAuthenticated ? (isVi ? 'Quản trị' : 'Admin') : 'Admin' }}</span>
        </button>

        <a
          href="#about"
          class="inline-flex items-center gap-1.5 rounded border border-zinc-200 bg-white px-2.5 py-1 font-medium text-zinc-700 transition-colors hover:border-zinc-300 hover:text-zinc-900"
        >
          <span>{{ isVi ? 'Lên đầu trang' : 'Back to top' }}</span>
          <ArrowUp class="h-3 w-3" />
        </a>
      </div>
    </div>
  </footer>
</template>
