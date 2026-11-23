<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowRight, ArrowUpRight, ExternalLink, FolderGit2, Pencil, Plus, Star, Trash2, X } from '@lucide/vue'
import ProjectEditModal from '@/components/admin/ProjectEditModal.vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'
import type { ProjectItem } from '@/types/profile'

const { t, tList, isVi } = useI18n()
const { portfolio, editMode, deleteProject } = usePortfolio()

type FilterType = 'all' | ProjectItem['category']
const activeFilter = ref<FilterType>('all')
const selectedProject = ref<ProjectItem | null>(null)
const isModalOpen = computed(() => selectedProject.value !== null)

const editModalOpen = ref(false)
const editingProject = ref<ProjectItem | null>(null)

useModalEffects(isModalOpen, () => {
  selectedProject.value = null
})

const allFilters: { id: FilterType; label: { vi: string; en: string } }[] = [
  { id: 'all', label: { vi: 'Tất cả', en: 'All' } },
  { id: 'fullstack', label: { vi: 'Fullstack', en: 'Fullstack' } },
  { id: 'frontend', label: { vi: 'Frontend', en: 'Frontend' } },
  { id: 'tooling', label: { vi: 'Tooling', en: 'Tooling' } },
  { id: 'work', label: { vi: 'Việc đã làm', en: 'Work' } },
]

function categoryLabel(category: ProjectItem['category']) {
  const match = allFilters.find((f) => f.id === category)
  return match ? t(match.label) : category
}

const filters = computed(() => {
  if (editMode.value) return allFilters
  const categories = new Set(portfolio.value.projects.map((p) => p.category))
  return allFilters.filter((f) => f.id === 'all' || categories.has(f.id))
})

const filteredProjects = computed(() => {
  if (activeFilter.value === 'all') return portfolio.value.projects
  return portfolio.value.projects.filter((project) => project.category === activeFilter.value)
})

function openCreateProject() {
  editingProject.value = null
  editModalOpen.value = true
}

function openEditProject(project: ProjectItem) {
  editingProject.value = project
  editModalOpen.value = true
}

async function handleDeleteProject(project: ProjectItem) {
  const ok = window.confirm(
    isVi.value ? `Xóa dự án "${project.title}"?` : `Delete project "${project.title}"?`,
  )
  if (!ok) return
  if (selectedProject.value?.id === project.id) {
    selectedProject.value = null
  }
  await deleteProject(project.id)
}
</script>

