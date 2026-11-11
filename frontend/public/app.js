// Página de teste: só conversa com o serviço de geocodificação, por um caminho relativo.
// Quem repassa a chamada ao serviço é o Nginx (veja nginx.conf).
const GEO_API = "/api/geocoding";

// Centro e zoom iniciais, aproveitados do protótipo anterior (campus da UFRPE).
const INITIAL_CENTER = [-8.017598865420956, -34.94933404268544];
const INITIAL_ZOOM = 15;

const form = document.getElementById("occurrence-form");
const addressInput = document.getElementById("address");
const searchButton = document.getElementById("search");
const statusEl = document.getElementById("status");
const coordsEl = document.getElementById("coords");
const badgeEl = document.getElementById("campus-badge");
const latitudeInput = document.getElementById("latitude");
const longitudeInput = document.getElementById("longitude");

// ---------- Chamadas à API ----------

async function api(path) {
  let response;
  try {
    response = await fetch(GEO_API + path);
  } catch (e) {
    throw new Error("Não foi possível falar com o serviço de geocodificação.");
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || "Erro " + response.status);
  }
  return response.json();
}

function setStatus(text, isError) {
  statusEl.textContent = text;
  statusEl.classList.toggle("error", Boolean(isError));
}

// ---------- Mapa ----------

const map = L.map("map").setView(INITIAL_CENTER, INITIAL_ZOOM);
let marker = null;
let busy = false; // evita várias consultas seguidas ao Nominatim

// As configurações dos tiles vêm do serviço (GET /map/config).
api("/map/config")
  .then(cfg => L.tileLayer(cfg.urlTemplate, { attribution: cfg.attribution, maxZoom: cfg.maxZoom }).addTo(map))
  .catch(e => setStatus(e.message, true));

function placeMarker(latlng) {
  if (marker) {
    marker.setLatLng(latlng);
  } else {
    marker = L.marker(latlng).addTo(map);
  }
  latitudeInput.value = latlng.lat;
  longitudeInput.value = latlng.lng;
  coordsEl.textContent = "Lat " + latlng.lat.toFixed(6) + ", Lon " + latlng.lng.toFixed(6);
}

async function checkCampus(latlng) {
  badgeEl.hidden = true;
  try {
    const result = await api("/campus/validate?latitude=" + latlng.lat + "&longitude=" + latlng.lng);
    badgeEl.textContent = result.insideCampus ? "Dentro do campus" : "Fora do campus";
    badgeEl.className = "badge " + (result.insideCampus ? "inside" : "outside");
    badgeEl.hidden = false;
  } catch (e) {
    setStatus(e.message, true);
  }
}

// Clique no mapa: marca o ponto e busca o endereço (GET /address).
map.on("click", async event => {
  if (busy) return;
  busy = true;
  const latlng = event.latlng.wrap();
  placeMarker(latlng);
  setStatus("Buscando o endereço...");
  try {
    const location = await api("/address?latitude=" + latlng.lat + "&longitude=" + latlng.lng);
    addressInput.value = location.address;
    setStatus("Endereço encontrado.");
  } catch (e) {
    setStatus(e.message, true); // o ponto continua marcado, só sem endereço
  }
  await checkCampus(latlng);
  busy = false;
});

// Busca por texto: encontra as coordenadas (GET /coordinates) e move o mapa.
async function searchAddress() {
  const text = addressInput.value.trim();
  if (!text) {
    setStatus("Digite o nome do local ou o endereço para buscar.", true);
    return;
  }
  if (busy) {
    setStatus("Aguarde a consulta anterior terminar.");
    return;
  }
  busy = true;
  searchButton.disabled = true;
  setStatus("Buscando o local...");
  try {
    const location = await api("/coordinates?address=" + encodeURIComponent(text));
    const latlng = L.latLng(location.coordinates.latitude, location.coordinates.longitude);
    placeMarker(latlng);
    map.setView(latlng, 17);
    setStatus("Encontrado: " + location.address);
    await checkCampus(latlng);
  } catch (e) {
    setStatus(e.message, true);
  }
  searchButton.disabled = false;
  busy = false;
}

searchButton.addEventListener("click", searchAddress);
addressInput.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    searchAddress();
  }
});

// ---------- Etapas do formulário ----------

function showPage(number) {
  document.querySelectorAll(".page").forEach(p => p.classList.remove("active"));
  document.getElementById("page" + number).classList.add("active");
  document.querySelectorAll("#steps li").forEach((li, index) => li.classList.toggle("active", index < number));
  if (number === 2) {
    setTimeout(() => map.invalidateSize(), 50); // o mapa estava escondido
  }
}

function currentPageIsValid() {
  const page = document.querySelector(".page.active");
  let valid = true;
  page.querySelectorAll("input, select, textarea").forEach(field => {
    const ok = field.checkValidity();
    field.classList.toggle("invalid", !ok);
    if (!ok && valid) {
      field.focus();
      valid = false;
    }
  });
  if (valid && page.id === "page2" && !latitudeInput.value) {
    setStatus("Marque o local no mapa ou busque um endereço.", true);
    valid = false;
  }
  return valid;
}

document.querySelectorAll("[data-go]").forEach(button => {
  button.addEventListener("click", () => {
    const target = Number(button.dataset.go);
    const goingForward = button.classList.contains("next");
    if (goingForward && !currentPageIsValid()) return;
    showPage(target);
  });
});

// ---------- Envio (somente exibe os dados) ----------

form.addEventListener("submit", event => {
  event.preventDefault();
  if (!currentPageIsValid()) return;
  const data = Object.fromEntries(new FormData(form).entries());
  document.getElementById("result").textContent = JSON.stringify(data, null, 2);
  document.getElementById("result-box").hidden = false;
});
