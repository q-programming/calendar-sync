import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
    base: '/calendarsync',
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@api': path.resolve(__dirname, './src/api.ts'),
            // Note: '@api/*' wildcard alias handled by tsconfig for type-checking;
            // at runtime, '@api/X' resolves via the '@api' prefix above
        },
    },
    build: {
        commonjsOptions: {
            transformMixedEsModules: true,
            include: [/client-api/, /node_modules/],
        },
    },
    server: {
        proxy: {
            '/calendarsync/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    test: {
        fileParallelism: true,
        globals: true,
        testTimeout: 10000,
        environment: 'jsdom',
        setupFiles: ['src/test/setupBrowserTests.tsx'],
        browser: {
            enabled: true,
            headless: true,
            provider: 'playwright',
            screenshotFailures: false,
            instances: [
                {
                    browser: 'chromium',
                },
            ],
        },
        include: ['src/**/*.spec.{ts,tsx}'],
        reporters: ['default', ['junit', { outputFile: 'test-report.xml' }], 'verbose'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'cobertura', 'lcov'],
            exclude: [
                '**/*.js',
                '**/*.mjs',
                '**/*.mts',
                'src/test/**',
                '**/vite.config.ts',
                '**/main.tsx',
            ],
        },
    },
});
