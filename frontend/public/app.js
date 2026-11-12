// Front-end integrado com geocodificacao-service (/api/geocoding) e ocorrencias-service (/api/ocorrencias).
// Quem repassa as chamadas aos microsserviços é o Nginx (veja nginx.conf).
const GEO_API = "/api/geocoding";
const OCORRENCIAS_API = "/api/ocorrencias";

// Centro e zoom iniciais (campus da UFRPE).
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

let ocorrenciaAtualDetalhe = null;

// ---------- Navegação entre Abas (CDU05, CDU06/08, CDU07) ----------

function switchTab(tabId) {
  document.querySelectorAll(".nav-tab").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === tabId);
  });
  document.querySelectorAll(".tab-panel").forEach(panel => {
    panel.classList.toggle("active", panel.id === tabId);
  });
  if (tabId === "tab-registrar") {
    setTimeout(() => map.invalidateSize(), 60);
  } else if (tabId === "tab-analista") {
    carregarPainelAnalista();
  }
}

document.querySelectorAll(".nav-tab").forEach(btn => {
  btn.addEventListener("click", () => switchTab(btn.dataset.tab));
});

// ---------- Utilitários ----------

function formatDateTime(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("pt-BR");
}

async function geoApi(path) {
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

async function occApi(path = "", options = {}) {
  let response;
  try {
    response = await fetch(OCORRENCIAS_API + path, {
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
      ...options
    });
  } catch (e) {
    throw new Error("Não foi possível falar com o serviço de ocorrências.");
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const msg = body.detail || body.title || ("Erro HTTP " + response.status);
    const err = new Error(msg);
    err.problem = body;
    err.status = response.status;
    throw err;
  }
  return body;
}

function setStatus(text, isError) {
  statusEl.textContent = text;
  statusEl.classList.toggle("error", Boolean(isError));
}

function setFeedback(el, text, isError = false) {
  el.textContent = text;
  el.classList.toggle("error", Boolean(isError));
}

// ---------- ABA 1: Mapa e Registro de Ocorrência (CDU05) ----------

const map = L.map("map").setView(INITIAL_CENTER, INITIAL_ZOOM);
let marker = null;
let busy = false;

geoApi("/map/config")
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
    const result = await geoApi("/campus/validate?latitude=" + latlng.lat + "&longitude=" + latlng.lng);
    badgeEl.textContent = result.insideCampus ? "Dentro do campus" : "Fora do campus";
    badgeEl.className = "badge " + (result.insideCampus ? "inside" : "outside");
    badgeEl.hidden = false;
  } catch (e) {
    setStatus(e.message, true);
  }
}

map.on("click", async event => {
  if (busy) return;
  busy = true;
  const latlng = event.latlng.wrap();
  placeMarker(latlng);
  setStatus("Buscando o endereço...");
  try {
    const location = await geoApi("/address?latitude=" + latlng.lat + "&longitude=" + latlng.lng);
    addressInput.value = location.address;
    setStatus("Endereço encontrado.");
  } catch (e) {
    setStatus(e.message, true);
  }
  await checkCampus(latlng);
  busy = false;
});

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
    const location = await geoApi("/coordinates?address=" + encodeURIComponent(text));
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

