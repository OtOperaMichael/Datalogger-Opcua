<script setup lang="ts">


import GlobalHeader from "@/components/GlobalHeader.vue";
</script>

<template>
  <div id="basicLayout">
    <a-layout class="layout">

      <a-layout-header id="globalHeader"  class="header">
        <GlobalHeader/>
      </a-layout-header>
      <a-layout id="globalContent" class="mainContent">
        <router-view></router-view>
      </a-layout>

    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout{
  height: 100vh;
  display: flex;
  flex-direction: column;
  /* 2. 防止内容溢出导致整个页面滚动，让滚动发生在子组件内部 */
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

#globalHeader {
  height: 60px;
  line-height: 60px;
  background-color: #ffffff;
  color: white;
  padding: 0 24px;
  font-size: 16px;
  margin-bottom: 10px;
}







</style>
