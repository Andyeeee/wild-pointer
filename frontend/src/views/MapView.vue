<template>
  <div class="map-view">
    <div class="info-section">
      <div v-if="resultInfo" class="result-row">
        <span>📏 {{ resultInfo.distance }}</span>
        <span>🧭 {{ resultInfo.duration }}</span>
      </div>
      <div class="location-row">
        <div class="address-wrapper">
          <span class="loc-label">当前:</span>
          <span class="loc-value address-text">
            {{ currentAddress || '正在连接卫星...' }}
          </span>
        </div>
        <button class="refresh-btn" @click="refreshLocation" :disabled="isLocating">
          <span v-if="isLocating" class="spinning">⟳</span>
          <span v-else>📍</span>
        </button>
      </div>
    </div>

    <div class="map-wrapper">
      <div id="amap-container"></div>

      <div class="radar-wave" :class="{ scanning: loading }"></div>

      <div class="theme-switch" @click="toggleTheme">
        {{ isDarkMode ? '☀️' : '🌙' }}
      </div>
    </div>

    <div class="card control-panel">
      <div class="global-settings">
        <label class="toggle-switch">
          <input type="checkbox" v-model="useGpxFilter">
          <span class="slider"></span>
        </label>
        <span class="setting-label">
          {{ useGpxFilter ? '⚔️ 破雾模式 (探索新图)' : '普通模式 (允许重复)' }}
        </span>
      </div>

      <div class="tabs">
        <button :class="['tab-btn', currentMode === 'random' ? 'active' : '']" @click="currentMode = 'random'">
          🎲 随机瞎逛
        </button>
        <button :class="['tab-btn', currentMode === 'dest' ? 'active' : '']" @click="currentMode = 'dest'">
          🚩 目的地探索
        </button>
      </div>

      <div v-if="currentMode === 'random'" class="tab-content">
        <div class="input-row">
          <div class="input-group">
            <label>最小半径 (km)</label>
            <input type="number" v-model.number="minRadius"/>
          </div>
          <div class="input-group">
            <label>最大半径 (km)</label>
            <input type="number" v-model.number="maxRadius"/>
          </div>
        </div>
      </div>

      <div v-if="currentMode === 'dest'" class="tab-content">
        <div class="input-group full-width">
          <label>输入目的地 (支持模糊搜索)</label>
          <div class="search-box">
            <input id="tipinput" type="text" v-model="destKeyword" placeholder="例如: 杭州西湖"
                   @keyup.enter="handleSearch"/>
            <button class="search-btn" @click="handleSearch">🔍</button>
          </div>
          <div v-if="selectedDestLoc" class="dest-confirm">
            ✅ 已选择: {{ selectedDestLoc.name }}
          </div>
        </div>
      </div>

      <p v-if="apiError" class="error">{{ apiError }}</p>

      <button @click="handleStart" :disabled="loading || !currentLoc || (currentMode === 'dest' && !selectedDestLoc)" class="jump-btn">
        {{ getButtonText() }}
      </button>

      <button v-if="result" @click="openExternalMap" class="nav-btn">
        🚀 确认路线并出发
      </button>

      <button v-if="user && result" @click="saveRoute" class="save-route-btn">
        💾 保存到历史
      </button>

      <button v-if="user && result" @click="toggleFavorite" class="favorite-btn" :class="{ liked: isRouteFavorited }">
        {{ isRouteFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
      </button>
    </div>
  </div>
</template>

<script>
import api from '@/utils/axios';
import AMapLoader from '@amap/amap-jsapi-loader';

export default {
  name: 'MapView',
  props: {
    user: {
      type: Object,
      default: null
    },
    isDarkMode: {
      type: Boolean,
      default: false
    },
    fogVersion: {
      type: Number,
      default: 0
    },
    fogColor: {
      type: String,
      default: '#42b983'
    }
  },
  data() {
    return {
      amapConfig: null,
      mapMarkers: {start: null, end: null, waypoint: null},
      currentWaypoint: null,
      placeSearch: null,
      currentMode: 'random',
      useGpxFilter: false,
      minRadius: 10,
      maxRadius: 50,
      destKeyword: '',
      selectedDestLoc: null,
      currentLoc: null,
      currentAddress: '',
      isLocating: false,
      loading: false,
      apiError: '',
      result: null,
      resultInfo: null,
      finalDest: null,
      AMap: null,
      map: null,
      geocoder: null,
      driving: null,
      autoComplete: null,
      isRouteFavorited: false,
      fogCells: null,
      fogCanvas: null,
      fogLayer: null,
      fogKeySet: null
    };
  },
  watch: {
    isDarkMode(val) {
      this.applyTheme(val);
    },
    fogVersion() {
      // 新 GPX 上传后清空缓存重新加载
      this.fogCells = [];
      this.fogKeySet = new Set();
      if (this.map) this.loadFogCells();
    },
    fogColor() {
      if (this.map) this.renderFogLayer();
    }
  },
  created() {
    this.fetchAmapConfig();
  },
  mounted() {
    this.initAMap();
  },
  beforeDestroy() {
    if (this.fogLayer) {
      this.fogLayer.setMap(null);
    }
  },
  methods: {
    async fetchAmapConfig() {
      try {
        const response = await api.get('/api/config/amap');
        this.amapConfig = response.data;
        console.log('✅ 高德地图配置已从后端加载');
      } catch (error) {
        console.error('❌ 获取高德地图配置失败:', error);
        this.apiError = '系统初始化失败，请刷新重试';
      }
    },

    updateMarker(type, position) {
      if (!this.map || !position) return;
      if (this.mapMarkers[type]) {
        this.map.remove(this.mapMarkers[type]);
        this.mapMarkers[type] = null;
      }
      let iconContent = '';
      let className = 'custom-marker';
      switch (type) {
        case 'start':
          iconContent = '📍';
          className += ' marker-start';
          break;
        case 'end':
          iconContent = '🏁';
          className += ' marker-end';
          break;
        case 'waypoint':
          iconContent = '✨';
          className += ' marker-way';
          break;
      }
      const marker = new this.AMap.Marker({
        position: position,
        offset: new this.AMap.Pixel(-16, -32),
        content: `<div class="${className}">${iconContent}</div>`,
        map: this.map,
        zIndex: 150
      });
      this.mapMarkers[type] = marker;
    },

    toggleTheme() {
      this.$emit('toggle-theme');
    },

    applyTheme(isDark) {
      if (this.map) {
        const styleName = isDark ? 'amap://styles/dark' : 'amap://styles/normal';
        this.map.setMapStyle(styleName);
      }
    },

    initAMap() {
      if (!this.amapConfig) {
        this.currentAddress = "加载配置中...";
        setTimeout(() => this.initAMap(), 200);
        return;
      }

      this.currentAddress = "正在加载地图资源...";

      window._AMapSecurityConfig = {
        securityJsCode: this.amapConfig.securityCode
      };

      AMapLoader.load({
        key: this.amapConfig.key,
        version: "2.0",
        plugins: ['AMap.Geocoder', 'AMap.Driving', 'AMap.AutoComplete', 'AMap.PlaceSearch', 'AMap.Geolocation']
      }).then((AMap) => {
        this.AMap = AMap;
        this.map = new AMap.Map("amap-container", {
          viewMode: "3D", zoom: 13, center: [116.397428, 39.90923],
          mapStyle: this.isDarkMode ? 'amap://styles/dark' : 'amap://styles/normal',
        });
        this.geocoder = new AMap.Geocoder({city: "全国"});
        this.driving = new AMap.Driving({map: this.map, hideMarkers: true, autoFitView: false});
        this.placeSearch = new AMap.PlaceSearch({city: '全国', map: this.map});
        const autoOptions = {input: "tipinput"};
        this.autoComplete = new AMap.AutoComplete(autoOptions);

        this.autoComplete.on("select", (e) => {
          if (e.poi.location) {
            this.selectedDestLoc = {lat: e.poi.location.lat, lon: e.poi.location.lng, name: e.poi.name};
            this.updateMarker('end', [e.poi.location.lng, e.poi.location.lat]);
            this.updateMarker('waypoint', null);

            setTimeout(() => {
              this.map.setFitView(null, false, [60, 40, 60, 40]);
            }, 100);
          } else {
            this.apiError = "该地点没有具体的坐标信息";
          }
        });
        this.refreshLocation();

        // 初始化迷雾覆盖层
        if (this.user) {
          this.initFogOverlay();
        }
      }).catch((e) => {
        console.error(e);
        this.apiError = "地图加载失败";
      });
    },

    handleSearch() {
      if (!this.destKeyword) return;
      this.placeSearch.search(this.destKeyword, (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          const poi = result.poiList.pois[0];
          if (poi && poi.location) {
            this.selectedDestLoc = {lat: poi.location.lat, lon: poi.location.lng, name: poi.name};
            this.map.setZoomAndCenter(15, [poi.location.lng, poi.location.lat]);
            this.updateMarker('end', [poi.location.lng, poi.location.lat]);
            this.updateMarker('waypoint', null);
          } else {
            this.apiError = "未找到相关地点";
          }
        } else {
          this.apiError = "搜索失败";
        }
      });
    },

    refreshLocation() {
      this.isLocating = true;
      if (!navigator.geolocation) return;
      navigator.geolocation.getCurrentPosition((pos) => {
        this.currentLoc = {lat: pos.coords.latitude, lon: pos.coords.longitude};
        this.getAmapAddress(this.currentLoc.lat, this.currentLoc.lon);
        if (this.map) {
          const center = [this.currentLoc.lon, this.currentLoc.lat];
          this.map.setZoomAndCenter(15, center);
          this.updateMarker('start', center);
        }
        this.isLocating = false;
      }, () => {
        this.isLocating = false;
        this.currentAddress = "定位失败";
      }, {enableHighAccuracy: true});
    },

    getAmapAddress(lat, lon) {
      if (!this.geocoder) return;
      this.geocoder.getAddress([lon, lat], (status, result) => {
        if (status === 'complete') this.currentAddress = result.regeocode.formattedAddress;
      });
    },

    getButtonText() {
      if (this.loading) return '正在计算新航线...';
      if (this.currentMode === 'random') {
        if (!this.currentLoc) return '正在定位...';
        return this.useGpxFilter ? '🎲 随机破雾探索' : '🎲 随机瞎逛';
      }
      if (this.currentMode === 'dest') {
        if (!this.selectedDestLoc) return '🔍 请先搜索目的地';
        return this.useGpxFilter ? '⚔️ 规划破雾路线 (绕路)' : '🚩 规划路线';
      }
      return '启动引擎';
    },

    handleStart() {
      if (!this.currentLoc) {
        this.apiError = '请等待位置定位完成';
        return;
      }
      if (this.currentMode === 'dest' && !this.selectedDestLoc) {
        this.apiError = '请先搜索并选择一个目的地';
        return;
      }
      this.loading = true;
      this.apiError = '';
      this.result = null;
      this.resultInfo = null;
      if (this.driving) this.driving.clear();
      if (this.currentMode === 'random') this.startRandomMode();
      else this.startDestMode();
    },

    startRandomMode() {
      this.currentWaypoint = null;
      api.get('/api/generate-random', {
        params: {
          lat: this.currentLoc.lat,
          lon: this.currentLoc.lon,
          minRadius: this.minRadius,
          maxRadius: this.maxRadius,
          useGpx: this.useGpxFilter
        }
      }).then(res => {
        const dest = res.data;
        this.finalDest = {lat: dest.destLat, lon: dest.destLon};
        this.updateMarker('waypoint', null);
        this.updateMarker('end', [dest.destLon, dest.destLat]);
        this.planRoute([this.currentLoc.lon, this.currentLoc.lat], [dest.destLon, dest.destLat]);
        if (this.useGpxFilter && dest.isFogCleared) {
          this.apiError = '⚔️ 发现未探索区域！';
        } else if (this.useGpxFilter) {
          this.apiError = '🌫️ 该区域已探索过';
        }
      }).catch(err => {
        this.apiError = "随机生成失败: " + err.message;
        this.loading = false;
      });
    },

    startDestMode() {
      if (!this.selectedDestLoc) {
        this.apiError = "请先搜索并选择一个目的地";
        this.loading = false;
        return;
      }
      this.finalDest = this.selectedDestLoc;
      this.currentWaypoint = null;
      this.loading = true;
      if (!this.useGpxFilter) {
        this.planRoute([this.currentLoc.lon, this.currentLoc.lat], [this.selectedDestLoc.lon, this.selectedDestLoc.lat], []);
        return;
      }
      api.get('/api/generate-waypoint', {
        params: {
          startLat: this.currentLoc.lat, startLon: this.currentLoc.lon,
          endLat: this.selectedDestLoc.lat, endLon: this.selectedDestLoc.lon,
          useGpx: true
        }
      }).then(res => {
        const waypoint = res.data;
        this.currentWaypoint = {lat: waypoint.wayLat, lon: waypoint.wayLon};
        this.updateMarker('waypoint', [waypoint.wayLon, waypoint.wayLat]);
        this.updateMarker('end', [this.selectedDestLoc.lon, this.selectedDestLoc.lat]);
        this.planRoute(
            [this.currentLoc.lon, this.currentLoc.lat],
            [this.selectedDestLoc.lon, this.selectedDestLoc.lat],
            [[waypoint.wayLon, waypoint.wayLat]]
        );
      }).catch(() => {
        console.error('waypoint generation failed');
        this.apiError = "未找到合适中间点，已规划直达路线";
        this.planRoute([this.currentLoc.lon, this.currentLoc.lat], [this.selectedDestLoc.lon, this.selectedDestLoc.lat]);
      }).finally(() => {
        setTimeout(() => {
          this.loading = false;
        }, 1000);
      })
    },

    planRoute(start, end, waypoints = []) {
      const searchOpts = {waypoints: waypoints};
      this.driving.search(start, end, searchOpts, (status, result) => {
        if (status === 'complete') {
          this.result = {destLat: end[1], destLon: end[0]};
          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0];
            this.resultInfo = {
              distance: (route.distance / 1000).toFixed(1) + ' km',
              duration: Math.ceil(route.time / 60) + ' 分钟'
            };
          }
          console.log('✅ 路线规划成功');
          setTimeout(() => {
            this.map.setFitView(null, false, [60, 40, 60, 40]);
          }, 150);
        } else {
          this.apiError = '路线规划失败: ' + status;
          console.error(result);
        }
        setTimeout(() => {
          this.loading = false;
        }, 500);
      });
    },

    openExternalMap() {
      if (!this.finalDest || !this.currentLoc) {
        alert("坐标不全");
        return;
      }
      const end = this.finalDest;
      const mid = this.currentWaypoint;
      const appName = 'WildPointer';
      const eLon = Number(end.lon).toFixed(6);
      const eLat = Number(end.lat).toFixed(6);
      const eName = encodeURIComponent(end.name || '探索终点');

      if (mid) {
        const mLon = Number(mid.lon).toFixed(6);
        const mLat = Number(mid.lat).toFixed(6);
        let webUrl = `https://uri.amap.com/navigation?to=${eLon},${eLat},${eName}&mode=car&policy=1&src=${appName}&coordinate=gaode&callnative=1`;
        webUrl += `&via=${mLon},${mLat},神秘中间点`;
        window.location.href = webUrl;
        return;
      }
      const u = navigator.userAgent;
      const isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/);
      const isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1;
      let schemaUrl = '';
      if (isiOS) schemaUrl = `iosamap://path?sourceApplication=${appName}&dname=${eName}&dlat=${eLat}&dlon=${eLon}&dev=0&t=0`;
      else if (isAndroid) schemaUrl = `androidamap://route/plan?sourceApplication=${appName}&dname=${eName}&dlat=${eLat}&dlon=${eLon}&dev=0&t=0`;
      window.location.href = schemaUrl;
    },

    async saveRoute() {
      if (!this.user || !this.result) {
        this.apiError = '请先登录并生成路线';
        return;
      }
      try {
        const res = (await api.post('/api/routes', {
            name: `${this.currentMode === 'random' ? '随机探索' : '目的地路线'} - ${new Date().toLocaleString('zh-CN')}`,
            startLocation: this.currentAddress,
            endLocation: this.selectedDestLoc?.name || '目的地',
            startLat: this.currentLoc?.lat,
            startLon: this.currentLoc?.lon,
            endLat: this.finalDest?.lat,
            endLon: this.finalDest?.lon,
            distance: this.resultInfo?.distance || '未知',
            duration: this.resultInfo?.duration || '未知',
            routeType: this.currentMode === 'random' ? 'RANDOM' : 'DESTINATION'
        })).data;
        this.apiError = res.success ? '✅ 已保存到历史' : ('保存失败: ' + res.message);
      } catch (err) {
        this.apiError = '保存失败: ' + err.message;
      }
    },

    toggleFavorite() {
      if (!this.user || !this.result) {
        this.apiError = '请先登录并生成路线';
        return;
      }
      this.isRouteFavorited = !this.isRouteFavorited;
      if (this.isRouteFavorited) {
        api.post('/api/favorites', {
            name: this.selectedDestLoc?.name || '我的收藏路线',
            location: `${this.selectedDestLoc?.lon},${this.selectedDestLoc?.lat}`,
            type: 'ROUTE'
        }).catch(() => {
          this.apiError = '收藏失败';
          this.isRouteFavorited = false;
        });
      }
    },

    initFogOverlay() {
      this.fogCanvas = document.createElement('canvas');
      this.fogLayer = new this.AMap.CustomLayer(this.fogCanvas, {
        zIndex: 120,
        zooms: [3, 20]
      });
      this.fogLayer.render = () => this.renderFogLayer();
      this.fogLayer.setMap(this.map);
      this.fogCells = [];
      this.fogKeySet = new Set();
      this.loadFogCells();
      this.map.on('moveend', () => this.loadFogCells());
    },

    async loadFogCells() {
      if (!this.user || !this.map) return;
      try {
        const bounds = this.map.getBounds();
        const ne = bounds.getNorthEast();
        const sw = bounds.getSouthWest();
        const res = (await api.get('/api/fog/cells', {
          params: {
            southLat: sw.lat,
            northLat: ne.lat,
            westLon: sw.lng,
            eastLon: ne.lng
          }
        })).data;
        if (res.success) {
          let hasNew = false;
          for (const cell of res.data) {
            const key = cell.gridLat + ',' + cell.gridLon;
            if (!this.fogKeySet.has(key)) {
              this.fogKeySet.add(key);
              this.fogCells.push(cell);
              hasNew = true;
            }
          }
          if (hasNew && this.fogLayer) this.fogLayer.render();
        }
      } catch (error) {
        console.error('加载迷雾数据失败:', error);
      }
    },

    renderFogLayer() {
      if (!this.fogCanvas || !this.AMap || !this.map) return;
      const canvas = this.fogCanvas;
      const ctx = canvas.getContext('2d');
      const size = this.map.getSize();
      const w = size.getWidth();
      const h = size.getHeight();

      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
      ctx.clearRect(0, 0, w, h);
      ctx.imageSmoothingEnabled = false;

      const cells = this.fogCells;
      if (!cells || cells.length === 0) return;

      const bounds = this.map.getBounds();
      const ne = bounds.getNorthEast();
      const sw = bounds.getSouthWest();
      const southLat = sw.lat * 10000;
      const northLat = ne.lat * 10000;
      const westLng = sw.lng * 10000;
      const eastLng = ne.lng * 10000;
      const lngRange = ne.lng - sw.lng;
      const gridSizeDeg = 0.0001;
      const cellW = Math.max(1, Math.ceil(gridSizeDeg / lngRange * w));

      const hex = this.fogColor.replace('#', '');
      ctx.fillStyle = 'rgba(' + parseInt(hex.substring(0, 2), 16) + ','
        + parseInt(hex.substring(2, 4), 16) + ','
        + parseInt(hex.substring(4, 6), 16) + ',0.25)';

      for (let i = 0, len = cells.length; i < len; i++) {
        const c = cells[i];
        if (c.gridLat < southLat || c.gridLat > northLat || c.gridLon < westLng || c.gridLon > eastLng) continue;
        const pixel = this.map.lngLatToContainer(new this.AMap.LngLat(c.gridLon / 10000, c.gridLat / 10000));
        ctx.fillRect(pixel.getX() | 0, pixel.getY() | 0, cellW, cellW);
      }
    }
  }
};
</script>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  gap: 10px;
  box-sizing: border-box;
}

