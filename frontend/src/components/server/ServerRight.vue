<script setup lang="ts">
import {useInstanceStore} from '@/stores/useInstanceStore.ts'

const instanceStore = useInstanceStore()
</script>

<template>
  <div id="ServerRightFrame">
    <div class="log-content">
      <p v-for="(log, index) in instanceStore.getLogs" :key="index" class="log-line">
        {{ log }}
      </p>
    </div>
  </div>
</template>

<style scoped>
#ServerRightFrame {
  background-color: lightgrey;
  padding: 10px;
  border-radius: 5px;
  align-items: center;
  margin: 10px;
}

.log-content {
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.log-line {
  margin: 2px 0;
  padding: 2px 5px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
