// Die Familie: Geräte, Kopplung, Einträge, Rechte und Löschfrist (Protokoll 2).
// Läuft in einem Durable Object je Familie (siehe familie-do.js). Dieser Kern kennt nur eine kleine Schnittstelle zur
// Außenwelt und lässt sich deshalb mit echtem SQLite lokal testen.
//
// Schnittstelle (siehe Konstruktor): sql (wie ctx.storage.sql), transaktion, alarm, alleLoeschen, pusher, jetzt.

import { geraetId, pubBytes, sha256Hex, unterschriftGilt, unterschriftText, zufallB64 } from "./krypto.js";
import { TYPEN, ablaufFuer } from "./typen.js";

const MINUTE = 60 * 1000;
const TAG = 24 * 60 * MINUTE;
const ZEIT_TOLERANZ = 5 * MINUTE;
const LEERE_FAMILIE_TAGE = 30;
const FAMILIE_TAGE = 365;
/** Höchstens so viele Anfragen je Familie und Tag (bei 4 Geräten und Abfrage jede Minute braucht eine Familie etwa 6000). */
export const TAGESLIMIT = 20000;
const EINLADUNG_MIN = 10;
const MAX_GERAETE = 12;
const MAX_WARTEND = 3;
const MAX_EINLADUNGEN = 5;
const ROLLEN = ["kind", "eltern"];

function antwort(status, obj) {
  return new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json; charset=utf-8" } });
}
const fehler = (status, text, extra = {}) => antwort(status, { fehler: text, ...extra });

/** Ein Name für ein Gerät: 1 bis 40 Zeichen, einfache Leerzeichen, keine Steuerzeichen. */
export function sauberName(wert, max = 40) {
  if (typeof wert !== "string") return null;
  const t = wert.replace(/\s+/g, " ").trim();
  return t.length >= 1 && t.length <= max && !/[\u0000-\u001f\u007f]/.test(t) ? t : null;
}

export class FamilieKern {
  constructor({ id, jurisdiktion = null, sql, transaktion, alarm, alleLoeschen, pusher = null, jetzt = Date.now }) {
    this.id = id;
    this.jurisdiktion = jurisdiktion;
    this.sql = sql;
    this.transaktion = transaktion;
    this.alarm_ = alarm;
    this.alleLoeschen = alleLoeschen;
    this.pusher = pusher;
    this.jetzt = jetzt;
    this.gesehen = new Map();   // Unterschrift -> bis wann merken (gegen Wiederholung)
    this.drosseln = new Map();  // Schlüssel -> { start, n }
    this.angelegtCache = null;
  }

  // ---------- Ablage ----------

  abfrage(q, ...b) { return this.sql.exec(q, ...b).toArray(); }
  eine(q, ...b) { return this.abfrage(q, ...b)[0] || null; }
  ausfuehren(q, ...b) { this.sql.exec(q, ...b).toArray(); }

