const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
    transpileDependencies: true,

    // 核心配置在这里
    devServer: {
        // 你的后端服务地址 (Spring Boot 默认是 8080)
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                pathRewrite: {
                    // 如果你的后端接口本身就是 /api 开头，这里就不需要 pathRewrite
                    // 如果后端是 /generate 但前端请求 /api/generate，这里就需要重写
                    // 根据你之前的代码，后端 Controller 是 @RequestMapping("/api")，所以这里不用重写
                }
            }
        }
    }
})