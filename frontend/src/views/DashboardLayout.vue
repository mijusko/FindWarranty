<script setup>
import { RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { 
  Home, 
  Receipt, 
  PlusCircle, 
  BarChart2, 
  User, 
  LogOut 
} from 'lucide-vue-next';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const handleLogout = () => {
  auth.logout();
  router.push('/login');
};

const isActive = (path) => route.path === path;
</script>

<template>
  <div class="dashboard-layout flex h-screen bg-[#09130f] text-white overflow-hidden">
    
    <!-- Sidebar (Desktop) -->
    <aside class="hidden lg:flex flex-col w-64 border-r border-white/10 p-6 glass-sidebar">
      <div class="flex items-center gap-3 mb-10 px-2">
        <div class="bg-primary/20 p-2 rounded-lg">
           <Receipt class="text-primary" />
        </div>
        <h1 class="text-2xl font-bold tracking-tight">RISY</h1>
      </div>
      
      <nav class="flex-1 flex flex-col gap-2">
        <router-link to="/receipts" class="nav-item" :class="{ active: isActive('/receipts') }">
          <Receipt size="20" />
          <span>My Receipts</span>
        </router-link>
        
        <router-link to="/create" class="nav-item" :class="{ active: isActive('/create') }">
          <PlusCircle size="20" />
          <span>Add Receipt</span>
        </router-link>

        <router-link to="/stats" class="nav-item" :class="{ active: isActive('/stats') }">
          <BarChart2 size="20" />
          <span>Statistics</span>
        </router-link>

        <div class="mt-auto">
          <button @click="handleLogout" class="nav-item w-full text-left text-red-400 hover:text-red-300 hover:bg-red-500/10">
            <LogOut size="20" />
            <span>Sign Out</span>
          </button>
        </div>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto relative">
      <!-- Header Mobile -->
      <div class="lg:hidden flex justify-between items-center p-4 sticky top-0 bg-[#09130f]/80 backdrop-blur-md z-30">
        <h1 class="font-bold text-lg">RISY</h1>
        <div class="w-8 h-8 bg-gray-700 rounded-full overflow-hidden">
           <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="Avatar" />
        </div>
      </div>

      <div class="max-w-3xl mx-auto w-full">
        <RouterView />
      </div>
    </main>

    <!-- Bottom Navigation (Mobile) -->
    <nav class="lg:hidden fixed bottom-6 left-6 right-6 bg-[#12281e]/90 backdrop-blur-lg border border-white/10 rounded-2xl flex justify-between items-center px-6 py-4 shadow-2xl z-40">
      <router-link to="/receipts" class="mobile-nav-item" :class="{ active: isActive('/receipts') }">
        <Home size="24" />
      </router-link>
      
      <router-link to="/stats" class="mobile-nav-item" :class="{ active: isActive('/stats') }">
        <Receipt size="24" />
      </router-link>

      <!-- FAB (Floating Action Button) Center -->
      <router-link to="/create" class="mobile-fab">
        <PlusCircle size="32" class="text-black" />
      </router-link>

      <router-link to="/profile" class="mobile-nav-item" :class="{ active: isActive('/profile') }">
        <BarChart2 size="24" />
      </router-link>

      <button @click="handleLogout" class="mobile-nav-item text-gray-500">
        <User size="24" />
      </button>
    </nav>

  </div>
</template>

<style scoped>
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  color: #94a3b8;
  font-weight: 500;
  transition: all 0.2s;
  text-decoration: none;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

.nav-item.active {
  background: rgba(0, 255, 102, 0.1);
  color: var(--color-primary);
}

.mobile-nav-item {
  color: #64748b;
  transition: color 0.2s;
}

.mobile-nav-item.active {
  color: var(--color-primary);
}

.mobile-fab {
  background: var(--color-primary);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px rgba(0, 255, 102, 0.4);
  transform: translateY(-20px);
  border: 4px solid #09130f; /* Match bg to create gap effect */
}

/* Glass Sidebar */
.glass-sidebar {
  background: rgba(18, 40, 30, 0.4);
  backdrop-filter: blur(20px);
}
</style>
