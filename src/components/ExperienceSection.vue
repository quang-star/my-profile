<script setup lang="ts">
import { computed, ref } from 'vue'
import { Award, Briefcase, Compass, GraduationCap, Languages, Pencil, Plus, Trash2 } from '@lucide/vue'
import EducationEditModal from '@/components/admin/EducationEditModal.vue'
import ExperienceEditModal from '@/components/admin/ExperienceEditModal.vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'
import type { EducationItem, ExperienceItem, LocalizedText } from '@/types/profile'

const { t, tList, isVi } = useI18n()
const { portfolio, editMode, deleteEducation, deleteExperience } = usePortfolio()

const eduModalOpen = ref(false)
const editingEdu = ref<EducationItem | null>(null)
const expModalOpen = ref(false)
const editingExp = ref<ExperienceItem | null>(null)

const hasExperiences = computed(() => portfolio.value.experiences.length > 0)
const languagesList = computed(() => tList(portfolio.value.languages))
const coreStack = computed(() => portfolio.value.skills[0]?.items ?? [])

function formatPeriod(period: string | LocalizedText) {
  return typeof period === 'string' ? period : t(period)
}

function openCreateEdu() {
  editingEdu.value = null
  eduModalOpen.value = true
}

function openEditEdu(edu: EducationItem) {
  editingEdu.value = edu
  eduModalOpen.value = true
}

function openCreateExp() {
  editingExp.value = null
  expModalOpen.value = true
}

function openEditExp(exp: ExperienceItem) {
  editingExp.value = exp
  expModalOpen.value = true
}

async function handleDeleteEdu(edu: EducationItem) {
  if (!window.confirm(isVi.value ? `Xóa mục học vấn "${edu.school}"?` : `Delete education "${edu.school}"?`)) return
  await deleteEducation(edu.id)
}

async function handleDeleteExp(exp: ExperienceItem) {
  if (!window.confirm(isVi.value ? `Xóa kinh nghiệm tại "${exp.company}"?` : `Delete experience at "${exp.company}"?`)) return
  await deleteExperience(exp.id)
}
</script>

