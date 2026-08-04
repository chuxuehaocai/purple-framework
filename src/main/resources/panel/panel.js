const $ = selector => document.querySelector(selector);
const state = { plugins: [], pending: new Set() };
let snackbarTimer;

function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.method && options.method !== 'GET') headers['X-Panel-Request'] = 'plugin-state';
  return fetch(path, { ...options, headers }).then(async response => {
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.message || `HTTP ${response.status}`);
    return body;
  });
}

function formatDuration(milliseconds) {
  const seconds = Math.floor(milliseconds / 1000);
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor(seconds % 86400 / 3600);
  const minutes = Math.floor(seconds % 3600 / 60);
  if (days) return `${days}d ${hours}h`;
  if (hours) return `${hours}h ${minutes}m`;
  return `${minutes}m ${seconds % 60}s`;
}

function formatBytes(bytes) {
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(0)} KiB`;
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MiB`;
  return `${(bytes / 1024 ** 3).toFixed(2)} GiB`;
}

function setText(selector, value) {
  const element = $(selector);
  if (element) element.textContent = value;
}

function showMessage(message) {
  const snackbar = $('#snackbar');
  if (!snackbar) return;
  snackbar.textContent = message;
  snackbar.classList.add('show');
  clearTimeout(snackbarTimer);
  snackbarTimer = setTimeout(() => snackbar.classList.remove('show'), 3200);
}

function renderPlugins() {
  const list = $('#pluginList');
  if (!list) return;
  if (!state.plugins.length) {
    list.innerHTML = '<div class="loading">没有发现插件。把 JAR 放入 plugins 目录后重启 Framework。</div>';
    return;
  }
  list.replaceChildren(...state.plugins.map(plugin => {
    const row = document.createElement('article');
    row.className = 'plugin-row';
    const identity = document.createElement('div');
    const id = document.createElement('span');
    id.className = 'plugin-id';
    id.textContent = plugin.id;
    const source = document.createElement('span');
    source.className = 'plugin-source';
    source.textContent = plugin.embedded ? 'Framework embedded' : plugin.source;
    identity.append(id, source);

    const status = document.createElement('div');
    status.className = `plugin-state ${plugin.lastError ? 'failed' : plugin.enabled ? '' : 'stopped'}`;
    const statusText = document.createElement('strong');
    const pending = state.pending.has(plugin.id);
    statusText.textContent = pending ? (plugin.enabled ? '正在停用…' : '正在启用…') : plugin.lastError ? '需要处理' : plugin.enabled ? '已启用' : '已停用';
    const detail = document.createElement('span');
    detail.textContent = plugin.lastError || (pending ? '等待 Framework 确认' : 'Framework 已确认');
    status.append(statusText, detail);

    const label = document.createElement('label');
    label.className = 'switch';
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.role = 'switch';
    input.checked = plugin.enabled;
    input.disabled = pending;
    input.setAttribute('aria-label', `${plugin.enabled ? '停用' : '启用'}插件 ${plugin.id}`);
    input.addEventListener('change', () => togglePlugin(plugin, input.checked));
    label.append(input, document.createElement('span'));
    row.append(identity, status, label);
    return row;
  }));
}

async function togglePlugin(plugin, shouldEnable) {
  state.pending.add(plugin.id);
  renderPlugins();
  try {
    const updated = await api(`/api/plugins/${encodeURIComponent(plugin.id)}/${shouldEnable ? 'enable' : 'disable'}`, { method: 'POST' });
    state.plugins = state.plugins.map(item => item.id === updated.id ? updated : item);
    showMessage(`${updated.id} 已${updated.enabled ? '启用' : '停用'}`);
    await loadStatus();
  } catch (error) {
    showMessage(`${plugin.id} 切换失败：${error.message}`);
  } finally {
    state.pending.delete(plugin.id);
    renderPlugins();
  }
}

async function loadStatus() {
  const status = await api('/api/status');
  const connection = $('#connection');
  if (connection) {
    connection.classList.toggle('connected', status.transportReady);
    connection.lastChild.textContent = status.transportReady ? '传输已连接' : 'Panel 在线 · 传输未就绪';
  }
  setText('#system-title', `${status.transportReady ? '运行时稳定' : '控制面在线'} · ${status.enabledPluginCount} 个插件已启用`);
  setText('#observedAt', `观测于 ${new Date(status.observedAt).toLocaleTimeString()}`);
  setText('#uptime', formatDuration(status.uptimeMillis));
  setText('#memory', formatBytes(status.memoryUsedBytes));
  setText('#memoryDetail', `上限 ${formatBytes(status.memoryMaxBytes)}`);
  setText('#cpu', status.cpuLoad == null ? '—' : `${(status.cpuLoad * 100).toFixed(1)}%`);
  setText('#processors', `${status.processors} 个逻辑处理器`);
  setText('#pluginMetric', `${status.enabledPluginCount} / ${status.pluginCount}`);
  setText('#enabledPluginCount', status.enabledPluginCount);
  setText('#discoveredPluginCount', status.pluginCount);
}

async function loadPlugins() {
  if (!$('#pluginList')) return;
  const result = await api('/api/plugins');
  state.plugins = result.plugins;
  renderPlugins();
}

async function refresh() {
  const button = $('#refresh');
  if (button) {
    button.disabled = true;
    button.textContent = '正在刷新';
  }
  try {
    await Promise.all([loadStatus(), loadPlugins()]);
  } catch (error) {
    setText('#system-title', '无法读取 Framework 状态');
    showMessage(`刷新失败：${error.message}`);
  } finally {
    if (button) {
      button.disabled = false;
      button.textContent = '刷新观测';
    }
  }
}

$('#refresh')?.addEventListener('click', refresh);
refresh();
setInterval(loadStatus, 5000);
