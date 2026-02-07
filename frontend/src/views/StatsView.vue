<script setup>
import { computed, onMounted } from 'vue';
import { useReceiptStore } from '../stores/receipts';
import { useAuthStore } from '../stores/auth';

const store = useReceiptStore();
const auth = useAuthStore();

onMounted(() => {
  store.fetchReceipts();
});

const totalSpent = computed(() => {
  return store.receipts.reduce((sum, r) => sum + Number(r.price), 0);
});

const byCategory = computed(() => {
  const stats = {};
  store.receipts.forEach(r => {
    stats[r.category] = (stats[r.category] || 0) + Number(r.price);
  });
  return stats;
});

const formatPrice = (price) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price);
};
</script>

<template>
  <div class="container">
    <h1 class="mb-4">Profile & Stats</h1>
    
    <div class="card mb-4 flex items-center gap-4">
      <div class="avatar">
        {{ auth.user?.username.charAt(0).toUpperCase() }}
      </div>
      <div>
        <h2>{{ auth.user?.username }}</h2>
        <p class="text-muted">Member since 2026</p>
      </div>
    </div>

    <div class="stats-grid">
      <div class="card stat-card">
        <h3>Total Spent</h3>
        <p class="big-number">{{ formatPrice(totalSpent) }}</p>
      </div>
      
      <div class="card stat-card">
        <h3>Receipts</h3>
        <p class="big-number">{{ store.receipts.length }}</p>
      </div>
    </div>

    <h2 class="mt-8 mb-4">Spending by Category</h2>
    <div class="grid">
      <div v-for="(amount, cat) in byCategory" :key="cat" class="card">
        <h3>{{ cat }}</h3>
        <p class="text-xl font-bold text-primary">{{ formatPrice(amount) }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: var(--color-primary);
  color: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bold;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
}
.big-number {
  font-size: 2.5rem;
  font-weight: bold;
  color: var(--color-primary);
}
.text-primary { color: var(--color-primary); }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}
</style>
