import { assertEquals } from "jsr:@std/assert@1";
import { type AccessTokenSource, createFcmSend, PushConfigError } from "./fcm.ts";
import { handlePush, type PushDeps } from "./handler.ts";

const record = { id: "n1", user_id: "u1", title: "Nuevo reporte cercano", body: "Herido", report_id: "r1" };

function request(body: unknown, secret: string | null = "s3cret"): Request {
  return rawRequest(JSON.stringify(body), secret);
}

function rawRequest(body: string, secret: string | null = "s3cret"): Request {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (secret !== null) headers.set("x-webhook-secret", secret);
  return new Request("http://localhost/send-push", { method: "POST", headers, body });
}

function fakeDeps(overrides: Partial<PushDeps> & { responses?: Record<string, { status: number; body: unknown }> } = {}) {
  const sent: unknown[] = [];
  const deleted: string[] = [];
  const deps: PushDeps = {
    webhookSecret: "s3cret",
    pushEnabled: () => Promise.resolve(true),
    deviceTokens: () => Promise.resolve(["tok-a", "tok-b"]),
    deleteToken: (_user, token) => {
      deleted.push(token);
      return Promise.resolve();
    },
    send: (message) => {
      sent.push(message);
      const token = (message as { message: { token: string } }).message.token;
      return Promise.resolve(overrides.responses?.[token] ?? { status: 200, body: { name: "ok" } });
    },
    log: () => {},
    ...overrides,
  };
  return { deps, sent, deleted };
}

const fcmError = (status: string, details: unknown[], message = "") => ({ error: { status, message, details } });

Deno.test("rejects a request without the shared secret", async () => {
  const { deps, sent } = fakeDeps();
  assertEquals((await handlePush(request({ type: "INSERT", record }, null), deps)).status, 401);
  assertEquals((await handlePush(request({ type: "INSERT", record }, "wrong"), deps)).status, 401);
  assertEquals(sent.length, 0);
});

Deno.test("refuses everything when the secret is not configured", async () => {
  const { deps, sent } = fakeDeps({ webhookSecret: undefined });
  assertEquals((await handlePush(request({ type: "INSERT", record }, ""), deps)).status, 500);
  assertEquals(sent.length, 0);
});

Deno.test("sends one data message per device token", async () => {
  const { deps, sent } = fakeDeps();
  const response = await handlePush(request({ type: "INSERT", table: "notifications", record }), deps);
  assertEquals(response.status, 200);
  assertEquals(await response.json(), { sent: 2, deleted: 0, failed: 0 });
  assertEquals(sent, ["tok-a", "tok-b"].map((token) => ({
    message: {
      token,
      android: { priority: "high" },
      data: { title: "Nuevo reporte cercano", body: "Herido", report_id: "r1" },
    },
  })));
});

Deno.test("skips users whose push is disabled", async () => {
  const { deps, sent } = fakeDeps({ pushEnabled: () => Promise.resolve(false) });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals((await response.json()).sent, 0);
  assertEquals(sent.length, 0);
});

Deno.test("deletes unregistered tokens and keeps tokens with transient errors", async () => {
  const { deps, deleted } = fakeDeps({
    responses: {
      "tok-a": { status: 404, body: fcmError("NOT_FOUND", [{ errorCode: "UNREGISTERED" }]) },
      "tok-b": { status: 503, body: fcmError("UNAVAILABLE", [{ errorCode: "UNAVAILABLE" }]) },
    },
  });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(response.status, 200);
  assertEquals(await response.json(), { sent: 0, deleted: 1, failed: 1 });
  assertEquals(deleted, ["tok-a"]);
});

Deno.test("deletes a token FCM rejects as an invalid registration token", async () => {
  const { deps, deleted } = fakeDeps({
    responses: {
      "tok-a": {
        status: 400,
        body: fcmError("INVALID_ARGUMENT", [
          { errorCode: "INVALID_ARGUMENT" },
          { fieldViolations: [{ field: "message.token", description: "Invalid registration token" }] },
        ]),
      },
    },
  });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(await response.json(), { sent: 1, deleted: 1, failed: 0 });
  assertEquals(deleted, ["tok-a"]);
});

Deno.test("a bare 404 without UNREGISTERED keeps the token", async () => {
  const { deps, deleted } = fakeDeps({
    responses: { "tok-a": { status: 404, body: fcmError("NOT_FOUND", [], "Requested entity was not found.") } },
  });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(response.status, 200);
  assertEquals(await response.json(), { sent: 1, deleted: 0, failed: 1 });
  assertEquals(deleted, []);
});

Deno.test("returns 500 when every send fails because of the configuration", async () => {
  const wrongProject = { status: 404, body: fcmError("NOT_FOUND", []) };
  const { deps, deleted } = fakeDeps({ responses: { "tok-a": wrongProject, "tok-b": wrongProject } });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(response.status, 500);
  assertEquals(await response.json(), { error: "push misconfigured", sent: 0, deleted: 0, failed: 2 });
  assertEquals(deleted, []);

  const broken = fakeDeps({ send: () => Promise.reject(new PushConfigError("OAuth token request failed (400): invalid_grant")) });
  assertEquals((await handlePush(request({ type: "INSERT", record }), broken.deps)).status, 500);
});

