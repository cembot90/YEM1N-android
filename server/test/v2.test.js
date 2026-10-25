import test from "node:test";
import assert from "node:assert/strict";
import { behandle } from "../src/index.js";
import { b64urlDekodiere, b64urlKodiere, geraetId, pubBytes, sha256Hex, unterschriftGilt, unterschriftText } from "../src/v2/krypto.js";
import { TYPEN } from "../src/v2/typen.js";
import { falscheFamilien, neuesGeraet, neueIp, offeneAnfrage, uhr } from "./v2-hilfen.js";

const MIN = 60 * 1000;
const TAG = 24 * 60 * MIN;

/** Eine Familie mit einem Eltern-Gerät, fertig angelegt. */
async function start(optionen = {}) {
  uhr.t = 1_800_000_000_000;
  const ns = falscheFamilien(optionen);
  const env = { FAMILIE: ns };
  const eltern = await neuesGeraet("Mama");
  const r = await behandle(offeneAnfrage("/v2/familie", { pub: eltern.pub, name: "Mama" }), env);
  const a = await r.json();
  assert.equal(r.status, 200, JSON.stringify(a));
  const senden = async (g, pfad, body, opt) => {
    const r = await behandle(await g.anfrage(pfad, body, a.familie, opt), env);
    return { status: r.status, j: await r.json() };
  };
  /** Ein weiteres Gerät koppelt sich und wird (wenn [erlauben]) von den Eltern freigegeben. */
  const dazu = async (name, rolle, erlauben = true) => {
    const g = await neuesGeraet(name);
    const e = await senden(eltern, "/v2/einladung", { rolle });
    assert.equal(e.status, 200);
    const k = await behandle(offeneAnfrage("/v2/koppeln", { token: e.j.token, pub: g.pub, name }, { "Y-Familie": a.familie }), env);
    assert.equal(k.status, 200, JSON.stringify(await k.clone().json()));
    if (erlauben) assert.equal((await senden(eltern, "/v2/geraet/erlauben", { geraet: g.id })).status, 200);
    return g;
  };
  return { ns, env, eltern, familie: a.familie, senden, dazu, kern: ns.kernFuer(a.familie).kern, ablage: ns.kernFuer(a.familie).ablage };
}

// ---------- Krypto ----------

test("base64url: Hin und zurück, Fehler werden erkannt", () => {
  const b = new Uint8Array([0, 250, 251, 252, 253, 254, 255, 1, 2]);
  assert.deepEqual([...b64urlDekodiere(b64urlKodiere(b))], [...b]);
  assert.equal(b64urlDekodiere("ab+/"), null);
  assert.equal(b64urlDekodiere("a"), null);
  assert.equal(b64urlKodiere(new Uint8Array([251, 255])), "-_8");
});

test("Gerätekennung kommt nur aus einem echten öffentlichen Schlüssel", async () => {
  const g = await neuesGeraet("A");
  assert.equal(g.id.length, 24);
  assert.equal(g.id, (await sha256Hex(pubBytes(g.pub))).slice(0, 24));
  assert.equal(await geraetId("abc"), null);
  assert.equal(await geraetId(b64urlKodiere(new Uint8Array(65))), null); // beginnt nicht mit 4
});

test("Unterschrift gilt nur für genau diesen Text und diesen Schlüssel", async () => {
  const g = await neuesGeraet("A"), h = await neuesGeraet("B");
  const text = unterschriftText({ methode: "POST", pfad: "/v2/senden", familie: "f", geraet: g.id, zeit: 1, bodyHash: "00" });
  const sig = b64urlKodiere(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, g.paar.privateKey, new TextEncoder().encode(text)));
  assert.equal(await unterschriftGilt(g.pub, sig, text), true);
  assert.equal(await unterschriftGilt(g.pub, sig, text + "x"), false);
  assert.equal(await unterschriftGilt(h.pub, sig, text), false);
  assert.equal(await unterschriftGilt(g.pub, "kurz", text), false);
  assert.equal(await unterschriftGilt("kaputt", sig, text), false);
});

test("Der Text zur Unterschrift hat festes Aussehen (gleiche Vorlage in Swift und Kotlin)", () => {
  assert.equal(
    unterschriftText({ methode: "POST", pfad: "/v2/lesen", familie: "ab", geraet: "cd", zeit: 1800000000000, bodyHash: "ef" }),
    "YEM1N-V2\nPOST\n/v2/lesen\nab\ncd\n1800000000000\nef",
  );
});

