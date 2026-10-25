// Das Durable Object je Familie. Dünne Hülle um den Kern (familie.js), die Cloudflare-Teile hineinreicht.
import { DurableObject } from "cloudflare:workers";
import { FamilieKern } from "./familie.js";
import { pushVersand } from "./push.js";

export class Familie extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.kern = new FamilieKern({
      id: ctx.id.toString(),
      jurisdiktion: ctx.id.jurisdiction || null,
      sql: ctx.storage.sql,
      transaktion: (f) => ctx.storage.transactionSync(f),
      alarm: { setzen: (t) => ctx.storage.setAlarm(t) },
      alleLoeschen: async () => { await ctx.storage.deleteAlarm(); await ctx.storage.deleteAll(); },
      pusher: pushVersand(env),
    });
  }

  async fetch(request) {
    return this.kern.behandle(request);
  }

  async alarm() {
    await this.kern.alarm();
  }
}
