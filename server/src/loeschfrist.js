// Löschfrist wie bei iOS: Einträge, die älter als 365 Tage sind, werden gelöscht.
// Der Server darf nur seine eigenen Einträge löschen (das sind alle, die von Android kamen).
// Einträge, die ein iPhone angelegt hat, räumt das iPhone selbst auf.

export const FRIST_TAGE = 365;
export const TYPEN_LOESCHEN = ["RundenErgebnis", "JokerAnfrage", "JokerAntwort", "JokerDaumen",
  "JokerNachschub", "JokerFreigabe", "Duell", "DuellAntwort"];

const TAG_MS = 24 * 60 * 60 * 1000;

/** Namen der Einträge in einer CloudKit-Antwort, die älter sind als die Frist. */
export function abgelaufene(text, jetzt = Date.now(), tage = FRIST_TAGE) {
  let a;
  try { a = JSON.parse(text); } catch { return []; }
  const grenze = jetzt - tage * TAG_MS;
  return (a.records || [])
    .filter((r) => !r.serverErrorCode && r.recordName && r.created && r.created.timestamp > 0 && r.created.timestamp < grenze)
    .map((r) => r.recordName);
}

export function loeschKoerper(namen) {
  return JSON.stringify({
    operations: namen.map((n) => ({ operationType: "forceDelete", record: { recordName: n } })),
  });
}

/** Läuft einmal am Tag um 03:00 UTC (Minute 0), sonst nicht. */
export function istAufraeumzeit(zeitMs) {
  const d = new Date(zeitMs);
  return d.getUTCHours() === 3 && d.getUTCMinutes() === 0;
}
