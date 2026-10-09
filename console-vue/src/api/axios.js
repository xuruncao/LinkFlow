import axios from 'axios'
import {getToken, getUsername, removeKey, removeUsername} from '@/core/auth.js'
import {isNotEmpty} from '@/utils/plugins.js'
import router from "@/router";
import { ElMessage } from 'element-plus'

// const router = useRouter()
const baseURL = '/api/short-link/admin/v1'
// 创建实例
const http = axios.create({
    // api 代理为服务器请求地址
    baseURL: baseURL,
    timeout: 15000
})
// 请求拦截 -->在请求发送之前做一些事情
http.interceptors.request.use(
    (config) => {
        config.headers.Token = isNotEmpty(getToken()) ? getToken() : ''
        config.headers.Username = isNotEmpty(getUsername()) ? getUsername() : ''
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)
// 响应拦截 -->在返回结果之前做一些事情
http.interceptors.response.use(
    (res) => {
        if (res.status == 0 || res.status == 200) {
            // 请求成功对响应数据做处理，此处返回的数据是axios.then(res)中接收的数据
            // code值为 0 或 200 时视为成功
            return Promise.resolve(res)
        }
        return Promise.reject(res)
    },
    (err) => {
        // 在请求错误时要做的事儿
        // 此处返回的数据是axios.catch(err)中接收的数据
        if (err.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('username')
            removeKey()
            removeUsername()
            if (router.currentRoute.value.path !== '/login') {
                router.push('/login')
            }
            ElMessage.error('登录已过期，请重新登录')
        } else if (!err.response) {
            ElMessage.error(err.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '无法连接服务，请检查网络或后端服务是否启动')
        } else {
            ElMessage.error(err.response.data?.message || '服务暂时不可用，请稍后重试')
        }
        return Promise.reject(err)
    }
)
export default http
