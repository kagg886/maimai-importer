import {createApp} from 'vue'
import './style.css'
import App from './App.vue'

import 'vuetify/styles'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import Step0 from "./step/Step0.vue";
import * as VueRouter from 'vue-router'
import Step1 from "./step/Step1.vue";
import {aliases, mdi} from 'vuetify/iconsets/mdi'
import '@mdi/font/css/materialdesignicons.css'
import Step2 from "./step/Step2.vue";
import {VFab} from 'vuetify/labs/VFab'

const vuetify = createVuetify({
    components: {
        ...components,
        VFab
    },
    directives,
    icons: {
        defaultSet: 'mdi',
        aliases,
        sets: {
            mdi,
        },
    },

})

const routes = [
    {path: '/', component: Step0},
    {path: '/step1', component: Step1},
    {path: '/step2', component: Step2, name: 'step2'},
]

const router = VueRouter.createRouter({
    history: VueRouter.createWebHashHistory(),
    routes,
})

createApp(App).use(vuetify).use(router).mount('#app')
