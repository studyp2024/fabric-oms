<template>
  <div class="audit-logs">
    <h2>Audit Logs</h2>
    <div class="controls">
      <input v-model="searchQuery" placeholder="Search commands..." @input="search">
      <label><input type="checkbox" v-model="onlySensitive" @change="fetchLogs"> Only Sensitive</label>
    </div>
    <table>
      <thead>
        <tr>
          <th>Timestamp</th>
          <th>IP</th>
          <th>User</th>
          <th>Command</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="log in logs" :key="log.id" :class="{ sensitive: log.sensitive }">
          <td>{{ log.timestamp }}</td>
          <td>{{ log.ip }}</td>
          <td>{{ log.user }}</td>
          <td>{{ log.command }}</td>
          <td>{{ log.sensitive ? '⚠️ WARNING' : 'OK' }}</td>
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
      logs: [],
      searchQuery: '',
      onlySensitive: false
    };
  },
  async mounted() {
    this.fetchLogs();
  },
  methods: {
    async fetchLogs() {
      try {
        let url = '/api/audit';
        if (this.onlySensitive) {
          url = '/api/audit/sensitive';
        } else if (this.searchQuery) {
          url = `/api/audit/search?q=${this.searchQuery}`;
        }
        const response = await axios.get(url);
        this.logs = response.data;
      } catch (error) {
        console.error('Failed to fetch logs:', error);
      }
    },
    search() {
      this.fetchLogs();
    }
  }
};
</script>

<style scoped>
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}
th, td {
  padding: 0.75rem;
  border-bottom: 1px solid #ddd;
  text-align: left;
}
th {
  background-color: #f8f9fa;
}
.sensitive {
  background-color: #ffebee;
  color: #c62828;
}
.controls {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}
input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
