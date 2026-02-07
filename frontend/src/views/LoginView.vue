<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();
const username = ref('');
const password = ref('');
const isRegister = ref(false);

const handleSubmit = async () => {
  let success;
  if (isRegister.value) {
    success = await auth.register(username.value, password.value);
  } else {
    success = await auth.login(username.value, password.value);
  }
  
  if (success) {
    router.push('/receipts');
  } else {
    alert('Authentication failed');
  }
};
</script>

<template>
  <div class="flex items-center justify-center min-h-screen">
    <div class="card w-full max-w-md">
      <h2 class="text-center mb-4">{{ isRegister ? 'Register' : 'Login' }}</h2>
      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div>
          <label>Username</label>
          <input v-model="username" type="text" required />
        </div>
        <div>
          <label>Password</label>
          <input v-model="password" type="password" required />
        </div>
        <button type="submit" class="primary mt-4">
          {{ isRegister ? 'Sign Up' : 'Sign In' }}
        </button>
      </form>
      <p class="text-center mt-4 text-sm">
        {{ isRegister ? 'Already have an account?' : "Don't have an account?" }}
        <a href="#" @click.prevent="isRegister = !isRegister">
          {{ isRegister ? 'Login' : 'Register' }}
        </a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.min-h-screen { min-height: 100vh; }
.max-w-md { max-width: 28rem; }
</style>