function showPage(number) {
  document.querySelectorAll(".page").forEach(p => p.classList.remove("active"));
  document.getElementById("page" + number).classList.add("active");
  document.querySelectorAll("#steps li").forEach((li, index) => li.classList.toggle("active", index < number));
  if (number === 2) {
    setTimeout(() => map.invalidateSize(), 50);
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

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!currentPageIsValid()) return;

  const data = Object.fromEntries(new FormData(form).entries());
  const baseAddress = (data.address || "").trim()
    || (data.latitude ? `Coordenadas (${Number(data.latitude).toFixed(6)}, ${Number(data.longitude).toFixed(6)}) - Campus UFRPE` : "");
  const reference = (data.reference || "").trim();
  const enderecoFinal = reference ? `${baseAddress} (Ref.: ${reference})` : baseAddress;

  const payload = {
    usuarioId: data.userId ? Number(data.userId) : null,
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
  const resultActions = document.getElementById("result-actions");
  const resultEl = document.getElementById("result");
  const btnVerCriada = document.getElementById("btn-ver-criada");

  submitBtn.disabled = true;
  resultActions.hidden = true;
  try {
    const criada = await occApi("", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    resultTitle.textContent = `Ocorrência registrada! Protocolo: ${criada.protocolo}`;
    resultHint.textContent = `ID #${criada.id} criado com status ${criada.status}.`;
    resultEl.textContent = JSON.stringify(criada, null, 2);
    resultActions.hidden = false;
    btnVerCriada.onclick = () => abrirDetalhePorId(criada.id);
    resultBox.hidden = false;
  } catch (e) {
    resultTitle.textContent = "Erro ao registrar ocorrência";
    resultHint.textContent = e.message;
    resultEl.textContent = JSON.stringify(e.problem || { erro: e.message }, null, 2);
    resultBox.hidden = false;
  } finally {
    submitBtn.disabled = false;
  }
});

// ---------- ABA 2: Consultar Detalhes, Histórico e Cancelar (CDU06 e CDU08) ----------

const consultaFeedback = document.getElementById("consulta-feedback");
const usuarioBox = document.getElementById("usuario-ocorrencias-box");
const usuarioLista = document.getElementById("usuario-ocorrencias-lista");
const usuarioTitulo = document.getElementById("usuario-ocorrencias-titulo");
const detalheBox = document.getElementById("detalhe-ocorrencia-box");
const cancelFeedback = document.getElementById("cancel-feedback");

function renderizarDetalheOcorrencia(det) {
  ocorrenciaAtualDetalhe = det;
  detalheBox.hidden = false;
  setFeedback(cancelFeedback, "");

  document.getElementById("det-protocolo").textContent = `${det.protocolo} (ID #${det.id})`;
  document.getElementById("det-subtitulo").textContent =
    `Criada em ${formatDateTime(det.criadoEm)} · Última atualização: ${formatDateTime(det.atualizadoEm)}`;

  const badge = document.getElementById("det-status-badge");
  badge.textContent = det.status;
  badge.className = `status-pill status-${det.status}`;

  document.getElementById("det-animal").textContent = det.descricaoAnimal || "—";

  const loc = det.localizacao || {};
  const coordsTxt = (loc.latitude != null && loc.longitude != null)
    ? `\nCoordenadas: ${loc.latitude}, ${loc.longitude}`
    : "\n(Sem coordenadas — modo contingência)";
  document.getElementById("det-localizacao").textContent = (loc.endereco || "—") + coordsTxt;

  const com = det.comunicante || {};
  document.getElementById("det-comunicante").textContent =
    `${com.nome || "—"} (${com.relacaoUfrpe || "—"})\nE-mail: ${com.email || "—"}\nTel: ${com.telefone || "—"}${det.usuarioId ? `\nID Usuário: ${det.usuarioId}` : ""}`;

  document.getElementById("det-atendimento").textContent = det.analistaResponsavelId
    ? `Analista responsável ID #${det.analistaResponsavelId}`
    : "Aguardando analista assumir o chamado";

  if (det.usuarioId) {
    document.getElementById("cancel-usuario-id").value = det.usuarioId;
  }

  // Renderizar linha do tempo do histórico
  const ul = document.getElementById("det-historico");
  ul.innerHTML = "";
  const hist = det.historico || [];
  if (hist.length === 0) {
    const li = document.createElement("li");
    li.textContent = "Nenhum registro no histórico.";
    ul.appendChild(li);
  } else {
    hist.forEach(item => {
      const li = document.createElement("li");
      li.innerHTML = `
        <div class="tl-header">
          <span class="status-pill status-${item.statusRegistrado}">${item.statusRegistrado}</span>
          <span>${formatDateTime(item.dataHora)}${item.usuarioId ? ` · Usuário/Analista #${item.usuarioId}` : ""}</span>
        </div>
        <p class="tl-obs"></p>
      `;
      li.querySelector(".tl-obs").textContent = item.observacao || "—";
      ul.appendChild(li);
    });
  }
}

async function abrirDetalhePorId(id) {
  switchTab("tab-consultar");
  document.getElementById("input-busca-id").value = id;
  setFeedback(consultaFeedback, "Buscando ocorrência #" + id + "...");
  try {
    const det = await occApi("/" + encodeURIComponent(id));
    setFeedback(consultaFeedback, "Ocorrência encontrada.");
    renderizarDetalheOcorrencia(det);
  } catch (e) {
    detalheBox.hidden = true;
    setFeedback(consultaFeedback, e.message, true);
  }
}

document.getElementById("form-busca-protocolo").addEventListener("submit", async event => {
  event.preventDefault();
  const protocolo = document.getElementById("input-busca-protocolo").value.trim();
  if (!protocolo) return;
  usuarioBox.hidden = true;
  setFeedback(consultaFeedback, "Buscando protocolo " + protocolo + "...");
  try {
    const det = await occApi("/protocolo/" + encodeURIComponent(protocolo));
    setFeedback(consultaFeedback, "Ocorrência encontrada.");
    renderizarDetalheOcorrencia(det);
  } catch (e) {
    detalheBox.hidden = true;
    setFeedback(consultaFeedback, e.message, true);
  }
});

document.getElementById("form-busca-id").addEventListener("submit", async event => {
  event.preventDefault();
  const id = document.getElementById("input-busca-id").value.trim();
  if (!id) return;
  usuarioBox.hidden = true;
  await abrirDetalhePorId(id);
});

document.getElementById("form-busca-usuario").addEventListener("submit", async event => {
  event.preventDefault();
  const usuarioId = document.getElementById("input-busca-usuario").value.trim();
  if (!usuarioId) return;
  detalheBox.hidden = true;
  setFeedback(consultaFeedback, `Listando ocorrências do usuário #${usuarioId}...`);
  try {
    const lista = await occApi("/usuario/" + encodeURIComponent(usuarioId));
    usuarioTitulo.textContent = `Ocorrências do Usuário #${usuarioId} (${lista.length})`;
    usuarioLista.innerHTML = "";
    if (lista.length === 0) {
      usuarioLista.innerHTML = `<p class="hint">Nenhuma ocorrência encontrada para este usuário.</p>`;
    } else {
      lista.forEach(occ => {
        const div = document.createElement("div");
        div.className = "occ-card";
        div.innerHTML = `
          <div class="occ-card-top">
            <span class="occ-card-title">${occ.protocolo} (ID #${occ.id})</span>
            <span class="status-pill status-${occ.status}">${occ.status}</span>
          </div>
          <p class="occ-card-meta"><strong>Animal:</strong> <span class="occ-desc"></span></p>
          <p class="occ-card-meta"><strong>Local:</strong> <span class="occ-end"></span></p>
          <div class="occ-card-actions">
            <button type="button" class="btn-ver-det">Ver Detalhes / Histórico / Cancelar</button>
          </div>
        `;
        div.querySelector(".occ-desc").textContent = occ.descricaoAnimal || "—";
        div.querySelector(".occ-end").textContent = occ.localizacao?.endereco || "—";
        div.querySelector(".btn-ver-det").addEventListener("click", () => abrirDetalhePorId(occ.id));
        usuarioLista.appendChild(div);
      });
    }
    usuarioBox.hidden = false;
    setFeedback(consultaFeedback, `${lista.length} ocorrência(s) carregada(s).`);
  } catch (e) {
    usuarioBox.hidden = true;
    setFeedback(consultaFeedback, e.message, true);
  }
});

// Cancelar ocorrência (CDU08)
document.getElementById("form-cancelar").addEventListener("submit", async event => {
  event.preventDefault();
  if (!ocorrenciaAtualDetalhe) return;

  const usuarioId = Number(document.getElementById("cancel-usuario-id").value);
  const justificativa = document.getElementById("cancel-justificativa").value.trim();
  if (!justificativa) {
    setFeedback(cancelFeedback, "Informe a justificativa para cancelar a ocorrência.", true);
    return;
  }

  const btn = document.getElementById("btn-cancelar-ocorrencia");
  btn.disabled = true;
  setFeedback(cancelFeedback, "Enviando solicitação de cancelamento...");
  try {
    await occApi(`/${ocorrenciaAtualDetalhe.id}/cancelar`, {
      method: "PATCH",
      body: JSON.stringify({ usuarioId, justificativa })
    });
    document.getElementById("cancel-justificativa").value = "";
    const atualizado = await occApi(`/${ocorrenciaAtualDetalhe.id}`);
    renderizarDetalheOcorrencia(atualizado);
    setFeedback(cancelFeedback, "Ocorrência cancelada com sucesso!");
  } catch (e) {
    setFeedback(cancelFeedback, e.message, true);
  } finally {
    btn.disabled = false;
  }
});

// ---------- ABA 3: Painel do Analista (CDU07) ----------

const analistaFeedback = document.getElementById("analista-feedback");
const analistaLista = document.getElementById("analista-lista");
const filtroStatusSelect = document.getElementById("filtro-status-select");
const analistaIdInput = document.getElementById("analista-id-input");

function opcoesTransicao(statusAtual) {
  if (statusAtual === "EM_ATENDIMENTO") {
    return [
      "NECESSITA_ATENDIMENTO_MEDICO",
      "FINALIZADO_COM_SUCESSO",
      "FINALIZADO_COM_COMPLICACOES"
    ];
  }
  if (statusAtual === "NECESSITA_ATENDIMENTO_MEDICO") {
    return [
      "FINALIZADO_COM_SUCESSO",
      "FINALIZADO_COM_COMPLICACOES"
    ];
  }
  return [];
}

async function carregarPainelAnalista() {
  const statusFiltro = filtroStatusSelect.value;
  setFeedback(analistaFeedback, "Carregando ocorrências...");
  try {
    let lista;
    if (statusFiltro === "EM_ABERTO") {
      lista = await occApi("/em-aberto");
    } else if (statusFiltro) {
      lista = await occApi("?status=" + encodeURIComponent(statusFiltro));
    } else {
      lista = await occApi("");
    }
    renderizarListaAnalista(lista);
    setFeedback(analistaFeedback, `${lista.length} ocorrência(s) listada(s).`);
  } catch (e) {
    analistaLista.innerHTML = "";
    setFeedback(analistaFeedback, e.message, true);
  }
}

function renderizarListaAnalista(lista) {
  analistaLista.innerHTML = "";
  if (lista.length === 0) {
    analistaLista.innerHTML = `<div class="card"><p class="hint">Nenhuma ocorrência encontrada para o filtro selecionado.</p></div>`;
    return;
  }

  lista.forEach(occ => {
    const card = document.createElement("div");
    card.className = "occ-card";

    const opcoes = opcoesTransicao(occ.status);
    const podeAssumir = occ.status === "EM_ABERTO" && !occ.analistaResponsavelId;

    card.innerHTML = `
      <div class="occ-card-top">
        <span class="occ-card-title">${occ.protocolo} (ID #${occ.id})</span>
        <span class="status-pill status-${occ.status}">${occ.status}</span>
      </div>
      <p class="occ-card-meta"><strong>Animal / Descrição:</strong> <span class="c-animal"></span></p>
      <p class="occ-card-meta"><strong>Localização:</strong> <span class="c-local"></span></p>
      <p class="occ-card-meta"><strong>Comunicante:</strong> <span class="c-comunicante"></span></p>
      <p class="occ-card-meta"><strong>Analista Responsável:</strong> ${occ.analistaResponsavelId ? `ID #${occ.analistaResponsavelId}` : "Não vinculado"} · <strong>Criada em:</strong> ${formatDateTime(occ.criadoEm)}</p>
      <p class="status c-msg" role="status"></p>
      <div class="occ-card-actions">
        <button type="button" class="outline btn-detalhe">Ver Detalhes / Histórico</button>
      </div>
    `;

    card.querySelector(".c-animal").textContent = occ.descricaoAnimal || "—";
    card.querySelector(".c-local").textContent = occ.localizacao?.endereco || "—";
    card.querySelector(".c-comunicante").textContent =
      `${occ.comunicante?.nome || "—"} (${occ.comunicante?.email || "—"})`;

    const actions = card.querySelector(".occ-card-actions");
    const msgEl = card.querySelector(".c-msg");

    card.querySelector(".btn-detalhe").addEventListener("click", () => abrirDetalhePorId(occ.id));

    if (podeAssumir) {
      const btnAssumir = document.createElement("button");
      btnAssumir.type = "button";
      btnAssumir.textContent = "Assumir Atendimento (CDU07)";
      btnAssumir.addEventListener("click", async () => {
        const analistaId = Number(analistaIdInput.value) || 99;
        btnAssumir.disabled = true;
        setFeedback(msgEl, "Vinculando analista...");
        try {
          await occApi(`/${occ.id}/vincular-analista`, {
            method: "POST",
            body: JSON.stringify({ analistaId })
          });
          await carregarPainelAnalista();
        } catch (e) {
          setFeedback(msgEl, e.message, true);
          btnAssumir.disabled = false;
        }
      });
      actions.appendChild(btnAssumir);
    }

    if (opcoes.length > 0) {
      const select = document.createElement("select");
      opcoes.forEach(st => {
        const opt = document.createElement("option");
        opt.value = st;
        opt.textContent = "Mudar para: " + st;
        select.appendChild(opt);
      });

      const obsInput = document.createElement("input");
      obsInput.type = "text";
      obsInput.placeholder = "Observação do atendimento...";

      const btnStatus = document.createElement("button");
      btnStatus.type = "button";
      btnStatus.className = "secondary";
      btnStatus.textContent = "Atualizar Status";
      btnStatus.addEventListener("click", async () => {
        const analistaId = Number(analistaIdInput.value) || occ.analistaResponsavelId || 99;
        btnStatus.disabled = true;
        setFeedback(msgEl, "Atualizando status...");
        try {
          await occApi(`/${occ.id}/status`, {
            method: "PATCH",
            body: JSON.stringify({
              novoStatus: select.value,
              analistaId,
              observacao: obsInput.value.trim() || null
            })
          });
          await carregarPainelAnalista();
        } catch (e) {
          setFeedback(msgEl, e.message, true);
          btnStatus.disabled = false;
        }
      });

      actions.appendChild(select);
      actions.appendChild(obsInput);
      actions.appendChild(btnStatus);
    }

    analistaLista.appendChild(card);
  });
}

filtroStatusSelect.addEventListener("change", carregarPainelAnalista);
document.getElementById("btn-atualizar-painel").addEventListener("click", carregarPainelAnalista);
