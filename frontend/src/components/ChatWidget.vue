<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth';
import { MessageSquare, X, Send, Trash2, Bot, User } from 'lucide-vue-next';

const authStore = useAuthStore();
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const isOpen = ref(false);
const messages = ref([]);
const newMessage = ref('');
const includeReceipts = ref(false);
const isLoading = ref(false);
const chatContainer = ref(null);

const fetchHistory = async () => {
  if (!authStore.user) return;
  try {
    const response = await fetch(`${API_URL}/api/chat/history/${authStore.user.username}`);
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
    const response = await fetch(`${API_URL}/api/chat/send`, {
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
    await fetch(`${API_URL}/api/chat/clear/${authStore.user.username}`, { method: 'DELETE' });
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
    <div class="chat-window shadow-lg" :class="{ 'is-open': isOpen }">
      <div class="chat-header">
        <div class="header-info">
          <Bot size="24" class="text-primary" />
          <div class="advisor-title">
            <span class="advisor-name">Financial Advisor</span>
            <span class="advisor-status">Always online</span>
          </div>
        </div>
        <div class="header-actions">
          <button @click="clearChat" title="Clear History" class="action-btn">
            <Trash2 size="18" />
          </button>
          <button @click="toggleChat" title="Close Chat" class="action-btn close-desktop">
            <X size="20" />
          </button>
        </div>
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
  top: 0;
  right: 0;
  height: 100vh;
  z-index: 1000;
  pointer-events: none; /* Let clicks pass through to background when closed */
}

.chat-toggle-btn {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  background-color: var(--color-primary);
  color: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  z-index: 1001;
  pointer-events: auto;
}

.chat-toggle-btn:hover {
  transform: scale(1.1) rotate(5deg);
  background-color: var(--color-primary-hover);
  color: white;
}

.chat-toggle-btn.is-open {
  background-color: #ef4444;
  color: white;
  transform: rotate(90deg);
}

.chat-window {
  position: fixed;
  top: 0;
  right: 0;
  width: 400px;
  height: 100vh;
  background-color: var(--color-bg);
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  box-shadow: -10px 0 50px rgba(0, 0, 0, 0.8);
  pointer-events: auto;
  transform: translateX(100%);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat-window.is-open {
  transform: translateX(0);
}

/* Desktop sizing */
@media (min-width: 769px) {
  .chat-window {
    width: 450px;
  }
}

/* Mobile full-screen */
@media (max-width: 768px) {
  .chat-window {
    width: 100%;
    border-left: none;
  }
  
  .chat-toggle-btn {
    top: auto;
    bottom: 6rem; /* Above mobile bottom bar */
    right: 1.5rem;
  }
}

.chat-header {
  padding: 1.5rem;
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  padding: 0.5rem;
  border-radius: 0.5rem;
  color: var(--color-text-muted);
  background: transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background-color: var(--color-bg);
  color: var(--color-text);
}

.action-btn.close-desktop {
  color: #ef4444;
}

.action-btn.close-desktop:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

@media (max-width: 768px) {
  .close-desktop {
    display: none;
  }
}

.header-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.advisor-title {
  display: flex;
  flex-direction: column;
}

.advisor-name {
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-text);
}

.advisor-status {
  font-size: 0.75rem;
  color: #22c55e;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.advisor-status::before {
  content: '';
  width: 6px;
  height: 6px;
  background-color: #22c55e;
  border-radius: 50%;
  display: inline-block;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background-color: var(--color-bg);
  background-image: radial-gradient(var(--color-border) 1px, transparent 1px);
  background-size: 24px 24px;
}

.message-wrapper {
  display: flex;
  gap: 0.75rem;
  max-width: 85%;
  animation: messageFadeIn 0.3s ease-out;
}

@keyframes messageFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-wrapper.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background-color: var(--color-surface);
  color: var(--color-text-muted);
  box-shadow: 0 2px 8px rgba(0,0,0,0.4);
}

.user .message-icon {
  background-color: var(--color-primary);
  color: var(--color-bg);
}

.message-content {
  padding: 0.8rem 1rem;
  border-radius: 1rem;
  font-size: 0.9rem;
  line-height: 1.5;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
}

.user .message-content {
  background-color: var(--color-primary);
  color: var(--color-bg);
  border-top-right-radius: 0.2rem;
  font-weight: 500;
}

.assistant .message-content {
  background-color: var(--color-surface);
  color: var(--color-text);
  border-top-left-radius: 0.2rem;
}

.chat-footer {
  padding: 1.25rem;
  background-color: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.context-options {
  margin-bottom: 1rem;
  padding: 0.6rem;
  background: var(--color-bg);
  border-radius: 0.5rem;
  border: 1px solid var(--color-border);
}

.receipts-context {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  cursor: pointer;
}

.input-area {
  display: flex;
  gap: 0.75rem;
}

.input-area input {
  flex: 1;
  padding: 0.8rem 1.2rem;
  border-radius: 2rem;
  background-color: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  font-size: 0.95rem;
  transition: all 0.3s;
}

.input-area input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
  outline: none;
}

.input-area button {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  background-color: var(--color-primary);
  color: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.input-area button:hover:not(:disabled) {
  transform: scale(1.1);
  background-color: var(--color-primary-hover);
  color: white;
}

.input-area button:disabled {
  opacity: 0.4;
  background-color: var(--color-text-muted);
}
</style>
