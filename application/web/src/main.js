import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
// 导入视图组件
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import ServerList from './views/ServerList.vue'
import AuditLogs from './views/AuditLogs.vue'
import UserList from './views/UserList.vue'

// 定义前端路由规则
const routes = [
  { path: '/', component: Login }, // 默认根路径为登录页
  { 
    path: '/dashboard', 
    component: Dashboard, // 主控制台视图
    children: [
      // 控制台的子路由页面
      { path: 'servers', component: ServerList }, // 服务器列表页
      { path: 'logs', component: AuditLogs },     // 审计日志页
      { path: 'users', component: UserList }      // 用户管理页
    ]
  }
]

// 创建路由实例，使用 HTML5 history 模式
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 创建并挂载 Vue 应用实例
createApp(App).use(router).mount('#app')
