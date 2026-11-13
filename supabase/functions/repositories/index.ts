import { actor, db, json, serve } from "../_shared/core.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  const client = db();
  let query = client.from("repositories").select(
    "id,owner,name,default_branch,active",
  ).eq("active", true).order("name");
  if (p.role !== "ADMIN") {
    const { data, error } = await client.from("user_repository_access").select(
      "repository_id",
    ).eq("user_id", p.id);
    if (error) throw error;
    query = query.in("id", (data ?? []).map((a) => a.repository_id));
  }
  const { data, error } = await query;
  if (error) throw error;
  return json({ repositories: data });
});
