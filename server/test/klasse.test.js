import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, sign } from "node:crypto";
import { cloudCode, paketName, pruefeKlasse } from "../src/klasse.js";
import { behandle } from "../src/index.js";

const CODE = "ABCDE-23456";
const K = "K-" + CODE;

function schluesselPaar() {
  const { publicKey, privateKey } = generateKeyPairSync("ed25519");
  const raw = Buffer.from(publicKey.export({ format: "jwk" }).x, "base64url");
  return { pub: raw.toString("base64"), unterschreibe: (t) => sign(null, Buffer.from(t), privateKey).toString("base64") };
}

async function umgebung() {
  const { privateKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  return {
    CK_CONTAINER: "iCloud.x", CK_UMGEBUNG: "development", CK_KEY_ID: "kid",
    CK_PRIVATE_KEY: privateKey.export({ type: "pkcs8", format: "pem" }),
    GERAETE: { get: async () => null, put: async () => {} },
  };
}

const post = (body, ip) => new Request("https://x.test/v1/klasse", { method: "POST", headers: { "cf-connecting-ip": ip }, body: JSON.stringify(body) });

/** Ein kleines Gegenstück zu CloudKit: lookup liefert den Schlüssel (oder nichts), modify merkt sich den Befehl. */
function cloud(pub) {
  const gesehen = [];
  const holen = async (url, opt) => {
    gesehen.push({ url, body: opt.body });
    if (url.endsWith("records/lookup")) {
      return new Response(JSON.stringify({ records: [pub ? { recordName: "x", fields: { publicKey: { value: pub } } } : { recordName: "x", serverErrorCode: "NOT_FOUND" }] }), { status: 200 });
    }
    return new Response("{}", { status: 200 });
  };
  return { holen, gesehen };
}

const arbeit = JSON.stringify({ klasse: "Klasse 3", fach: "Mathe", arbeit: "Klassenarbeit Nr. 1", uebungen: [{ titel: "A", aufgaben: [{ art: "zahl", frage: "x", rechnung: "1 + 1 =", antwort: "2" }] }] });

test("Klassencode bekommt ein K- davor", () => {
  assert.equal(cloudCode("abcde23456"), K);
  assert.equal(cloudCode("nope"), null);
});

test("Paketname wie bei iOS", () => {
  assert.equal(paketName(K, "Klasse 3", "Mathe", "Klassenarbeit Nr. 1"), "paket-K-ABCDE-23456-klasse-3-mathe-klassenarbeit-nr-1");
  assert.equal(paketName(K, "Klasse 3", "Deutsch", "Wörter"), "paket-K-ABCDE-23456-klasse-3-deutsch-w-rter");
});

test("Ungültige Anfragen werden erkannt", () => {
  assert.ok(pruefeKlasse({ code: "x", art: "paket", daten: {} }).fehler);
  assert.ok(pruefeKlasse({ code: CODE, art: "schluessel", daten: { publicKey: "AAAA" } }).fehler);
  assert.ok(pruefeKlasse({ code: CODE, art: "paket", daten: { json: "{}", sig: "abc" } }).fehler);
  assert.ok(pruefeKlasse({ code: CODE, art: "loeschen", daten: { name: "paket-K-ZZZZZ-23456-x", sig: "s" } }).fehler);
});

test("Schlüssel anlegen: neu geht, derselbe noch einmal geht, ein anderer wird abgelehnt", async () => {
  const env = await umgebung();
  const a = schluesselPaar(), b = schluesselPaar();
  let c = cloud(null);
  assert.equal((await behandle(post({ code: CODE, art: "schluessel", daten: { publicKey: a.pub } }, "1.0.0.1"), env, c.holen)).status, 200);
  assert.ok(c.gesehen.at(-1).body.includes("KlassenSchluessel"));
  c = cloud(a.pub);
  assert.equal((await behandle(post({ code: CODE, art: "schluessel", daten: { publicKey: a.pub } }, "1.0.0.2"), env, c.holen)).status, 200);
  assert.equal((await behandle(post({ code: CODE, art: "schluessel", daten: { publicKey: b.pub } }, "1.0.0.3"), env, c.holen)).status, 409);
  assert.ok(!c.gesehen.some((g) => g.url.endsWith("records/modify")));
});

test("Paket: richtige Unterschrift wird geschrieben, falsche und fremde nicht", async () => {
  const env = await umgebung();
  const admin = schluesselPaar(), fremd = schluesselPaar();
  const c = cloud(admin.pub);
  const gut = { code: CODE, art: "paket", daten: { json: arbeit, sig: admin.unterschreibe(K + "|" + arbeit) } };
  assert.equal((await behandle(post(gut, "2.0.0.1"), env, c.holen)).status, 200);
  const modify = c.gesehen.find((g) => g.url.endsWith("records/modify"));
  assert.ok(modify.body.includes("forceReplace"));
  assert.ok(modify.body.includes("paket-K-ABCDE-23456-klasse-3-mathe-klassenarbeit-nr-1"));

  const c2 = cloud(admin.pub);
  const schlecht = { code: CODE, art: "paket", daten: { json: arbeit, sig: fremd.unterschreibe(K + "|" + arbeit) } };
  assert.equal((await behandle(post(schlecht, "2.0.0.2"), env, c2.holen)).status, 403);
  const geaendert = { code: CODE, art: "paket", daten: { json: arbeit.replace("1 + 1", "9 + 9"), sig: admin.unterschreibe(K + "|" + arbeit) } };
  assert.equal((await behandle(post(geaendert, "2.0.0.3"), env, c2.holen)).status, 403);
  assert.ok(!c2.gesehen.some((g) => g.url.endsWith("records/modify")));
});

test("Paket ohne Schlüssel in der Cloud wird abgelehnt", async () => {
  const env = await umgebung();
  const admin = schluesselPaar();
  const c = cloud(null);
  const q = { code: CODE, art: "paket", daten: { json: arbeit, sig: admin.unterschreibe(K + "|" + arbeit) } };
  assert.equal((await behandle(post(q, "3.0.0.1"), env, c.holen)).status, 403);
});

test("Löschen braucht die Unterschrift über den Paketnamen", async () => {
  const env = await umgebung();
  const admin = schluesselPaar(), fremd = schluesselPaar();
  const name = paketName(K, "Klasse 3", "Mathe", "Klassenarbeit Nr. 1");
  const c = cloud(admin.pub);
  assert.equal((await behandle(post({ code: CODE, art: "loeschen", daten: { name, sig: fremd.unterschreibe(K + "|loeschen|" + name) } }, "4.0.0.1"), env, c.holen)).status, 403);
  assert.equal((await behandle(post({ code: CODE, art: "loeschen", daten: { name, sig: admin.unterschreibe(K + "|loeschen|" + name) } }, "4.0.0.2"), env, c.holen)).status, 200);
  assert.ok(c.gesehen.at(-1).body.includes("forceDelete"));
});
