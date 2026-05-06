<template>
  <div class="profile-view">
    <div class="profile-content">
      <!-- 用户信息头 -->
      <div class="profile-header">
        <div class="avatar-section" @click="$refs.avatarInput.click()">
          <img v-if="user.avatar" :src="user.avatar" class="avatar-img" alt="头像"/>
          <div v-else class="avatar">{{ user.nickname?.charAt(0) || 'U' }}</div>
          <div class="avatar-overlay">📷</div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            style="display:none"
            @change="handleAvatarUpload"
          />
        </div>
        <div class="user-basic">
          <h2>{{ user.nickname || user.username }}</h2>
          <p class="email">📧 {{ user.email }}</p>
          <p class="join-time">📅 加入于 {{ formatDate(user.createdAt) }}</p>
        </div>
      </div>

      <!-- 数据统计 -->
      <div class="stats-section">
        <h3>📊 数据统计</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalRoutes }}</div>
            <div class="stat-label">探索路线</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalFavorites }}</div>
            <div class="stat-label">收藏数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalDistance }}</div>
            <div class="stat-label">总里程</div>
          </div>
        </div>
      </div>

      <!-- GPX 迷雾数据上传 -->
      <div class="settings-section">
        <h3>🗺️ 探索迷雾</h3>
        <div class="fog-stats">
          <div class="stat-item">
            <div class="stat-value">{{ fogStats.totalCells || 0 }}</div>
            <div class="stat-label">已探索网格</div>
          </div>
        </div>
        <div class="setting-item">
          <button @click="$refs.gpxInput.click()" class="save-btn gpx-upload-btn" :disabled="uploadingGpx">
            {{ uploadingGpx ? '上传中...' : '📤 上传 GPX 文件' }}
          </button>
          <input
            ref="gpxInput"
            type="file"
            accept=".gpx"
            style="display:none"
            @change="handleGpxUpload"
          />
          <p class="hint-text">支持世界迷雾导出的 .gpx 格式，最大 15MB</p>
        </div>
      </div>

      <!-- 个人资料编辑 -->
      <div class="settings-section">
        <h3>个人资料</h3>
        <div class="setting-item">
          <label>昵称</label>
          <input v-model="editForm.nickname" type="text" placeholder="输入昵称"/>
          <button @click="updateProfile" class="save-btn">保存</button>
        </div>

        <div class="setting-item">
          <label>邮箱</label>
          <input v-model="editForm.email" type="email" placeholder="输入邮箱"/>
          <button @click="updateProfile" class="save-btn">保存</button>
        </div>

        <div class="setting-item">
          <label>个人简介</label>
          <textarea v-model="editForm.bio" placeholder="输入个人简介" rows="3"></textarea>
          <button @click="updateProfile" class="save-btn">保存</button>
        </div>
      </div>

      <!-- 偏好设置 -->
      <div class="settings-section">
        <h3>偏好设置</h3>
        <div class="setting-item">
          <label>默认探索距离 (km)</label>
          <input v-model.number="editForm.defaultDistance" type="number" min="1" placeholder="例如：10"/>
          <button @click="updatePreferences" class="save-btn">保存</button>
        </div>

        <div class="setting-item">
          <label>默认探索时长 (分钟)</label>
          <input v-model.number="editForm.defaultDuration" type="number" min="1" placeholder="例如：30"/>
          <button @click="updatePreferences" class="save-btn">保存</button>
        </div>

        <div class="setting-item color-setting">
          <label>迷雾路径颜色</label>
          <div class="color-row">
            <div class="color-presets">
              <span v-for="c in presetColors" :key="c"
                class="color-dot" :style="{ background: c }"
                :class="{ active: fogColor === c }"
                @click="$emit('update-fog-color', c)">
              </span>
            </div>
            <input type="color" :value="fogColor" @input="$emit('update-fog-color', $event.target.value)" class="color-picker"/>
          </div>
        </div>
      </div>

      <!-- 账户安全 -->
      <div class="settings-section">
        <h3>账户安全</h3>
        <div class="setting-item">
          <label>当前密码</label>
          <input v-model="passwordForm.currentPassword" type="password" placeholder="输入当前密码"/>
        </div>

        <div class="setting-item">
          <label>新密码</label>
          <input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码"/>
        </div>

        <div class="setting-item">
          <label>确认新密码</label>
          <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码"/>
          <button @click="changePassword" class="save-btn">修改密码</button>
        </div>
      </div>

      <!-- 消息提示 -->
      <p v-if="message" :class="['message', messageType]">{{ message }}</p>

      <!-- 登出按钮 -->
      <div class="logout-section">
        <button @click="logout" class="logout-btn">🚪 登出</button>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/utils/axios';

