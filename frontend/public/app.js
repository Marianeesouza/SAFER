const API = {
  authLogin: "/api/auth/login",
  authMe: "/api/auth/me",
  usuarios: "/api/usuarios",
  meuPerfil: "/api/usuarios/me",
  ocorrencias: "/api/ocorrencias",
};

const state = {
  token: localStorage.getItem("safer_token") || null,
  usuario: null,
  mostrarAuth: false,
  modoVisitanteRegistrar: false,
};

const el = (id) => document.getElementById(id);

function showToast(message, type = "success") {
  const toast = el("toast");
  toast.textContent = message;
  toast.className = `toast ${type}`;
  toast.classList.remove("hidden");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => toast.classList.add("hidden"), 3500);
}

async function apiFetch(url, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(url, { ...options, headers });
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    const msg =
      (data && (data.detail || data.message || data.title)) ||
      `Erro ${response.status}`;
    throw new Error(msg);
  }
  return data;
}

function setSession(token, usuario) {
  state.token = token;
  state.usuario = usuario;
  if (token) {
    localStorage.setItem("safer_token", token);
  } else {
    localStorage.removeItem("safer_token");
  }
  renderAppState();
}

function renderAppState() {
  const isLogged = Boolean(state.token && state.usuario);
  const showAuth = !isLogged && state.mostrarAuth;
  const showPublicHome = !isLogged && !state.mostrarAuth && !state.modoVisitanteRegistrar;
  const showShell = isLogged || (!showAuth && state.modoVisitanteRegistrar);

  el("public-portal").classList.toggle("hidden", !showPublicHome);
  el("view-auth").classList.toggle("hidden", !showAuth);
  el("app-shell").classList.toggle("hidden", !showShell);

  if (showAuth || showPublicHome) {
    if (showPublicHome) initHomeMap();
    return;
  }

  el("visitante-campos").classList.toggle("hidden", isLogged);

  if (!isLogged) {
    document.querySelectorAll(".nav-btn").forEach((btn) => {
      const view = btn.dataset.view;
      btn.classList.toggle("hidden", view !== "registrar" && view !== "protocolo");
    });
    el("btn-logout").textContent = "← Voltar para Home";
    switchView("registrar");
    return;
  }

  const cargo = state.usuario.cargo;
  const isStaff = cargo === "ANALISTA" || cargo === "ADMINISTRADOR";
  document.querySelectorAll(".nav-btn").forEach((btn) => {
    const view = btn.dataset.view;
    if (view === "usuarios") {
      btn.classList.toggle("hidden", cargo !== "ADMINISTRADOR");
    } else if (view === "ocorrencias") {
      btn.classList.toggle("hidden", !isStaff);
    } else {
      btn.classList.remove("hidden");
    }
  });

  el("btn-logout").textContent = "Sair";

  preencherPerfilForm();
  switchView(isStaff ? "ocorrencias" : "minhas");
}

function switchView(viewName) {
  const secId = viewName === "minhas" ? "view-ocorrencias" : `view-${viewName}`;
  document.querySelectorAll(".view-section").forEach((sec) => {
    sec.classList.toggle("hidden", sec.id !== secId);
  });
  document.querySelectorAll(".nav-btn").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === viewName);
  });

  if (viewName === "ocorrencias") carregarOcorrencias(false);
  if (viewName === "minhas") carregarOcorrencias(true);
  if (viewName === "usuarios") carregarUsuarios();
  if (viewName === "registrar") initMap();
}

let mapInstance = null;
let mapMarker = null;
let geoIndisponivel = false;

function initMap() {
  if (typeof L === "undefined") return;
  const lat = parseFloat(el("oc-lat").value) || -8.0175;
  const lng = parseFloat(el("oc-lng").value) || -34.9492;

  if (!mapInstance) {
    mapInstance = L.map("map").setView([lat, lng], 16);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
    }).addTo(mapInstance);

    mapMarker = L.marker([lat, lng], { draggable: true }).addTo(mapInstance);

    const updateCoords = async (pos) => {
      el("oc-lat").value = pos.lat.toFixed(6);
      el("oc-lng").value = pos.lng.toFixed(6);
      await validarCampusEAtualizarEndereco(pos.lat, pos.lng);
    };

    mapInstance.on("click", (e) => {
      mapMarker.setLatLng(e.latlng);
      updateCoords(e.latlng);
    });

    mapMarker.on("dragend", () => updateCoords(mapMarker.getLatLng()));
    validarCampusEAtualizarEndereco(lat, lng);
  } else {
    setTimeout(() => mapInstance.invalidateSize(), 100);
  }
}

