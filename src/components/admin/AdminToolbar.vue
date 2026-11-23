<script setup lang="ts">
import { Eye, Inbox, Layers, LogOut, Pencil } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi, locale, setLocale } = useI18n()
const { openEditorFor, activeTextId, visualSaving, visualError, canvasNodeId, resizingNodeId, draggedNodeId, inspectorDraft } = useNodes()
const {
  isAuthenticated,
  adminUsername,
  editMode,
  inboxModalOpen,
  unreadMessagesCount,
  loadMessages,
  logout,
} = usePortfolio()

async function openInbox() {
  inboxModalOpen.value = true
  await loadMessages()
}
</script>

<template>
  <div
    v-if="isAuthenticated"
    class="no-print fixed bottom-4 left-1/2 z-40 flex w-max max-w-[calc(100vw-2rem)] -translate-x-1/2 flex-wrap items-center gap-2 rounded-2xl border border-slate-700/90 bg-slate-900/95 px-3 py-2 text-xs text-white shadow-2xl backdrop-blur-md"
    data-editor-control
  >
    <p v-if="editMode && visualError" role="alert" class="w-full rounded-lg bg-rose-950 px-2 py-1.5 text-rose-100">
      {{ isVi ? 'Không thể lưu thay đổi.' : 'Could not save changes.' }} {{ visualError }}
    </p>
    <span class="hidden items-center gap-1.5 pl-1.5 font-mono text-[11px] text-slate-300 sm:inline-flex">
      <span class="h-2 w-2 rounded-full bg-emerald-400" />
      {{ adminUsername }}
    </span>

    <button
      type="button"
      class="inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 font-semibold transition-colors disabled:opacity-50"
      :disabled="!!activeTextId || !!resizingNodeId || !!draggedNodeId || !!inspectorDraft || visualSaving"
      :aria-pressed="editMode"
      :class="
        editMode
          ? 'bg-emerald-500 text-slate-950 hover:bg-emerald-400'
          : 'bg-slate-800 text-slate-200 hover:bg-slate-700'
      "
      @click="editMode = !editMode"
    >
      <Pencil v-if="!editMode" class="h-3.5 w-3.5" />
      <Eye v-else class="h-3.5 w-3.5" />
      <span>
        {{
          editMode
            ? isVi
              ? 'Xem trang'
              : 'Preview'
            : isVi
              ? 'Sửa trên trang'
              : 'Edit on page'
        }}
      </span>
    </button>

    <div v-if="editMode" class="flex rounded-xl bg-slate-800 p-0.5" role="group" :aria-label="isVi ? 'Ngôn ngữ nội dung' : 'Content language'">
      <button
        v-for="language in (['vi', 'en'] as const)"
        :key="language"
        type="button"
        class="rounded-lg px-2 py-1 font-semibold uppercase"
        :class="language === locale ? 'bg-white text-slate-900' : 'text-slate-200 hover:bg-slate-700'"
        :aria-label="language === 'vi' ? 'Tiếng Việt' : 'English'"
        :aria-pressed="language === locale"
        :disabled="visualSaving"
        @click="setLocale(language)"
      >
        {{ language }}
      </button>
    </div>

    <button
      type="button"
      class="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 py-1.5 font-semibold text-white transition-colors hover:bg-indigo-500 disabled:opacity-50"
      :disabled="!!activeTextId || !!resizingNodeId || !!draggedNodeId || !!inspectorDraft || visualSaving"
      @click="openEditorFor(canvasNodeId)"
    >
      <Layers class="h-3.5 w-3.5" />
      <span>{{ isVi ? 'Cấu trúc & định dạng' : 'Layout & formatting' }}</span>
    </button>

    <button
      type="button"
      class="inline-flex items-center gap-1.5 rounded-xl bg-slate-800 px-3 py-1.5 font-semibold text-slate-200 transition-colors hover:bg-slate-700"
      @click="openInbox"
    >
      <Inbox class="h-3.5 w-3.5 text-emerald-400" />
      <span>{{ isVi ? 'Hộp thư' : 'Inbox' }}</span>
      <span
        v-if="unreadMessagesCount > 0"
        class="rounded-full bg-emerald-500 px-1.5 py-0.5 font-mono text-[10px] font-bold text-slate-950"
      >
        {{ unreadMessagesCount }}
      </span>
    </button>

    <button
      type="button"
      class="inline-flex items-center gap-1 rounded-xl border border-slate-700 px-2.5 py-1.5 text-slate-300 transition-colors hover:bg-slate-800 hover:text-white"
      :title="isVi ? 'Đăng xuất' : 'Sign out'"
      @click="logout"
    >
      <LogOut class="h-3.5 w-3.5" />
      <span class="hidden sm:inline">{{ isVi ? 'Thoát' : 'Sign out' }}</span>
    </button>
  </div>
</template>
