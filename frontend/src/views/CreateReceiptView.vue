<script setup>
import { ref, onUnmounted, nextTick, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import { useReceiptStore } from '../stores/receipts';
import { jsPDF } from 'jspdf';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import { Camera, FileUp, CheckCircle, Image as ImageIcon, ArrowLeft, Image, FileText, X, Receipt, Save, Calendar } from 'lucide-vue-next';

const router = useRouter();
const store = useReceiptStore();

const form = ref({
  storeName: '',
  productName: '',
  purchaseDate: new Date().toISOString().split('T')[0],
  price: '',
  category: 'Electronics',
  warrantyDuration: '1 Year'
});

const imagePreview = ref(null); // Used for cropping UI
const processedPreview = ref(null); // Used for final B/W preview
const pdfFile = ref(null);
const fileName = ref('');
const isProcessing = ref(false);
const showCropper = ref(false);
const isCropperReady = ref(false);
const cropperImgRef = ref(null);
const cropperInstance = ref(null);

// Additional refs for file inputs
const fileInput = ref(null);
const cameraInput = ref(null);
const pdfInput = ref(null);
const previewImage = ref(null); // To display the chosen image
const originalImage = ref(null); // For cropper

const categories = ['Electronics', 'Clothing & Footwear', 'Groceries', 'Home & Garden', 'Other'];
const warranties = ['6 Months', '1 Year', '2 Years', '3 Years', '5 Years', 'Lifetime', 'Other'];

const triggerFileInput = (type) => {
  if (type === 'image') fileInput.value.click();
  else if (type === 'pdf') pdfInput.value.click();
};

const triggerCamera = () => {
  cameraInput.value.click();
};

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // Reset states
  pdfFile.value = null;
  processedPreview.value = null;
  
  const reader = new FileReader();
  reader.onload = (e) => {
    originalImage.value = e.target.result;
    showCropper.value = true;
    // Wait for DOM update to ensure image element is rendered
    nextTick(() => {
        initCropper();
    });
  };
  reader.readAsDataURL(file);
};

const handlePdfChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  if (file.type !== 'application/pdf') {
    alert('Please upload a PDF file');
    return;
  }

  pdfFile.value = file;
  fileName.value = file.name;
  processedPreview.value = null; 
  previewImage.value = null;
};

const clearAttachment = () => {
    previewImage.value = null;
    pdfFile.value = null;
    processedPreview.value = null;
    fileName.value = '';
};

const initCropper = () => {
  const imageElement = cropperImgRef.value;
  if (!imageElement) return;

  // If already initialized for this image, don't do it again
  if (cropperInstance.value && cropperInstance.value.element === imageElement) {
    return;
  }

  if (cropperInstance.value) {
    cropperInstance.value.destroy();
    cropperInstance.value = null;
  }

  console.log('Initializing Cropper...');
  const instance = new Cropper(imageElement, {
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 1,
    responsive: true,
    restore: false,
    guides: true,
    center: true,
    highlight: false,
    cropBoxMovable: true,
    cropBoxResizable: true,
    toggleDragModeOnDblclick: false,
    ready() {
      console.log('Cropper ready');
      isCropperReady.value = true;
    },
    error(err) {
      console.error('Cropper error:', err);
    }
  });
  
  cropperInstance.value = markRaw(instance);
};

const cancelCrop = () => {
  showCropper.value = false;
  originalImage.value = null;
  if (cropperInstance.value) {
    cropperInstance.value.destroy();
    cropperInstance.value = null;
  }
};

