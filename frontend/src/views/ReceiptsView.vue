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
  <div class="receipts-container p-4 pb-24">
    <div class="header mb-6">
      <h1 class="text-2xl font-bold mb-4">All Receipts</h1>
      
      <!-- Search Bar -->
      <div class="relative mb-4">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size="20" />
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="Search receipts..." 
          class="w-full pl-10 pr-4 py-3 rounded-xl border-none bg-[rgba(255,255,255,0.05)] text-white placeholder-gray-500 focus:ring-2 focus:ring-primary"
        />
        <button class="absolute right-3 top-1/2 -translate-y-1/2 bg-[rgba(255,255,255,0.1)] p-2 rounded-lg">
           <SlidersHorizontal size="16" class="text-primary" />
        </button>
      </div>

      <!-- Filter Pills -->
      <div class="flex gap-2 overflow-x-auto pb-2 no-scrollbar">
        <button 
          v-for="filter in ['All Receipts', 'Active Warranty', 'Expiring', 'Expired']" 
          :key="filter"
          class="px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors"
          :class="filter === 'All Receipts' ? 'bg-primary text-black' : 'bg-[rgba(255,255,255,0.05)] text-gray-400 border border-white/10'"
        >
          {{ filter }}
        </button>
      </div>
    </div>

    <!-- Receipts List -->
    <div v-if="loading" class="text-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
    </div>

    <div v-else-if="filteredReceipts.length === 0" class="text-center py-12 text-muted">
      <p>No receipts found.</p>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div 
        v-for="receipt in filteredReceipts" 
        :key="receipt.id" 
        class="receipt-card relative flex items-stretch min-h-[100px]"
      >
        <!-- Left Color Bar (Status) -->
        <div 
          class="w-3 rounded-l-xl"
          :class="isExpired(receipt.warrantyExpiryDate) ? 'bg-red-500' : 'bg-primary'"
        ></div>

        <!-- Main Card Body -->
        <div class="flex-1 bg-white p-4 flex flex-col justify-between rounded-r-sm shadow-lg text-gray-900">
           <div class="flex justify-between items-start mb-2">
              <div>
                 <h3 class="font-bold text-lg leading-tight">{{ receipt.storeName }}</h3>
                 <p class="text-xs text-gray-400 font-mono mt-1">{{ formatDate(receipt.purchaseDate) }}</p>
              </div>
              <div 
                class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wide border"
                :class="isExpired(receipt.warrantyExpiryDate) ? 'text-red-500 border-red-200 bg-red-50' : 'text-primary border-green-200 bg-green-50'"
              >
                 {{ isExpired(receipt.warrantyExpiryDate) ? 'Expired' : 'Valid Warranty' }}
              </div>
           </div>
           
           <div class="flex justify-between items-end">
              <span class="text-gray-400 text-xs font-mono">#{{ receipt.id.toString().padStart(4, '0') }}</span>
              <span class="text-2xl font-bold tracking-tight">{{ receipt.price }}</span>
           </div>
        </div>

        <!-- Right Jagged Edge (Pure CSS) -->
        <div class="jagged-edge"></div>
      </div>
    </div>

    <!-- Edit Modal -->
    <div v-if="editingReceipt" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div class="bg-[#12281e] w-full max-w-md rounded-2xl p-6 border border-white/10 shadow-2xl">
        <h2 class="text-xl font-bold mb-4 text-white">Edit Receipt</h2>
        
        <form @submit.prevent="saveEdit" class="flex flex-col gap-4">
          <div>
            <label class="block text-xs text-gray-400 mb-1">Store Name</label>
            <input v-model="editingReceipt.storeName" type="text" required class="bg-white text-gray-900" />
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs text-gray-400 mb-1">Price</label>
              <input v-model="editingReceipt.price" type="number" step="0.01" required class="bg-white text-gray-900" />
            </div>
            <div>
               <label class="block text-xs text-gray-400 mb-1">Category</label>
               <input v-model="editingReceipt.category" type="text" class="bg-white text-gray-900" />
            </div>
          </div>

          <div class="flex gap-3 mt-4">
            <button type="button" @click="cancelEdit" class="flex-1 py-3 bg-transparent border border-white/20 text-white rounded-xl">Cancel</button>
            <button type="submit" class="flex-1 py-3 bg-primary text-black font-bold rounded-xl">Save Changes</button>
          </div>
        </form>
      </div>
    </div>

  </div>
</template>

<style scoped>
.receipt-card {
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3));
  transition: transform 0.2s;
}

.receipt-card:active {
  transform: scale(0.98);
}

.jagged-edge {
  width: 20px;
  background: #fff;
  position: relative;
  background-image: linear-gradient(135deg, transparent 50%, #fff 50%), linear-gradient(225deg, transparent 50%, #fff 50%);
  background-position: top left, top left;
  background-size: 20px 20px;
  background-repeat: repeat-y;
  /* This creates the jagged effect pointing outwards */
  mask-image: linear-gradient(to right, black 50%, transparent 50%);
  -webkit-mask-image: linear-gradient(to right, black 50%, transparent 50%);
  /* Actually, simpler approach for "receipt tear" look: */
  background: radial-gradient(circle at 10px 50%, transparent 10px, #fff 11px);
  background-size: 20px 20px;
  background-position: -10px 0;
}

/* Redefining the jagged edge properly */
.receipt-card::after {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  right: -10px;
  width: 10px;
  background-size: 10px 20px;
  background-repeat: repeat-y;
  background-image: linear-gradient(45deg, transparent 50%, #fff 50%), linear-gradient(135deg, #fff 50%, transparent 50%);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
