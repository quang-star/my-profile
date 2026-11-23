<script setup lang="ts">
import { ref } from 'vue'
import { Code2, Database, Globe, Layers, Pencil, Plus, Server, Sparkles, Trash2, Wrench } from '@lucide/vue'
import SkillEditModal from '@/components/admin/SkillEditModal.vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'
import type { SkillGroup } from '@/types/profile'

const { t, isVi } = useI18n()
const { portfolio, editMode, deleteSkill } = usePortfolio()

const skillModalOpen = ref(false)
const editingSkill = ref<SkillGroup | null>(null)

const iconMap = {
  backend: Server,
  data: Database,
  integration: Sparkles,
  web: Globe,
  frontend: Code2,
  tooling: Wrench,
  product: Layers,
} as const

const accentClasses: Record<SkillGroup['accent'], { badge: string; iconBg: string }> = {
  emerald: {
    badge: 'border-emerald-200/80 bg-emerald-50/80 text-emerald-800',
    iconBg: 'bg-emerald-100 text-emerald-700',
  },
  indigo: {
    badge: 'border-indigo-200/80 bg-indigo-50/80 text-indigo-800',
    iconBg: 'bg-indigo-100 text-indigo-700',
  },
  amber: {
    badge: 'border-amber-200/80 bg-amber-50/80 text-amber-800',
    iconBg: 'bg-amber-100 text-amber-700',
  },
  sky: {
    badge: 'border-sky-200/80 bg-sky-50/80 text-sky-800',
    iconBg: 'bg-sky-100 text-sky-700',
  },
}

function getIcon(id: string) {
  return iconMap[id as keyof typeof iconMap] ?? Code2
}

function openCreateSkill() {
  editingSkill.value = null
  skillModalOpen.value = true
}

function openEditSkill(group: SkillGroup) {
  editingSkill.value = group
  skillModalOpen.value = true
}

async function handleDeleteSkill(group: SkillGroup) {
  const ok = window.confirm(
    isVi.value
      ? `Xóa nhóm kỹ năng "${group.title.vi}"?`
      : `Delete skill group "${group.title.en}"?`,
  )
  if (!ok) return
  await deleteSkill(group.id)
}
</script>

<template>
  <section id="skills" class="py-8">
    <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="font-mono text-xs font-semibold uppercase tracking-wider text-emerald-600">
          01 / {{ isVi ? 'Kỹ năng & Công nghệ' : 'Skills & Stack' }}
        </p>
        <h2 class="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
          {{ isVi ? 'Bộ công cụ làm việc hằng ngày' : 'Tools & technologies I work with' }}
        </h2>
      </div>

      <button
        v-if="editMode"
        type="button"
        class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-emerald-700"
        @click="openCreateSkill"
      >
        <Plus class="h-3.5 w-3.5" />
        <span>{{ isVi ? 'Thêm nhóm kỹ năng' : 'Add skill group' }}</span>
      </button>
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
      <article
        v-for="group in portfolio.skills"
        :key="group.id"
        class="bento-card bento-card-interactive flex flex-col justify-between"
      >
        <div>
          <div class="flex items-start justify-between gap-2">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-2xl"
                :class="accentClasses[group.accent]?.iconBg ?? accentClasses.emerald.iconBg"
              >
                <component :is="getIcon(group.id)" class="h-5 w-5" />
              </div>
              <div>
                <h3 class="text-base font-bold text-slate-900">
                  {{ t(group.title) }}
                </h3>
              </div>
            </div>

            <div v-if="editMode" class="flex items-center gap-1">
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-slate-50 p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                :title="isVi ? 'Sửa' : 'Edit'"
                @click="openEditSkill(group)"
              >
                <Pencil class="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                class="rounded-lg border border-rose-200 bg-rose-50 p-1.5 text-rose-600 hover:bg-rose-100"
                :title="isVi ? 'Xóa' : 'Delete'"
                @click="handleDeleteSkill(group)"
              >
                <Trash2 class="h-3.5 w-3.5" />
              </button>
            </div>
          </div>

          <p class="mt-3 text-sm leading-relaxed text-slate-600">
            {{ t(group.description) }}
          </p>
        </div>

        <div class="mt-5 flex flex-wrap gap-2">
          <span
            v-for="item in group.items"
            :key="item"
            class="rounded-xl border px-3 py-1.5 font-mono text-xs font-medium"
            :class="accentClasses[group.accent]?.badge ?? accentClasses.emerald.badge"
          >
            {{ item }}
          </span>
        </div>
      </article>
    </div>

    <SkillEditModal :open="skillModalOpen" :skill="editingSkill" @close="skillModalOpen = false" />
  </section>
</template>
