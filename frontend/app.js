const API = 'http://localhost:8080';
const state = { token: sessionStorage.getItem('amanah_token'), username: sessionStorage.getItem('amanah_user'), accounts: [], transactions: [] };
const $ = (id) => document.getElementById(id);

async function request(path, options = {}) {
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}) };
  const response = await fetch(`${API}${path}`, { ...options, headers });
  let payload = null;
  try { payload = await response.json(); } catch (_) { /* empty response */ }
  if (!response.ok) throw new Error(payload?.message || `Request failed (${response.status})`);
  return payload;
}

function money(value) { return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(Number(value || 0)); }
function showNotice(message, error = false) { const node = $('notice'); node.textContent = message; node.classList.toggle('hidden', !message); node.style.borderColor = error ? '#c34f40' : ''; node.style.background = error ? '#fff0ef' : ''; }
function showView(authenticated) { $('loginView').classList.toggle('hidden', authenticated); $('appView').classList.toggle('hidden', !authenticated); }

async function signIn(event) {
  event.preventDefault(); $('loginError').textContent = '';
  try {
    const result = await request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: $('username').value.trim(), password: $('password').value }) });
    state.token = result.data.token; state.username = result.data.username;
    sessionStorage.setItem('amanah_token', state.token); sessionStorage.setItem('amanah_user', state.username);
    showView(true); await loadDashboard();
  } catch (error) { $('loginError').textContent = error.message; }
}

async function loadDashboard() {
  $('welcomeName').textContent = state.username || 'there'; $('userBadge').textContent = state.username || '';
  showNotice('');
  try {
    const result = await request('/api/accounts');
    state.accounts = result.data || [];
    $('accountCount').textContent = state.accounts.length;
    $('totalBalance').textContent = money(state.accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0));
    renderAccounts();
    await loadActivity();
  } catch (error) { showNotice(error.message, true); }
}

async function loadActivity() {
  const histories = await Promise.all(state.accounts.map(async (account) => {
    try { const response = await request(`/api/accounts/${account.id}/transactions`); return response.data || []; } catch (_) { return []; }
  }));
  state.transactions = histories.flat().sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
  $('activityCount').textContent = state.transactions.length;
  renderActivity();
}

function renderAccounts() {
  const list = $('accountsList');
  if (!state.accounts.length) { list.innerHTML = '<div class="empty-state">No accounts yet. Create your first account to get started.</div>'; return; }
  list.innerHTML = state.accounts.map((account) => `<div class="account-row"><div><div class="account-title">${account.accountType}</div><div class="account-number">${account.accountNumber} · ${account.status}</div></div><div><div class="account-balance">${money(account.balance)}</div><div class="account-actions"><button class="mini-button" data-action="deposit" data-id="${account.id}">Deposit</button><button class="mini-button" data-action="withdraw" data-id="${account.id}">Withdraw</button></div></div></div>`).join('');
  list.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => openMoneyAction(button.dataset.action, Number(button.dataset.id))));
}

function renderActivity() {
  const list = $('activityList');
  if (!state.transactions.length) { list.innerHTML = '<div class="empty-state">No transactions yet.</div>'; return; }
  list.innerHTML = state.transactions.slice(0, 8).map((tx) => { const incoming = tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN'; const sign = incoming ? '+' : '-'; const label = tx.transactionType.replace('_', ' '); return `<div class="activity-item"><div class="activity-main"><span class="activity-icon">${incoming ? '↗' : '↘'}</span><div><div class="activity-label">${label}</div><div class="activity-date">${tx.description || 'Account movement'}</div></div></div><span class="activity-amount ${incoming ? 'positive' : 'negative'}">${sign}${money(tx.amount)}</span></div>`; }).join('');
}

function openDialog(title, description, fields, submit) { $('dialogTitle').textContent = title; $('dialogDescription').textContent = description; $('dialogFields').innerHTML = fields; $('actionError').textContent = ''; $('actionForm').onsubmit = async (event) => { event.preventDefault(); try { await submit(new FormData(event.currentTarget)); $('actionDialog').close(); await loadDashboard(); showNotice('Action completed successfully.'); } catch (error) { $('actionError').textContent = error.message; } }; $('actionDialog').showModal(); }
function openMoneyAction(action, accountId) { const account = state.accounts.find((item) => item.id === accountId); const title = action === 'deposit' ? 'Add money' : 'Withdraw funds'; openDialog(title, `${account.accountType} · ${account.accountNumber}`, `<label>Amount<input name="amount" type="number" min="0.01" step="0.01" required placeholder="0.00"></label><label>Description<input name="description" maxlength="255" placeholder="Optional note"></label>`, async (form) => { await request(`/api/accounts/${accountId}/${action}`, { method: 'POST', body: JSON.stringify({ amount: Number(form.get('amount')), description: form.get('description') || null }) }); }); }
function openNewAccount() { openDialog('Open an account', 'Choose the account structure that fits your everyday banking.', '<label>Account type<select name="accountType"><option value="SAVINGS">Savings</option><option value="WADIAH">Wadiah</option></select></label>', async (form) => { await request('/api/accounts', { method: 'POST', body: JSON.stringify({ accountType: form.get('accountType') }) }); }); }
function logout() { state.token = null; state.username = null; sessionStorage.clear(); showView(false); $('loginForm').reset(); }

$('loginForm').addEventListener('submit', signIn); $('logoutButton').addEventListener('click', logout); $('refreshButton').addEventListener('click', loadDashboard); $('newAccountButton').addEventListener('click', openNewAccount);
if (state.token) { showView(true); loadDashboard(); } else showView(false);
