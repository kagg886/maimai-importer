<script setup lang="ts">

import {onMounted, provide, ref} from "vue";

const progress = ref(1)

onMounted(() => {
  provide('container_height',document.getElementById('container')!!.clientHeight)
})

</script>

<template>
  <div id="container" class="container">
    <v-progress-linear v-model="progress"></v-progress-linear>
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.container {
  width: 90%;
  height: 90%;
  border: 2px solid #ccc;
  border-radius: 10px;
  overflow: hidden;
}

</style>
