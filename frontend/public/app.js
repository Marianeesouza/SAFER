// Página de teste integrada com geocodificacao-service e ocorrencias-service.
// Quem repassa a chamada aos serviços é o Nginx (veja nginx.conf).
const GEO_API = "/api/geocoding";
const OCORRENCIAS_API = "/api/ocorrencias";

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
  if (valid && page.id === "page2" && !latitudeInput.value && !addressInput.value.trim()) {
    setStatus("Marque o local no mapa ou informe um endereço.", true);
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

// ---------- Envio ao ocorrencias-service ----------

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!currentPageIsValid()) return;

  const data = Object.fromEntries(new FormData(form).entries());
  const baseAddress = (data.address || "").trim()
    || (data.latitude ? `Coordenadas (${Number(data.latitude).toFixed(6)}, ${Number(data.longitude).toFixed(6)}) - Campus UFRPE` : "");
  const reference = (data.reference || "").trim();
  const enderecoFinal = reference ? `${baseAddress} (Ref.: ${reference})` : baseAddress;

  const payload = {
    usuarioId: null,
    comunicante: {
      nome: data.name.trim(),
      email: data.email.trim(),
      telefone: data.mobile.trim() || (data.landline ? data.landline.trim() : null),
      relacaoUfrpe: data.ufrpeRelation
    },
    localizacao: {
      latitude: data.latitude ? Number(data.latitude) : null,
      longitude: data.longitude ? Number(data.longitude) : null,
      endereco: enderecoFinal
    },
    descricaoAnimal: `${data.animal.trim()}: ${data.description.trim()}`
  };

  const submitBtn = document.getElementById("submit-btn");
  const resultBox = document.getElementById("result-box");
  const resultTitle = document.getElementById("result-title");
  const resultHint = document.getElementById("result-hint");
  const resultEl = document.getElementById("result");

  submitBtn.disabled = true;
  try {
    const response = await fetch(OCORRENCIAS_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      resultTitle.textContent = "Erro ao registrar ocorrência";
      resultHint.textContent = body.detail || `Erro HTTP ${response.status}`;
      resultEl.textContent = JSON.stringify(body, null, 2);
      resultBox.hidden = false;
      return;
    }
    resultTitle.textContent = `Ocorrência registrada! Protocolo: ${body.protocolo}`;
    resultHint.textContent = "Salva com sucesso no ocorrencias-service:";
    resultEl.textContent = JSON.stringify(body, null, 2);
    resultBox.hidden = false;
  } catch (e) {
    resultTitle.textContent = "Erro de conexão";
    resultHint.textContent = "Não foi possível comunicar com o ocorrencias-service.";
    resultEl.textContent = e.message;
    resultBox.hidden = false;
  } finally {
    submitBtn.disabled = false;
  }
});