async function validarCampusEAtualizarEndereco(lat, lng) {
  const badge = el("campus-badge");
  const statusEl = el("geo-status");
  if (!badge || !statusEl) return;

  statusEl.textContent = "Validando campus e buscando endereço...";
  try {
    const [campusRes, addrRes] = await Promise.all([
      apiFetch(`/api/geocoding/campus/validate?latitude=${lat}&longitude=${lng}`),
      apiFetch(`/api/geocoding/address?latitude=${lat}&longitude=${lng}`),
    ]);

    badge.classList.remove("hidden", "status-inside", "status-outside");
    if (campusRes.insideCampus) {
      badge.textContent = "Dentro do campus (UFRPE)";
      badge.classList.add("status-inside");
    } else {
      badge.textContent = "Fora do campus";
      badge.classList.add("status-outside");
    }

    if (addrRes && addrRes.address) {
      el("oc-logradouro").value = addrRes.address;
      el("oc-logradouro").readOnly = true;
    }
    geoIndisponivel = false;
    statusEl.textContent = "Localização atualizada.";
  } catch (err) {
    geoIndisponivel = true;
    el("oc-logradouro").readOnly = false;
    el("oc-logradouro").placeholder = "Preencha o endereço manualmente";
    statusEl.textContent = "Geocodificação indisponível — preencha o endereço manualmente.";
  }
}

// Auth tabs
document.querySelectorAll("[data-auth-tab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    const tab = btn.dataset.authTab;
    document.querySelectorAll("[data-auth-tab]").forEach((b) => {
      b.classList.toggle("active", b.dataset.authTab === tab);
    });
    el("form-login").classList.toggle("hidden", tab !== "login");
    el("form-cadastro").classList.toggle("hidden", tab !== "cadastro");
  });
});

// Navigation
document.querySelectorAll(".nav-btn").forEach((btn) => {
  btn.addEventListener("click", () => switchView(btn.dataset.view));
});

el("btn-logout").addEventListener("click", () => {
  if (state.token && state.usuario) {
    state.mostrarAuth = false;
    state.modoVisitanteRegistrar = false;
    setSession(null, null);
    showToast("Sessão encerrada.");
  } else {
    state.modoVisitanteRegistrar = false;
    renderAppState();
  }
});

el("btn-public-entrar").addEventListener("click", () => {
  state.mostrarAuth = true;
  renderAppState();
  document.querySelector('[data-auth-tab="login"]').click();
});

el("btn-public-cadastrar").addEventListener("click", () => {
  state.mostrarAuth = true;
  renderAppState();
  document.querySelector('[data-auth-tab="cadastro"]').click();
});

let homeMapInstance = null;
let homeMapMarker = null;
let homeGeoIndisponivel = false;

function initHomeMap() {
  if (typeof L === "undefined") return;
  const lat = parseFloat(el("home-oc-lat").value) || -8.0175;
  const lng = parseFloat(el("home-oc-lng").value) || -34.9492;

  if (!homeMapInstance) {
    homeMapInstance = L.map("home-map").setView([lat, lng], 16);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
    }).addTo(homeMapInstance);

    homeMapMarker = L.marker([lat, lng], { draggable: true }).addTo(homeMapInstance);

    const updateHomeCoords = async (pos) => {
      el("home-oc-lat").value = pos.lat.toFixed(6);
      el("home-oc-lng").value = pos.lng.toFixed(6);
      await validarHomeCampusEEndereco(pos.lat, pos.lng);
    };

    homeMapInstance.on("click", (e) => {
      homeMapMarker.setLatLng(e.latlng);
      updateHomeCoords(e.latlng);
    });

    homeMapMarker.on("dragend", () => updateHomeCoords(homeMapMarker.getLatLng()));
    validarHomeCampusEEndereco(lat, lng);
  } else {
    setTimeout(() => homeMapInstance.invalidateSize(), 100);
  }
}