// ---------- Anlegen und Router ----------

test("Eine Familie anlegen: eigene Kennung, Jurisdiktion EU, erste Eltern sind sofort aktiv", async () => {
  const f = await start();
  assert.match(f.familie, /^[0-9a-f]{64}$/);
  assert.deepEqual(f.ns.protokoll.jurisdiktionen, ["eu"]);
  const s = await f.senden(f.eltern, "/v2/status", {});
  assert.equal(s.j.status, "aktiv");
  assert.equal(s.j.rolle, "eltern");
  assert.equal(s.j.jurisdiktion, "eu");
});

test("Ohne ausdrückliche Ausnahme legt der Dienst nie eine Familie außerhalb der EU an", async () => {
  const ns = falscheFamilien();
  ns.jurisdiction = () => { throw new Error("Jurisdiction restrictions are not implemented"); };
  const g = await neuesGeraet("A");
  const r = await behandle(offeneAnfrage("/v2/familie", { pub: g.pub, name: "A" }), { FAMILIE: ns }).catch(() => ({ status: 500 }));
  assert.equal(r.status, 500);
  assert.equal(ns.familien.size, 0);
  // Die Ausnahme für lokale Tests muss ausdrücklich gesetzt sein
  const ns2 = falscheFamilien();
  const r2 = await behandle(offeneAnfrage("/v2/familie", { pub: g.pub, name: "A" }), { FAMILIE: ns2, FAMILIE_JURISDIKTION: "keine" });
  assert.equal(r2.status, 200);
  assert.deepEqual(ns2.protokoll.jurisdiktionen, []);
});

test("Ping und falsche Wege", async () => {
  const env = { FAMILIE: falscheFamilien() };
  assert.equal((await behandle(new Request("https://x.test/v2/ping"), env)).status, 200);
  assert.equal((await behandle(new Request("https://x.test/v2/senden"), env)).status, 405);
  assert.equal((await behandle(offeneAnfrage("/v2/senden", {}, { "Y-Familie": "zzz" }), env)).status, 400);
  assert.equal((await behandle(offeneAnfrage("/v2/senden", {}), env)).status, 400);
  assert.equal((await behandle(offeneAnfrage("/v2/senden", {}, { "Y-Familie": "a".repeat(64) }), {})).status, 503);
});

test("Eine unbekannte Familie ist 404 und legt nichts an", async () => {
  const ns = falscheFamilien();
  const env = { FAMILIE: ns };
  const g = await neuesGeraet("A");
  const fam = "b".repeat(64);
  const r = await behandle(await g.anfrage("/v2/status", {}, fam), env);
  assert.equal(r.status, 404);
  const tabellen = ns.kernFuer(fam).kern.abfrage("SELECT name FROM sqlite_master WHERE type = 'table'");
  assert.equal(tabellen.length, 0);
});

test("Neue Familien von einer Adresse werden gebremst (Bremse von Cloudflare)", async () => {
  const env = { FAMILIE: falscheFamilien(), RATE_NEU: { limit: async () => ({ success: false }) } };
  const g = await neuesGeraet("A");
  const r = await behandle(offeneAnfrage("/v2/familie", { pub: g.pub, name: "A" }), env);
  assert.equal(r.status, 429);
});

test("Anlegen ohne gültigen Schlüssel oder Namen geht nicht", async () => {
  const env = { FAMILIE: falscheFamilien() };
  assert.equal((await behandle(offeneAnfrage("/v2/familie", { pub: "x", name: "A" }), env)).status, 400);
  const g = await neuesGeraet("A");
  assert.equal((await behandle(offeneAnfrage("/v2/familie", { pub: g.pub, name: "  " }), env)).status, 400);
  assert.equal((await behandle(offeneAnfrage("/v2/familie", { pub: g.pub, name: "x".repeat(41) }), env)).status, 400);
});

// ---------- Unterschrift, Zeit, Wiederholung ----------

test("Falsche Unterschrift, falsche Uhr, unbekanntes Gerät, Wiederholung", async () => {
  const f = await start();
  assert.equal((await f.senden(f.eltern, "/v2/status", {}, { kaputt: true })).status, 401);
  assert.equal((await f.senden(f.eltern, "/v2/status", {}, { zeit: uhr.t - 6 * MIN })).status, 401);
  assert.equal((await f.senden(f.eltern, "/v2/status", {}, { zeit: uhr.t + 6 * MIN })).status, 401);
  const fremd = await neuesGeraet("Fremd");
  assert.equal((await f.senden(fremd, "/v2/status", {})).status, 401);
  // Dieselbe Anfrage zweimal: die zweite wird abgewiesen.
  const r = await f.eltern.anfrage("/v2/status", {}, f.familie);
  assert.equal((await behandle(r.clone(), f.env)).status, 200);
  assert.equal((await behandle(r.clone(), f.env)).status, 401);
});

