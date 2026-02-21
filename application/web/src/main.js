import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import ServerList from './views/ServerList.vue'
import AuditLogs from './views/AuditLogs.vue'
import UserList from './views/UserList.vue'

const routes = [
  { path: '/', component: Login },
  { 
    path: '/dashboard', 
    component: Dashboard,
    children: [
      { path: 'servers', component: ServerList },
      { path: 'logs', component: AuditLogs },
      { path: 'users', component: UserList }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp(App).use(router).mount('#app')