async function validarHomeCampusEEndereco(lat, lng) {
  const badge = el("home-campus-badge");
  const statusEl = el("home-geo-status");
  if (!badge || !statusEl) return;

  statusEl.textContent = "Validando campus e buscando endereço...";
  try {
    const [campusRes, addrRes] = await Promise.all([
      apiFetch(`/api/geocoding/campus/validate?latitude=${lat}&longitude=${lng}`),
      apiFetch(`/api/geocoding/address?latitude=${lat}&longitude=${lng}`),
    ]);

    badge.classList.remove("hidden", "status-inside", "status-outside");
    if (campusRes.insideCampus) {
      badge.textContent = "Dentro do campus (UFRPE)";
      badge.classList.add("status-inside");
    } else {
      badge.textContent = "Fora do campus";
      badge.classList.add("status-outside");
    }

    if (addrRes && addrRes.address) {
      el("home-oc-logradouro").value = addrRes.address;
      el("home-oc-logradouro").readOnly = true;
    }
    homeGeoIndisponivel = false;
    statusEl.textContent = "Localização atualizada.";
  } catch (err) {
    homeGeoIndisponivel = true;
    el("home-oc-logradouro").readOnly = false;
    el("home-oc-logradouro").placeholder = "Preencha o endereço manualmente";
    statusEl.textContent = "Geocodificação indisponível — preencha o endereço manualmente.";
  }
}

function abrirModalProtocoloCriado(protocolo) {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").innerHTML = '<span style="color: var(--success);">Sua ocorrência foi registrada com sucesso!</span>';
  body.innerHTML = `
    <p>Guarde o número de protocolo abaixo para acompanhar o atendimento:</p>
    <div class="input-row">
      <input type="text" id="modal-prot-valor" value="${protocolo}" readonly />
      <button type="button" id="btn-copiar-prot" class="btn btn-primary">Copiar</button>
    </div>
  `;
  modal.classList.remove("hidden");

  el("btn-copiar-prot").onclick = async () => {
    try {
      await navigator.clipboard.writeText(protocolo);
      showToast("Protocolo copiado!");
    } catch {
      el("modal-prot-valor").select();
      document.execCommand("copy");
      showToast("Protocolo copiado!");
    }
  };
}

el("home-form-ocorrencia").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const criada = await apiFetch(API.ocorrencias, {
      method: "POST",
      body: JSON.stringify({
        usuarioId: null,
        descricaoAnimal: el("home-oc-descricao").value,
        comunicante: {
          nome: el("home-oc-vis-nome").value.trim() || "Visitante",
          email: el("home-oc-vis-email").value.trim() || "visitante@safer.ufrpe.br",
        },
        localizacao: {
          latitude: homeGeoIndisponivel ? null : parseFloat(el("home-oc-lat").value),
          longitude: homeGeoIndisponivel ? null : parseFloat(el("home-oc-lng").value),
          endereco: el("home-oc-logradouro").value,
        },
      }),
    });
    abrirModalProtocoloCriado(criada.protocolo || `#${criada.id}`);
    el("home-form-ocorrencia").reset();
  } catch (err) {
    showToast(err.message, "error");
  }
});

el("form-home-protocolo").addEventListener("submit", async (e) => {
  e.preventDefault();
  const codigo = el("home-prot-codigo").value.trim();
  if (!codigo) return;
  try {
    const oc = await apiFetch(`${API.ocorrencias}/protocolo/${encodeURIComponent(codigo)}`);
    await abrirDetalhesOcorrencia(oc.id);
  } catch (err) {
    showToast(err.message, "error");
  }
});

el("btn-voltar-app").addEventListener("click", () => {
  state.mostrarAuth = false;
  renderAppState();
});

// Login
el("form-login").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const data = await apiFetch(API.authLogin, {
      method: "POST",
      body: JSON.stringify({
        email: el("login-email").value,
        senha: el("login-senha").value,
      }),
    });
    setSession(data.tokenSessao, data.usuario);
    showToast("Login realizado com sucesso!");
  } catch (err) {
    showToast(err.message, "error");
  }
});

