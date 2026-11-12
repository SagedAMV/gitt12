import {
  actor,
  admin,
  audited,
  HttpError,
  json,
  readBounded,
  repository,
  safePath,
  serve,
} from "../_shared/core.ts";
import { base, commitTree, github, snapshot } from "../_shared/github.ts";
// Conservative verified-in-code cap, NOT a claim that 90 MiB is supported by Edge Functions.
const LIMIT = 8 * 1024 * 1024;
export const handler = serve(async (req) => {
  const p = await actor(req);
  admin(p);
  const u = new URL(req.url);
  const r = await repository(p, u.searchParams.get("repository_id")!);
  const path = safePath(u.searchParams.get("path"));
  if (!path || path === ".git" || path.startsWith(".git/")) {
    throw new HttpError(400, "Invalid path");
  }
  return json(
    await audited(p.id, "upload", r.id + ":" + path, async () => {
      const bytes = await readBounded(req, LIMIT);
      const snap = await snapshot(r);
      const current = snap.items.find((e) => e.path === path);
      const expected = req.headers.get("x-expected-sha");
      if (
        current && (!expected || expected !== current.sha)
      ) {
        throw new HttpError(409, "File changed: refresh and confirm overwrite");
      }
      if (
        snap.items.some((e) =>
          e.type === "tree" && e.path === path ||
          e.type === "blob" && path.startsWith(e.path + "/")
        )
      ) {
        throw new HttpError(409, "Path conflicts with directory");
      }
      let binary = "";
      for (let i = 0; i < bytes.length; i += 16384) {
        binary += String.fromCharCode(...bytes.subarray(i, i + 16384));
      }
      const blob = await github(base(r) + "/git/blobs", "POST", {
        content: btoa(binary),
        encoding: "base64",
      });
      const sha = await commitTree(r, snap, [{
        path,
        mode: "100644",
        type: "blob",
        sha: blob.sha,
      }], "Upload " + path);
      return { ok: true, sha, blob_sha: blob.sha };
    }),
  );
});