const cropImage = async () => {
  if (!cropperInstance.value || !isCropperReady.value) {
    console.error('Cropper not ready or instance missing');
    return;
  }

  const instance = cropperInstance.value;

  // Check if getCroppedCanvas exists on the instance
  if (typeof instance.getCroppedCanvas !== 'function') {
    console.error('getCroppedCanvas is missing. Instance:', instance);
    alert('Greška sa cropper bibliotekom. Molimo osvežite stranicu.');
    return;
  }

  try {
    isProcessing.value = true;
    
    // Get cropped canvas
    const canvas = instance.getCroppedCanvas({
      maxWidth: 1024,
      maxHeight: 1024,
      imageSmoothingQuality: 'high',
    });

    if (!canvas) {
      throw new Error('Failed to get cropped canvas');
    }
    
    // Process for B/W High Contrast
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Failed to get canvas context');

    const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const data = imgData.data;

    // Grayscale conversion
    for (let i = 0; i < data.length; i += 4) {
      const gray = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
      data[i] = data[i+1] = data[i+2] = gray;
    }
    ctx.putImageData(imgData, 0, 0);

    // Sharpening (Convolution filter)
    const sharpenImage = (context, w, h) => {
      const weights = [0, -1, 0, -1, 5, -1, 0, -1, 0];
      const side = 3;
      const halfSide = 1;
      const pixels = context.getImageData(0, 0, w, h);
      const src = pixels.data;
      const output = context.createImageData(w, h);
      const dst = output.data;

      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          const sy = y;
          const sx = x;
          const dstOff = (y * w + x) * 4;
          let r = 0;
          
          for (let cy = 0; cy < side; cy++) {
            for (let cx = 0; cx < side; cx++) {
              const scy = sy + cy - halfSide;
              const scx = sx + cx - halfSide;
              if (scy >= 0 && scy < h && scx >= 0 && scx < w) {
                const srcOff = (scy * w + scx) * 4;
                const wt = weights[cy * side + cx];
                r += src[srcOff] * wt;
              }
            }
          }
          
          // Contrast enhancement after sharpening
          const contrast = 1.2; // Faktor kontrasta (1.0 je original, >1.0 pojačava)
          let finalVal = contrast * (r - 128) + 128;
          
          // Clamp values to 0-255
          finalVal = Math.max(0, Math.min(255, finalVal));
          
          dst[dstOff] = dst[dstOff+1] = dst[dstOff+2] = finalVal;
          dst[dstOff + 3] = 255;
        }
      }
      context.putImageData(output, 0, 0);
    };

    sharpenImage(ctx, canvas.width, canvas.height);
    
    // Show processed preview (Increased quality)
    previewImage.value = canvas.toDataURL('image/jpeg', 0.95);
    
    // Generate PDF (Higher quality settings)
    const pdf = new jsPDF();
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
    
    pdf.addImage(previewImage.value, 'JPEG', 0, 0, pdfWidth, pdfHeight);
    const pdfBlob = pdf.output('blob');
    
    pdfFile.value = new File([pdfBlob], "receipt.pdf", { type: "application/pdf" });
    
    showCropper.value = false;
  } catch (error) {
    console.error('Error during cropping/processing:', error);
    alert('Failed to process image: ' + error.message);
  } finally {
    isProcessing.value = false;
    if (cropperInstance.value) {
      cropperInstance.value.destroy();
      cropperInstance.value = null;
    }
  }
};

onUnmounted(() => {
  if (cropperInstance.value) cropperInstance.value.destroy();
});

const handleSubmit = async () => {
  try {
    isProcessing.value = true;
    const formData = new FormData();
    formData.append('storeName', form.value.storeName);
    formData.append('productName', form.value.productName);
    formData.append('purchaseDate', form.value.purchaseDate);
    formData.append('price', form.value.price);
    formData.append('category', form.value.category);
    formData.append('warrantyDuration', form.value.warrantyDuration);
    
    if (pdfFile.value) {
      formData.append('file', pdfFile.value);
    }

    const result = await store.createReceipt(formData);
    if (result.success) {
      router.push('/receipts');
    } else {
      alert('Failed to create receipt: ' + (result.error || 'Unknown error'));
    }
  } catch (error) {
    console.error('Submit error:', error);
    alert('An error occurred while saving');
  } finally {
    isProcessing.value = false;
  }
};
</script>