test("Die Unterschrift gilt nur für den Weg und den Inhalt, für die sie gemacht wurde", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  const echt = await kind.anfrage("/v2/senden", { typ: "frage", daten: { id: "frage-0001", kind: "Yemin", text: "Hilfe" } }, f.familie);
  // Anderen Inhalt unter dieselbe Unterschrift legen
  const kopf = Object.fromEntries(echt.headers);
  const gefaelscht = new Request("https://x.test/v2/senden", { method: "POST", headers: kopf, body: JSON.stringify({ typ: "frage", daten: { id: "frage-0001", kind: "Yemin", text: "Ganz anders" } }) });
  assert.equal((await behandle(gefaelscht, f.env)).status, 401);
  // Anderen Weg unter dieselbe Unterschrift legen
  const anderer = new Request("https://x.test/v2/lesen", { method: "POST", headers: kopf, body: await echt.clone().text() });
  assert.equal((await behandle(anderer, f.env)).status, 401);
});

test("Eine Anfrage für eine andere Familie wird abgewiesen", async () => {
  const a = await start();
  const b = await start();
  const r = await behandle(await a.eltern.anfrage("/v2/status", {}, b.familie), b.env);
  assert.equal(r.status, 401);
});

// ---------- Koppeln ----------

test("Koppeln: Einladung gilt einmal, 10 Minuten, die Rolle bestimmt die Einladung", async () => {
  const f = await start();
  const e = await f.senden(f.eltern, "/v2/einladung", { rolle: "kind" });
  const g = await neuesGeraet("Yemin");
  const k = await behandle(offeneAnfrage("/v2/koppeln", { token: e.j.token, pub: g.pub, name: "Yemin", rolle: "eltern" }, { "Y-Familie": f.familie }), f.env);
  const kj = await k.json();
  assert.equal(kj.status, "wartet");
  assert.equal(kj.rolle, "kind"); // "eltern" im Body wird ignoriert
  // Zweites Gerät mit demselben Schlüssel geht nicht mehr
  const h = await neuesGeraet("Evin");
  const k2 = await behandle(offeneAnfrage("/v2/koppeln", { token: e.j.token, pub: h.pub, name: "Evin" }, { "Y-Familie": f.familie }), f.env);
  assert.equal(k2.status, 403);
  // Abgelaufen
  const e2 = await f.senden(f.eltern, "/v2/einladung", { rolle: "kind" });
  uhr.t += 11 * MIN;
  const k3 = await behandle(offeneAnfrage("/v2/koppeln", { token: e2.j.token, pub: h.pub, name: "Evin" }, { "Y-Familie": f.familie }), f.env);
  assert.equal(k3.status, 403);
});

