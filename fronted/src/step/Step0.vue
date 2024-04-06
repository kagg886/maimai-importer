<script setup lang="ts">
//主页
import DescriptionCard from "../componments/DescriptionCard.vue";
import {useRouter} from "vue-router";

const router = useRouter()

const goStep1 = () => {
  router.push("/step1");
}
</script>

<template>
  <div class="step0_container">
    <h1>舞萌导分工具</h1>

    <ul>
      <li>
        <description-card title="无代理模式" details="您完全不需要设置代理服务器，只需要扫码，然后等待即可"/>
      </li>
      <li>
        <description-card title="后台导入" details="只要扫码成功，导入工作便不会停止。就算关闭网页重新打开，你也可以随时获得最新进度"/>
      </li>
    </ul>

    <v-btn @click="goStep1" size="large">让我们开始!</v-btn>
  </div>
</template>

<style scoped>
.step0_container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

h1 {
  margin-top: 3%;
}

ul {
  list-style: none;
  margin-top: 30px;
  display: flex;
  flex-direction: row;
  width: 60%;
  justify-content: space-around;
  flex-wrap: wrap;
}
</style>