<template>
  <div class="webssh-container">
    <div class="header">
      <h3>WebSSH Terminal - Server #{{ serverId }}</h3>
      <button @click="goBack" class="btn-back">Disconnect & Go Back</button>
    </div>
    <!-- 终端显示区域 -->
    <div ref="terminal" class="terminal-box"></div>
  </div>
</template>

<script>
// 引入 xterm 库
import { Terminal } from 'xterm';
// 引入自适应插件
import { FitAddon } from 'xterm-addon-fit';
// 引入终端的样式，必须有否则界面错乱
import 'xterm/css/xterm.css';

export default {
  name: 'WebSsh',
  data() {
    return {
      serverId: this.$route.params.serverId,
      ws: null,   // WebSocket 连接对象
      term: null, // Terminal 实例
      fitAddon: null // 适配插件实例
    };
  },
  mounted() {
    this.initTerminal();
    this.initWebSocket();
    // 监听窗口缩放事件，使终端也能自适应
    window.addEventListener('resize', this.resizeTerminal);
  },
  beforeUnmount() {
    // 组件销毁前断开 WebSocket 和清理终端实例，释放内存和连接
    window.removeEventListener('resize', this.resizeTerminal);
    if (this.ws) {
      this.ws.close();
    }
    if (this.term) {
      this.term.dispose();
    }
  },
  methods: {
    // 初始化 xterm 终端 UI
    initTerminal() {
      this.term = new Terminal({
        cursorBlink: true, // 光标闪烁
        theme: {
          background: '#1e1e1e', // 深色背景
          foreground: '#f8f8f2'  // 浅色字体
        }
      });
      this.fitAddon = new FitAddon();
      this.term.loadAddon(this.fitAddon);

      // 将终端挂载到页面的 div 容器中
      this.term.open(this.$refs.terminal);
      // 必须在 open 之后调用 fit 才能让终端适配容器大小
      this.fitAddon.fit();
    },

    // 初始化 WebSocket 并与终端绑定数据流
    initWebSocket() {
      // 动态获取当前访问地址，拼接成 ws:// 协议发送给后端
      const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
      const host = window.location.hostname;
      // 注意：如果是开发环境，端口可能是 Vue 的 5173，而 Spring Boot 是 8080
      // 考虑到代理或直接请求，这里使用硬编码的 8080 (实际生产中应动态配置或使用相对路径代理)
      const wsUrl = `ws://localhost:8080/api/ws/ssh?serverId=${this.serverId}`;
      
      this.ws = new WebSocket(wsUrl);

      // WebSocket 收到消息（服务器的 SSH 输出），写入到终端
      this.ws.onmessage = (event) => {
        this.term.write(event.data);
      };

      // 终端有输入时（用户敲键盘），发送给 WebSocket 后端
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

    // 窗口调整大小时，重新计算终端尺寸
    resizeTerminal() {
      if (this.fitAddon) {
        this.fitAddon.fit();
      }
    },

    // 返回上一页
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
  background-color: #1e1e1e; /* 跟随终端的深色背景 */
  border-radius: 8px;
  overflow: hidden;
}
</style>