/* =====================================================================
   Foodi · front. Todo el estado viene de la API REST de Spring Boot (/api/...).
   ===================================================================== */

// ---------- utilidades ----------
const fmt = n => '$ ' + Number(n || 0).toLocaleString('es-CO', {maximumFractionDigits: 0});
const esc = t => String(t ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const $ = id => document.getElementById(id);

async function api(path, {method = 'GET', body} = {}) {
  const res = await fetch('/api' + path, {
    method,
    headers: {'Content-Type': 'application/json'},
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (res.status === 401 && !path.startsWith('/auth/')) { mostrarLogin(); throw new Error('Tu sesión expiró, ingresa de nuevo'); }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = data ? (data.error || Object.values(data)[0]) : 'Error del servidor';
    throw new Error(msg);
  }
  return data;
}

function toast(msg, error = false) {
  const el = document.createElement('div');
  el.className = 'toast' + (error ? ' error' : '');
  el.textContent = msg;
  $('toasts').appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

/** Ejecuta una acción y muestra el error como toast si falla. */
async function accion(fn, okMsg) {
  try { const r = await fn(); if (okMsg) toast(okMsg); return r; }
  catch (e) { toast(e.message, true); }
}

function abrirModal(titulo, camposHtml, onOk, textoOk = 'Guardar') {
  const bg = document.createElement('div');
  bg.className = 'modal-bg';
  bg.innerHTML = `<div class="modal"><h3>${esc(titulo)}</h3>${camposHtml}
    <div class="modal-actions"><button class="btn-secondary" id="mCancel">Cancelar</button>
    <button class="btn-primary" id="mOk">${esc(textoOk)}</button></div></div>`;
  document.body.appendChild(bg);
  const cerrar = () => bg.remove();
  bg.querySelector('#mCancel').onclick = cerrar;
  bg.addEventListener('mousedown', e => { if (e.target === bg) cerrar(); });
  bg.querySelector('#mOk').onclick = async () => {
    try { await onOk(bg); cerrar(); } catch (e) { toast(e.message, true); }
  };
  const primero = bg.querySelector('input,select');
  if (primero) primero.focus();
}
const campo = (id, label, valor = '', tipo = 'text') =>
  `<div class="form-field"><label class="form-label">${label}</label><input class="form-input" id="${id}" type="${tipo}" value="${esc(valor)}"></div>`;

// ---------- estado ----------
const S = {mesas: [], ingredientes: [], clientes: [], facturas: [], resumen: null, dash: null,
           selMesa: null, selCliente: null, selIng: null, movTab: 'ENTRADA', usuario: null, historial: null, movimientos: []};

const CARTA = [
  ['Bandeja paisa', 32000], ['Sancocho trifásico', 30000], ['Ajiaco santafereño', 26000],
  ['Trucha al ajillo', 34000], ['Arepa con queso', 6000], ['Limonada de coco', 9000],
  ['Limonada natural', 8000], ['Jugo de mora', 8000], ['Cerveza Club Colombia', 7000], ['Tres leches', 11000]
];
const ESTADO_MESA = {LIBRE: ['free', 'Libre'], OCUPADA: ['occupied', 'Ocupada'], ESPERANDO_PAGO: ['waiting', 'Esperando pago']};
const ESTADO_FAC = {ACEPTADA_DIAN: ['ok', 'Aceptada DIAN'], PENDIENTE: ['warn', 'Pendiente DIAN'], RECHAZADA: ['bad', 'Rechazada']};
const ESTADO_PEDIDO = {ABIERTO: 'Abierto', EN_COCINA: 'En cocina', SERVIDO: 'Servido', PAGADO: 'Pagado'};

// ---------- login / sesión ----------
function mostrarLogin() {
  $('appRoot').style.display = 'none';
  $('loginScreen').style.display = 'flex';
}

function entrarAlPanel(usuario) {
  S.usuario = usuario;
  const iniciales = usuario.nombre.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  document.querySelector('.user-name').textContent = usuario.nombre;
  document.querySelector('.sidebar-footer .user-avatar').textContent = iniciales;
  $('userRole').textContent = usuario.rol.charAt(0) + usuario.rol.slice(1).toLowerCase();
  $('loginScreen').style.display = 'none';
  $('appRoot').style.display = 'block';
  const f = new Date().toLocaleDateString('es-CO', {weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'});
  titles.dashboard[1] = f.charAt(0).toUpperCase() + f.slice(1) + ' · Servicio en curso';
  goTo('dashboard');
  cargarTodo();
}

async function doLogin() {
  const correo = $('loginEmail').value.trim();
  const contrasena = $('loginPass').value.trim();
  const err = $('loginError');
  try {
    const res = await fetch('/api/auth/login', {method: 'POST', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({correo, contrasena})});
    if (!res.ok) {
      err.textContent = res.status === 401 || res.status === 400 ? 'Correo o contraseña incorrectos.' : 'Error del servidor. Intenta de nuevo.';
      err.classList.add('show');
      return;
    }
    err.classList.remove('show');
    entrarAlPanel(await res.json());
  } catch (e) {
    err.textContent = 'No se pudo conectar con el servidor.';
    err.classList.add('show');
  }
}

async function doLogout() {
  await fetch('/api/auth/logout', {method: 'POST'});
  mostrarLogin();
}
$('loginPass').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });

// ---------- navegación ----------
const titles = {
  dashboard: ['Dashboard', '', '⚡ Cerrar caja del día'],
  mesas: ['Cajero · Cobro de mesas', 'Toma pedidos, envía a cocina y cobra', 'Cierre parcial Z'],
  inventario: ['Inventario', 'Descuento y reposición de ingredientes', '+ Nuevo ingrediente'],
  clientes: ['Clientes', 'Historial y total gastado por cliente', '+ Nuevo cliente'],
  facturacion: ['Facturación', 'Documentos emitidos · validación DIAN', 'Reporte DIAN']
};
let vistaActual = 'dashboard';

function goTo(view) {
  vistaActual = view;
  document.querySelectorAll('.content').forEach(c => c.classList.remove('active'));
  $('view-' + view).classList.add('active');
  document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.view === view));
  const t = titles[view];
  $('pageTitle').textContent = t[0];
  $('pageSub').textContent = t[1];
  $('topAction').textContent = t[2];
}
document.querySelectorAll('.nav-item[data-view]').forEach(b => b.addEventListener('click', () => goTo(b.dataset.view)));

$('topAction').addEventListener('click', () => {
  if (vistaActual === 'clientes') return nuevoCliente();
  if (vistaActual === 'inventario') return nuevoIngrediente();
  if (vistaActual === 'facturacion') return reporteDian();
  return cierreDeCaja();
});

// ---------- carga de datos ----------
async function cargarTodo() {
  await accion(async () => {
    const [dash, mesas, ing, cli, fac, res] = await Promise.all([
      api('/dashboard'), api('/mesas'), api('/inventario/ingredientes'),
      api('/clientes'), api('/facturas'), api('/facturas/resumen')]);
    Object.assign(S, {dash, mesas, ingredientes: ing, clientes: cli, facturas: fac, resumen: res});
    if (S.selMesa === null && mesas.length) S.selMesa = mesas.find(m => m.estado !== 'LIBRE')?.id ?? mesas[0].id;
    if (S.selCliente === null && cli.length) S.selCliente = cli[0].id;
    renderTodo();
  });
}
async function refrescarMesasYDashboard() {
  const [dash, mesas, fac, res] = await Promise.all([api('/dashboard'), api('/mesas'), api('/facturas'), api('/facturas/resumen')]);
  Object.assign(S, {dash, mesas, facturas: fac, resumen: res});
  renderDashboard(); renderMesas(); renderFacturas();
}
async function cargarFacturas() {
  const q = $('facSearch').value.trim();
  S.facturas = await api('/facturas' + (q ? '?q=' + encodeURIComponent(q) : ''));
  renderFacturas();
}
async function cargarClientes() {
  const q = $('clienteSearch').value.trim();
  S.clientes = await api('/clientes' + (q ? '?q=' + encodeURIComponent(q) : ''));
  renderClientes();
}
function renderTodo() { renderDashboard(); renderMesas(); renderInventario(); renderClientes(); renderFacturas(); }

// ---------- DASHBOARD ----------
function renderDashboard() {
  const d = S.dash; if (!d) return;
  $('kpiVentas').textContent = fmt(d.ventasDelDia);
  $('kpiPedidos').textContent = d.totalPedidos;
  $('kpiTicket').textContent = fmt(d.ticketPromedio);
  $('kpiMesas').textContent = `${d.mesasOcupadas} / ${d.totalMesas}`;
  $('kpiMesasPct').textContent = d.totalMesas ? Math.round(d.mesasOcupadas * 100 / d.totalMesas) + '%' : '—';

  const sem = d.ventasSemana || [];
  const ayer = sem.length >= 2 ? Number(sem[sem.length - 2].ventas) : 0;
  const hoy = Number(d.ventasDelDia);
  $('kpiVentasDelta').textContent = ayer > 0 ? ((hoy - ayer) / ayer * 100 >= 0 ? '+' : '') + Math.round((hoy - ayer) / ayer * 100) + '%' : '—';
  $('kpiPedidosDelta').textContent = d.totalPedidos;

  const max = Math.max(1, ...sem.map(v => Math.max(Number(v.ventas), Number(v.prediccion || 0))));
  $('chartBars').innerHTML = sem.map(v => {
    const h = Math.round(Number(v.ventas) / max * 100);
    const p = v.prediccion ? Math.round(Number(v.prediccion) / max * 100) : null;
    return `<div class="bar-col" title="${esc(v.dia)}: ${fmt(v.ventas)}${p !== null ? ' · IA ' + fmt(v.prediccion) : ''}">
      <div style="position:relative;width:100%;height:100%;display:flex;align-items:flex-end;justify-content:center">
        <div class="bar" style="height:${h}%"></div>
        ${p !== null ? `<div style="position:absolute;bottom:${p}%;width:70%;height:2px;background:#333"></div>` : ''}
      </div><div class="bar-day">${esc(v.dia)}</div></div>`;
  }).join('') || '<div class="empty-hint">Sin ventas registradas</div>';

  renderPrediccion(d.prediccion);

  $('dashInvoices').innerHTML = S.facturas.slice(0, 5).map(f => {
    const [cls, label] = ESTADO_FAC[f.estado];
    return `<div class="row-item" onclick="goTo('facturacion')">
      <div><div class="row-main">${esc(f.numero)}</div><div class="row-meta">${esc(f.cliente)} · ${hora(f.fecha)}</div></div>
      <div><span class="badge ${cls}">${label}</span><div class="row-amount">${fmt(f.total)}</div></div></div>`;
  }).join('') || '<div class="empty-hint">Aún no hay facturas</div>';

  $('dashMesas').innerHTML = S.mesas.slice(0, 8).map(m => {
    const [cls, label] = ESTADO_MESA[m.estado];
    return `<div class="table-cell ${cls}" onclick="selectMesa(${m.id});goTo('mesas')">MESA ${m.numero}<div class="mesa-num">${label}</div>${m.estado !== 'LIBRE' ? `<div class="mesa-status">${esc(m.tiempoOcupacion)}</div>` : ''}</div>`;
  }).join('');
}
const hora = iso => new Date(iso).toLocaleTimeString('es-CO', {hour: '2-digit', minute: '2-digit', hour12: false});

function renderPrediccion(p) {
  if (!p) {
    $('iaValor').textContent = '—';
    $('iaConf').textContent = 'Sin predicción todavía';
    $('iaAlertas').innerHTML = '';
    return;
  }
  const v = Number(p.ventasPredichas);
  $('iaValor').textContent = v >= 1e6 ? '$ ' + (v / 1e6).toFixed(1) + 'M' : fmt(v);
  $('iaConf').textContent = `Confianza ${Math.round(p.nivelConfianza * 100)}% · pico entre ${(p.horaPico || '—').replace(' - ', ' y ')}`;
  $('iaAlertas').innerHTML = (p.recomendacion || '').split(' | ').filter(Boolean).map(r => {
    const [titulo, ...resto] = r.split(':');
    return `<div class="ia-alert"><div class="ia-alert-title">${resto.length ? esc(titulo) : '⚠ ' + esc(r)}</div>${resto.length ? `<div class="ia-alert-desc">${esc(resto.join(':').trim())}</div>` : ''}</div>`;
  }).join('');
}
async function regenerarPrediccion() {
  const p = await accion(() => api('/prediccion/generar', {method: 'POST'}), 'Predicción recalculada');
  if (p) { S.dash.prediccion = p; renderPrediccion(p); }
}

function cierreDeCaja() {
  const d = S.dash; if (!d) return;
  abrirModal('Cierre de caja del día', `
    <div class="total-line"><span>Ventas del día</span><span>${fmt(d.ventasDelDia)}</span></div>
    <div class="total-line"><span>Pedidos</span><span>${d.totalPedidos}</span></div>
    <div class="total-line"><span>Ticket promedio</span><span>${fmt(d.ticketPromedio)}</span></div>
    <div class="total-line"><span>Mesas con cuenta abierta</span><span>${d.mesasOcupadas}</span></div>
    <div class="panel-sub" style="margin-top:10px">${d.mesasOcupadas > 0 ? 'Aún hay mesas sin cobrar.' : 'Todas las mesas están cobradas.'}</div>`,
    () => toast('Cierre registrado'), 'Confirmar cierre');
}

// ---------- MESAS / CAJERO ----------
function renderMesas() {
  const espera = S.mesas.filter(m => m.estado === 'ESPERANDO_PAGO').length;
  $('mesasSub').textContent = `Salón principal · ${S.mesas.length} mesas · ${espera} esperando pago`;
  $('mesasGrid').innerHTML = S.mesas.map(m => {
    const [cls, label] = ESTADO_MESA[m.estado];
    return `<div class="table-cell ${cls} ${m.id === S.selMesa ? 'sel' : ''}" onclick="selectMesa(${m.id})">MESA ${m.numero}<div class="mesa-num">${label}</div>${m.estado !== 'LIBRE' ? `<div class="mesa-status">${esc(m.tiempoOcupacion)}</div>` : ''}</div>`;
  }).join('');
  renderMesaDetail();
}
function selectMesa(id) { S.selMesa = id; renderMesas(); }

function renderMesaDetail() {
  const m = S.mesas.find(x => x.id === S.selMesa);
  const el = $('mesaDetail');
  if (!m) { el.innerHTML = '<div class="empty-hint">Selecciona una mesa.</div>'; return; }
  const p = m.pedidoActivo;
  if (!p) {
    el.innerHTML = `<div class="panel-title">Mesa ${m.numero}</div>
      <div class="empty-hint">${m.estado === 'LIBRE' ? 'Esta mesa está libre.' : 'Mesa ocupada sin cuenta abierta.'}</div>
      <button class="btn-primary" style="width:100%" onclick="abrirPedido(${m.id})">＋ Abrir pedido</button>
      ${m.estado !== 'LIBRE' ? `<button class="btn-secondary" style="width:100%;margin-top:8px" onclick="liberarMesa(${m.id})">Liberar mesa</button>` : ''}`;
    return;
  }
  const [cls, label] = ESTADO_MESA[m.estado];
  el.innerHTML = `
    <div class="panel-head"><div><div class="panel-title">Mesa ${m.numero}</div>
      <div class="panel-sub">Pedido ${esc(p.numeroPedido)} · ${esc(p.mesero || 'sin mesero')} · ${esc(m.tiempoOcupacion)} en mesa</div></div>
      <span class="badge ${cls === 'waiting' ? 'bad' : 'pending'}">${label}</span></div>
    <div class="form-field"><label class="form-label">Estado del pedido</label>
      <select class="form-input" onchange="cambiarEstadoPedido(${p.id}, this.value)">
        ${['ABIERTO', 'EN_COCINA', 'SERVIDO'].map(e => `<option value="${e}" ${e === p.estado ? 'selected' : ''}>${ESTADO_PEDIDO[e]}</option>`).join('')}
      </select></div>
    ${p.detalles.map(d => `<div class="order-line">
      <span><span class="qty-ctl"><button class="btn-sm" onclick="cambiarCantidad(${p.id},${d.id},${d.cantidad - 1})">−</button>
        <b>${d.cantidad}</b><button class="btn-sm" onclick="cambiarCantidad(${p.id},${d.id},${d.cantidad + 1})">+</button></span>${esc(d.descripcion)}</span>
      <span>${fmt(d.subtotal)} <button class="btn-sm danger" title="Quitar" onclick="quitarDetalle(${p.id},${d.id})">×</button></span></div>`).join('')
      || '<div class="empty-hint">Aún no hay ítems en la cuenta.</div>'}
    <div class="add-line">
      <select class="form-input" id="addItem">${CARTA.map(([n, pr], i) => `<option value="${i}">${esc(n)} · ${fmt(pr)}</option>`).join('')}</select>
      <input class="form-input" id="addQty" type="number" min="1" value="1">
      <button class="btn-secondary" onclick="agregarItem(${p.id})">Agregar</button></div>
    <div class="total-line"><span>Subtotal</span><span>${fmt(p.subtotal)}</span></div>
    <div class="total-line"><span>IVA (8%)</span><span>${fmt(p.iva)}</span></div>
    <div class="total-line"><span>Propina (10%)</span><span>${fmt(p.propina)}</span></div>
    <div class="total-line grand"><span>Total a pagar</span><span>${fmt(p.total)}</span></div>
    <div class="form-field" style="margin-top:12px"><label class="form-label">Factura a nombre de</label>
      <select class="form-input" id="cobroCliente"><option value="">Consumidor final</option>
        ${S.clientes.map(c => `<option value="${c.id}" ${p.clienteId === c.id ? 'selected' : ''}>${esc(c.nombre)}</option>`).join('')}</select></div>
    ${m.estado !== 'ESPERANDO_PAGO' ? `<button class="btn-secondary" style="width:100%;margin-bottom:8px" onclick="solicitarPago(${m.id})">🧾 Solicitar pago</button>` : ''}
    <button class="btn-primary" style="width:100%" ${p.detalles.length ? '' : 'disabled'} onclick="cobrarMesa(${m.id})">✓ Cobrar ${fmt(p.total)} y cerrar mesa</button>`;
}

const mesero = () => S.usuario ? S.usuario.nombre : null;
async function abrirPedido(mesaId) {
  await accion(async () => { await api('/pedidos', {method: 'POST', body: {mesaId, mesero: mesero()}}); await refrescarMesasYDashboard(); }, 'Pedido abierto');
}
async function liberarMesa(id) {
  await accion(async () => { await api(`/mesas/${id}/liberar`, {method: 'POST'}); await refrescarMesasYDashboard(); }, 'Mesa liberada');
}
async function solicitarPago(id) {
  await accion(async () => { await api(`/mesas/${id}/solicitar-pago`, {method: 'POST'}); await refrescarMesasYDashboard(); }, 'Mesa esperando pago');
}
async function agregarItem(pedidoId) {
  const [descripcion, precioUnitario] = CARTA[Number($('addItem').value)];
  const cantidad = Math.max(1, Number($('addQty').value) || 1);
  await accion(async () => { await api(`/pedidos/${pedidoId}/detalles`, {method: 'POST', body: {descripcion, cantidad, precioUnitario}}); await refrescarMesasYDashboard(); });
}
async function cambiarCantidad(pedidoId, detalleId, cantidad) {
  if (cantidad < 1) return quitarDetalle(pedidoId, detalleId);
  await accion(async () => { await api(`/pedidos/${pedidoId}/detalles/${detalleId}`, {method: 'PUT', body: {cantidad}}); await refrescarMesasYDashboard(); });
}
async function quitarDetalle(pedidoId, detalleId) {
  await accion(async () => { await api(`/pedidos/${pedidoId}/detalles/${detalleId}`, {method: 'DELETE'}); await refrescarMesasYDashboard(); });
}
async function cambiarEstadoPedido(pedidoId, estado) {
  await accion(async () => { await api(`/pedidos/${pedidoId}/estado`, {method: 'PATCH', body: {estado}}); await refrescarMesasYDashboard(); }, 'Estado actualizado');
}
async function cobrarMesa(id) {
  const clienteId = $('cobroCliente').value ? Number($('cobroCliente').value) : null;
  const f = await accion(async () => {
    const r = await api(`/mesas/${id}/cobrar`, {method: 'POST', body: {clienteId}});
    S.clientes = await api('/clientes');
    await refrescarMesasYDashboard();
    renderClientes();
    return r;
  });
  if (f) toast(`Cobrado ${fmt(f.total)} · factura ${f.numero} (${ESTADO_FAC[f.estado][1]})`);
}
function reporteMesas() { cierreDeCaja(); }

// ---------- INVENTARIO ----------
const EST_ING = {NORMAL: ['ok', 'Normal'], BAJO: ['warn', 'Bajo'], CRITICO: ['bad', 'Crítico']};

function renderInventario() {
  const crit = S.ingredientes.filter(i => i.estado !== 'NORMAL').length;
  titles.inventario[1] = `${S.ingredientes.length} ingredientes · ${crit} con stock bajo o crítico`;
  if (vistaActual === 'inventario') $('pageSub').textContent = titles.inventario[1];
  $('invBody').innerHTML = S.ingredientes.map(i => {
    const [cls, label] = EST_ING[i.estado];
    return `<tr class="${i.id === S.selIng ? 'sel' : ''}" onclick="selectIng(${i.id})">
      <td>${esc(i.nombre)} <span class="badge ${cls}">${label}</span></td>
      <td>${i.stock} ${esc(i.unidad)} <span style="color:var(--text-muted)">mín ${i.stockMinimo}</span></td>
      <td>${fmt(i.costoUnitario)}</td><td>${esc(i.proveedor || '—')}</td></tr>`;
  }).join('');
  renderIngDetail();
}
async function selectIng(id) {
  S.selIng = id;
  S.movimientos = await accion(() => api(`/inventario/ingredientes/${id}/movimientos`)) || [];
  renderInventario();
}
function setMovTab(t) { S.movTab = t; renderIngDetail(); }

function renderIngDetail() {
  const el = $('invDetail');
  const ing = S.ingredientes.find(i => i.id === S.selIng);
  if (!ing) { el.innerHTML = '<div class="empty-hint">Selecciona un ingrediente para registrar un movimiento.</div>'; return; }
  const tabs = {ENTRADA: 'Entrada', MERMA: 'Merma', AJUSTE: 'Ajuste'};
  el.innerHTML = `
    <div class="panel-head"><div><div class="panel-title">Registrar movimiento</div>
      <div class="panel-sub">${esc(ing.nombre)} · ${ing.stock} ${esc(ing.unidad)} en stock</div></div></div>
    <div class="tabs" style="margin-bottom:14px">${Object.entries(tabs).map(([k, v]) => `<button class="tab ${k === S.movTab ? 'active' : ''}" onclick="setMovTab('${k}')">${v}</button>`).join('')}</div>
    ${campo('movCant', S.movTab === 'AJUSTE' ? 'Nuevo stock total' : 'Cantidad', '', 'number')}
    ${campo('movCosto', 'Costo unitario', ing.costoUnitario, 'number')}
    ${campo('movObs', 'Observaciones (opcional)')}
    <button class="btn-primary" style="width:100%" onclick="registrarMovimiento(${ing.id})">✓ Confirmar ${tabs[S.movTab].toLowerCase()}</button>
    <div class="panel-sub" style="margin:14px 0 4px">Últimos movimientos</div>
    ${S.movimientos.slice(0, 6).map(m => `<div class="mov-row"><span>${m.tipo} · ${m.cantidad}</span><span>${new Date(m.fecha).toLocaleDateString('es-CO')}</span></div>`).join('')
      || '<div class="row-meta">Sin movimientos todavía</div>'}`;
}
async function registrarMovimiento(id) {
  const cantidad = Number($('movCant').value);
  if ($('movCant').value === '' || isNaN(cantidad)) return toast('Escribe una cantidad', true);
  const costo = $('movCosto').value ? Number($('movCosto').value) : null;
  const ok = await accion(() => api(`/inventario/ingredientes/${id}/movimientos`, {method: 'POST',
    body: {tipo: S.movTab, cantidad, costoUnitario: costo, observaciones: $('movObs').value || null}}), 'Movimiento registrado');
  if (ok) {
    S.ingredientes = await api('/inventario/ingredientes');
    S.movimientos = await api(`/inventario/ingredientes/${id}/movimientos`);
    renderInventario();
    S.dash = await api('/dashboard');
  }
}
async function nuevoIngrediente() {
  const proveedores = await accion(() => api('/proveedores')) || [];
  abrirModal('Nuevo ingrediente',
    campo('niNombre', 'Nombre') + campo('niUnidad', 'Unidad (kg, un., l...)', 'kg') +
    campo('niStock', 'Stock inicial', '0', 'number') + campo('niMin', 'Stock mínimo', '0', 'number') +
    campo('niCosto', 'Costo unitario', '0', 'number') +
    `<div class="form-field"><label class="form-label">Proveedor</label><select class="form-input" id="niProv"><option value="">—</option>
      ${proveedores.map(p => `<option value="${p.id}">${esc(p.nombre)}</option>`).join('')}</select></div>`,
    async bg => {
      const v = id => bg.querySelector('#' + id).value;
      await api('/inventario/ingredientes', {method: 'POST', body: {
        nombre: v('niNombre'), unidad: v('niUnidad'), stock: Number(v('niStock')), stockMinimo: Number(v('niMin')),
        costoUnitario: Number(v('niCosto')), proveedorId: v('niProv') ? Number(v('niProv')) : null}});
      S.ingredientes = await api('/inventario/ingredientes');
      renderInventario();
      toast('Ingrediente creado');
    });
}

// ---------- CLIENTES ----------
function renderClientes() {
  titles.clientes[1] = `${S.clientes.length} clientes · historial y total gastado`;
  $('clientesSub').textContent = `${S.clientes.length} clientes`;
  if (vistaActual === 'clientes') $('pageSub').textContent = titles.clientes[1];
  $('clientesBody').innerHTML = S.clientes.map(c => `
    <tr class="${c.id === S.selCliente ? 'sel' : ''}" onclick="selectCliente(${c.id})">
      <td>${esc(c.nombre)}</td><td>${esc(c.documento || '—')}</td><td>${c.visitas}</td><td>${fmt(c.totalGastado)}</td></tr>`).join('')
    || '<tr><td colspan="4" style="text-align:center;color:var(--text-muted)">Sin resultados</td></tr>';
  renderClienteDetail();
}
function selectCliente(id) { S.selCliente = id; S.historial = null; renderClientes(); }

function renderClienteDetail() {
  const c = S.clientes.find(x => x.id === S.selCliente);
  const el = $('clienteDetail');
  if (!c) { el.innerHTML = '<div class="empty-hint">Selecciona un cliente.</div>'; return; }
  el.innerHTML = `
    <div class="detail-card">${c.vip ? '<span class="pill">VIP</span>' : ''}
      <div style="font-weight:700;font-size:16px;margin-top:4px">${esc(c.nombre)}</div>
      <div style="font-size:11.5px;color:#a9aec8;margin-top:2px">${esc(c.documento || '—')}</div>
      <div style="font-size:11.5px;color:#a9aec8;margin-top:8px">${esc(c.email || '—')} · ${esc(c.telefono || '—')}</div>
      <div style="font-size:11.5px;color:#a9aec8">${esc(c.direccion || '—')}</div>
      <div class="stat-mini"><div><b>${fmt(c.totalGastado)}</b><span>Total gastado</span></div><div><b>${c.visitas}</b><span>Visitas</span></div><div><b>${esc(c.frecuencia || '—')}</b><span>Frecuencia</span></div></div>
    </div>
    <button class="btn-secondary" style="width:100%;margin-bottom:8px" onclick="verHistorial(${c.id})">📋 Ver historial de pedidos</button>
    <button class="btn-primary" style="width:100%" onclick="editarCliente(${c.id})">✎ Editar datos</button>
    ${S.historial ? `<div class="panel-sub" style="margin:14px 0 4px">Historial (${S.historial.length})</div>
      ${S.historial.slice(0, 8).map(p => `<div class="order-line"><span>${esc(p.numeroPedido)} · ${new Date(p.fecha).toLocaleDateString('es-CO')}</span><span>${fmt(p.total)}</span></div>`).join('')
        || '<div class="row-meta">Sin pedidos aún</div>'}` : ''}`;
}
async function verHistorial(id) {
  S.historial = await accion(() => api(`/clientes/${id}/historial`)) || [];
  renderClienteDetail();
}
function formCliente(c = {}) {
  return campo('cNombre', 'Nombre / Razón social', c.nombre) + campo('cDoc', 'Documento (CC o NIT)', c.documento) +
    campo('cEmail', 'Correo', c.email, 'email') + campo('cTel', 'Teléfono', c.telefono) + campo('cDir', 'Dirección', c.direccion);
}
const leerCliente = bg => {
  const v = id => bg.querySelector('#' + id).value.trim() || null;
  return {nombre: v('cNombre'), documento: v('cDoc'), email: v('cEmail'), telefono: v('cTel'), direccion: v('cDir')};
};
function nuevoCliente() {
  abrirModal('Nuevo cliente', formCliente(), async bg => {
    const nuevo = await api('/clientes', {method: 'POST', body: leerCliente(bg)});
    S.selCliente = nuevo.id;
    await cargarClientes();
    toast('Cliente registrado');
  });
}
function editarCliente(id) {
  const c = S.clientes.find(x => x.id === id);
  abrirModal('Editar cliente', formCliente(c), async bg => {
    await api('/clientes/' + id, {method: 'PUT', body: leerCliente(bg)});
    await cargarClientes();
    toast('Cliente actualizado');
  });
}

// ---------- FACTURACIÓN ----------
function renderFacturas() {
  const r = S.resumen;
  if (r) {
    $('facTotal').textContent = fmt(r.totalVentas);
    $('facTotalFoot').textContent = `${r.totalFacturas} facturas`;
    $('facIva').textContent = fmt(r.ivaRecaudado);
    $('facAcept').textContent = r.aceptadas;
    $('facAceptFoot').textContent = r.totalFacturas ? (r.aceptadas * 100 / r.totalFacturas).toFixed(1).replace('.', ',') + '% del total' : '';
    $('facRech').textContent = r.rechazadas;
    $('facRechFoot').textContent = r.pendientes ? `${r.pendientes} pendientes de DIAN` : 'sin pendientes';
    $('facSub').textContent = `${r.totalFacturas} facturas emitidas`;
  }
  $('facBody').innerHTML = S.facturas.map(f => {
    const [cls, label] = ESTADO_FAC[f.estado];
    return `<tr><td>${esc(f.numero)}</td><td>${esc(f.cliente)}</td><td>${fmt(f.subtotal)}</td><td>${fmt(f.total)}</td>
      <td><span class="badge ${cls}">${label}</span></td>
      <td style="white-space:nowrap">${f.estado !== 'ACEPTADA_DIAN' ? `<button class="btn-sm" onclick="validarDian(${f.id})">Validar DIAN</button> ` : ''}
        ${f.estado !== 'RECHAZADA' ? `<button class="btn-sm danger" onclick="anularFactura(${f.id})">Anular</button>` : ''}</td></tr>`;
  }).join('') || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted)">Sin resultados</td></tr>';
}
async function validarDian(id) {
  const f = await accion(() => api(`/facturas/${id}/validar-dian`, {method: 'POST'}));
  if (f) { toast(`${f.numero}: ${ESTADO_FAC[f.estado][1]}`); await refrescarMesasYDashboard(); }
}
async function anularFactura(id) {
  if (!confirm('¿Anular esta factura?')) return;
  const f = await accion(() => api(`/facturas/${id}/anular`, {method: 'POST'}), 'Factura anulada');
  if (f) { S.clientes = await api('/clientes'); renderClientes(); await refrescarMesasYDashboard(); }
}
function reporteDian() {
  const r = S.resumen; if (!r) return;
  abrirModal('Reporte DIAN', `
    <div class="total-line"><span>Facturas emitidas</span><span>${r.totalFacturas}</span></div>
    <div class="total-line"><span>Aceptadas</span><span>${r.aceptadas}</span></div>
    <div class="total-line"><span>Pendientes</span><span>${r.pendientes}</span></div>
    <div class="total-line"><span>Rechazadas / anuladas</span><span>${r.rechazadas}</span></div>
    <div class="total-line grand"><span>Total facturado</span><span>${fmt(r.totalVentas)}</span></div>`,
    () => {}, 'Cerrar');
}

// ---------- arranque: ¿ya hay sesión abierta? ----------
(async function iniciar() {
  try {
    const res = await fetch('/api/auth/me');
    if (res.ok) entrarAlPanel(await res.json());
  } catch (e) { /* se queda en la pantalla de login */ }
})();