Deno.test("a send that throws counts as failed", async () => {
  let calls = 0;
  const { deps } = fakeDeps({
    send: () => {
      calls++;
      return calls === 1 ? Promise.reject(new Error("connection reset")) : Promise.resolve({ status: 200, body: {} });
    },
  });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(response.status, 200);
  assertEquals(await response.json(), { sent: 1, deleted: 0, failed: 1 });
});

Deno.test("returns 400 when the record misses user_id, title or id", async () => {
  const { deps, sent } = fakeDeps();
  for (const missing of ["user_id", "title", "id"]) {
    const partial: Record<string, unknown> = { ...record };
    delete partial[missing];
    assertEquals((await handlePush(request({ type: "INSERT", record: partial }), deps)).status, 400, missing);
  }
  assertEquals((await handlePush(request({ type: "INSERT", record: { ...record, title: 7 } }), deps)).status, 400);
  assertEquals((await handlePush(request({ type: "INSERT" }), deps)).status, 400);
  assertEquals(sent.length, 0);
});

Deno.test("returns 400 for invalid JSON or a body that is not an object", async () => {
  const { deps, sent } = fakeDeps();
  assertEquals((await handlePush(rawRequest("{not json"), deps)).status, 400);
  assertEquals((await handlePush(rawRequest("null"), deps)).status, 400);
  assertEquals((await handlePush(rawRequest("[1]"), deps)).status, 400);
  assertEquals((await handlePush(rawRequest('"text"'), deps)).status, 400);
  assertEquals(sent.length, 0);
});

Deno.test("ignores events that are not notification inserts", async () => {
  const { deps, sent } = fakeDeps();
  assertEquals((await handlePush(request({ type: "UPDATE", record }), deps)).status, 200);
  assertEquals((await handlePush(request({ type: "INSERT", table: "reports", record }), deps)).status, 200);
  assertEquals(sent.length, 0);
});

/** Wires the real FCM sender with a scripted `fetch`, an instant `sleep` and a fake token source. */
type ScriptedResponse = { status: number; headers?: Record<string, string>; body?: unknown };

function realSender(statuses: ScriptedResponse[]) {
  const authorizations: string[] = [];
  const sleeps: number[] = [];
  const invalidated: string[] = [];
  let current = "at-1";
  const tokens: AccessTokenSource = {
    get: () => Promise.resolve(current),
    invalidate: (token) => {
      invalidated.push(token);
      current = `at-${Number(token.slice(3)) + 1}`;
    },
  };
  const fetchFn = ((_url: string, init?: RequestInit) => {
    authorizations.push(new Headers(init?.headers).get("Authorization") ?? "");
    const next: ScriptedResponse = statuses.shift() ?? { status: 200 };
    return Promise.resolve(Response.json(next.body ?? {}, { status: next.status, headers: next.headers }));
  }) as typeof fetch;
  const send = createFcmSend({
    projectId: "p",
    tokens,
    fetchFn,
    sleep: (ms) => {
      sleeps.push(ms);
      return Promise.resolve();
    },
  });
  return { send, authorizations, sleeps, invalidated };
}

Deno.test("a 401 from FCM refreshes the access token once", async () => {
  const fcm = realSender([{ status: 401, body: fcmError("UNAUTHENTICATED", []) }, { status: 200 }]);
  const { deps } = fakeDeps({ deviceTokens: () => Promise.resolve(["tok-a"]), send: fcm.send });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(await response.json(), { sent: 1, deleted: 0, failed: 0 });
  assertEquals(fcm.authorizations, ["Bearer at-1", "Bearer at-2"]);
  assertEquals(fcm.invalidated, ["at-1"]);

  const stillRejected = realSender([{ status: 401 }, { status: 401 }, { status: 200 }]);
  const second = fakeDeps({ deviceTokens: () => Promise.resolve(["tok-a"]), send: stillRejected.send });
  assertEquals((await handlePush(request({ type: "INSERT", record }), second.deps)).status, 500);
  assertEquals(stillRejected.authorizations.length, 2, "refreshes only once");
});

Deno.test("a transient 503 is retried with backoff", async () => {
  const fcm = realSender([{ status: 503 }, { status: 200 }]);
  const { deps } = fakeDeps({ deviceTokens: () => Promise.resolve(["tok-a"]), send: fcm.send });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(await response.json(), { sent: 1, deleted: 0, failed: 0 });
  assertEquals(fcm.authorizations.length, 2);
  assertEquals(fcm.sleeps, [250]);
});

Deno.test("transient errors stop after two retries and honor Retry-After", async () => {
  const fcm = realSender([
    { status: 429, headers: { "Retry-After": "1" } },
    { status: 500 },
    { status: 503 },
    { status: 200 },
  ]);
  const { deps } = fakeDeps({ deviceTokens: () => Promise.resolve(["tok-a"]), send: fcm.send });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(await response.json(), { sent: 0, deleted: 0, failed: 1 });
  assertEquals(fcm.authorizations.length, 3);
  assertEquals(fcm.sleeps, [1000, 500]);
});