// Cadastro
el("form-cadastro").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await apiFetch(API.usuarios, {
      method: "POST",
      body: JSON.stringify({
        nome: el("cad-nome").value,
        email: el("cad-email").value,
        relacaoUfrpe: el("cad-relacao").value,
        celular: el("cad-celular").value || null,
        telefoneFixo: el("cad-fixo").value || null,
        senha: el("cad-senha").value,
      }),
    });
    showToast("Conta criada! Faça login para continuar.");
    document.querySelector('[data-auth-tab="login"]').click();
    el("login-email").value = el("cad-email").value;
  } catch (err) {
    showToast(err.message, "error");
  }
});

// Ocorrências
let apenasMinhasAtivo = false;

async function carregarOcorrencias(apenasMinhas = apenasMinhasAtivo) {
  apenasMinhasAtivo = apenasMinhas;
  const grid = el("ocorrencias-grid");
  const titulo = document.querySelector("#view-ocorrencias .section-header h2");
  if (titulo) {
    titulo.textContent = apenasMinhas ? "Minhas Ocorrências" : "Ocorrências";
  }
  grid.innerHTML = "<p class='card-meta'>Carregando ocorrências...</p>";
  try {
    const filtro = el("filtro-status").value;
    let endpoint = API.ocorrencias;
    if (apenasMinhas && state.usuario) {
      endpoint = `${API.ocorrencias}/usuario/${state.usuario.id}`;
    } else if (filtro !== "TODAS") {
      endpoint = `${API.ocorrencias}?status=${encodeURIComponent(filtro)}`;
    }
    const lista = await apiFetch(endpoint);
    const filtradas =
      apenasMinhas && filtro !== "TODAS"
        ? lista.filter((o) => o.status === filtro)
        : lista;

    if (!filtradas.length) {
      grid.innerHTML = "<p class='card-meta'>Nenhuma ocorrência encontrada.</p>";
      return;
    }

    const cargo = state.usuario?.cargo;
    const podeAtender = cargo === "ANALISTA" || cargo === "ADMINISTRADOR";

    grid.innerHTML = "";
    filtradas.forEach((oc) => {
      const card = document.createElement("article");
      card.className = "card";
      const dataFormatada = oc.criadoEm ? new Date(oc.criadoEm).toLocaleString("pt-BR") : "-";
      card.innerHTML = `
        <div class="card-top">
          <span class="card-title">${oc.protocolo || "#" + oc.id}</span>
          <span class="status-badge status-${oc.status}">${oc.status}</span>
        </div>
        <p>${oc.descricaoAnimal || ""}</p>
        <p class="card-meta"><strong>Local:</strong> ${oc.localizacao?.endereco || "-"}</p>
        <p class="card-meta"><strong>Comunicante:</strong> ${oc.comunicante?.nome || "-"}</p>
        <p class="card-meta"><strong>Analista:</strong> ${oc.analistaResponsavelId ?? "Não atribuído"}</p>
        <p class="card-meta"><strong>Registrada em:</strong> ${dataFormatada}</p>
        <div class="card-actions"></div>
      `;

      const actions = card.querySelector(".card-actions");

      const btnDetalhes = document.createElement("button");
      btnDetalhes.className = "btn btn-secondary";
      btnDetalhes.textContent = "Ver Detalhes";
      btnDetalhes.onclick = () => abrirDetalhesOcorrencia(oc.id);
      actions.appendChild(btnDetalhes);

      if (podeAtender && oc.status === "EM_ABERTO") {
        const btnAssumir = document.createElement("button");
        btnAssumir.className = "btn btn-primary";
        btnAssumir.textContent = "Assumir";
        btnAssumir.onclick = () => assumirOcorrencia(oc.id);
        actions.appendChild(btnAssumir);
      }

      const ehResponsavel = oc.analistaResponsavelId === state.usuario?.id || cargo === "ADMINISTRADOR";
      if (podeAtender && ehResponsavel && (oc.status === "EM_ATENDIMENTO" || oc.status === "NECESSITA_ATENDIMENTO_MEDICO")) {
        const btnStatus = document.createElement("button");
        btnStatus.className = "btn btn-secondary";
        btnStatus.textContent = "Atualizar Status";
        btnStatus.onclick = () => abrirModalAtualizarStatus(oc.id, oc.status);
        actions.appendChild(btnStatus);
      }

      const podeCancelar = oc.usuarioId === state.usuario?.id || cargo === "ADMINISTRADOR";
      if (oc.status === "EM_ABERTO" && podeCancelar) {
        const btnCancelar = document.createElement("button");
        btnCancelar.className = "btn btn-danger";
        btnCancelar.textContent = "Cancelar";
        btnCancelar.onclick = () => cancelarOcorrencia(oc.id);
        actions.appendChild(btnCancelar);
      }

      grid.appendChild(card);
    });
  } catch (err) {
    grid.innerHTML = `<p class='card-meta'>Erro ao carregar: ${err.message}</p>`;
  }
}

