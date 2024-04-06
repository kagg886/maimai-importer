<script setup lang="ts">
withDefaults(
    defineProps<{
      title: string
      description: string
      details: string
    }>(), {
      description: ""
    }
)
</script>

<template>
  <v-card
      class="mx-auto my-8"
      hover
      max-width="200"
      min-height="200"
  >
    <v-card-item>
      <v-card-title>
        {{ title }}
      </v-card-title>
      <v-card-subtitle>
        {{ description }}
      </v-card-subtitle>
    </v-card-item>

    <v-card-text>
      {{ details }}
    </v-card-text>
  </v-card>
</template>

<style scoped>

</style>