<template>
  <div id="app" :data-theme="isDarkMode ? 'dark' : 'light'">
    <!-- 未登录：显示登录页面 -->
    <AuthModal v-if="!user" @login="handleLogin" @register="handleRegister"></AuthModal>

    <!-- 已登录：显示主界面 -->
    <template v-else>
      <div class="app-container">
        <!-- 头部 -->
        <header class="app-header">
          <div class="logo-wrapper">🚀</div>
          <span class="app-name">Wild Pointer</span>
          <div class="header-right">
            <div class="user-info">
              <span>👤 {{ user.nickname }}</span>
            </div>
          </div>
        </header>

        <!-- 主内容区域 -->
        <main class="app-main">
          <component :is="currentViewComponent" :user="user" :isDarkMode="isDarkMode" :route="selectedRoute" :fog-version="fogVersion" :fog-color="fogColor" @toggle-theme="toggleTheme" @logout="logout" @show-auth="showAuth" @update-user="updateUser" @update-fog-color="updateFogColor" @view-route="viewRoute" @back="backFromRouteDetail" @fog-uploaded="onFogUploaded"></component>
        </main>

        <!-- 底部导航栏 -->
        <BottomTabBar v-if="!selectedRoute" :activeTab="currentTab" @tab-change="switchTab"></BottomTabBar>
      </div>

      <div class="app-version">Wild Pointer v0.5.0 Alpha</div>
    </template>
  </div>
</template>

<script>
import api from '@/utils/axios';
import BottomTabBar from '@/components/BottomTabBar.vue';
import AuthModal from '@/components/AuthModal.vue';
import MapView from '@/views/MapView.vue';
import HistoryView from '@/views/HistoryView.vue';
import FavoriteView from '@/views/FavoriteView.vue';
import ProfileView from '@/views/ProfileView.vue';
import RouteDetailView from '@/views/RouteDetailView.vue';

export default {
  name: 'App',
  components: {
    BottomTabBar,
    AuthModal,
    MapView,
    HistoryView,
    FavoriteView,
    ProfileView,
    RouteDetailView
  },
  data() {
    return {
      currentTab: 'map',
      isDarkMode: false,
      user: null,
      selectedRoute: null,
      fogVersion: 0,
      fogColor: '#42b983'
    };
  },
  computed: {
    currentViewComponent() {
      if (this.selectedRoute) return 'RouteDetailView';
      const views = {
        map: 'MapView',
        history: 'HistoryView',
        favorite: 'FavoriteView',
        profile: 'ProfileView'
      };
      return views[this.currentTab] || 'MapView';
    }
  },
  created() {
    this.initTheme();
    this.loadUserFromStorage();
    const savedColor = localStorage.getItem('wildpointer_fog_color');
    if (savedColor) this.fogColor = savedColor;
  },
  methods: {
    switchTab(tab) {
      this.currentTab = tab;
    },

    initTheme() {
      const savedTheme = localStorage.getItem('user_theme');
      if (savedTheme) {
        this.isDarkMode = savedTheme === 'dark';
      } else {
        const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        this.isDarkMode = systemDark;
      }
      this.applyTheme(this.isDarkMode);
    },

    toggleTheme() {
      this.isDarkMode = !this.isDarkMode;
      localStorage.setItem('user_theme', this.isDarkMode ? 'dark' : 'light');
      this.applyTheme(this.isDarkMode);
    },

    applyTheme(isDark) {
      if (isDark) document.documentElement.setAttribute('data-theme', 'dark');
      else document.documentElement.removeAttribute('data-theme');
    },

    loadUserFromStorage() {
      const userStr = localStorage.getItem('wildpointer_user');
      const token = localStorage.getItem('wildpointer_token');
      if (userStr && token) {
        this.user = JSON.parse(userStr);
      } else {
        localStorage.removeItem('wildpointer_user');
        localStorage.removeItem('wildpointer_token');
      }
    },

    async handleLogin(credentials) {
      const response = await api.post('/api/auth/login', {
        username: credentials.username,
        password: credentials.password
      });

      const res = response.data;
      if (res.success) {
        this.user = res.data;
        localStorage.setItem('wildpointer_user', JSON.stringify(res.data));
        localStorage.setItem('wildpointer_token', res.data.token);
      } else {
        throw new Error(res.message || '登录失败');
      }
    },

    async handleRegister(credentials) {
      const response = await api.post('/api/auth/register', {
        username: credentials.username,
        email: credentials.email,
        password: credentials.password,
        nickname: credentials.nickname || credentials.username
      });

      const res = response.data;
      if (!res.success) {
        throw new Error(res.message || '注册失败');
      }
    },

    showAuth() {
      // 已登录状态下不需要此方法，保留兼容性
    },

    logout() {
      this.user = null;
      localStorage.removeItem('wildpointer_user');
      localStorage.removeItem('wildpointer_token');
      this.currentTab = 'map';
    },

    updateUser(userData) {
      this.user = userData;
      localStorage.setItem('wildpointer_user', JSON.stringify(userData));
    },

    viewRoute(route) {
      this.selectedRoute = route;
    },

    backFromRouteDetail() {
      this.selectedRoute = null;
    },

    onFogUploaded() {
      this.fogVersion++;
    },

    updateFogColor(color) {
      this.fogColor = color;
      localStorage.setItem('wildpointer_fog_color', color);
    }
  }
};
</script>

