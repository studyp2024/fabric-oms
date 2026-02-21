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
          <th>Server IP</th>
          <th>Client IP</th>
          <th>User</th>
          <th>Command</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="log in logs" :key="log.id" :class="{ sensitive: log.sensitive }">
          <td>{{ log.timestamp }}</td>
          <td>{{ log.serverIp }}</td>
          <td>{{ log.ip }}</td>
          <td>{{ log.user }}</td>
          <td>{{ log.command }}</td>
          <td>{{ log.sensitive ? '⚠️ WARNING' : 'OK' }}</td>
        </tr>
      </tbody>
    </table>

    <div class="pagination">
      <button @click="prevPage" :disabled="currentPage === 0">Previous</button>
      <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage >= totalPages - 1">Next</button>
      <select v-model="pageSize" @change="handlePageSizeChange">
        <option :value="10">10 per page</option>
        <option :value="20">20 per page</option>
        <option :value="50">50 per page</option>
      </select>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      logs: [],
      searchQuery: '',
      onlySensitive: false,
      currentPage: 0,
      pageSize: 10,
      totalPages: 0,
      totalElements: 0
    };
  },
  async mounted() {
    this.fetchLogs();
  },
  methods: {
    async fetchLogs() {
      try {
        let url = '/api/audit';
        const params = {
          page: this.currentPage,
          size: this.pageSize
        };

        if (this.onlySensitive) {
          url = '/api/audit/sensitive';
        } else if (this.searchQuery) {
          url = '/api/audit/search';
          params.q = this.searchQuery;
        }
        
        const response = await axios.get(url, { params });
        this.logs = response.data.content;
        this.totalPages = response.data.totalPages;
        this.totalElements = response.data.totalElements;
      } catch (error) {
        console.error('Failed to fetch logs:', error);
      }
    },
    search() {
      this.currentPage = 0;
      this.fetchLogs();
    },
    prevPage() {
      if (this.currentPage > 0) {
        this.currentPage--;
        this.fetchLogs();
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages - 1) {
        this.currentPage++;
        this.fetchLogs();
      }
    },
    handlePageSizeChange() {
      this.currentPage = 0;
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
.pagination {
  margin-top: 1rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
}
button {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background-color: #f8f9fa;
  cursor: pointer;
  border-radius: 4px;
}
button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
