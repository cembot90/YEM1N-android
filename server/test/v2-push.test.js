import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, createVerify } from "node:crypto";
import { apnsAnmeldeblatt, apnsKoerper, apnsSenden, leereApnsCache } from "../src/v2/apns.js";
import { pushVersand } from "../src/v2/push.js";
import { leereCache } from "../src/fcm.js";

function apnsUmgebung() {
  const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  return {
    publicKey,
    env: {
      APNS_KEY: privateKey.export({ type: "pkcs8", format: "pem" }),
      APNS_KEY_ID: "ABC123DEFG", APNS_TEAM_ID: "TEAM123456", APNS_TOPIC: "de.cemaras.yem1n",
    },
  };
}
const ZIEL = { token: "a".repeat(64), umgebung: "production" };

test("APNs: Anmeldeblatt ist ein gültiges ES256-JWT mit Schlüssel- und Team-Kennung", async () => {
  leereApnsCache();
  const { env, publicKey } = apnsUmgebung();
  const jwt = await apnsAnmeldeblatt(env, 1_800_000_000_000);
  const [kopf, inh, sig] = jwt.split(".");
  assert.deepEqual(JSON.parse(Buffer.from(kopf, "base64url")), { alg: "ES256", kid: "ABC123DEFG" });
  assert.deepEqual(JSON.parse(Buffer.from(inh, "base64url")), { iss: "TEAM123456", iat: 1_800_000_000 });
  assert.equal(Buffer.from(sig, "base64url").length, 64); // rohes r||s
  const v = createVerify("SHA256");
  v.update(kopf + "." + inh);
  assert.ok(v.verify({ key: publicKey, dsaEncoding: "ieee-p1363" }, Buffer.from(sig, "base64url")));
});

test("APNs: das Anmeldeblatt wird 40 Minuten wiederverwendet, danach neu gemacht", async () => {
  leereApnsCache();
  const { env } = apnsUmgebung();
  const a = await apnsAnmeldeblatt(env, 1_800_000_000_000);
  assert.equal(await apnsAnmeldeblatt(env, 1_800_000_000_000 + 39 * 60000), a);
  assert.notEqual(await apnsAnmeldeblatt(env, 1_800_000_000_000 + 41 * 60000), a);
});

test("APNs: Mitteilung geht an den richtigen Server mit den richtigen Kopfzeilen", async () => {
  leereApnsCache();
  const { env } = apnsUmgebung();
  const aufrufe = [];
  const holen = async (url, opt) => { aufrufe.push({ url, opt }); return new Response("", { status: 200 }); };
  const e = await apnsSenden(env, ZIEL, "YEM1N", "Neue Nachricht", "frage", holen, 1_800_000_000_000);
  assert.deepEqual(e, { ok: true, ungueltig: false, status: 200 });
  assert.equal(aufrufe[0].url, `https://api.push.apple.com/3/device/${"a".repeat(64)}`);
  const k = aufrufe[0].opt.headers;
  assert.match(k.authorization, /^bearer [\w-]+\.[\w-]+\.[\w-]+$/);
  assert.equal(k["apns-topic"], "de.cemaras.yem1n");
  assert.equal(k["apns-push-type"], "alert");
  assert.equal(k["apns-expiration"], String(1_800_000_000 + 24 * 3600));
  assert.deepEqual(JSON.parse(aufrufe[0].opt.body), apnsKoerper("YEM1N", "Neue Nachricht", "frage"));
  await apnsSenden(env, { ...ZIEL, umgebung: "sandbox" }, "t", "x", "frage", holen);
  assert.equal(aufrufe[1].url.split("/")[2], "api.sandbox.push.apple.com");
});

