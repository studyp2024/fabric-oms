<template>
  <div class="webssh-container">
    <div class="header">
      <h3>WebSSH Terminal - Server #{{ serverId }}</h3>
      <button @click="goBack" class="btn-back">Disconnect & Go Back</button>
    </div>
    <div ref="terminal" class="terminal-box"></div>
  </div>
</template>

<script>
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm/css/xterm.css';

export default {
  name: 'WebSsh',
  data() {
    return {
      serverId: this.$route.params.serverId,
      ws: null,
      term: null,
      fitAddon: null
    };
  },
  mounted() {
    this.initTerminal();
    this.initWebSocket();
    window.addEventListener('resize', this.resizeTerminal);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeTerminal);
    if (this.ws) {
      this.ws.close();
    }
    if (this.term) {
      this.term.dispose();
    }
  },
  methods: {
    initTerminal() {
      this.term = new Terminal({
        cursorBlink: true,
        theme: {
          background: '#1e1e1e',
          foreground: '#f8f8f2'
        }
      });
      this.fitAddon = new FitAddon();
      this.term.loadAddon(this.fitAddon);
      this.term.open(this.$refs.terminal);
      this.fitAddon.fit();
    },
    initWebSocket() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;
      const wsUrl = `${protocol}//${host}:8080/api/ws/ssh?serverId=${this.serverId}`;
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onmessage = (event) => {
        this.term.write(event.data);
      };
      
      this.term.onData((data) => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(data);
        }
      });
      
      this.ws.onopen = () => {
        this.term.writeln('\x1b[32m*** WebSocket Connection Established ***\x1b[0m');
      };
      
      this.ws.onclose = () => {
        this.term.writeln('\r\n\x1b[31m*** WebSocket Connection Closed ***\x1b[0m');
      };
      
      this.ws.onerror = (e) => {
        this.term.writeln('\r\n\x1b[31m*** WebSocket Connection Error ***\x1b[0m');
      };
    },
    resizeTerminal() {
      if (this.fitAddon) {
        this.fitAddon.fit();
      }
    },
    goBack() {
      this.$router.go(-1);
    }
  }
};
</script>

<style scoped>
.webssh-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fa;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: white;
  border-bottom: 1px solid #ddd;
  margin-bottom: 15px;
  border-radius: 8px;
}
.header h3 {
  margin: 0;
  color: #2c3e50;
}
.btn-back {
  background-color: #e74c3c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
.btn-back:hover {
  background-color: #c0392b;
}
.terminal-box {
  flex: 1;
  width: 100%;
  padding: 10px;
  background-color: #1e1e1e;
  border-radius: 8px;
  overflow: hidden;
}
</style>