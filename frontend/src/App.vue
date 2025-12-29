<template>
  <div id="app">
    <div class="container">
      <h1>🚀 Wild Pointer</h1>
      <p class="subtitle">未知的路，才是最迷人的。</p>

      <div class="card">
        <div class="input-group">
          <label>最小半径 (km)</label>
          <input type="number" v-model.number="minRadius" />
        </div>
        <div class="input-group">
          <label>最大半径 (km)</label>
          <input type="number" v-model.number="maxRadius" />
        </div>

        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

        <button
            @click="startExploration"
            :disabled="loading"
            class="jump-btn"
        >
          {{ loading ? '计算航线中...' : '启动超空间引擎' }}
        </button>
      </div>

      <div v-if="result" class="result-box">
        <p>🎯 目标锁定</p>
        <p>距离: {{ result.distance }} km</p>
        <p>方向: {{ result.angle }}°</p>
        <button @click="openMap" class="nav-btn">打开高德地图出发</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'App',
  data() {
    return {
      minRadius: 10,
      maxRadius: 50,
      loading: false,
      errorMsg: '',
      result: null,
      currentLoc: null
    };
  },
  methods: {
    startExploration() {
      this.loading = true;
      this.errorMsg = '';
      this.result = null;

      // 1. 获取浏览器定位
      if (!navigator.geolocation) {
        this.errorMsg = "你的浏览器不支持地理定位";
        this.loading = false;
        return;
      }

      navigator.geolocation.getCurrentPosition(
          (position) => {
            this.currentLoc = {
              lat: position.coords.latitude,
              lon: position.coords.longitude
            };
            // 2. 拿到定位后，请求后端
            this.fetchRandomPoint();
          },
          (err) => {
            this.loading = false;
            console.error(err);
            this.errorMsg = "无法获取定位，请确保允许定位权限。";
          }
      );
    },

    fetchRandomPoint() {
      // ⚠️注意：如果你是用手机访问电脑，这里不能写 localhost，要写你电脑的局域网IP (如 192.168.1.x)
      // 如果只是电脑浏览器测试，用 localhost 没问题
      const API_URL = '/api/generate';

      axios.get(API_URL, {
        params: {
          lat: this.currentLoc.lat,
          lon: this.currentLoc.lon,
          minRadius: this.minRadius,
          maxRadius: this.maxRadius
        }
      })
          .then(res => {
            this.result = res.data;
          })
          .catch(err => {
            this.errorMsg = "连接后端失败: " + err.message;
          })
          .finally(() => {
            this.loading = false;
          });
    },

    openMap() {
      if (!this.result) return;

      const lat = this.result.destLat;
      const lon = this.result.destLon;
      const destName = "未知探索点"; // 目的地名称

      // 判断设备类型
      const u = navigator.userAgent;
      const isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/);
      const isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1;

      let url = "";

      if (isAndroid) {
        // Android: 使用 route 协议 (路径规划)
        // t=0 代表驾车
        url = `androidamap://route?sourceApplication=WildPointer&dlat=${lat}&dlon=${lon}&dname=${destName}&dev=0&t=0`;
      } else if (isiOS) {
        // iOS: 使用 path 协议 (路径规划)
        url = `iosamap://path?sourceApplication=WildPointer&dlat=${lat}&dlon=${lon}&dname=${destName}&dev=0&t=0`;
      } else {
        // 网页版/电脑版通用回退
        // 这里的 callnative=1 会尝试唤起 App 的路线详情页
        url = `https://uri.amap.com/navigation?to=${lon},${lat},${destName}&mode=car&callnative=1`;
      }

      console.log("Opening Route Plan:", url);
      window.location.href = url;
    }
  }
};
</script>

<style>
/* 简单写点样式，让它看起来像个仪表盘 */
body {
  background-color: #1a1a1a;
  color: #fff;
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  margin: 0;
}
.container {
  max-width: 400px;
  margin: 0 auto;
  padding: 40px 20px;
  text-align: center;
}
h1 { color: #42b983; }
.subtitle { color: #888; font-size: 0.9em; margin-bottom: 30px; }
.card {
  background: #2c3e50;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
}
.input-group { margin-bottom: 15px; text-align: left; }
.input-group label { display: block; font-size: 0.8em; color: #aaa; margin-bottom: 5px; }
.input-group input {
  width: 100%; box-sizing: border-box; padding: 10px;
  background: #1a1a1a; border: 1px solid #444; color: #fff; border-radius: 6px;
}
.jump-btn {
  width: 100%; padding: 15px; margin-top: 10px;
  background: linear-gradient(45deg, #42b983, #2c3e50);
  border: none; color: white; font-weight: bold; border-radius: 6px; cursor: pointer;
}
.jump-btn:disabled { opacity: 0.6; }
.result-box { margin-top: 30px; animation: fadeIn 0.5s; }
.nav-btn {
  background: #e67e22; color: white; border: none; padding: 10px 20px;
  border-radius: 20px; font-weight: bold; margin-top: 10px; cursor: pointer;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.error { color: #ff6b6b; font-size: 0.8em; }
</style>