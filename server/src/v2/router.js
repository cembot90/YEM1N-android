// Die Eingangstür für Protokoll 2: prüft Adresse und Größe, sucht die Familie und reicht die Anfrage weiter.
// Alles Weitere (Unterschrift, Rechte, Daten) passiert in der Familie selbst (familie.js).

import { neueBremse } from "../limit.js";

const proAdresse = neueBremse(240, 60000);
const neueFamilien = neueBremse(5, 60 * 60000);
const FAMILIEN_ID = /^[0-9a-f]{64}$/;

function antwort(status, obj) {
  return new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json; charset=utf-8" } });
}

/** Die Bremse je Adresse: erst die von Cloudflare (falls eingerichtet), dazu eine einfache im Speicher. */
async function adresseErlaubt(env, ip, binding, lokal) {
  if (!lokal(ip)) return false;
  if (binding) {
    try { return (await binding.limit({ key: ip })).success !== false; } catch { return true; }
  }
  return true;
}

/**
 * Neue Familien liegen immer in der EU. Nur für Tests auf dem eigenen Rechner (die lokale Laufzeit kennt keine
 * Jurisdiktionen) kann man mit der Variable FAMILIE_JURISDIKTION = "keine" darauf verzichten. In der echten
 * Umgebung ist die Variable nicht gesetzt, und ein Fehler hier lässt das Anlegen scheitern, statt still ohne EU weiterzumachen.
 */
function namensraum(env) {
  return env.FAMILIE_JURISDIKTION === "keine" ? env.FAMILIE : env.FAMILIE.jurisdiction("eu");
}

export async function v2Behandle(request, env) {
  const url = new URL(request.url);
  if (url.pathname === "/v2/ping") return antwort(200, { ok: true, protokoll: 2 });
  if (request.method !== "POST") return antwort(405, { fehler: "Nur POST" });
  if (!env.FAMILIE) return antwort(503, { fehler: "Der Dienst ist noch nicht eingerichtet" });

  const ip = request.headers.get("cf-connecting-ip") || "unbekannt";
  if (!(await adresseErlaubt(env, ip, env.RATE_IP, proAdresse))) return antwort(429, { fehler: "Zu viele Anfragen, bitte kurz warten" });

  const roh = await request.text();
  if (roh.length > 120000) return antwort(413, { fehler: "Zu groß" });

  let id, familie;
  if (url.pathname === "/v2/familie") {
    if (!(await adresseErlaubt(env, ip, env.RATE_NEU, neueFamilien))) return antwort(429, { fehler: "Zu viele neue Familien von dieser Adresse" });
    id = namensraum(env).newUniqueId();
    familie = id.toString();
  } else {
    familie = request.headers.get("Y-Familie") || "";
    if (!FAMILIEN_ID.test(familie)) return antwort(400, { fehler: "Familie ungültig" });
    try { id = env.FAMILIE.idFromString(familie); } catch { return antwort(400, { fehler: "Familie ungültig" }); }
  }

  // Nur die Kopfzeilen weitergeben, die das Protokoll kennt.
  const kopf = new Headers({ "Content-Type": "application/json" });
  for (const n of ["Y-Familie", "Y-Geraet", "Y-Zeit", "Y-Signatur"]) {
    const w = request.headers.get(n);
    if (w) kopf.set(n, w);
  }
  kopf.set("Y-Familie", familie);
  try {
    const stub = env.FAMILIE.get(id);
    return await stub.fetch(new Request("https://familie.intern" + url.pathname, { method: "POST", headers: kopf, body: roh }));
  } catch (e) {
    console.log("v2:", url.pathname, String(e));
    return antwort(502, { fehler: "Der Dienst ist gerade nicht erreichbar" });
  }
}
