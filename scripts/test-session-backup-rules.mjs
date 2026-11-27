import assert from 'node:assert/strict';

const project = 'demo-soleil-backup';
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const dbHost = process.env.FIREBASE_DATABASE_EMULATOR_HOST ?? '127.0.0.1:9000';
for (const host of [authHost, dbHost]) {
  assert.match(host, /^(127\.0\.0\.1|localhost):\d+$/, 'Use local emulators only');
}

async function signUp(name) {
  const response = await fetch(
    `http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: `${name}-${crypto.randomUUID()}@example.test`,
        password: 'emulator-only-password',
        returnSecureToken: true,
      }),
    },
  );
  assert.equal(response.status, 200, 'Auth emulator signup');
  return response.json();
}

async function request(method, path, user, value) {
  const url = new URL(`http://${dbHost}/${path}.json`);
  url.searchParams.set('ns', `${project}-default-rtdb`);
  if (user) url.searchParams.set('auth', user.idToken);
  return fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: value === undefined ? undefined : JSON.stringify(value),
  });
}

async function allowed(method, path, user, value) {
  const response = await request(method, path, user, value);
  assert.equal(response.status, 200, `${method} ${path} should be allowed`);
  return response.json();
}

async function denied(method, path, user, value) {
  const response = await request(method, path, user, value);
  assert.ok([401, 403].includes(response.status), `${method} ${path} returned ${response.status}`);
}

const a = await signUp('a');
const b = await signUp('b');
const session = `users/${a.localId}/sessions/73657373696f6e5f31`;
const chunk = `${session}/chunks/0/0`;
const complete = `${session}/complete`;
const payload = '[[100,0,0,0,0,0,0,0,0,0,0,0]]';
const manifest = JSON.stringify({ version: 1, sessionId: 'session_1' });

await allowed('PUT', chunk, a, payload);
await allowed('PUT', chunk, a, payload);
await denied('PUT', chunk, a, 'different chunk');
await denied('GET', session, b);
await denied('PUT', chunk, b, payload);
await denied('PUT', complete, b, manifest);
await denied('GET', session, null);
await denied('PUT', chunk, null, payload);
await allowed('PUT', complete, a, manifest);
await allowed('PUT', complete, a, manifest);
await denied('PUT', complete, a, 'different manifest');
await denied('PUT', chunk, a, payload);
await denied('PUT', session, a, { complete: manifest });
assert.equal(await allowed('GET', chunk, a), payload);
assert.equal(await allowed('GET', complete, a), manifest);
console.log('Realtime Database emulator passed UID isolation, retries, and sealed backup read-back.');
