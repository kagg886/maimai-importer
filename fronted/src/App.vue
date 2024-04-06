<script setup lang="ts">

import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";

const progress = ref(1)

const route = useRouter()
onMounted(() => {
  route.push(window.location.pathname)
})

</script>

<template>
  <div class="container">
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
