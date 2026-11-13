import {
  actor,
  audited,
  body,
  checked,
  db,
  HttpError,
  json,
  password,
  publicClient,
  serve,
  throttle,
} from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const b = await body(req);
  if (b.action === "refresh") {
    if (typeof b.refresh_token !== "string" || b.refresh_token.length > 4096) {
      throw new HttpError(400, "Invalid refresh token");
    }
    const { data, error } = await publicClient().auth.refreshSession({
      refresh_token: b.refresh_token,
    });
    if (error || !data.session) throw new HttpError(401, "Session expired");
    const p = checked(
      await db().from("profiles").select("*").eq("id", data.user!.id).single(),
    );
    if (!p || p.status !== "ACTIVE") {
      await db().auth.admin.signOut(data.session.access_token, "global");
      throw new HttpError(403, "Account unavailable");
    }
    return json({ session: data.session, profile: p });
  }
  const p = await actor(req, true);
  if (b.action === "me") return json({ profile: p });
  if (b.action === "logout") {
    const token = req.headers.get("Authorization")!.slice(7);
    const { error } = await db().auth.admin.signOut(token, "local");
    if (error) throw error;
    return json({ ok: true });
  }
  if (b.action !== "password") throw new HttpError(400, "Invalid action");
  const next = password(b.password);
  await throttle("password:" + p.id);
  if (next === b.current_password) {
    throw new HttpError(400, "Choose a new password");
  }
  const identity = await db().auth.admin.getUserById(p.id);
  if (identity.error || !identity.data.user?.email) {
    throw new HttpError(401, "Account unavailable");
  }
  const client = publicClient();
  const { data, error } = await client.auth.signInWithPassword({
    email: identity.data.user.email,
    password: b.current_password,
  });
  if (error || !data.session) {
    throw new HttpError(401, "Current password is incorrect");
  }
  try {
    await audited(p.id, "password_change", p.id, async () => {
      const { error } = await db().auth.admin.updateUserById(p.id, {
        password: next,
      });
      if (error) throw error;
      checked(
        await db().from("profiles").update({ must_change_password: false }).eq(
          "id",
          p.id,
        ),
      );
    });
  } finally {
    await db().auth.admin.signOut(data.session.access_token, "global");
  }
  return json({ ok: true, reauthenticate: true });
});
