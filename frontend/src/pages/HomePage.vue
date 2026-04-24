<script setup lang="ts">

import HomeLeft from "@/components/home/HomeLeft.vue";
import HomeRight from "@/components/home/HomeRight.vue";
</script>

<template>
  <div class="page-wrapper">
    <a-layout>
      <a-layout-sider class="layout" :width=300>
        <HomeLeft></HomeLeft>
      </a-layout-sider>
      <a-layout-content class="layout" >
        <HomeRight></HomeRight>
      </a-layout-content>
    </a-layout>
  </div>

</template>

<style scoped>

.layout{
  background-color: white;

}
</style>

