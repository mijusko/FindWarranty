<script setup>
import { onMounted } from 'vue';
import { useReceiptStore } from '../stores/receipts';
import { Trash2, FileText, Calendar, Tag } from 'lucide-vue-next';

const store = useReceiptStore();

onMounted(() => {
  store.fetchReceipts();
});

const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString();
};

const formatPrice = (price) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price);
};

const viewPdf = (receipt) => {
  if (receipt.pdfData) {
    const blob = new Blob([Uint8Array.from(atob(receipt.pdfData), c => c.charCodeAt(0))], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    window.open(url);
  } else {
    alert('No PDF available');
  }
};
</script>

<template>
  <div class="container">
    <h1 class="mb-4">My Receipts</h1>
    <div class="grid">
      <div v-for="receipt in store.receipts" :key="receipt.id" class="card receipt-card">
        <div class="card-header flex justify-between items-center">
          <h3 class="store-name">{{ receipt.storeName }}</h3>
          <span class="price">{{ formatPrice(receipt.price) }}</span>
        </div>
        
        <div class="product-name">{{ receipt.productName }}</div>
        
        <div class="meta-info">
          <div class="flex items-center gap-2">
            <Calendar size="16" />
            <span>{{ formatDate(receipt.purchaseDate) }}</span>
          </div>
          <div class="flex items-center gap-2">
            <Tag size="16" />
            <span>{{ receipt.category }}</span>
          </div>
          <div v-if="receipt.warrantyExpiryDate" class="flex items-center gap-2 warranty">
            <span>Expires: {{ formatDate(receipt.warrantyExpiryDate) }}</span>
          </div>
        </div>

        <div class="actions flex gap-2 mt-4">
          <button @click="viewPdf(receipt)" class="icon-btn" title="View PDF">
            <FileText size="20" />
          </button>
          <button @click="store.deleteReceipt(receipt.id)" class="icon-btn danger" title="Delete">
            <Trash2 size="20" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}
.receipt-card {
  border-left: 4px solid var(--color-primary);
}
.store-name {
  font-size: 1.2rem;
  color: white;
}
.price {
  font-weight: bold;
  color: var(--color-primary);
  font-size: 1.2rem;
}
.product-name {
  font-size: 1rem;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}
.meta-info {
  font-size: 0.9rem;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.warranty {
  color: #fbbf24; /* Amber for warning/expiry */
}
.actions {
  justify-content: flex-end;
}
.icon-btn {
  padding: 0.5rem;
  background: transparent;
  border: 1px solid var(--color-border);
}
.icon-btn:hover {
  border-color: var(--color-primary);
}
.icon-btn.danger:hover {
  border-color: #ef4444;
  color: #ef4444;
}
</style>