/* 信息区域 */
.info-section {
  flex-shrink: 0;
  background: var(--card-bg);
  z-index: 100;
  border-bottom: 1px solid var(--border-color);
  border-radius: 12px;
  box-sizing: border-box;
}

.result-row {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  font-size: 0.95rem;
  font-weight: bold;
  color: var(--text-primary);
  padding: 0 20px;
}

.result-row span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.location-row {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  transition: background-color 0.3s;
}

.address-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
  margin-right: 10px;
}

.loc-label {
  display: none;
}

.address-text {
  font-size: 0.85rem;
  white-space: normal;
  line-height: 1.4;
  text-align: left;
  color: var(--text-primary);
}

.refresh-btn {
  background: var(--btn-bg);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 28px;
  height: 28px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.spinning {
  animation: spin 1s linear infinite;
  display: block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 地图区域 */
.map-wrapper {
  flex: 1;
  width: 100%;
  position: relative;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

#amap-container {
  width: 100%;
  height: 100%;
  flex: 1;
}

.theme-switch {
  position: absolute;
  top: auto;
  left: auto;
  bottom: 20px;
  right: 15px;
  z-index: 100;
  background-color: var(--card-bg);
  color: var(--text-primary);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  font-size: 20px;
  user-select: none;
  transition: all 0.3s;
}

.theme-switch:active {
  transform: scale(0.9);
}

/* 控制面板 */
.control-panel {
  flex-shrink: 0;
  background: var(--card-bg);
  padding: 12px 16px 16px 16px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  border-radius: 12px;
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.15);
  z-index: 101;
  border-top: 1px solid var(--border-color);
  box-sizing: border-box;
  overflow-y: auto;
  max-height: 35vh;
}

.global-settings {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  background: var(--bg-color);
  padding: 8px;
  border-radius: 8px;
}

.setting-label {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-left: 10px;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 22px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #555;
  transition: .4s;
  border-radius: 22px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--accent-color);
}