async function abrirDetalhesOcorrencia(id) {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").textContent = `Ocorrência #${id}`;
  body.innerHTML = "<p class='card-meta'>Carregando detalhes...</p>";
  modal.classList.remove("hidden");

  try {
    const det = await apiFetch(`${API.ocorrencias}/${id}`);
    const lat = det.localizacao?.coordenadas?.latitude ?? det.localizacao?.latitude;
    const lng = det.localizacao?.coordenadas?.longitude ?? det.localizacao?.longitude;
    const coords = lat != null && lng != null ? `${lat}, ${lng}` : "-";
    const historicoHtml =
      det.historico && det.historico.length
        ? `<ul class="historico-list">${det.historico
            .map(
              (h) => `
            <li class="historico-item">
              <strong>${h.statusRegistrado || h.statusNovo || h.statusAtual || det.status}</strong>
              <div class="card-meta">Usuário #${h.usuarioId ?? "-"} · ${h.dataHora || ""} ${h.observacao ? "· " + h.observacao : ""}</div>
            </li>`
            )
            .join("")}</ul>`
        : "<p class='card-meta'>Sem registros de histórico.</p>";

    const temCoords = lat != null && lng != null;
    body.innerHTML = `
      <div><span class="status-badge status-${det.status}">${det.status}</span> <strong>${det.protocolo || ""}</strong></div>
      <p><strong>Descrição:</strong> ${det.descricaoAnimal || "-"}</p>
      <p><strong>Endereço:</strong> ${det.localizacao?.endereco || "-"}</p>
      <p><strong>Coordenadas:</strong> ${temCoords ? coords : "Coordenadas não informadas"}</p>
      ${temCoords ? '<div id="modal-map" class="map-box" style="height: 220px;"></div>' : ""}
      <p><strong>Comunicante:</strong> ${det.comunicante?.nome || "-"} (${det.comunicante?.email || "-"})</p>
      <p><strong>Analista responsável:</strong> ${det.analistaResponsavelId ?? "Não atribuído"}</p>
      <hr class="divider" />
      <strong>Histórico de Status</strong>
      ${historicoHtml}
    `;

    if (temCoords && typeof L !== "undefined") {
      const modalMap = L.map("modal-map").setView([lat, lng], 16);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap",
      }).addTo(modalMap);
      L.marker([lat, lng]).addTo(modalMap);
      setTimeout(() => modalMap.invalidateSize(), 100);
    }
  } catch (err) {
    body.innerHTML = `<p class='card-meta'>Erro ao carregar detalhes: ${err.message}</p>`;
  }
}

el("btn-fechar-modal").addEventListener("click", () => {
  el("modal-detalhes").classList.add("hidden");
});

