<template>
  <div id="app">
    <div class="container">
      <header class="app-header">
        <div class="logo-wrapper">🚀</div>
        <span class="app-name">Wild Pointer</span>
      </header>



      <div class="map-wrapper">
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
        <div id="amap-container"></div>
        <div class="radar-wave" :class="{ scanning: loading }"></div>
        <div class="theme-switch" @click="toggleTheme">
          {{ isDarkMode ? '☀️' : '🌙' }}
        </div>
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
    <div class="app-version">Wild Pointer v0.5.0 Alpha</div>
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
      mapMarkers: {
        start: null,
        end: null,
        waypoint: null
      },
      isDarkMode: false, // 深色模式状态
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
  watch: {
    isDarkMode(val) {
      this.applyTheme(val);
    }
  },

  created() {
    this.initTheme(); // 初始化主题
  },

  mounted() {
    window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE };
    this.initAMap();
  },
  methods: {
    // 🔥 核心：更新/创建自定义标记
    // type: 'start' | 'end' | 'waypoint'
    // position: [lon, lat] 或 AMap.LngLat 对象
    updateMarker(type, position) {
      if (!this.map || !position) return;

      // 1. 如果该类型标记已存在，先移除旧的
      if (this.mapMarkers[type]) {
        this.map.remove(this.mapMarkers[type]);
        this.mapMarkers[type] = null;
      }

      // 2. 定义不同类型的图标内容 (Emoji)
      let iconContent = '';
      let className = 'custom-marker';

      switch (type) {
        case 'start':
          iconContent = '📍'; // 起点
          className += ' marker-start';
          break;
        case 'end':
          iconContent = '🏁'; // 终点 (也可以用 🎁 宝箱)
          className += ' marker-end';
          break;
        case 'waypoint':
          iconContent = '✨'; // 神秘点 (也可以用 ❔ 或 ⚔️)
          className += ' marker-way';
          break;
      }

// ✅ 使用 this.AMap 替代全局 AMap，不需要 disable eslint 了
      const marker = new this.AMap.Marker({
        position: position,
        offset: new this.AMap.Pixel(-16, -32),
        content: `<div class="${className}">${iconContent}</div>`,
        map: this.map,
        zIndex: 150
      });

      // 4. 保存实例
      this.mapMarkers[type] = marker;

      // (可选) 如果是终点，自动缩放视野以包含所有点
      // this.map.setFitView();
    },

    initTheme() {
      // 优先读取用户手动保存的设置
      const savedTheme = localStorage.getItem('user_theme');

      if (savedTheme) {
        this.isDarkMode = savedTheme === 'dark';
      } else {
        // 如果没保存过，就跟随系统
        const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        this.isDarkMode = systemDark;
      }

      // 监听系统变化 (如果用户没手动设置过，就一直自动跟随)
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (!localStorage.getItem('user_theme')) {
          this.isDarkMode = e.matches;
        }
      });
    },

    // 2. 切换开关 (绑定给按钮)
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode;
      // 用户一旦手动点击，就保存偏好，不再自动跟随系统
      localStorage.setItem('user_theme', this.isDarkMode ? 'dark' : 'light');
    },

    // 3. 执行变色 (UI + 地图)
    applyTheme(isDark) {
      // A. 设置 HTML 属性供 CSS 使用
      if (isDark) {
        document.documentElement.setAttribute('data-theme', 'dark');
      } else {
        document.documentElement.removeAttribute('data-theme');
      }

      // B. 切换高德地图皮肤
      if (this.map) {
        // amap://styles/normal (标准)
        // amap://styles/dark   (幻影黑 - 推荐)
        // amap://styles/grey   (雅士灰)
        // amap://styles/blue   (极夜蓝 - 很酷)
        const styleName = isDark ? 'amap://styles/dark' : 'amap://styles/normal';
        this.map.setMapStyle(styleName);
      }
    },

    initAMap() {
      this.currentAddress = "正在加载地图资源...";
      AMapLoader.load({
        key: AMAP_KEY,
        version: "2.0",
        plugins: ['AMap.Geocoder', 'AMap.Driving', 'AMap.AutoComplete', 'AMap.PlaceSearch', 'AMap.Geolocation'] // 确保 Geolocation 也加载了
      }).then((AMap) => {
        this.AMap = AMap;
        this.map = new AMap.Map("amap-container", {
          viewMode: "3D",
          zoom: 13,
          center: [116.397428, 39.90923],
          mapStyle: this.isDarkMode ? 'amap://styles/dark' : 'amap://styles/normal',
        });

        this.geocoder = new AMap.Geocoder({ city: "全国" });

        // 🔥 修改1：把 hideMarkers 改为 true
        // 这样规划路线时，高德就不会画它自带的蓝色起终点图标了，只显示路线
        this.driving = new AMap.Driving({
          map: this.map,
          hideMarkers: true // 🔥 必须是 true，否则高德会画自带的蓝色图标，挡住你的 Emoji
        });

        this.placeSearch = new AMap.PlaceSearch({
          city: '全国',
          map: this.map
        });

        const autoOptions = { input: "tipinput" };
        this.autoComplete = new AMap.AutoComplete(autoOptions);

        // 🔥 修改2：搜索选中地点后的逻辑
        this.autoComplete.on("select", (e) => {
          if (e.poi.location) {
            this.selectedDestLoc = {
              lat: e.poi.location.lat,
              lon: e.poi.location.lng,
              name: e.poi.name
            };

            // ✨ 新增：绘制自定义终点图标 (🏁)
            this.updateMarker('end', [e.poi.location.lng, e.poi.location.lat]);

            // ✨ 新增：因为换了终点，如果有旧的中间点，把它清掉
            this.updateMarker('waypoint', null);

            // ✨ 新增：自动调整视野包围所有点
            this.map.setFitView();

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
        // 1. 成功获取坐标
        this.currentLoc = { lat: pos.coords.latitude, lon: pos.coords.longitude };

        // 2. 解析地址文字
        this.getAmapAddress(this.currentLoc.lat, this.currentLoc.lon);

        // 3. 更新地图与标记
        if (this.map) {
          const center = [this.currentLoc.lon, this.currentLoc.lat];
          this.map.setZoomAndCenter(15, center);

          // 🔥🔥🔥 核心修改在这里 🔥🔥🔥
          // 删除原来那种 new AMap.Marker 的写法
          // 改用我们封装好的 updateMarker 方法绘制漂亮的起点图标 (📍)
          this.updateMarker('start', center);
        }

        // 建议在这里关掉加载状态
        this.isLocating = false;

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
        this.finalDest = { lat: dest.destLat, lon: dest.destLon };

        // ✅ 1.【新增】先清除可能存在的中间点 (防止从破雾模式切过来残留)
        this.updateMarker('waypoint', null);

        // ✅ 2.【新增】必须手动绘制终点标记 (🏁)
        this.updateMarker('end', [dest.destLon, dest.destLat]);

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
      this.loading = true; // 🔥 开始动画
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
        // // ✅ 1.【新增】先清除可能存在的中间点 (防止从破雾模式切过来残留)
        this.updateMarker('waypoint', [waypoint.wayLon, waypoint.wayLat] );
        //
        // // ✅ 2.【新增】必须手动绘制终点标记 (🏁)
        this.updateMarker('end', [this.selectedDestLoc.lon, this.selectedDestLoc.lat]);

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
      }).finally(()=> {
        setTimeout(() => {
          this.loading = false; // 🔥 结束动画
        }, 1000);
      })
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
        setTimeout(() => {
          this.loading = false;
        }, 500);
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
/* =========================================
   1. 定义主题变量 (CSS Variables)
   ========================================= */
:root {
  /* ☀️ 浅色模式 (Light Mode) 默认值 */
  --bg-color: #f0f2f5;           /* 整体背景：浅灰 */
  --card-bg: #ffffff;            /* 卡片背景：纯白 */
  --input-bg: #ffffff;           /* 输入框背景 */
  --text-primary: #2c3e50;       /* 主要文字：深蓝灰 */
  --text-secondary: #7f8c8d;     /* 次要文字：灰色 */
  --border-color: #dcdfe6;       /* 边框颜色 */
  --accent-color: #42b983;       /* 主色调：Vue绿 */
  --btn-bg: #ecf0f1;             /* 刷新/Tab按钮背景 */
  --shadow-color: rgba(0, 0, 0, 0.1);
}

/* 🌙 深色模式 (Dark Mode) 覆盖值 */
[data-theme="dark"] {
  --bg-color: #1a1a1a;           /* 原来的背景黑 */
  --card-bg: #2c3e50;            /* 原来的卡片蓝灰 */
  --input-bg: #1a1a1a;           /* 输入框变黑 */
  --text-primary: #ffffff;       /* 文字变白 */
  --text-secondary: #bdc3c7;     /* 次要文字变浅灰 */
  --border-color: #555555;       /* 边框变深 */
  --accent-color: #42b983;       /* 保持不变 */
  --btn-bg: #34495e;             /* 按钮背景变深 */
  --shadow-color: rgba(0, 0, 0, 0.5);
}

/* =========================================
   2. 基础设置 (应用变量)
   ========================================= */
body {
  background: var(--bg-color);
  color: var(--text-primary);
  margin: 0;
  font-family: sans-serif;
  transition: background-color 0.3s, color 0.3s; /* 丝滑切换动画 */
}

.container { max-width: 400px; margin: 0 auto; padding: 20px; text-align: center; }

h1 { color: var(--accent-color); margin-bottom: 5px; }
.subtitle { color: var(--text-secondary); font-size: 0.9em; margin-bottom: 25px; }

/* =========================================
   3. 组件样式
   ========================================= */

/* 定位卡片 */
.location-card {
  background: var(--card-bg); /* 变量 */
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px var(--shadow-color);
  transition: background-color 0.3s;
}

.loc-row { display: flex; align-items: center; justify-content: space-between; }
.address-wrapper { display: flex; align-items: flex-start; flex: 1; overflow: hidden; margin-right: 10px; }
.loc-label { color: var(--text-secondary); font-weight: bold; margin-right: 8px; flex-shrink: 0; }

.address-text {
  white-space: normal;
  overflow: visible;
  line-height: 1.4;
  text-align: left;
  color: var(--text-primary); /* 确保文字颜色跟随主题 */
}

.refresh-btn {
  background: var(--btn-bg); /* 变量 */
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 32px; height: 32px;
  color: var(--text-primary); /* 图标颜色 */
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.spinning { animation: spin 1s linear infinite; display: block; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 地图容器 */
.map-wrapper {
  position: relative;
  background: var(--card-bg);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 15px;
  box-shadow: 0 4px 10px var(--shadow-color);
}
#amap-container { width: 100%; height: 350px; }
/* 结果浮层 (距离和时间) - 移到地图上方 */
.result-overlay {
  position: absolute;
  z-index: 999;

  /* 🟢 定位：底部，放在日夜按钮(右侧)的左边 */
  bottom: 20px;
  right: 70px; /* 15px(边距) + 40px(按钮宽) + 15px(间距) */
  top: auto;   /* 清除顶部定位 */
  left: auto;  /* 清除左侧定位 */

  /* 🟢 外观：跟随主题变色 */
  background: var(--card-bg);
  color: var(--text-primary);

  height: 40px; /* 和右边的按钮一样高 */
  padding: 0 15px; /* 左右内边距 */
  border-radius: 20px; /* 胶囊圆角 */

  display: flex;
  align-items: center;
  gap: 15px; /* 两个数据之间的间距 */

  font-size: 0.9rem;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
  transition: all 0.3s;
  white-space: nowrap; /* 防止文字换行 */
}

/* 让里面的数字醒目一点 (绿色) */
.result-overlay span {
  display: flex;
  align-items: center;
  gap: 4px;
}
/* 控制面板 */
.control-panel {
  background: var(--card-bg); /* 变量 */
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px var(--shadow-color);
  transition: background-color 0.3s;
}

/* 全局设置行 */
.global-settings {
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 20px;
  background: var(--bg-color); /* 这里的背景稍微深一点/浅一点，用 bg-color 区分 card-bg */
  padding: 12px;
  border-radius: 8px;
}
.setting-label { font-size: 0.9rem; color: var(--text-secondary); margin-left: 10px; }

/* Switch 开关 (保持不变，绿白配色通用) */
.toggle-switch { position: relative; display: inline-block; width: 44px; height: 22px; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #555; transition: .4s; border-radius: 22px; }
.slider:before { position: absolute; content: ""; height: 16px; width: 16px; left: 3px; bottom: 3px; background-color: white; transition: .4s; border-radius: 50%; }
input:checked + .slider { background-color: var(--accent-color); }
input:checked + .slider:before { transform: translateX(22px); }

/* Tabs 切换 */
.tabs { display: flex; margin-bottom: 20px; border-bottom: 2px solid var(--border-color); }
.tab-btn { flex: 1; background: none; border: none; color: var(--text-secondary); padding: 12px; font-size: 1rem; cursor: pointer; transition: all 0.3s; }
.tab-btn.active { color: var(--accent-color); font-weight: bold; border-bottom: 3px solid var(--accent-color); margin-bottom: -2px; }

/* 输入控件 */
.input-row { display: flex; gap: 10px; }
.input-group { flex: 1; text-align: left; margin-bottom: 10px; }
.input-group label { display: block; font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 5px; }
.input-group input {
  width: 100%;
  padding: 12px;
  background: var(--input-bg); /* 变量 */
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.3s, color 0.3s, border-color 0.3s;
}
.input-group.full-width { width: 100%; }

/* 按钮 */
.jump-btn { width: 100%; padding: 15px; background: var(--accent-color); border: none; border-radius: 8px; color: white; font-weight: bold; font-size: 1rem; margin-top: 10px; cursor: pointer; transition: opacity 0.2s; }
.jump-btn:disabled { opacity: 0.6; cursor: wait; }
.nav-btn { width: 100%; margin-top: 15px; padding: 12px; background: #e67e22; border: none; border-radius: 25px; color: white; font-weight: bold; cursor: pointer; box-shadow: 0 4px 6px rgba(0,0,0,0.2); }
.error { color: #ff6b6b; font-size: 0.9em; margin-top: 10px; }

/* 搜索组合框样式 */
.search-box { display: flex; align-items: center; gap: 8px; }
.search-box input { flex: 1; width: auto; }
.search-btn {
  background: var(--btn-bg); /* 变量 */
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  width: 42px; height: 42px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.2rem;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.2s;
}
.search-btn:hover { background: var(--accent-color); border-color: var(--accent-color); color: white; }
.search-btn:active { transform: scale(0.95); }


/* 版本号 */
.app-version {
  position: fixed;
  bottom: 5px; right: 5px;
  font-size: 10px;
  color: var(--text-secondary); /* 稍微变一下变量，适应不同底色 */
  opacity: 0.5;
  z-index: 999;
  pointer-events: none;
}
.theme-switch {
  position: absolute;

  /* 🔴 清除原来的 top 设置 */
  top: auto;
  left: auto;

  /* 🟢 新位置：地图右下角 */
  bottom: 20px;
  right: 15px;

  z-index: 999;

  /* 样式微调：加个半透明模糊背景，和上面的位置卡片风格统一 */
  background-color: var(--card-bg); /* 或者用 rgba(255,255,255,0.9) */
  /* backdrop-filter: blur(5px); */ /* 可选：开启毛玻璃效果 */

  width: 40px; /* 稍微大一点点好按 */
  height: 40px;
  border-radius: 50%;

  /* 居中图标 */
  display: flex;
  align-items: center;
  justify-content: center;

  box-shadow: 0 4px 10px rgba(0,0,0,0.2); /* 投影加重一点，浮起感更强 */
  cursor: pointer;
  font-size: 20px;
  user-select: none;
  transition: all 0.3s;
}

/* 深色模式下的微调 */
[data-theme="dark"] .theme-switch {
  background-color: var(--card-bg);
  box-shadow: 0 4px 10px rgba(0,0,0,0.5);
}

.theme-switch:active {
  transform: scale(0.9);
}


/* 雷达中心点 */
.radar-wave {
  position: absolute;
  top: 50%;
  left: 50%;
  /* 居中修正 */
  transform: translate(-50%, -50%);
  width: 0;
  height: 0;
  /* 🔥 关键 1：层级必须极高，盖过高德地图 */
  z-index: 9999;
  /* 让点击穿透，否则雷达挡住地图不能拖动 */
  pointer-events: none;
}

/* 只有当有 .scanning 类时才显示伪元素 */
.radar-wave.scanning::before,
.radar-wave.scanning::after {
  content: '';
  display: block; /* 确保显示 */
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  /* 🔥 关键 2：先用红色测试，防止变量没生效看不见 */
  /* border: 4px solid red; */
  border: 4px solid var(--accent-color, #42b983); /* 如果变量无效，回退到绿色 */
  opacity: 0;
  animation: ripple 2s infinite ease-out;
}

.radar-wave.scanning::after {
  animation-delay: 1s; /* 错开波纹 */
}

@keyframes ripple {
  0% {
    width: 0px;
    height: 0px;
    opacity: 1;
    border-width: 4px;
  }
  100% {
    width: 300px; /* 扩散范围 */
    height: 300px;
    opacity: 0;
    border-width: 0px;
  }
}
.custom-marker {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 32px; /* 图标大小 */
  line-height: 1;
  /* 给图标加个投影，更有立体感 */
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.4));
  /* 加上一点弹跳动画的过渡 */
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
  user-select: none;
}

/* 鼠标悬停或点击时的效果 */
.custom-marker:active, .custom-marker:hover {
  transform: scale(1.3) translateY(-5px);
  filter: drop-shadow(0 8px 10px rgba(0,0,0,0.5));
}

/* 针对不同类型的微调 (可选) */
.marker-start { z-index: 100; }
.marker-end { z-index: 101; }
.marker-way { z-index: 102; animation: float 2s infinite ease-in-out; }

/* 中间点漂浮动画 */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
/* 1. 锁死屏幕高度，禁止页面级滚动 */
html, body {
  height: 100%;
  overflow: hidden; /* 关键：禁止系统滚动条 */
  overscroll-behavior: none; /* 禁止 iOS 橡皮筋效果 */
}

#app {
  height: 100%;
  width: 100%;
}

/* 2. 改造主容器为 Flex 列布局 */
.container {
  max-width: 100%; /* 手机上铺满 */
  height: 100%;    /* 占满屏幕 */
  margin: 0;
  padding: 0;      /* 去掉边距 */
  display: flex;
  flex-direction: column; /* 上下排列 */
  position: relative;
  background: var(--bg-color);
}

/* 3. 标题栏微调 (可选：为了省空间，可以把副标题在手机上隐藏) */
h1 {
  font-size: 1.2rem; /* 字体改小一点 */
  margin: 10px 100px 5px 60px; /* 左右留出 60px 给按钮，防止标题太长重叠 */
  flex-shrink: 0; /* 禁止被压缩 */
}
.subtitle {
  display: none; /* 手机上直接隐藏副标题，省空间 */
}


/* =========================================
   🗺️ 地图区域改造 (弹性伸缩 + 悬浮卡片)
   ========================================= */

/* 5. 地图容器变成弹性元素 */
.map-wrapper {
  flex: 1; /* 🔥 核心：吃掉所有剩余高度！ */
  width: 100%;
  margin: 0; /* 去掉间距 */
  border-radius: 0; /* 地图铺满，不需要圆角了(或者只保留底部圆角) */
  position: relative; /* 为内部悬浮元素做定位基准 */
  box-shadow: none;
}

#amap-container {
  width: 100%;
  height: 100%; /* 强制跟随父容器高度 */
}

/* 6. 🔥 关键优化：把“当前位置卡片”变成悬浮球 */
/* 原来的 .location-card 改造成浮在地图上的样式 */
.location-card {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  z-index: 100; /* 浮在地图上面 */
  background: rgba(255, 255, 255, 0.9); /* 半透明背景 */
  backdrop-filter: blur(5px);
  padding: 8px 12px;
  margin: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  border-radius: 20px; /* 变成胶囊形状 */
}

/* 深色模式适配悬浮卡片 */
[data-theme="dark"] .location-card {
  background: rgba(44, 62, 80, 0.85);
}

/* 调整卡片内部文字大小 */
.loc-label { display: none; /* 省略“当前:”这两个字，省空间 */ }
.address-text { font-size: 0.85rem; }
.refresh-btn { width: 28px; height: 28px; font-size: 14px; }



/* =========================================
   🎛️ 底部控制面板 (Bottom Sheet 风格)
   ========================================= */

.control-panel {
  flex-shrink: 0; /* 禁止压缩 */
  background: var(--card-bg);
  padding: 15px 20px 20px 20px; /* 底部留多点，适配 iPhone 横条 */
  padding-bottom: calc(20px + env(safe-area-inset-bottom)); /* 适配全面屏 */
  border-radius: 24px 24px 0 0; /* 只有上面有圆角 */
  box-shadow: 0 -4px 20px rgba(0,0,0,0.1); /* 向上投影 */
  z-index: 101;
}

/* 压缩一下输入框和按钮的间距 */
.global-settings { margin-bottom: 10px; padding: 8px; }
.tabs { margin-bottom: 10px; }
.tab-btn { padding: 8px; font-size: 0.9rem; }
.jump-btn, .nav-btn { padding: 12px; margin-top: 8px; }
.app-header {
  flex-shrink: 0; /* 禁止压缩 */
  height: 50px;   /* 固定高度 */
  display: flex;
  align-items: center;
  justify-content: center; /* 居中对齐 */
  gap: 10px;      /* 图标和文字的间距 */
  background: var(--bg-color); /* 跟随背景色 */
  z-index: 200;
  padding-top: env(safe-area-inset-top); /* 适配刘海屏 */
  box-shadow: 0 1px 0 var(--border-color); /* 极细的分割线 */
}

/* Logo 容器 */
.logo-wrapper {
  font-size: 24px; /* 如果是 Emoji */
  /* 如果是图片，可以用 width: 24px; height: 24px; */
  animation: float-logo 3s ease-in-out infinite;
}

/* 应用名称 */
.app-name {
  font-size: 1.2rem;
  font-weight: 900; /* 特粗字体 */
  letter-spacing: 1px;
  background: linear-gradient(45deg, var(--accent-color), #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent; /* 渐变文字效果 */
}

/* 简单的 Logo 漂浮动画 */
@keyframes float-logo {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}
</style>