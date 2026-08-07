import { defineConfig } from 'vite'; import vue from '@vitejs/plugin-vue';
export default defineConfig({plugins:[vue()],server:{port:Number(process.env.VITE_DEV_PORT||5177),proxy:{'/api':process.env.VITE_API_PROXY_TARGET||'http://localhost:8084'}}});