input:checked + .slider:before {
  transform: translateX(22px);
}

.tabs {
  display: flex;
  margin-bottom: 10px;
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  color: var(--text-secondary);
  padding: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn.active {
  color: var(--accent-color);
  font-weight: bold;
  border-bottom: 3px solid var(--accent-color);
  margin-bottom: -2px;
}

.tab-content {
  margin-bottom: 10px;
}

.input-row {
  display: flex;
  gap: 10px;
}

.input-group {
  flex: 1;
  text-align: left;
  margin-bottom: 10px;
}

.input-group label {
  display: block;
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 5px;
}

.input-group input {
  width: 100%;
  padding: 12px;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.3s, color 0.3s;
}

.input-group.full-width {
  width: 100%;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box input {
  flex: 1;
  width: auto;
}

.search-btn {
  background: var(--btn-bg);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  width: 42px;
  height: 42px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.search-btn:hover {
  background: var(--accent-color);
  border-color: var(--accent-color);
  color: white;
}

.dest-confirm {
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(66, 185, 131, 0.1);
  border-left: 3px solid var(--accent-color);
  border-radius: 4px;
  font-size: 0.85rem;
  color: var(--accent-color);
  font-weight: 600;
}

.jump-btn {
  width: 100%;
  padding: 12px;
  background: var(--accent-color);
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: bold;
  font-size: 1rem;
  margin-top: 8px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.jump-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.nav-btn {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  background: #e67e22;
  border: none;
  border-radius: 25px;
  color: white;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);
}

.save-route-btn,
.favorite-btn {
  width: 100%;
  margin-top: 8px;
  padding: 10px;
  background: var(--btn-bg);
  border: 1px solid var(--accent-color);
  color: var(--accent-color);
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.favorite-btn.liked {
  background: rgba(66, 185, 131, 0.2);
  color: var(--accent-color);
}

.error {
  color: #ff6b6b;
  font-size: 0.9em;
  margin-top: 10px;
}

.radar-wave {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 0;
  height: 0;
  z-index: 9999;
  pointer-events: none;
}

.radar-wave.scanning::before, .radar-wave.scanning::after {
  content: '';
  display: block;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 4px solid var(--accent-color, #42b983);
  opacity: 0;
  animation: ripple 2s infinite ease-out;
}

.radar-wave.scanning::after {
  animation-delay: 1s;
}

@keyframes ripple {
  0% {
    width: 0;
    height: 0;
    opacity: 1;
    border-width: 4px;
  }
  100% {
    width: 300px;
    height: 300px;
    opacity: 0;
    border-width: 0px;
  }
}

.custom-marker {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 32px;
  line-height: 1;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.4));
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
  user-select: none;
}

.custom-marker:active, .custom-marker:hover {
  transform: scale(1.3) translateY(-5px);
}

.marker-start {
  z-index: 100;
}

.marker-end {
  z-index: 101;
}

.marker-way {
  z-index: 102;
  animation: float 2s infinite ease-in-out;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>
