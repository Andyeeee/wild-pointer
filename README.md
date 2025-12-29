# 🚀 Wild Pointer (野指针)

> "Traveling through hyperspace ain't like dusting crops, boy." — Han Solo

![Java](https://img.shields.io/badge/Java-Spring%20Boot-green) ![Vue](https://img.shields.io/badge/Vue.js-2.x-42b883) ![License](https://img.shields.io/badge/License-MIT-blue)

**Wild Pointer** 是一个为探索者设计的随机导航工具。

作为《世界迷雾 (Fog of World)》的玩家或电车车主，你是否厌倦了每天重复的通勤路线？该项目旨在通过生成指定半径内的**随机坐标**，利用算法帮你打破“回音室效应”，探索城市中从未踏足的角落。

**Wild Pointer** is a random navigation tool designed for explorers. It generates random coordinates within a specified radius to help you break out of your routine and explore the unknown parts of your city.

## ✨ Features (功能特性)

- 🎯 **Hyperdrive Jump**: 设定最小/最大探索半径（例如 10km - 50km），一键生成随机目的地。
- 🗺️ **Auto Navigation**: 自动唤起高德地图/百度地图 App，并进入**路径规划**模式。
- 📱 **Mobile First**: 专为手机浏览器适配，支持 iOS/Android 通用链接 (Universal Link)。
- 🔒 **Privacy Focused**: 纯数学计算，无后台追踪，只记录你的探索精神。

## 🛠️ Tech Stack (技术栈)

- **Backend**: Java (Spring Boot 2.7)
  - 核心逻辑：极坐标转换算法、RESTful API
- **Frontend**: Vue.js 2
  - 交互：Geolocation API、AmAP URI Scheme
- **Deployment**: Nginx + Ubuntu Server (Self-signed SSL)

## 🚀 Quick Start (快速开始)

### Prerequisites (前置要求)
- JDK 1.8+
- Node.js & npm
- Maven

### 1. Backend (后端)

```bash
cd wild-pointer
# 运行 Spring Boot 服务
mvn spring-boot:run
# 服务将启动在 http://localhost:8080
2. Frontend (前端)
Bash

cd wild-pointer-web
# 安装依赖
npm install
# 开发模式运行
npm run serve
# 访问 http://localhost:8080 (注意修改 API 地址)
📦 Deployment (服务器部署)
本项目支持部署在 Linux 服务器 (Ubuntu/CentOS) 上。

Build:

后端: mvn clean package -> wild-pointer.jar

前端: npm run build -> dist/

Nginx Config: 由于现代浏览器要求 Geolocation API 必须在 HTTPS 环境下运行，建议配置 Nginx 反向代理并启用 SSL (自签名证书即可)。

Nginx

server {
    listen 443 ssl;
    # SSL 配置...

    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass [http://127.0.0.1:8080/api/](http://127.0.0.1:8080/api/);
    }
}
📝 Roadmap (未来计划)
[ ] Fog Integration: 导入 GPX 轨迹，利用 PostGIS 避开已探索区域。

[ ] EV Mode: 结合充电桩数据，防止随机到无充电设施的荒野。

[ ] Captain's Log: 记录每次探索的足迹和照片。

🤝 Contributing
欢迎提交 Issue 和 PR！让我们一起驱散迷雾。

Created by Andy with ❤️ & Java.
