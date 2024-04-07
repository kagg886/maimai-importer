<script setup lang="ts">

import {onMounted, provide, ref} from "vue";
import {request} from "./util/network.ts";

const progress = ref(1)

onMounted(() => {
  provide('container_height', document.getElementById('container')!!.clientHeight)
})

const icons: {
  [key: string]: string
} = {
  "mdi-github": "https://github.com/kagg886/maimai-importer",
  "mdi-home": "https://kagg886.top"
}

const open = (url: string) => {
  window.open(url)
}

type Static = {
  connection: number
  scan: number
  import: number
}

const msg = ref<string>()

request("/static").then((res: Static) => {
  msg.value = `在过去1小时内，共发生${res.connection}次连接。成功登录${res.scan}次，成功导入${res.import}次。`
})

</script>

<template>
  <div id="container" class="container">
    <v-progress-linear v-model="progress"></v-progress-linear>
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <keep-alive>
          <component :is="Component" :key="Math.random()"/>
        </keep-alive>
      </transition>
    </router-view>

    <v-layout class="footer">
      <v-footer
          class="text-center d-flex flex-column"
      >
        <div>
          <v-btn @click="open(icons[key])"
                 v-for="key in Object.keys(icons)"
                 :key="icons[key]"
                 :icon="key"
                 class="mx-4"
                 variant="text"
          ></v-btn>
        </div>
        <div class="pt-0">
          {{ msg }}
        </div>

        <v-divider></v-divider>

        <div>
          {{ new Date().getFullYear() }} — <strong>kagg886</strong>
        </div>
      </v-footer>
    </v-layout>
  </div>
</template>

<style scoped>
.container {
  position: relative;
  width: 90%;
  height: 90%;
  border: 2px solid #ccc;
  border-radius: 10px;
  overflow: hidden;

  display: flex;
  flex-direction: column;
}

.footer {
  width: 100%;
  position: absolute;
  bottom: 0;
}

</style>
