import { anfrage, schluesselLaden } from "./cloudkit.js";
import { pruefeGeraet, pruefeSenden } from "./regeln.js";
import { neueBremse } from "./limit.js";

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
    // "Gibt es schon" (feste Namen bei Daumen und Ergebnis) gilt als geschafft, wie in der iOS-App.
    if (r.status === 200 || r.status === 409) return antwort(200, { ok: true });
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

export default {
  fetch: (request, env) => behandle(request, env),
};
