<template>
  <div class="route-detail-view">
    <div class="detail-header">
      <button @click="$emit('back')" class="back-btn">← 返回</button>
      <h2>{{ route.name }}</h2>
    </div>

    <div class="detail-map-wrapper">
      <div id="detail-map-container"></div>
    </div>

    <div class="detail-info">
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">📏 距离</span>
          <span class="info-value">{{ route.distance }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">🧭 时长</span>
          <span class="info-value">{{ route.duration }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">📍 起点</span>
          <span class="info-value">{{ route.startLocation || '未知' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">🏁 终点</span>
          <span class="info-value">{{ route.endLocation || '未知' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">🏷️ 类型</span>
          <span class="info-value">{{ route.routeType === 'RANDOM' ? '🎲 随机探索' : '🚩 目的地' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">🕒 时间</span>
          <span class="info-value">{{ formatDate(route.createdAt) }}</span>
        </div>
      </div>

      <button @click="openExternalMap" class="nav-btn">
        🚀 用高德地图导航
      </button>

      <button @click="shareRoute" class="share-btn">
        📤 分享路线
      </button>
    </div>
  </div>
</template>

<script>
import api from '@/utils/axios';
import AMapLoader from '@amap/amap-jsapi-loader';

export default {
  name: 'RouteDetailView',
  props: {
    route: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      AMap: null,
      map: null
    };
  },
  mounted() {
    this.initMap();
  },
  beforeDestroy() {
    if (this.map) {
      this.map.destroy();
    }
  },
  methods: {
    async initMap() {
      try {
        const configRes = await api.get('/api/config/amap');
        const config = configRes.data;

        window._AMapSecurityConfig = {
          securityJsCode: config.securityCode
        };

        this.AMap = await AMapLoader.load({
          key: config.key,
          version: '2.0',
          plugins: ['AMap.Driving']
        });

        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        this.map = new this.AMap.Map('detail-map-container', {
          viewMode: '3D',
          zoom: 13,
          mapStyle: isDark ? 'amap://styles/dark' : 'amap://styles/normal'
        });

        // Use stored coordinates directly
        const hasStart = this.route.startLat != null && this.route.startLon != null;
        const hasEnd = this.route.endLat != null && this.route.endLon != null;

        if (hasStart && hasEnd) {
          const startPos = [this.route.startLon, this.route.startLat];
          const endPos = [this.route.endLon, this.route.endLat];

          // Add markers
          new this.AMap.Marker({
            position: startPos,
            content: '<div style="font-size:28px">📍</div>',
            offset: new this.AMap.Pixel(-14, -28),
            map: this.map
          });

          new this.AMap.Marker({
            position: endPos,
            content: '<div style="font-size:28px">🏁</div>',
            offset: new this.AMap.Pixel(-14, -28),
            map: this.map
          });

          // Plan driving route
          const driving = new this.AMap.Driving({ map: this.map });
          driving.search(startPos, endPos, (status) => {
            if (status === 'complete') {
              setTimeout(() => {
                this.map.setFitView(null, false, [60, 40, 60, 40]);
              }, 200);
            }
          });
        } else if (hasEnd) {
          // Only end coordinate available
          this.map.setZoomAndCenter(15, [this.route.endLon, this.route.endLat]);
          new this.AMap.Marker({
            position: [this.route.endLon, this.route.endLat],
            content: '<div style="font-size:28px">🏁</div>',
            offset: new this.AMap.Pixel(-14, -28),
            map: this.map
          });
        } else if (hasStart) {
          // Only start coordinate available
          this.map.setZoomAndCenter(15, [this.route.startLon, this.route.startLat]);
          new this.AMap.Marker({
            position: [this.route.startLon, this.route.startLat],
            content: '<div style="font-size:28px">📍</div>',
            offset: new this.AMap.Pixel(-14, -28),
            map: this.map
          });
        }
        // else: no coordinates, show default Beijing map
      } catch (error) {
        console.error('地图加载失败:', error);
      }
    },

    openExternalMap() {
      if (this.route.endLat != null && this.route.endLon != null) {
        const eLon = Number(this.route.endLon).toFixed(6);
        const eLat = Number(this.route.endLat).toFixed(6);
        const eName = encodeURIComponent(this.route.endLocation || '探索终点');
        window.location.href = `https://uri.amap.com/navigation?to=${eLon},${eLat},${eName}&mode=car&src=WildPointer&coordinate=gaode&callnative=1`;
      } else {
        const keyword = encodeURIComponent(this.route.endLocation || '目的地');
        window.location.href = `https://uri.amap.com/search?keyword=${keyword}&src=WildPointer`;
      }
    },

    async shareRoute() {
      const text = `我用 Wild Pointer 探索了一条路线：${this.route.name}，距离 ${this.route.distance}，耗时 ${this.route.duration}`;
      if (navigator.share) {
        try {
          await navigator.share({ title: 'Wild Pointer 路线分享', text });
          // eslint-disable-next-line no-empty
        } catch (e) {}
      } else {
        try {
          await navigator.clipboard.writeText(text);
          alert('路线信息已复制到剪贴板');
        } catch (e) {
          alert(text);
        }
      }
    },

    formatDate(dateString) {
      if (!dateString) return '未知';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN');
    }
  }
};
</script>

<style scoped>
.route-detail-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-color);
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.back-btn {
  background: none;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
}

.back-btn:active {
  background: var(--accent-color);
  color: white;
}

.detail-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-map-wrapper {
  flex-shrink: 0;
  height: 300px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
}

#detail-map-container {
  width: 100%;
  height: 100%;
}

.detail-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  background: var(--card-bg);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.info-value {
  font-size: 0.95rem;
  color: var(--text-primary);
  font-weight: 600;
  word-break: break-all;
}

.nav-btn {
  width: 100%;
  padding: 12px;
  background: #e67e22;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
}

.share-btn {
  width: 100%;
  padding: 12px;
  background: var(--btn-bg);
  border: 1px solid var(--accent-color);
  color: var(--accent-color);
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
}

.share-btn:active {
  background: var(--accent-color);
  color: white;
}
</style>
