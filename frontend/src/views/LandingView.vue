<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { onMounted, ref } from 'vue';
import { Smartphone, Apple, X, Share, PlusSquare, ArrowUpCircle } from 'lucide-vue-next';

const router = useRouter();
const auth = useAuthStore();

const deferredPrompt = ref(null);
const showAndroidModal = ref(false);
const showIosModal = ref(false);

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

  showIosModal.value = true;
};
</script>

<template>
  <div class="landing flex flex-col items-center justify-center relative overflow-hidden">
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

    <!-- iOS Install Modal -->
    <div v-if="showIosModal" class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-darker w-full max-w-sm rounded-3xl p-8 border border-gray-800 shadow-2xl mb-4 sm:mb-0">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-white/10 rounded-2xl">
            <Apple class="text-white" size="32" />
          </div>
          <button @click="showIosModal = false" class="p-2 hover:bg-white/5 rounded-full">
            <X size="24" />
          </button>
        </div>
        
        <h3 class="text-2xl font-bold mb-6">Install on iPhone</h3>
        
        <div class="space-y-6">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-white/5 rounded-full flex items-center justify-center shrink-0 border border-white/10">1</div>
            <p class="text-gray-300">Tap the <span class="inline-flex items-center px-2 py-1 bg-white/10 rounded text-white mx-1"><Share size="16" class="mr-1"/> Share</span> button in Safari.</p>
          </div>
          
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-white/5 rounded-full flex items-center justify-center shrink-0 border border-white/10">2</div>
            <p class="text-gray-300">Scroll down and select <span class="inline-flex items-center px-2 py-1 bg-white/10 rounded text-white mx-1"><PlusSquare size="16" class="mr-1"/> Add to Home Screen</span>.</p>
          </div>

          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-white/5 rounded-full flex items-center justify-center shrink-0 border border-white/10">3</div>
            <p class="text-gray-300">Tap <span class="text-primary font-bold">Add</span> in the top right corner.</p>
          </div>
        </div>

        <div class="mt-10 pt-6 border-t border-gray-800 text-center">
          <p class="text-xs text-gray-500 flex items-center justify-center gap-2">
            Look for this icon <ArrowUpCircle size="14" /> at the bottom
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  background: radial-gradient(circle at top, #1e293b, #020617);
}
.title {
  font-size: 3rem;
  color: #3b82f6;
  margin-bottom: 1rem;
}
.subtitle {
  font-size: 1.25rem;
  color: #94a3b8;
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
