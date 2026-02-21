<template>
  <div class="dashboard">
    <div class="sidebar">
      <h2>Audit System</h2>
      <nav>
        <router-link to="/dashboard/servers">Servers</router-link>
        <router-link to="/dashboard/logs">Audit Logs</router-link>
        <router-link to="/dashboard/users" v-if="user.role === 'ADMIN'">User Management</router-link>
      </nav>
      <div class="user-info">
        <p>{{ user.username }} ({{ user.role }})</p>
        <button @click="logout">Logout</button>
      </div>
    </div>
    <div class="content">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      user: JSON.parse(localStorage.getItem('user') || '{}')
    };
  },
  methods: {
    logout() {
      localStorage.removeItem('user');
      this.$router.push('/');
    }
  }
};
</script>

<style scoped>
.dashboard {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 250px;
  background-color: #2c3e50;
  color: white;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}
.content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}
nav a {
  display: block;
  color: white;
  text-decoration: none;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 4px;
}
nav a.router-link-active {
  background-color: #34495e;
}
.user-info {
  margin-top: auto;
  border-top: 1px solid #34495e;
  padding-top: 1rem;
}
button {
  background: none;
  border: 1px solid white;
  color: white;
  padding: 0.5rem;
  width: 100%;
  cursor: pointer;
  margin-top: 0.5rem;
}
</style>