test("Koppeln: erfundener Schlüssel, nur Eltern laden ein, höchstens 5 offene Einladungen", async () => {
  const f = await start();
  const g = await neuesGeraet("X");
  assert.equal((await behandle(offeneAnfrage("/v2/koppeln", { token: "erfunden", pub: g.pub, name: "X" }, { "Y-Familie": f.familie }), f.env)).status, 403);
  const kind = await f.dazu("Yemin", "kind");
  assert.equal((await f.senden(kind, "/v2/einladung", { rolle: "kind" })).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/einladung", { rolle: "admin" })).status, 400);
  for (let i = 0; i < 5; i++) assert.equal((await f.senden(f.eltern, "/v2/einladung", { rolle: "kind" })).status, 200);
  assert.equal((await f.senden(f.eltern, "/v2/einladung", { rolle: "kind" })).status, 429);
});

test("Ein wartendes Gerät darf nur nach seinem Stand fragen", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind", false);
  const s = await f.senden(kind, "/v2/status", {});
  assert.equal(s.j.status, "wartet");
  const v = await f.senden(kind, "/v2/senden", { typ: "frage", daten: { id: "frage-0001", kind: "Y", text: "x" } });
  assert.equal(v.status, 403);
  assert.equal((await f.senden(kind, "/v2/lesen", { typ: "antwort" })).status, 403);
  // Nur ein Eltern-Gerät kann erlauben, das wartende Gerät selbst nicht
  assert.equal((await f.senden(kind, "/v2/geraet/erlauben", { geraet: kind.id })).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/geraet/erlauben", { geraet: kind.id })).status, 200);
  assert.equal((await f.senden(kind, "/v2/status", {})).j.status, "aktiv");
});

test("Ablehnen und Entfernen", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind", false);
  assert.equal((await f.senden(f.eltern, "/v2/geraet/ablehnen", { geraet: kind.id })).status, 200);
  assert.equal((await f.senden(kind, "/v2/status", {})).j.status, "abgelehnt");
  assert.equal((await f.senden(kind, "/v2/senden", { typ: "frage", daten: {} })).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/geraet/erlauben", { geraet: kind.id })).status, 409);
  const zweites = await f.dazu("Evin", "kind");
  assert.equal((await f.senden(f.eltern, "/v2/geraet/entfernen", { geraet: zweites.id })).status, 200);
  assert.equal((await f.senden(zweites, "/v2/status", {})).status, 401);
  assert.equal((await f.senden(f.eltern, "/v2/geraet/entfernen", { geraet: f.eltern.id })).status, 400);
  const liste = await f.senden(f.eltern, "/v2/geraete", {});
  assert.deepEqual(liste.j.geraete.map((x) => [x.name, x.status]), [["Mama", "aktiv"], ["Yemin", "abgelehnt"]]);
  assert.equal(liste.j.geraete[0].ich, true);
  assert.ok(!JSON.stringify(liste.j).includes("pub"));
});

test("Grenzen: höchstens 3 wartende und 12 Geräte", async () => {
  const f = await start();
  for (let i = 0; i < 3; i++) await f.dazu("K" + i, "kind", false);
  const e = await f.senden(f.eltern, "/v2/einladung", { rolle: "kind" });
  const g = await neuesGeraet("Viertes");
  assert.equal((await behandle(offeneAnfrage("/v2/koppeln", { token: e.j.token, pub: g.pub, name: "V" }, { "Y-Familie": f.familie }), f.env)).status, 429);
  const f2 = await start();
  for (let i = 0; i < 11; i++) await f2.dazu("K" + i, "kind", true);
  const e2 = await f2.senden(f2.eltern, "/v2/einladung", { rolle: "kind" });
  assert.equal((await behandle(offeneAnfrage("/v2/koppeln", { token: e2.j.token, pub: g.pub, name: "V" }, { "Y-Familie": f2.familie }), f2.env)).status, 409);
});

// ---------- Einträge und Rechte ----------

const FRAGE = { typ: "frage", daten: { id: "frage-0001", kind: "Yemin", fach: "Mathe", uebung: "Geteilt", text: "Was ist 56 : 7?" } };

test("Rechte: Kinder senden Fragen, Eltern antworten, Rollen sind getrennt", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  assert.equal((await f.senden(kind, "/v2/senden", FRAGE)).j.neu, true);
  assert.equal((await f.senden(f.eltern, "/v2/senden", FRAGE)).status, 403);
  const antwort = { typ: "antwort", daten: { id: "antw-0001", frageId: "frage-0001", text: "8", emoji: "😀" } };
  assert.equal((await f.senden(kind, "/v2/senden", antwort)).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/senden", antwort)).status, 200);
  assert.equal((await f.senden(kind, "/v2/senden", { typ: "freigabe", daten: { id: "freigabe-01" } })).status, 403);
  assert.equal((await f.senden(kind, "/v2/senden", { typ: "paket", daten: { id: "paket-0001", json: "{}" } })).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "gibtesnicht", daten: {} })).status, 400);
});

test("Rechte beim Lesen: Kinder sehen keine Ergebnisse und nur ihre eigenen Fragen", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  const schwester = await f.dazu("Evin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  await f.senden(schwester, "/v2/senden", { typ: "frage", daten: { ...FRAGE.daten, id: "frage-0002", kind: "Evin" } });
  const erg = { typ: "ergebnis", daten: { id: "erg-0001", kind: "Yemin", klasse: "Klasse 3", fach: "Mathe", arbeit: "Nr. 1", uebung: "Geteilt", richtig: 8, gesamt: 10, angesehen: 0, sterne: 2, zeitpunkt: uhr.t } };
  assert.equal((await f.senden(kind, "/v2/senden", erg)).status, 200);
  assert.equal((await f.senden(f.eltern, "/v2/lesen", { typ: "ergebnis" })).j.eintraege.length, 1);
  assert.equal((await f.senden(kind, "/v2/lesen", { typ: "ergebnis" })).status, 403);
  assert.equal((await f.senden(schwester, "/v2/lesen", { typ: "ergebnis" })).status, 403);
  assert.deepEqual((await f.senden(kind, "/v2/lesen", { typ: "frage" })).j.eintraege.map((e) => e.id), ["frage-0001"]);
  assert.deepEqual((await f.senden(f.eltern, "/v2/lesen", { typ: "frage" })).j.eintraege.map((e) => e.id), ["frage-0001", "frage-0002"]);
});

