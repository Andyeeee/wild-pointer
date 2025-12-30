<template>
  <div id="app">
    <div class="container">
      <h1>🚀 Wild Pointer</h1>
      <p class="subtitle">未知的路，才是最迷人的。</p>

      <div class="location-card">
        <div class="loc-row address-row">
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
        <div v-if="resultInfo" class="result-overlay">
          <span>📏 {{ resultInfo.distance }}</span>
          <span>🧭 {{ resultInfo.duration }}</span>
        </div>
      </div>

      <div class="card control-panel">

        <div class="global-settings">
          <label class="toggle-switch">
            <input type="checkbox" v-model="useGpxFilter">
            <span class="slider"></span>
          </label>
          <span class="setting-label">
            {{ useGpxFilter ? '⚔️ 破雾模式已开启 (探索新图)' : '破雾模式关闭 (允许重复)' }}
          </span>
        </div>

        <div class="tabs">
          <button
              :class="['tab-btn', currentMode === 'random' ? 'active' : '']"
              @click="currentMode = 'random'"
          >
            🎲 随机瞎逛
          </button>
          <button
              :class="['tab-btn', currentMode === 'dest' ? 'active' : '']"
              @click="currentMode = 'dest'"
          >
            🚩 目的地探索
          </button>
        </div>

        <div v-if="currentMode === 'random'" class="tab-content">
          <div class="input-row">
            <div class="input-group">
              <label>最小半径 (km)</label>
              <input type="number" v-model.number="minRadius" />
            </div>
            <div class="input-group">
              <label>最大半径 (km)</label>
              <input type="number" v-model.number="maxRadius" />
            </div>
          </div>
        </div>

        <div v-if="currentMode === 'dest'" class="tab-content">
          <div class="input-group full-width">
            <label>输入目的地 (支持模糊搜索)</label>

            <div class="search-box">
              <input
                  id="tipinput"
                  type="text"
                  v-model="destKeyword"
                  placeholder="例如: 杭州西湖"
                  @keyup.enter="handleSearch"
              />
              <button class="search-btn" @click="handleSearch">🔍</button>
            </div>

          </div>
        </div>

        <p v-if="apiError" class="error">{{ apiError }}</p>

        <button
            @click="handleStart"
            :disabled="loading || !currentLoc"
            class="jump-btn"
        >
          {{ getButtonText() }}
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
      // 🔥 新增：用来存储中间途经点 {lat, lon}
      currentWaypoint: null,
      placeSearch: null, // 🔥 新增：地点搜索插件实例
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
      resultInfo: null, // 新增：用于显示距离时间
      finalDest: null,  // 新增：最终导航目标
      AMap: null,
      map: null,
      geocoder: null,
      driving: null,
      autoComplete: null,
      currentMarker: null,
    };
  },
  mounted() {
    window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE };
    this.initAMap();
  },
  methods: {
    initAMap() {
      this.currentAddress = "正在加载地图资源...";
      AMapLoader.load({
        key: AMAP_KEY,
        version: "2.0",
        plugins: ['AMap.Geocoder', 'AMap.Driving', 'AMap.AutoComplete', 'AMap.PlaceSearch']
      }).then((AMap) => {
        this.AMap = AMap;
        this.map = new AMap.Map("amap-container", {
          viewMode: "3D", zoom: 13, center: [116.397428, 39.90923], mapStyle: 'amap://styles/dark',
        });
        this.geocoder = new AMap.Geocoder({ city: "全国" });
        this.driving = new AMap.Driving({ map: this.map, hideMarkers: false });
        this.placeSearch = new AMap.PlaceSearch({
          city: '全国', // 搜索范围
          map: this.map // 结果自动显示在地图上(可选，这里主要为了拿坐标)
        });
        const autoOptions = { input: "tipinput" };
        this.autoComplete = new AMap.AutoComplete(autoOptions);
        this.autoComplete.on("select", (e) => {
          if (e.poi.location) {
            this.selectedDestLoc = {
              lat: e.poi.location.lat,
              lon: e.poi.location.lng,
              name: e.poi.name
            };
            this.map.setZoomAndCenter(15, [e.poi.location.lng, e.poi.location.lat]);
          } else {
            this.apiError = "该地点没有具体的坐标信息";
          }
        });
        this.refreshLocation();
      }).catch((e) => {
        console.error(e);
        this.locError = "地图加载失败，请检查 Key";
      });
    },
    handleSearch() {
      if (!this.destKeyword) return;

      // 使用高德 PlaceSearch 搜索关键字
      this.placeSearch.search(this.destKeyword, (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          // 获取第一个搜索结果
          const poi = result.poiList.pois[0];

          if (poi && poi.location) {
            // 更新选中的目的地
            this.selectedDestLoc = {
              lat: poi.location.lat,
              lon: poi.location.lng,
              name: poi.name
            };
            console.log("搜索命中:", poi.name);

            // 地图跳转
            this.map.setZoomAndCenter(15, [poi.location.lng, poi.location.lat]);

            // 自动添加一个标记提醒用户搜到了
            if (this.currentMarker) this.currentMarker.setMap(null);
            this.currentMarker = new this.AMap.Marker({
              position: [poi.location.lng, poi.location.lat],
              map: this.map,
              title: poi.name
            });
          } else {
            this.apiError = "未找到相关地点";
          }
        } else {
          this.apiError = "搜索失败，请尝试更具体的关键词";
        }
      });
    },
    refreshLocation() {
      this.isLocating = true;
      if (!navigator.geolocation) return;
      navigator.geolocation.getCurrentPosition((pos) => {
        this.currentLoc = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        this.getAmapAddress(this.currentLoc.lat, this.currentLoc.lon);
        if (this.map) {
          const center = [this.currentLoc.lon, this.currentLoc.lat];
          this.map.setZoomAndCenter(15, center);
          if (!this.currentMarker) {
            this.currentMarker = new this.AMap.Marker({ position: center, map: this.map });
          } else {
            this.currentMarker.setPosition(center);
          }
        }
      }, (err) => {
        this.isLocating = false;
        this.currentAddress = "定位失败";
        this.locError = err.message;
      }, { enableHighAccuracy: true });
    },

    getAmapAddress(lat, lon) {
      this.geocoder.getAddress([lon, lat], (status, result) => {
        this.isLocating = false;
        if (status === 'complete') this.currentAddress = result.regeocode.formattedAddress;
      });
    },

    getButtonText() {
      if (this.loading) return '正在计算新航线...';

      // 1. 随机模式
      if (this.currentMode === 'random') {
        // 如果开启了破雾，文字变燃
        return this.useGpxFilter ? '🎲 随机破雾探索' : '🎲 随机瞎逛';
      }

      // 2. 目的地模式
      if (this.currentMode === 'dest') {
        return this.useGpxFilter ? '⚔️ 生成破雾路线 (绕路)' : '🚩 生成直达路线';
      }

      return '启动引擎';
    },

    handleStart() {
      if (!this.currentLoc) return;
      this.loading = true;
      this.apiError = '';
      this.result = null; // 重置结果
      this.resultInfo = null;
      if (this.driving) this.driving.clear();

      if (this.currentMode === 'random') {
        this.startRandomMode();
      } else {
        this.startDestMode();
      }
    },

    startRandomMode() {
      this.currentWaypoint = null;
      axios.get('/api/generate-random', {
        params: {
          lat: this.currentLoc.lat,
          lon: this.currentLoc.lon,
          minRadius: this.minRadius,
          maxRadius: this.maxRadius,
          useGpx: this.useGpxFilter
        }
      }).then(res => {
        const dest = res.data;
        this.finalDest = { lat: dest.destLat, lon: dest.destLon }; // 更新最终目标
        this.planRoute(
            [this.currentLoc.lon, this.currentLoc.lat],
            [dest.destLon, dest.destLat]
        );
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
      this.currentWaypoint = null; // 先重置

      // 1. 【普通模式】直接去终点
      if (!this.useGpxFilter) {
        this.planRoute(
            [this.currentLoc.lon, this.currentLoc.lat],
            [this.selectedDestLoc.lon, this.selectedDestLoc.lat],
            [] // 空途经点
        );
        return;
      }

      axios.get('/api/generate-waypoint', {
        params: {
          startLat: this.currentLoc.lat,
          startLon: this.currentLoc.lon,
          endLat: this.selectedDestLoc.lat,
          endLon: this.selectedDestLoc.lon,
          useGpx: true
        }
      }).then(res => {
        const waypoint = res.data;

        // 保存中间点用于跳转 APP
        this.currentWaypoint = { lat: waypoint.wayLat, lon: waypoint.wayLon };

        // 🔥 核心调用：
        // 参数3 必须是数组格式: [ [经度, 纬度] ]
        // 即使只有一个点，也要包在数组里
        this.planRoute(
            [this.currentLoc.lon, this.currentLoc.lat], // 起点
            [this.selectedDestLoc.lon, this.selectedDestLoc.lat], // 终点
            [ [waypoint.wayLon, waypoint.wayLat] ] // 途经点数组
        );
      }).catch(err => {
        console.error(err);
        this.apiError = "未找到合适中间点，已规划直达路线";
        this.planRoute(
            [this.currentLoc.lon, this.currentLoc.lat],
            [this.selectedDestLoc.lon, this.selectedDestLoc.lat]
        );
      });
    },

    // 通用规划方法 (高德画线)
    planRoute(start, end, waypoints = []) {

      // 构造配置对象，对应你文档里的 opts
      const searchOpts = {
        // 高德要求 waypoints 是一个数组，里面可以是坐标 [lon, lat]
        waypoints: waypoints
      };

      // 调用高德 Driving 插件
      // 参数1: 起点
      // 参数2: 终点
      // 参数3: 配置项 (包含途经点)
      // 参数4: 回调
      this.driving.search(start, end, searchOpts, (status, result) => {
        this.loading = false;
        if (status === 'complete') {
          this.result = { destLat: end[1], destLon: end[0] };

          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0];
            this.resultInfo = {
              distance: (route.distance / 1000).toFixed(1) + ' km',
              duration: Math.ceil(route.time / 60) + ' 分钟'
            };
          }
          console.log('✅ 预览路线规划成功，包含途经点:', waypoints);
        } else {
          this.apiError = '路线规划失败: ' + status;
          console.error(result);
        }
      });
    },

    openExternalMap() {
      // 1. 安全检查
      if (!this.finalDest || !this.currentLoc) {
        alert("坐标不全，无法出发");
        return;
      }

      const end = this.finalDest;
      const mid = this.currentWaypoint;
      const appName = 'WildPointer';

      // 2. 坐标精度处理
      const eLon = Number(end.lon).toFixed(6);
      const eLat = Number(end.lat).toFixed(6);
      // 注意：路径规划协议中，终点名称参数通常是 dname
      const eName = encodeURIComponent(end.name || '探索终点');

      // =============================================
      // 策略 A: 破雾模式 (必须带途经点) -> 保持 Web 协议
      // =============================================
      if (mid) {
        console.log("⚔️ 破雾模式：使用 Web 协议以支持途经点");
        const mLon = Number(mid.lon).toFixed(6);
        const mLat = Number(mid.lat).toFixed(6);

        // 这里的 callnative=1 依然会尝试拉起 App，显示带途经点的规划页
        let webUrl = `https://uri.amap.com/navigation?to=${eLon},${eLat},${eName}&mode=car&policy=1&src=${appName}&coordinate=gaode&callnative=1`;
        webUrl += `&via=${mLon},${mLat},神秘中间点`;

        window.location.href = webUrl;
        return;
      }

      // =============================================
      // 策略 B: 直达模式 (随机/普通导航) -> 改用【路径规划】原生协议
      // =============================================
      console.log("🚀 直达模式：使用原生路径规划协议");

      const u = navigator.userAgent;
      const isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/);
      const isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1;

      let schemaUrl = '';

      if (isiOS) {
        // [iOS] 改用 path (路径规划)
        // dlat/dlon: 终点坐标, t: 0 (驾车)
        schemaUrl = `iosamap://path?sourceApplication=${appName}&dname=${eName}&dlat=${eLat}&dlon=${eLon}&dev=0&t=0`;
      }
      else if (isAndroid) {
        // [Android] 改用 route/plan (路径规划)
        // dlat/dlon: 终点坐标, t: 0 (驾车)
        schemaUrl = `androidamap://route/plan?sourceApplication=${appName}&dname=${eName}&dlat=${eLat}&dlon=${eLon}&dev=0&t=0`;
      }
      // 执行跳转
      window.location.href = schemaUrl;
    }
  }
};
</script>

