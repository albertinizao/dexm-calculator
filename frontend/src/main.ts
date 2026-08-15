import {createApp} from 'vue';
import {createRouter, createWebHistory} from 'vue-router';
import App from './App.vue';
import './styles.css';
import { logClientError } from './services/errors';

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

const app = createApp(App);
app.config.errorHandler = (error, instance, info) => logClientError(error, { source: 'vue', info, component: instance?.$options?.name });
router.onError((error, to, from) => logClientError(error, { source: 'router', to: to.fullPath.split('?')[0], from: from.fullPath.split('?')[0] }));
window.addEventListener('error', event => logClientError(event.error || new Error(event.message), { source: 'window', path: window.location.pathname }));
window.addEventListener('unhandledrejection', event => logClientError(event.reason, { source: 'unhandledrejection', path: window.location.pathname }));
app.use(router).mount('#app');