test("Den Absendernamen setzt der Server, nicht das Gerät", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  await f.senden(f.eltern, "/v2/senden", { typ: "antwort", daten: { id: "antw-0001", frageId: "frage-0001", text: "8", absender: "Papa" } });
  const a = (await f.senden(kind, "/v2/lesen", { typ: "antwort" })).j.eintraege[0];
  assert.equal(a.absender, "Mama");
  assert.equal(a.von, f.eltern.id);
  assert.equal(a.daten.absender, undefined);
});

test("Doppelt gesendet: der erste Eintrag bleibt, die Antwort sagt neu: false", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  const a = await f.senden(kind, "/v2/senden", FRAGE);
  const b = await f.senden(kind, "/v2/senden", { typ: "frage", daten: { ...FRAGE.daten, text: "Anderer Text" } });
  assert.equal(b.j.neu, false);
  assert.equal(b.j.nr, a.j.nr);
  assert.equal((await f.senden(f.eltern, "/v2/lesen", { typ: "frage" })).j.eintraege[0].daten.text, "Was ist 56 : 7?");
});

test("Haustier und Paket werden überschrieben, Duell-Antworten gibt es je Gerät einmal", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", { typ: "haustier", daten: { json: '{"stufe":1}' } });
  const z = await f.senden(kind, "/v2/senden", { typ: "haustier", daten: { json: '{"stufe":2}' } });
  assert.equal(z.j.neu, true);
  const h = (await f.senden(f.eltern, "/v2/lesen", { typ: "haustier" })).j.eintraege;
  assert.equal(h.length, 1);
  assert.equal(JSON.parse(h[0].daten.json).stufe, 2);
  await f.senden(f.eltern, "/v2/senden", { typ: "paket", daten: { id: "paket-0001", json: '{"v":1}' } });
  await f.senden(f.eltern, "/v2/senden", { typ: "paket", daten: { id: "paket-0001", json: '{"v":2}' } });
  assert.equal((await f.senden(kind, "/v2/lesen", { typ: "paket" })).j.eintraege.length, 1);
  const da = { typ: "duellantwort", daten: { duellId: "duell-0001", json: "{}" } };
  assert.equal((await f.senden(kind, "/v2/senden", da)).j.neu, true);
  assert.equal((await f.senden(kind, "/v2/senden", da)).j.neu, false);
  assert.equal((await f.senden(f.eltern, "/v2/senden", da)).j.neu, true);
});

test("Eingaben werden streng geprüft", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  const schlecht = [
    { typ: "frage", daten: { ...FRAGE.daten, id: "x" } },                      // Kennung zu kurz
    { typ: "frage", daten: { ...FRAGE.daten, id: "mit leerzeichen" } },
    { typ: "frage", daten: { ...FRAGE.daten, text: "" } },
    { typ: "frage", daten: { ...FRAGE.daten, text: "x".repeat(501) } },
    { typ: "frage", daten: { ...FRAGE.daten, text: "Steuer\u0001zeichen" } },
    { typ: "frage", daten: { ...FRAGE.daten, kind: 5 } },
    { typ: "frage", daten: "text" },
    { typ: "duell", daten: { id: "duell-0001", json: "kein json" } },
    { typ: "ergebnis", daten: { id: "erg-0001", richtig: "8" } },
    { typ: "daumen", daten: { frageId: "frage-0001" } },
  ];
  for (const s of schlecht) assert.equal((await f.senden(kind, "/v2/senden", s)).status, 400, JSON.stringify(s));
  assert.equal((await f.senden(kind, "/v2/lesen", { typ: "gibtesnicht" })).status, 400);
});

