// Welche neuen Einträge in CloudKit welche Mitteilung auslösen, und an wen (Rolle).
// Gleiche Aufteilung wie bei den Abos der iOS-App: Eltern bekommen Fragen, Ergebnisse, Daumen und Nachschub,
// Kinder bekommen Antworten und Freigaben.

export const TYPEN = {
  JokerAnfrage: "eltern",
  RundenErgebnis: "eltern",
  JokerDaumen: "eltern",
  JokerNachschub: "eltern",
  JokerAntwort: "kind",
  JokerFreigabe: "kind",
  // Das Duell geht an alle Geräte der Familie
  Duell: "alle",
  DuellAntwort: "alle",
};

function duellJson(felder) {
  try { return JSON.parse(felder && felder.json ? felder.json.value : "{}"); } catch { return {}; }
}

/** Die Gerätekennung, die ein Duell-Eintrag geschickt hat (damit sie nicht an sich selbst geht), sonst "". */
export function absenderGeraet(typ, felder) {
  if (typ === "Duell") return duellJson(felder).vonGeraet || "";
  if (typ === "DuellAntwort") return duellJson(felder).geraet || "";
  return "";
}

function kurz(t, n = 100) {
  const s = String(t || "").replace(/\s+/g, " ").trim();
  return s.length > n ? s.slice(0, n - 1) + "…" : s;
}

/** { titel, text } für einen Datensatz (Felder als Wertepaare wie bei CloudKit). */
export function inhalt(typ, felder) {
  const f = (k) => (felder && felder[k] && felder[k].value !== undefined ? felder[k].value : "");
  switch (typ) {
    case "JokerAnfrage":
      return { titel: `🆘 Joker-Frage von ${kurz(f("kind"), 30) || "deinem Kind"}`, text: kurz(f("text")) };
    case "RundenErgebnis":
      return {
        titel: `📊 ${kurz(f("kind"), 30) || "Dein Kind"} hat geübt`,
        text: `${kurz(f("uebung"), 60)}: ${f("richtig")} von ${f("gesamt")} richtig`,
      };
    case "JokerDaumen":
      return { titel: `👍 Daumen von ${kurz(f("absender"), 30) || "deinem Kind"}`, text: "Deine Antwort hat geholfen." };
    case "JokerNachschub":
      return { titel: `🪙 ${kurz(f("kind"), 30) || "Dein Kind"} braucht neue Joker`, text: "Tippe, um Joker freizugeben." };
    case "JokerAntwort":
      return { titel: `💬 Antwort von ${kurz(f("absender"), 30) || "deinen Eltern"}`, text: kurz(f("text")) };
    case "JokerFreigabe":
      return { titel: "🎁 Neue Joker!", text: "Deine Eltern haben dir Joker geschenkt." };
    case "Duell":
      return { titel: "⚔️ Neue Duell-Herausforderung", text: `${kurz(duellJson(felder).von, 30) || "Jemand aus deiner Familie"} fordert dich heraus. Nimmst du an?` };
    case "DuellAntwort":
      return { titel: "⚔️ Ein Duell ist entschieden", text: "Schau nach, wer gewonnen hat!" };
    default:
      return null;
  }
}

/** Welche Rollen unter den Geräten einer Familie vorkommen, daraus die Typen, die abgefragt werden müssen. */
export function typenFuer(geraete) {
  const rollen = new Set(geraete.map((g) => g.rolle));
  return Object.keys(TYPEN).filter((t) => TYPEN[t] === "alle" ? geraete.length > 0 : rollen.has(TYPEN[t]));
}

/** Aus einer CloudKit-Antwort die Einträge, die neuer sind als der Stand. */
export function neueEintraege(text, stand) {
  let a;
  try { a = JSON.parse(text); } catch { return []; }
  return (a.records || [])
    .filter((r) => !r.serverErrorCode && r.created && r.created.timestamp > stand)
    .sort((x, y) => x.created.timestamp - y.created.timestamp);
}

export function abfrageKoerper(typ, code, limit = 100) {
  return JSON.stringify({
    query: { recordType: typ, filterBy: [{ comparator: "EQUALS", fieldName: "familienCode", fieldValue: { value: code } }] },
    resultsLimit: limit,
  });
}
