<template>
  <div id="app">
    <div class="container">
      <h1>🚀 Wild Pointer</h1>
      <p class="subtitle">未知的路，才是最迷人的。</p>

      <div class="location-card">
        <div class="loc-row">
          <span class="loc-label">当前坐标:</span>
          <span class="loc-value mono" v-if="currentLoc">
            {{ currentLoc.lat.toFixed(4) }}, {{ currentLoc.lon.toFixed(4) }}
          </span>
          <span class="loc-value" v-else>--</span>
        </div>

        <div class="loc-row address-row">
          <div class="address-wrapper">
            <span class="loc-label">当前位置:</span>
            <span class="loc-value address-text">
              {{ currentAddress || '等待定位卫星信号...' }}
            </span>
          </div>

          <button
              class="refresh-btn"
              @click="refreshLocation"
              :disabled="isLocating"
              title="刷新定位"
          >
            <span v-if="isLocating" class="spinning">⟳</span>
            <span v-else>📍</span>
          </button>
        </div>

        <p v-if="locError" class="loc-error-text">{{ locError }}</p>
      </div>
      <div class="card">
        <div class="input-group">
          <label>最小半径 (km)</label>
          <input type="number" v-model.number="minRadius" />
        </div>
        <div class="input-group">
          <label>最大半径 (km)</label>
          <input type="number" v-model.number="maxRadius" />
        </div>

        <p v-if="apiError" class="error">{{ apiError }}</p>

        <button
            @click="startExploration"
            :disabled="loading || !currentLoc"
            class="jump-btn"
        >
          {{ loading ? '正在计算航线...' : '启动超空间引擎' }}
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

      // 定位相关状态
      currentLoc: null,       // { lat, lon }
      currentAddress: '',     // 中文地址字符串
      isLocating: false,      // 定位中状态
      locError: '',           // 定位错误信息

      // 业务相关状态
      loading: false,         // 接口请求中状态
      apiError: '',           // 接口错误信息
      result: null            // 后端返回的结果
    };
  },
  mounted() {
    // 页面加载完毕，自动触发一次定位
    this.refreshLocation();
  },
  methods: {
    // --- 1. 核心定位方法 (独立出来) ---
    refreshLocation() {
      this.isLocating = true;
      this.locError = '';
      this.currentAddress = "正在校准坐标...";

      if (!navigator.geolocation) {
        this.locError = "浏览器不支持地理定位";
        this.isLocating = false;
        return;
      }

      // 定义成功的回调函数（复用）
      const handleSuccess = (position) => {
        console.log("定位成功:", position);
        this.currentLoc = {
          lat: position.coords.latitude,
          lon: position.coords.longitude
        };
        // 调用逆地理编码
        this.getAddress(this.currentLoc.lat, this.currentLoc.lon);
      };

      // 定义最终失败的回调函数
      const handleError = (err) => {
        this.isLocating = false;
        this.currentAddress = "定位失败";
        switch (err.code) {
          case err.PERMISSION_DENIED: this.locError = "请开启位置权限"; break;
          case err.POSITION_UNAVAILABLE: this.locError = "无法获取位置信息"; break;
          case err.TIMEOUT: this.locError = "定位请求超时"; break;
          default: this.locError = err.message;
        }
      };

      // 🛑 策略 1：先尝试高精度 (手机 GPS / 电脑 WiFi)
      // 设置较短的超时时间 (5秒)，如果不刑立刻降级
      navigator.geolocation.getCurrentPosition(
          handleSuccess,
          (err) => {
            console.warn("高精度定位失败，尝试低精度模式...", err.message);

            // ⚠️ 策略 2：降级到低精度 (IP 定位)
            // 这种模式对电脑最友好，几乎必定成功
            navigator.geolocation.getCurrentPosition(
                handleSuccess,
                handleError,
                {
                  enableHighAccuracy: false, // 关键：允许低精度
                  timeout: 10000,            // 给更多时间
                  maximumAge: 0              // 依然禁止缓存，防止"飞回日本"
                }
            );
          },
          {
            enableHighAccuracy: true,
            timeout: 5000, // 5秒定不到位就放弃高精度
            maximumAge: 0
          }
      );
    },

    // --- 2. 逆地理编码 (OpenStreetMap 免费接口) ---
    async getAddress(lat, lon) {
      try {
        // 使用 fetch 调用免费接口
        const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1&accept-language=zh-CN`;

        const response = await fetch(url);
        const data = await response.json();

        if (data && data.address) {
          // 智能拼接地址：优先取城市/区，如果没有则取兜底字段
          const city = data.address.city || data.address.town || data.address.county || '';
          const district = data.address.district || data.address.suburb || '';
          const road = data.address.road || '';

          if (city || district) {
            this.currentAddress = `${city} ${district} ${road}`;
          } else {
            this.currentAddress = data.display_name.split(',')[0]; // 兜底
          }
        } else {
          this.currentAddress = "未知荒野";
        }
      } catch (e) {
        console.error(e);
        this.currentAddress = "地址解析超时";
      } finally {
        this.isLocating = false; // 无论成功失败，停止转圈
      }
    },

    // --- 3. 业务逻辑：启动引擎 ---
    startExploration() {
      if (!this.currentLoc) {
        this.apiError = "请先点击上方按钮获取定位";
        return;
      }

      this.loading = true;
      this.apiError = '';
      this.result = null;

      // 直接使用 currentLoc，不需要再重新定位了
      this.fetchRandomPoint();
    },

    fetchRandomPoint() {
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
            this.apiError = "连接后端失败: " + err.message;
          })
          .finally(() => {
            this.loading = false;
          });
    },

    openMap() {
      if (!this.result) return;
      const { destLat, destLon } = this.result;
      const u = navigator.userAgent;
      const isMobile = !!u.match(/Android|iPhone|iPad|iPod/i);

      // 电脑端：标点链接
      const pcUrl = `https://uri.amap.com/marker?position=${destLon},${destLat}&name=神秘目的地&callnative=0`;

      // 手机端：导航链接
      let mobileUrl = "";
      if (u.indexOf('Android') > -1) {
        mobileUrl = `androidamap://route?sourceApplication=WildPointer&dlat=${destLat}&dlon=${destLon}&dev=0&t=0`;
      } else {
        mobileUrl = `iosamap://path?sourceApplication=WildPointer&dlat=${destLat}&dlon=${destLon}&dev=0&t=0`;
      }

      if (!isMobile) {
        window.open(pcUrl, '_blank');
      } else {
        window.location.href = mobileUrl;
        setTimeout(() => {
          if (!document.hidden) window.location.href = pcUrl;
        }, 2500);
      }
    }
  }
};
</script>

