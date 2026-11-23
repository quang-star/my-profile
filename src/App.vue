<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, watchEffect } from 'vue'
import CvModal from '@/components/CvModal.vue'
import Footer from '@/components/Footer.vue'
import Navbar from '@/components/Navbar.vue'
import AdminLoginModal from '@/components/admin/AdminLoginModal.vue'
import AdminMessagesModal from '@/components/admin/AdminMessagesModal.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import NodeInsertMenu from '@/components/nodes/NodeInsertMenu.vue'
import CanvasWorkspace from '@/components/nodes/CanvasWorkspace.vue'
import NodeRenderer from '@/components/nodes/NodeRenderer.vue'
import TagTreeEditor from '@/components/nodes/TagTreeEditor.vue'
import { useI18n } from '@/composables/useI18n'
import { useNodes } from '@/composables/useNodes'
import { usePortfolio } from '@/composables/usePortfolio'

const { t, isVi } = useI18n()
const { portfolio, initPortfolio, isAuthenticated, editMode, loginModalOpen } = usePortfolio()
const { nodesTree, initNodes, canvasNodeId, activeTextId, visualSaving, tagEditorOpen, inspectorDraft, clearHistory } = useNodes()

const isCvOpen = ref(false)

function clearSelection() {
  if (editMode.value && !activeTextId.value && !visualSaving.value && !tagEditorOpen.value && !inspectorDraft.value) {
    canvasNodeId.value = null
  }
}

function handleOutsideClick(event: MouseEvent) {
  if (event.target instanceof Element && !event.target.closest('[data-node-id], [data-editor-control]')) {
    clearSelection()
  }
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') clearSelection()
}

watch(editMode, (enabled) => { if (!enabled) canvasNodeId.value = null })
watch(isAuthenticated, (authenticated) => { if (!authenticated) clearHistory() })
onBeforeUnmount(() => window.removeEventListener('keydown', handleEscape))

onMounted(async () => {
  window.addEventListener('keydown', handleEscape)
  await Promise.all([initPortfolio(), initNodes()])
  if (window.location.hash === '#admin' && !isAuthenticated.value) {
    loginModalOpen.value = true
  }
})

watchEffect(() => {
  document.title = `${portfolio.value.name} — ${t(portfolio.value.role)}`
})
</script>

<template>
  <div class="min-h-screen bg-[#f5f6f8] text-zinc-900" @click="handleOutsideClick">
    <Navbar @open-cv="isCvOpen = true" />

    <CanvasWorkspace>
    <main class="no-print mx-auto max-w-4xl px-4 pb-16 sm:px-6">
      <NodeRenderer v-for="node in nodesTree" :key="node.id" :node="node" />
      <NodeInsertMenu
        v-if="editMode && !nodesTree.length"
        class="my-8 w-fit"
        :parent-id="null"
        :label="isVi ? 'Thêm khối' : 'Add block'"
      />
    </main>
    </CanvasWorkspace>

    <Footer />
    <CvModal :open="isCvOpen" @close="isCvOpen = false" />
    <TagTreeEditor />
    <AdminToolbar />
    <AdminLoginModal />
    <AdminMessagesModal />
  </div>
</template>
