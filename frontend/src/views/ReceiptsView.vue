<script setup>
import { onMounted, ref, computed } from 'vue';
import { useReceiptStore } from '../stores/receipts';
import { 
  Trash2, FileText, Calendar, Tag, Search, 
  ArrowUpDown, Edit2, X, Check, Filter,
  Clock, ExternalLink
} from 'lucide-vue-next';

const store = useReceiptStore();
const searchQuery = ref('');
const sortBy = ref('purchaseDate');
const sortOrder = ref('desc'); // 'asc' or 'desc'

// Edit state
const editingReceipt = ref(null);
const editForm = ref({
  storeName: '',
  productName: '',
  purchaseDate: '',
  price: '',
  category: '',
  warrantyDuration: ''
});
const editFile = ref(null);
const isUpdating = ref(false);

const categories = ['Electronics', 'Clothing & Footwear', 'Groceries', 'Home & Garden', 'Other'];
const warranties = ['6 Months', '1 Year', '2 Years', '3 Years', '5 Years', 'Lifetime', 'Other'];

onMounted(() => {
  store.fetchReceipts();
});

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString();
};

const formatPrice = (price) => {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(price);
};

const filteredReceipts = computed(() => {
  let result = [...store.receipts];

  // Search
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    result = result.filter(r => 
      r.storeName.toLowerCase().includes(q) || 
      r.productName.toLowerCase().includes(q) ||
      r.category.toLowerCase().includes(q)
    );
  }

  // Sort
  result.sort((a, b) => {
    let valA = a[sortBy.value];
    let valB = b[sortBy.value];

    // Handle nulls
    if (valA === null) return 1;
    if (valB === null) return -1;

    if (sortBy.value === 'price') {
      valA = Number(valA);
      valB = Number(valB);
    }

    if (valA < valB) return sortOrder.value === 'asc' ? -1 : 1;
    if (valA > valB) return sortOrder.value === 'asc' ? 1 : -1;
    return 0;
  });

  return result;
});

const toggleSort = (key) => {
  if (sortBy.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortBy.value = key;
    sortOrder.value = 'desc';
  }
};

const startEdit = (receipt) => {
  editingReceipt.value = receipt.id;
  editForm.value = {
    storeName: receipt.storeName,
    productName: receipt.productName,
    purchaseDate: receipt.purchaseDate,
    price: receipt.price,
    category: receipt.category,
    warrantyDuration: receipt.warrantyDuration
  };
  editFile.value = null;
};

const cancelEdit = () => {
  editingReceipt.value = null;
  editFile.value = null;
};

const handleEditFileUpload = (event) => {
  editFile.value = event.target.files[0];
};

