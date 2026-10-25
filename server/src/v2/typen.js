// Was Geräte in der Familie ablegen und lesen dürfen (Protokoll 2).
//
// Für jede Art gilt:
//   schreiben  welche Rollen sie anlegen dürfen
//   lesen      pro Rolle "alle", "eigene" (nur was das Gerät selbst angelegt hat) oder "nein"
//   tage       so lange bleibt ein Eintrag
//   max        höchste Zahl Einträge dieser Art in einer Familie (die ältesten fallen heraus)
//   ersetzen   true: Der Eintrag mit demselben Schlüssel wird überschrieben, sonst bleibt der erste
//   pruefe     prüft die Daten und liefert { schluessel, daten } oder { fehler }
//   push       { an, text } wer eine neutrale Mitteilung bekommt (ohne Namen und ohne Inhalt)

const TAG = 24 * 3600 * 1000;
const KENNUNG = /^[A-Za-z0-9_-]{6,64}$/;

function text(wert, max, pflicht = true) {
  if (wert === undefined || wert === null || wert === "") return pflicht ? null : "";
  if (typeof wert !== "string" || wert.length > max || /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/.test(wert)) return null;
  return wert;
}

function kennung(wert) {
  return typeof wert === "string" && KENNUNG.test(wert) ? wert : null;
}

function zahl(wert, min, max) {
  return Number.isInteger(wert) && wert >= min && wert <= max ? wert : null;
}

function jsonText(wert, max) {
  const t = text(wert, max);
  if (t === null) return null;
  try { JSON.parse(t); } catch { return null; }
  return t;
}

const hatFehler = (...werte) => werte.some((w) => w === null);

