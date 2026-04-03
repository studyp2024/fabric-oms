<template>
  <div class="server-list">
    <h2>{{ isAdmin ? 'Manage Servers' : 'Assigned Servers' }}</h2>
    <div v-if="isAdmin" class="admin-controls">
      <div class="add-server-form">
        <h3>Add New Server</h3>
        <input v-model="newServer.ip" placeholder="Server IP">
        <input v-model.number="newServer.sshPort" placeholder="SSH Port" type="number" min="1" max="65535">
        <input v-model="newServer.sshUser" placeholder="SSH User">
        <input v-model="newServer.sshPassword" placeholder="SSH Password">
        <button @click="addServer">Add Server</button>
      </div>
    </div>
    <div class="card-container">
      <div v-for="server in servers" :key="server.id" class="card">
        <div class="card-header">
          <h3>{{ server.ip }}</h3>
          <span :class="['status-badge', server.status === 'ONLINE' ? 'online' : 'offline']">
            {{ server.status || 'OFFLINE' }}
          </span>
        </div>
        <div v-if="isAdmin">
          <p><strong>SSH Port:</strong> {{ server.sshPort }}</p>
          <p><strong>SSH User:</strong> {{ server.sshUser }}</p>
          <p><strong>SSH Password:</strong> *******</p>
          <div class="assign-user">
            <label>Assign to (Hold Ctrl/Cmd to select multiple):</label>
            <select multiple v-model="server.assignedUserIds" @change="assignUser(server)" style="height: 80px; width: 100%; margin-top: 5px;">
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.username }}
              </option>
            </select>
          </div>
          <div class="admin-actions" style="margin-top: 10px; display: flex; gap: 10px;">
            <button @click="openEditModal(server)" style="background-color: #f39c12; flex: 1;">Edit</button>
            <button @click="deleteServer(server.id)" style="background-color: #c0392b; flex: 1;">Delete</button>
          </div>
        </div>
        <div v-else>
          <p style="color: #7f8c8d; font-size: 0.9em; margin-top: 10px;">
            You are assigned to manage this server. Direct SSH login credentials are hidden for security reasons.
          </p>
        </div>
        <div class="actions">
          <button @click="connectSSH(server.id)" class="btn-ssh">Connect WebSSH</button>
        </div>
      </div>
    </div>

    <!-- Edit Server Modal -->
    <div v-if="showEditModal" class="modal-overlay">
      <div class="modal-content">
        <h3>Edit Server</h3>
        <div class="form-group">
          <label>IP Address</label>
          <input v-model="editServerData.ip" placeholder="Server IP">
        </div>
        <div class="form-group">
          <label>SSH Port</label>
          <input v-model.number="editServerData.sshPort" type="number" min="1" max="65535">
        </div>
        <div class="form-group">
          <label>SSH User</label>
          <input v-model="editServerData.sshUser" placeholder="SSH User">
        </div>
        <div class="form-group">
          <label>SSH Password</label>
          <input v-model="editServerData.sshPassword" type="password" placeholder="New Password (or leave current)">
        </div>
        <div class="modal-actions">
          <button @click="saveEdit" class="btn-save">Save</button>
          <button @click="closeEditModal" class="btn-cancel">Cancel</button>
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
        sshPort: 22,
        sshUser: '',
        sshPassword: ''
      },
      isAdmin: false,
      showEditModal: false,
      editServerData: {}
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
        this.servers = serversRes.data.map(server => ({
          ...server,
          assignedUserIds: server.assignedUserIds || []
        }));
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
      if (!this.newServer.ip || !this.newServer.sshPort || !this.newServer.sshUser || !this.newServer.sshPassword) {
        alert('Please fill in all fields');
        return;
      }
      try {
        const response = await axios.post('/api/servers', this.newServer);
        this.servers.push(response.data);
        this.newServer = { ip: '', sshPort: 22, sshUser: '', sshPassword: '' };
        alert('Server added successfully');
      } catch (error) {
        console.error('Failed to add server:', error);
      }
    },
    async assignUser(server) {
      try {
        await axios.put(`/api/servers/${server.id}/assign`, server.assignedUserIds);
        alert('Users assigned successfully');
      } catch (error) {
        console.error('Failed to assign user:', error);
      }
    },
    connectSSH(serverId) {
      this.$router.push(`/dashboard/ssh/${serverId}`);
    },
    openEditModal(server) {
      this.editServerData = { ...server };
      this.showEditModal = true;
    },
    closeEditModal() {
      this.showEditModal = false;
      this.editServerData = {};
    },
    async saveEdit() {
      try {
        await axios.put(`/api/servers/${this.editServerData.id}`, this.editServerData);
        const index = this.servers.findIndex(s => s.id === this.editServerData.id);
        if (index !== -1) {
          this.servers[index] = { ...this.editServerData };
        }
        this.closeEditModal();
        alert('Server updated successfully');
      } catch (error) {
        console.error('Failed to update server:', error);
        alert('Failed to update server');
      }
    },
    async deleteServer(id) {
      if (confirm('Are you sure you want to delete this server?')) {
        try {
          await axios.delete(`/api/servers/${id}`);
          this.servers = this.servers.filter(s => s.id !== id);
          alert('Server deleted successfully');
        } catch (error) {
          console.error('Failed to delete server:', error);
          alert('Failed to delete server');
        }
      }
    }
  }
};
</script>
<style scoped>
.server-list {
  padding: 20px;
}
.card-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}
.card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.8em;
  font-weight: bold;
}
.status-badge.online {
  background-color: #e8f5e9;
  color: #2e7d32;
}
.status-badge.offline {
  background-color: #ffebee;
  color: #c62828;
}
h3 {
  margin: 0;
  color: #2c3e50;
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
.btn-ssh {
  background-color: #2980b9;
  width: 100%;
  font-weight: bold;
}
.btn-ssh:hover {
  background-color: #2471a3;
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
}
.form-group input {
  width: 100%;
  box-sizing: border-box;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
.btn-save {
  background-color: #27ae60;
  color: white;
}
.btn-cancel {
  background-color: #95a5a6;
  color: white;
}
</style>
