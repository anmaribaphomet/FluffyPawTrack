import { assert, assertEquals, assertFalse, assertRejects, assertThrows } from "jsr:@std/assert@1";
import {
  AccessTokenCache,
  base64UrlEncode,
  buildFcmMessage,
  DEFAULT_TOKEN_URI,
  FCM_SCOPE,
  isUnregisteredError,
  parseServiceAccount,
  secretsMatch,
  type ServiceAccount,
  signServiceAccountJwt,
} from "./fcm.ts";

Deno.test("buildFcmMessage sends a high-priority data-only message with report_id", () => {
  const message = buildFcmMessage("tok", { id: "n1", user_id: "u1", title: "T", body: "B", report_id: "r1" });
  assertEquals(message, {
    message: { token: "tok", android: { priority: "high" }, data: { title: "T", body: "B", report_id: "r1" } },
  });
});

Deno.test("buildFcmMessage omits report_id when the notification has none", () => {
  const message = buildFcmMessage("tok", { id: "n1", user_id: "u1", title: "T", body: "B", report_id: null });
  assertEquals(message.message.data, { title: "T", body: "B" });
  assertFalse("notification" in message.message);
});

const fcmError = (status: string, details: unknown[], message = "") => ({ error: { status, message, details } });

Deno.test("isUnregisteredError recognizes UNREGISTERED and NOT_FOUND", () => {
  assert(isUnregisteredError(404, fcmError("NOT_FOUND", [{ errorCode: "UNREGISTERED" }])));
  assert(isUnregisteredError(404, fcmError("NOT_FOUND", [])));
});

Deno.test("isUnregisteredError deletes only for INVALID_ARGUMENT about the token", () => {
  const badToken = fcmError("INVALID_ARGUMENT", [
    { errorCode: "INVALID_ARGUMENT" },
    { fieldViolations: [{ field: "message.token", description: "Invalid registration token" }] },
  ]);
  assert(isUnregisteredError(400, badToken));
  assert(isUnregisteredError(400, fcmError("INVALID_ARGUMENT", [], "The registration token is not a valid FCM registration token")));
  const badPayload = fcmError("INVALID_ARGUMENT", [
    { fieldViolations: [{ field: "message.data[0].value", description: "Invalid value" }] },
  ], "Invalid value at 'message.data[0].value'");
  assertFalse(isUnregisteredError(400, badPayload));
});

Deno.test("isUnregisteredError keeps the token on transient or auth errors", () => {
  assertFalse(isUnregisteredError(503, fcmError("UNAVAILABLE", [{ errorCode: "UNAVAILABLE" }])));
  assertFalse(isUnregisteredError(401, fcmError("UNAUTHENTICATED", [])));
  assertFalse(isUnregisteredError(500, null));
});

Deno.test("parseServiceAccount validates fields without echoing the secret", () => {
  assertThrows(() => parseServiceAccount(undefined), Error, "not set");
  const err = assertThrows(() => parseServiceAccount("{not json secret-value"), Error);
  assertFalse(err.message.includes("secret-value"));
  assertThrows(() => parseServiceAccount(JSON.stringify({ project_id: "p" })), Error, "client_email");
  const account = parseServiceAccount(JSON.stringify({ project_id: "p", client_email: "e", private_key: "k" }));
  assertEquals(account.token_uri, DEFAULT_TOKEN_URI);
});

Deno.test("secretsMatch compares exactly", () => {
  assert(secretsMatch("abc", "abc"));
  assertFalse(secretsMatch("abd", "abc"));
  assertFalse(secretsMatch("abcd", "abc"));
  assertFalse(secretsMatch(null, "abc"));
});

async function testAccount(): Promise<{ account: ServiceAccount; publicKey: CryptoKey }> {
  const pair = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  );
  const der = new Uint8Array(await crypto.subtle.exportKey("pkcs8", pair.privateKey));
  let binary = "";
  for (const b of der) binary += String.fromCharCode(b);
  const pem = `-----BEGIN PRIVATE KEY-----\n${btoa(binary).replace(/.{64}/g, "$&\n")}\n-----END PRIVATE KEY-----\n`;
  return {
    account: { project_id: "p", client_email: "svc@example.test", private_key: pem, token_uri: DEFAULT_TOKEN_URI },
    publicKey: pair.publicKey,
  };
}

function decodePart(part: string): Record<string, unknown> {
  const base64 = part.replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(atob(base64 + "=".repeat((4 - base64.length % 4) % 4)));
}

Deno.test("signServiceAccountJwt produces a verifiable RS256 assertion with FCM scope", async () => {
  const { account, publicKey } = await testAccount();
  const jwt = await signServiceAccountJwt(account, 1_000);
  const [header, claims, signature] = jwt.split(".");
  assertEquals(decodePart(header), { alg: "RS256", typ: "JWT" });
  assertEquals(decodePart(claims), {
    iss: "svc@example.test",
    scope: FCM_SCOPE,
    aud: DEFAULT_TOKEN_URI,
    iat: 1_000,
    exp: 4_600,
  });
  const sigBytes = Uint8Array.from(atob(signature.replace(/-/g, "+").replace(/_/g, "/") + "=="), (c) => c.charCodeAt(0));
  assert(await crypto.subtle.verify("RSASSA-PKCS1-v1_5", publicKey, sigBytes, new TextEncoder().encode(`${header}.${claims}`)));
  assertEquals(base64UrlEncode(new Uint8Array([251, 255])), "-_8");
});

Deno.test("AccessTokenCache reuses the token until 5 minutes before expiry", async () => {
  const { account } = await testAccount();
  let now = 0;
  let calls = 0;
  const fakeFetch = ((_url: string, init?: RequestInit) => {
    calls++;
    const body = init?.body as URLSearchParams;
    assertEquals(body.get("grant_type"), "urn:ietf:params:oauth:grant-type:jwt-bearer");
    return Promise.resolve(Response.json({ access_token: `at-${calls}`, expires_in: 3600 }));
  }) as typeof fetch;
  const cache = new AccessTokenCache(account, fakeFetch, () => now);

  assertEquals(await cache.get(), "at-1");
  now = 54 * 60_000;
  assertEquals(await cache.get(), "at-1");
  now = 56 * 60_000;
  assertEquals(await cache.get(), "at-2");
  assertEquals(calls, 2);
});

Deno.test("AccessTokenCache surfaces a failed token request", async () => {
  const { account } = await testAccount();
  const failing = (() => Promise.resolve(new Response('{"error":"invalid_grant"}', { status: 400 }))) as typeof fetch;
  await assertRejects(() => new AccessTokenCache(account, failing).get(), Error, "invalid_grant");
});
