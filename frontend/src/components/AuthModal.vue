<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <button class="modal-close" @click="$emit('close')">✕</button>

      <!-- 登录标签页 -->
      <div v-if="mode === 'login'" class="auth-form">
        <h2>登录</h2>
        <input v-model="form.username" type="text" placeholder="用户名" class="input-field" />
        <input v-model="form.password" type="password" placeholder="密码" class="input-field" />
        <button @click="submitLogin" class="auth-btn">登录</button>
        <p class="toggle-auth">
          还没有账户？<a href="#" @click.prevent="mode = 'register'">立即注册</a>
        </p>
      </div>

      <!-- 注册标签页 -->
      <div v-if="mode === 'register'" class="auth-form">
        <h2>注册</h2>
        <input v-model="form.username" type="text" placeholder="用户名" class="input-field" />
        <input v-model="form.email" type="email" placeholder="邮箱" class="input-field" />
        <input v-model="form.password" type="password" placeholder="密码" class="input-field" />
        <input v-model="form.nickname" type="text" placeholder="昵称 (可选)" class="input-field" />
        <button @click="submitRegister" class="auth-btn">注册</button>
        <p class="toggle-auth">
          已有账户？<a href="#" @click.prevent="mode = 'login'">立即登录</a>
        </p>
      </div>

      <!-- 错误提示 -->
      <p v-if="error" class="auth-error">{{ error }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AuthModal',
  data() {
    return {
      mode: 'login',
      form: {
        username: '',
        email: '',
        password: '',
        nickname: ''
      },
      error: ''
    };
  },
  methods: {
    async submitLogin() {
      this.error = '';
      if (!this.form.username || !this.form.password) {
        this.error = '用户名和密码不能为空';
        return;
      }
      try {
        await this.$emit('login', {
          username: this.form.username,
          password: this.form.password
        });
      } catch (err) {
        this.error = err.message;
      }
    },

    async submitRegister() {
      this.error = '';
      if (!this.form.username || !this.form.email || !this.form.password) {
        this.error = '用户名、邮箱和密码不能为空';
        return;
      }
      try {
        await this.$emit('register', {
          username: this.form.username,
          email: this.form.email,
          password: this.form.password,
          nickname: this.form.nickname || this.form.username
        });
        this.mode = 'login';
        this.form = { username: '', email: '', password: '', nickname: '' };
        this.error = '注册成功，请登录';
      } catch (err) {
        this.error = err.message;
      }
    }
  }
};
</script>

<style scoped>
/* 弹窗覆盖层 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal-content {
  position: relative;
  background: var(--card-bg);
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 400px;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-close {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.modal-close:active {
  color: var(--text-primary);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.auth-form h2 {
  margin: 0 0 10px 0;
  color: var(--text-primary);
  text-align: center;
  font-size: 1.3rem;
}

.input-field {
  padding: 10px 12px;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: var(--accent-color);
}

.auth-btn {
  padding: 12px;
  background: var(--accent-color);
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  font-size: 0.95rem;
  transition: opacity 0.2s;
}

.auth-btn:active {
  opacity: 0.8;
}

.toggle-auth {
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0;
}

.toggle-auth a {
  color: var(--accent-color);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.toggle-auth a:active {
  color: #3aa375;
}

.auth-error {
  color: #ff6b6b;
  font-size: 0.85rem;
  text-align: center;
  margin: 10px 0 0 0;
  padding: 8px;
  background: rgba(255, 107, 107, 0.1);
  border-radius: 4px;
}
</style>