<style>
/* 基础设置 */
body { background: #1a1a1a; color: white; margin: 0; font-family: sans-serif; }
.container { max-width: 400px; margin: 0 auto; padding: 20px; text-align: center; }
h1 { color: #42b983; margin-bottom: 5px; }
.subtitle { color: #7f8c8d; font-size: 0.9em; margin-bottom: 25px; }

/* 定位卡片 */
.location-card { background: #2c3e50; padding: 12px; border-radius: 8px; margin-bottom: 15px; }
.loc-row { display: flex; align-items: center; justify-content: space-between; }
.address-wrapper { display: flex; align-items: flex-start; flex: 1; overflow: hidden; margin-right: 10px; }
.loc-label { color: #bdc3c7; font-weight: bold; margin-right: 8px; flex-shrink: 0; }
/* 🔥 修复：地址自动换行 */
.address-text {
  white-space: normal;
  overflow: visible;
  line-height: 1.4;
  text-align: left;
}
.refresh-btn { background: #34495e; border: 1px solid #555; border-radius: 50%; width: 32px; height: 32px; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.spinning { animation: spin 1s linear infinite; display: block; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 地图容器 */
.map-wrapper { position: relative; background: #2c3e50; border-radius: 12px; overflow: hidden; margin-bottom: 15px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); }
#amap-container { width: 100%; height: 350px; }
.result-overlay { position: absolute; top: 10px; left: 10px; right: 10px; background: rgba(0,0,0,0.7); padding: 8px; border-radius: 8px; display: flex; justify-content: space-around; font-weight: bold; backdrop-filter: blur(4px); }

/* 控制面板 */
.control-panel { background: #2c3e50; padding: 20px; border-radius: 12px; }

/* 全局设置行 */
.global-settings { display: flex; align-items: center; justify-content: center; margin-bottom: 20px; background: #1a2634; padding: 12px; border-radius: 8px; }
.setting-label { font-size: 0.9rem; color: #bdc3c7; margin-left: 10px; }

/* Switch 开关 */
.toggle-switch { position: relative; display: inline-block; width: 44px; height: 22px; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #555; transition: .4s; border-radius: 22px; }
.slider:before { position: absolute; content: ""; height: 16px; width: 16px; left: 3px; bottom: 3px; background-color: white; transition: .4s; border-radius: 50%; }
input:checked + .slider { background-color: #42b983; }
input:checked + .slider:before { transform: translateX(22px); }

/* Tabs 切换 */
.tabs { display: flex; margin-bottom: 20px; border-bottom: 2px solid #34495e; }
.tab-btn { flex: 1; background: none; border: none; color: #7f8c8d; padding: 12px; font-size: 1rem; cursor: pointer; transition: all 0.3s; }
.tab-btn.active { color: #42b983; font-weight: bold; border-bottom: 3px solid #42b983; margin-bottom: -2px; }

/* 输入控件 */
.input-row { display: flex; gap: 10px; }
.input-group { flex: 1; text-align: left; margin-bottom: 10px; }
.input-group label { display: block; font-size: 0.8rem; color: #bdc3c7; margin-bottom: 5px; }
.input-group input { width: 100%; padding: 12px; background: #1a1a1a; border: 1px solid #555; color: white; border-radius: 6px; box-sizing: border-box; }
.input-group.full-width { width: 100%; }

/* 按钮 */
.jump-btn { width: 100%; padding: 15px; background: #42b983; border: none; border-radius: 8px; color: white; font-weight: bold; font-size: 1rem; margin-top: 10px; cursor: pointer; transition: opacity 0.2s; }
.jump-btn:disabled { opacity: 0.6; cursor: wait; }
.nav-btn { width: 100%; margin-top: 15px; padding: 12px; background: #e67e22; border: none; border-radius: 25px; color: white; font-weight: bold; cursor: pointer; box-shadow: 0 4px 6px rgba(0,0,0,0.2); }
.error { color: #ff6b6b; font-size: 0.9em; margin-top: 10px; }

/* 搜索组合框样式 */
.search-box {
  display: flex;
  align-items: center;
  gap: 8px; /* 间距 */
}

/* 输入框自适应宽度 */
.search-box input {
  flex: 1; /* 占满剩余空间 */
  /* 复用之前的 input 样式，但去掉宽度限制 */
  width: auto;
}

/* 搜索小按钮 */
.search-btn {
  background: #34495e;
  border: 1px solid #555;
  color: white;
  width: 42px; /* 方形按钮 */
  height: 42px; /* 和输入框高度一致 */
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #42b983;
  border-color: #42b983;
}

.search-btn:active {
  transform: scale(0.95);
}

.app-version {
  position: fixed;
  bottom: 5px;
  right: 5px;
  font-size: 10px;
  color: rgba(0, 0, 0, 0.3); /* 半透明黑色，不抢眼 */
  z-index: 999; /* 保证在地图上面 */
  pointer-events: none; /* 让点击穿透，不影响操作地图 */
}
</style>