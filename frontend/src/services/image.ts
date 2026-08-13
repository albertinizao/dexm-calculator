const MAX_SOURCE_FILE_BYTES = 15 * 1024 * 1024;
const MAX_IMAGE_PIXELS = 16_000_000;
export const MAX_IMAGE_DATA_URL_LENGTH = 150 * 1024;
const MAX_IMAGE_DIMENSION = 1024;
const SUPPORTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function imageError(message: string): Error {
  return new Error(message);
}

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(imageError('No se ha podido procesar la imagen seleccionada.'));
    };
    image.src = objectUrl;
  });
}

/**
 * Converts a local portrait to a small, Firestore-safe JPEG data URL.
 * External URLs must bypass this helper and be kept as entered by the user.
 */
export async function processPortraitFile(file: File): Promise<string> {
  if (!SUPPORTED_IMAGE_TYPES.has(file.type)) {
    throw imageError('Formato no compatible. Usa una imagen JPEG, PNG o WebP.');
  }
  if (file.size > MAX_SOURCE_FILE_BYTES) {
    throw imageError('La imagen original supera los 15 MB y no se puede procesar.');
  }

  const image = await loadImage(file);
  if (!image.naturalWidth || !image.naturalHeight) {
    throw imageError('La imagen no tiene unas dimensiones válidas.');
  }
  if (image.naturalWidth * image.naturalHeight > MAX_IMAGE_PIXELS) {
    throw imageError('La imagen tiene demasiados píxeles para procesarse con seguridad.');
  }

  let dimension = Math.min(MAX_IMAGE_DIMENSION, Math.max(image.naturalWidth, image.naturalHeight));
  while (dimension >= 1) {
    const scale = Math.min(1, dimension / Math.max(image.naturalWidth, image.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
    const context = canvas.getContext('2d');
    if (!context) throw imageError('Tu navegador no permite procesar imágenes en este momento.');
    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    for (const quality of [0.82, 0.72, 0.62, 0.52, 0.42, 0.32]) {
      const dataUrl = canvas.toDataURL('image/jpeg', quality);
      if (dataUrl.length <= MAX_IMAGE_DATA_URL_LENGTH) return dataUrl;
    }
    if (dimension === 1) break;
    dimension = Math.max(1, Math.floor(dimension * 0.8));
  }

  throw imageError('No se ha podido comprimir la imagen por debajo de 150 KB. Elige otra imagen.');
}
