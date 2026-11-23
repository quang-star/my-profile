<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Lock, X } from '@lucide/vue'
import { useI18n } from '@/composables/useI18n'
import { useModalEffects } from '@/composables/useModalEffects'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi } = useI18n()
const { loginModalOpen, login } = usePortfolio()

useModalEffects(loginModalOpen, () => {
  loginModalOpen.value = false
})

const form = reactive({
  username: 'admin',
  password: '',
})

const loading = ref(false)
const errorText = ref('')

async function handleSubmit() {
  errorText.value = ''
  if (!form.username.trim() || !form.password) {
    errorText.value = isVi.value
      ? 'Vui lòng nhập tên đăng nhập và mật khẩu.'
      : 'Please enter username and password.'
    return
  }

  loading.value = true
  try {
    await login(form.username.trim(), form.password)
    form.password = ''
  } catch (err) {
    errorText.value =
      err instanceof Error
        ? err.message
        : isVi.value
          ? 'Đăng nhập thất bại. Kiểm tra lại backend hoặc mật khẩu.'
          : 'Login failed. Please check credentials or backend availability.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="loginModalOpen"
      class="no-print fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-label="Admin Login"
      @click.self="loginModalOpen = false"
    >
      <div class="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-7">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-900 text-white">
              <Lock class="h-4 w-4" />
            </div>
            <div>
              <h2 class="text-lg font-bold text-slate-950">
                {{ isVi ? 'Đăng nhập quản trị' : 'Admin Sign In' }}
              </h2>
              <p class="text-xs text-slate-500">
                {{ isVi ? 'Bật chế độ thêm/sửa từng thẻ trên Portfolio.' : 'Unlock card-by-card editing & inbox.' }}
              </p>
            </div>
          </div>

          <button
            type="button"
            class="rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
            aria-label="Close login modal"
            @click="loginModalOpen = false"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <form class="mt-6 space-y-4" @submit.prevent="handleSubmit">
          <div>
            <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="admin-username">
              {{ isVi ? 'Tài khoản' : 'Username' }}
            </label>
            <input
              id="admin-username"
              v-model="form.username"
              type="text"
              autocomplete="username"
              class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-900 focus:bg-white"
            />
          </div>

          <div>
            <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="admin-password">
              {{ isVi ? 'Mật khẩu' : 'Password' }}
            </label>
            <input
              id="admin-password"
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
              class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-900 focus:bg-white"
            />
          </div>

          <p v-if="errorText" class="rounded-xl bg-rose-50 px-3 py-2 text-xs font-medium text-rose-700">
            {{ errorText }}
          </p>

          <button
            type="submit"
            :disabled="loading"
            class="w-full rounded-2xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:opacity-60"
          >
            {{
              loading
                ? isVi
                  ? 'Đang xác thực...'
                  : 'Signing in...'
                : isVi
                  ? 'Đăng nhập (JWT)'
                  : 'Sign in (JWT)'
            }}
          </button>
        </form>
      </div>
    </div>
  </Teleport>
</template>
