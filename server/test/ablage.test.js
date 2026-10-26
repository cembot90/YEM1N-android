import test from "node:test";
import assert from "node:assert/strict";
import { istAlterSchluessel, zaehle } from "../scripts/alte-ablage-leeren.mjs";

test("Nur Schlüssel des alten Familienweges gelten als alt", () => {
  assert.equal(istAlterSchluessel("familien"), true);
  assert.equal(istAlterSchluessel("geraete:ABCDE-23456"), true);
  assert.equal(istAlterSchluessel("stand:ABCDE-23456:frage"), true);
  assert.equal(istAlterSchluessel("irgendwas"), false);
  assert.equal(istAlterSchluessel("familienxyz"), false);
  assert.equal(istAlterSchluessel(""), false);
});

test("Das Zählen nennt nur Mengen und keine Namen", () => {
  const z = zaehle(["familien", "geraete:A", "geraete:B", "stand:A:frage", "stand:A:antwort", "stand:B:frage", "sonst"]);
  assert.deepEqual(z, { geraete: 2, stand: 3, familien: 1, uebrig: 1 });
});
