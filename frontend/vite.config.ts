import { defineConfig } from 'vite'; import vue from '@vitejs/plugin-vue';
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [vue()],
  server: {
    // Listen on the LAN interface so the dev server is reachable from other devices.
    host: '0.0.0.0',
    port: Number(process.env.VITE_DEV_PORT || 5177),
    // Vite accepts IP addresses by default, but sslip.io hostnames need to be
    // explicitly allowlisted to pass the development-server host check.
    allowedHosts: ['192-168-1-201.sslip.io'],
    proxy: {
      '/api': apiProxyTarget,
      '/oauth2': apiProxyTarget,
      '/login': apiProxyTarget,
      // Official catalog images are static resources served by Spring Boot.
      // Proxy them in development too, otherwise Vite returns its own 404.
      '/weapons': apiProxyTarget,
      '/armors': apiProxyTarget,
      '/physical-shields': apiProxyTarget,
    },
  },
});