<template>
  <section id="projects" class="py-8">
    <div class="mb-5 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="font-mono text-xs font-semibold uppercase tracking-wider text-emerald-600">
          03 / {{ isVi ? 'Dự án tiêu biểu' : 'Featured Projects' }}
        </p>
        <h2 class="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
          {{ isVi ? 'Sản phẩm đã xây dựng' : 'Things I have built' }}
        </h2>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <button
          v-if="editMode"
          type="button"
          class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-emerald-700"
          @click="openCreateProject"
        >
          <Plus class="h-3.5 w-3.5" />
          <span>{{ isVi ? 'Thêm dự án' : 'Add project' }}</span>
        </button>

        <div class="flex flex-wrap gap-1.5 rounded-2xl border border-slate-200 bg-white p-1">
          <button
            v-for="filter in filters"
            :key="filter.id"
            type="button"
            class="rounded-xl px-3.5 py-1.5 text-xs font-semibold transition-colors"
            :class="
              activeFilter === filter.id
                ? 'bg-slate-900 text-white'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
            "
            @click="activeFilter = filter.id"
          >
            {{ t(filter.label) }}
          </button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-12">
      <article
        v-for="project in filteredProjects"
        :key="project.id"
        class="bento-card bento-card-interactive group flex cursor-pointer flex-col justify-between md:col-span-6"
        :class="
          project.featured
            ? 'border-emerald-200/90 bg-gradient-to-b from-emerald-50/45 via-white to-white ring-1 ring-emerald-500/10'
            : ''
        "
        role="button"
        tabindex="0"
        :aria-label="`${isVi ? 'Xem chi tiết' : 'View details'}: ${project.title}`"
        @click="selectedProject = project"
        @keydown.enter.self="selectedProject = project"
        @keydown.space.self.prevent="selectedProject = project"
      >
        <div>
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-2.5 py-1 font-mono text-[11px] font-medium uppercase text-slate-600"
              >
                <FolderGit2 class="h-3.5 w-3.5 text-emerald-600" />
                {{ categoryLabel(project.category) }}
              </span>

              <span
                v-if="project.featured"
                class="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700"
              >
                <Star class="h-3 w-3 fill-emerald-500 text-emerald-600" />
                {{ isVi ? 'Chủ lực' : 'Featured' }}
              </span>
            </div>

            <div class="flex items-center gap-2">
              <span class="font-mono text-xs text-slate-400">
                {{ project.year }}
              </span>
              <div v-if="editMode" class="flex items-center gap-1" @click.stop @keydown.stop>
                <button
                  type="button"
                  class="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                  :title="isVi ? 'Sửa dự án' : 'Edit project'"
                  @click="openEditProject(project)"
                >
                  <Pencil class="h-3.5 w-3.5" />
                </button>
                <button
                  type="button"
                  class="rounded-lg border border-rose-200 bg-rose-50 p-1.5 text-rose-600 hover:bg-rose-100"
                  :title="isVi ? 'Xóa dự án' : 'Delete project'"
                  @click="handleDeleteProject(project)"
                >
                  <Trash2 class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          <h3 class="mt-4 text-lg font-bold tracking-tight text-slate-950 transition-colors group-hover:text-emerald-800">
            {{ project.title }}
          </h3>
          <p class="mt-2 text-sm leading-relaxed text-slate-600">
            {{ t(project.tagline) }}
          </p>

          <ul class="mt-4 space-y-1.5 text-xs text-slate-600">
            <li
              v-for="(point, idx) in tList(project.highlights).slice(0, 2)"
              :key="idx"
              class="flex items-start gap-2"
            >
              <span class="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-emerald-500" />
              <span>{{ point }}</span>
            </li>
          </ul>
        </div>

        <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="tech in project.stack.slice(0, 4)"
              :key="tech"
              class="rounded-lg bg-slate-100 px-2 py-1 font-mono text-[11px] text-slate-700"
            >
              {{ tech }}
            </span>
          </div>

          <div class="flex items-center gap-2">
            <span class="inline-flex items-center gap-1 text-xs font-semibold text-slate-400 transition-all group-hover:text-emerald-700">
              <span>{{ isVi ? 'Chi tiết' : 'Details' }}</span>
              <ArrowRight class="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
            </span>

            <div class="flex items-center gap-1.5" @click.stop @keydown.stop>
              <a
                v-if="project.repoUrl"
                :href="project.repoUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-100"
              >
                {{ project.secondaryRepoUrl ? 'GitHub FE' : 'GitHub' }}
              </a>
              <a
                v-if="project.secondaryRepoUrl"
                :href="project.secondaryRepoUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-100"
              >
                GitHub BE
              </a>
              <a
                v-if="project.liveUrl"
                :href="project.liveUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-slate-800"
              >
                <span>Demo</span>
                <ArrowUpRight class="h-3.5 w-3.5" />
              </a>
            </div>
          </div>
        </div>
      </article>
    </div>

    <!-- Project Detail Modal -->
    <Teleport to="body">
      <div
        v-if="selectedProject"
        class="no-print fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-xs"
        role="dialog"
        aria-modal="true"
        :aria-label="selectedProject.title"
        @click.self="selectedProject = null"
      >
        <div class="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8">
          <div class="flex items-start justify-between gap-4">
            <div>
              <span class="font-mono text-xs font-semibold uppercase text-emerald-600">
                {{ categoryLabel(selectedProject.category) }} · {{ selectedProject.year }}
              </span>
              <h3 class="mt-1 text-xl font-extrabold text-slate-950">
                {{ selectedProject.title }}
              </h3>
            </div>
            <button
              type="button"
              class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
              aria-label="Close modal"
              @click="selectedProject = null"
            >
              <X class="h-4 w-4" />
            </button>
          </div>

          <p class="mt-4 text-sm leading-relaxed text-slate-700">
            {{ t(selectedProject.description) }}
          </p>

          <div class="mt-5">
            <h4 class="text-xs font-semibold uppercase tracking-wider text-slate-400">
              {{ isVi ? 'Điểm nổi bật' : 'Key highlights' }}
            </h4>
            <ul class="mt-2.5 space-y-2 text-sm text-slate-700">
              <li
                v-for="(item, idx) in tList(selectedProject.highlights)"
                :key="idx"
                class="flex items-start gap-2.5"
              >
                <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
                <span>{{ item }}</span>
              </li>
            </ul>
          </div>

          <div class="mt-5">
            <h4 class="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Tech stack
            </h4>
            <div class="mt-2 flex flex-wrap gap-1.5">
              <span
                v-for="tech in selectedProject.stack"
                :key="tech"
                class="rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs text-slate-800"
              >
                {{ tech }}
              </span>
            </div>
          </div>

          <div class="mt-7 flex flex-wrap gap-3">
            <a
              v-if="selectedProject.liveUrl"
              :href="selectedProject.liveUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="inline-flex items-center gap-1.5 rounded-2xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800"
            >
              <ExternalLink class="h-4 w-4" />
              <span>{{ isVi ? 'Xem Live Demo' : 'Open Live Demo' }}</span>
            </a>
            <a
              v-if="selectedProject.repoUrl"
              :href="selectedProject.repoUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="inline-flex items-center gap-1.5 rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 hover:bg-slate-100"
            >
              <span>{{ isVi ? 'Xem mã nguồn (GitHub)' : 'View Source (GitHub)' }}</span>
            </a>
            <a
              v-if="selectedProject.secondaryRepoUrl"
              :href="selectedProject.secondaryRepoUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="inline-flex items-center rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 hover:bg-slate-100"
            >
              {{ isVi ? 'Mã nguồn Backend' : 'Backend source' }}
            </a>
          </div>
        </div>
      </div>
    </Teleport>

    <ProjectEditModal :open="editModalOpen" :project="editingProject" @close="editModalOpen = false" />
  </section>
</template>