test("Pflege: nur ungefähr heute, je Tag und Art einmal", async () => {
  const f = await start();
  const heute = new Date(uhr.t).toISOString().slice(0, 10);
  const alt = new Date(uhr.t - 5 * TAG).toISOString().slice(0, 10);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "pflege", daten: { art: "leckerli", tag: heute } })).j.neu, true);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "pflege", daten: { art: "leckerli", tag: heute } })).j.neu, false);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "pflege", daten: { art: "spielen", tag: heute } })).j.neu, true);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "pflege", daten: { art: "leckerli", tag: alt } })).status, 400);
  assert.equal((await f.senden(f.eltern, "/v2/senden", { typ: "pflege", daten: { art: "gift", tag: heute } })).status, 400);
});

test("Lesen in Seiten: nr als Merker, mehr zeigt weitere Einträge an", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  for (let i = 1; i <= 5; i++) await f.senden(kind, "/v2/senden", { typ: "nachschub", daten: { id: "nach-000" + i, kind: "Yemin" } });
  const a = (await f.senden(f.eltern, "/v2/lesen", { typ: "nachschub", limit: 2 })).j;
  assert.equal(a.eintraege.length, 2);
  assert.equal(a.mehr, true);
  const b = (await f.senden(f.eltern, "/v2/lesen", { typ: "nachschub", seit: a.letzte, limit: 10 })).j;
  assert.deepEqual(b.eintraege.map((e) => e.id), ["nach-0003", "nach-0004", "nach-0005"]);
  assert.equal(b.mehr, false);
  assert.equal((await f.senden(f.eltern, "/v2/lesen", { typ: "nachschub", seit: b.letzte })).j.eintraege.length, 0);
});

test("Von jeder Art bleiben höchstens so viele Einträge, wie die Tabelle erlaubt", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  const max = TYPEN.nachschub.max;
  for (let i = 0; i < max + 3; i++) {
    const id = "nach-" + String(i).padStart(5, "0");
    if (i % 50 === 0) uhr.t += 2 * MIN; // die Bremse von 120 Anfragen pro Minute soll hier nicht stören
    assert.equal((await f.senden(kind, "/v2/senden", { typ: "nachschub", daten: { id, kind: "Yemin" } }, { ip: neueIp() })).status, 200);
  }
  uhr.t += 2 * MIN;
  const alle = (await f.senden(f.eltern, "/v2/lesen", { typ: "nachschub", limit: 200 })).j.eintraege;
  assert.equal(alle.length, 200);
  assert.equal(alle[0].id, "nach-00003");
});

test("Pro Gerät höchstens 120 Anfragen in der Minute", async () => {
  const f = await start();
  let letzter;
  for (let i = 0; i < 122; i++) letzter = await f.senden(f.eltern, "/v2/status", {});
  assert.equal(letzter.status, 429);
  uhr.t += 2 * MIN;
  assert.equal((await f.senden(f.eltern, "/v2/status", {})).status, 200);
});

// ---------- Austreten und Löschen ----------

test("Austreten: das letzte Eltern-Gerät kann nicht gehen, ein Kind nimmt auf Wunsch seine Einträge mit", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  assert.equal((await f.senden(f.eltern, "/v2/geraet/austreten", {})).status, 409);
  assert.equal((await f.senden(kind, "/v2/geraet/austreten", { eintraegeLoeschen: true })).status, 200);
  assert.equal((await f.senden(kind, "/v2/status", {})).status, 401);
  assert.equal((await f.senden(f.eltern, "/v2/lesen", { typ: "frage" })).j.eintraege.length, 0);
  const zweite = await f.dazu("Papa", "eltern");
  assert.equal((await f.senden(zweite, "/v2/geraet/austreten", {})).status, 200);
});

test("Familie löschen: nur Eltern, nur mit Bestätigung, danach ist alles weg", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  assert.equal((await f.senden(kind, "/v2/familie/loeschen", { bestaetigung: "LOESCHEN" })).status, 403);
  assert.equal((await f.senden(f.eltern, "/v2/familie/loeschen", {})).status, 400);
  assert.equal((await f.senden(f.eltern, "/v2/familie/loeschen", { bestaetigung: "LOESCHEN" })).status, 200);
  assert.equal(f.ablage.st.geloescht, 1);
  assert.equal((await f.senden(f.eltern, "/v2/status", {})).status, 404);
  assert.equal((await f.senden(kind, "/v2/lesen", { typ: "frage" })).status, 404);
});

// ---------- Fristen ----------

