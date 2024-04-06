<script setup lang="ts">
import {useRouter} from "vue-router";
import {inject, ref} from "vue";
import {createSocket, LogMessage, RequireMessage} from "../util/network.ts";

const router = useRouter()
const config = sessionStorage.getItem("config") as string

console.log("接收的step1配置文件", config)


if (config == null) {
  router.push("/")
}

const calcIcon = (level: number) => {
  let s = ""
  switch (level) {
    case 1:
      s = "mdi-information"
      break
    case 2:
      s = "mdi-alert"
      break
    case 3:
      s = "mdi-skull"
      break
  }
  return s
}
const calcClass = (level: number) => {
  let s = ""
  switch (level) {
    case 1:
      s = "info"
      break
    case 2:
      s = "warning"
      break
    case 3:
      s = "error"
      break
  }
  return s
}

const loggerList = ref<Array<LogMessage>>([])

loggerList.value.push({
  level: 1,
  msg: "qwq",
  time: new Date()
})
loggerList.value.push({
  level: 1,
  msg: "qwq".repeat(100),
  time: new Date()
})
// loggerList.value.push({
//   level: 2,
//   msg: "qwq",
//   time: new Date()
// })
// loggerList.value.push({
//   level: 3,
//   msg: "qwq",
//   time: new Date()
// })

// for (let i = 0; i < 100; i++) {
//   loggerList.value.push({
//     level: 1,
//     msg: "qwq",
//     time: new Date()
//   })
// }


// setInterval(() => {
//   loggerList.value.push({
//     level: 1,
//     msg: "qwq",
//     time: new Date()
//   })
// },1000)

const height = inject('container_height') as number

const show = ref(false)
const url = ref('')

const socket = createSocket({
  onLogger: (log: LogMessage) => {
    loggerList.value.push(log)
  },
  onImageGenerated: (url0: string) => {
    if (url0 === "") {
      show.value = false
      return
    }
    url.value = url0
    show.value = true
  },
  onConfigWantToGet: (content: RequireMessage["msg"]) => {
    switch (content) {
      case "PROTOCOL_CONFIG":
        socket.send(config) //发送配置文件
        break
    }
  }
})

socket.onclose = (ev: CloseEvent) => {
  loggerList.value.push({
    level: 1,
    msg: `WebSocket服务已断开,标识码:${ev.code},原因:${ev.reason}`,
    time: new Date()
  })
}

const log = ref(false)
const msg = ref("")
const showLog = (msg0: string) => {
  log.value = true
  msg.value = msg0
}

// show.value = true
// setTimeout(() => {
//   show.value = false
// },3000)
</script>

<template>
  <div>
    <v-dialog v-model="log" width="auto">
      <v-card max-width="400">
        <template v-slot:title>日志详情</template>
        <template v-slot:subtitle>按ESC以关闭</template>
        <v-card-text>{{ msg }}</v-card-text>
      </v-card>
    </v-dialog>
    <v-virtual-scroll
        :height="height"
        :items="loggerList"
    >
      <template v-slot:default="{ item }">
        <v-list-item
            @click="showLog(item.msg)"
            :value="item"
            :active="false"
            class="enter"
            color="primary">

          <template v-slot:prepend>
            <v-icon :icon="calcIcon(item.level)"></v-icon>
          </template>

          <template v-slot:append>
            <v-card-text>{{ item.time.getHours() }}：{{ item.time.getMinutes() }}</v-card-text>
          </template>

          <v-list-item-title :class="calcClass(item.level)"
                             v-text="item.msg.substring(1,Math.min(item.msg.length,25))"></v-list-item-title>
        </v-list-item>
      </template>
    </v-virtual-scroll>

    <v-dialog
        persistent
        v-model="show"
        width="auto"
    >
      <v-card
          max-width="400"
          prepend-icon="mdi-update"
          subtitle="请使用微信扫描二维码"
          title="扫描二维码"
      >
        <template v-slot:text>
          <v-img
              :width="300"
              :height="300"
              :src="url"
          ></v-img>
        </template>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.enter {
  animation: ss 0.5s;
}

@keyframes ss {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateY(0);
  }
}

.info {
  color: green;
}

.warning {
  color: orange;
}

.error {
  color: red;
}
</style>