import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, createVerify } from "node:crypto";
import { derAusRoh, nachricht, schluesselLaden, signiere, datumText, pfad, anfrage } from "../src/cloudkit.js";
import { pruefeCode, pruefeGeraet, pruefeSenden } from "../src/regeln.js";
import { neueBremse } from "../src/limit.js";
import { behandle } from "../src/index.js";

const CODE = "ABCDE23456";

test("Code wird normalisiert und geprüft", () => {
  assert.equal(pruefeCode("abcde-23456"), CODE);
  assert.equal(pruefeCode("ABCDE-2345O"), null);
  assert.equal(pruefeCode(5), null);
});

test("Frage wird zu einem CloudKit-Befehl", () => {
  const p = pruefeSenden({ code: CODE, art: "frage", daten: { kind: "Yemin", fach: "Mathe", uebung: "Geteilt", text: "Hilfe" } });
  const r = p.operation.operations[0].record;
  assert.equal(r.recordType, "JokerAnfrage");
  assert.equal(r.fields.status.value, "offen");
  assert.equal(r.fields.familienCode.value, CODE);
});

test("Daumen und Ergebnis haben feste Namen", () => {
  const d = pruefeSenden({ code: CODE, art: "daumen", daten: { frageId: "a", antwortId: "w", absender: "Mama" } });
  assert.equal(d.operation.operations[0].record.recordName, "daumen-w");
  const e = pruefeSenden({ code: CODE, art: "ergebnis", daten: {
    eintragId: "x", klasse: "Klasse 3", fach: "Mathe", arbeit: "Nr. 1", uebung: "Geteilt", richtig: 8, gesamt: 10,
    angesehen: 0, sterne: 2, kind: "Yemin", zeitpunkt: 1000 } });
  assert.equal(e.operation.operations[0].record.recordName, "erg-x");
  assert.ok(!("details" in e.operation.operations[0].record.fields));
});

test("Ungültiges wird abgelehnt", () => {
  assert.ok(pruefeSenden({ code: "X", art: "frage", daten: {} }).fehler);
  assert.ok(pruefeSenden({ code: CODE, art: "loeschen", daten: {} }).fehler);
  assert.ok(pruefeSenden({ code: CODE, art: "frage", daten: { kind: "Y", text: "x".repeat(501) } }).fehler);
  assert.ok(pruefeSenden({ code: CODE, art: "ergebnis", daten: { eintragId: "x", richtig: "8" } }).fehler);
  assert.ok(pruefeSenden(null).fehler);
});

test("Gerät melden", () => {
  assert.deepEqual(pruefeGeraet({ code: CODE, rolle: "eltern", token: "abc" }), { code: CODE, rolle: "eltern", token: "abc" });
  assert.ok(pruefeGeraet({ code: CODE, rolle: "admin", token: "abc" }).fehler);
  assert.ok(pruefeGeraet({ code: CODE, rolle: "kind" }).fehler);
});

test("Bremse", () => {
  const b = neueBremse(2, 1000);
  assert.ok(b("a", 0)); assert.ok(b("a", 10)); assert.ok(!b("a", 20));
  assert.ok(b("b", 20)); assert.ok(b("a", 2000));
});

test("Datum ohne Millisekunden", () => {
  assert.equal(datumText(new Date("2026-10-21T20:00:00.123Z")), "2026-10-21T20:00:00Z");
});

test("Signatur ist gültiges DER und passt zum öffentlichen Schlüssel", async () => {
  const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  const pem = privateKey.export({ type: "pkcs8", format: "pem" });
  const k = await schluesselLaden(pem);
  const text = await nachricht("2026-10-21T20:00:00Z", "{}", pfad("iCloud.x", "development", "records/modify"));
  for (let i = 0; i < 20; i++) {
    const sig = await signiere(k, text + i);
    const v = createVerify("SHA256").update(text + i);
    assert.ok(v.verify(publicKey, Buffer.from(sig, "base64")));
  }
});

test("DER mit führendem Bit", () => {
  const roh = new Uint8Array(64).fill(0xff);
  const der = derAusRoh(roh);
  assert.equal(der[0], 0x30);
  assert.equal(der[2], 0x02);
  assert.equal(der[3], 33);
});

