import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useAuthStore } from './auth';

export const useReceiptStore = defineStore('receipts', () => {
  const receipts = ref([]);
  const auth = useAuthStore();

  const fetchReceipts = async () => {
    if (!auth.user) return;
    try {
      const response = await fetch(`http://localhost:8080/api/receipts/user/${auth.user.id}`);
      if (response.ok) {
        receipts.value = await response.json();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const createReceipt = async (formData) => {
    try {
      formData.append('userId', auth.user.id);
      const response = await fetch('http://localhost:8080/api/receipts', {
        method: 'POST',
        body: formData,
      });
      
      if (response.ok) {
        const newReceipt = await response.json();
        receipts.value.push(newReceipt);
        return { success: true };
      } else {
        const errorText = await response.text();
        console.error('Server error:', errorText);
        return { success: false, error: errorText };
      }
    } catch (e) {
      console.error('Network or unknown error:', e);
      return { success: false, error: e.message };
    }
  };

  const deleteReceipt = async (id) => {
    try {
      const response = await fetch(`http://localhost:8080/api/receipts/${id}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        receipts.value = receipts.value.filter(r => r.id !== id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  return { receipts, fetchReceipts, createReceipt, deleteReceipt };
});
