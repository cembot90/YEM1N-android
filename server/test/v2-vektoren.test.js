import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { b64urlDekodiere, b64urlKodiere, geraetId, pubBytes, sha256Hex, unterschriftGilt, unterschriftText } from "../src/v2/krypto.js";

const v = JSON.parse(readFileSync(new URL("../../cloud/testvektoren-v2.json", import.meta.url), "utf8"));
const bytes = (hex) => Uint8Array.from(Buffer.from(hex, "hex"));

test("Testvektoren: Schlüssel, Kennung und Text zur Unterschrift stimmen", async () => {
  assert.equal(Buffer.from(pubBytes(v.geraet.pub)).toString("hex"), v.geraet.pubHex);
  assert.equal(await geraetId(v.geraet.pub), v.geraet.geraetId);
  assert.equal(Buffer.from(v.geraet.spkiBase64, "base64").subarray(-65).toString("hex"), v.geraet.pubHex);
  const a = v.anfrage;
  assert.equal(await sha256Hex(new TextEncoder().encode(a.body)), a.bodyHash);
  assert.equal(unterschriftText({ methode: a.methode, pfad: a.pfad, familie: a.familie, geraet: a.geraet, zeit: a.zeit, bodyHash: a.bodyHash }), a.nachricht);
});

test("Testvektoren: die Unterschrift gilt, jede Änderung macht sie ungültig", async () => {
  assert.equal(await unterschriftGilt(v.geraet.pub, v.anfrage.signatur, v.anfrage.nachricht), true);
  for (const u of v.ungueltig) assert.equal(await unterschriftGilt(v.geraet.pub, u.signatur, u.nachricht), false, u.grund);
});

test("Testvektoren: eine DER-Unterschrift (Android Keystore) wird zu rohem r||s", async () => {
  const roh = b64urlDekodiere(v.derUmrechnung.rohBase64url);
  assert.equal(roh.length, 64);
  assert.equal(await unterschriftGilt(v.geraet.pub, v.derUmrechnung.rohBase64url, v.anfrage.nachricht), true);
});

test("Testvektoren: base64url ohne Auffüllzeichen", () => {
  assert.equal(b64urlKodiere(bytes(v.base64url.hex)), v.base64url.kodiert);
  assert.equal(b64urlKodiere(bytes(v.base64url.hexZwei)), v.base64url.kodiertZwei);
  assert.equal(Buffer.from(b64urlDekodiere(v.base64url.kodiertZwei)).toString("hex"), v.base64url.hexZwei);
});
