// Kleine Helfer für die Geräte-Unterschrift (Protokoll 2).
// Jedes Gerät hat ein Schlüsselpaar P-256. Der öffentliche Schlüssel ist der unkomprimierte Punkt (65 Bytes, beginnt mit 4),
// die Unterschrift ist das rohe Paar r||s (64 Bytes). Beides als base64url ohne Auffüllzeichen.

const KODIERER = new TextEncoder();

export function b64urlKodiere(daten) {
  const bytes = typeof daten === "string" ? KODIERER.encode(daten) : new Uint8Array(daten);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Gibt die Bytes zurück oder null, wenn der Text kein sauberes base64url ist. */
export function b64urlDekodiere(text) {
  if (typeof text !== "string" || !/^[A-Za-z0-9_-]*$/.test(text) || text.length % 4 === 1) return null;
  const b64 = text.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((text.length + 3) % 4);
  try {
    return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  } catch {
    return null;
  }
}

export function hex(bytes) {
  return [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export async function sha256Hex(daten) {
  const bytes = typeof daten === "string" ? KODIERER.encode(daten) : daten;
  return hex(await crypto.subtle.digest("SHA-256", bytes));
}

export function zufallB64(anzahlBytes = 16) {
  return b64urlKodiere(crypto.getRandomValues(new Uint8Array(anzahlBytes)));
}

/** Der öffentliche Schlüssel als Bytes, wenn er wie ein unkomprimierter P-256-Punkt aussieht, sonst null. */
export function pubBytes(text) {
  const b = b64urlDekodiere(text);
  return b && b.length === 65 && b[0] === 4 ? b : null;
}

/** Die Kennung eines Geräts: die ersten 24 Hex-Zeichen von SHA-256 über den öffentlichen Schlüssel. */
export async function geraetId(pubText) {
  const b = pubBytes(pubText);
  return b ? (await sha256Hex(b)).slice(0, 24) : null;
}

/** Der Text, den das Gerät unterschreibt. Jede Zeile ist ein Feld, Zeilen werden mit "\n" verbunden. */
export function unterschriftText({ methode, pfad, familie, geraet, zeit, bodyHash }) {
  return ["YEM1N-V2", methode, pfad, familie, geraet, String(zeit), bodyHash].join("\n");
}

/** Prüft die Unterschrift. Jeder Fehler (falsches Format, kein gültiger Punkt) zählt als "nicht gültig". */
export async function unterschriftGilt(pubText, sigText, text) {
  const pub = pubBytes(pubText);
  const sig = b64urlDekodiere(sigText);
  if (!pub || !sig || sig.length !== 64) return false;
  try {
    const schluessel = await crypto.subtle.importKey("raw", pub, { name: "ECDSA", namedCurve: "P-256" }, false, ["verify"]);
    return await crypto.subtle.verify({ name: "ECDSA", hash: "SHA-256" }, schluessel, sig, KODIERER.encode(text));
  } catch {
    return false;
  }
}