async function umgebung() {
  const { privateKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  const speicher = new Map();
  return {
    CK_CONTAINER: "iCloud.x", CK_UMGEBUNG: "development", CK_KEY_ID: "kid",
    CK_PRIVATE_KEY: privateKey.export({ type: "pkcs8", format: "pem" }),
    GERAETE: { get: async (k) => speicher.get(k) ?? null, put: async (k, v) => { speicher.set(k, v); } },
  };
}

const post = (pfadText, body, ip = "1.1.1.1") => new Request("https://x.test" + pfadText, {
  method: "POST", headers: { "cf-connecting-ip": ip }, body: JSON.stringify(body),
});

test("Senden: signierte Anfrage geht an CloudKit", async () => {
  const env = await umgebung();
  let gesehen;
  const holen = async (url, opt) => { gesehen = { url, opt }; return new Response("{}", { status: 200 }); };
  const a = await behandle(post("/v1/senden", { code: CODE, art: "frage", daten: { kind: "Yemin", text: "Hilfe" } }, "2.2.2.2"), env, holen);
  assert.equal(a.status, 200);
  assert.ok(gesehen.url.endsWith("/database/1/iCloud.x/development/public/records/modify"));
  assert.equal(gesehen.opt.headers["X-Apple-CloudKit-Request-KeyID"], "kid");
  assert.ok(gesehen.opt.headers["X-Apple-CloudKit-Request-SignatureV1"].length > 50);
});

test("Senden: ungültige Anfrage erreicht CloudKit nicht", async () => {
  const env = await umgebung();
  let aufgerufen = false;
  const holen = async () => { aufgerufen = true; return new Response("{}"); };
  const a = await behandle(post("/v1/senden", { code: "nope", art: "frage", daten: {} }, "3.3.3.3"), env, holen);
  assert.equal(a.status, 400);
  assert.equal(aufgerufen, false);
});

test("Senden: Konflikt (gibt es schon) gilt als geschafft, Fehler als 502", async () => {
  const env = await umgebung();
  const q = { code: CODE, art: "daumen", daten: { frageId: "a", antwortId: "w", absender: "M" } };
  assert.equal((await behandle(post("/v1/senden", q, "4.4.4.4"), env, async () => new Response("{}", { status: 409 }))).status, 200);
  assert.equal((await behandle(post("/v1/senden", q, "4.4.4.4"), env, async () => new Response("{}", { status: 500 }))).status, 502);
});

test("Gerät wird gespeichert, ohne Doppelte", async () => {
  const env = await umgebung();
  const g = { code: CODE, rolle: "eltern", token: "t1" };
  await behandle(post("/v1/geraet", g, "5.5.5.5"), env);
  await behandle(post("/v1/geraet", g, "5.5.5.5"), env);
  assert.equal(JSON.parse(await env.GERAETE.get("geraete:" + CODE)).length, 1);
});

test("Zu viele Anfragen von einer Adresse werden gebremst", async () => {
  const env = await umgebung();
  let letzter;
  for (let i = 0; i < 31; i++) letzter = await behandle(post("/v1/geraet", { code: "x" }, "9.9.9.9"), env);
  assert.equal(letzter.status, 429);
});

test("Senden: Apples Fehlergrund wird weitergegeben", async () => {
  const env = await umgebung();
  const q = { code: CODE, art: "frage", daten: { kind: "Y", text: "x" } };
  const a = await behandle(post("/v1/senden", q, "6.6.6.6"), env,
    async () => new Response('{"serverErrorCode":"AUTHENTICATION_FAILED","reason":"Signature"}', { status: 401 }));
  const j = await a.json();
  assert.equal(a.status, 502);
  assert.equal(j.code, "AUTHENTICATION_FAILED");
  assert.equal(j.grund, "Signature");
});

test("Senden: abgelehnter Eintrag trotz 200 wird gemeldet", async () => {
  const env = await umgebung();
  const q = { code: CODE, art: "frage", daten: { kind: "Y", text: "x" } };
  const a = await behandle(post("/v1/senden", q, "7.7.7.7"), env,
    async () => new Response('{"records":[{"serverErrorCode":"ACCESS_DENIED","reason":"CREATE operation not permitted"}]}', { status: 200 }));
  const j = await a.json();
  assert.equal(a.status, 502);
  assert.equal(j.code, "ACCESS_DENIED");
});

test("Senden: Konflikt im Eintrag gilt als geschafft", async () => {
  const env = await umgebung();
  const q = { code: CODE, art: "daumen", daten: { frageId: "a", antwortId: "w", absender: "M" } };
  const a = await behandle(post("/v1/senden", q, "8.8.8.8"), env,
    async () => new Response('{"records":[{"serverErrorCode":"CONFLICT","reason":"exists"}]}', { status: 200 }));
  assert.equal(a.status, 200);
});

test("Senden: normaler Erfolg bleibt Erfolg", async () => {
  const env = await umgebung();
  const q = { code: CODE, art: "frage", daten: { kind: "Y", text: "x" } };
  const a = await behandle(post("/v1/senden", q, "8.8.4.4"), env,
    async () => new Response('{"records":[{"recordName":"abc","recordType":"JokerAnfrage"}]}', { status: 200 }));
  assert.equal(a.status, 200);
});
