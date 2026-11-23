<script setup lang="ts">
import { CheckCircle2, Mail, Trash2, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi } = useI18n()
const { inboxModalOpen, messages, markMessageRead, deleteMessage } = usePortfolio()

useModalEffects(inboxModalOpen, () => {
  inboxModalOpen.value = false
})

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString(isVi.value ? 'vi-VN' : 'en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="inboxModalOpen"
      class="no-print fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-label="Contact Messages Inbox"
      @click.self="inboxModalOpen = false"
    >
      <div class="flex max-h-[85vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200 bg-white shadow-2xl">
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <div class="flex items-center gap-2.5">
            <Mail class="h-5 w-5 text-emerald-600" />
            <div>
              <h2 class="text-base font-bold text-slate-950">
                {{ isVi ? 'Hộp thư liên hệ (PostgreSQL)' : 'Contact Inbox (PostgreSQL)' }}
              </h2>
              <p class="text-xs text-slate-500">
                {{ messages.length }} {{ isVi ? 'tin nhắn đã lưu' : 'saved messages' }}
              </p>
            </div>
          </div>

          <button
            type="button"
            class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
            @click="inboxModalOpen = false"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <div
            v-if="messages.length === 0"
            class="rounded-2xl border border-dashed border-slate-200 py-10 text-center text-sm text-slate-500"
          >
            {{ isVi ? 'Chưa có tin nhắn liên hệ nào.' : 'No contact messages yet.' }}
          </div>

          <div v-else class="space-y-3">
            <article
              v-for="msg in messages"
              :key="msg.id"
              class="rounded-2xl border p-4 transition-colors"
              :class="msg.read ? 'border-slate-200 bg-slate-50/50' : 'border-emerald-200 bg-emerald-50/35'"
            >
              <div class="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="text-sm font-bold text-slate-950">{{ msg.subject }}</h3>
                    <span
                      v-if="!msg.read"
                      class="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700"
                    >
                      {{ isVi ? 'Mới' : 'New' }}
                    </span>
                  </div>
                  <p class="mt-0.5 text-xs text-slate-600">
                    <span class="font-semibold">{{ msg.name }}</span> ·
                    <a :href="`mailto:${msg.email}`" class="font-mono text-emerald-700 hover:underline">
                      {{ msg.email }}
                    </a>
                    · <span class="text-slate-400">{{ formatDate(msg.createdAt) }}</span>
                  </p>
                </div>

                <div class="flex items-center gap-1.5">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 hover:bg-slate-100"
                    @click="markMessageRead(msg.id, !msg.read)"
                  >
                    <CheckCircle2 class="h-3.5 w-3.5 text-emerald-600" />
                    <span>{{ msg.read ? (isVi ? 'Chưa đọc' : 'Mark unread') : (isVi ? 'Đã đọc' : 'Mark read') }}</span>
                  </button>
                  <button
                    type="button"
                    class="rounded-xl border border-rose-200 bg-white p-1.5 text-rose-600 hover:bg-rose-50"
                    :title="isVi ? 'Xóa' : 'Delete'"
                    @click="deleteMessage(msg.id)"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>

              <p class="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">
                {{ msg.message }}
              </p>
            </article>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
