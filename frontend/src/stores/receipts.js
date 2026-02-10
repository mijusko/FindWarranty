import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useAuthStore } from './auth';

export const useReceiptStore = defineStore('receipts', () => {
  const receipts = ref([]);
  const auth = useAuthStore();
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

  const fetchReceipts = async () => {
    if (!auth.user) return;
    try {
      const response = await fetch(`${API_URL}/api/receipts/user/${auth.user.id}`);
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
      const response = await fetch(`${API_URL}/api/receipts`, {
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
      const response = await fetch(`${API_URL}/api/receipts/${id}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        receipts.value = receipts.value.filter(r => r.id !== id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const updateReceipt = async (id, formData) => {
    try {
      // If we're updating, we might not want to change the userId
      // but the backend might expect it
      if (auth.user) {
        formData.append('userId', auth.user.id);
      }
      
      const response = await fetch(`${API_URL}/api/receipts/${id}`, {
        method: 'PUT',
        body: formData,
      });

      if (response.ok) {
        const updated = await response.json();
        const index = receipts.value.findIndex(r => r.id === id);
        if (index !== -1) {
          receipts.value[index] = updated;
        }
        return { success: true };
      } else {
        const errorText = await response.text();
        return { success: false, error: errorText };
      }
    } catch (e) {
      return { success: false, error: e.message };
    }
  };

  return { receipts, fetchReceipts, createReceipt, deleteReceipt, updateReceipt };
});
