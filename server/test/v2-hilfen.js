// Hilfen für die Tests von Protokoll 2: echtes SQLite (node:sqlite), eine Uhr zum Stellen und Geräte mit echten Schlüsseln.
import { DatabaseSync } from "node:sqlite";
import { FamilieKern } from "../src/v2/familie.js";
import { b64urlKodiere, geraetId, hex, sha256Hex, unterschriftText } from "../src/v2/krypto.js";

export const uhr = { t: 1_800_000_000_000 };

let ipZaehler = 0;
/** Jede Anfrage kommt von einer anderen Adresse, damit die Bremse im Router die Tests nicht stört. */
export function neueIp() { ipZaehler += 1; return `10.${(ipZaehler >> 16) & 255}.${(ipZaehler >> 8) & 255}.${ipZaehler & 255}`; }

/** So tun, als wäre ctx.storage.sql von Cloudflare da. */
export function sqliteAblage() {
  const db = new DatabaseSync(":memory:");
  const st = { alarm: null, geloescht: 0 };
  const sql = {
    exec(q, ...b) {
      const mehrere = q.trim().replace(/;\s*$/, "").includes(";");
      if (mehrere) { db.exec(q); return { toArray: () => [] }; }
      const zeilen = db.prepare(q).all(...b);
      return { toArray: () => zeilen };
    },
  };
  return {
    sql, st,
    transaktion(f) {
      db.exec("BEGIN");
      try { const r = f(); db.exec("COMMIT"); return r; } catch (e) { db.exec("ROLLBACK"); throw e; }
    },
    alarm: { setzen: async (t) => { st.alarm = t; } },
    alleLoeschen: async () => {
      st.geloescht += 1; st.alarm = null;
      for (const t of db.prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").all()) db.exec(`DROP TABLE "${t.name}"`);
    },
  };
}

/** Ein nachgebautes Namensraum-Objekt wie env.FAMILIE. Alle Familien liegen im Speicher. */
export function falscheFamilien({ pusher = null } = {}) {
  const familien = new Map();
  const protokoll = { jurisdiktionen: [] };
  const kernFuer = (id) => {
    if (!familien.has(id)) {
      const ablage = sqliteAblage();
      familien.set(id, { ablage, kern: new FamilieKern({
        id, jurisdiktion: "eu", sql: ablage.sql, transaktion: ablage.transaktion, alarm: ablage.alarm,
        alleLoeschen: ablage.alleLoeschen, pusher, jetzt: () => uhr.t,
      }) });
    }
    return familien.get(id);
  };
  const ns = {
    familien, protokoll, kernFuer,
    jurisdiction(j) { protokoll.jurisdiktionen.push(j); return ns; },
    newUniqueId() { const id = hex(crypto.getRandomValues(new Uint8Array(32))); return { toString: () => id }; },
    idFromString(s) { return { toString: () => s }; },
    get(id) { return { fetch: (req) => kernFuer(id.toString()).kern.behandle(req) }; },
  };
  return ns;
}

/** Ein Gerät mit eigenem Schlüsselpaar. [anfrage] baut eine unterschriebene Anfrage. */
export async function neuesGeraet(name) {
  const paar = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  const pub = b64urlKodiere(new Uint8Array(await crypto.subtle.exportKey("raw", paar.publicKey)));
  const id = await geraetId(pub);
  const g = {
    name, pub, id, paar,
    async anfrage(pfad, body, familie, opt = {}) {
      const roh = body === undefined ? "" : typeof body === "string" ? body : JSON.stringify(body);
      const zeit = opt.zeit ?? uhr.t;
      const text = unterschriftText({ methode: "POST", pfad, familie, geraet: id, zeit, bodyHash: await sha256Hex(roh) });
      let sig = b64urlKodiere(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, paar.privateKey, new TextEncoder().encode(text)));
      if (opt.kaputt) sig = sig.slice(0, -2) + (sig.endsWith("AA") ? "BB" : "AA");
      const kopf = { "Content-Type": "application/json", "Y-Familie": familie, "Y-Geraet": id, "Y-Zeit": String(zeit), "Y-Signatur": sig, "cf-connecting-ip": opt.ip || neueIp() };
      return new Request("https://x.test" + pfad, { method: "POST", headers: kopf, body: roh });
    },
  };
  return g;
}

export function offeneAnfrage(pfad, body, kopf = {}) {
  return new Request("https://x.test" + pfad, {
    method: "POST", headers: { "Content-Type": "application/json", "cf-connecting-ip": neueIp(), ...kopf }, body: JSON.stringify(body),
  });
}
