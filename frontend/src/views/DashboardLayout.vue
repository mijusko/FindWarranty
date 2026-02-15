<script setup>
import { RouterView, RouterLink } from 'vue-router';
import { Receipt, PlusCircle, User } from 'lucide-vue-next';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const auth = useAuthStore();
const router = useRouter();

const logout = () => {
  auth.logout();
  router.push('/');
};
</script>

<template>
  <div class="dashboard-layout">
    <!-- Desktop Sidebar -->
    <nav class="sidebar hidden md-flex">
      <div class="logo">FW</div>
      <div class="nav-links">
        <RouterLink to="/receipts" class="nav-item">
          <Receipt /> <span>Receipts</span>
        </RouterLink>
        <RouterLink to="/create" class="nav-item">
          <PlusCircle /> <span>Create</span>
        </RouterLink>
        <RouterLink to="/stats" class="nav-item">
          <User /> <span>Profile</span>
        </RouterLink>
      </div>
      <button @click="logout" class="logout-btn">Logout</button>
    </nav>

    <!-- Main Content -->
    <main class="content-area">
      <RouterView />
    </main>

    <!-- Mobile Bottom Bar -->
    <nav class="bottom-bar md-hidden">
      <RouterLink to="/receipts" class="nav-item">
        <Receipt />
        <span class="text-xs">Receipts</span>
      </RouterLink>
      <RouterLink to="/create" class="nav-item create-btn">
        <PlusCircle :size="32" />
      </RouterLink>
      <RouterLink to="/stats" class="nav-item">
        <User />
        <span class="text-xs">Profile</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 80px; /* Space for bottom bar */
}

/* Desktop Sidebar */
.sidebar {
  width: 250px;
  background-color: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  padding: 1rem;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--color-primary);
  margin-bottom: 2rem;
  text-align: center;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 8px;
  color: var(--color-text-muted);
}

.nav-item:hover, .nav-item.router-link-active {
  background-color: rgba(56, 189, 248, 0.1);
  color: var(--color-primary);
}

.logout-btn {
  margin-top: auto;
}

/* Mobile Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background-color: var(--color-surface);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 100;
}

.bottom-bar .nav-item {
  flex-direction: column;
  gap: 0;
  padding: 0.5rem;
  font-size: 0.8rem;
}

.create-btn {
  color: var(--color-primary) !important;
  transform: translateY(-10px);
  background-color: var(--color-surface);
  border-radius: 50%;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  box-shadow: 0 -2px 10px rgba(0,0,0,0.2);
}

/* Responsive Utilities */
.md-hidden { display: flex; }
.hidden { display: none; }
.md-flex { display: none; }

@media (min-width: 768px) {
  .md-hidden { display: none; }
  .md-flex { display: flex; }
  .content-area { padding-bottom: 0; }
}
</style>
