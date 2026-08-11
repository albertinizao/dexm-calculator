import { defineConfig } from 'vite'; import vue from '@vitejs/plugin-vue';
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8084';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: Number(process.env.VITE_DEV_PORT || 5177),
    proxy: {
      '/api': apiProxyTarget,
      '/oauth2': apiProxyTarget,
      '/login': apiProxyTarget,
    },
  },
});
