import {
  actor,
  body,
  HttpError,
  json,
  repository,
  safePath,
  serve,
} from "../_shared/core.ts";
import { snapshot } from "../_shared/github.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  const b = await body(req);
  const r = await repository(p, b.repository_id);
  const path = safePath(b.path ?? "");
  if (b.ref && (typeof b.ref !== "string" || !/^[0-9a-f]{40}$/.test(b.ref))) {
    throw new HttpError(400, "Invalid commit");
  }
  const snap = await snapshot(r, b.action === "versions" ? b.ref : undefined);
  if (b.action === "versions") {
    return json({
      versions: Object.fromEntries(
        snap.items.filter((e) => e.type === "blob" && e.mode !== "120000").map(
          (e) => [e.path, e.sha],
        ),
      ),
    });
  }
  if (path && !snap.items.some((e) => e.path === path && e.type === "tree")) {
    throw new HttpError(404, "Directory not found");
  }
  const prefix = path ? path + "/" : "";
  return json({
    items: snap.items.filter((e) =>
      (e.type === "tree" || e.type === "blob" && e.mode !== "120000") &&
      e.path.startsWith(prefix) && !e.path.slice(prefix.length).includes("/")
    ).map((e) => ({
      name: e.path.slice(prefix.length),
      path: e.path,
      type: e.type === "tree" ? "dir" : "file",
      size: e.size ?? 0,
      sha: e.sha,
    })),
  });
});