export default {
  name: 'ProfileView',
  props: {
    user: {
      type: Object,
      default: null
    },
    fogColor: {
      type: String,
      default: '#42b983'
    }
  },
  data() {
    return {
      editForm: {
        nickname: '',
        email: '',
        bio: '',
        defaultDistance: null,
        defaultDuration: null
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      stats: {
        totalRoutes: 0,
        totalFavorites: 0,
        totalDistance: '0 km'
      },
      presetColors: ['#42b983', '#3498db', '#9b59b6', '#e67e22', '#e74c3c', '#1abc9c'],
      fogStats: { totalCells: 0 },
      uploadingGpx: false,
      message: '',
      messageType: ''
    };
  },
  watch: {
    user(val) {
      if (val) {
        this.editForm = {
          nickname: val.nickname || '',
          email: val.email || '',
          bio: val.bio || '',
          defaultDistance: val.defaultDistance || null,
          defaultDuration: val.defaultDuration || null
        };
      }
    }
  },
  mounted() {
    if (this.user) {
      this.editForm = {
        nickname: this.user.nickname || '',
        email: this.user.email || '',
        bio: this.user.bio || '',
        defaultDistance: this.user.defaultDistance || null,
        defaultDuration: this.user.defaultDuration || null
      };
      this.loadStats();
      this.loadFogStats();
    }
  },
  methods: {
    async loadStats() {
      try {
        const res = (await api.get('/api/auth/stats')).data;
        if (res.success) {
          this.stats = res.data;
        }
      } catch (error) {
        console.error('加载统计失败:', error);
      }
    },

    async loadFogStats() {
      try {
        const res = (await api.get('/api/fog/stats')).data;
        if (res.success) this.fogStats = res.data;
      } catch (error) {
        console.error('加载迷雾统计失败:', error);
      }
    },

    async handleGpxUpload(event) {
      const file = event.target.files?.[0];
      if (!file) return;
      this.uploadingGpx = true;
      const formData = new FormData();
      formData.append('file', file);
      try {
        const res = (await api.post('/api/fog/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })).data;
        if (!res.success) throw new Error(res.message);
        this.fogStats.totalCells = res.data.uniqueCells;
        this.message = `✅ 上传成功: ${res.data.uniqueCells} 个网格已加载`;
        this.messageType = 'success';
        this.$emit('fog-uploaded');
      } catch (error) {
        this.message = '❌ 上传失败: ' + error.message;
        this.messageType = 'error';
      }
      this.uploadingGpx = false;
      event.target.value = '';
      setTimeout(() => this.message = '', 3000);
    },

    async updateProfile() {
      if (!this.user) return;
      try {
        const res = (await api.patch('/api/auth/profile', {
            nickname: this.editForm.nickname,
            email: this.editForm.email,
            bio: this.editForm.bio
        })).data;
        if (!res.success) throw new Error(res.message);
        this.message = '✅ 资料已更新';
        this.messageType = 'success';
        this.$emit('update-user', {
          ...this.user,
          nickname: this.editForm.nickname,
          email: this.editForm.email,
          bio: this.editForm.bio
        });
      } catch (error) {
        this.message = '❌ 更新失败: ' + error.message;
        this.messageType = 'error';
      }
      setTimeout(() => this.message = '', 3000);
    },

    async updatePreferences() {
      if (!this.user) return;
      try {
        const res = (await api.patch('/api/auth/preferences', {
            defaultDistance: this.editForm.defaultDistance,
            defaultDuration: this.editForm.defaultDuration
        })).data;
        if (!res.success) throw new Error(res.message);
        this.message = '✅ 偏好设置已更新';
        this.messageType = 'success';
      } catch (error) {
        this.message = '❌ 更新失败: ' + error.message;
        this.messageType = 'error';
      }
      setTimeout(() => this.message = '', 3000);
    },

    async changePassword() {
      if (!this.passwordForm.currentPassword || !this.passwordForm.newPassword) {
        this.message = '❌ 请填写所有密码字段';
        this.messageType = 'error';
        return;
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.message = '❌ 两次输入的新密码不一致';
        this.messageType = 'error';
        return;
      }
      try {
        const res = (await api.post('/api/auth/change-password', {
            currentPassword: this.passwordForm.currentPassword,
            newPassword: this.passwordForm.newPassword
        })).data;
        if (!res.success) throw new Error(res.message);
        this.message = '✅ 密码已修改';
        this.messageType = 'success';
        this.passwordForm = {
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        };
      } catch (error) {
        this.message = '❌ 修改失败: ' + error.message;
        this.messageType = 'error';
      }
      setTimeout(() => this.message = '', 3000);
    },

    async handleAvatarUpload(event) {
      const file = event.target.files?.[0];
      if (!file) return;

      const formData = new FormData();
      formData.append('file', file);

      try {
        const res = (await api.post('/api/auth/avatar', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })).data;
        if (!res.success) throw new Error(res.message);

        this.message = '✅ 头像已更新';
        this.messageType = 'success';
        this.$emit('update-user', {
          ...this.user,
          avatar: res.data
        });
      } catch (error) {
        this.message = '❌ 上传失败: ' + error.message;
        this.messageType = 'error';
      }
      setTimeout(() => this.message = '', 3000);
    },

    logout() {
      if (confirm('确定要登出吗？')) {
        this.$emit('logout');
      }
    },

    formatDate(dateString) {
      if (!dateString) return '未知';
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN');
    }
  }
};
</script>

<style scoped>
.profile-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  background: var(--bg-color);
  overflow-y: auto;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 个人信息头 */
.profile-header {
  display: flex;
  gap: 16px;
  background: var(--card-bg);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.avatar-section {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
  width: 80px;
  height: 80px;
}

.avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #42b983, #3aa375);
  color: white;
  font-size: 32px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: white;
  border: 2px solid var(--accent-color);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.avatar-section:hover .avatar-overlay {
  transform: scale(1.1);
}

.user-basic {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.user-basic h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.2rem;
}

.user-basic p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

/* 数据统计 */
.stats-section {
  background: var(--card-bg);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.stats-section h3 {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  font-size: 1rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 12px 8px;
  background: var(--bg-color);
  border-radius: 8px;
}

.stat-value {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--accent-color);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

/* 设置分区 */
.settings-section {
  background: var(--card-bg);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.settings-section h3 {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  font-size: 1rem;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.setting-item input,
.setting-item textarea {
  padding: 10px;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 6px;
  font-size: 0.9rem;
  font-family: inherit;
  transition: border-color 0.3s;
}

.setting-item input:focus,
.setting-item textarea:focus {
  outline: none;
  border-color: var(--accent-color);
}

.color-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-presets {
  display: flex;
  gap: 8px;
}

.color-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.2s;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: var(--text-primary);
}

.color-picker {
  width: 40px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  background: none;
}

.save-btn {
  padding: 8px 12px;
  background: var(--btn-bg);
  border: 1px solid var(--accent-color);
  color: var(--accent-color);
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
  transition: all 0.3s;
  align-self: flex-start;
}

.save-btn:active {
  background: var(--accent-color);
  color: white;
}

/* 消息提示 */
.message {
  padding: 12px;
  border-radius: 6px;
  font-size: 0.9rem;
  text-align: center;
}

.message.success {
  background: rgba(66, 185, 131, 0.1);
  color: var(--accent-color);
}

.message.error {
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.message.info {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

/* 登出区域 */
.logout-section {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

.logout-btn {
  padding: 12px 24px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}

.logout-btn:active {
  opacity: 0.8;
}

/* 探索迷雾 */
.fog-stats {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.gpx-upload-btn {
  width: 100%;
  background: var(--accent-color);
  color: white;
  border: none;
}

.gpx-upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hint-text {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin: 4px 0 0 0;
}
</style>
