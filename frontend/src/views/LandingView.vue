<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { onMounted, ref } from 'vue';
import { Smartphone, Apple } from 'lucide-vue-next';

const router = useRouter();
const auth = useAuthStore();

const deferredPrompt = ref(null);

onMounted(() => {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt.value = e;
  });
});

const handleGetStarted = () => {
  if (auth.user) {
    router.push('/receipts');
  } else {
    router.push('/register');
  }
};

const installAndroid = async () => {
  const ua = navigator.userAgent.toLowerCase();
  const isAndroid = ua.indexOf("android") > -1;

  if (!isAndroid) {
    alert("This device is not an Android.");
    return;
  }

  if (deferredPrompt.value) {
    deferredPrompt.value.prompt();
    const { outcome } = await deferredPrompt.value.userChoice;
    if (outcome === 'accepted') {
      deferredPrompt.value = null;
    }
  } else {
    alert("Please use Chrome/Edge and look for the install icon in the address bar if this button doesn't work.");
  }
};

const installIos = () => {
  const ua = navigator.userAgent.toLowerCase();
  const isIos = /iphone|ipad|ipod/.test(ua);

  if (!isIos) {
    alert("This device is not an iPhone.");
    return;
  }

  alert("To install on iOS: \n1. Tap the 'Share' icon (square with arrow up) \n2. Scroll down and tap 'Add to Home Screen' \n3. Tap 'Add'");
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
      
      <div class="flex flex-col gap-4 items-center">
        <button @click="handleGetStarted" class="primary text-lg px-12 py-4 w-64">
          Get Started
        </button>

        <div class="flex flex-col sm:flex-row gap-3 mt-4">
          <button @click="installAndroid" class="install-btn android-btn">
            <Smartphone size="20" />
            <span>Install Android</span>
          </button>
          
          <button @click="installIos" class="install-btn ios-btn">
            <Apple size="20" />
            <span>Install iOS</span>
          </button>
        </div>
      </div>
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

.install-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  font-weight: 600;
  width: 200px;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.install-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.android-btn {
  color: #3DDC84; /* Android green */
  border-color: rgba(61, 220, 132, 0.2);
}

.ios-btn {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}
</style>
