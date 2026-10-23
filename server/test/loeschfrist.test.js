import test from "node:test";
import assert from "node:assert/strict";
import { abgelaufene, istAufraeumzeit, loeschKoerper } from "../src/loeschfrist.js";

const TAG = 24 * 60 * 60 * 1000;
const JETZT = Date.UTC(2026, 9, 23, 3, 0, 0);

test("abgelaufene: nur Einträge älter als 365 Tage", () => {
  const text = JSON.stringify({ records: [
    { recordName: "alt", created: { timestamp: JETZT - 366 * TAG } },
    { recordName: "knapp", created: { timestamp: JETZT - 364 * TAG } },
    { recordName: "neu", created: { timestamp: JETZT - TAG } },
    { recordName: "ohne" },
    { recordName: "fehler", serverErrorCode: "X", created: { timestamp: 1 } },
  ] });
  assert.deepEqual(abgelaufene(text, JETZT), ["alt"]);
  assert.deepEqual(abgelaufene("kein json", JETZT), []);
});

test("loeschKoerper: ein forceDelete je Name", () => {
  const k = JSON.parse(loeschKoerper(["a", "b"]));
  assert.equal(k.operations.length, 2);
  assert.equal(k.operations[1].operationType, "forceDelete");
  assert.equal(k.operations[1].record.recordName, "b");
});

test("istAufraeumzeit: nur 03:00 UTC", () => {
  assert.equal(istAufraeumzeit(Date.UTC(2026, 0, 1, 3, 0)), true);
  assert.equal(istAufraeumzeit(Date.UTC(2026, 0, 1, 3, 1)), false);
  assert.equal(istAufraeumzeit(Date.UTC(2026, 0, 1, 4, 0)), false);
});
