<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { onMounted, ref } from 'vue';
import { Smartphone, Apple, ArrowRight, Receipt } from 'lucide-vue-next';

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
  <div class="landing flex flex-col items-center justify-center p-6">
    <div class="content text-center w-full max-w-md">
      
      <!-- Icon Header -->
      <div class="mb-6 flex justify-center">
        <div class="bg-primary/20 p-4 rounded-2xl backdrop-blur-sm border border-primary/20 shadow-[0_0_30px_rgba(0,255,102,0.2)]">
          <Receipt class="text-primary w-12 h-12" />
        </div>
      </div>

      <h1 class="title mb-4">
        RISY: <span class="text-primary">Receipts</span> and Invoices Saved for You
      </h1>
      
      <p class="subtitle mb-12">
        Organize your expenses with the magic of AI and deep glassmorphism.
      </p>

      <!-- Glass Card Visualization -->
      <div class="glass-card-visual mb-12 mx-auto relative overflow-hidden">
        <div class="absolute top-4 left-4 right-4 h-3 bg-white/10 rounded-full w-1/3"></div>
        <div class="absolute top-4 right-4 h-3 bg-white/10 rounded-full w-3"></div>
        <div class="absolute top-10 left-4 right-4 h-8 bg-white/5 rounded-lg w-3/4"></div>
        
        <div class="absolute inset-0 flex items-center justify-center">
           <div class="w-16 h-10 border-b-2 border-r-2 border-primary rotate-45 transform translate-y-[-10px]"></div>
        </div>
        
        <div class="absolute bottom-4 left-4 flex gap-2">
           <div class="w-2 h-2 rounded-full bg-primary"></div>
           <div class="w-2 h-2 rounded-full bg-white/20"></div>
           <div class="w-2 h-2 rounded-full bg-white/20"></div>
        </div>
      </div>

      <div class="flex flex-col gap-4 items-center w-full">
        <button @click="handleGetStarted" class="primary w-full py-4 text-lg flex items-center justify-center gap-2 uppercase tracking-wide">
          Start Now <ArrowRight size="20" />
        </button>
        
        <div class="h-px w-full bg-white/10 my-2 relative">
            <span class="absolute left-1/2 -translate-x-1/2 -top-3 bg-[var(--color-bg)] px-2 text-xs text-muted font-mono uppercase tracking-widest">Get the app</span>
        </div>

        <div class="grid grid-cols-2 gap-4 w-full">
          <button @click="installAndroid" class="install-btn android-btn">
            <Smartphone size="20" />
            <span class="text-xs font-bold mt-1">ANDROID</span>
          </button>
          
          <button @click="installIos" class="install-btn ios-btn">
            <Apple size="20" />
            <span class="text-xs font-bold mt-1">IOS</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  background: radial-gradient(circle at 50% 0%, #1a3c2f, var(--color-bg) 70%);
}

.title {
  font-size: 2.5rem;
  line-height: 1.1;
  color: #fff;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 1.1rem;
  color: #94a3b8;
  line-height: 1.5;
}

.glass-card-visual {
  width: 100%;
  height: 200px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  position: relative;
  backdrop-filter: blur(10px);
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.5);
}

.install-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  border-radius: 16px;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  height: 80px;
}

.install-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.android-btn { color: #fff; }
.ios-btn { color: #fff; }

.text-muted { color: var(--color-text-muted); }
</style>