<style>
/* =========================================
   1. 定义主题变量
   ========================================= */
:root {
  --bg-color: #f0f2f5;
  --card-bg: #ffffff;
  --input-bg: #ffffff;
  --text-primary: #2c3e50;
  --text-secondary: #7f8c8d;
  --border-color: #dcdfe6;
  --accent-color: #42b983;
  --btn-bg: #ecf0f1;
  --shadow-color: rgba(0, 0, 0, 0.1);
}

[data-theme="dark"] {
  --bg-color: #1a1a1a;
  --card-bg: #2c3e50;
  --input-bg: #1a1a1a;
  --text-primary: #ffffff;
  --text-secondary: #bdc3c7;
  --border-color: #555555;
  --accent-color: #42b983;
  --btn-bg: #34495e;
  --shadow-color: rgba(0, 0, 0, 0.5);
}

/* =========================================
   2. 移动端布局基础 (Flex + No Scroll)
   ========================================= */
html, body {
  height: 100%;
  overflow: hidden;
  overscroll-behavior: none;
  box-sizing: border-box;
}

body {
  background: var(--bg-color);
  color: var(--text-primary);
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  transition: background-color 0.3s, color 0.3s;
  box-sizing: border-box;
}

#app {
  height: 100%;
  width: 100%;
  box-sizing: border-box;
}

.app-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  background: var(--bg-color);
  overflow: hidden;
  box-sizing: border-box;
  padding: 10px 10px 0 10px;
  gap: 10px;
}

/* =========================================
   3. 顶部标题栏
   ========================================= */
.app-header {
  flex-shrink: 0;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: transparent;
  z-index: 200;
  padding: 0 10px;
}

.logo-wrapper {
  font-size: 24px;
  animation: float-logo 3s ease-in-out infinite;
}

@keyframes float-logo {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.app-name {
  font-size: 1.2rem;
  font-weight: 900;
  letter-spacing: 1px;
  background: linear-gradient(45deg, var(--accent-color), #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* =========================================
   4. 主内容区域
   ========================================= */
.app-main {
  flex: 1;
  width: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  border-radius: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 0.9rem;
  color: var(--text-primary);
}

.app-version {
  position: fixed;
  bottom: 5px;
  right: 5px;
  font-size: 10px;
  color: var(--text-secondary);
  opacity: 0.5;
  z-index: 999;
  pointer-events: none;
}
</style>
