<template>
  <div class="server-list">
    <h2>{{ isAdmin ? 'Manage Servers' : 'Assigned Servers' }}</h2>
    
    <!-- Admin Controls -->
    <div v-if="isAdmin" class="admin-controls">
      <div class="add-server-form">
        <h3>Add New Server</h3>
        <input v-model="newServer.ip" placeholder="Server IP">
        <input v-model="newServer.sshUser" placeholder="SSH User">
        <input v-model="newServer.sshPassword" placeholder="SSH Password">
        <button @click="addServer">Add Server</button>
      </div>
    </div>

    <div class="card-container">
      <div v-for="server in servers" :key="server.id" class="card">
        <h3>{{ server.ip }}</h3>
        <p><strong>SSH User:</strong> {{ server.sshUser }}</p>
        <p><strong>SSH Password:</strong> {{ server.sshPassword }}</p>
        
        <!-- Admin Assign User -->
        <div v-if="isAdmin" class="assign-user">
          <label>Assign to:</label>
          <select v-model="server.assignedUserId" @change="assignUser(server)">
            <option :value="null">Unassigned</option>
            <option v-for="user in users" :key="user.id" :value="user.id">
              {{ user.username }}
            </option>
          </select>
        </div>

        <div class="actions">
          <button @click="copy(server.sshPassword)">Copy Password</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      servers: [],
      users: [],
      newServer: {
        ip: '',
        sshUser: '',
        sshPassword: ''
      },
      isAdmin: false
    };
  },
  async mounted() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    this.isAdmin = user.role === 'ADMIN';

    try {
      if (this.isAdmin) {
        const [serversRes, usersRes] = await Promise.all([
          axios.get('/api/servers'),
          axios.get('/api/auth/users')
        ]);
        this.servers = serversRes.data;
        this.users = usersRes.data.filter(u => u.role === 'OPS');
      } else {
        const response = await axios.get(`/api/servers/user/${user.id}`);
        this.servers = response.data;
      }
    } catch (error) {
      console.error('Failed to fetch data:', error);
    }
  },
  methods: {
    async addServer() {
      if (!this.newServer.ip || !this.newServer.sshUser || !this.newServer.sshPassword) return;
      try {
        const response = await axios.post('/api/servers', this.newServer);
        this.servers.push(response.data);
        this.newServer = { ip: '', sshUser: '', sshPassword: '' };
        alert('Server added successfully');
      } catch (error) {
        console.error('Failed to add server:', error);
      }
    },
    async assignUser(server) {
      try {
        await axios.put(`/api/servers/${server.id}/assign`, null, {
          params: { userId: server.assignedUserId }
        });
        alert('User assigned successfully');
      } catch (error) {
        console.error('Failed to assign user:', error);
      }
    },
    copy(text) {
      navigator.clipboard.writeText(text);
      alert('Password copied to clipboard');
    }
  }
};
</script>

<style scoped>
.server-list {
  padding: 1rem;
}
.card-container {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 1rem;
}
.card {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  width: 300px;
}
.actions {
  margin-top: 1rem;
  text-align: right;
}
button {
  background-color: #27ae60;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}
.admin-controls {
  margin-bottom: 2rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}
.add-server-form {
  display: flex;
  gap: 1rem;
  align-items: center;
}
input, select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}
.assign-user {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}
</style>
