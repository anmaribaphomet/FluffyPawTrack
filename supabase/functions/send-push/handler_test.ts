import { assertEquals } from "jsr:@std/assert@1";
import { handlePush, type PushDeps } from "./handler.ts";

const record = { id: "n1", user_id: "u1", title: "Nuevo reporte cercano", body: "Herido", report_id: "r1" };

function request(body: unknown, secret: string | null = "s3cret"): Request {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (secret !== null) headers.set("x-webhook-secret", secret);
  return new Request("http://localhost/send-push", { method: "POST", headers, body: JSON.stringify(body) });
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
      "tok-a": { status: 404, body: { error: { status: "NOT_FOUND", details: [{ errorCode: "UNREGISTERED" }] } } },
      "tok-b": { status: 503, body: { error: { status: "UNAVAILABLE", details: [{ errorCode: "UNAVAILABLE" }] } } },
    },
  });
  const response = await handlePush(request({ type: "INSERT", record }), deps);
  assertEquals(await response.json(), { sent: 0, deleted: 1, failed: 1 });
  assertEquals(deleted, ["tok-a"]);
});

Deno.test("ignores events that are not notification inserts", async () => {
  const { deps, sent } = fakeDeps();
  assertEquals((await handlePush(request({ type: "UPDATE", record }), deps)).status, 200);
  assertEquals((await handlePush(request({ type: "INSERT", table: "reports", record }), deps)).status, 200);
  assertEquals(sent.length, 0);
});
