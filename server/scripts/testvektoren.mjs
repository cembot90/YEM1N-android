// Erzeugt cloud/testvektoren-v2.json: feste Beispiele, damit Swift, Kotlin und der Worker dieselbe Unterschrift lesen.
// Aufruf: node scripts/testvektoren.mjs > ../cloud/testvektoren-v2.json
// Der Schlüssel hier ist nur ein Beispiel für Tests und schützt nichts.
import { createPrivateKey, createPublicKey, sign } from "node:crypto";
import { b64urlKodiere, geraetId, sha256Hex, unterschriftGilt, unterschriftText } from "../src/v2/krypto.js";

// Fester Beispielschlüssel (P-256), nur für Tests.
const jwk = {
  kty: "EC", crv: "P-256",
  x: "AIGFdZDAxDDG9ZYUvkoKY6B6FmUpKVIROGe3l2eL0Zs",
  y: "6lYOFvMdTdEqH6IIeZXNMevq-P81VXlRUs9HlNCEYow",
  d: "ElWT4sxruXsfZoDDSe-2wiAwrBdATfkUCUtJ2DDatrg",
};
const roh = createPrivateKey({ key: jwk, format: "jwk" });
const oeffentlich = createPublicKey(roh);
const pubJwk = oeffentlich.export({ format: "jwk" });
const pubBytes = Buffer.concat([Buffer.from([4]), Buffer.from(pubJwk.x, "base64url"), Buffer.from(pubJwk.y, "base64url")]);
const pub = b64urlKodiere(pubBytes);

const familie = "0123456789abcdef".repeat(4);
const zeit = 1800000000000;
const body = JSON.stringify({ typ: "frage", daten: { id: "frage-0001", kind: "Yemin", text: "Was ist 56 : 7?" } });
const geraet = await geraetId(pub);
const nachricht = unterschriftText({ methode: "POST", pfad: "/v2/senden", familie, geraet, zeit, bodyHash: await sha256Hex(new TextEncoder().encode(body)) });
const sig = b64urlKodiere(sign("sha256", Buffer.from(nachricht), { key: roh, dsaEncoding: "ieee-p1363" }));
const derSig = sign("sha256", Buffer.from(nachricht), { key: roh, dsaEncoding: "der" });

// DER nach rohem r||s (so muss es auch in Kotlin gemacht werden)
function derZuRoh(der) {
  let i = 2; if (der[1] & 0x80) i += der[1] & 0x7f;
  const lies = () => { i += 1; const n = der[i]; i += 1; let w = der.subarray(i, i + n); i += n; while (w.length > 32 && w[0] === 0) w = w.subarray(1); return Buffer.concat([Buffer.alloc(32 - w.length), w]); };
  const r = lies(), s = lies();
  return Buffer.concat([r, s]);
}
const rohAusDer = derZuRoh(derSig);
if (!(await unterschriftGilt(pub, b64urlKodiere(rohAusDer), nachricht))) throw new Error("DER-Umrechnung stimmt nicht");
if (!(await unterschriftGilt(pub, sig, nachricht))) throw new Error("Unterschrift stimmt nicht");

const spki = oeffentlich.export({ type: "spki", format: "der" });
const ausgabe = {
  hinweis: "Beispiele für Protokoll 2. Der Schlüssel ist nur für Tests. Siehe cloud/PROTOKOLL-V2.md.",
  geraet: {
    privatPkcs8Base64: roh.export({ type: "pkcs8", format: "der" }).toString("base64"),
    pub, pubHex: pubBytes.toString("hex"), spkiBase64: spki.toString("base64"), geraetId: geraet,
  },
  anfrage: {
    methode: "POST", pfad: "/v2/senden", familie, geraet, zeit, body,
    bodyHash: await sha256Hex(new TextEncoder().encode(body)), nachricht, signatur: sig,
  },
  derUmrechnung: { derBase64: derSig.toString("base64"), derHex: derSig.toString("hex"), rohBase64url: b64urlKodiere(rohAusDer) },
  base64url: { hex: "fbff", kodiert: "-_8", hexZwei: "00fafbfcfdfeff0102", kodiertZwei: "APr7_P3-_wEC" },
  ungueltig: [
    { grund: "anderer Inhalt", nachricht: nachricht.replace("\n" + (await sha256Hex(new TextEncoder().encode(body))), "\n" + (await sha256Hex(new TextEncoder().encode(body + " ")))), signatur: sig },
    { grund: "andere Zeit", nachricht: nachricht.replace(String(zeit), String(zeit + 1)), signatur: sig },
    { grund: "anderer Weg", nachricht: nachricht.replace("/v2/senden", "/v2/lesen"), signatur: sig },
    { grund: "andere Familie", nachricht: nachricht.replace(familie, "f".repeat(64)), signatur: sig },
  ],
};
process.stdout.write(JSON.stringify(ausgabe, null, 2) + "\n");