async function assumirOcorrencia(id) {
  try {
    await apiFetch(`${API.ocorrencias}/${id}/vincular-analista`, {
      method: "POST",
      body: JSON.stringify({ analistaId: state.usuario.id }),
    });
    showToast(`Ocorrência #${id} assumida!`);
    carregarOcorrencias();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function abrirModalAtualizarStatus(id, statusAtual) {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").textContent = `Atualizar Status · Ocorrência #${id}`;
  const opcoes =
    statusAtual === "EM_ATENDIMENTO"
      ? `
          <option value="3">Necessita Atendimento Médico</option>
          <option value="4">Finalizado com Sucesso</option>
          <option value="5">Finalizado com Complicações</option>
        `
      : `
          <option value="4">Finalizado com Sucesso</option>
          <option value="5">Finalizado com Complicações</option>
        `;
  body.innerHTML = `
    <form id="form-modal-status" class="stack-form">
      <p class="card-meta">Status atual: <strong>${statusAtual}</strong></p>
      <label>
        Novo status
        <select id="modal-novo-status" required>
          ${opcoes}
        </select>
      </label>
      <label>
        Observação (opcional)
        <textarea id="modal-status-obs" rows="3" maxlength="1000" placeholder="Descreva a atualização do atendimento..."></textarea>
      </label>
      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Salvar Status</button>
      </div>
    </form>
  `;
  modal.classList.remove("hidden");

  el("form-modal-status").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await apiFetch(`${API.ocorrencias}/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          novoStatus: parseInt(el("modal-novo-status").value, 10),
          analistaId: state.usuario.id,
          observacao: el("modal-status-obs").value.trim() || null,
        }),
      });
      modal.classList.add("hidden");
      showToast(`Status da ocorrência #${id} atualizado.`);
      carregarOcorrencias();
    } catch (err) {
      showToast(err.message, "error");
    }
  });
}

function cancelarOcorrencia(id) {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").textContent = `Cancelar Ocorrência #${id}`;
  body.innerHTML = `
    <form id="form-modal-cancelar" class="stack-form">
      <label>
        Justificativa do cancelamento
        <textarea id="modal-justificativa" rows="3" required maxlength="500" placeholder="Informe o motivo do cancelamento..."></textarea>
      </label>
      <div class="form-actions">
        <button type="submit" class="btn btn-danger">Confirmar Cancelamento</button>
      </div>
    </form>
  `;
  modal.classList.remove("hidden");

  el("form-modal-cancelar").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await apiFetch(`${API.ocorrencias}/${id}/cancelar`, {
        method: "PATCH",
        body: JSON.stringify({
          usuarioId: state.usuario.id,
          justificativa: el("modal-justificativa").value.trim(),
        }),
      });
      modal.classList.add("hidden");
      showToast(`Ocorrência #${id} cancelada.`);
      carregarOcorrencias();
    } catch (err) {
      showToast(err.message, "error");
    }
  });
}

el("filtro-status").addEventListener("change", carregarOcorrencias);

// Registrar Ocorrência
el("form-ocorrencia").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const criada = await apiFetch(API.ocorrencias, {
      method: "POST",
      body: JSON.stringify({
        usuarioId: state.usuario ? state.usuario.id : null,
        descricaoAnimal: el("oc-descricao").value,
        comunicante: state.usuario
          ? {
              nome: state.usuario.nome,
              email: state.usuario.email,
            }
          : {
              nome: el("oc-vis-nome").value.trim() || "Visitante",
              email: el("oc-vis-email").value.trim() || "visitante@safer.ufrpe.br",
            },
        localizacao: {
          latitude: geoIndisponivel ? null : parseFloat(el("oc-lat").value),
          longitude: geoIndisponivel ? null : parseFloat(el("oc-lng").value),
          endereco: el("oc-logradouro").value,
        },
      }),
    });
    abrirModalProtocoloCriado(criada.protocolo || `#${criada.id}`);
    el("form-ocorrencia").reset();
    if (state.usuario) {
      switchView(state.usuario.cargo === "COMUM" ? "minhas" : "ocorrencias");
    }
  } catch (err) {
    showToast(err.message, "error");
  }
});

// Consultar Protocolo
el("form-protocolo").addEventListener("submit", async (e) => {
  e.preventDefault();
  const res = el("protocolo-resultado");
  const codigo = el("prot-codigo").value.trim();
  if (!codigo) return;
  res.innerHTML = "<p class='card-meta'>Consultando protocolo...</p>";
  try {
    const oc = await apiFetch(`${API.ocorrencias}/protocolo/${encodeURIComponent(codigo)}`);
    res.innerHTML = `
      <article class="card">
        <div class="card-top">
          <span class="card-title">${oc.protocolo || "#" + oc.id}</span>
          <span class="status-badge status-${oc.status}">${oc.status}</span>
        </div>
        <p>${oc.descricaoAnimal || ""}</p>
        <p class="card-meta"><strong>Local:</strong> ${oc.localizacao?.endereco || "-"}</p>
        <p class="card-meta"><strong>Analista:</strong> ${oc.analistaResponsavelId ?? "Não atribuído"}</p>
      </article>
    `;
  } catch (err) {
    res.innerHTML = `<p class='card-meta'>${err.message}</p>`;
  }
});

