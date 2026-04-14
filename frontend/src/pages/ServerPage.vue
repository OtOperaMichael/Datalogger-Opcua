<script setup lang="ts">

import HomeRight from "@/components/home/HomeRight.vue";
import HomeLeft from "@/components/home/HomeLeft.vue";
import ServerLeft from "@/components/server/ServerLeft.vue";
import ServerRight from "@/components/server/ServerRight.vue";
</script>

<template>
  <div class="page-wrapper">
    <a-layout>
      <a-layout-sider class="layout" :width=1000>
        <ServerLeft></ServerLeft>
      </a-layout-sider>
      <a-layout-content class="layout">
        <ServerRight></ServerRight>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<style scoped>
.layout{
  background-color: white;

}
</style>
