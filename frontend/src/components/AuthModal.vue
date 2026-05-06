<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 品牌区域 -->
      <div class="brand">
        <div class="brand-icon">🚀</div>
        <h1 class="brand-name">Wild Pointer</h1>
        <p class="brand-slogan">野指针 — 随机探索，发现未知</p>
      </div>

      <!-- 登录表单 -->
      <div v-if="mode === 'login'" class="auth-form">
        <div class="input-group">
          <span class="input-icon">👤</span>
          <input v-model="form.username" type="text" placeholder="用户名" class="input-field"
                 @keyup.enter="submitLogin" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" type="password" placeholder="密码" class="input-field"
                 @keyup.enter="submitLogin" />
        </div>
        <button @click="submitLogin" class="auth-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
        <div class="auth-links">
          <a href="#" @click.prevent="switchMode('forgot')">忘记密码？</a>
          <span>还没有账户？<a href="#" @click.prevent="switchMode('register')">立即注册</a></span>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-if="mode === 'register'" class="auth-form">
        <div class="input-group">
          <span class="input-icon">👤</span>
          <input v-model="form.username" type="text" placeholder="用户名 (3-50字符)" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">📧</span>
          <input v-model="form.email" type="email" placeholder="邮箱" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" type="password" placeholder="密码 (至少6位)" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">✨</span>
          <input v-model="form.nickname" type="text" placeholder="昵称 (可选)" class="input-field"
                 @keyup.enter="submitRegister" />
        </div>
        <button @click="submitRegister" class="auth-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
        <p class="toggle-auth">
          已有账户？<a href="#" @click.prevent="switchMode('login')">立即登录</a>
        </p>
      </div>

      <!-- 忘记密码表单 -->
      <div v-if="mode === 'forgot'" class="auth-form">
        <p class="forgot-hint">输入注册邮箱，获取验证码重置密码</p>
        <div class="input-group">
          <span class="input-icon">📧</span>
          <input v-model="form.email" type="email" placeholder="注册邮箱" class="input-field" />
        </div>
        <button @click="sendCode" class="auth-btn secondary" :disabled="loading || countdown > 0">
          {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
        </button>
      </div>

      <!-- 重置密码表单 -->
      <div v-if="mode === 'reset'" class="auth-form">
        <div class="input-group">
          <span class="input-icon">🔑</span>
          <input v-model="form.code" type="text" placeholder="6位验证码" class="input-field" maxlength="6" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.newPassword" type="password" placeholder="新密码 (至少6位)" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.confirmPassword" type="password" placeholder="确认新密码" class="input-field"
                 @keyup.enter="resetPassword" />
        </div>
        <button @click="resetPassword" class="auth-btn" :disabled="loading">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>
        <p class="toggle-auth">
          <a href="#" @click.prevent="switchMode('forgot')">返回</a>
        </p>
      </div>

      <!-- 错误/成功提示 -->
      <p v-if="message" :class="['auth-message', messageType]">{{ message }}</p>
    </div>

    <div class="login-version">Wild Pointer v0.5.0 Alpha</div>
  </div>
</template>

<script>
import api from '@/utils/axios';

export default {
  name: 'AuthModal',
  data() {
    return {
      mode: 'login',
      loading: false,
      countdown: 0,
      form: {
        username: '',
        email: '',
        password: '',
        nickname: '',
        code: '',
        newPassword: '',
        confirmPassword: ''
      },
      message: '',
      messageType: 'error'
    };
  },
  methods: {
    switchMode(mode) {
      this.mode = mode;
      this.message = '';
      this.form = { username: '', email: '', password: '', nickname: '', code: '', newPassword: '', confirmPassword: '' };
    },

    async submitLogin() {
      this.message = '';
      if (!this.form.username || !this.form.password) {
        this.message = '用户名和密码不能为空';
        this.messageType = 'error';
        return;
      }
      this.loading = true;
      try {
        await this.$emit('login', {
          username: this.form.username,
          password: this.form.password
        });
      } catch (err) {
        this.message = err.message.replace('登录失败: ', '');
        this.messageType = 'error';
      } finally {
        this.loading = false;
      }
    },

    async submitRegister() {
      this.message = '';
      if (!this.form.username || !this.form.email || !this.form.password) {
        this.message = '用户名、邮箱和密码不能为空';
        this.messageType = 'error';
        return;
      }
      this.loading = true;
      try {
        await this.$emit('register', {
          username: this.form.username,
          email: this.form.email,
          password: this.form.password,
          nickname: this.form.nickname || this.form.username
        });
        this.message = '注册成功，请登录';
        this.messageType = 'success';
        this.mode = 'login';
        this.form = { username: this.form.username, email: '', password: '', nickname: '', code: '', newPassword: '', confirmPassword: '' };
      } catch (err) {
        this.message = err.message.replace('注册失败: ', '');
        this.messageType = 'error';
      } finally {
        this.loading = false;
      }
    },

    async sendCode() {
      this.message = '';
      if (!this.form.email) {
        this.message = '请输入邮箱';
        this.messageType = 'error';
        return;
      }
      this.loading = true;
      try {
        const res = (await api.post('/api/auth/forgot-password', {
          email: this.form.email
        })).data;
        if (!res.success) throw new Error(res.message);
        this.message = '验证码已发送到邮箱';
        this.messageType = 'success';
        this.mode = 'reset';
        this.countdown = 60;
        const timer = setInterval(() => {
          this.countdown--;
          if (this.countdown <= 0) clearInterval(timer);
        }, 1000);
      } catch (err) {
        this.message = err.message;
        this.messageType = 'error';
      } finally {
        this.loading = false;
      }
    },

    async resetPassword() {
      this.message = '';
      if (!this.form.code || !this.form.newPassword || !this.form.confirmPassword) {
        this.message = '请填写所有字段';
        this.messageType = 'error';
        return;
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        this.message = '两次输入的密码不一致';
        this.messageType = 'error';
        return;
      }
      if (this.form.newPassword.length < 6) {
        this.message = '密码长度至少6位';
        this.messageType = 'error';
        return;
      }
      this.loading = true;
      try {
        const res = (await api.post('/api/auth/reset-password', {
          email: this.form.email,
          code: this.form.code,
          newPassword: this.form.newPassword
        })).data;
        if (!res.success) throw new Error(res.message);
        this.message = '密码重置成功，请登录';
        this.messageType = 'success';
        setTimeout(() => this.switchMode('login'), 1500);
      } catch (err) {
        this.message = err.message;
        this.messageType = 'error';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color);
  z-index: 9999;
  overflow: hidden;
}

/* 背景装饰圆 */
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.bg-circle-1 {
  width: 300px;
  height: 300px;
  background: var(--accent-color);
  top: -80px;
  right: -60px;
  animation: float-1 8s ease-in-out infinite;
}

.bg-circle-2 {
  width: 200px;
  height: 200px;
  background: #3498db;
  bottom: -40px;
  left: -40px;
  animation: float-2 10s ease-in-out infinite;
}

.bg-circle-3 {
  width: 150px;
  height: 150px;
  background: var(--accent-color);
  top: 40%;
  left: 60%;
  animation: float-3 6s ease-in-out infinite;
}

@keyframes float-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, 20px); }
}

