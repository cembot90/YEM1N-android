import { anfrage, schluesselLaden } from "./cloudkit.js";
import { pruefeGeraet, pruefeSenden } from "./regeln.js";
import { loeschBefehl, paketBefehl, pruefeKlasse, schluesselAbfrage, schluesselAusAntwort, schluesselBefehl, unterschriftGilt } from "./klasse.js";
import { neueBremse } from "./limit.js";
import { TYPEN_LOESCHEN, abgelaufene, istAufraeumzeit, loeschKoerper } from "./loeschfrist.js";
import { pushSenden } from "./fcm.js";
import { TYPEN, absenderGeraet, abfrageKoerper, inhalt, neueEintraege, typenFuer } from "./mitteilungen.js";
import { v2Behandle } from "./v2/router.js";

const proGeraet = neueBremse(30, 60000);
const proCode = neueBremse(120, 60000);
// Geräteanmeldungen sind selten (nur bei neuer Kennung). Wenige pro Minute und Adresse reichen und erschweren das Füllen der Familienliste.
const proAnmeldung = neueBremse(5, 60000);
let schluesselCache = null;

function antwort(status, obj) {
  return new Response(JSON.stringify(obj), {
    status, headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

async function schluessel(env) {
  if (!schluesselCache) schluesselCache = await schluesselLaden(env.CK_PRIVATE_KEY.trim());
  return schluesselCache;
}

/** Fehlerdetails von CloudKit gehören ins Protokoll des Servers, nicht an die App (kein Einblick in Aufbau und Schlüssel). */
function ablehnung(ort, status, f) {
  console.log("CloudKit lehnt ab:", ort, status, f && f.code ? f.code : "", f && f.grund ? String(f.grund).slice(0, 200) : "");
  return antwort(502, { fehler: "Der Eintrag wurde nicht angenommen" });
}

/** Sucht in der Antwort von records/modify nach einem abgelehnten Eintrag. */
export function ersterFehler(text) {
  try {
    const a = JSON.parse(text);
    for (const r of a.records || []) {
      if (r.serverErrorCode) return { code: r.serverErrorCode, grund: r.reason };
    }
  } catch { /* nicht lesbar */ }
  return null;
}

export async function behandle(request, env, holen = fetch) {
  const url = new URL(request.url);
  if (url.pathname.startsWith("/v2/")) return v2Behandle(request, env);
  if (url.pathname === "/v1/ping") return antwort(200, { ok: true });
  if (request.method !== "POST") return antwort(405, { fehler: "Nur POST" });

  const ip = request.headers.get("cf-connecting-ip") || "unbekannt";
  if (!proGeraet(ip)) return antwort(429, { fehler: "Zu viele Anfragen, bitte kurz warten" });

  const roh = await request.text();
  if (roh.length > 40000) return antwort(413, { fehler: "Zu groß" });
  let body;
  try { body = JSON.parse(roh); } catch { return antwort(400, { fehler: "Kein JSON" }); }

  if (url.pathname === "/v1/senden") {
    const p = pruefeSenden(body);
    if (p.fehler) return antwort(400, { fehler: p.fehler });
    if (!proCode(p.code)) return antwort(429, { fehler: "Zu viele Anfragen für diese Familie" });
    const r = await anfrage(env, await schluessel(env), "records/modify", JSON.stringify(p.operation), holen);
    if (r.status === 200) {
      // CloudKit antwortet auch dann mit 200, wenn ein einzelner Eintrag abgelehnt wurde. Das steht in der Antwort.
      const f = ersterFehler(r.text);
      // "Gibt es schon" (feste Namen bei Daumen und Ergebnis) gilt als geschafft, wie in der iOS-App.
      if (!f || f.code === "CONFLICT") return antwort(200, { ok: true });
      return ablehnung("senden", 200, f);
    }
    if (r.status === 409) return antwort(200, { ok: true });
    let grund = null;
    try {
      const a = JSON.parse(r.text);
      grund = { code: a.serverErrorCode, grund: a.reason };
    } catch { /* Antwort war kein JSON */ }
    return ablehnung("senden", r.status, grund);
  }

  if (url.pathname === "/v1/klasse") {
    const k = pruefeKlasse(body);
    if (k.fehler) return antwort(400, { fehler: k.fehler });
    if (!proCode(k.kcode)) return antwort(429, { fehler: "Zu viele Anfragen für diese Klasse" });
    const key = await schluessel(env);
    const lookup = await anfrage(env, key, "records/lookup", schluesselAbfrage(k.kcode), holen);
    const vorhanden = lookup.status === 200 ? schluesselAusAntwort(lookup.text) : null;

    if (k.art === "schluessel") {
      // Ein vorhandener Schlüssel wird nie überschrieben. Derselbe Schlüssel noch einmal gilt als geschafft.
      if (vorhanden) return vorhanden === k.publicKey ? antwort(200, { ok: true }) : antwort(409, { fehler: "Diesen Klassencode gibt es schon" });
      const r = await anfrage(env, key, "records/modify", schluesselBefehl(k.kcode, k.publicKey), holen);
      const f = r.status === 200 ? ersterFehler(r.text) : { code: String(r.status) };
      return f ? ablehnung("klasse-schluessel", r.status, f) : antwort(200, { ok: true });
    }

    if (!vorhanden) return antwort(403, { fehler: "Für diesen Klassencode gibt es keinen Schlüssel" });
    const text = k.art === "paket" ? k.kcode + "|" + k.json : k.kcode + "|loeschen|" + k.name;
    if (!(await unterschriftGilt(vorhanden, text, k.sig))) return antwort(403, { fehler: "Unterschrift passt nicht" });
    const befehl = k.art === "paket" ? paketBefehl(k) : loeschBefehl(k.name);
    const r = await anfrage(env, key, "records/modify", befehl, holen);
    const f = r.status === 200 ? ersterFehler(r.text) : { code: String(r.status) };
    return f ? ablehnung("klasse-paket", r.status, f) : antwort(200, { ok: true });
  }

  if (url.pathname === "/v1/geraet") {
    if (!proAnmeldung(ip)) return antwort(429, { fehler: "Zu viele Anfragen, bitte kurz warten" });
    const g = pruefeGeraet(body);
    if (g.fehler) return antwort(400, { fehler: g.fehler });
    const key = "geraete:" + g.code;
    const alt = JSON.parse((await env.GERAETE.get(key)) || "[]");
    const neu = alt.filter((x) => x.token !== g.token);
    neu.push({ rolle: g.rolle, token: g.token, geraet: g.geraet || "", seit: Date.now() });
    await env.GERAETE.put(key, JSON.stringify(neu.slice(-10)));
    await familieMerken(env, g.code);
    return antwort(200, { ok: true });
  }

  return antwort(404, { fehler: "Unbekannt" });
}

const MAX_FAMILIEN = 5;
// Der Stand "bis wohin Mitteilungen verschickt sind" läuft nach 90 Tagen ohne Neues ab. Danach wird er einfach neu gemerkt.
const STAND_ABLAUF = { expirationTtl: 90 * 24 * 3600 };
const FAMILIEN_KEY = "familien";

/** Merkt sich den Familiencode in einer eigenen Liste, damit der Minutenlauf nie "list" braucht (Gratis-Grenze 1000 pro Tag). */
export async function familieMerken(env, code) {
  const alt = JSON.parse((await env.GERAETE.get(FAMILIEN_KEY)) || "[]");
  if (alt.includes(code)) return;
  alt.push(code);
  await env.GERAETE.put(FAMILIEN_KEY, JSON.stringify(alt.slice(-MAX_FAMILIEN)));
}

/** Codes der Familien. Gibt es die Liste noch nicht, wird einmal aus den Geräteeinträgen aufgebaut. */
export async function familienCodes(env) {
  const roh = await env.GERAETE.get(FAMILIEN_KEY);
  if (roh) return JSON.parse(roh).slice(-MAX_FAMILIEN);
  const liste = await env.GERAETE.list({ prefix: "geraete:", limit: MAX_FAMILIEN });
  const codes = liste.keys.map((k) => k.name.slice("geraete:".length));
  await env.GERAETE.put(FAMILIEN_KEY, JSON.stringify(codes));
  return codes;
}

/**
 * Läuft jede Minute: schaut pro Familie nach neuen Einträgen und schickt Mitteilungen.
 * Beim ersten Mal wird nur der Stand gemerkt, damit alte Einträge keine Mitteilung auslösen.
 * Zurück: Anzahl der verschickten Mitteilungen.
 */
export async function pruefeNeues(env, holen = fetch, jetzt = Date.now()) {
  if (!env.FCM_DIENSTKONTO) { console.log("pruefeNeues: FCM_DIENSTKONTO fehlt"); return 0; }
  const konto = JSON.parse(env.FCM_DIENSTKONTO);
  console.log("pruefeNeues: Lauf");
  let verschickt = 0;
  for (const code of await familienCodes(env)) {
    const geraeteKey = "geraete:" + code;
    let geraete = JSON.parse((await env.GERAETE.get(geraeteKey)) || "[]");
    const vorher = geraete.length;
    for (const typ of typenFuer(geraete)) {
      const standKey = `stand:${code}:${typ}`;
      const stand = Number(await env.GERAETE.get(standKey));
      const r = await anfrage(env, await schluessel(env), "records/query", abfrageKoerper(typ, code), holen);
      if (r.status !== 200) { console.log("pruefeNeues: Abfrage", typ, "Status", r.status, r.text.slice(0, 200)); continue; }
      if (!stand) { await env.GERAETE.put(standKey, String(jetzt), STAND_ABLAUF); continue; }
      const neu = neueEintraege(r.text, stand);
      if (!neu.length) continue;
      await env.GERAETE.put(standKey, String(neu[neu.length - 1].created.timestamp), STAND_ABLAUF);
      for (const eintrag of neu.slice(-3)) {
        const text = inhalt(typ, eintrag.fields);
        if (!text) continue;
        for (const g of geraete.filter((x) => TYPEN[typ] === "alle" || x.rolle === TYPEN[typ])) {
          if (g.geraet && absenderGeraet(typ, eintrag.fields) === g.geraet) continue;
          const e = await pushSenden(konto, g.token, text.titel, text.text, typ, holen, jetzt);
          console.log("pruefeNeues: Push", typ, g.rolle, e.ok ? "ok" : "Fehler " + e.status);
          if (e.ok) verschickt++;
          if (e.ungueltig) geraete = geraete.filter((x) => x.token !== g.token);
        }
      }
    }
    if (geraete.length !== vorher) await env.GERAETE.put(geraeteKey, JSON.stringify(geraete));
  }
  return verschickt;
}

/** Löscht abgelaufene Einträge aller bekannten Familien. Zurück: Anzahl der Löschaufträge. */
export async function raeumeAuf(env, holen = fetch, jetzt = Date.now()) {
  let geloescht = 0;
  for (const code of await familienCodes(env)) {
    for (const typ of TYPEN_LOESCHEN) {
      const r = await anfrage(env, await schluessel(env), "records/query", abfrageKoerper(typ, code, 200), holen);
      if (r.status !== 200) continue;
      const namen = abgelaufene(r.text, jetzt);
      if (!namen.length) continue;
      const e = await anfrage(env, await schluessel(env), "records/modify", loeschKoerper(namen), holen);
      console.log("raeumeAuf:", typ, namen.length, "Status", e.status);
      if (e.status === 200) geloescht += namen.length;
    }
  }
  return geloescht;
}

export default {
  fetch: (request, env) => behandle(request, env),
  scheduled: (event, env, ctx) => {
    ctx.waitUntil(pruefeNeues(env).catch((e) => console.log("pruefeNeues", String(e))));
    if (istAufraeumzeit(event.scheduledTime)) ctx.waitUntil(raeumeAuf(env).catch((e) => console.log("raeumeAuf", String(e))));
  },
};
