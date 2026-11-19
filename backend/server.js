/*
 * Shitgram LAN backend
 * Internal test currency only. It never connects to Telegram or Telegram Stars.
 */
const http = require('node:http');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const host = process.env.SHITGRAM_HOST || '0.0.0.0';
const port = Number(process.env.SHITGRAM_PORT || 8787);
const adminKey = process.env.SHITGRAM_ADMIN_KEY || 'change-this-before-sharing';
const dataFile = path.join(__dirname, 'shitgram-data.json');

const blank = () => ({ users: {}, messages: [], sessions: {} });
let data = blank();
try { data = { ...blank(), ...JSON.parse(fs.readFileSync(dataFile, 'utf8')) }; } catch (_) {}
function save() { fs.writeFileSync(dataFile, JSON.stringify(data, null, 2)); }
function json(res, status, value) {
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'access-control-allow-origin': '*', 'access-control-allow-headers': 'content-type, authorization' });
  res.end(JSON.stringify(value));
}
function read(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', part => { body += part; if (body.length > 32_000) req.destroy(); });
    req.on('end', () => { try { resolve(body ? JSON.parse(body) : {}); } catch { reject(new Error('Invalid JSON')); } });
    req.on('error', reject);
  });
}
function session(req) {
  const token = String(req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  const userId = data.sessions[token];
  return userId ? data.users[userId] : null;
}
function cleanName(value) { return String(value || '').trim().replace(/[^\p{L}\p{N}_ .-]/gu, '').slice(0, 32); }

const adminHtml = `<!doctype html><meta charset="utf-8"><title>Shitgram LAN admin</title>
<style>body{font:16px system-ui;max-width:540px;margin:48px auto;padding:0 16px;background:#17212b;color:#fff}input,button{font:inherit;padding:9px;margin:5px 0;width:100%;box-sizing:border-box}button{background:#3390ec;color:#fff;border:0;border-radius:6px}small{color:#aab8c2}</style>
<h1>Shitgram LAN</h1><p>Выдача <b>тестовых</b> звёзд. Они существуют только на этом компьютере.</p>
<input id="key" type="password" placeholder="Ключ администратора"><input id="name" placeholder="Ник пользователя"><input id="stars" type="number" value="100" min="1"><button onclick="grant()">Выдать звёзды</button><pre id="out"></pre>
<small>Сервер: ${host}:${port}. Измени SHITGRAM_ADMIN_KEY перед использованием другими людьми.</small>
<script>async function grant(){let r=await fetch('/api/admin/stars',{method:'POST',headers:{'content-type':'application/json','authorization':'Bearer '+key.value},body:JSON.stringify({name:name.value,amount:+stars.value})});out.textContent=await r.text()}</script>`;

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return json(res, 204, {});
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  if (req.method === 'GET' && url.pathname === '/') { res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' }); return res.end(adminHtml); }
  try {
    if (req.method === 'POST' && url.pathname === '/api/session') {
      const { name } = await read(req); const safeName = cleanName(name);
      if (safeName.length < 2) return json(res, 400, { error: 'Ник: от 2 символов' });
      let user = Object.values(data.users).find(item => item.name.toLowerCase() === safeName.toLowerCase());
      if (!user) { user = { id: crypto.randomUUID(), name: safeName, stars: 0, createdAt: Date.now() }; data.users[user.id] = user; }
      const token = crypto.randomBytes(24).toString('hex'); data.sessions[token] = user.id; save();
      return json(res, 201, { token, user: { id: user.id, name: user.name, stars: user.stars } });
    }
    if (req.method === 'GET' && url.pathname === '/api/me') {
      const user = session(req); return user ? json(res, 200, { id: user.id, name: user.name, stars: user.stars }) : json(res, 401, { error: 'Нужна локальная сессия' });
    }
    if (req.method === 'GET' && url.pathname === '/api/messages') {
      const after = Number(url.searchParams.get('after') || 0);
      return json(res, 200, data.messages.filter(m => m.id > after).slice(-100));
    }
    if (req.method === 'POST' && url.pathname === '/api/messages') {
      const user = session(req); if (!user) return json(res, 401, { error: 'Нужна локальная сессия' });
      const { text } = await read(req); const messageText = String(text || '').trim().slice(0, 4096);
      if (!messageText) return json(res, 400, { error: 'Пустое сообщение' });
      const message = { id: Date.now() * 1000 + data.messages.length % 1000, userId: user.id, name: user.name, text: messageText, sentAt: Date.now(), type: 'text' };
      data.messages.push(message); data.messages = data.messages.slice(-1000); save(); return json(res, 201, message);
    }
    if (req.method === 'POST' && url.pathname === '/api/gifts/send') {
      const user = session(req); if (!user) return json(res, 401, { error: 'Нужна локальная сессия' });
      const { to, title, cost } = await read(req);
      const recipient = Object.values(data.users).find(item => item.name.toLowerCase() === cleanName(to).toLowerCase());
      const stars = Math.floor(Number(cost));
      if (!recipient) return json(res, 404, { error: 'Получатель ещё не заходил в Shitgram' });
      if (recipient.id === user.id) return json(res, 400, { error: 'Нельзя отправить подарок себе' });
      if (!Number.isSafeInteger(stars) || stars < 1 || stars > 100_000) return json(res, 400, { error: 'Стоимость: 1–100000' });
      if (user.stars < stars) return json(res, 400, { error: 'Недостаточно тестовых звёзд' });
      user.stars -= stars; recipient.stars += stars;
      const gift = { id: Date.now() * 1000 + data.messages.length % 1000, userId: user.id, name: user.name, text: String(title || 'Подарок').trim().slice(0, 80), to: recipient.name, cost: stars, sentAt: Date.now(), type: 'gift' };
      data.messages.push(gift); data.messages = data.messages.slice(-1000); save(); return json(res, 201, gift);
    }
    if (req.method === 'POST' && url.pathname === '/api/admin/stars') {
      if (String(req.headers.authorization || '').replace(/^Bearer\s+/i, '') !== adminKey) return json(res, 403, { error: 'Неверный ключ администратора' });
      const { name, amount } = await read(req); const user = Object.values(data.users).find(item => item.name.toLowerCase() === cleanName(name).toLowerCase());
      const count = Math.floor(Number(amount));
      if (!user) return json(res, 404, { error: 'Пользователь ещё не заходил в Shitgram' });
      if (!Number.isSafeInteger(count) || count < 1 || count > 1_000_000) return json(res, 400, { error: 'Количество: 1–1000000' });
      user.stars += count; save(); return json(res, 200, { name: user.name, stars: user.stars });
    }
    return json(res, 404, { error: 'Not found' });
  } catch (error) { return json(res, 400, { error: error.message || 'Bad request' }); }
});
server.listen(port, host, () => console.log(`Shitgram LAN: http://localhost:${port}  admin key: ${adminKey}`));
