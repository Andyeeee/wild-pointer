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
              {{ currentAddress || '正在初始化卫星链路...' }}
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
import AMapLoader from '@amap/amap-jsapi-loader';

// 🔥🔥🔥 请在此处填入你的高德 Key 和 安全密钥 🔥🔥🔥
// Web端(JSAPI) Key
const AMAP_KEY = process.env.VUE_APP_AMAP_KEY;
const AMAP_SECURITY_CODE = process.env.VUE_APP_AMAP_SECURITY_CODE;

export default {
  name: 'App',
  data() {
    return {
      minRadius: 10,
      maxRadius: 50,
      currentLoc: null,
      currentAddress: '',
      isLocating: false,
      locError: '',
      loading: false,
      apiError: '',
      result: null,

      // 高德相关对象
      geocoder: null, // 逆地理编码插件实例
    };
  },
  mounted() {
    // 1. 配置安全密钥 (必须在加载 loader 之前)
    window._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE,
    };

    // 2. 初始化高德 API
    this.initAMap();
  },
  methods: {
    // --- 初始化高德地图资源 ---
    initAMap() {
      this.currentAddress = "正在加载地图资源...";

      AMapLoader.load({
        key: AMAP_KEY,
        version: "2.0",
        plugins: ['AMap.Geocoder'] // 🔥 重点：加载逆地理编码插件
      })
          .then((AMap) => {
            // 初始化 Geocoder
            this.geocoder = new AMap.Geocoder({
              city: "全国", // 范围
              radius: 1000  // 搜索半径
            });

            // 资源加载完毕后，开始定位
            this.refreshLocation();
          })
          .catch((e) => {
            console.error(e);
            this.locError = "地图资源加载失败，请检查 Key";
          });
    },

    // --- 核心定位逻辑 ---
    refreshLocation() {
      this.isLocating = true;
      this.locError = '';
      this.currentAddress = "正在校准坐标...";

      if (!navigator.geolocation) {
        this.locError = "浏览器不支持定位";
        return;
      }

      navigator.geolocation.getCurrentPosition(
          (position) => {
            this.currentLoc = {
              lat: position.coords.latitude,
              lon: position.coords.longitude
            };

            // 🔥 拿到坐标后，调用高德解析地址
            this.getAmapAddress(this.currentLoc.lat, this.currentLoc.lon);
          },
          (err) => {
            this.isLocating = false;
            this.currentAddress = "定位失败";
            this.locError = err.message;
          },
          { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
      );
    },

    // --- 🔥 高德逆地理编码 (替代 OpenStreetMap) ---
    getAmapAddress(lat, lon) {
      if (!this.geocoder) {
        this.currentAddress = "地图组件未就绪";
        this.isLocating = false;
        return;
      }

      // 注意高德参数顺序是 [经度, 纬度] (lon, lat)
      this.geocoder.getAddress([lon, lat], (status, result) => {
        this.isLocating = false; // 停止转圈

        if (status === 'complete' && result.regeocode) {
          // formattedAddress 是高德拼接好的标准地址：xx省xx市xx区xx路xx号
          this.currentAddress = result.regeocode.formattedAddress;
        } else {
          this.currentAddress = "未知荒野";
          console.error('地址解析失败:', result);
        }
      });
    },

    // --- 业务逻辑 ---
    startExploration() {
      this.loading = true;
      this.apiError = '';
      this.result = null;

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
            this.apiError = "连接失败: " + err.message;
          })
          .finally(() => {
            this.loading = false;
          });
    },

    openMap() {
      if (!this.result) return;
      const { destLat, destLon } = this.result;
      const u = navigator.userAgent;
      const isMobile = !!u.match(/Android|iPhone/i);

      // 电脑端
      const pcUrl = `https://uri.amap.com/marker?position=${destLon},${destLat}&name=神秘目的地&callnative=0`;

      // 手机端
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
/* 保持你的暗黑极客风格 */
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

/* 定位卡片 */
.location-card {
  background: #34495e;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
  text-align: left;
  font-size: 0.9rem;
  border: 1px solid #465c71;
  box-shadow: 0 4px 6px rgba(0,0,0,0.2);
}
.loc-row { display: flex; align-items: center; margin-bottom: 8px; }
.address-row { justify-content: space-between; margin-bottom: 0; }
.address-wrapper { display: flex; align-items: center; overflow: hidden; }
.loc-label { color: #bdc3c7; font-weight: bold; margin-right: 10px; flex-shrink: 0; }
.loc-value { color: #fff; }
.mono { font-family: monospace; color: #42b983; }
.address-text {
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 200px; display: block;
}
.loc-error-text { color: #e74c3c; font-size: 0.8rem; margin: 5px 0 0 0; }

/* 刷新按钮 */
.refresh-btn {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid #5d6d7e; background: #2c3e50; color: white;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  padding: 0; transition: all 0.2s; margin-left: 10px; flex-shrink: 0;
}
.refresh-btn:hover { background: #42b983; border-color: #42b983; }
.refresh-btn:disabled { opacity: 0.6; cursor: wait; }
.spinning { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 主卡片 */
.card {
  background: #2c3e50; padding: 20px; border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
}
.input-group { margin-bottom: 15px; text-align: left; }
.input-group label { display: block; font-size: 0.85em; color: #bdc3c7; margin-bottom: 5px; }
.input-group input {
  width: 100%; box-sizing: border-box; padding: 12px;
  background: #1a1a1a; border: 1px solid #465c71; color: #fff;
  border-radius: 8px; font-size: 1rem;
}
.input-group input:focus { outline: none; border-color: #42b983; }

.jump-btn {
  width: 100%; padding: 15px; margin-top: 10px;
  background: linear-gradient(135deg, #42b983 0%, #3aa876 100%);
  border: none; color: white; font-weight: bold; border-radius: 8px;
  cursor: pointer; font-size: 1rem; transition: transform 0.1s;
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