test("Eine leere Familie löscht sich nach 30 Tagen, eine benutzte nach 365 Tagen", async () => {
  const f = await start();
  assert.equal(f.ablage.st.alarm, uhr.t + 30 * TAG);
  uhr.t += 29 * TAG;
  await f.kern.alarm();
  assert.equal(f.ablage.st.geloescht, 0);
  uhr.t -= 29 * TAG;
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  assert.equal(Number(f.kern.meta("loeschen_am")), uhr.t + 365 * TAG);
  uhr.t += 364 * TAG;
  await f.kern.alarm();
  assert.equal(f.ablage.st.geloescht, 0);
  uhr.t += 2 * TAG;
  await f.kern.alarm();
  assert.equal(f.ablage.st.geloescht, 1);
});

test("Wer die Familie benutzt, verschiebt die Frist, aber nur einmal am Tag (spart Schreibvorgänge)", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", FRAGE);
  const erste = Number(f.kern.meta("loeschen_am"));
  uhr.t += 2 * MIN;
  await f.senden(kind, "/v2/senden", { typ: "nachschub", daten: { id: "nach-0001", kind: "Yemin" } });
  assert.equal(Number(f.kern.meta("loeschen_am")), erste);
  uhr.t += 2 * TAG;
  await f.senden(kind, "/v2/senden", { typ: "nachschub", daten: { id: "nach-0002", kind: "Yemin" } });
  assert.ok(Number(f.kern.meta("loeschen_am")) > erste);
});

test("Einträge laufen ab: Duelle nach 14 Tagen, der Alarm räumt auf", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await f.senden(kind, "/v2/senden", { typ: "duell", daten: { id: "duell-0001", json: "{}" } });
  await f.senden(kind, "/v2/senden", FRAGE);
  assert.equal(f.ablage.st.alarm <= uhr.t + 14 * TAG, true);
  uhr.t += 15 * TAG;
  assert.equal((await f.senden(f.eltern, "/v2/lesen", { typ: "duell" })).j.eintraege.length, 0);
  await f.kern.alarm();
  assert.equal(f.kern.eine("SELECT COUNT(*) AS n FROM eintraege WHERE typ = 'duell'").n, 0);
  assert.equal(f.kern.eine("SELECT COUNT(*) AS n FROM eintraege WHERE typ = 'frage'").n, 1);
});

test("Abgelehnte Geräte verschwinden nach einem Tag", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind", false);
  await f.senden(f.eltern, "/v2/geraet/ablehnen", { geraet: kind.id });
  uhr.t += 2 * TAG;
  await f.kern.alarm();
  assert.equal(f.kern.eine("SELECT COUNT(*) AS n FROM geraete").n, 1);
});

// ---------- Mitteilungen ----------

function pusherAufzeichnen() {
  const gesendet = [];
  const pusher = async ({ ziele, titel, text, typ }) => {
    gesendet.push({ an: ziele.map((z) => z.id), titel, text, typ, arten: ziele.map((z) => z.art) });
    return ziele.map((z) => ({ ok: true, ungueltig: z.token.startsWith("tot") }));
  };
  return { gesendet, pusher };
}

async function mitPush(g, f, art, token, umgebung) {
  const r = await f.senden(g, "/v2/push", { art, token, umgebung });
  assert.equal(r.status, 200, JSON.stringify(r.j));
}
const FCM_A = "fcm-token-aaaaaaaaaaaaaaaaaaaa", FCM_B = "fcm-token-bbbbbbbbbbbbbbbbbbbb";
const APNS_A = "a".repeat(64);

test("Mitteilungen sind neutral: kein Name, kein Text der Frage", async () => {
  const p = pusherAufzeichnen();
  const f = await start({ pusher: p.pusher });
  const kind = await f.dazu("Yemin", "kind");
  await mitPush(f.eltern, f, "apns", APNS_A, "sandbox");
  await mitPush(kind, f, "fcm", FCM_A);
  await f.senden(kind, "/v2/senden", FRAGE);
  const m = p.gesendet.at(-1);
  assert.deepEqual(m.an, [f.eltern.id]);
  assert.deepEqual(m.arten, ["apns"]);
  assert.equal(m.titel, "YEM1N");
  assert.equal(m.typ, "frage");
  const sichtbar = m.titel + " " + m.text; // die Gerätekennungen sind zufällige Hex-Zeichen und zählen nicht
  assert.ok(!sichtbar.includes("Yemin"));
  assert.ok(!sichtbar.includes("56"));
  assert.ok(!sichtbar.includes("Mathe"));
});

