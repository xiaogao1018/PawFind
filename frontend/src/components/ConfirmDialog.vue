<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
const props = defineProps<{
  open: boolean; title: string; description: string; requireReason?: boolean; busy?: boolean; error?: string
}>()
const emit = defineEmits<{ confirm: [reason: string]; cancel: [] }>()
const dialog = ref<HTMLDialogElement | null>(null)
const reason = ref('')
watch(() => props.open, async open => {
  await nextTick()
  if (open && !dialog.value?.open) {
    reason.value = ''
    dialog.value?.showModal()
  } else if (!open && dialog.value?.open) dialog.value.close()
}, { immediate: true })
function submit() {
  if (props.requireReason && !reason.value.trim()) return
  emit('confirm', reason.value.trim())
}
function cancel() {
  if (!props.busy) emit('cancel')
}
</script>
<template>
  <dialog ref="dialog" class="confirm-dialog" aria-labelledby="confirm-title" @cancel.prevent="cancel">
    <form class="form-stack" @submit.prevent="submit">
      <h2 id="confirm-title">{{ title }}</h2><p class="muted">{{ description }}</p>
      <label v-if="requireReason">处理原因<textarea v-model="reason" maxlength="255" rows="3" required autofocus placeholder="请填写原因，便于后续核查"></textarea></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <div class="dialog-actions"><button class="button button-light" type="button" :disabled="busy" @click="cancel">取消</button><button class="button" :disabled="busy">{{ busy ? '处理中……' : '确认操作' }}</button></div>
    </form>
  </dialog>
</template>
