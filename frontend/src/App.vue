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

      <div class="map-wrapper">
        <div id="amap-container"></div>

        <div v-if="result" class="result-overlay">
          <span>📏 {{ result.distance }} km</span>
          <span>🧭 {{ result.angle }}°</span>
        </div>
      </div>

      <div class="card control-panel">
        <div class="input-row">
          <div class="input-group">
            <label>最小 (km)</label>
            <input type="number" v-model.number="minRadius" />
          </div>
          <div class="input-group">
            <label>最大 (km)</label>
            <input type="number" v-model.number="maxRadius" />
          </div>
        </div>

        <p v-if="apiError" class="error">{{ apiError }}</p>

        <button
            @click="startExploration"
            :disabled="loading || !currentLoc"
            class="jump-btn"
        >
          {{ loading ? '正在规划路线...' : '启动超空间引擎' }}
        </button>

        <button v-if="result" @click="openExternalMap" class="nav-btn">
          🚀 确认路线并出发
        </button>
      </div>

    </div>
  </div>
</template>

<script>
import axios from 'axios';
import AMapLoader from '@amap/amap-jsapi-loader';

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

      // 地图相关对象
      AMap: null, // 🔥 修复1: 增加这个变量，用来存储高德核心类
      map: null,
      geocoder: null,
      driving: null,
      currentMarker: null,
    };
  },
  mounted() {
    window._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE,
    };
    this.initAMap();
  },
  methods: {
    initAMap() {
      this.currentAddress = "正在加载地图资源...";

      AMapLoader.load({
        key: AMAP_KEY,
        version: "2.0",
        plugins: ['AMap.Geocoder', 'AMap.Driving']
      })
          .then((AMap) => {
            // 🔥 修复1: 把 AMap 类存到 this 中，供其他方法使用
            this.AMap = AMap;

            this.map = new AMap.Map("amap-container", {
              viewMode: "3D",
              zoom: 13,
              center: [116.397428, 39.90923],
              mapStyle: 'amap://styles/dark',
            });

            this.geocoder = new AMap.Geocoder({
              city: "全国",
              radius: 1000
            });

            this.driving = new AMap.Driving({
              map: this.map,
              hideMarkers: false,
            });

            this.refreshLocation();
          })
          .catch((e) => {
            console.error(e);
            this.locError = "地图加载失败，请检查 Key";
          });
    },

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

            this.getAmapAddress(this.currentLoc.lat, this.currentLoc.lon);

            if (this.map && this.AMap) { // 确保 AMap 存在
              const center = [this.currentLoc.lon, this.currentLoc.lat];
              this.map.setZoomAndCenter(15, center);

              if (!this.currentMarker) {
                // 🔥 修复1: 使用 this.AMap 而不是 AMap
                this.currentMarker = new this.AMap.Marker({
                  position: center,
                  map: this.map,
                  title: '我的位置'
                });
              } else {
                this.currentMarker.setPosition(center);
              }
            }
          },
          (err) => {
            this.isLocating = false;
            this.currentAddress = "定位失败";
            this.locError = err.message;
          },
          { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
      );
    },

    getAmapAddress(lat, lon) {
      if (!this.geocoder) return;
      this.geocoder.getAddress([lon, lat], (status, result) => {
        this.isLocating = false;
        if (status === 'complete' && result.regeocode) {
          this.currentAddress = result.regeocode.formattedAddress;
        } else {
          this.currentAddress = "未知荒野";
        }
      });
    },

    startExploration() {
      if(!this.currentLoc) return;

      this.loading = true;
      this.apiError = '';

      if(this.driving) this.driving.clear();
      // 生成结果前，先清除当前位置标记，避免视觉干扰（可选）
      if(this.currentMarker) this.currentMarker.setMap(null);

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

            this.driving.search(
                [this.currentLoc.lon, this.currentLoc.lat],
                [this.result.destLon, this.result.destLat],
                (status) => { // 🔥 修复2: 删掉了 unused 'result' 参数
                  if (status === 'complete') {
                    console.log('路线规划成功');
                  } else {
                    this.apiError = '路线规划失败(可能跨海或无法到达)';
                    // 强制移动视角
                    this.map.setCenter([this.result.destLon, this.result.destLat]);
                  }
                }
            );
          })
          .catch(err => {
            this.apiError = "计算失败: " + err.message;
          })
          .finally(() => {
            this.loading = false;
          });
    },

    openExternalMap() {
      if (!this.result) return;
      const { destLat, destLon } = this.result;
      const u = navigator.userAgent;
      const isMobile = !!u.match(/Android|iPhone/i);

      const pcUrl = `https://uri.amap.com/navigation?to=${destLon},${destLat},神秘目的地&mode=car&callnative=1`;

      if (!isMobile) {
        window.open(pcUrl, '_blank');
      } else {
        if (u.indexOf('Android') > -1) {
          window.location.href = `androidamap://route?sourceApplication=WildPointer&dlat=${destLat}&dlon=${destLon}&dev=0&t=0`;
        } else {
          window.location.href = `iosamap://path?sourceApplication=WildPointer&dlat=${destLat}&dlon=${destLon}&dev=0&t=0`;
        }
      }
    }
  }
};
</script>

