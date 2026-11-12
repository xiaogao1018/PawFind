<script setup lang="ts">
defineProps<{ status: string }>()
const labels: Record<string, string> = {
  OPEN: '待认领', LOOKING: '寻找中', FOUND: '已寻回', WITHDRAWN: '已撤回',
  PENDING: '待审核', VERIFIED: '已核实', INVALID: '无效线索',
  APPROVED: '审核通过', REJECTED: '审核拒绝',
  VISIBLE: '公开', HIDDEN: '已隐藏', ACTIVE: '正常', DISABLED: '已停用'
}
</script>
<template><span class="status-badge" :data-status="status">{{ labels[status] || status }}</span></template>