<template>
  <div class="create-receipt p-6 pb-24 max-w-lg mx-auto">
    <!-- Header -->
    <div class="flex items-center gap-4 mb-6">
      <button @click="$router.back()" class="p-2 rounded-full bg-white/5 hover:bg-white/10 text-white transition-colors">
        <ArrowLeft size="20" />
      </button>
      <h1 class="text-xl font-bold text-white">Add New Receipt</h1>
    </div>

    <form @submit.prevent="handleSubmit" class="flex flex-col gap-5">
      
      <!-- Store Name -->
      <div class="form-group">
        <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Store Name</label>
        <input 
          v-model="form.storeName" 
          type="text" 
          placeholder="Apple Store, Starbucks, etc." 
          required
          class="bg-white text-gray-900 placeholder-gray-400 rounded-xl px-4 py-3 w-full border-none focus:ring-2 focus:ring-primary"
        />
      </div>

      <!-- Product Name -->
      <div class="form-group">
        <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Product Name</label>
        <input 
          v-model="form.productName" 
          type="text" 
          placeholder="iPhone 15 Pro, Coffee Latte" 
          required
          class="bg-white text-gray-900 placeholder-gray-400 rounded-xl px-4 py-3 w-full border-none focus:ring-2 focus:ring-primary"
        />
      </div>

      <!-- Price & Date Row -->
      <div class="grid grid-cols-2 gap-4">
        <div class="form-group">
          <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Price</label>
          <div class="relative">
             <span class="absolute left-4 top-1/2 -translate-y-1/2 text-primary font-bold">$</span>
             <input 
               v-model="form.price" 
               type="number" 
               step="0.01" 
               placeholder="0.00" 
               required
               class="bg-white text-gray-900 placeholder-gray-400 rounded-xl pl-8 pr-4 py-3 w-full border-none focus:ring-2 focus:ring-primary font-mono"
             />
          </div>
        </div>

        <div class="form-group">
          <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Date</label>
          <div class="relative">
             <input 
               v-model="form.purchaseDate" 
               type="date" 
               required
               class="bg-white text-gray-900 placeholder-gray-400 rounded-xl px-4 py-3 w-full border-none focus:ring-2 focus:ring-primary"
             />
             <Calendar class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" size="18" />
          </div>
        </div>
      </div>

      <!-- Warranty & Category Row -->
      <div class="grid grid-cols-2 gap-4">
        <div class="form-group">
          <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Warranty</label>
          <select 
            v-model="form.warrantyDuration"
            class="bg-[#1a2c24] text-gray-300 rounded-xl px-4 py-3 w-full border border-white/10 focus:border-primary appearance-none"
          >
            <option value="None">None</option>
            <option value="1 Year">1 Year</option>
            <option value="2 Years">2 Years</option>
            <option value="3 Years">3 Years</option>
            <option value="Lifetime">Lifetime</option>
          </select>
        </div>

        <div class="form-group">
          <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Category</label>
          <select 
            v-model="form.category"
            class="bg-[#1a2c24] text-gray-300 rounded-xl px-4 py-3 w-full border border-white/10 focus:border-primary appearance-none"
          >
            <option value="General">General</option>
            <option value="Technology">Technology</option>
            <option value="Groceries">Groceries</option>
            <option value="Clothing">Clothing</option>
          </select>
        </div>
      </div>

      <!-- Attachments Section -->
      <div class="form-group mt-2">
        <label class="text-xs font-medium text-gray-400 ml-1 mb-1 block">Add Attachment</label>
        
        <div class="grid grid-cols-3 gap-3 mb-4">
           <!-- Gallery Upload -->
           <button 
             type="button" 
             @click="triggerFileInput('image')" 
             class="flex flex-col items-center justify-center gap-2 p-4 rounded-xl bg-[#1a2c24] border border-white/5 hover:bg-[#23382f] transition-colors group"
           >
             <Image class="text-primary group-hover:scale-110 transition-transform" size="24" />
             <span class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Gallery</span>
           </button>
           
           <!-- Camera Capture -->
           <button 
             type="button" 
             @click="triggerCamera" 
             class="flex flex-col items-center justify-center gap-2 p-4 rounded-xl bg-[#1a2c24] border border-white/5 hover:bg-[#23382f] transition-colors group"
           >
             <Camera class="text-primary group-hover:scale-110 transition-transform" size="24" />
             <span class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Camera</span>
           </button>

           <!-- PDF Upload -->
           <button 
             type="button" 
             @click="triggerFileInput('pdf')" 
             class="flex flex-col items-center justify-center gap-2 p-4 rounded-xl bg-[#1a2c24] border border-white/5 hover:bg-[#23382f] transition-colors group"
           >
             <FileText class="text-primary group-hover:scale-110 transition-transform" size="24" />
             <span class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">PDF</span>
           </button>
        </div>

        <!-- Hidden Inputs -->
        <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
        <input ref="cameraInput" type="file" accept="image/*" capture="environment" class="hidden" @change="handleFileChange" />
        <input ref="pdfInput" type="file" accept="application/pdf" class="hidden" @change="handlePdfChange" />

        <!-- Preview Area -->
        <div v-if="previewImage || pdfFile" class="relative rounded-xl overflow-hidden border-2 border-dashed border-primary/30 min-h-[150px] bg-[#1a2c24] flex items-center justify-center">
           <img v-if="previewImage" :src="previewImage" class="w-full h-full object-cover opacity-80" />
           <div v-else-if="pdfFile" class="text-center p-4">
              <FileText size="40" class="text-primary mx-auto mb-2" />
              <p class="text-sm text-white font-medium">{{ pdfFile.name }}</p>
           </div>
           
           <!-- Remove Button -->
           <button @click="clearAttachment" class="absolute top-2 right-2 bg-black/50 p-1.5 rounded-full hover:bg-red-500/80 transition-colors text-white">
              <X size="16" />
           </button>
        </div>
        
        <!-- Empty State Preview -->
        <div v-else class="rounded-xl border-2 border-dashed border-white/10 min-h-[150px] bg-[#1a2c24] flex flex-col items-center justify-center text-gray-500">
           <Receipt size="40" class="opacity-20 mb-2" />
           <p class="text-xs">No receipt attached yet</p>
        </div>
      </div>

      <!-- Submit Button -->
      <button 
        type="submit" 
        class="mt-4 w-full py-4 bg-primary hover:bg-primary-hover text-black font-bold text-lg rounded-xl shadow-[0_0_20px_rgba(0,255,102,0.3)] transition-all transform hover:scale-[1.02] flex items-center justify-center gap-2"
        :disabled="isProcessing"
      >
        <span v-if="isProcessing" class="animate-spin rounded-full h-5 w-5 border-b-2 border-black"></span>
        <span v-else class="flex items-center gap-2"><Save size="20" /> SAVE RECEIPT</span>
      </button>

    </form>

    <!-- Cropper Modal -->
    <div v-if="showCropper" class="fixed inset-0 z-50 bg-black/90 backdrop-blur-sm flex flex-col items-center justify-center p-4">
      <div class="w-full max-w-lg flex justify-between items-center mb-4 px-2">
         <button @click="cancelCrop" class="text-white font-medium px-4 py-2 rounded-lg hover:bg-white/10">Cancel</button>
         <button @click="cropImage" class="bg-primary text-black font-bold px-6 py-2 rounded-lg">Done</button>
      </div>
      
      <div class="relative w-full h-[60vh] bg-[#1a2c24] rounded-2xl overflow-hidden shadow-2xl border border-white/10">
        <img ref="cropperImg" :src="originalImage" class="max-w-full block" />
      </div>
      
      <p class="text-gray-400 text-sm mt-4 text-center">Drag corners to crop the receipt</p>
    </div>

  </div>
</template>

<style scoped>
.form-group input:focus, .form-group select:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--color-primary);
}
</style>