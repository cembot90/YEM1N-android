// Was der Server annimmt. Alles andere wird abgelehnt.
// Der Familiencode steht in der iOS-App als 10 Zeichen aus diesem Alphabet, gespeichert mit Strich in der Mitte.

const ALPHABET = /^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{10}$/;
const ROLLEN = ["kind", "eltern"];

/** Gibt den Code in der Form XXXXX-XXXXX zurück, so wie ihn die iOS-App speichert und abfragt. */
export function pruefeCode(eingabe) {
  if (typeof eingabe !== "string") return null;
  const s = eingabe.toUpperCase().replace(/[-\s]/g, "");
  return ALPHABET.test(s) ? s.slice(0, 5) + "-" + s.slice(5) : null;
}

function text(wert, max, pflicht = true) {
  if (wert === undefined || wert === null || wert === "") return pflicht ? null : "";
  if (typeof wert !== "string" || wert.length > max) return null;
  return wert;
}

/** Das Duell wird als JSON-Text abgelegt, wie bei iOS. Muss gültiges JSON sein. */
function jsonText(wert) {
  const t = text(wert, 20000);
  if (t === null) return null;
  try { JSON.parse(t); } catch { return null; }
  return t;
}

function zahl(wert, min, max) {
  return Number.isInteger(wert) && wert >= min && wert <= max ? wert : null;
}

function feldFehler(...werte) {
  return werte.some((w) => w === null);
}

function s(v) { return { value: v }; }

/**
 * Prüft eine Sendeanfrage. Rückgabe: { operation } mit dem fertigen CloudKit-Befehl,
 * oder { fehler } mit einem kurzen Grund.
 */
export function pruefeSenden(body) {
  if (!body || typeof body !== "object") return { fehler: "Anfrage unlesbar" };
  const code = pruefeCode(body.code);
  if (!code) return { fehler: "Familiencode ungültig" };
  const d = body.daten;
  if (!d || typeof d !== "object") return { fehler: "Daten fehlen" };

  let typ, name = null, felder;
  switch (body.art) {
    case "frage": {
      const kind = text(d.kind, 40), fach = text(d.fach, 40, false), uebung = text(d.uebung, 80, false), t = text(d.text, 500);
      if (feldFehler(kind, fach, uebung, t)) return { fehler: "Frage ungültig" };
      typ = "JokerAnfrage";
      felder = { familienCode: s(code), kind: s(kind), fach: s(fach), uebung: s(uebung), text: s(t), status: s("offen") };
      break;
    }
    case "antwort": {
      const id = text(d.frageId, 80), absender = text(d.absender, 40), emoji = text(d.emoji, 16, false), t = text(d.text, 500);
      if (feldFehler(id, absender, emoji, t)) return { fehler: "Antwort ungültig" };
      typ = "JokerAntwort";
      felder = { familienCode: s(code), anfrageID: s(id), absender: s(absender), emoji: s(emoji || "🙂"), text: s(t) };
      break;
    }
    case "daumen": {
      const frage = text(d.frageId, 80), antwort = text(d.antwortId, 80), absender = text(d.absender, 40);
      if (feldFehler(frage, antwort, absender)) return { fehler: "Daumen ungültig" };
      typ = "JokerDaumen";
      name = "daumen-" + antwort;
      felder = { familienCode: s(code), anfrageID: s(frage), antwortID: s(antwort), absender: s(absender) };
      break;
    }
    case "nachschub": {
      const kind = text(d.kind, 40);
      if (feldFehler(kind)) return { fehler: "Nachschub ungültig" };
      typ = "JokerNachschub";
      felder = { familienCode: s(code), kind: s(kind) };
      break;
    }
    case "freigabe": {
      const anzahl = zahl(d.anzahl === undefined ? 3 : d.anzahl, 1, 10);
      if (feldFehler(anzahl)) return { fehler: "Freigabe ungültig" };
      typ = "JokerFreigabe";
      felder = { familienCode: s(code), anzahl: s(anzahl) };
      break;
    }
    case "duell": {
      const id = text(d.id, 80), json = jsonText(d.json);
      if (feldFehler(id, json)) return { fehler: "Duell ungültig" };
      typ = "Duell";
      name = "duell-" + id;
      felder = { familienCode: s(code), json: s(json) };
      break;
    }
    case "duellantwort": {
      const id = text(d.duellId, 80), geraet = text(d.geraet, 80), json = jsonText(d.json);
      if (feldFehler(id, geraet, json)) return { fehler: "Duell-Antwort ungültig" };
      typ = "DuellAntwort";
      name = "duellantwort-" + id + "-" + geraet;
      felder = { familienCode: s(code), json: s(json) };
      break;
    }
    case "ergebnis": {
      const id = text(d.eintragId, 80), klasse = text(d.klasse, 40, false), fach = text(d.fach, 40, false);
      const arbeit = text(d.arbeit, 120, false), uebung = text(d.uebung, 120, false), kind = text(d.kind, 40, false);
      const details = text(d.details, 20000, false);
      const richtig = zahl(d.richtig, 0, 1000), gesamt = zahl(d.gesamt, 0, 1000), angesehen = zahl(d.angesehen, 0, 1000);
      const sterne = zahl(d.sterne, 0, 3), zeit = zahl(d.zeitpunkt, 0, 4102444800000);
      if (feldFehler(id, klasse, fach, arbeit, uebung, kind, details, richtig, gesamt, angesehen, sterne, zeit)) {
        return { fehler: "Ergebnis ungültig" };
      }
      typ = "RundenErgebnis";
      name = "erg-" + id;
      felder = {
        familienCode: s(code), eintragID: s(id), klasse: s(klasse), fach: s(fach), arbeit: s(arbeit),
        uebung: s(uebung), richtig: s(richtig), gesamt: s(gesamt), angesehen: s(angesehen),
        sterne: s(sterne), kind: s(kind), zeitpunkt: s(zeit),
      };
      if (details) felder.details = s(details);
      break;
    }
    default:
      return { fehler: "Unbekannte Art" };
  }
  const record = { recordType: typ, fields: felder };
  if (name) record.recordName = name;
  return { code, typ, operation: { operations: [{ operationType: "create", record }] } };
}

/** Prüft die Meldung eines Geräts für Mitteilungen. */
export function pruefeGeraet(body) {
  if (!body || typeof body !== "object") return { fehler: "Anfrage unlesbar" };
  const code = pruefeCode(body.code);
  if (!code) return { fehler: "Familiencode ungültig" };
  if (!ROLLEN.includes(body.rolle)) return { fehler: "Rolle ungültig" };
  const token = text(body.token, 400);
  if (!token) return { fehler: "Push-Kennung ungültig" };
  // Die Gerätekennung aus dem Duell, damit man sich nicht selbst benachrichtigt (optional)
  const geraet = text(body.geraet, 80, false);
  if (geraet === null) return { fehler: "Gerätekennung ungültig" };
  return { code, rolle: body.rolle, token, geraet };
}