<template>
  <section id="experience" class="py-8">
    <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="font-mono text-xs font-semibold uppercase tracking-wider text-emerald-600">
          02 /
          {{
            hasExperiences
              ? isVi
                ? 'Kinh nghiệm & Học vấn'
                : 'Experience & Education'
              : isVi
                ? 'Học vấn & Định hướng'
                : 'Education & Direction'
          }}
        </p>
        <h2 class="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
          {{
            hasExperiences
              ? isVi
                ? 'Hành trình làm việc'
                : 'Work experience'
              : isVi
                ? 'Nền tảng học tập & mục tiêu'
                : 'Academic background & direction'
          }}
        </h2>
      </div>

      <div v-if="editMode" class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700"
          @click="openCreateExp"
        >
          <Plus class="h-3.5 w-3.5" />
          <span>{{ isVi ? 'Thêm kinh nghiệm' : 'Add experience' }}</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-100"
          @click="openCreateEdu"
        >
          <Plus class="h-3.5 w-3.5" />
          <span>{{ isVi ? 'Thêm học vấn' : 'Add education' }}</span>
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-4 lg:grid-cols-12">
      <!-- Experience Cards -->
      <div v-if="hasExperiences" class="flex flex-col gap-4 lg:col-span-8">
        <article
          v-for="exp in portfolio.experiences"
          :key="exp.id"
          class="bento-card bento-card-interactive"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="flex items-start gap-3">
              <div class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
                <Briefcase class="h-5 w-5" />
              </div>
              <div>
                <h3 class="text-lg font-bold text-slate-950">
                  {{ t(exp.role) }}
                </h3>
                <p class="text-sm font-semibold text-emerald-700">
                  {{ exp.company }}
                  <span class="font-normal text-slate-400">· {{ t(exp.location) }}</span>
                </p>
              </div>
            </div>

            <div class="flex items-start gap-2">
              <div class="flex flex-col items-start sm:items-end">
                <span class="rounded-full bg-slate-100 px-3 py-1 font-mono text-xs font-medium text-slate-700">
                  {{ t(exp.period) }}
                </span>
                <span class="mt-1 text-xs text-slate-400">
                  {{ t(exp.type) }}
                </span>
              </div>

              <div v-if="editMode" class="flex items-center gap-1">
                <button
                  type="button"
                  class="rounded-lg border border-slate-200 bg-slate-50 p-1.5 text-slate-600 hover:bg-slate-100"
                  @click="openEditExp(exp)"
                >
                  <Pencil class="h-3.5 w-3.5" />
                </button>
                <button
                  type="button"
                  class="rounded-lg border border-rose-200 bg-rose-50 p-1.5 text-rose-600 hover:bg-rose-100"
                  @click="handleDeleteExp(exp)"
                >
                  <Trash2 class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          <p class="mt-4 text-sm leading-relaxed text-slate-600">
            {{ t(exp.summary) }}
          </p>

          <ul class="mt-4 space-y-2 text-sm text-slate-700">
            <li
              v-for="(item, idx) in tList(exp.achievements)"
              :key="idx"
              class="flex items-start gap-2.5"
            >
              <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
              <span class="leading-relaxed">{{ item }}</span>
            </li>
          </ul>

          <div class="mt-5 flex flex-wrap gap-1.5">
            <span
              v-for="tech in exp.stack"
              :key="tech"
              class="rounded-lg bg-slate-100 px-2.5 py-1 font-mono text-xs text-slate-700"
            >
              {{ tech }}
            </span>
          </div>
        </article>
      </div>

      <!-- Education Column -->
      <div
        class="flex flex-col gap-4"
        :class="hasExperiences ? 'lg:col-span-4' : 'lg:col-span-7'"
      >
        <div
          v-for="edu in portfolio.education"
          :key="edu.id"
          class="bento-card bento-card-interactive flex h-full flex-col justify-between"
        >
          <div>
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
                <GraduationCap class="h-4 w-4 text-emerald-600" />
                <span>{{ isVi ? 'Học vấn' : 'Education' }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="rounded-full bg-slate-100 px-3 py-1 font-mono text-xs font-medium text-slate-600">
                  {{ formatPeriod(edu.period) }}
                </span>
                <div v-if="editMode" class="flex items-center gap-1">
                  <button
                    type="button"
                    class="rounded-lg border border-slate-200 bg-slate-50 p-1.5 text-slate-600 hover:bg-slate-100"
                    @click="openEditEdu(edu)"
                  >
                    <Pencil class="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    class="rounded-lg border border-rose-200 bg-rose-50 p-1.5 text-rose-600 hover:bg-rose-100"
                    @click="handleDeleteEdu(edu)"
                  >
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>

            <h3 class="mt-4 text-lg font-bold text-slate-950">
              {{ edu.school }}
            </h3>
            <p class="mt-1 text-sm font-semibold text-emerald-700">
              {{ t(edu.degree) }}
            </p>
            <p class="mt-3 text-sm leading-relaxed text-slate-600">
              {{ t(edu.details) }}
            </p>
          </div>

          <div v-if="edu.gpa" class="mt-5 flex items-center gap-2 border-t border-slate-100 pt-4">
            <span class="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200/80 bg-emerald-50 px-3 py-1.5 font-mono text-xs font-semibold text-emerald-800">
              <Award class="h-3.5 w-3.5 text-emerald-600" />
              GPA {{ edu.gpa }}
            </span>
          </div>
        </div>

        <div v-if="hasExperiences && languagesList.length" class="bento-card">
          <div class="flex items-center gap-2.5 text-xs font-semibold uppercase tracking-wider text-slate-400">
            <Languages class="h-4 w-4 text-indigo-600" />
            <span>{{ isVi ? 'Ngoại ngữ' : 'Languages' }}</span>
          </div>
          <ul class="mt-3 space-y-2 text-sm text-slate-700">
            <li
              v-for="(lang, idx) in languagesList"
              :key="idx"
              class="flex items-start gap-2.5"
            >
              <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
              <span>{{ lang }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Companion Bento Card when experiences is empty -->
      <div v-if="!hasExperiences" class="flex flex-col gap-4 lg:col-span-5">
        <div v-if="languagesList.length" class="bento-card">
          <div class="flex items-center gap-2.5 text-xs font-semibold uppercase tracking-wider text-slate-400">
            <Languages class="h-4 w-4 text-indigo-600" />
            <span>{{ isVi ? 'Ngoại ngữ' : 'Languages' }}</span>
          </div>
          <ul class="mt-3 space-y-2 text-sm text-slate-700">
            <li
              v-for="(lang, idx) in languagesList"
              :key="idx"
              class="flex items-start gap-2.5"
            >
              <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
              <span>{{ lang }}</span>
            </li>
          </ul>
        </div>

        <div class="bento-card flex h-full flex-col justify-between bg-slate-900 text-white border-slate-800">
          <div>
            <div class="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <Compass class="h-4 w-4 text-emerald-400" />
              <span>{{ isVi ? 'Định hướng phát triển' : 'Career direction' }}</span>
            </div>
            <h3 class="mt-3 text-base font-bold text-white">
              {{ t(portfolio.role) }}
            </h3>
            <p class="mt-2 text-sm leading-relaxed text-slate-300">
              {{ t(portfolio.currentFocus) }}
            </p>
          </div>

          <div v-if="coreStack.length" class="mt-5 flex flex-wrap gap-1.5 border-t border-slate-800 pt-4">
            <span
              v-for="item in coreStack"
              :key="item"
              class="rounded-lg border border-slate-700 bg-slate-800/80 px-2.5 py-1 font-mono text-xs text-emerald-300"
            >
              {{ item }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <EducationEditModal :open="eduModalOpen" :item="editingEdu" @close="eduModalOpen = false" />
    <ExperienceEditModal :open="expModalOpen" :item="editingExp" @close="expModalOpen = false" />
  </section>
</template>
