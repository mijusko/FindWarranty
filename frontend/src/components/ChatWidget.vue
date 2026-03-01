<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth';
import { MessageSquare, X, Send, Trash2, Bot, User } from 'lucide-vue-next';

const authStore = useAuthStore();
const isOpen = ref(false);
const messages = ref([]);
const newMessage = ref('');
const includeReceipts = ref(false);
const isLoading = ref(false);
const chatContainer = ref(null);

const fetchHistory = async () => {
  if (!authStore.user) return;
  try {
    const response = await fetch(`http://localhost:8080/api/chat/history/${authStore.user.username}`);
    if (response.ok) {
      messages.value = await response.json();
      scrollToBottom();
    }
  } catch (err) {
    console.error('History fetch error:', err);
  }
};

const sendMessage = async () => {
  if (!newMessage.value.trim() || isLoading.value) return;

  const content = newMessage.value;
  newMessage.value = '';
  isLoading.value = true;

  // Add user message to UI immediately
  messages.value.push({
    role: 'user',
    content: content,
    timestamp: new Date().toISOString()
  });
  scrollToBottom();

  try {
    const response = await fetch('http://localhost:8080/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: authStore.user.username,
        content: content,
        includeReceipts: includeReceipts.value
      })
    });

    if (response.ok) {
      const aiResponse = await response.json();
      messages.value.push(aiResponse);
      scrollToBottom();
    }
  } catch (err) {
    console.error('Chat error:', err);
    messages.value.push({
      role: 'assistant',
      content: 'Error: Could not reach the advisor.',
      timestamp: new Date().toISOString()
    });
  } finally {
    isLoading.value = false;
  }
};

const clearChat = async () => {
  if (!confirm('Clear chat history?')) return;
  try {
    await fetch(`http://localhost:8080/api/chat/clear/${authStore.user.username}`, { method: 'DELETE' });
    messages.value = [];
  } catch (err) {
    console.error('Clear chat error:', err);
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
};

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    fetchHistory();
  }
};

onMounted(() => {
  if (authStore.user) {
    fetchHistory();
  }
});
</script>

<template>
  <div class="chat-widget">
    <!-- Floating Button -->
    <button @click="toggleChat" class="chat-toggle-btn" :class="{ 'is-open': isOpen }">
      <MessageSquare v-if="!isOpen" />
      <X v-else />
    </button>

    <!-- Chat Window -->
    <div v-if="isOpen" class="chat-window shadow-lg">
      <div class="chat-header">
        <div class="header-info">
          <Bot size="20" class="text-primary" />
          <span>Financial Advisor (Qwen)</span>
        </div>
        <button @click="clearChat" title="Clear History" class="clear-btn">
          <Trash2 size="16" />
        </button>
      </div>

      <div ref="chatContainer" class="chat-messages">
        <div v-if="messages.length === 0" class="empty-state">
          <p>Hello! I am your AI financial advisor. How can I help you manage your receipts and spending today?</p>
        </div>
        
        <div v-for="(msg, idx) in messages" :key="idx" class="message-wrapper" :class="msg.role">
          <div class="message-icon">
            <User v-if="msg.role === 'user'" size="14" />
            <Bot v-else size="14" />
          </div>
          <div class="message-content">
            {{ msg.content }}
          </div>
        </div>

        <div v-if="isLoading" class="message-wrapper assistant loading">
          <div class="message-icon"><Bot size="14" /></div>
          <div class="message-content">Thinking...</div>
        </div>
      </div>

      <div class="chat-footer">
        <div class="context-options">
          <label class="receipts-context">
            <input type="checkbox" v-model="includeReceipts" />
            <span>Include my receipts in chat</span>
          </label>
        </div>
        
        <form @submit.prevent="sendMessage" class="input-area">
          <input 
            v-model="newMessage" 
            placeholder="Type your question..." 
            :disabled="isLoading"
          />
          <button type="submit" :disabled="!newMessage.trim() || isLoading">
            <Send size="18" />
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-widget {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
}

.chat-toggle-btn {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  background-color: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat-toggle-btn:hover {
  transform: scale(1.05);
  background-color: var(--primary-hover);
}

.chat-toggle-btn.is-open {
  background-color: var(--bg-card);
  color: var(--text-muted);
}

.chat-window {
  position: absolute;
  top: 4.5rem;
  right: 0;
  width: 20rem;
  height: 28rem;
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 1rem;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 768px) {
  .chat-window {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    border-radius: 0;
    z-index: 1001;
  }
  .chat-widget {
    top: auto;
    bottom: 5.5rem; /* Above bottom bar */
    right: 1rem;
  }
}

.chat-header {
  padding: 1rem;
  background-color: var(--bg-darker);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  font-size: 0.9rem;
}

.clear-btn {
  padding: 0.4rem;
  border-radius: 0.4rem;
  color: var(--text-muted);
  background: transparent;
}

.clear-btn:hover {
  background-color: rgba(255, 0, 0, 0.1);
  color: #ff4d4d;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background-color: var(--bg-card);
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  font-size: 0.85rem;
  margin-top: 2rem;
  padding: 0 1rem;
}

.message-wrapper {
  display: flex;
  gap: 0.5rem;
  max-width: 85%;
}

.message-wrapper.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-icon {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background-color: var(--bg-darker);
  color: var(--text-muted);
}

.user .message-icon {
  background-color: var(--primary);
  color: white;
}

.message-content {
  padding: 0.6rem 0.8rem;
  border-radius: 0.8rem;
  font-size: 0.85rem;
  line-height: 1.4;
  white-space: pre-wrap;
}

.user .message-content {
  background-color: var(--primary);
  color: white;
  border-bottom-right-radius: 0;
}

.assistant .message-content {
  background-color: var(--bg-darker);
  color: var(--text-main);
  border-bottom-left-radius: 0;
}

.loading .message-content {
  color: var(--text-muted);
  font-style: italic;
}

.chat-footer {
  padding: 0.8rem;
  background-color: var(--bg-darker);
  border-top: 1px solid var(--border-color);
}

.context-options {
  margin-bottom: 0.6rem;
}

.receipts-context {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--text-muted);
  cursor: pointer;
}

.receipts-context input {
  width: 0.8rem;
  height: 0.8rem;
}

.input-area {
  display: flex;
  gap: 0.5rem;
}

.input-area input {
  flex: 1;
  padding: 0.6rem 0.8rem;
  border-radius: 2rem;
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-main);
  font-size: 0.85rem;
}

.input-area button {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background-color: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-area button:disabled {
  opacity: 0.5;
  background-color: var(--text-muted);
}
</style>