<style>
/* 全局样式 */
body {
  background-color: #1a1a1a;
  color: #ecf0f1;
  font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  margin: 0;
}
.container {
  max-width: 400px;
  margin: 0 auto;
  padding: 40px 20px;
  text-align: center;
}
h1 { color: #42b983; margin-bottom: 5px; }
.subtitle { color: #7f8c8d; font-size: 0.9em; margin-bottom: 25px; }

/* 📍 新增：定位卡片样式 */
.location-card {
  background: #34495e; /* 比背景稍亮一点的深色 */
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
  text-align: left;
  font-size: 0.9rem;
  border: 1px solid #465c71;
  box-shadow: 0 4px 6px rgba(0,0,0,0.2);
}

.loc-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.address-row {
  justify-content: space-between; /* 文字靠左，按钮靠右 */
  margin-bottom: 0;
}

.address-wrapper {
  display: flex;
  align-items: center;
  overflow: hidden; /* 防止文字溢出 */
}

.loc-label {
  color: #bdc3c7;
  font-weight: bold;
  margin-right: 10px;
  flex-shrink: 0;
}

.loc-value {
  color: #fff;
}

.mono {
  font-family: monospace; /* 经纬度用等宽字体好看 */
  color: #42b983;
}

.address-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px; /* 限制地址长度 */
  display: block;
}

.loc-error-text {
  color: #e74c3c;
  font-size: 0.8rem;
  margin: 5px 0 0 0;
}

/* 刷新按钮样式 */
.refresh-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #5d6d7e;
  background: #2c3e50;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.2s;
  margin-left: 10px;
  flex-shrink: 0;
}

.refresh-btn:hover {
  background: #42b983;
  border-color: #42b983;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

/* 旋转动画 */
.spinning {
  display: inline-block;
  animation: spin 1s linear infinite;
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 主输入卡片 */
.card {
  background: #2c3e50;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
}

.input-group { margin-bottom: 15px; text-align: left; }
.input-group label { display: block; font-size: 0.85em; color: #bdc3c7; margin-bottom: 5px; }
.input-group input {
  width: 100%; box-sizing: border-box; padding: 12px;
  background: #1a1a1a; border: 1px solid #465c71; color: #fff; border-radius: 8px;
  font-size: 1rem;
}
.input-group input:focus { outline: none; border-color: #42b983; }

.jump-btn {
  width: 100%; padding: 15px; margin-top: 10px;
  background: linear-gradient(135deg, #42b983 0%, #3aa876 100%);
  border: none; color: white; font-weight: bold; border-radius: 8px; cursor: pointer;
  font-size: 1rem;
  transition: transform 0.1s;
}
.jump-btn:active { transform: scale(0.98); }
.jump-btn:disabled { background: #7f8c8d; cursor: not-allowed; }

.result-box { margin-top: 30px; animation: fadeIn 0.5s; }
.nav-btn {
  background: #e67e22; color: white; border: none; padding: 12px 25px;
  border-radius: 25px; font-weight: bold; margin-top: 15px; cursor: pointer;
  box-shadow: 0 4px 6px rgba(230, 126, 34, 0.3);
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.error { color: #ff6b6b; font-size: 0.9em; margin-top: 10px; }
</style>