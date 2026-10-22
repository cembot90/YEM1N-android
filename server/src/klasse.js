// Klassencode als Admin: Schlüssel anlegen, Pakete unterschreiben und senden, löschen.
// Der Server schreibt nur, wenn die Unterschrift zum öffentlichen Schlüssel der Klasse passt (Ed25519, wie bei iOS).
// Unterschrieben wird der Text "K-XXXXX-XXXXX|<json>" und beim Löschen "K-XXXXX-XXXXX|loeschen|<Paketname>".

import { pruefeCode } from "./regeln.js";

const KODIERER = new TextEncoder();

export function cloudCode(code) {
  const c = pruefeCode(code);
  return c ? "K-" + c : null;
}

export function schluesselName(kcode) { return "klassenkey-" + kcode; }

/** Gleicher Name wie bei iOS (PaketJSON.schluessel), damit erneutes Senden ersetzt statt zu verdoppeln. */
export function paketSchluessel(klasse, fach, titel) {
  const erlaubt = /[a-z0-9]/;
  let aus = "";
  let letzterStrich = false;
  for (const z of (klasse + "-" + fach + "-" + titel).toLowerCase()) {
    if (erlaubt.test(z)) { aus += z; letzterStrich = false; }
    else if (!letzterStrich) { aus += "-"; letzterStrich = true; }
  }
  return aus;
}

export function paketName(kcode, klasse, fach, titel) {
  return "paket-" + kcode + "-" + paketSchluessel(klasse, fach, titel);
}

function base64Bytes(text) {
  try {
    const bin = atob(text);
    return Uint8Array.from(bin, (c) => c.charCodeAt(0));
  } catch { return null; }
}

function s(v) { return { value: v }; }

function kurz(w, max) { return typeof w === "string" && w.length > 0 && w.length <= max ? w : null; }

/** Prüft eine Anfrage an /v1/klasse, ohne Netz. Gibt { fehler } oder die Angaben zurück. */
export function pruefeKlasse(body) {
  if (!body || typeof body !== "object") return { fehler: "Anfrage unlesbar" };
  const kcode = cloudCode(body.code);
  if (!kcode) return { fehler: "Klassencode ungültig" };
  const d = body.daten;
  if (!d || typeof d !== "object") return { fehler: "Daten fehlen" };
  switch (body.art) {
    case "schluessel": {
      const roh = typeof d.publicKey === "string" ? base64Bytes(d.publicKey) : null;
      if (!roh || roh.length !== 32) return { fehler: "Schlüssel ungültig" };
      return { art: "schluessel", kcode, publicKey: d.publicKey };
    }
    case "paket": {
      const json = kurz(d.json, 38000), sig = kurz(d.sig, 200);
      if (!json || !sig) return { fehler: "Paket ungültig" };
      let p;
      try { p = JSON.parse(json); } catch { return { fehler: "Paket ist kein JSON" }; }
      const klasse = kurz(p.klasse, 40), fach = kurz(p.fach, 40), titel = kurz(p.arbeit, 120);
      if (!klasse || !fach || !titel || !Array.isArray(p.uebungen) || p.uebungen.length === 0) return { fehler: "Paket unvollständig" };
      const aufgaben = p.uebungen.reduce((n, u) => n + (Array.isArray(u.aufgaben) ? u.aufgaben.length : 0), 0);
      return { art: "paket", kcode, json, sig, klasse, fach, titel, aufgaben, name: paketName(kcode, klasse, fach, titel) };
    }
    case "loeschen": {
      const name = kurz(d.name, 300), sig = kurz(d.sig, 200);
      if (!name || !sig || !name.startsWith("paket-" + kcode + "-")) return { fehler: "Löschen ungültig" };
      return { art: "loeschen", kcode, name, sig };
    }
    default:
      return { fehler: "Unbekannte Art" };
  }
}

/** Stimmt die Unterschrift (Base64) zum öffentlichen Schlüssel (Base64, 32 Byte)? */
export async function unterschriftGilt(publicKeyB64, nachricht, sigB64) {
  const pk = base64Bytes(publicKeyB64), sig = base64Bytes(sigB64);
  if (!pk || !sig || pk.length !== 32 || sig.length !== 64) return false;
  try {
    const key = await crypto.subtle.importKey("raw", pk, { name: "Ed25519" }, false, ["verify"]);
    return await crypto.subtle.verify({ name: "Ed25519" }, key, sig, KODIERER.encode(nachricht));
  } catch { return false; }
}

export function schluesselAbfrage(kcode) {
  return JSON.stringify({ records: [{ recordName: schluesselName(kcode) }] });
}

/** Liest den öffentlichen Schlüssel aus der Antwort von records/lookup, null wenn es keinen gibt. */
export function schluesselAusAntwort(text) {
  try {
    const r = JSON.parse(text).records?.[0];
    if (!r || r.serverErrorCode) return null;
    const v = r.fields?.publicKey?.value;
    return typeof v === "string" ? v : null;
  } catch { return null; }
}

export function schluesselBefehl(kcode, publicKey) {
  return JSON.stringify({ operations: [{ operationType: "create", record: {
    recordType: "KlassenSchluessel", recordName: schluesselName(kcode),
    fields: { klassenCode: s(kcode), publicKey: s(publicKey) },
  } }] });
}

export function paketBefehl(p) {
  return JSON.stringify({ operations: [{ operationType: "forceReplace", record: {
    recordType: "Lernpaket", recordName: p.name,
    fields: { familienCode: s(p.kcode), klasse: s(p.klasse), fach: s(p.fach), titel: s(p.titel), json: s(p.json), aufgaben: s(p.aufgaben), sig: s(p.sig) },
  } }] });
}

export function loeschBefehl(name) {
  return JSON.stringify({ operations: [{ operationType: "forceDelete", record: { recordName: name } }] });
}
