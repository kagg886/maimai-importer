<script setup lang="ts">
//第一步：填写查分器账号
import {onMounted, ref} from "vue";
import {request} from "../util/network.ts";
import {ProtocolDescription} from "../util/types.ts";
import {useRouter} from "vue-router";

const server_config = ref<Array<ProtocolDescription>>()

onMounted(async () => {
  server_config.value = await request("config")
  console.log(server_config.value)
})

const currentProtocol = ref<ProtocolDescription>()
const protocolArgs = ref(new Map<string, any>())
const router = useRouter()

const open = ref(false)
const openConfigDialog = (config: ProtocolDescription) => {
  currentProtocol.value = config

  const map = new Map<string, any>()

  config.require.forEach((value) => {
    let def: any = ""
    if (value.type === 1) {
      def = [false, false, false, false, false]
    }
    map.set(value.name, def)
  })
  protocolArgs.value = map

  open.value = true
}

const generateConfig = () => {
  let config: {
    [key: string]: any,
  } = {}
  protocolArgs.value.forEach((value: any, key: string) => {
    if (value instanceof Array) {
      let arr = (value as Array<boolean>)
      let clo = []
      for (let i = 0; i < arr.length; i++) {
        if (arr[i]) {
          clo.push(i)
        }
      }
      config[key] = clo
      return
    }
    config[key] = value
  })
  //{"clazz":"top.kagg886.maimai.upload.DivingFishUploadProtocol$DivingFishUploadConfig","data":"{\"username\":\"root\",\"password\":\"123456\",\"diff\":[3,4]}"}

  config = {
    "clazz": currentProtocol.value?.className,
    "data": JSON.stringify(config),
  }
  //TODO 配置文件连接到服务器
  const pack = JSON.stringify(config)
  localStorage.setItem("config", pack)
  importNow()
}

const importNow = () => {
  router.push("/step2")
  open.value = false
  contain_config.value = false
}

const contain_config = ref(false)
if (localStorage.getItem("config") != null) {
  contain_config.value = true
}

const clearConfig = () => {
  localStorage.removeItem("config")
  contain_config.value = false
}
</script>

<template>
  <div>
    <div class="step1_container">
      <h1>连接到服务器</h1>

      <v-progress-circular indeterminate v-if="server_config===undefined"></v-progress-circular>
      <ul v-else>
        <li v-for="config in server_config">
          <v-card width="400">
            <template v-slot:title>
              {{ config.name }}
            </template>

            <template v-slot:subtitle>
              <a :href="config.url" target="_blank">{{ config.url }}</a>
            </template>

            <template v-slot:actions>
              <v-btn @click="openConfigDialog(config)">就决定是你了!</v-btn>
            </template>
          </v-card>
        </li>
      </ul>
    </div>

    <v-dialog persistent
              v-model="contain_config"
              width="auto">
      <v-card max-width="400">
        <template v-slot:title>您似乎之前使用过该工具</template>
        <template v-slot:subtitle>是否使用之前的设置导入数据?</template>
        <template v-slot:actions>
          <v-btn @click="clearConfig">否，并清除配置数据</v-btn>
          <v-btn @click="importNow">是，现在就开始导入</v-btn>
        </template>
      </v-card>
    </v-dialog>

    <v-dialog
        v-model="open"
        width="auto">
      <v-card
          max-width="400"
          prepend-icon="mdi-update"
          :title="`配置${currentProtocol!!.name}`">

        <template v-slot:text>
          <ul>
            <li v-for="form in currentProtocol?.require">
              <v-text-field v-if="form.type === 0"
                            :model-value="protocolArgs.get(form.name)"
                            @update:model-value="(v) => {
                            protocolArgs.set(form.name,v)
                          }"
                            :label="form.name">
              </v-text-field>
              <v-container fluid v-if="form.type === 1">
                <v-card-text>{{ form.name }}</v-card-text>
                <v-checkbox label="Basic"
                            :value="true"
                            :model-value="protocolArgs.get(form.name)[0]"
                            @update:model-value="(v) => {
                            protocolArgs.get(form.name)[0] = v
                          }"
                ></v-checkbox>
                <v-checkbox label="Advance"
                            :value="true"
                            :model-value="protocolArgs.get(form.name)[1]"
                            @update:model-value="(v) => {
                            protocolArgs.get(form.name)[1] = v
                          }"
                ></v-checkbox>
                <v-checkbox label="Expert"
                            :value="true"
                            :model-value="protocolArgs.get(form.name)[2]"
                            @update:model-value="(v) => {
                            protocolArgs.get(form.name)[2] = v
                          }"
                ></v-checkbox>
                <v-checkbox label="Master"
                            :value="true"
                            :model-value="protocolArgs.get(form.name)[3]"
                            @update:model-value="(v) => {
                            protocolArgs.get(form.name)[3] = v
                          }"
                ></v-checkbox>
                <v-checkbox label="Re:Master"
                            :value="true"
                            :model-value="protocolArgs.get(form.name)[4]"
                            @update:model-value="(v) => {
                            protocolArgs.get(form.name)[4] = v
                          }"
                ></v-checkbox>
              </v-container>
            </li>
          </ul>
        </template>

        <template v-slot:actions>
          <v-btn
              class="ms-auto"
              text="确定"
              @click="generateConfig"
          ></v-btn>
          <v-btn
              class="ms-auto"
              text="取消"
              @click="open = false"
          ></v-btn>
        </template>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.step1_container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

h1 {
  margin-top: 3%;
}

ul {
  list-style: none;
}
</style>