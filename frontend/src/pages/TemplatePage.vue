<script setup lang="ts">

import HomeRight from "@/components/home/HomeRight.vue";
import HomeLeft from "@/components/home/HomeLeft.vue";
import TemplateLeft from "@/components/template/TemplateLeft.vue";
import TemplateRight from "@/components/template/TemplateRight.vue";
</script>

<template>
  <div class="page-wrapper">
    <a-layout>
      <a-layout-sider class="layout" :width=300>
        <TemplateLeft></TemplateLeft>
      </a-layout-sider>
      <a-layout-content class="layout">
        <TemplateRight></TemplateRight>
      </a-layout-content>
    </a-layout>
  </div>

</template>

<style scoped>
.layout{
  background-color: white;

}
</style>
