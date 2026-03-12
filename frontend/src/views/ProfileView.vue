<template>
  <div class="profile-view">
    <div v-if="!user" class="not-logged-in">
      <p>👤 请先登录</p>
      <button @click="$emit('show-auth')" class="login-btn">🔐 去登录</button>
    </div>

    <div v-else class="profile-content">
      <!-- 用户信息头 -->
      <div class="profile-header">
        <div class="avatar-section">
          <div class="avatar">{{ user.nickname?.charAt(0) || 'U' }}</div>
          <button @click="showAvatarInput = true" class="avatar-btn">📷</button>
          <input
            v-show="false"
            ref="avatarInput"
            type="file"
            accept="image/*"
            @change="handleAvatarUpload"
          />
        </div>
        <div class="user-basic">
          <h2>{{ user.nickname || user.username }}</h2>
          <p class="email">📧 {{ user.email }}</p>
          <p class="join-time">📅 加入于 {{ formatDate(user.createdAt) }}</p>
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
import axios from 'axios';

export default {
  name: 'ProfileView',
  props: {
    user: {
      type: Object,
      default: null
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
      message: '',
      messageType: '',
      showAvatarInput: false
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
    }
  },
  methods: {
    async updateProfile() {
      if (!this.user) return;
      try {
        await axios.patch(`/api/auth/profile/${this.user.userId}`, null, {
          params: {
            nickname: this.editForm.nickname,
            email: this.editForm.email,
            bio: this.editForm.bio
          }
        });
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
        await axios.patch(`/api/auth/preferences/${this.user.userId}`, null, {
          params: {
            defaultDistance: this.editForm.defaultDistance,
            defaultDuration: this.editForm.defaultDuration
          }
        });
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
        await axios.post('/api/auth/change-password', null, {
          params: {
            userId: this.user.userId,
            currentPassword: this.passwordForm.currentPassword,
            newPassword: this.passwordForm.newPassword
          }
        });
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

    handleAvatarUpload(event) {
      const file = event.target.files?.[0];
      if (!file) return;
      // TODO: 实现头像上传逻辑
      this.message = '📷 头像上传功能开发中';
      this.messageType = 'info';
      setTimeout(() => this.message = '', 2000);
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

.not-logged-in {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 20px;
  text-align: center;
}

.not-logged-in p {
  color: var(--text-secondary);
  font-size: 1.1rem;
}

.login-btn {
  padding: 12px 24px;
  background: var(--accent-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}

.login-btn:active {
  opacity: 0.8;
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

.avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: white;
  border: 2px solid var(--accent-color);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.avatar-btn:active {
  transform: scale(0.9);
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
</style>
