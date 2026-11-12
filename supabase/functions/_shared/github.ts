import { importPKCS8, SignJWT } from "npm:jose@5.9.6";
import { env, HttpError } from "./core.ts";
let cached: { token: string; expires: number } | undefined;
async function token() {
  if (cached && cached.expires > Date.now() + 60000) return cached.token;
  const key = await importPKCS8(
    env("GITHUB_PRIVATE_KEY").replace(/\\n/g, "\n"),
    "RS256",
  );
  const jwt = await new SignJWT({}).setProtectedHeader({ alg: "RS256" })
    .setIssuer(env("GITHUB_APP_ID")).setIssuedAt(
      Math.floor(Date.now() / 1000) - 60,
    ).setExpirationTime("9m").sign(key);
  const response = await fetch(
    `https://api.github.com/app/installations/${
      env("GITHUB_INSTALLATION_ID")
    }/access_tokens`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
      },
      signal: AbortSignal.timeout(20000),
    },
  );
  if (!response.ok) throw new HttpError(502, "GitHub authentication failed");
  const data = await response.json();
  cached = { token: data.token, expires: Date.parse(data.expires_at) };
  return cached.token;
}
export async function githubResponse(
  path: string,
  method = "GET",
  body?: unknown,
  accept = "application/vnd.github+json",
) {
  const response = await fetch(`https://api.github.com${path}`, {
    method,
    body: body === undefined ? undefined : JSON.stringify(body),
    headers: {
      Authorization: `Bearer ${await token()}`,
      Accept: accept,
      "Content-Type": "application/json",
      "X-GitHub-Api-Version": "2022-11-28",
    },
    signal: AbortSignal.timeout(90000),
    redirect: "error",
  });
  if (!response.ok) {
    throw new HttpError(
      response.status === 404
        ? 404
        : response.status === 409 || response.status === 422
        ? 409
        : 502,
      "GitHub request failed",
    );
  }
  return response;
}
export async function github(path: string, method = "GET", body?: unknown) {
  return (await githubResponse(path, method, body)).json();
}
export const base = (r: { owner: string; name: string }) =>
  `/repos/${encodeURIComponent(r.owner)}/${encodeURIComponent(r.name)}`;
export type TreeItem = {
  path: string;
  sha: string;
  type: string;
  mode: string;
  size?: number;
};
export async function snapshot(
  r: { owner: string; name: string; default_branch: string },
  refSha?: string,
) {
  const b = base(r);
  const ref = await github(
    `${b}/git/ref/heads/${
      r.default_branch.split("/").map(encodeURIComponent).join("/")
    }`,
  );
  const commit = await github(`${b}/git/commits/${refSha ?? ref.object.sha}`);
  const result = await github(`${b}/git/trees/${commit.tree.sha}?recursive=1`);
  if (result.truncated || result.tree.length > 10000) {
    throw new HttpError(413, "Repository tree too large");
  }
  return {
    head: ref.object.sha as string,
    treeSha: commit.tree.sha as string,
    items: result.tree as TreeItem[],
  };
}
export async function commitTree(
  r: { owner: string; name: string; default_branch: string },
  snap: { head: string; treeSha: string },
  tree: unknown[],
  message: string,
) {
  const b = base(r);
  const t = await github(`${b}/git/trees`, "POST", {
    base_tree: snap.treeSha,
    tree,
  });
  const commit = await github(`${b}/git/commits`, "POST", {
    message,
    tree: t.sha,
    parents: [snap.head],
  });
  // Never force: competing changes are rejected rather than overwritten.
  await github(
    `${b}/git/refs/heads/${
      r.default_branch.split("/").map(encodeURIComponent).join("/")
    }`,
    "PATCH",
    { sha: commit.sha, force: false },
  );
  return commit.sha;
}
