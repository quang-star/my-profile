<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowUpRight, Check, Copy, Mail, Send } from '@lucide/vue'
import SocialIcon from '@/components/SocialIcon.vue'
import { useI18n } from '@/composables/useI18n'
import { usePortfolio } from '@/composables/usePortfolio'

const { isVi } = useI18n()
const { portfolio, sendContactMessage } = usePortfolio()

const form = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const copied = ref(false)
const sending = ref(false)
const submittedMode = ref<'api' | 'mailto' | 'copied' | null>(null)
const errorText = ref('')

const hasEmail = computed(() => portfolio.value.email.trim().length > 0)

async function copyEmail() {
  if (!hasEmail.value) return
  try {
    await navigator.clipboard.writeText(portfolio.value.email)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    window.location.href = `mailto:${portfolio.value.email}`
  }
}

async function handleSubmit() {
  errorText.value = ''
  submittedMode.value = null

  if (!form.name.trim() || !form.email.trim() || !form.message.trim()) {
    errorText.value = isVi.value
      ? 'Vui lòng điền đầy đủ Họ tên, Email và Nội dung tin nhắn.'
      : 'Please fill in your name, email, and message.'
    return
  }

  const subjectText = form.subject.trim() || `[Portfolio] Liên hệ từ ${form.name.trim()}`
  const bodyText = `From: ${form.name.trim()} (${form.email.trim()})\nSubject: ${subjectText}\n\n${form.message.trim()}`

  sending.value = true
  try {
    const savedToApi = await sendContactMessage({
      name: form.name.trim(),
      email: form.email.trim(),
      subject: subjectText,
      message: form.message.trim(),
    })

    if (savedToApi) {
      submittedMode.value = 'api'
    } else if (hasEmail.value) {
      const mailSubject = encodeURIComponent(subjectText)
      const mailBody = encodeURIComponent(bodyText)
      submittedMode.value = 'mailto'
      window.location.href = `mailto:${portfolio.value.email}?subject=${mailSubject}&body=${mailBody}`
    } else {
      try {
        await navigator.clipboard.writeText(bodyText)
      } catch {
        // Ignore clipboard errors in restricted environments
      }
      submittedMode.value = 'copied'
    }

    form.name = ''
    form.email = ''
    form.subject = ''
    form.message = ''
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <section id="contact" class="py-8">
    <div class="mb-5">
      <p class="font-mono text-xs font-semibold uppercase tracking-wider text-emerald-600">
        04 / {{ isVi ? 'Liên hệ' : 'Contact' }}
      </p>
      <h2 class="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
        {{ isVi ? 'Kết nối & trao đổi công việc' : "Let's work together" }}
      </h2>
    </div>

    <div class="grid grid-cols-1 gap-4 lg:grid-cols-12">
      <!-- Direct Info Bento Card -->
      <div class="bento-card flex flex-col justify-between bg-slate-900 text-white border-slate-800 lg:col-span-5">
        <div>
          <h3 class="text-xl font-bold text-white">
            {{ isVi ? 'Sẵn sàng trao đổi' : 'Always happy to chat' }}
          </h3>
          <p class="mt-3 text-sm leading-relaxed text-slate-300">
            {{
              isVi
                ? 'Bạn muốn trao đổi về cơ hội thực tập, vị trí Java Backend hoặc hợp tác dự án? Hãy kết nối qua kênh bên dưới hoặc để lại lời nhắn.'
                : 'Want to discuss an internship, Java Backend role, or project collaboration? Reach out via the links below or send a message.'
            }}
          </p>

          <div v-if="hasEmail" class="mt-6 rounded-2xl border border-slate-800 bg-slate-800/70 p-4">
            <div class="text-xs font-medium text-slate-400">
              {{ isVi ? 'Email trực tiếp' : 'Direct email' }}
            </div>
            <div class="mt-1 flex items-center justify-between gap-2">
              <a
                :href="`mailto:${portfolio.email}`"
                class="truncate font-mono text-sm font-semibold text-emerald-400 hover:underline"
              >
                {{ portfolio.email }}
              </a>
              <button
                type="button"
                class="inline-flex shrink-0 items-center gap-1 rounded-xl border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-xs font-medium text-slate-200 hover:bg-slate-800"
                @click="copyEmail"
              >
                <Check v-if="copied" class="h-3.5 w-3.5 text-emerald-400" />
                <Copy v-else class="h-3.5 w-3.5" />
                <span>{{ copied ? (isVi ? 'Đã chép' : 'Copied') : (isVi ? 'Chép' : 'Copy') }}</span>
              </button>
            </div>
          </div>

          <div class="mt-4 space-y-2">
            <a
              v-for="social in portfolio.socials"
              :key="`${social.id}-${social.url}`"
              :href="social.url"
              target="_blank"
              rel="noopener noreferrer"
              class="group flex items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-800/60 px-4 py-3 text-sm transition-colors hover:border-slate-700 hover:bg-slate-800"
            >
              <span class="flex shrink-0 items-center gap-2.5 font-medium text-white">
                <SocialIcon :id="social.id" class="h-4 w-4 text-emerald-400" />
                {{ social.label }}
              </span>
              <span class="flex min-w-0 items-center gap-1 font-mono text-xs text-slate-300 group-hover:text-white">
                <span class="truncate">{{ social.handle }}</span>
                <ArrowUpRight class="h-3.5 w-3.5 shrink-0 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
              </span>
            </a>
          </div>
        </div>

        <div class="mt-6 flex items-center gap-2 text-xs text-slate-400">
          <Mail class="h-4 w-4 text-emerald-400" />
          <span>
            {{ isVi ? 'Cảm ơn bạn đã ghé thăm.' : 'Thanks for stopping by.' }}
          </span>
        </div>
      </div>

      <!-- Contact Form Bento Card -->
      <div class="bento-card lg:col-span-7">
        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="contact-name">
                {{ isVi ? 'Họ và tên' : 'Your name' }}
              </label>
              <input
                id="contact-name"
                v-model="form.name"
                type="text"
                :placeholder="isVi ? 'Nguyễn Văn A' : 'Jane Doe'"
                class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-2.5 text-sm text-slate-900 outline-none transition-colors focus:border-slate-900 focus:bg-white"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="contact-email">
                Email
              </label>
              <input
                id="contact-email"
                v-model="form.email"
                type="email"
                placeholder="you@company.com"
                class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-2.5 text-sm text-slate-900 outline-none transition-colors focus:border-slate-900 focus:bg-white"
              />
            </div>
          </div>

          <div>
            <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="contact-subject">
              {{ isVi ? 'Chủ đề' : 'Subject' }}
            </label>
            <input
              id="contact-subject"
              v-model="form.subject"
              type="text"
              :placeholder="isVi ? 'Trao đổi cơ hội hợp tác / Tuyển dụng' : 'Job opportunity / Project inquiry'"
              class="w-full rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-2.5 text-sm text-slate-900 outline-none transition-colors focus:border-slate-900 focus:bg-white"
            />
          </div>

          <div>
            <label class="mb-1.5 block text-xs font-semibold text-slate-700" for="contact-message">
              {{ isVi ? 'Nội dung' : 'Message' }}
            </label>
            <textarea
              id="contact-message"
              v-model="form.message"
              rows="4"
              :placeholder="isVi ? 'Chia sẻ ngắn gọn về vị trí hoặc dự án của bạn...' : 'Tell me briefly about your role or project...'"
              class="w-full resize-none rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3 text-sm text-slate-900 outline-none transition-colors focus:border-slate-900 focus:bg-white"
            />
          </div>

          <p v-if="errorText" class="text-xs font-medium text-rose-600">
            {{ errorText }}
          </p>

          <div
            v-if="submittedMode === 'api'"
            class="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-medium text-emerald-800"
          >
            {{
              isVi
                ? 'Đã gửi tin nhắn thành công! Mình sẽ xem trong hộp thư và phản hồi sớm nhất.'
                : 'Message sent successfully! I will review it in my inbox and get back to you soon.'
            }}
          </div>

          <div
            v-else-if="submittedMode === 'mailto'"
            class="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-medium text-emerald-800"
          >
            {{
              isVi
                ? 'Đã mở ứng dụng email của bạn với nội dung vừa nhập. Cảm ơn bạn đã liên hệ!'
                : 'Opened your email client with the prepared message. Thanks for reaching out!'
            }}
          </div>

          <div
            v-else-if="submittedMode === 'copied'"
            class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs font-medium text-amber-900"
          >
            {{
              isVi
                ? 'Backend chưa bật & chưa có email — nội dung tin nhắn đã được sao chép vào clipboard.'
                : 'Backend is offline and no email is set — your message has been copied to clipboard.'
            }}
          </div>

          <div class="flex justify-end">
            <button
              type="submit"
              :disabled="sending"
              class="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:opacity-60"
            >
              <Send class="h-4 w-4" />
              <span>{{ sending ? (isVi ? 'Đang gửi...' : 'Sending...') : (isVi ? 'Gửi tin nhắn' : 'Send message') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>
