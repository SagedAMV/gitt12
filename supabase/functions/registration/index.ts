import {
  audit,
  body,
  db,
  HttpError,
  json,
  password,
  publicThrottle,
  serve,
  username,
} from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const b = await body(req);
  const name = username(b.username);
  const pass = password(b.password);
  if (name === "sagedkhan746") throw new HttpError(400, "Username unavailable");
  await publicThrottle(req, "registration", name);
  const client = db();
  const { data, error } = await client.auth.admin.createUser({
    email: `${name}@users.vault.invalid`,
    password: pass,
    email_confirm: true,
  });
  if (error || !data.user) throw new HttpError(400, "Unable to create request");
  const id = data.user.id;
  try {
    const p = await client.from("profiles").insert({ id, username: name });
    if (p.error) throw p.error;
    const q = await client.from("registration_requests").insert({
      user_id: id,
    });
    if (q.error) throw q.error;
    await audit(id, "registration", id, "pending");
  } catch (e) {
    await client.auth.admin.deleteUser(id);
    throw e;
  }
  return json({ status: "PENDING" }, 201);
});
