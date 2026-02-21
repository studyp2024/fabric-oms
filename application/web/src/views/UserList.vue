<template>
  <div class="user-list-container">
    <h2>User Management</h2>
    
    <div class="add-user-form">
      <h3>Add New User</h3>
      <div class="form-group">
        <input v-model="newUser.username" placeholder="Username" />
        <input v-model="newUser.password" type="password" placeholder="Password" />
        <select v-model="newUser.role">
          <option value="OPS">Operator (OPS)</option>
          <option value="ADMIN">Administrator (ADMIN)</option>
        </select>
        <button @click="addUser" class="btn-primary">Add User</button>
      </div>
    </div>

    <table class="user-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Username</th>
          <th>Role</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.username }}</td>
          <td>
            <span :class="['role-badge', user.role.toLowerCase()]">{{ user.role }}</span>
          </td>
          <td>
            <button v-if="user.username !== 'admin'" @click="deleteUser(user.id)" class="btn-danger">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      users: [],
      newUser: {
        username: '',
        password: '',
        role: 'OPS'
      }
    };
  },
  async mounted() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.role !== 'ADMIN') {
      this.$router.push('/dashboard/servers');
      return;
    }
    this.fetchUsers();
  },
  methods: {
    async fetchUsers() {
      try {
        const response = await axios.get('/api/auth/users');
        this.users = response.data;
      } catch (error) {
        console.error('Failed to fetch users:', error);
      }
    },
    async addUser() {
      if (!this.newUser.username || !this.newUser.password) {
        alert('Please fill in all fields');
        return;
      }
      try {
        await axios.post('/api/auth/register', this.newUser);
        this.newUser = { username: '', password: '', role: 'OPS' };
        this.fetchUsers();
        alert('User added successfully');
      } catch (error) {
        alert('Failed to add user: ' + (error.response?.data || error.message));
      }
    },
    async deleteUser(id) {
      if (!confirm('Are you sure you want to delete this user?')) return;
      try {
        await axios.delete(`/api/auth/users/${id}`);
        this.fetchUsers();
      } catch (error) {
        console.error('Failed to delete user:', error);
      }
    }
  }
};
</script>

<style scoped>
.user-list-container {
  padding: 20px;
}

.add-user-form {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

input, select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background-color: #f8f9fa;
  font-weight: 600;
}

.role-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.85em;
  font-weight: 600;
}

.role-badge.admin {
  background-color: #e3f2fd;
  color: #1976d2;
}

.role-badge.ops {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.btn-primary {
  background-color: #1976d2;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-danger:hover {
  background-color: #c82333;
}
</style>