  initialisieren() {
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT NOT NULL);
      CREATE TABLE IF NOT EXISTS geraete (
        id TEXT PRIMARY KEY, rolle TEXT NOT NULL, name TEXT NOT NULL, pub TEXT NOT NULL, status TEXT NOT NULL,
        push_art TEXT NOT NULL DEFAULT '', push_token TEXT NOT NULL DEFAULT '', push_umgebung TEXT NOT NULL DEFAULT '',
        erstellt INTEGER NOT NULL, zuletzt INTEGER NOT NULL, geaendert INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS einladungen (hash TEXT PRIMARY KEY, rolle TEXT NOT NULL, bis INTEGER NOT NULL, von TEXT NOT NULL);
      CREATE TABLE IF NOT EXISTS eintraege (
        nr INTEGER PRIMARY KEY AUTOINCREMENT, typ TEXT NOT NULL, schluessel TEXT NOT NULL, von TEXT NOT NULL,
        absender TEXT NOT NULL, daten TEXT NOT NULL, erstellt INTEGER NOT NULL, bis INTEGER NOT NULL,
        UNIQUE (typ, schluessel)
      );
      CREATE INDEX IF NOT EXISTS eintraege_typ_nr ON eintraege (typ, nr);
    `).toArray();
  }

  istAngelegt() {
    if (this.angelegtCache === true) return true;
    const t = this.eine("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'meta'");
    if (!t) return false;
    const ok = !!this.eine("SELECT v FROM meta WHERE k = 'erstellt'");
    if (ok) this.angelegtCache = true;
    return ok;
  }

  meta(k) { const r = this.eine("SELECT v FROM meta WHERE k = ?", k); return r ? r.v : null; }
  setzeMeta(k, v) { this.ausfuehren("INSERT INTO meta (k, v) VALUES (?, ?) ON CONFLICT (k) DO UPDATE SET v = excluded.v", k, String(v)); }

  // ---------- Drosseln und Alarm ----------

  /** true, wenn noch erlaubt. Zählt im Speicher, das spart Schreibvorgänge. Nach einem Neustart des Objekts beginnt es neu. */
  erlaubt(schluessel, max, fensterMs) {
    const jetzt = this.jetzt();
    const e = this.drosseln.get(schluessel);
    if (!e || jetzt - e.start >= fensterMs) { this.drosseln.set(schluessel, { start: jetzt, n: 1 }); return true; }
    e.n += 1;
    return e.n <= max;
  }

  /** Setzt den Löschtermin der Familie nach hinten, aber höchstens einmal am Tag (spart Schreibvorgänge). */
  async verlaengere(tage = FAMILIE_TAGE) {
    const ziel = this.jetzt() + tage * TAG;
    const alt = Number(this.meta("loeschen_am") || 0);
    if (ziel - alt > TAG) {
      this.setzeMeta("loeschen_am", ziel);
      await this.planeAlarm();
    }
  }

  /** Der nächste Zeitpunkt, an dem aufgeräumt werden muss: Löschtermin, ablaufender Eintrag, alte Geräte-Anfragen. */
  async planeAlarm() {
    const kandidaten = [Number(this.meta("loeschen_am") || 0)];
    const e = this.eine("SELECT MIN(bis) AS t FROM eintraege");
    if (e && e.t) kandidaten.push(e.t);
    const g = this.eine("SELECT MIN(geaendert) AS t FROM geraete WHERE status IN ('abgelehnt', 'wartet')");
    if (g && g.t) kandidaten.push(g.t + TAG);
    const ziel = Math.max(Math.min(...kandidaten.filter((x) => x > 0)), this.jetzt() + 1000);
    if (ziel !== Number(this.meta("alarm") || 0)) {
      this.setzeMeta("alarm", ziel);
      await this.alarm_.setzen(ziel);
    }
  }

  async alarm() {
    if (!this.istAngelegt()) return;
    const jetzt = this.jetzt();
    if (Number(this.meta("loeschen_am") || 0) <= jetzt) { await this.allesLoeschen(); return; }
    this.ausfuehren("DELETE FROM eintraege WHERE bis <= ?", jetzt);
    this.ausfuehren("DELETE FROM einladungen WHERE bis <= ?", jetzt);
    this.ausfuehren("DELETE FROM geraete WHERE status IN ('abgelehnt', 'wartet') AND geaendert <= ?", jetzt - TAG);
    await this.planeAlarm();
  }

  async allesLoeschen() {
    await this.alleLoeschen();
    this.angelegtCache = false;
    this.gesehen.clear();
    this.drosseln.clear();
  }

  // ---------- Anfrage ----------

  async behandle(request) {
    const pfad = new URL(request.url).pathname;
    if (request.method !== "POST") return fehler(405, "Nur POST");
    // Tageslimit je Familie: schützt vor einem Fehler, der eine Familie mit Anfragen überschwemmt (und damit die Kosten).
    if (!this.erlaubt("tag", TAGESLIMIT, TAG)) return fehler(429, "Für heute ist das Limit dieser Familie erreicht, morgen geht es weiter");
    const roh = await request.text();
    if (roh.length > 120000) return fehler(413, "Zu groß");
    let body = {};
    if (roh) {
      try { body = JSON.parse(roh); } catch { return fehler(400, "Kein JSON"); }
      if (body === null || typeof body !== "object" || Array.isArray(body)) return fehler(400, "Anfrage unlesbar");
    }
    const kopf = (n) => request.headers.get(n) || "";

    if (pfad === "/v2/familie") return this.anlegen(body, kopf("Y-Familie"));
    if (!this.istAngelegt()) return fehler(404, "Diese Familie gibt es nicht");
    if (pfad === "/v2/koppeln") return this.koppeln(body);

    const a = await this.anmelden(request.method, pfad, roh, kopf);
    if (a.antwort) return a.antwort;
    const g = a.geraet;
    const eltern = g.rolle === "eltern";

    // Wartende und abgelehnte Geräte dürfen nur nach ihrem Stand fragen (und zurückziehen, solange sie warten).
    if (g.status !== "aktiv") {
      if (pfad === "/v2/status") return this.status(g);
      if (pfad === "/v2/geraet/austreten" && g.status === "wartet") return this.austreten(g, {});
      return fehler(403, "Dieses Gerät ist nicht freigegeben", { status: g.status });
    }

    switch (pfad) {
      case "/v2/status": return this.status(g);
      case "/v2/push": return this.pushAnmelden(g, body);
      case "/v2/senden": return this.senden(g, body);
      case "/v2/lesen": return this.lesen(g, body);
      case "/v2/geraet/austreten": return this.austreten(g, body);
      default: break;
    }
    if (!eltern) {
      return ["/v2/einladung", "/v2/geraete", "/v2/geraet/erlauben", "/v2/geraet/ablehnen", "/v2/geraet/entfernen", "/v2/familie/loeschen"].includes(pfad)
        ? fehler(403, "Das dürfen nur die Eltern") : fehler(404, "Unbekannt");
    }
    switch (pfad) {
      case "/v2/einladung": return this.einladung(g, body);
      case "/v2/geraete": return this.geraeteListe(g);
      case "/v2/geraet/erlauben": return this.freigeben(g, body, "aktiv");
      case "/v2/geraet/ablehnen": return this.freigeben(g, body, "abgelehnt");
      case "/v2/geraet/entfernen": return this.entfernen(g, body);
      case "/v2/familie/loeschen": return this.familieLoeschen(body);
      default: return fehler(404, "Unbekannt");
    }
  }

  /** Prüft Zeit, Gerät, Unterschrift und Wiederholung. Gibt { geraet } oder { antwort } zurück. */
  async anmelden(methode, pfad, roh, kopf) {
    const familie = kopf("Y-Familie"), gid = kopf("Y-Geraet"), sig = kopf("Y-Signatur");
    const zeit = /^\d{10,16}$/.test(kopf("Y-Zeit")) ? Number(kopf("Y-Zeit")) : NaN;
    const jetzt = this.jetzt();
    if (familie !== this.id) return { antwort: fehler(401, "Familie passt nicht") };
    if (!Number.isFinite(zeit) || Math.abs(zeit - jetzt) > ZEIT_TOLERANZ) {
      return { antwort: fehler(401, "Die Uhrzeit des Geräts passt nicht", { serverZeit: jetzt }) };
    }
    const g = this.eine("SELECT * FROM geraete WHERE id = ?", gid);
    if (!g) return { antwort: fehler(401, "Unbekanntes Gerät") };
    const text = unterschriftText({ methode, pfad, familie, geraet: gid, zeit, bodyHash: await sha256Hex(roh) });
    if (!(await unterschriftGilt(g.pub, sig, text))) return { antwort: fehler(401, "Unterschrift passt nicht") };
    if (this.gesehen.has(sig)) return { antwort: fehler(401, "Anfrage wurde schon einmal gesendet") };
    this.gesehen.set(sig, zeit + 2 * ZEIT_TOLERANZ);
    if (this.gesehen.size > 500) for (const [k, bis] of this.gesehen) if (bis < jetzt) this.gesehen.delete(k);
    if (!this.erlaubt("geraet:" + gid, 120, MINUTE)) return { antwort: fehler(429, "Zu viele Anfragen, bitte kurz warten") };
    // Zuletzt gesehen: höchstens einmal pro Stunde festhalten.
    if (jetzt - g.zuletzt > 60 * MINUTE) { this.ausfuehren("UPDATE geraete SET zuletzt = ? WHERE id = ?", jetzt, gid); g.zuletzt = jetzt; }
    return { geraet: g };
  }

  // ---------- Familie und Geräte ----------

  async anlegen(body, familieKopf) {
    if (familieKopf !== this.id) return fehler(400, "Familie passt nicht");
    if (this.istAngelegt()) return fehler(409, "Diese Familie gibt es schon");
    const gid = await geraetId(body.pub);
    const name = sauberName(body.name);
    if (!gid || !name) return fehler(400, "Gerät ungültig");
    const jetzt = this.jetzt();
    this.initialisieren();
    this.transaktion(() => {
      this.setzeMeta("erstellt", jetzt);
      this.setzeMeta("loeschen_am", jetzt + LEERE_FAMILIE_TAGE * TAG);
      this.ausfuehren(
        "INSERT INTO geraete (id, rolle, name, pub, status, erstellt, zuletzt, geaendert) VALUES (?, 'eltern', ?, ?, 'aktiv', ?, ?, ?)",
        gid, name, body.pub, jetzt, jetzt, jetzt,
      );
    });
    this.angelegtCache = true;
    await this.planeAlarm();
    return antwort(200, { ok: true, familie: this.id, geraet: gid, rolle: "eltern", jurisdiktion: this.jurisdiktion });
  }

  status(g) {
    const r = { ok: true, status: g.status, rolle: g.rolle, name: g.name, geraet: g.id, familie: this.id, serverZeit: this.jetzt() };
    if (g.status === "aktiv") {
      r.geraete = this.eine("SELECT COUNT(*) AS n FROM geraete WHERE status = 'aktiv'").n;
      r.jurisdiktion = this.jurisdiktion;
    }
    return antwort(200, r);
  }

  async einladung(g, body) {
    if (!ROLLEN.includes(body.rolle)) return fehler(400, "Rolle ungültig");
    const jetzt = this.jetzt();
    this.ausfuehren("DELETE FROM einladungen WHERE bis <= ?", jetzt);
    if (this.eine("SELECT COUNT(*) AS n FROM einladungen").n >= MAX_EINLADUNGEN) return fehler(429, "Zu viele offene Einladungen");
    const token = zufallB64(16);
    const bis = jetzt + EINLADUNG_MIN * MINUTE;
    this.ausfuehren("INSERT INTO einladungen (hash, rolle, bis, von) VALUES (?, ?, ?, ?)", await sha256Hex(token), body.rolle, bis, g.id);
    return antwort(200, { ok: true, token, rolle: body.rolle, bis });
  }

  /** Ein neues Gerät meldet sich mit dem Einmal-Schlüssel aus dem QR-Code. Es bleibt "wartend", bis ein Eltern-Gerät es erlaubt. */
  async koppeln(body) {
    const token = typeof body.token === "string" && body.token.length <= 64 ? body.token : null;
    const gid = await geraetId(body.pub);
    const name = sauberName(body.name);
    if (!token || !gid || !name) return fehler(400, "Anfrage ungültig");
    const jetzt = this.jetzt();
    const hash = await sha256Hex(token);
    const einl = this.eine("SELECT * FROM einladungen WHERE hash = ? AND bis > ?", hash, jetzt);
    if (!einl) return fehler(403, "Die Einladung gilt nicht (mehr)");
    if (this.eine("SELECT id FROM geraete WHERE id = ?", gid)) return fehler(409, "Dieses Gerät ist schon in der Familie");
    const n = this.eine("SELECT COUNT(*) AS gesamt, SUM(status = 'wartet') AS wartend FROM geraete");
    if (n.gesamt >= MAX_GERAETE) return fehler(409, "Die Familie hat schon die höchste Zahl an Geräten");
    if ((n.wartend || 0) >= MAX_WARTEND) return fehler(429, "Es warten schon andere Geräte auf eine Freigabe");
    this.transaktion(() => {
      this.ausfuehren("DELETE FROM einladungen WHERE hash = ?", hash);
      this.ausfuehren(
        "INSERT INTO geraete (id, rolle, name, pub, status, erstellt, zuletzt, geaendert) VALUES (?, ?, ?, ?, 'wartet', ?, ?, ?)",
        gid, einl.rolle, name, body.pub, jetzt, jetzt, jetzt,
      );
    });
    await this.planeAlarm();
    await this.pushen(this.ziele("eltern", null), "geraet", "Ein neues Gerät möchte der Familie beitreten.");
    return antwort(200, { ok: true, geraet: gid, status: "wartet", rolle: einl.rolle });
  }

  geraeteListe(g) {
    const liste = this.abfrage("SELECT id, name, rolle, status, erstellt, zuletzt FROM geraete ORDER BY erstellt").map((r) => ({ ...r, ich: r.id === g.id }));
    return antwort(200, { ok: true, geraete: liste });
  }

  async freigeben(g, body, neu) {
    const ziel = typeof body.geraet === "string" ? this.eine("SELECT * FROM geraete WHERE id = ?", body.geraet) : null;
    if (!ziel) return fehler(404, "Gerät nicht gefunden");
    if (ziel.status !== "wartet") return fehler(409, "Das Gerät wartet nicht auf eine Freigabe");
    this.ausfuehren("UPDATE geraete SET status = ?, geaendert = ? WHERE id = ?", neu, this.jetzt(), ziel.id);
    if (neu === "aktiv") await this.verlaengere();
    else await this.planeAlarm();
    return antwort(200, { ok: true, geraet: ziel.id, status: neu });
  }

  async entfernen(g, body) {
    const ziel = typeof body.geraet === "string" ? this.eine("SELECT * FROM geraete WHERE id = ?", body.geraet) : null;
    if (!ziel) return fehler(404, "Gerät nicht gefunden");
    if (ziel.id === g.id) return fehler(400, "Das eigene Gerät entfernt man mit Austreten");
    this.ausfuehren("DELETE FROM geraete WHERE id = ?", ziel.id);
    return antwort(200, { ok: true });
  }

  /** Das Gerät verlässt die Familie (Widerruf). Auf Wunsch verschwinden auch die Einträge, die es angelegt hat. */
  async austreten(g, body) {
    if (g.rolle === "eltern" && g.status === "aktiv") {
      const n = this.eine("SELECT COUNT(*) AS n FROM geraete WHERE rolle = 'eltern' AND status = 'aktiv'").n;
      if (n <= 1) return fehler(409, "Das ist das letzte Eltern-Gerät. Löscht stattdessen die ganze Familie.");
    }
    this.transaktion(() => {
      this.ausfuehren("DELETE FROM geraete WHERE id = ?", g.id);
      if (body.eintraegeLoeschen === true) this.ausfuehren("DELETE FROM eintraege WHERE von = ?", g.id);
    });
    return antwort(200, { ok: true });
  }

  async familieLoeschen(body) {
    if (body.bestaetigung !== "LOESCHEN") return fehler(400, "Bestätigung fehlt");
    await this.allesLoeschen();
    return antwort(200, { ok: true });
  }

  pushAnmelden(g, body) {
    const art = body.art === "" || body.art === "fcm" || body.art === "apns" ? body.art : null;
    if (art === null) return fehler(400, "Art ungültig");
    let token = "", umgebung = "";
    if (art) {
      token = typeof body.token === "string" ? body.token : "";
      if (art === "apns" && !/^[0-9a-fA-F]{32,200}$/.test(token)) return fehler(400, "Kennung ungültig");
      if (art === "fcm" && !/^[\w:.\-]{20,400}$/.test(token)) return fehler(400, "Kennung ungültig");
      umgebung = art === "apns" ? (body.umgebung === "sandbox" ? "sandbox" : "production") : "";
    }
    this.transaktion(() => {
      // Dieselbe Kennung gehört immer nur einem Gerät (zum Beispiel nach einer Neuinstallation).
      if (token) this.ausfuehren("UPDATE geraete SET push_art = '', push_token = '' WHERE push_token = ? AND id <> ?", token, g.id);
      this.ausfuehren("UPDATE geraete SET push_art = ?, push_token = ?, push_umgebung = ? WHERE id = ?", art, token, umgebung, g.id);
    });
    return antwort(200, { ok: true });
  }

  // ---------- Einträge ----------

  async senden(g, body) {
    const def = TYPEN[body.typ];
    if (!def) return fehler(400, "Unbekannte Art");
    if (!def.schreiben.includes(g.rolle)) return fehler(403, "Das darf dieses Gerät nicht senden");
    const jetzt = this.jetzt();
    const p = def.pruefe(body.daten && typeof body.daten === "object" ? body.daten : {}, { geraet: g.id, jetzt });
    if (p.fehler) return fehler(400, p.fehler);
    if (!this.erlaubt("senden:" + g.id, 600, 60 * MINUTE)) return fehler(429, "Zu viele Einträge, bitte später wieder");

    const alt = this.eine("SELECT nr FROM eintraege WHERE typ = ? AND schluessel = ?", body.typ, p.schluessel);
    if (alt && !def.ersetzen) return antwort(200, { ok: true, neu: false, nr: alt.nr });
    const bis = ablaufFuer(body.typ, jetzt);
    this.transaktion(() => {
      if (alt) this.ausfuehren("DELETE FROM eintraege WHERE nr = ?", alt.nr);
      this.ausfuehren(
        "INSERT INTO eintraege (typ, schluessel, von, absender, daten, erstellt, bis) VALUES (?, ?, ?, ?, ?, ?, ?)",
        body.typ, p.schluessel, g.id, g.name, JSON.stringify(p.daten), jetzt, bis,
      );
      // Die ältesten Einträge dieser Art fallen heraus, wenn es zu viele werden.
      this.ausfuehren(
        "DELETE FROM eintraege WHERE typ = ? AND nr IN (SELECT nr FROM eintraege WHERE typ = ? ORDER BY nr DESC LIMIT -1 OFFSET ?)",
        body.typ, body.typ, def.max,
      );
    });
    const neu = this.eine("SELECT nr FROM eintraege WHERE typ = ? AND schluessel = ?", body.typ, p.schluessel);
    await this.verlaengere();
    const naechster = Number(this.meta("alarm") || 0);
    if (!naechster || bis < naechster) await this.planeAlarm();
    if (def.push) await this.pushen(this.ziele(def.push.an, g.id), body.typ, def.push.text);
    return antwort(200, { ok: true, neu: true, nr: neu ? neu.nr : null });
  }

  lesen(g, body) {
    const def = TYPEN[body.typ];
    if (!def) return fehler(400, "Unbekannte Art");
    const recht = def.lesen[g.rolle];
    if (recht === "nein") return fehler(403, "Das darf dieses Gerät nicht lesen");
    const seit = Number.isInteger(body.seit) && body.seit >= 0 ? body.seit : 0;
    const limit = Number.isInteger(body.limit) ? Math.min(Math.max(body.limit, 1), 200) : 100;
    const eigene = recht === "eigene";
    const zeilen = this.abfrage(
      `SELECT nr, typ, schluessel, von, absender, daten, erstellt FROM eintraege
       WHERE typ = ? AND nr > ? AND bis > ? ${eigene ? "AND von = ?" : ""} ORDER BY nr LIMIT ?`,
      ...[body.typ, seit, this.jetzt(), ...(eigene ? [g.id] : []), limit],
    );
    const eintraege = zeilen.map((z) => ({
      nr: z.nr, typ: z.typ, id: z.schluessel, von: z.von, absender: z.absender, erstellt: z.erstellt, daten: JSON.parse(z.daten),
    }));
    return antwort(200, { ok: true, eintraege, letzte: eintraege.length ? eintraege[eintraege.length - 1].nr : seit, mehr: zeilen.length === limit });
  }

  // ---------- Mitteilungen ----------

  /** Geräte mit Push-Kennung, an die eine Mitteilung gehen soll. [an] ist "eltern", "kind" oder "alle". Der Absender bekommt nichts. */
  ziele(an, absender) {
    return this.abfrage("SELECT id, push_art AS art, push_token AS token, push_umgebung AS umgebung, rolle FROM geraete WHERE status = 'aktiv' AND push_token <> ''")
      .filter((r) => r.id !== absender && (an === "alle" || r.rolle === an));
  }

  /** Schickt eine neutrale Mitteilung (ohne Namen und ohne Inhalt). Fehler stoppen nie die eigentliche Anfrage. */
  async pushen(ziele, typ, text) {
    if (!ziele.length || !this.pusher) return;
    let uhr;
    try {
      const erg = await Promise.race([
        this.pusher({ ziele, titel: "YEM1N", text, typ }),
        new Promise((_, nein) => { uhr = setTimeout(() => nein(new Error("Zeit abgelaufen")), 4000); }),
      ]);
      (erg || []).forEach((e, i) => {
        if (e && e.ungueltig) this.ausfuehren("UPDATE geraete SET push_art = '', push_token = '' WHERE id = ?", ziele[i].id);
      });
    } catch (e) {
      console.log("push:", String(e));
    } finally {
      clearTimeout(uhr);
    }
  }
}