<style>
/* 保持样式不变 */
body {
  background-color: #1a1a1a;
  color: #ecf0f1;
  font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  margin: 0;
}
.container {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px;
  text-align: center;
}
h1 { color: #42b983; margin-bottom: 5px; font-size: 1.8rem; }
.subtitle { color: #7f8c8d; font-size: 0.9em; margin-bottom: 20px; }

.location-card {
  background: #34495e;
  border-radius: 12px;
  padding: 12px 15px;
  margin-bottom: 15px;
  text-align: left;
  font-size: 0.9rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.2);
}
.loc-row { display: flex; align-items: center; margin-bottom: 6px; }
.loc-label { color: #bdc3c7; font-weight: bold; margin-right: 10px; flex-shrink: 0; }
.loc-value { color: #fff; }
.mono { font-family: monospace; color: #42b983; }

.address-row { margin-bottom: 0; align-items: flex-start; }
.address-wrapper { display: flex; align-items: flex-start; flex-grow: 1; margin-right: 5px; }
.address-text {
  white-space: normal; overflow: visible; line-height: 1.4; word-break: break-all;
}

.refresh-btn {
  width: 28px; height: 28px; border-radius: 50%;
  border: 1px solid #5d6d7e; background: #2c3e50; color: white;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  padding: 0; flex-shrink: 0; margin-top: -2px;
}
.refresh-btn:hover { background: #42b983; border-color: #42b983; }

.map-wrapper {
  position: relative;
  background: #2c3e50;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 15px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.3);
}
#amap-container {
  width: 100%;
  height: 350px;
}
.result-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  background: rgba(35, 48, 61, 0.9);
  padding: 8px;
  border-radius: 8px;
  display: flex;
  justify-content: space-around;
  font-weight: bold;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255,255,255,0.1);
  animation: fadeIn 0.3s;
}

.control-panel {
  background: #2c3e50; padding: 20px; border-radius: 12px;
}
.input-row { display: flex; gap: 10px; margin-bottom: 15px; }
.input-group { flex: 1; text-align: left; }
.input-group label { display: block; font-size: 0.8em; color: #bdc3c7; margin-bottom: 5px; }
.input-group input {
  width: 100%; box-sizing: border-box; padding: 10px;
  background: #1a1a1a; border: 1px solid #465c71; color: #fff;
  border-radius: 6px; font-size: 0.95rem; text-align: center;
}

.jump-btn {
  width: 100%; padding: 15px;
  background: linear-gradient(135deg, #42b983 0%, #3aa876 100%);
  border: none; color: white; font-weight: bold; border-radius: 8px;
  cursor: pointer; font-size: 1rem;
}
.jump-btn:disabled { background: #7f8c8d; cursor: not-allowed; }

.nav-btn {
  width: 100%; margin-top: 15px; padding: 12px;
  background: #e67e22; color: white; border: none; border-radius: 25px;
  font-weight: bold; cursor: pointer;
  box-shadow: 0 4px 6px rgba(0,0,0,0.2);
}

.spinning { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
.error { color: #ff6b6b; font-size: 0.9em; margin-top: 10px; }
.loc-error-text { color: #e74c3c; font-size: 0.8rem; margin: 5px 0 0 0; }
</style>