// Spielt einen kompletten Ablauf gegen einen laufenden Worker durch (lokal mit "npx wrangler dev" oder gegen die eigene Testadresse).
// Lokal:  npx wrangler dev --port 8799 --var FAMILIE_JURISDIKTION:keine   und dann   node scripts/lokal-pruefen.mjs http://localhost:8799
// Echt:   node scripts/lokal-pruefen.mjs https://DEINE-ADRESSE --eu     (prüft zusätzlich, dass die Familie in der EU liegt)
// Es werden nur eigene Testfamilien angelegt und am Ende wieder gelöscht.
import { neuesGeraet, uhr } from "../test/v2-hilfen.js";

const BASIS = (process.argv[2] || "http://localhost:8799").replace(/\/$/, "");
const EU_PFLICHT = process.argv.includes("--eu");
let fehler = 0;
const ok = (bedingung, text) => { console.log((bedingung ? "OK   " : "FEHLER ") + text); if (!bedingung) fehler += 1; };

const LOKAL = /^http:\/\/(localhost|127\.0\.0\.1)/.test(BASIS);
async function schicken(req) {
  const kopf = Object.fromEntries(req.headers);
  // Bei der echten Adresse setzt Cloudflare die Absenderadresse selbst, deshalb schicken wir dort keine mit.
  if (!LOKAL) delete kopf["cf-connecting-ip"];
  const r = await fetch(BASIS + new URL(req.url).pathname, { method: "POST", headers: kopf, body: await req.text() });
  const text = await r.text();
  let j = null; try { j = JSON.parse(text); } catch { /* keine lesbare Antwort */ }
  // Kommt keine JSON-Antwort, zeigen wir den Anfang des Textes, damit man sieht, wer geantwortet hat.
  return j ? { status: r.status, j } : { status: r.status, j: null, text: text.slice(0, 300) };
}
async function signiert(g, pfad, body, familie) { uhr.t = Date.now(); return schicken(await g.anfrage(pfad, body, familie)); }
async function offen(pfad, body, kopf) { const { offeneAnfrage } = await import("../test/v2-hilfen.js"); return schicken(offeneAnfrage(pfad, body, kopf)); }

const ping = await fetch(BASIS + "/v2/ping"); ok(ping.status === 200, "ping");
const mama = await neuesGeraet("Mama"), yemin = await neuesGeraet("Yemin");
const neu = await offen("/v2/familie", { pub: mama.pub, name: "Mama" });
ok(neu.status === 200 && /^[0-9a-f]{64}$/.test(neu.j.familie), "Familie angelegt " + (neu.j && neu.j.familie ? neu.j.familie.slice(0, 8) + "..." : JSON.stringify(neu)));
if (!neu.j) { console.log("\nAbbruch: Der Dienst hat keine lesbare Antwort gegeben."); process.exit(1); }
const fam = neu.j.familie;
ok(!EU_PFLICHT || neu.j.jurisdiktion === "eu", "Jurisdiktion der neuen Familie: " + neu.j.jurisdiktion + (EU_PFLICHT ? "" : " (nicht geprüft, lokal gibt es keine)"));
const st = await signiert(mama, "/v2/status", {}, fam);
ok(st.status === 200 && st.j.status === "aktiv" && (!EU_PFLICHT || st.j.jurisdiktion === "eu"), "Status der Eltern, Jurisdiktion " + st.j.jurisdiktion);
const einl = await signiert(mama, "/v2/einladung", { rolle: "kind" }, fam);
ok(einl.status === 200 && einl.j.token, "Einladung");
const k = await offen("/v2/koppeln", { token: einl.j.token, pub: yemin.pub, name: "Yemin" }, { "Y-Familie": fam });
ok(k.status === 200 && k.j.status === "wartet", "Kind koppelt, wartet");
ok((await signiert(yemin, "/v2/senden", { typ: "frage", daten: { id: "frage-0001", kind: "Yemin", text: "Hilfe?" } }, fam)).status === 403, "wartendes Gerät darf nicht senden");
ok((await signiert(mama, "/v2/geraet/erlauben", { geraet: yemin.id }, fam)).status === 200, "Eltern erlauben");
const s = await signiert(yemin, "/v2/senden", { typ: "frage", daten: { id: "frage-0001", kind: "Yemin", fach: "Mathe", text: "Was ist 56 : 7?" } }, fam);
ok(s.status === 200 && s.j.neu === true, "Kind sendet Frage");
const s2 = await signiert(yemin, "/v2/senden", { typ: "frage", daten: { id: "frage-0001", kind: "Yemin", text: "anders" } }, fam);
ok(s2.status === 200 && s2.j.neu === false, "doppelt gesendet bleibt einmal");
const l = await signiert(mama, "/v2/lesen", { typ: "frage" }, fam);
ok(l.status === 200 && l.j.eintraege.length === 1 && l.j.eintraege[0].daten.text === "Was ist 56 : 7?", "Eltern lesen die Frage");
ok((await signiert(yemin, "/v2/lesen", { typ: "ergebnis" }, fam)).status === 403, "Kind darf Ergebnisse nicht lesen");
const h = await signiert(yemin, "/v2/senden", { typ: "haustier", daten: { json: '{"stufe":1}' } }, fam);
const h2 = await signiert(yemin, "/v2/senden", { typ: "haustier", daten: { json: '{"stufe":2}' } }, fam);
const hl = await signiert(mama, "/v2/lesen", { typ: "haustier" }, fam);
ok(h.status === 200 && h2.status === 200 && hl.j.eintraege.length === 1 && JSON.parse(hl.j.eintraege[0].daten.json).stufe === 2, "Haustier wird überschrieben");
const re = await fetch(BASIS + "/v2/status", { method: "POST", headers: { "Y-Familie": "0".repeat(64) }, body: "{}" });
ok(re.status === 400 || re.status === 404, "erfundene Familienkennung wird abgewiesen (" + re.status + ")");
const fremd = await neuesGeraet("Fremd");
ok((await signiert(fremd, "/v2/lesen", { typ: "frage" }, fam)).status === 401, "unbekanntes Gerät mit richtiger Familienkennung: 401");
const liste = await signiert(mama, "/v2/geraete", {}, fam);
ok(liste.status === 200 && liste.j.geraete.length === 2 && !JSON.stringify(liste.j).includes(yemin.pub), "Geräteliste ohne Schlüssel");
ok((await signiert(yemin, "/v2/familie/loeschen", { bestaetigung: "LOESCHEN" }, fam)).status === 403, "Kind darf die Familie nicht löschen");
ok((await signiert(mama, "/v2/familie/loeschen", { bestaetigung: "LOESCHEN" }, fam)).status === 200, "Eltern löschen die Familie");
ok((await signiert(mama, "/v2/status", {}, fam)).status === 404, "danach gibt es die Familie nicht mehr");
console.log(fehler ? `\n${fehler} Fehler` : "\nAlles gut");
process.exit(fehler ? 1 : 0);
