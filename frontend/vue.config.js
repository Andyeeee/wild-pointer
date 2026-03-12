const { defineConfig } = require("@vue/cli-service");

// 从环境变量读取API服务器地址，默认localhost:8082
const apiUrl = process.env.VUE_APP_API_URL || "http://localhost:8082";

module.exports = defineConfig({
  transpileDependencies: true,

  // 核心配置在这里
  devServer: {
    proxy: {
      "/api": {
        target: apiUrl,
        changeOrigin: true,
        pathRewrite: {
          // 如果你的后端接口本身就是 /api 开头，这里就不需要 pathRewrite
          // 如果后端是 /generate 但前端请求 /api/generate，这里就需要重写
          // 根据你之前的代码，后端 Controller 是 @RequestMapping("/api")，所以这里不用重写
        },
      },
    },
  },
});