test("APNs: nur bei unbekannter Kennung wird sie als ungültig gemeldet", async () => {
  leereApnsCache();
  const { env } = apnsUmgebung();
  const antwort = (status, reason) => async () => new Response(JSON.stringify({ reason }), { status });
  assert.equal((await apnsSenden(env, ZIEL, "t", "x", "frage", antwort(410, "Unregistered"))).ungueltig, true);
  assert.equal((await apnsSenden(env, ZIEL, "t", "x", "frage", antwort(400, "BadDeviceToken"))).ungueltig, true);
  assert.equal((await apnsSenden(env, ZIEL, "t", "x", "frage", antwort(400, "DeviceTokenNotForTopic"))).ungueltig, true);
  const kurz = await apnsSenden(env, ZIEL, "t", "x", "frage", antwort(429, "TooManyRequests"));
  assert.deepEqual([kurz.ok, kurz.ungueltig, kurz.status], [false, false, 429]);
  assert.equal((await apnsSenden(env, ZIEL, "t", "x", "frage", antwort(503, "ServiceUnavailable"))).ungueltig, false);
});

test("APNs: ein abgelehntes Anmeldeblatt wird nicht weiterverwendet", async () => {
  leereApnsCache();
  const { env } = apnsUmgebung();
  const jetzt = 1_800_000_000_000;
  const a = await apnsAnmeldeblatt(env, jetzt);
  await apnsSenden(env, ZIEL, "t", "x", "frage", async () => new Response(JSON.stringify({ reason: "ExpiredProviderToken" }), { status: 403 }), jetzt + 1000);
  assert.notEqual(await apnsAnmeldeblatt(env, jetzt + 2000), a);
});

test("APNs: ohne Einrichtung wird nichts geschickt und nichts abgerufen", async () => {
  let aufgerufen = false;
  const e = await apnsSenden({}, ZIEL, "t", "x", "frage", async () => { aufgerufen = true; return new Response(""); });
  assert.equal(aufgerufen, false);
  assert.equal(e.ok, false);
  assert.equal(e.ungueltig, false);
});

test("Versand: FCM und APNs werden getrennt bedient, Fehler eines Ziels stören die anderen nicht", async () => {
  leereApnsCache(); leereCache();
  const { env } = apnsUmgebung();
  const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  env.FCM_DIENSTKONTO = JSON.stringify({
    client_email: "d@yem1n.iam.gserviceaccount.com", project_id: "yem1n-test",
    private_key: privateKey.export({ type: "pkcs8", format: "pem" }),
  });
  const urls = [];
  const holen = async (url, opt) => {
    urls.push(url);
    if (url.includes("oauth2")) return new Response(JSON.stringify({ access_token: "T", expires_in: 3600 }));
    if (url.includes("fcm.googleapis.com")) return new Response("UNREGISTERED", { status: 404 });
    if (url.includes("/3/device/" + "b".repeat(64))) throw new Error("Netz weg");
    return new Response("", { status: 200 });
  };
  const senden = pushVersand(env, holen);
  const r = await senden({
    ziele: [
      { id: "1", art: "fcm", token: "fcm-token-aaaaaaaaaaaaaaaaaaaa" },
      { id: "2", art: "apns", token: "a".repeat(64), umgebung: "sandbox" },
      { id: "3", art: "apns", token: "b".repeat(64), umgebung: "production" },
      { id: "4", art: "unbekannt", token: "x" },
    ],
    titel: "YEM1N", text: "Neue Nachricht", typ: "frage",
  });
  assert.equal(r[0].ungueltig, true);
  assert.equal(r[1].ok, true);
  assert.equal(r[2].ok, false);
  assert.equal(r[3].ok, false);
  assert.ok(urls.some((u) => u.includes("api.sandbox.push.apple.com")));
});

test("Versand: ohne Dienstkonto für FCM wird nichts abgerufen", async () => {
  let n = 0;
  const senden = pushVersand({}, async () => { n += 1; return new Response(""); });
  const r = await senden({ ziele: [{ id: "1", art: "fcm", token: "fcm-token-aaaaaaaaaaaaaaaaaaaa" }], titel: "t", text: "x", typ: "frage" });
  assert.equal(r[0].ok, false);
  assert.equal(n, 0);
});
