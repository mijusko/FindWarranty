<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const handleGetStarted = () => {
  if (auth.user) {
    router.push('/receipts');
  } else {
    router.push('/register');
  }
};
</script>

<template>
  <div class="landing flex flex-col items-center justify-center">
    <div class="content text-center p-4">
      <h1 class="title">FindWarranty</h1>
      <p class="subtitle">Never lose a receipt again. Track your warranties effortlessly.</p>
      <div class="hero-image my-8">
        <!-- Placeholder for image -->
        <div class="placeholder-img">📦 🧾 🛡️</div>
      </div>
      <button @click="handleGetStarted" class="primary text-lg px-8 py-3">
        Get Started
      </button>
    </div>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  background: radial-gradient(circle at top, #1e293b, var(--color-bg));
}
.title {
  font-size: 3rem;
  color: var(--color-primary);
  margin-bottom: 1rem;
}
.subtitle {
  font-size: 1.25rem;
  color: var(--color-text-muted);
  max-width: 600px;
  margin: 0 auto;
}
.placeholder-img {
  font-size: 5rem;
  padding: 2rem;
  background: rgba(255,255,255,0.05);
  border-radius: 20px;
  display: inline-block;
}
</style>
