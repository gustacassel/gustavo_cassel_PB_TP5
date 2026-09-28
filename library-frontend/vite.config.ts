/// <reference types="vitest/config" />
import plugin from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const gateway = process.env.GATEWAY_URL ?? "http://localhost:8000";

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [plugin()],
    server: {
        port: 5173,
        host: true,
        // em desenvolvimento o Vite repassa as chamadas de API para o gateway
        proxy: {
            "/library-api": gateway,
            "/students-api": gateway,
        },
    },
    test: {
        environment: "jsdom",
        setupFiles: "./src/test/setup.ts",
    },
})
