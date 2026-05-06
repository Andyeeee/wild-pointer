<template>
  <div class="favorite-view">
    <div class="favorite-header">
      <h2>⭐ 我的收藏</h2>
      <p v-if="favorites.length === 0 && !loading" class="empty-hint">还没有收藏，在发现页收藏喜欢的地点吧！</p>
    </div>

    <div class="favorites-list">
      <div v-for="fav in favorites" :key="fav.id" class="favorite-item">
        <div class="favorite-info">
          <div class="favorite-name">{{ fav.name }}</div>
          <div class="favorite-details">
            <span class="favorite-type" :class="fav.type.toLowerCase()">
              {{ fav.type === 'PLACE' ? '📍 地点' : '🗺️ 路线' }}
            </span>
            <span class="favorite-time">{{ formatDate(fav.createdAt) }}</span>
          </div>
        </div>
        <div class="favorite-actions">
          <button @click="deleteFavorite(fav.id)" class="delete-btn" title="删除">🗑️</button>
        </div>
      </div>

      <div v-if="loading" class="loading-hint">加载中...</div>

      <div v-if="hasMore && !loading" class="load-more">
        <button @click="loadMore" class="load-more-btn">加载更多</button>
      </div>

      <div v-if="favorites.length > 0 && !hasMore" class="no-more">
        — 已加载全部 —
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/utils/axios';

export default {
  name: 'FavoriteView',
  props: {
    user: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      favorites: [],
      page: 1,
      size: 10,
      total: 0,
      loading: false
    };
  },
  computed: {
    hasMore() {
      return this.favorites.length < this.total;
    }
  },
  mounted() {
    this.loadFavorites();
  },
  methods: {
    async loadFavorites() {
      if (!this.user) {
        this.favorites = [];
        return;
      }
      this.loading = true;
      try {
        const response = await api.get('/api/favorites', {
          params: { page: this.page, size: this.size }
        });
        const res = response.data;
        this.favorites = res.records || [];
        this.total = res.total || 0;
      } catch (error) {
        console.error('加载收藏失败:', error);
        this.favorites = [];
      } finally {
        this.loading = false;
      }
    },

    async loadMore() {
      if (this.loading || !this.hasMore) return;
      this.page++;
      this.loading = true;
      try {
        const response = await api.get('/api/favorites', {
          params: { page: this.page, size: this.size }
        });
        const res = response.data;
        this.favorites = [...this.favorites, ...(res.records || [])];
        this.total = res.total || 0;
      } catch (error) {
        console.error('加载更多失败:', error);
      } finally {
        this.loading = false;
      }
    },

    async deleteFavorite(favoriteId) {
      if (!confirm('确定要删除这个收藏吗？')) return;
      try {
        await api.delete(`/api/favorites/${favoriteId}`);
        this.favorites = this.favorites.filter(f => f.id !== favoriteId);
        this.total--;
      } catch (error) {
        alert('删除失败');
      }
    },

    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN', {
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
.favorite-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  background: var(--bg-color);
}

.favorite-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.favorite-header h2 {
  margin: 0 0 8px 0;
  color: var(--text-primary);
  font-size: 1.3rem;
}

.empty-hint {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0;
}

.favorites-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: var(--card-bg);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.favorite-item:active {
  background: rgba(66, 185, 131, 0.05);
}

.favorite-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.favorite-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.95rem;
}

.favorite-details {
  display: flex;
  gap: 12px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.favorite-type {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(66, 185, 131, 0.1);
  color: var(--accent-color);
  font-weight: 600;
}

.favorite-type.place {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.favorite-type.route {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.favorite-actions {
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

.loading-hint {
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.9rem;
  padding: 12px;
}

.load-more {
  text-align: center;
  padding: 8px;
}

.load-more-btn {
  background: var(--btn-bg);
  border: 1px solid var(--accent-color);
  color: var(--accent-color);
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  font-weight: 600;
}

.load-more-btn:active {
  background: var(--accent-color);
  color: white;
}

.no-more {
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.8rem;
  padding: 12px;
}
</style>