test("Mitteilungen gehen an die richtige Rolle und nie an den Absender", async () => {
  const p = pusherAufzeichnen();
  const f = await start({ pusher: p.pusher });
  const kind = await f.dazu("Yemin", "kind");
  const papa = await f.dazu("Papa", "eltern");
  await mitPush(f.eltern, f, "fcm", FCM_A);
  await mitPush(papa, f, "fcm", FCM_B);
  await mitPush(kind, f, "apns", APNS_A, "production");
  p.gesendet.length = 0;
  await f.senden(f.eltern, "/v2/senden", { typ: "antwort", daten: { id: "antw-0001", frageId: "frage-0001", text: "8" } });
  assert.deepEqual(p.gesendet.map((m) => m.an), [[kind.id]]);
  await f.senden(kind, "/v2/senden", { typ: "duell", daten: { id: "duell-0001", json: "{}" } });
  assert.deepEqual(p.gesendet.at(-1).an.sort(), [f.eltern.id, papa.id].sort());
  const vorher = p.gesendet.length;
  await f.senden(kind, "/v2/senden", { typ: "haustier", daten: { json: "{}" } });
  assert.equal(p.gesendet.length, vorher); // Haustier-Stand löst keine Mitteilung aus
});

test("Ein neues Gerät meldet sich bei den Eltern, ohne den Namen zu nennen", async () => {
  const p = pusherAufzeichnen();
  const f = await start({ pusher: p.pusher });
  await mitPush(f.eltern, f, "fcm", FCM_A);
  await f.dazu("Yemin", "kind", false);
  const m = p.gesendet.at(-1);
  assert.equal(m.typ, "geraet");
  assert.deepEqual(m.an, [f.eltern.id]);
  assert.ok(!(m.titel + " " + m.text).includes("Yemin"));
});

test("Eine ungültige Push-Kennung wird vergessen, derselbe Token gehört nur einem Gerät, ein Fehler im Versand stört nicht", async () => {
  const p = pusherAufzeichnen();
  const f = await start({ pusher: p.pusher });
  const kind = await f.dazu("Yemin", "kind");
  await mitPush(f.eltern, f, "fcm", "tot-token-aaaaaaaaaaaaaaaaaaaa");
  await f.senden(kind, "/v2/senden", FRAGE);
  assert.equal(f.kern.eine("SELECT push_token AS t FROM geraete WHERE id = ?", f.eltern.id).t, "");
  await mitPush(f.eltern, f, "fcm", FCM_A);
  await mitPush(kind, f, "fcm", FCM_A);
  assert.equal(f.kern.eine("SELECT push_token AS t FROM geraete WHERE id = ?", f.eltern.id).t, "");
  const kaputt = await start({ pusher: async () => { throw new Error("Apple nicht erreichbar"); } });
  const k2 = await kaputt.dazu("Yemin", "kind");
  await mitPush(kaputt.eltern, kaputt, "fcm", FCM_A);
  assert.equal((await kaputt.senden(k2, "/v2/senden", FRAGE)).status, 200);
});

test("Push-Kennung: nur bekannte Arten, saubere Kennungen, abmelden geht", async () => {
  const f = await start();
  assert.equal((await f.senden(f.eltern, "/v2/push", { art: "sms", token: "x" })).status, 400);
  assert.equal((await f.senden(f.eltern, "/v2/push", { art: "apns", token: "zzzz" })).status, 400);
  assert.equal((await f.senden(f.eltern, "/v2/push", { art: "fcm", token: "kurz" })).status, 400);
  assert.equal((await f.senden(f.eltern, "/v2/push", { art: "apns", token: APNS_A, umgebung: "sandbox" })).status, 200);
  assert.equal(f.kern.eine("SELECT push_umgebung AS u FROM geraete").u, "sandbox");
  assert.equal((await f.senden(f.eltern, "/v2/push", { art: "" })).status, 200);
  assert.equal(f.kern.eine("SELECT push_token AS t FROM geraete").t, "");
});

// ---------- Was nie nach außen gehen darf ----------

test("Antworten enthalten nie öffentliche Schlüssel oder Push-Kennungen anderer Geräte", async () => {
  const f = await start();
  const kind = await f.dazu("Yemin", "kind");
  await mitPush(kind, f, "fcm", FCM_A);
  const alles = [
    await f.senden(f.eltern, "/v2/geraete", {}),
    await f.senden(f.eltern, "/v2/status", {}),
    await f.senden(kind, "/v2/status", {}),
  ].map((r) => JSON.stringify(r.j)).join("");
  assert.ok(!alles.includes(kind.pub));
  assert.ok(!alles.includes(FCM_A));
  assert.ok(!alles.includes(f.eltern.pub));
});
