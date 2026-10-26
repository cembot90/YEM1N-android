// Löscht die Ablage des alten Familienwegs im Cloudflare-KV "GERAETE":
// "geraete:<Familiencode>" (Rollen und Push-Kennungen), "stand:<Familiencode>:<Typ>" und "familien".
// Erst ausführen, wenn der alte Weg abgeschaltet ist (V1_AUS = "1") und beide Apps auf Protokoll 2 laufen.
// Aufruf im Ordner server:  node scripts/alte-ablage-leeren.mjs          (zählt nur, löscht nichts)
//                           node scripts/alte-ablage-leeren.mjs --ja     (löscht wirklich)
// Die Schlüsselnamen enthalten Familiencodes. Sie werden deshalb nie ausgegeben, nur gezählt.
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const wirklich = process.argv.includes("--ja");
const npx = process.platform === "win32" ? "npx.cmd" : "npx";
const wrangler = (...args) => execFileSync(npx, ["wrangler@latest", ...args], { encoding: "utf8", maxBuffer: 256 * 1024 * 1024 });

/** Nur Schlüssel des alten Weges. Alles andere bleibt unberührt. */
export function istAlterSchluessel(name) {
  return name === "familien" || name.startsWith("geraete:") || name.startsWith("stand:");
}

export function zaehle(namen) {
  const z = { geraete: 0, stand: 0, familien: 0, uebrig: 0 };
  for (const n of namen) {
    if (n === "familien") z.familien++;
    else if (n.startsWith("geraete:")) z.geraete++;
    else if (n.startsWith("stand:")) z.stand++;
    else z.uebrig++;
  }
  return z;
}

async function main() {
  const text = wrangler("kv", "key", "list", "--binding", "GERAETE", "--remote");
  const alle = JSON.parse(text.slice(text.indexOf("["))).map((k) => k.name);
  const z = zaehle(alle);
  console.log(`Gefunden: ${z.geraete} Geräte-Listen, ${z.stand} Stände, ${z.familien} Familien-Liste, ${z.uebrig} andere (bleiben).`);

  const weg = alle.filter(istAlterSchluessel);
  if (!wirklich) {
    console.log(`Nichts gelöscht. Mit "--ja" am Ende löscht das Skript ${weg.length} Einträge.`);
    return;
  }
  if (!weg.length) { console.log("Nichts zu löschen."); return; }

  const ordner = mkdtempSync(join(tmpdir(), "yem1n-kv-"));
  const datei = join(ordner, "schluessel.json");
  try {
    writeFileSync(datei, JSON.stringify(weg));
    wrangler("kv", "bulk", "delete", datei, "--binding", "GERAETE", "--remote", "--force");
    console.log(`Gelöscht: ${weg.length} Einträge.`);
  } finally {
    rmSync(ordner, { recursive: true, force: true });
  }
}

import { fileURLToPath } from "node:url";
if (process.argv[1] === fileURLToPath(import.meta.url)) await main();
