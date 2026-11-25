import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { proxyPaths } from './proxy-paths.js';

export default defineConfig({
	plugins: [react()],
	server: {
		proxy: Object.fromEntries(
			proxyPaths.map((path) => [
				path,
				{
					target: process.env.DEMO_API_TARGET || 'http://127.0.0.1:8080',
					changeOrigin: true,
				},
			]),
		),
	},
	test: { environment: 'jsdom', setupFiles: './src/test-setup.js', restoreMocks: true },
});
