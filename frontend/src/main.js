import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import './style.css';
import App from './App.vue';

// Views
import LandingView from './views/LandingView.vue';
import LoginView from './views/LoginView.vue';
import DashboardLayout from './views/DashboardLayout.vue';
import ReceiptsView from './views/ReceiptsView.vue';
import CreateReceiptView from './views/CreateReceiptView.vue';
import StatsView from './views/StatsView.vue';

import { useAuthStore } from './stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: LandingView },
    { path: '/login', component: LoginView },
    { path: '/register', component: LoginView }, // Reusing LoginView with logic
    { 
      path: '/', 
      component: DashboardLayout,
      children: [
        { path: 'receipts', component: ReceiptsView },
        { path: 'create', component: CreateReceiptView },
        { path: 'stats', component: StatsView },
      ],
      meta: { requiresAuth: true }
    }
  ]
});

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.user) {
    next('/login');
  } else {
    next();
  }
});

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
app.use(router);
app.mount('#app');
