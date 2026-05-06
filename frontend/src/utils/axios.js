import axios from 'axios';

const api = axios.create();

// 请求拦截器：自动附加 JWT token
api.interceptors.request.use(config => {
    const token = localStorage.getItem('wildpointer_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 响应拦截器：401 自动登出
api.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('wildpointer_token');
            localStorage.removeItem('wildpointer_user');
            window.location.reload();
        }
        return Promise.reject(error);
    }
);

export default api;
