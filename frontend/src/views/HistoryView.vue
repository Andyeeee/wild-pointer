<template>
  <div class="history-view">
    <div class="history-header">
      <h2>📋 历史记录</h2>
      <p v-if="routes.length === 0" class="empty-hint">还没有历史记录，去发现页探索吧！</p>
    </div>

    <div class="routes-list">
      <div v-for="route in routes" :key="route.id" class="route-item">
        <div class="route-info">
          <div class="route-name">{{ route.name }}</div>
          <div class="route-details">
            <span class="route-time">🕒 {{ formatDate(route.createdAt) }}</span>
            <span class="route-type" :class="route.routeType.toLowerCase()">
              {{ route.routeType === 'RANDOM' ? '🎲 随机' : '🚩 目的地' }}
            </span>
          </div>
          <div class="route-meta">
            <span>📏 {{ route.distance }}</span>
            <span>🧭 {{ route.duration }}</span>
          </div>
        </div>
        <div class="route-actions">
          <button @click="deleteRoute(route.id)" class="delete-btn" title="删除">🗑️</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'HistoryView',
  props: {
    user: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      routes: []
    };
  },
  mounted() {
    this.loadRoutes();
  },
  methods: {
    async loadRoutes() {
      if (!this.user) {
        this.routes = [];
        return;
      }
      try {
        const response = await axios.get(`/api/routes/user/${this.user.userId}`);
        this.routes = response.data || [];
      } catch (error) {
        console.error('加载历史记录失败:', error);
        this.routes = [];
      }
    },

    async deleteRoute(routeId) {
      if (!confirm('确定要删除这条记录吗？')) return;
      try {
        await axios.delete(`/api/routes/${routeId}`, {
          params: { userId: this.user.userId }
        });
        this.routes = this.routes.filter(r => r.id !== routeId);
      } catch (error) {
        alert('删除失败');
      }
    },

    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  }
};
</script>

<style scoped>
.history-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  background: var(--bg-color);
}

.history-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.history-header h2 {
  margin: 0 0 8px 0;
  color: var(--text-primary);
  font-size: 1.3rem;
}

.empty-hint {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0;
}

.routes-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.route-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: var(--card-bg);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.route-item:active {
  background: rgba(66, 185, 131, 0.05);
}

.route-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.route-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.95rem;
}

.route-details {
  display: flex;
  gap: 12px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.route-type {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(66, 185, 131, 0.1);
  color: var(--accent-color);
  font-weight: 600;
}

.route-type.random {
  background: rgba(100, 150, 200, 0.1);
  color: #6496c8;
}

.route-type.destination {
  background: rgba(220, 100, 50, 0.1);
  color: #dc6432;
}

.route-meta {
  display: flex;
  gap: 16px;
  font-size: 0.85rem;
  color: var(--text-primary);
  font-weight: 500;
}

.route-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.delete-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  transition: transform 0.2s;
  padding: 4px;
}

.delete-btn:active {
  transform: scale(1.2);
}
</style>
