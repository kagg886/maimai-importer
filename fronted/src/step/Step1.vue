<script setup lang="ts">

import {onMounted, ref} from "vue";
import {request} from "../util/network.ts";
import {ProtocolDescription} from "../util/types.ts";

const server_config = ref<Array<ProtocolDescription>>()

onMounted(async () => {
  server_config.value = await request("config")
  console.log(server_config.value)
})

const currentProtocol = ref<ProtocolDescription>()
const protocolArgs = ref(new Map<string, any>())

const open = ref(false)
const openConfigDialog = (config: ProtocolDescription) => {
  currentProtocol.value = config

  const map = new Map<string, any>()

  config.require.forEach((value) => {
    let def: any = ""
    if (value.type === 1) {
      def = [false,false,false,false,false]
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
  console.log(JSON.stringify(config))
}
</script>

<template>
  <div class="step1_container">
    <h1>连接到服务器</h1>

    <v-progress-circular indeterminate v-if="server_config===undefined"></v-progress-circular>
    <ul>
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
                          :v-model="protocolArgs.get(form.name)!!"
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