import {
  actor,
  admin,
  audited,
  body,
  checked,
  db,
  env,
  HttpError,
  json,
  serve,
} from "../_shared/core.ts";
import { github } from "../_shared/github.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  admin(p);
  const b = await body(req);
  const c = db();
  if (b.action === "list") {
    return json({
      repositories: checked(
        await c.from("repositories").select("*").order("name"),
      ),
    });
  }
  if (b.action === "available") {
    const page = Math.max(1, Math.min(100, Number(b.page) || 1));
    const result = await github(
      `/installation/repositories?per_page=100&page=${page}`,
    );
    return json({
      repositories: result.repositories.filter((
        r: { owner: { login: string } },
      ) => r.owner.login.toLowerCase() === env("GITHUB_OWNER").toLowerCase())
        .map((r: { id: number; name: string; default_branch: string }) => ({
          github_id: r.id,
          name: r.name,
          default_branch: r.default_branch,
        })),
      page,
    });
  }
  if (b.action === "link") {
    if (typeof b.name !== "string" || !/^[A-Za-z0-9_.-]{1,100}$/.test(b.name)) {
      throw new HttpError(400, "Invalid name");
    }
    const r = await github(
      `/repos/${encodeURIComponent(env("GITHUB_OWNER"))}/${
        encodeURIComponent(b.name)
      }`,
    );
    if (r.owner.login.toLowerCase() !== env("GITHUB_OWNER").toLowerCase()) {
      throw new HttpError(403, "Owner not allowed");
    }
    return json(
      await audited(p.id, "repository_link", r.name, async () => {
        checked(
          await c.from("repositories").upsert({
            github_id: r.id,
            owner: env("GITHUB_OWNER"),
            name: r.name,
            default_branch: r.default_branch,
            active: true,
          }, { onConflict: "github_id" }),
        );
        return { ok: true };
      }),
    );
  }
  const r = checked(
    await c.from("repositories").select("*").eq("id", b.repository_id).single(),
  );
  if (!r) throw new HttpError(404, "Repository not found");
  return json(
    await audited(p.id, "repository_" + b.action, r.id, async () => {
      if (b.action === "unlink") {
        checked(await c.from("repositories").delete().eq("id", r.id));
      } else if (b.action === "status" && typeof b.active === "boolean") {
        checked(
          await c.from("repositories").update({ active: b.active }).eq(
            "id",
            r.id,
          ),
        );
      } else throw new HttpError(400, "Invalid action");
      return { ok: true };
    }),
  );
});
