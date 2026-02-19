<template>
  <div class="server-list">
    <h2>Assigned Servers</h2>
    <div class="card-container">
      <div v-for="server in servers" :key="server.id" class="card">
        <h3>{{ server.ip }}</h3>
        <p><strong>SSH User:</strong> {{ server.sshUser }}</p>
        <p><strong>SSH Password:</strong> {{ server.sshPassword }}</p>
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
      servers: []
    };
  },
  async mounted() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    try {
      const response = await axios.get(`/api/servers/user/${user.id}`);
      this.servers = response.data;
    } catch (error) {
      console.error('Failed to fetch servers:', error);
    }
  },
  methods: {
    copy(text) {
      navigator.clipboard.writeText(text);
      alert('Password copied to clipboard');
    }
  }
};
</script>

<style scoped>
.card-container {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
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
</style>
