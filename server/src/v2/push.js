// Verteilt eine Mitteilung auf FCM (Android) und APNs (iPhone). Die Texte sind neutral, ohne Namen und Inhalte.
import { pushSenden } from "../fcm.js";
import { apnsSenden } from "./apns.js";

/** Gibt die Funktion zurück, die die Familie zum Verschicken aufruft. Die Antwort hat pro Ziel { ok, ungueltig }. */
export function pushVersand(env, holen = fetch) {
  return async ({ ziele, titel, text, typ }) => Promise.all(ziele.map(async (z) => {
    try {
      if (z.art === "fcm") {
        if (!env.FCM_DIENSTKONTO) return { ok: false, ungueltig: false };
        return await pushSenden(JSON.parse(env.FCM_DIENSTKONTO), z.token, titel, text, typ, holen);
      }
      if (z.art === "apns") return await apnsSenden(env, z, titel, text, typ, holen);
    } catch (e) {
      console.log("push:", z.art, String(e));
    }
    return { ok: false, ungueltig: false };
  }));
}
