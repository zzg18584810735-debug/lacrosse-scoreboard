<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  title: {
    type: String,
    required: true,
  },
  formId: {
    type: String,
    default: '',
  },
  confirmLabel: {
    type: String,
    default: '确定',
  },
  cancelLabel: {
    type: String,
    default: '取消',
  },
  wide: Boolean,
  danger: Boolean,
})

const emit = defineEmits(['close'])
const element = ref(null)

async function syncOpen(open) {
  await nextTick()
  if (!element.value) return
  if (open && !element.value.open) element.value.showModal()
  if (!open && element.value.open) element.value.close()
}

watch(() => props.open, syncOpen)
onMounted(() => syncOpen(props.open))
</script>

<template>
  <dialog
    ref="element"
    :class="['base-dialog', { 'base-dialog-wide': wide }]"
    aria-labelledby="dialog-title"
    @cancel.prevent="emit('close')"
  >
    <div class="base-dialog-shell">
      <header class="base-dialog-header">
        <h2 id="dialog-title">{{ title }}</h2>
        <button type="button" @click="emit('close')">关闭</button>
      </header>

      <div class="base-dialog-body"><slot /></div>

      <footer class="base-dialog-footer">
        <slot name="footer">
          <button type="button" @click="emit('close')">{{ cancelLabel }}</button>
          <button type="submit" :class="danger ? 'danger' : 'primary'" :form="formId">{{ confirmLabel }}</button>
        </slot>
      </footer>
    </div>
  </dialog>
</template>