// Meu Perfil
function preencherPerfilForm() {
  if (!state.usuario) return;
  el("perf-nome").value = state.usuario.nome || "";
  el("perf-relacao").value = state.usuario.relacaoUfrpe || "DISCENTE";
  el("perf-celular").value = state.usuario.celular || "";
  el("perf-fixo").value = state.usuario.telefoneFixo || "";
  el("perf-email").value = state.usuario.email || "";
  el("perf-nova-senha").value = "";
  el("perf-senha-atual").value = "";
}

el("form-perfil").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const payload = {
      nome: el("perf-nome").value,
      relacaoUfrpe: el("perf-relacao").value,
      celular: el("perf-celular").value || null,
      telefoneFixo: el("perf-fixo").value || null,
      email: el("perf-email").value !== state.usuario.email ? el("perf-email").value : null,
      novaSenha: el("perf-nova-senha").value || null,
      senhaAtual: el("perf-senha-atual").value || null,
    };
    const alterouEmail = Boolean(payload.email);
    const atualizado = await apiFetch(API.meuPerfil, {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
    if (alterouEmail) {
      setSession(null, null);
      showToast("E-mail alterado! Faça login novamente com o novo e-mail.");
      return;
    }
    state.usuario = atualizado;
    renderAppState();
    switchView("perfil");
    showToast("Perfil atualizado com sucesso!");
  } catch (err) {
    showToast(err.message, "error");
  }
});

// Gestão de Usuários (ADMIN)
let usuariosCache = [];

async function carregarUsuarios() {
  const grid = el("usuarios-grid");
  grid.innerHTML = "<p class='card-meta'>Carregando usuários...</p>";
  try {
    usuariosCache = await apiFetch(API.usuarios);
    renderizarUsuarios();
  } catch (err) {
    grid.innerHTML = `<p class='card-meta'>Erro ao carregar usuários: ${err.message}</p>`;
  }
}

function renderizarUsuarios() {
  const grid = el("usuarios-grid");
  const busca = (el("filtro-usuario-busca")?.value || "").trim().toLowerCase();
  const cargoFiltro = el("filtro-usuario-cargo")?.value || "TODOS";

  const filtrados = usuariosCache.filter((u) => {
    const matchBusca =
      !busca ||
      u.nome.toLowerCase().includes(busca) ||
      u.email.toLowerCase().includes(busca);
    const matchCargo = cargoFiltro === "TODOS" || u.cargo === cargoFiltro;
    return matchBusca && matchCargo;
  });

  if (!filtrados.length) {
    grid.innerHTML = "<p class='card-meta'>Nenhum usuário encontrado.</p>";
    return;
  }

  grid.innerHTML = "";
  filtrados.forEach((u) => {
    const card = document.createElement("article");
    card.className = "card";
    const opcoesCargo =
      u.cargo === "COMUM"
        ? `<option value="COMUM" selected>COMUM</option><option value="ANALISTA">ANALISTA</option>`
        : u.cargo === "ANALISTA"
          ? `<option value="ANALISTA" selected>ANALISTA</option><option value="COMUM">COMUM</option><option value="ADMINISTRADOR">ADMINISTRADOR</option>`
          : `<option value="ADMINISTRADOR" selected>ADMINISTRADOR</option>`;

    card.innerHTML = `
      <div class="card-top">
        <span class="card-title">${u.nome}</span>
        <span class="status-badge">${u.cargo}</span>
      </div>
      <p class="card-meta">${u.email}</p>
      <p class="card-meta">Vínculo: ${u.relacaoUfrpe}</p>
      <div class="card-actions">
        <select aria-label="Novo cargo para ${u.nome}" ${u.cargo === "ADMINISTRADOR" ? "disabled" : ""}>
          ${opcoesCargo}
        </select>
        ${u.cargo !== "ADMINISTRADOR" ? '<button type="button" class="btn btn-primary btn-salvar-cargo">Salvar Cargo</button>' : ""}
        ${u.id !== state.usuario.id ? '<button type="button" class="btn btn-danger btn-excluir-usuario">Excluir</button>' : ""}
      </div>
    `;
    const select = card.querySelector("select");
    const btnSalvar = card.querySelector(".btn-salvar-cargo");
    if (btnSalvar) {
      btnSalvar.onclick = async () => {
        try {
          await apiFetch(`${API.usuarios}/${u.id}/cargo`, {
            method: "PATCH",
            body: JSON.stringify({ novoCargo: select.value }),
          });
          showToast(`Cargo de ${u.nome} atualizado para ${select.value}.`);
          carregarUsuarios();
        } catch (err) {
          showToast(err.message, "error");
        }
      };
    }

    const btnExcluir = card.querySelector(".btn-excluir-usuario");
    if (btnExcluir) {
      btnExcluir.onclick = () => abrirModalExcluirUsuario(u);
    }
    grid.appendChild(card);
  });
}

