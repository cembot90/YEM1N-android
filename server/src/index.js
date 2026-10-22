import { anfrage, schluesselLaden } from "./cloudkit.js";
import { pruefeGeraet, pruefeSenden } from "./regeln.js";
import { neueBremse } from "./limit.js";
import { pushSenden } from "./fcm.js";
import { TYPEN, abfrageKoerper, inhalt, neueEintraege, typenFuer } from "./mitteilungen.js";

const proGeraet = neueBremse(30, 60000);
const proCode = neueBremse(120, 60000);
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
      return antwort(502, { fehler: "CloudKit hat den Eintrag abgelehnt", status: 200, code: f.code, grund: f.grund });
    }
    if (r.status === 409) return antwort(200, { ok: true });
    let grund = {};
    try {
      const a = JSON.parse(r.text);
      grund = { code: a.serverErrorCode, grund: a.reason };
    } catch { /* Antwort war kein JSON */ }
    return antwort(502, { fehler: "CloudKit hat abgelehnt", status: r.status, ...grund });
  }

  if (url.pathname === "/v1/geraet") {
    const g = pruefeGeraet(body);
    if (g.fehler) return antwort(400, { fehler: g.fehler });
    const key = "geraete:" + g.code;
    const alt = JSON.parse((await env.GERAETE.get(key)) || "[]");
    const neu = alt.filter((x) => x.token !== g.token);
    neu.push({ rolle: g.rolle, token: g.token, seit: Date.now() });
    await env.GERAETE.put(key, JSON.stringify(neu.slice(-10)));
    return antwort(200, { ok: true });
  }

  return antwort(404, { fehler: "Unbekannt" });
}

const MAX_FAMILIEN = 5;

/**
 * Läuft jede Minute: schaut pro Familie nach neuen Einträgen und schickt Mitteilungen.
 * Beim ersten Mal wird nur der Stand gemerkt, damit alte Einträge keine Mitteilung auslösen.
 * Zurück: Anzahl der verschickten Mitteilungen.
 */
export async function pruefeNeues(env, holen = fetch, jetzt = Date.now()) {
  if (!env.FCM_DIENSTKONTO) { console.log("pruefeNeues: FCM_DIENSTKONTO fehlt"); return 0; }
  const konto = JSON.parse(env.FCM_DIENSTKONTO);
  console.log("pruefeNeues: Lauf");
  const liste = await env.GERAETE.list({ prefix: "geraete:", limit: MAX_FAMILIEN });
  let verschickt = 0;
  for (const k of liste.keys) {
    const code = k.name.slice("geraete:".length);
    let geraete = JSON.parse((await env.GERAETE.get(k.name)) || "[]");
    const vorher = geraete.length;
    for (const typ of typenFuer(geraete)) {
      const standKey = `stand:${code}:${typ}`;
      const stand = Number(await env.GERAETE.get(standKey));
      const r = await anfrage(env, await schluessel(env), "records/query", abfrageKoerper(typ, code), holen);
      if (r.status !== 200) { console.log("pruefeNeues: Abfrage", typ, "Status", r.status, r.text.slice(0, 200)); continue; }
      if (!stand) { await env.GERAETE.put(standKey, String(jetzt)); continue; }
      const neu = neueEintraege(r.text, stand);
      if (!neu.length) continue;
      await env.GERAETE.put(standKey, String(neu[neu.length - 1].created.timestamp));
      for (const eintrag of neu.slice(-3)) {
        const text = inhalt(typ, eintrag.fields);
        if (!text) continue;
        for (const g of geraete.filter((x) => x.rolle === TYPEN[typ])) {
          const e = await pushSenden(konto, g.token, text.titel, text.text, typ, holen, jetzt);
          console.log("pruefeNeues: Push", typ, g.rolle, e.ok ? "ok" : "Fehler " + e.status);
          if (e.ok) verschickt++;
          if (e.ungueltig) geraete = geraete.filter((x) => x.token !== g.token);
        }
      }
    }
    if (geraete.length !== vorher) await env.GERAETE.put(k.name, JSON.stringify(geraete));
  }
  return verschickt;
}

export default {
  fetch: (request, env) => behandle(request, env),
  scheduled: (event, env, ctx) => ctx.waitUntil(pruefeNeues(env).catch((e) => console.log("pruefeNeues", String(e)))),
};
