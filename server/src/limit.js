// Einfache Bremse: höchstens `max` Anfragen je Schlüssel in einem Zeitfenster.
// Gilt je Server-Instanz, das reicht gegen versehentliches Zumüllen, ist aber kein Schutz gegen gezielte Angriffe.

export function neueBremse(max = 30, fensterMs = 60000) {
  const stand = new Map();
  return function erlaubt(schluessel, jetzt = Date.now()) {
    const alt = (stand.get(schluessel) || []).filter((t) => jetzt - t < fensterMs);
    if (alt.length >= max) { stand.set(schluessel, alt); return false; }
    alt.push(jetzt);
    stand.set(schluessel, alt);
    if (stand.size > 5000) {
      for (const [k, v] of stand) if (!v.some((t) => jetzt - t < fensterMs)) stand.delete(k);
    }
    return true;
  };
}
