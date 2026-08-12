import {createApp} from 'vue';
import {createRouter, createWebHistory} from 'vue-router';
import App from './App.vue';
import './styles.css';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: App },
    { path: '/characters/:id', name: 'character-sheet', component: App, meta: { section: 'sheet' } },
    { path: '/characters/:id/abilities', name: 'character-abilities', component: App, meta: { section: 'abilities' } },
    { path: '/characters/:id/inventory', name: 'character-inventory', component: App, meta: { section: 'inventory' } },
    { path: '/characters/:id/history', name: 'character-history', component: App, meta: { section: 'sheet', history: true } },
    { path: '/characters/:id/:pathMatch(.*)*', redirect: to => ({ name: 'character-sheet', params: { id: String(to.params.id) } }) },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

createApp(App).use(router).mount('#app');
