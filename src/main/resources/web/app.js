let csrf = '', current = null, loading = false;
const $ = id => document.getElementById(id);
const escapeHtml = value => { const node = document.createElement('div'); node.textContent = String(value); return node.innerHTML; };
async function api(path, options = {}) { options.headers = { 'Content-Type': 'application/json', ...(options.headers || {}), 'X-CSRF-Token': csrf }; const response = await fetch('/api/v1' + path, options); const json = await response.json(); if (!response.ok) { const error = new Error(json.error?.message || '操作に失敗しました'); error.traceId = json.error?.traceId; throw error; } return json.data; }
function notify(message, type = 'info', traceId = '') { $('notice').className = type; $('notice').textContent = traceId ? `${message}（追跡ID: ${traceId}）` : message; }
function setLoading(value) { loading = value; document.body.classList.toggle('loading', value); for (const element of document.querySelectorAll('button:not(nav button), input')) { if (value && !element.disabled) { element.disabled = true; element.dataset.loadingDisabled = 'true'; } if (!value && element.dataset.loadingDisabled) { element.disabled = false; delete element.dataset.loadingDisabled; } } }
function show(page) { for (const id of ['home', 'game', 'system']) $(id).hidden = true; $(page).hidden = false; $('title').textContent = { home: 'ホーム', game: 'ゲーム操作', system: 'システム' }[page]; if (page === 'system') loadDiagnostics(); }
document.querySelectorAll('[data-page]').forEach(button => button.addEventListener('click', () => show(button.dataset.page)));
function renderStatus(status) { current = status; $('paper').textContent = status.paperRunning ? '稼働中' : '停止'; $('state').textContent = status.gameState; $('participants').textContent = `${status.participantCount} / ${status.participantLimit}`; $('warnings').innerHTML = (status.warnings || []).map(value => `<li>${escapeHtml(value)}</li>`).join('') || '<li class="empty">警告はありません</li>'; }
function renderPlayers(playerResult) { $('players').innerHTML = playerResult.players.map(player => `<li><span>${escapeHtml(player.name)}</span></li>`).join('') || '<li class="empty">現在参加しているプレイヤーはいません</li>'; $('online-count').textContent = `${playerResult.players.length}人がオンライン`; }

async function refresh(options = { notifyResult: true }) {
  if (loading) return { complete: false, failures: [new Error('更新処理中です')] };
  setLoading(true); const failures = []; let statusAvailable = false, whitelistAvailable = false;
  try {
    if (!csrf) csrf = (await api('/auth/session')).csrfToken;
    const [statusResult, playersResult, whitelistResult] = await Promise.allSettled([api('/status'), api('/players'), api('/server/whitelist')]);
    if (statusResult.status === 'fulfilled') { renderStatus(statusResult.value); statusAvailable = true; }
    else { current = null; failures.push(statusResult.reason); $('paper').textContent = '取得失敗'; $('state').textContent = '—'; $('participants').textContent = '—'; $('warnings').innerHTML = '<li class="empty">サーバー状態を取得できませんでした</li>'; }
    if (playersResult.status === 'fulfilled') renderPlayers(playersResult.value);
    else { failures.push(playersResult.reason); $('players').innerHTML = '<li class="empty">オンラインプレイヤーを取得できませんでした</li>'; $('online-count').textContent = '取得失敗'; }
    if (whitelistResult.status === 'fulfilled') { renderWhitelist(whitelistResult.value); whitelistAvailable = true; }
    else { failures.push(whitelistResult.reason); $('whitelist-status').textContent = '取得失敗'; $('whitelist-players').innerHTML = '<li class="empty">ホワイトリスト情報を取得できませんでした</li>'; }
  } catch (error) { failures.push(error); }
  finally { setLoading(false); $('whitelist-toggle').disabled = !whitelistAvailable; $('player-name').disabled = !whitelistAvailable; $('whitelist-form').querySelector('button').disabled = !whitelistAvailable; $('start').disabled = !statusAvailable || current.gameState !== 'IDLE' || current.diagnosticMode; $('close').disabled = !statusAvailable || current.gameState !== 'RECRUITING' || current.diagnosticMode; }
  if (options.notifyResult) { if (failures.length) notify('一部の最新情報を取得できませんでした', 'error', failures[0].traceId); else notify(current.diagnosticMode ? '診断モードで動作しています' : '最新の状態です', current.diagnosticMode ? 'error' : 'success'); }
  return { complete: failures.length === 0, failures };
}

function renderWhitelist(whitelist) { $('whitelist-toggle').checked = whitelist.enabled; $('whitelist-toggle').nextElementSibling.textContent = whitelist.enabled ? 'ON' : 'OFF'; $('whitelist-status').textContent = whitelist.enabled ? 'ON' : 'OFF'; $('whitelist-players').innerHTML = whitelist.players.map(player => `<li><span>${escapeHtml(player.name)}</span><button class="danger" data-remove="${escapeHtml(player.name)}">削除</button></li>`).join('') || '<li class="empty">登録されているプレイヤーはいません</li>'; }
async function mutate(path, method, body, successMessage, headers = {}) { setLoading(true); try { await api(path, { method, headers, body: JSON.stringify(body) }); } catch (error) { setLoading(false); await refresh({ notifyResult: false }); notify(error.message, 'error', error.traceId); return; } setLoading(false); const refreshed = await refresh({ notifyResult: false }); if (refreshed.complete) notify(successMessage, 'success'); else notify(`${successMessage}。ただし、最新状態を確認できていません`, 'warning', refreshed.failures[0]?.traceId); }
$('refresh').onclick = () => refresh();
$('whitelist-toggle').onchange = event => mutate('/server/whitelist', 'PUT', { enabled: event.target.checked }, `ホワイトリストを${event.target.checked ? 'ON' : 'OFF'}にしました`);
$('whitelist-form').onsubmit = event => { event.preventDefault(); const name = $('player-name').value.trim(); mutate('/server/whitelist/players', 'POST', { name }, `${name}を追加しました`); $('player-name').value = ''; };
$('whitelist-players').onclick = event => { const name = event.target.dataset.remove; if (name && confirm(`${name}をホワイトリストから削除しますか？`)) mutate('/server/whitelist/players', 'DELETE', { name }, `${name}を削除しました`); };
$('start').onclick = () => mutate('/game/recruiting/start', 'POST', {}, '参加受付を開始しました', { 'If-Game-State': current.gameState });
$('close').onclick = () => mutate('/game/recruiting/close', 'POST', {}, '参加受付を終了しました', { 'If-Session-Id': current.sessionId });
async function loadDiagnostics() { try { const d = await api('/system/diagnostics'); const rows = [['Paper状態', d.paperRunning ? '稼働中' : '停止'], ['HTTP bind', d.httpBound ? '正常' : '異常'], ['保存領域', d.storageReady ? '利用可能' : '利用不可'], ['設定読込み', d.configLoaded ? '正常' : '失敗'], ['ゲーム状態', d.gameState], ['診断モード', d.diagnosticMode ? '有効' : '無効'], ['警告', (d.warnings || []).join(' / ') || 'なし'], ['追跡ID', d.traceId]]; $('diagnostics').innerHTML = rows.map(([key, value]) => `<dt>${escapeHtml(key)}</dt><dd>${escapeHtml(value)}</dd>`).join(''); } catch (error) { notify(error.message, 'error', error.traceId); } }
refresh(); setInterval(refresh, 5000);
