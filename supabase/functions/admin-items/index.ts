import {
  actor,
  admin,
  audited,
  body,
  HttpError,
  json,
  repository,
  safePath,
  serve,
} from "../_shared/core.ts";
import { commitTree, snapshot } from "../_shared/github.ts";
export const handler = serve(async (req) => {
  const p = await actor(req);
  admin(p);
  const b = await body(req);
  const r = await repository(p, b.repository_id);
  const source = safePath(b.path);
  if (!source || source === ".git" || source.startsWith(".git/")) {
    throw new HttpError(400, "Invalid path");
  }
  return json(
    await audited(p.id, "items_" + b.action, r.id + ":" + source, async () => {
      const snap = await snapshot(r);
      const blobs = snap.items.filter((e) => e.type === "blob");
      if (b.action === "mkdir") {
        if (
          snap.items.some((e) =>
            e.path === source || e.path.startsWith(source + "/")
          )
        ) throw new HttpError(409, "Path already exists");
        const sha = await commitTree(r, snap, [{
          path: source + "/.gitkeep",
          mode: "100644",
          type: "blob",
          content: "",
        }], "Create folder " + source);
        return { ok: true, sha };
      }
      const selected = blobs.filter((e) =>
        e.path === source || e.path.startsWith(source + "/")
      );
      if (!selected.length) throw new HttpError(404, "Path not found");
      if (selected.length > 1000) throw new HttpError(413, "Too many files");
      const changes: unknown[] = selected.map((e) => ({
        path: e.path,
        mode: e.mode,
        type: "blob",
        sha: null,
      }));
      if (b.action === "move") {
        const dest = safePath(b.destination);
        if (
          !dest || dest === source || dest.startsWith(source + "/") ||
          dest === ".git" || dest.startsWith(".git/")
        ) throw new HttpError(400, "Invalid destination");
        if (
          snap.items.some((e) =>
            e.path === dest || e.path.startsWith(dest + "/") ||
            (e.type === "blob" && dest.startsWith(e.path + "/"))
          )
        ) throw new HttpError(409, "Destination exists");
        for (const e of selected) {
          changes.push({
            path: dest + e.path.slice(source.length),
            mode: e.mode,
            type: "blob",
            sha: e.sha,
          });
        }
      } else if (b.action !== "delete") {
        throw new HttpError(400, "Invalid action");
      }
      return {
        ok: true,
        sha: await commitTree(
          r,
          snap,
          changes,
          (b.action === "delete" ? "Delete " : "Move ") + source,
        ),
      };
    }),
  );
});
