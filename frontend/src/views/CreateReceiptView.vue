<script setup>
import { ref, onUnmounted, nextTick, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import { useReceiptStore } from '../stores/receipts';
import { jsPDF } from 'jspdf';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import { Camera, FileUp, CheckCircle, Image as ImageIcon } from 'lucide-vue-next';

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

const categories = ['Electronics', 'Clothing & Footwear', 'Groceries', 'Home & Garden', 'Other'];
const warranties = ['6 Months', '1 Year', '2 Years', '3 Years', '5 Years', 'Lifetime', 'Other'];

const handlePdfUpload = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  if (file.type !== 'application/pdf') {
    alert('Please upload a PDF file');
    return;
  }

  pdfFile.value = file;
  fileName.value = file.name;
  processedPreview.value = null; // Clear image preview if PDF uploaded
};

const handleImageUpload = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  processedPreview.value = null;
  pdfFile.value = null;
  fileName.value = file.name;
  isCropperReady.value = false;

  const reader = new FileReader();
  reader.onload = (e) => {
    imagePreview.value = e.target.result;
    showCropper.value = true;
  };
  reader.readAsDataURL(file);
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
  imagePreview.value = null;
  if (cropperInstance.value) {
    cropperInstance.value.destroy();
    cropperInstance.value = null;
  }
};

const confirmCrop = async () => {
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
    processedPreview.value = canvas.toDataURL('image/jpeg', 0.95);
    
    // Generate PDF (Higher quality settings)
    const pdf = new jsPDF();
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
    
    pdf.addImage(processedPreview.value, 'JPEG', 0, 0, pdfWidth, pdfHeight);
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
  <div class="container max-w-lg">
    <h1 class="mb-4">Add Receipt</h1>
    
    <!-- Cropper Modal -->
    <div v-if="showCropper" class="fixed inset-0 z-[60] flex items-center justify-center p-2 sm:p-4 bg-black/95 backdrop-blur-md">
      <div class="bg-darker w-full max-w-4xl max-h-[98vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden border border-gray-800">
        <!-- Actions moved to top as requested -->
        <div class="p-4 bg-darker flex justify-between gap-3 border-b border-gray-800">
          <button @click="cancelCrop" type="button" class="secondary px-6" :disabled="isProcessing">Cancel</button>
          <button @click="confirmCrop" type="button" class="primary px-8" :disabled="!isCropperReady || isProcessing">
            {{ isProcessing ? 'Processing...' : (isCropperReady ? 'Save Selection' : 'Loading...') }}
          </button>
        </div>
        
        <div class="flex-1 relative bg-black min-h-[300px] overflow-hidden cropper-container-wrapper">
          <img ref="cropperImgRef" :src="imagePreview" class="block max-w-full" @load="initCropper" />
        </div>
      </div>
    </div>

    <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
      <div>
        <label>Store Name</label>
        <input v-model="form.storeName" required placeholder="e.g. Best Buy" />
      </div>
      
      <div>
        <label>Product Name</label>
        <input v-model="form.productName" required placeholder="e.g. Laptop" />
      </div>
      
      <div class="flex gap-4">
        <div class="w-full">
          <label>Date</label>
          <input v-model="form.purchaseDate" type="date" required />
        </div>
        <div class="w-full">
          <label>Price</label>
          <input v-model="form.price" type="number" step="0.01" required placeholder="0.00" />
        </div>
      </div>
      
      <div>
        <label>Category</label>
        <select v-model="form.category">
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
      </div>

      <div>
        <label>Warranty Duration</label>
        <select v-model="form.warrantyDuration">
          <option v-for="dur in warranties" :key="dur" :value="dur">{{ dur }}</option>
        </select>
      </div>

      <div class="card bg-darker p-5 border border-gray-800">
        <label class="block mb-4 font-bold text-lg">Receipt Document (Optional)</label>
        
        <div class="grid grid-cols-3 gap-2 sm:gap-4 mb-4">
          <!-- Take Photo (Camera) -->
          <label class="upload-option">
            <input type="file" accept="image/*" capture="environment" @change="handleImageUpload" class="hidden" />
            <div class="flex flex-col items-center gap-2 p-2 sm:p-4 rounded-xl border-2 border-dashed border-gray-700 hover:border-primary hover:bg-primary/5 transition-all cursor-pointer text-center">
              <Camera class="text-primary" size="24" />
              <span class="text-[10px] sm:text-xs font-medium">Camera</span>
            </div>
          </label>

          <!-- Upload Photo (Gallery) -->
          <label class="upload-option">
            <input type="file" accept="image/*" @change="handleImageUpload" class="hidden" />
            <div class="flex flex-col items-center gap-2 p-2 sm:p-4 rounded-xl border-2 border-dashed border-gray-700 hover:border-primary hover:bg-primary/5 transition-all cursor-pointer text-center">
              <ImageIcon class="text-primary" size="24" />
              <span class="text-[10px] sm:text-xs font-medium">Photo</span>
            </div>
          </label>

          <!-- PDF Option -->
          <label class="upload-option">
            <input type="file" accept="application/pdf" @change="handlePdfUpload" class="hidden" />
            <div class="flex flex-col items-center gap-2 p-2 sm:p-4 rounded-xl border-2 border-dashed border-gray-700 hover:border-primary hover:bg-primary/5 transition-all cursor-pointer text-center">
              <FileUp class="text-primary" size="24" />
              <span class="text-[10px] sm:text-xs font-medium">PDF</span>
            </div>
          </label>
        </div>
        
        <!-- Status Indicator -->
        <div v-if="pdfFile" class="flex items-center gap-2 p-3 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400">
          <CheckCircle size="18" />
          <span class="text-sm font-medium truncate">Ready: {{ fileName }}</span>
        </div>

        <div v-if="processedPreview" class="preview-box mt-4">
          <p class="text-xs text-gray-400 mb-2">Scanned Image Preview:</p>
          <img :src="processedPreview" alt="Processed Preview" class="w-full rounded-lg border border-gray-700 shadow-lg" />
        </div>
        
        <p v-if="isProcessing" class="text-center mt-4 text-primary animate-pulse">Processing document...</p>
      </div>

      <button type="submit" class="primary py-3 text-lg" :disabled="isProcessing || showCropper">
        Save Receipt
      </button>
    </form>
  </div>
</template>

<style scoped>
.cropper-container-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.cropper-container) {
  width: 100% !important;
  height: 100% !important;
}
img {
  max-width: 100%;
  display: block;
}
</style>
