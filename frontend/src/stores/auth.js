import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user')) || null);
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

  const login = async (username, password) => {
    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (!response.ok) throw new Error('Login failed');
      const data = await response.json();
      user.value = data;
      localStorage.setItem('user', JSON.stringify(data));
      return true;
    } catch (e) {
      console.error(e);
      return false;
    }
  };

  const register = async (username, password) => {
    try {
      const response = await fetch(`${API_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (!response.ok) throw new Error('Registration failed');
      const data = await response.json();
      user.value = data;
      localStorage.setItem('user', JSON.stringify(data));
      return true;
    } catch (e) {
      console.error(e);
      return false;
    }
  };

  const logout = () => {
    user.value = null;
    localStorage.removeItem('user');
  };

  return { user, login, register, logout };
});