el("filtro-usuario-busca").addEventListener("input", renderizarUsuarios);
el("filtro-usuario-cargo").addEventListener("change", renderizarUsuarios);

el("btn-novo-usuario").addEventListener("click", abrirModalNovoUsuario);

function abrirModalNovoUsuario() {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").textContent = "Novo Usuário";
  body.innerHTML = `
    <form id="form-modal-novo-usuario" class="stack-form">
      <label>
        Nome completo
        <input type="text" id="novo-u-nome" required maxlength="150" placeholder="Maria Silva" />
      </label>
      <label>
        E-mail
        <input type="email" id="novo-u-email" required maxlength="180" placeholder="maria@ufrpe.br" />
      </label>
      <label>
        Vínculo com a UFRPE
        <select id="novo-u-relacao" required>
          <option value="DISCENTE">Discente</option>
          <option value="DOCENTE">Docente</option>
          <option value="TECNICO">Técnico</option>
          <option value="TERCEIRIZADO">Terceirizado</option>
          <option value="VISITANTE">Visitante</option>
        </select>
      </label>
      <div class="grid-2">
        <label>
          Celular (opcional)
          <input type="text" id="novo-u-celular" maxlength="20" />
        </label>
        <label>
          Telefone fixo (opcional)
          <input type="text" id="novo-u-fixo" maxlength="20" />
        </label>
      </div>
      <label>
        Senha (mín. 10 caracteres, maiúscula, minúscula, número e especial)
        <input type="password" id="novo-u-senha" required placeholder="SenhaForte@123" />
      </label>
      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Criar Usuário</button>
      </div>
    </form>
  `;
  modal.classList.remove("hidden");

  el("form-modal-novo-usuario").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await apiFetch(API.usuarios, {
        method: "POST",
        body: JSON.stringify({
          nome: el("novo-u-nome").value,
          email: el("novo-u-email").value,
          relacaoUfrpe: el("novo-u-relacao").value,
          celular: el("novo-u-celular").value || null,
          telefoneFixo: el("novo-u-fixo").value || null,
          senha: el("novo-u-senha").value,
        }),
      });
      modal.classList.add("hidden");
      showToast("Usuário criado com sucesso!");
      carregarUsuarios();
    } catch (err) {
      showToast(err.message, "error");
    }
  });
}

function abrirModalExcluirUsuario(usuario) {
  const modal = el("modal-detalhes");
  const body = el("modal-conteudo");
  el("modal-titulo").textContent = "Excluir Usuário";
  body.innerHTML = `
    <p>Tem certeza que deseja excluir o usuário <strong>${usuario.nome}</strong> (${usuario.email})?</p>
    <div class="form-actions">
      <button type="button" id="btn-confirmar-excluir-u" class="btn btn-danger">Confirmar Exclusão</button>
    </div>
  `;
  modal.classList.remove("hidden");

  el("btn-confirmar-excluir-u").onclick = async () => {
    try {
      await apiFetch(`${API.usuarios}/${usuario.id}`, { method: "DELETE" });
      modal.classList.add("hidden");
      showToast(`Usuário ${usuario.nome} excluído.`);
      carregarUsuarios();
    } catch (err) {
      showToast(err.message, "error");
    }
  };
}

// Bootstrap session
(async function init() {
  if (!state.token) {
    renderAppState();
    return;
  }
  try {
    const usuario = await apiFetch(API.authMe);
    setSession(state.token, usuario);
  } catch {
    setSession(null, null);
  }
})();