export const TYPEN = {
  frage: {
    schreiben: ["kind"], lesen: { kind: "eigene", eltern: "alle" }, tage: 365, max: 500, ersetzen: false,
    push: { an: "eltern", text: "Ein Kind braucht Hilfe." },
    pruefe(d) {
      const id = kennung(d.id), kind = text(d.kind, 40), fach = text(d.fach, 40, false), uebung = text(d.uebung, 80, false), t = text(d.text, 500);
      if (hatFehler(id, kind, fach, uebung, t)) return { fehler: "Frage ungültig" };
      return { schluessel: id, daten: { id, kind, fach, uebung, text: t } };
    },
  },
  antwort: {
    schreiben: ["eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 365, max: 1000, ersetzen: false,
    push: { an: "kind", text: "Es gibt eine neue Antwort." },
    pruefe(d) {
      const id = kennung(d.id), frageId = kennung(d.frageId), emoji = text(d.emoji, 16, false), t = text(d.text, 500);
      if (hatFehler(id, frageId, emoji, t)) return { fehler: "Antwort ungültig" };
      return { schluessel: id, daten: { id, frageId, emoji: emoji || "🙂", text: t } };
    },
  },
  daumen: {
    schreiben: ["kind"], lesen: { kind: "alle", eltern: "alle" }, tage: 365, max: 1000, ersetzen: false,
    push: { an: "eltern", text: "Jemand hat sich bei dir bedankt." },
    pruefe(d) {
      const frageId = kennung(d.frageId), antwortId = kennung(d.antwortId);
      if (hatFehler(frageId, antwortId)) return { fehler: "Daumen ungültig" };
      return { schluessel: antwortId, daten: { frageId, antwortId } };
    },
  },
  nachschub: {
    schreiben: ["kind"], lesen: { kind: "eigene", eltern: "alle" }, tage: 90, max: 200, ersetzen: false,
    push: { an: "eltern", text: "Ein Kind möchte neue Joker." },
    pruefe(d) {
      const id = kennung(d.id), kind = text(d.kind, 40);
      if (hatFehler(id, kind)) return { fehler: "Nachschub ungültig" };
      return { schluessel: id, daten: { id, kind } };
    },
  },
  freigabe: {
    schreiben: ["eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 90, max: 200, ersetzen: false,
    push: { an: "kind", text: "Es gibt neue Joker!" },
    pruefe(d) {
      const id = kennung(d.id), anzahl = zahl(d.anzahl === undefined ? 3 : d.anzahl, 1, 10);
      if (hatFehler(id, anzahl)) return { fehler: "Freigabe ungültig" };
      return { schluessel: id, daten: { id, anzahl } };
    },
  },
  ergebnis: {
    schreiben: ["kind"], lesen: { kind: "nein", eltern: "alle" }, tage: 365, max: 3000, ersetzen: false,
    push: { an: "eltern", text: "Es gibt ein neues Ergebnis." },
    pruefe(d) {
      const id = kennung(d.id), klasse = text(d.klasse, 40, false), fach = text(d.fach, 40, false);
      const arbeit = text(d.arbeit, 120, false), uebung = text(d.uebung, 120, false), kind = text(d.kind, 40, false);
      const details = text(d.details, 20000, false);
      const richtig = zahl(d.richtig, 0, 1000), gesamt = zahl(d.gesamt, 0, 1000), angesehen = zahl(d.angesehen === undefined ? 0 : d.angesehen, 0, 1000);
      const sterne = zahl(d.sterne, 0, 3), zeitpunkt = zahl(d.zeitpunkt, 0, 4102444800000);
      if (hatFehler(id, klasse, fach, arbeit, uebung, kind, details, richtig, gesamt, angesehen, sterne, zeitpunkt)) {
        return { fehler: "Ergebnis ungültig" };
      }
      const daten = { id, klasse, fach, arbeit, uebung, kind, richtig, gesamt, angesehen, sterne, zeitpunkt };
      if (details) daten.details = details;
      return { schluessel: id, daten };
    },
  },
  duell: {
    schreiben: ["kind", "eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 14, max: 200, ersetzen: false,
    push: { an: "alle", text: "Neue Duell-Herausforderung!" },
    pruefe(d) {
      const id = kennung(d.id), json = jsonText(d.json, 20000);
      if (hatFehler(id, json)) return { fehler: "Duell ungültig" };
      return { schluessel: id, daten: { id, json } };
    },
  },
  duellantwort: {
    schreiben: ["kind", "eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 14, max: 400, ersetzen: false,
    push: { an: "alle", text: "Ein Duell ist entschieden." },
    pruefe(d, ctx) {
      const duellId = kennung(d.duellId), json = jsonText(d.json, 20000);
      if (hatFehler(duellId, json)) return { fehler: "Duell-Antwort ungültig" };
      // Je Gerät und Duell gibt es nur eine Antwort.
      return { schluessel: duellId + ":" + ctx.geraet, daten: { duellId, json } };
    },
  },
  haustier: {
    schreiben: ["kind"], lesen: { kind: "alle", eltern: "alle" }, tage: 365, max: 1, ersetzen: true,
    push: null,
    pruefe(d) {
      const json = jsonText(d.json, 8000);
      if (hatFehler(json)) return { fehler: "Haustier ungültig" };
      return { schluessel: "haustier", daten: { json } };
    },
  },
  pflege: {
    schreiben: ["eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 4, max: 20, ersetzen: false,
    push: { an: "kind", text: "Dein Haustier wurde versorgt." },
    pruefe(d, ctx) {
      const art = d.art === "leckerli" || d.art === "spielen" ? d.art : null;
      const tag = typeof d.tag === "string" && /^\d{4}-\d{2}-\d{2}$/.test(d.tag) ? d.tag : null;
      if (hatFehler(art, tag)) return { fehler: "Pflege ungültig" };
      // Der Tag muss ungefähr heute sein (Zeitzonen!), sonst könnte jemand beliebig viele Tage vorbuchen.
      const diff = Math.abs(Date.parse(tag + "T12:00:00Z") - ctx.jetzt);
      if (!(diff < 2 * TAG)) return { fehler: "Tag passt nicht" };
      return { schluessel: tag + ":" + art, daten: { art, tag } };
    },
  },
  paket: {
    schreiben: ["eltern"], lesen: { kind: "alle", eltern: "alle" }, tage: 365, max: 50, ersetzen: true,
    push: { an: "kind", text: "Neue Arbeit für euch." },
    pruefe(d) {
      const id = kennung(d.id), json = jsonText(d.json, 80000);
      if (hatFehler(id, json)) return { fehler: "Paket ungültig" };
      return { schluessel: id, daten: { id, json } };
    },
  },
};

/** Ablaufzeit eines Eintrags in Millisekunden. */
export function ablaufFuer(typ, jetzt) {
  return jetzt + TYPEN[typ].tage * TAG;
}