@keyframes float-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(15px, -15px); }
}

@keyframes float-3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-10px, -20px); }
}

/* 登录卡片 */
.login-card {
  position: relative;
  width: 90%;
  max-width: 380px;
  background: var(--card-bg);
  border-radius: 20px;
  padding: 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 品牌区域 */
.brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: float-logo 3s ease-in-out infinite;
}

@keyframes float-logo {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.brand-name {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 900;
  letter-spacing: 2px;
  background: linear-gradient(135deg, var(--accent-color), #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-slogan {
  margin: 8px 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

/* 表单 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  z-index: 1;
  pointer-events: none;
}

.input-field {
  width: 100%;
  padding: 14px 14px 14px 44px;
  background: var(--input-bg);
  border: 1.5px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 10px;
  font-size: 0.95rem;
  transition: border-color 0.3s, box-shadow 0.3s;
  box-sizing: border-box;
}

.input-field:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.15);
}

.input-field::placeholder {
  color: var(--text-secondary);
  opacity: 0.7;
}

.auth-btn {
  padding: 14px;
  background: linear-gradient(135deg, var(--accent-color), #3aa375);
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  font-size: 1rem;
  letter-spacing: 4px;
  transition: opacity 0.2s, transform 0.1s;
  margin-top: 4px;
}

.auth-btn.secondary {
  background: var(--btn-bg);
  color: var(--accent-color);
  border: 1px solid var(--accent-color);
  letter-spacing: 2px;
}

.auth-btn:active {
  opacity: 0.85;
  transform: scale(0.98);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.auth-links a {
  color: var(--accent-color);
  text-decoration: none;
  font-weight: 600;
}

.auth-links a:active {
  opacity: 0.7;
}

.toggle-auth {
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 4px 0 0;
}

.toggle-auth a {
  color: var(--accent-color);
  text-decoration: none;
  font-weight: 600;
}

.toggle-auth a:active {
  opacity: 0.7;
}

.forgot-hint {
  font-size: 0.85rem;
  color: var(--text-secondary);
  text-align: center;
  margin: 0;
}

/* 消息提示 */
.auth-message {
  font-size: 0.85rem;
  text-align: center;
  margin: 12px 0 0;
  padding: 10px;
  border-radius: 8px;
}

.auth-message.error {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.auth-message.success {
  color: var(--accent-color);
  background: rgba(66, 185, 131, 0.1);
}

.login-version {
  position: absolute;
  bottom: 16px;
  font-size: 11px;
  color: var(--text-secondary);
  opacity: 0.4;
}
</style>
