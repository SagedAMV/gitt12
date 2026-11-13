import { createClient } from "npm:@supabase/supabase-js@2.49.1";
import {
  audit,
  body,
  db,
  env,
  HttpError,
  json,
  publicThrottle,
  serve,
  username,
} from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const b = await body(req);
  const name = username(b.username);
  await publicThrottle(req, "login", name);
  if (typeof b.password !== "string" || b.password.length > 128) {
    throw new HttpError(400, "Invalid credentials");
  }
  const { data: p } = await db().from("profiles").select("*").eq(
    "username",
    name,
  ).maybeSingle();
  // Resolve the Auth email by trusted profile ID. Never accept a client-supplied role or email mapping.
  const identity = p ? await db().auth.admin.getUserById(p.id) : null;
  const authEmail = identity?.data.user?.email ?? `${name}@users.vault.invalid`;
  const client = createClient(env("SUPABASE_URL"), env("SUPABASE_ANON_KEY"), {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await client.auth.signInWithPassword({
    email: authEmail,
    password: b.password,
  });
  if (error || !p || p.status !== "ACTIVE") {
    if (data.session) {
      await db().auth.admin.signOut(data.session.access_token, "global");
    }
    await audit(p?.id ?? null, "login", null, "failed");
    throw new HttpError(401, "Invalid credentials or account unavailable");
  }
  await audit(p.id, "login", null, "success");
  return json({ session: data.session, profile: p });
});
