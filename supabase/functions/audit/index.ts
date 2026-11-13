import {
  actor,
  admin,
  body,
  checked,
  db,
  json,
  serve,
} from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  admin(p);
  const b = await body(req);
  const offset = Math.max(
    0,
    Math.min(100000, Math.floor(Number(b.offset) || 0)),
  );
  let q = db().from("audit_logs").select("*").order("created_at", {
    ascending: false,
  }).range(offset, offset + 49);
  if (typeof b.action === "string" && b.action.length <= 100 && b.action) {
    q = q.ilike("action", "%" + b.action.replace(/[%_]/g, "") + "%");
  }
  if (["started", "success", "failed", "pending"].includes(b.outcome)) {
    q = q.eq("outcome", b.outcome);
  }
  return json({ logs: checked(await q), offset });
});
