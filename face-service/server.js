/*
 * Service interne de calcul des empreintes faciales (128 reels) pour MediLinkPro.
 *
 * Meme modele que le navigateur (face-api : detecteur SSD MobileNet v1, reperes 68 points,
 * reseau de reconnaissance) : les empreintes calculees ici sont comparables a celles calculees
 * dans le navigateur. Backend TensorFlow.js WASM (aucune dependance native a compiler).
 *
 *   POST /empreinte?visageUnique=true   corps = image JPEG ou PNG (octets bruts)
 *     200 { descripteur: number[128], visages: n }
 *     422 { erreur: "..." }  aucun visage, ou plusieurs quand visageUnique=true
 *   GET  /sante                         200 { ok: true }
 *
 * A n'exposer que sur le reseau interne (docker-compose). Si FACE_SERVICE_TOKEN est defini,
 * chaque appel doit porter l'en-tete X-Face-Service-Token correspondant.
 */
const http = require('http');
const path = require('path');
const tf = require('@tensorflow/tfjs');
const wasm = require('@tensorflow/tfjs-backend-wasm');
const faceapi = require('@vladmandic/face-api/dist/face-api.node-wasm.js');
const jpeg = require('jpeg-js');
const { PNG } = require('pngjs');

const PORT = Number(process.env.PORT || 8095);
const JETON = process.env.FACE_SERVICE_TOKEN || '';
const TAILLE_MAX_IMAGE = 8 * 1024 * 1024;
const COTE_MAX_PX = 640;
const MODELES = path.join(path.dirname(require.resolve('@vladmandic/face-api/package.json')), 'model');

function decoder(octets) {
  if (octets[0] === 0xff && octets[1] === 0xd8) {
    const img = jpeg.decode(octets, { useTArray: true, formatAsRGBA: true, maxMemoryUsageInMB: 256 });
    return { largeur: img.width, hauteur: img.height, rgba: img.data };
  }
  if (octets[0] === 0x89 && octets[1] === 0x50) {
    const img = PNG.sync.read(octets);
    return { largeur: img.width, hauteur: img.height, rgba: img.data };
  }
  throw new Error('Format non supporte (JPEG ou PNG attendu)');
}

/** Tenseur RGB [1, h, w, 3] redimensionne (max 640 px), eventuellement entoure d'une marge grise. */
function versTenseur({ largeur, hauteur, rgba }, marge) {
  return tf.tidy(() => {
    const rgb = new Uint8Array(largeur * hauteur * 3);
    for (let i = 0, j = 0; i < rgba.length; i += 4, j += 3) {
      rgb[j] = rgba[i]; rgb[j + 1] = rgba[i + 1]; rgb[j + 2] = rgba[i + 2];
    }
    let image = tf.tensor3d(rgb, [hauteur, largeur, 3], 'int32');
    const ratio = Math.min(1, COTE_MAX_PX / Math.max(largeur, hauteur));
    if (ratio < 1) {
      image = tf.image.resizeBilinear(image, [Math.round(hauteur * ratio), Math.round(largeur * ratio)]);
    }
    if (marge) {
      // Comme cote navigateur : le detecteur rate les visages qui remplissent tout le cadre.
      const m = Math.round(Math.max(image.shape[0], image.shape[1]) * 0.5);
      image = tf.pad(image, [[m, m], [m, m], [0, 0]], 128);
    }
    return image.toInt().expandDims(0);
  });
}

async function detecter(image) {
  const entree = versTenseur(image, false);
  try {
    let detections = await faceapi.detectAllFaces(entree, new faceapi.SsdMobilenetv1Options({ minConfidence: 0.5 }))
      .withFaceLandmarks().withFaceDescriptors();
    if (detections.length === 0) {
      const avecMarge = versTenseur(image, true);
      try {
        detections = await faceapi.detectAllFaces(avecMarge, new faceapi.SsdMobilenetv1Options({ minConfidence: 0.5 }))
          .withFaceLandmarks().withFaceDescriptors();
      } finally {
        avecMarge.dispose();
      }
    }
    return detections;
  } finally {
    entree.dispose();
  }
}

function repondre(res, statut, corps) {
  res.writeHead(statut, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(corps));
}

function lireCorps(req) {
  return new Promise((resolve, reject) => {
    const morceaux = [];
    let taille = 0;
    req.on('data', (m) => {
      taille += m.length;
      if (taille > TAILLE_MAX_IMAGE) { reject(new Error('Image trop volumineuse')); req.destroy(); return; }
      morceaux.push(m);
    });
    req.on('end', () => resolve(Buffer.concat(morceaux)));
    req.on('error', reject);
  });
}

async function demarrer() {
  wasm.setWasmPaths(path.join(path.dirname(require.resolve('@tensorflow/tfjs-backend-wasm/package.json')), 'dist') + path.sep);
  await tf.setBackend('wasm');
  await tf.ready();
  await faceapi.nets.ssdMobilenetv1.loadFromDisk(MODELES);
  await faceapi.nets.faceLandmark68Net.loadFromDisk(MODELES);
  await faceapi.nets.faceRecognitionNet.loadFromDisk(MODELES);

  http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://local');
    if (req.method === 'GET' && url.pathname === '/sante') return repondre(res, 200, { ok: true });
    if (req.method !== 'POST' || url.pathname !== '/empreinte') return repondre(res, 404, { erreur: 'Introuvable' });
    if (JETON && req.headers['x-face-service-token'] !== JETON) return repondre(res, 401, { erreur: 'Non autorise' });
    try {
      const image = decoder(await lireCorps(req));
      const detections = await detecter(image);
      if (detections.length === 0) {
        return repondre(res, 422, { erreur: 'Aucun visage detecte. Photo de face, bien eclairee, sans lunettes de soleil.' });
      }
      if (url.searchParams.get('visageUnique') === 'true' && detections.length > 1) {
        return repondre(res, 422, { erreur: 'Plusieurs visages detectes : la photo ne doit montrer que le patient.' });
      }
      const aire = (d) => d.detection.box.width * d.detection.box.height;
      const principal = detections.reduce((a, b) => (aire(b) > aire(a) ? b : a));
      return repondre(res, 200, { descripteur: Array.from(principal.descriptor), visages: detections.length });
    } catch (e) {
      return repondre(res, 400, { erreur: e.message });
    }
  }).listen(PORT, () => console.log(`face-service pret sur le port ${PORT} (backend ${tf.getBackend()})`));
}

demarrer().catch((e) => {
  console.error('Demarrage impossible :', e);
  process.exit(1);
});
