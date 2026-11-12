import { createClient } from "npm:@supabase/supabase-js@2.49.1";
export const env = (key: string) => {
  const v = Deno.env.get(key);
  if (!v) throw new Error(`Missing environment: ${key}`);
  return v;
};
export const db = () =>
  createClient(env("SUPABASE_URL"), env("SUPABASE_SERVICE_ROLE_KEY"), {
    auth: { persistSession: false, autoRefreshToken: false },
  });
export const publicClient = () =>
  createClient(env("SUPABASE_URL"), env("SUPABASE_ANON_KEY"), {
    auth: { persistSession: false, autoRefreshToken: false },
  });
export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}
export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
export function serve(fn: (req: Request) => Promise<Response>) {
  const handler = async (req: Request): Promise<Response> => {
    try {
      if (req.method !== "POST") throw new HttpError(405, "POST required");
      return await fn(req);
    } catch (e) {
      return json({
        error: e instanceof HttpError ? e.message : "Internal error",
      }, e instanceof HttpError ? e.status : 500);
    }
  };
  if (Deno.env.get("VAULT_TEST") !== "1") Deno.serve(handler);
  return handler;
}
export async function readBounded(
  req: Request,
  limit = 16384,
): Promise<Uint8Array> {
  if (Number(req.headers.get("content-length") ?? 0) > limit) {
    throw new HttpError(413, "Request too large");
  }
  const reader = req.body?.getReader();
  if (!reader) return new Uint8Array();
  const chunks: Uint8Array[] = [];
  let count = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      count += value.length;
      if (count > limit) {
        await reader.cancel();
        throw new HttpError(413, "Request too large");
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }
  const result = new Uint8Array(count);
  let offset = 0;
  for (const c of chunks) {
    result.set(c, offset);
    offset += c.length;
  }
  return result;
}
export async function body(req: Request) {
  const bytes = await readBounded(req);
  try {
    const b = JSON.parse(new TextDecoder().decode(bytes));
    if (!b || Array.isArray(b) || typeof b !== "object") throw new Error();
    return b;
  } catch {
    throw new HttpError(400, "Invalid JSON");
  }
}
export function username(value: unknown) {
  if (typeof value !== "string" || !/^[a-z0-9_]{3,32}$/.test(value)) {
    throw new HttpError(400, "Invalid username");
  }
  return value;
}
export function password(value: unknown) {
  if (typeof value !== "string" || value.length < 12 || value.length > 128) {
    throw new HttpError(400, "Password must be 12–128 characters");
  }
  return value;
}
export async function audit(
  actor: string | null,
  action: string,
  target: string | null,
  outcome: string,
) {
  const { error } = await db().from("audit_logs").insert({
    actor_id: actor,
    action,
    target,
    outcome,
  });
  if (error) throw new Error("Audit write failed");
}
export async function audited<T>(
  id: string,
  action: string,
  target: string,
  fn: () => Promise<T>,
): Promise<T> {
  await audit(id, action, target, "started");
  try {
    const result = await fn();
    await audit(id, action, target, "success");
    return result;
  } catch (e) {
    await audit(id, action, target, "failed");
    throw e;
  }
}
export async function actor(req: Request, allowPasswordChange = false) {
  const token = req.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) throw new HttpError(401, "Authentication required");
  const client = db();
  const { data: { user }, error } = await client.auth.getUser(token);
  if (error || !user) throw new HttpError(401, "Invalid session");
  const { data: p } = await client.from("profiles").select("*").eq(
    "id",
    user.id,
  ).single();
  if (!p || p.status !== "ACTIVE") {
    throw new HttpError(403, "Account unavailable");
  }
  if (p.must_change_password && !allowPasswordChange) {
    throw new HttpError(403, "Password change required");
  }
  return p;
}
export function admin(p: { role: string }) {
  if (p.role !== "ADMIN") throw new HttpError(403, "Admin required");
}
export async function repository(p: { id: string; role: string }, id: string) {
  if (typeof id !== "string") throw new HttpError(400, "Repository required");
  const client = db();
  const { data: r } = await client.from("repositories").select("*").eq("id", id)
    .eq("active", true).single();
  if (!r) throw new HttpError(404, "Repository not found");
  if (p.role !== "ADMIN") {
    const { data: a } = await client.from("user_repository_access").select(
      "user_id",
    ).eq("user_id", p.id).eq("repository_id", id).maybeSingle();
    if (!a) throw new HttpError(404, "Repository not found");
  }
  if (r.owner !== env("GITHUB_OWNER")) {
    throw new HttpError(403, "Owner not allowed");
  }
  return r;
}
export function safePath(value: unknown) {
  if (
    typeof value !== "string" || value.length > 1024 || value.startsWith("/") ||
    value.includes("\\") || value.includes(":") ||
    /[\x00-\x1f\x7f]/.test(value) ||
    value.split("/").some((s) =>
      s === ".." || s === "." || (s === "" && value !== "")
    )
  ) throw new HttpError(400, "Invalid path");
  return value;
}
export async function throttle(name: string) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(env("RATE_LIMIT_SECRET")),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const hash = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(name),
  );
  const { data, error } = await db().rpc("consume_attempt", {
    p_key: Array.from(new Uint8Array(hash)).map((x) =>
      x.toString(16).padStart(2, "0")
    ).join(""),
  });
  if (error || !data) throw new HttpError(429, "Try again later");
}
// forwarded-for is trusted only behind the Supabase gateway; never log its raw value.
export async function publicThrottle(req: Request, kind: string, name: string) {
  await throttle(kind + ":name:" + name);
  await throttle(
    kind + ":ip:" +
      (req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "unknown"),
  );
}
export function checked<T>(result: { data: T; error: unknown }): T {
  if (result.error) throw new Error("Database operation failed");
  return result.data;
}
