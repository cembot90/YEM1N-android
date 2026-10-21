// Mitteilungen über Googles FCM (HTTP v1). Firebase dient nur als Postbote, es gibt keine Datenbank dort.
// Das Dienstkonto (JSON) liegt als Geheimnis FCM_DIENSTKONTO im Worker.

const BEREICH = "https://www.googleapis.com/auth/firebase.messaging";
const KODIERER = new TextEncoder();

function b64url(daten) {
  const bytes = typeof daten === "string" ? KODIERER.encode(daten) : new Uint8Array(daten);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemZuBytes(pem) {
  const roh = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(roh), (c) => c.charCodeAt(0));
}

export async function rsaSchluessel(pem) {
  return crypto.subtle.importKey(
    "pkcs8", pemZuBytes(pem), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
}

/** Das signierte Anmeldeblatt (JWT) für Google. Gültig 1 Stunde. */
export async function anmeldeblatt(konto, jetzt = Date.now()) {
  const sek = Math.floor(jetzt / 1000);
  const kopf = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const inhalt = b64url(JSON.stringify({
    iss: konto.client_email, scope: BEREICH, aud: "https://oauth2.googleapis.com/token", iat: sek, exp: sek + 3600,
  }));
  const schluessel = await rsaSchluessel(konto.private_key);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", schluessel, KODIERER.encode(kopf + "." + inhalt));
  return kopf + "." + inhalt + "." + b64url(sig);
}

let tokenCache = { wert: null, bis: 0 };

export async function zugriffstoken(konto, holen = fetch, jetzt = Date.now()) {
  if (tokenCache.wert && tokenCache.bis > jetzt + 60000) return tokenCache.wert;
  const antwort = await holen("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: "grant_type=" + encodeURIComponent("urn:ietf:params:oauth:grant-type:jwt-bearer") +
      "&assertion=" + await anmeldeblatt(konto, jetzt),
  });
  const a = await antwort.json();
  if (!a.access_token) throw new Error("Google-Anmeldung fehlgeschlagen: " + (a.error_description || a.error || antwort.status));
  tokenCache = { wert: a.access_token, bis: jetzt + (a.expires_in || 3600) * 1000 };
  return tokenCache.wert;
}

export function leereCache() { tokenCache = { wert: null, bis: 0 }; }

/** Der Inhalt einer Mitteilung für FCM. */
export function nachrichtKoerper(token, titel, text, art) {
  return {
    message: {
      token,
      notification: { title: titel, body: text },
      data: { art: art || "" },
      android: { priority: "HIGH", notification: { channel_id: "mitteilungen" } },
    },
  };
}

/** Schickt eine Mitteilung. ungueltig = true, wenn Google die Kennung nicht mehr kennt (App gelöscht). */
export async function pushSenden(konto, token, titel, text, art, holen = fetch, jetzt = Date.now()) {
  const zt = await zugriffstoken(konto, holen, jetzt);
  const antwort = await holen(`https://fcm.googleapis.com/v1/projects/${konto.project_id}/messages:send`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: "Bearer " + zt },
    body: JSON.stringify(nachrichtKoerper(token, titel, text, art)),
  });
  if (antwort.status === 200) return { ok: true, ungueltig: false };
  const t = await antwort.text();
  const ungueltig = antwort.status === 404 || /UNREGISTERED|INVALID_ARGUMENT/.test(t);
  return { ok: false, ungueltig, status: antwort.status };
}
