<template>
  <nav class="bottom-tab-bar">
    <button
      v-for="tab in tabs"
      :key="tab.id"
      :class="['tab-button', { active: activeTab === tab.id }]"
      @click="$emit('tab-change', tab.id)"
    >
      <span class="tab-icon">{{ tab.icon }}</span>
      <span class="tab-label">{{ tab.label }}</span>
    </button>
  </nav>
</template>

<script>
export default {
  name: 'BottomTabBar',
  props: {
    activeTab: {
      type: String,
      default: 'map'
    }
  },
  data() {
    return {
      tabs: [
        { id: 'map', icon: '🗺️', label: '发现' },
        { id: 'history', icon: '📋', label: '历史' },
        { id: 'favorite', icon: '⭐', label: '收藏' },
        { id: 'profile', icon: '👤', label: '我的' }
      ]
    };
  }
};
</script>

<style scoped>
.bottom-tab-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 60px;
  background: var(--card-bg);
  border-top: 1px solid var(--border-color);
  gap: 0;
  padding-bottom: calc(env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
  z-index: 200;
  box-shadow: 0 -2px 8px var(--shadow-color);
}

.tab-button {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px;
  font-size: 0.75rem;
  height: 100%;
  box-sizing: border-box;
}

.tab-button:active {
  background: rgba(66, 185, 131, 0.05);
}

.tab-button.active {
  color: var(--accent-color);
  font-weight: 600;
}

.tab-button.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--accent-color);
}

.tab-icon {
  font-size: 24px;
  display: block;
}

.tab-label {
  font-size: 0.65rem;
  line-height: 1;
  white-space: nowrap;
}

/* 深色模式支持 */
[data-theme="dark"] .bottom-tab-bar {
  background: var(--card-bg);
  border-top-color: var(--border-color);
}

[data-theme="dark"] .tab-button:active {
  background: rgba(66, 185, 131, 0.1);
}

/* 平板和桌面适配 */
@media (min-width: 768px) {
  .bottom-tab-bar {
    height: 70px;
  }

  .tab-button {
    gap: 6px;
  }

  .tab-icon {
    font-size: 28px;
  }

  .tab-label {
    font-size: 0.8rem;
  }
}
</style>
