import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, createVerify } from "node:crypto";
import { anmeldeblatt, leereCache, nachrichtKoerper, pushSenden } from "../src/fcm.js";
import { abfrageKoerper, inhalt, neueEintraege, typenFuer } from "../src/mitteilungen.js";
import { pruefeNeues } from "../src/index.js";

const CODE = "ABCDE-23456";

function konto() {
  const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  return {
    konto: {
      client_email: "dienst@yem1n.iam.gserviceaccount.com", project_id: "yem1n-test",
      private_key: privateKey.export({ type: "pkcs8", format: "pem" }),
    },
    publicKey,
  };
}

function ecKonto() {
  const { privateKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  return privateKey.export({ type: "pkcs8", format: "pem" });
}

test("Anmeldeblatt ist ein gültig signiertes JWT", async () => {
  const { konto: k, publicKey } = konto();
  const jwt = await anmeldeblatt(k, 1_700_000_000_000);
  const [kopf, inh, sig] = jwt.split(".");
  assert.equal(JSON.parse(Buffer.from(kopf, "base64url")).alg, "RS256");
  const daten = JSON.parse(Buffer.from(inh, "base64url"));
  assert.equal(daten.iss, k.client_email);
  assert.equal(daten.exp - daten.iat, 3600);
  assert.ok(daten.scope.endsWith("firebase.messaging"));
  const v = createVerify("RSA-SHA256");
  v.update(kopf + "." + inh);
  assert.ok(v.verify(publicKey, Buffer.from(sig, "base64url")));
});

test("Push geht an FCM mit Zugriffstoken, ungültige Kennung wird erkannt", async () => {
  leereCache();
  const { konto: k } = konto();
  const aufrufe = [];
  const holen = async (url, opt) => {
    aufrufe.push({ url, opt });
    if (url.includes("oauth2")) return new Response(JSON.stringify({ access_token: "T", expires_in: 3600 }));
    if (JSON.parse(opt.body).message.token === "weg") return new Response("UNREGISTERED", { status: 404 });
    return new Response("{}", { status: 200 });
  };
  const gut = await pushSenden(k, "abc", "Titel", "Text", "JokerAntwort", holen);
  assert.deepEqual(gut, { ok: true, ungueltig: false });
  const fcm = aufrufe.find((a) => a.url.includes("fcm.googleapis.com"));
  assert.ok(fcm.url.includes("/projects/yem1n-test/"));
  assert.equal(fcm.opt.headers.Authorization, "Bearer T");
  const schlecht = await pushSenden(k, "weg", "T", "x", "", holen);
  assert.equal(schlecht.ungueltig, true);
  // Zugriffstoken wurde nur einmal geholt
  assert.equal(aufrufe.filter((a) => a.url.includes("oauth2")).length, 1);
  assert.equal(nachrichtKoerper("t", "a", "b", "c").message.android.notification.channel_id, "mitteilungen");
});

test("Inhalte der Mitteilungen", () => {
  const f = (o) => Object.fromEntries(Object.entries(o).map(([k, v]) => [k, { value: v }]));
  assert.match(inhalt("JokerAnfrage", f({ kind: "Yemin", text: "Hilfe bei 7 · 8" })).titel, /Yemin/);
  assert.equal(inhalt("RundenErgebnis", f({ kind: "Yemin", uebung: "Geteilt", richtig: 8, gesamt: 10 })).text, "Geteilt: 8 von 10 richtig");
  assert.equal(inhalt("JokerFreigabe", {}).titel, "🎁 Neue Joker!");
  assert.equal(inhalt("Unbekannt", {}), null);
  assert.ok(inhalt("JokerAnfrage", f({ kind: "A", text: "x".repeat(300) })).text.length <= 100);
});

test("Typen richten sich nach den Rollen der Geräte", () => {
  assert.deepEqual(typenFuer([{ rolle: "kind" }]).sort(), ["JokerAntwort", "JokerFreigabe"]);
  assert.equal(typenFuer([{ rolle: "kind" }, { rolle: "eltern" }]).length, 6);
});

test("Nur neuere Einträge zählen, älteste zuerst", () => {
  const text = JSON.stringify({ records: [
    { recordName: "b", created: { timestamp: 300 } },
    { recordName: "a", created: { timestamp: 100 } },
    { recordName: "c", created: { timestamp: 200 } },
    { recordName: "x", serverErrorCode: "NOT_FOUND" },
  ] });
  assert.deepEqual(neueEintraege(text, 150).map((r) => r.recordName), ["c", "b"]);
  assert.deepEqual(neueEintraege("kein json", 0), []);
  assert.equal(JSON.parse(abfrageKoerper("JokerAnfrage", CODE)).query.recordType, "JokerAnfrage");
});

function kv(start = {}) {
  const m = new Map(Object.entries(start));
  return {
    m,
    get: async (k) => (m.has(k) ? m.get(k) : null),
    put: async (k, v) => { m.set(k, v); },
    list: async ({ prefix }) => ({ keys: [...m.keys()].filter((k) => k.startsWith(prefix)).map((name) => ({ name })) }),
  };
}

test("Cron: erster Lauf merkt nur den Stand, zweiter schickt Mitteilung an die richtige Rolle", async () => {
  leereCache();
  const { konto: k } = konto();
  const geraete = [{ rolle: "eltern", token: "E1" }, { rolle: "kind", token: "K1" }];
  const speicher = kv({ ["geraete:" + CODE]: JSON.stringify(geraete) });
  const env = {
    GERAETE: speicher, FCM_DIENSTKONTO: JSON.stringify(k), CK_KEY_ID: "k", CK_PRIVATE_KEY: ecKonto(),
    CK_CONTAINER: "iCloud.test", CK_UMGEBUNG: "production",
  };
  let eintraege = [];
  const gesendet = [];
  const holen = async (url, opt) => {
    if (url.includes("oauth2")) return new Response(JSON.stringify({ access_token: "T", expires_in: 3600 }));
    if (url.includes("fcm.googleapis")) { gesendet.push(JSON.parse(opt.body).message); return new Response("{}"); }
    const typ = JSON.parse(opt.body).query.recordType;
    return new Response(JSON.stringify({ records: eintraege.filter((e) => e.recordType === typ) }));
  };
  assert.equal(await pruefeNeues(env, holen, 1000), 0);
  eintraege = [{
    recordType: "JokerAnfrage", recordName: "r1", created: { timestamp: 2000 },
    fields: { kind: { value: "Yemin" }, text: { value: "Hilfe" } },
  }];
  assert.equal(await pruefeNeues(env, holen, 3000), 1);
  assert.equal(gesendet[0].token, "E1");
  assert.match(gesendet[0].notification.title, /Yemin/);
  // Derselbe Eintrag löst nichts mehr aus
  assert.equal(await pruefeNeues(env, holen, 4000), 0);
});

test("Ohne Dienstkonto passiert nichts", async () => {
  assert.equal(await pruefeNeues({}, fetch), 0);
});
