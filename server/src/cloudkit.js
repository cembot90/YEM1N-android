// Anfragen an CloudKit mit dem Server-Schlüssel signieren (Server-to-Server).
// Gesendet wird nur an die öffentliche Datenbank.

const KODIERER = new TextEncoder();

export function base64(bytes) {
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s);
}

/** Datum wie Apple es verlangt: ISO 8601 ohne Millisekunden. */
export function datumText(jetzt = new Date()) {
  return jetzt.toISOString().replace(/\.\d{3}Z$/, "Z");
}

export async function koerperHash(text) {
  const h = await crypto.subtle.digest("SHA-256", KODIERER.encode(text));
  return base64(new Uint8Array(h));
}

/** Die Nachricht, die signiert wird: Datum:Hash:Pfad. */
export async function nachricht(datum, koerper, pfad) {
  return `${datum}:${await koerperHash(koerper)}:${pfad}`;
}

function pemZuBytes(pem) {
  const b64 = pem.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  const roh = atob(b64);
  return Uint8Array.from(roh, (c) => c.charCodeAt(0));
}

export async function schluesselLaden(pkcs8Pem) {
  return crypto.subtle.importKey(
    "pkcs8", pemZuBytes(pkcs8Pem), { name: "ECDSA", namedCurve: "P-256" }, false, ["sign"],
  );
}

function ganzzahlDer(bytes) {
  let i = 0;
  while (i < bytes.length - 1 && bytes[i] === 0) i++;
  let wert = Array.from(bytes.slice(i));
  if (wert[0] & 0x80) wert = [0, ...wert];
  return [0x02, wert.length, ...wert];
}

/** WebCrypto liefert r und s hintereinander (64 Byte), Apple erwartet das DER-Format wie bei OpenSSL. */
export function derAusRoh(roh) {
  const r = ganzzahlDer(roh.slice(0, 32));
  const s = ganzzahlDer(roh.slice(32, 64));
  return Uint8Array.from([0x30, r.length + s.length, ...r, ...s]);
}

export async function signiere(schluessel, text) {
  const roh = new Uint8Array(
    await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, schluessel, KODIERER.encode(text)),
  );
  return base64(derAusRoh(roh));
}

export function pfad(container, umgebung, operation) {
  return `/database/1/${container}/${umgebung}/public/${operation}`;
}

/** Schickt eine signierte Anfrage. Gibt { status, text } zurück. */
export async function anfrage(env, schluessel, operation, koerper, holen = fetch, jetzt = new Date()) {
  const p = pfad(env.CK_CONTAINER, env.CK_UMGEBUNG || "development", operation);
  const datum = datumText(jetzt);
  const sig = await signiere(schluessel, await nachricht(datum, koerper, p));
  const antwort = await holen("https://api.apple-cloudkit.com" + p, {
    method: "POST",
    headers: {
      "Content-Type": "text/plain",
      "X-Apple-CloudKit-Request-KeyID": String(env.CK_KEY_ID).trim(),
      "X-Apple-CloudKit-Request-ISO8601Date": datum,
      "X-Apple-CloudKit-Request-SignatureV1": sig,
    },
    body: koerper,
  });
  return { status: antwort.status, text: await antwort.text() };
}
