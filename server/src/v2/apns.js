// Mitteilungen für iPhones direkt über Apples Dienst (APNs), ohne Firebase.
// Geheim im Worker (npx wrangler secret put): APNS_KEY (Inhalt der .p8-Datei), APNS_KEY_ID, APNS_TEAM_ID.
// Als Variable in wrangler.toml: APNS_TOPIC (die Bundle-ID der App).

import { b64urlKodiere } from "./krypto.js";

const KODIERER = new TextEncoder();
const UNGUELTIG = ["BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic"];

function pemZuBytes(pem) {
  const roh = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(roh), (c) => c.charCodeAt(0));
}

let jwtCache = { wert: null, bis: 0, schluessel: "" };

export function leereApnsCache() { jwtCache = { wert: null, bis: 0, schluessel: "" }; }

/** Das Anmeldeblatt (JWT, ES256). Apple will es nicht öfter als alle 20 Minuten neu und mindestens stündlich erneuert. */
export async function apnsAnmeldeblatt(env, jetzt = Date.now()) {
  const kennung = env.APNS_KEY_ID + "|" + env.APNS_TEAM_ID;
  if (jwtCache.wert && jwtCache.schluessel === kennung && jwtCache.bis > jetzt) return jwtCache.wert;
  const kopf = b64urlKodiere(JSON.stringify({ alg: "ES256", kid: env.APNS_KEY_ID }));
  const inhalt = b64urlKodiere(JSON.stringify({ iss: env.APNS_TEAM_ID, iat: Math.floor(jetzt / 1000) }));
  const schluessel = await crypto.subtle.importKey(
    "pkcs8", pemZuBytes(env.APNS_KEY.trim()), { name: "ECDSA", namedCurve: "P-256" }, false, ["sign"],
  );
  // WebCrypto liefert die Unterschrift schon als rohes r||s, so wie JWT es will.
  const sig = await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, schluessel, KODIERER.encode(kopf + "." + inhalt));
  const wert = kopf + "." + inhalt + "." + b64urlKodiere(sig);
  jwtCache = { wert, bis: jetzt + 40 * 60000, schluessel: kennung };
  return wert;
}

export function apnsKoerper(titel, text, typ) {
  return { aps: { alert: { title: titel, body: text }, sound: "default" }, typ };
}

/** Schickt eine Mitteilung. ungueltig = true, wenn Apple die Kennung nicht mehr kennt. */
export async function apnsSenden(env, ziel, titel, text, typ, holen = fetch, jetzt = Date.now()) {
  if (!env.APNS_KEY || !env.APNS_KEY_ID || !env.APNS_TEAM_ID) return { ok: false, ungueltig: false, status: 0, grund: "APNs nicht eingerichtet" };
  const host = ziel.umgebung === "sandbox" ? "api.sandbox.push.apple.com" : "api.push.apple.com";
  const antwort = await holen(`https://${host}/3/device/${ziel.token}`, {
    method: "POST",
    headers: {
      authorization: "bearer " + (await apnsAnmeldeblatt(env, jetzt)),
      "apns-topic": env.APNS_TOPIC || "de.cemaras.yem1n",
      "apns-push-type": "alert",
      "apns-priority": "10",
      "apns-expiration": String(Math.floor(jetzt / 1000) + 24 * 3600),
      "content-type": "application/json",
    },
    body: JSON.stringify(apnsKoerper(titel, text, typ)),
  });
  if (antwort.status === 200) return { ok: true, ungueltig: false, status: 200 };
  let grund = "";
  try { grund = (await antwort.json()).reason || ""; } catch { /* keine Begründung */ }
  if (grund === "ExpiredProviderToken" || grund === "InvalidProviderToken") leereApnsCache();
  return { ok: false, ungueltig: antwort.status === 410 || UNGUELTIG.includes(grund), status: antwort.status, grund };
}
