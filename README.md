# 🚀 Wild Pointer (野指针)

---

> “Traveling through hyperspace ain’t like dusting crops, boy.” — Han Solo
> 

**Wild Pointer** 是一个为探索者设计的随机导航工具。

作为《世界迷雾 (Fog of World)》的玩家或电车车主，你是否厌倦了每天重复的通勤路线？该项目通过在指定半径内生成**随机坐标**，帮助你打破“回音室效应”，探索城市中从未踏足的角落。

**Wild Pointer** is a random navigation tool designed for explorers. It generates random coordinates within a specified radius to help you break out of your routine and explore the unknown parts of your city.

---

## ✨ Features / 功能特性

- 🎯 **Hyperdrive Jump**
    
    设定最小/最大探索半径（例如 10km–50km），一键生成随机目的地。
    
- 🗺️ **Auto Navigation**
    
    自动唤起高德地图 / 百度地图 App，并进入**路径规划**模式。
    
- 📱 **Mobile First**
    
    专为手机浏览器适配，支持 iOS / Android 通用链接 (Universal Link)。
    
- 🔒 **Privacy Focused**
    
    纯数学计算，无后台追踪，不采集位置信息。
    

---

## 🛠 Tech Stack / 技术栈

- **Backend**: Java (Spring Boot 2.7)
    - 核心逻辑：极坐标转换算法、RESTful API
- **Frontend**: Vue.js 2
    - 交互：Geolocation API、AMap URI Scheme
- **Deployment**: Nginx + Ubuntu Server (Self-signed SSL)

---

## 🚀 Quick Start / 快速开始

### 1. Prerequisites / 前置要求

- JDK 1.8+
- Node.js & npm
- Maven
- 已安装并配置好 Git（可选）

---

### 2. Backend / 后端

### 2.1 获取代码

```bash
git clone <your-repo-url>
cd wild-pointer
```

### 2.2 启动 Spring Boot 服务

```bash
# 在项目根目录下
mvn spring-boot:run
```

服务默认会启动在：

- 本地地址：[http://localhost:8080](http://localhost:8080)

如需修改端口或其他配置，请在 `application.yml` / [`application.properties`](http://application.properties) 中调整。

---

### 3. Frontend / 前端

### 3.1 进入前端项目

```bash
cd wild-pointer-web
```

### 3.2 安装依赖

```bash
npm install
```

### 3.3 开发环境运行

```bash
npm run serve
```

启动成功后，浏览器访问：

- 开发环境：[http://localhost:8080](http://localhost:8080)
    
    > 注意：请根据实际后端服务地址修改前端中的 API Base URL。
    > 

---

## 📦 Deployment / 服务器部署

本项目支持部署在 Linux 服务器（例如 Ubuntu / CentOS）上。

### 1. 后端打包

```bash
cd wild-pointer
mvn clean package
```

生成的可执行包（示例）：

- `target/wild-pointer.jar`

运行：

```bash
java -jar target/wild-pointer.jar
```

---

### 2. 前端构建

```bash
cd wild-pointer-web
npm install
npm run build
```

构建输出目录：

- `dist/`

---

### 3. Nginx 配置（HTTPS + 反向代理）

由于现代浏览器要求 Geolocation API 必须在 **HTTPS** 环境下运行，建议配置 Nginx 反向代理并启用 SSL（自签名证书即可用于自用工具）。

下面是一个简化示例配置（请根据你的域名和证书路径调整）：

```
server {
    listen 443 ssl;
    server_name [your-domain.com](http://your-domain.com);

    # SSL 证书配置（示例）
    ssl_certificate     /path/to/ssl/fullchain.pem;
    ssl_certificate_key /path/to/ssl/privkey.pem;

    # 前端静态资源
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass [http://http://127.0.0.1:8080/api/](http://http://127.0.0.1:8080/api/);
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

如果需要同时开放 80 端口做重定向，也可以加一个 `server` 块，将 HTTP 流量重定向到 HTTPS。

---

## 📝 开发路线图 (Roadmap)

### ✅ Phase 1: 界面重构与基础优化 (前端为主)
**目标**：把“双模式”的架子搭起来，解决“点在水里”的核心体验问题。

- [x] **UI 拆分**：改造 `App.vue`，增加 Tab 切换（随机/目的地）和 Switch 开关（破雾模式）。
- [x] **目的地搜索**：引入高德 `AutoComplete` 与 `PlaceSearch` 插件，实现地点搜索与定位功能。
- [x] **后端接口 v1**：改造 `/generate-random` 接口，增加“道路吸附”逻辑（调用高德逆地理编码 API 修正坐标）。
- [x] **中间点算法**：新增 `/generate-waypoint` 接口，为模式 B 提供“沿途探索”计算（基于向量插值的随机偏移算法）。

### 🚧 Phase 2: 数据基建 (后端为主)
**目标**：能够存取轨迹，为迷雾模式做准备。

- [ ] **用户系统**：实现简单的登录/注册接口 (JWT / Sa-Token)。
- [ ] **数据库设计**：创建支持 GIS 的 MySQL 表结构 (`GEOMETRY` 类型, `SRID 4326`)。
- [ ] **GPX 导入**：编写 `.gpx` 文件解析模块，实现轨迹抽稀算法并入库。

### 📅 Phase 3: 迷雾算法落地 (核心联调)
**目标**：完全实现“不去去过的地方”。

- [ ] **空间查询**：后端实现基于 `ST_Distance_Sphere` 的判重逻辑。
- [ ] **核心联调**：前端“破雾”开关真正生效，开启后生成的点确实避开了历史轨迹。
- [ ] **周边设施**：在前端展示终点附近的停车场、充电站、公厕等信息，缓解探索焦虑。

---

## 🤝 Contributing / 参与贡献

欢迎提交 Issue 和 PR，一起让更多人走出熟悉的路径，去探索城市的未知角落。

---

Created by Andy with ❤️ & Java.
