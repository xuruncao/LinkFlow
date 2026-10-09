import { createRouter, createWebHistory } from 'vue-router'
import { isNotEmpty } from '@/utils/plugins'
import { setToken, setUsername, removeKey, removeUsername } from '@/core/auth' // 验权
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/login',
      name: 'LoginIndex',
      component: () => import('@/views/login/LoginIndex.vue')
    },
    {
      path: '/home',
      name: 'LayoutIndex',
      redirect: '/home/space',
      component: () => import('@/views/home/HomeIndex.vue'),
      children: [
        {
          // 前面不能加/
          path: 'space',
          name: 'MySpace',
          component: () => import('@/views/mySpace/MySpaceIndex.vue'),
          meta: { title: '我的空间' }
        },
        {
          path: 'recycleBin',
          name: 'RecycleBin',
          component: () => import('@/views/recycleBin/RecycleBinIndex.vue'),
          meta: { title: '账户设置' }
        },
        {
          path: 'account',
          name: 'Mine',
          component: () => import('@/views/mine/MineIndex.vue'),
          meta: { title: '个人中心' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const username = localStorage.getItem('username')
  const hasSession = isNotEmpty(token) && token !== 'null' && token !== 'undefined'
    && isNotEmpty(username) && username !== 'null' && username !== 'undefined'
  if (hasSession) {
    setToken(token)
    setUsername(username)
  } else {
    removeKey()
    removeUsername()
  }
  return to.path === '/login' || hasSession ? true : '/login'
})

export default router
