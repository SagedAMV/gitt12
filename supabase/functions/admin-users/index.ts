import {
  actor,
  admin,
  audited,
  body,
  checked,
  db,
  HttpError,
  json,
  password,
  serve,
  username,
} from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  admin(p);
  const b = await body(req);
  const c = db();
  if (b.action === "list") {
    return json({
      users: checked(
        await c.from("profiles").select("*").order("created_at", {
          ascending: false,
        }).limit(500),
      ),
      requests: checked(
        await c.from("registration_requests").select("*").eq(
          "status",
          "PENDING",
        ),
      ),
      access: checked(await c.from("user_repository_access").select("*")),
    });
  }
  if (b.action === "create") {
    const name = username(b.username);
    const pass = password(b.password);
    if (name === "sagedkhan746") throw new HttpError(409, "Reserved username");
    return json(
      await audited(p.id, "user_create", name, async () => {
        const { data, error } = await c.auth.admin.createUser({
          email: `${name}@users.vault.invalid`,
          password: pass,
          email_confirm: true,
        });
        if (error || !data.user) {
          throw new HttpError(409, "Unable to create user");
        }
        try {
          checked(
            await c.from("profiles").insert({
              id: data.user.id,
              username: name,
              status: "ACTIVE",
              must_change_password: true,
            }),
          );
        } catch (e) {
          await c.auth.admin.deleteUser(data.user.id);
          throw e;
        }
        return { ok: true };
      }),
    );
  }
  const target = checked(
    await c.from("profiles").select("*").eq("id", b.user_id).single(),
  );
  if (!target || target.role === "ADMIN") {
    throw new HttpError(403, "Cannot modify administrator");
  }
  return json(
    await audited(p.id, "user_" + b.action, target.id, async () => {
      if (b.action === "review") {
        if (typeof b.approve !== "boolean") {
          throw new HttpError(400, "Decision required");
        }
        checked(
          await c.rpc("review_registration", {
            p_user: target.id,
            p_admin: p.id,
            p_approve: b.approve,
          }),
        );
      } else if (b.action === "status") {
        if (
          !["ACTIVE", "DISABLED"].includes(b.status) ||
          !["ACTIVE", "DISABLED"].includes(target.status)
        ) {
          throw new HttpError(400, "Invalid status transition");
        }
        checked(
          await c.from("profiles").update({ status: b.status }).eq(
            "id",
            target.id,
          ),
        );
      } else if (b.action === "reset") {
        const next = password(b.password);
        // Block functions/RLS before changing the credential, failing closed if Auth fails.
        checked(
          await c.from("profiles").update({ must_change_password: true }).eq(
            "id",
            target.id,
          ),
        );
        const { error } = await c.auth.admin.updateUserById(target.id, {
          password: next,
        });
        if (error) throw error;
      } else if (b.action === "grant" || b.action === "revoke") {
        if (
          !checked(
            await c.from("repositories").select("id").eq("id", b.repository_id)
              .maybeSingle(),
          )
        ) throw new HttpError(404, "Repository not found");
        if (b.action === "grant") {
          checked(
            await c.from("user_repository_access").upsert({
              user_id: target.id,
              repository_id: b.repository_id,
            }),
          );
        } else {checked(
            await c.from("user_repository_access").delete().eq(
              "user_id",
              target.id,
            ).eq("repository_id", b.repository_id),
          );}
      } else throw new HttpError(400, "Invalid action");
      return { ok: true };
    }),
  );
});