const saveEdit = async () => {
  try {
    isUpdating.value = true;
    const formData = new FormData();
    formData.append('storeName', editForm.value.storeName);
    formData.append('productName', editForm.value.productName);
    formData.append('purchaseDate', editForm.value.purchaseDate);
    formData.append('price', editForm.value.price);
    formData.append('category', editForm.value.category);
    formData.append('warrantyDuration', editForm.value.warrantyDuration);
    
    if (editFile.value) {
      formData.append('file', editFile.value);
    }

    const result = await store.updateReceipt(editingReceipt.value, formData);
    if (result.success) {
      editingReceipt.value = null;
    } else {
      alert('Failed to update: ' + result.error);
    }
  } catch (error) {
    alert('Error updating receipt');
  } finally {
    isUpdating.value = false;
  }
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
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
      <h1>My Receipts</h1>
      
      <div class="search-bar flex-1 max-w-md w-full relative">
        <input 
          v-model="searchQuery" 
          placeholder="Search store, product or category..." 
          class="pl-10 w-full"
        />
      </div>
    </div>

    <!-- Toolbar -->
    <div class="flex flex-wrap gap-2 mb-6 bg-darker p-2 rounded-lg border border-gray-800">
      <button @click="toggleSort('purchaseDate')" :class="['sort-btn', { active: sortBy === 'purchaseDate' }]">
        Date
      </button>
      <button @click="toggleSort('storeName')" :class="['sort-btn', { active: sortBy === 'storeName' }]">
        Store
      </button>
      <button @click="toggleSort('price')" :class="['sort-btn', { active: sortBy === 'price' }]">
        Price
      </button>
      <button @click="toggleSort('warrantyExpiryDate')" :class="['sort-btn', { active: sortBy === 'warrantyExpiryDate' }]">
        Warranty
      </button>
      
      <div class="ml-auto flex items-center gap-2 text-xs text-muted px-2">
        <span>Order:</span>
        <span class="text-primary font-bold uppercase">{{ sortOrder }}</span>
      </div>
    </div>

    <!-- Receipts List -->
    <div class="flex flex-col gap-3">
      <div v-if="filteredReceipts.length === 0" class="text-center py-12 text-muted">
        No receipts found matching your criteria.
      </div>

      <div 
        v-for="receipt in filteredReceipts" 
        :key="receipt.id" 
        class="receipt-row-card"
        :class="{ 'editing': editingReceipt === receipt.id }"
      >
        <!-- Standard View -->
        <template v-if="editingReceipt !== receipt.id">
          <div class="receipt-content">
            <div class="main-info">
              <div class="store-info">
                <h3 class="store-name">{{ receipt.storeName }}</h3>
                <p class="product-name">{{ receipt.productName }}</p>
              </div>
              
              <div class="details-grid">
                <div class="detail-item">
                  <Calendar size="14" />
                  <span>{{ formatDate(receipt.purchaseDate) }}</span>
                </div>
                <div class="detail-item">
                  <Tag size="14" />
                  <span>{{ receipt.category }}</span>
                </div>
                <div class="detail-item expiry" :class="{ 'soon': isSoon(receipt.warrantyExpiryDate) }">
                  <Clock size="14" />
                  <span>Ends: {{ formatDate(receipt.warrantyExpiryDate) }}</span>
                </div>
              </div>
            </div>

            <div class="price-section">
              <span class="price-val">{{ formatPrice(receipt.price) }}</span>
            </div>

            <div class="actions">
              <button @click="viewPdf(receipt)" class="icon-btn" title="View PDF">
                <FileText size="18" />
              </button>
              <button @click="startEdit(receipt)" class="icon-btn" title="Edit">
                <Edit2 size="18" />
              </button>
              <button @click="store.deleteReceipt(receipt.id)" class="icon-btn danger" title="Delete">
                <Trash2 size="18" />
              </button>
            </div>
          </div>
        </template>

        <!-- Edit View -->
        <template v-else>
          <div class="edit-form-grid">
            <div class="field">
              <label>Store</label>
              <input v-model="editForm.storeName" />
            </div>
            <div class="field">
              <label>Product</label>
              <input v-model="editForm.productName" />
            </div>
            <div class="field">
              <label>Date</label>
              <input v-model="editForm.purchaseDate" type="date" />
            </div>
            <div class="field">
              <label>Price</label>
              <input v-model="editForm.price" type="number" step="0.01" />
            </div>
            <div class="field">
              <label>Category</label>
              <select v-model="editForm.category">
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
            <div class="field">
              <label>Warranty</label>
              <select v-model="editForm.warrantyDuration">
                <option v-for="dur in warranties" :key="dur" :value="dur">{{ dur }}</option>
              </select>
            </div>
            <div class="field full">
              <label>Update PDF (optional)</label>
              <input type="file" @change="handleEditFileUpload" accept="application/pdf,image/*" />
            </div>
            <div class="edit-actions full">
              <button @click="cancelEdit" class="secondary flex items-center gap-1">
                <X size="16" /> Cancel
              </button>
              <button @click="saveEdit" class="primary flex items-center gap-1" :disabled="isUpdating">
                <Check size="16" /> {{ isUpdating ? 'Saving...' : 'Save Changes' }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
// Helper for expiry warning
const isSoon = (dateStr) => {
  if (!dateStr) return false;
  const expiry = new Date(dateStr);
  const now = new Date();
  const diffTime = expiry - now;
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)); 
  return diffDays > 0 && diffDays < 30;
};
</script>

<style scoped>
.receipt-row-card {
  background: var(--color-surface); /* Plava boja kao sidebar/navbar */
  border-radius: 12px;
  border: 1px solid var(--color-border);
  transition: all 0.2s ease;
  overflow: hidden;
}

.receipt-row-card:hover {
  background: #1e293b; /* Malo svetlija nijansa plave za hover */
  border-color: var(--color-primary);
  transform: translateX(4px);
}

.receipt-row-card.editing {
  border-color: var(--color-primary);
  background: var(--color-bg-darker);
  transform: none;
}

.receipt-content {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem;
  gap: 1.5rem;
}

.main-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 2rem;
}

.store-info {
  min-width: 180px;
}

.store-name {
  font-size: 1.1rem;
  font-weight: bold;
  color: white;
  margin: 0;
}

.product-name {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin: 0.1rem 0 0 0;
}

.details-grid {
  display: flex;
  gap: 1.5rem;
  flex: 1;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.detail-item.expiry.soon {
  color: #fbbf24;
}

.price-section {
  min-width: 100px;
  text-align: right;
}

.price-val {
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--color-primary);
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.sort-btn {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border-radius: 6px;
  cursor: pointer;
}

.sort-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

.sort-btn.active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

/* Edit Form Styles */
.edit-form-grid {
  padding: 1.5rem;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field.full {
  grid-column: span 3;
}

.field label {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: bold;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 0.5rem;
}

/* Responsive */
@media (max-width: 900px) {
  .main-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  .details-grid {
    flex-wrap: wrap;
    gap: 0.8rem;
  }
  .edit-form-grid {
    grid-template-columns: 1fr 1fr;
  }
  .field.full {
    grid-column: span 2;
  }
}

@media (max-width: 600px) {
  .receipt-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  .price-section {
    text-align: left;
    min-width: unset;
  }
  .actions {
    width: 100%;
    justify-content: flex-end;
  }
  .edit-form-grid {
    grid-template-columns: 1fr;
  }
  .field.full {
    grid-column: span 1;
  }
}
</style>
