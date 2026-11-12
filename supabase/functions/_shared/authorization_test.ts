// Offline HTTP-level tests: Auth and PostgREST are mocked. Not a live Supabase integration test.
Deno.env.set("VAULT_TEST", "1");
Deno.env.set("SUPABASE_URL", "https://example.supabase.co");
Deno.env.set("SUPABASE_SERVICE_ROLE_KEY", "test-service-only");
Deno.env.set("SUPABASE_ANON_KEY", "test-public");
Deno.env.set("GITHUB_OWNER", "owner");
const endpoints = await Promise.all(
  [
    "admin-users",
    "admin-repositories",
    "admin-items",
    "audit",
    "uploads",
    "files",
    "downloads",
    "repositories",
  ].map(async (name) => ({
    name,
    handler: (await import(`../${name}/index.ts`)).handler as (
      req: Request,
    ) => Promise<Response>,
  })),
);
const id = "22222222-2222-4222-8222-222222222222";
let role = "USER", status = "ACTIVE", mustChange = false, grant = false;
let githubCalls = 0;
const originalFetch = globalThis.fetch;
globalThis.fetch = async (input) => {
  const u = new URL(input instanceof Request ? input.url : String(input));
  let data: unknown;
  if (u.hostname === "api.github.com") {
    githubCalls++;
    throw new Error("Should not reach GitHub");
  }
  if (u.pathname === "/auth/v1/user") {
    data = { id, email: "user@users.vault.invalid" };
  } else if (u.pathname === "/rest/v1/profiles") {
    data = {
      id,
      username: "user",
      role,
      status,
      must_change_password: mustChange,
    };
  } else if (u.pathname === "/rest/v1/repositories") {
    data = {
      id: "repo",
      owner: "owner",
      name: "private",
      active: true,
      default_branch: "main",
    };
  } else if (u.pathname === "/rest/v1/user_repository_access") {
    data = grant ? { user_id: id } : null;
  } else throw new Error("Unexpected mocked route " + u.pathname);
  return new Response(JSON.stringify(data), {
    headers: { "Content-Type": "application/json" },
  });
};
function request(auth = true) {
  return new Request("https://edge.invalid/function", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(auth ? { Authorization: "Bearer valid-mock-token" } : {}),
    },
    body: JSON.stringify({
      repository_id: "repo",
      path: "secret.txt",
      paths: ["secret.txt"],
      action: "list",
    }),
  });
}
for (const endpoint of endpoints) {
  Deno.test(`${endpoint.name}: denies missing session`, async () => {
    const res = await endpoint.handler(request(false));
    if (res.status !== 401) throw new Error("Expected 401, got " + res.status);
    await res.text();
  });
  if (
    endpoint.name.startsWith("admin-") || endpoint.name === "audit" ||
    endpoint.name === "uploads"
  ) {
    Deno.test(`${endpoint.name}: rejects USER`, async () => {
      role = "USER";
      status = "ACTIVE";
      mustChange = false;
      const res = await endpoint.handler(request());
      if (res.status !== 403) {
        throw new Error("Expected 403, got " + res.status);
      }
      await res.text();
    });
  }
  for (const account of ["PENDING", "DISABLED", "REJECTED", "TEMPORARY"]) {
    Deno.test(`${endpoint.name}: blocks ${account}`, async () => {
      role = "USER";
      status = account === "TEMPORARY" ? "ACTIVE" : account;
      mustChange = account === "TEMPORARY";
      const res = await endpoint.handler(request());
      if (res.status !== 403) {
        throw new Error("Expected 403, got " + res.status);
      }
      await res.text();
    });
  }
}
for (const name of ["files", "downloads"]) {
  Deno.test(`${name}: denies unauthorized repository ID`, async () => {
    role = "USER";
    status = "ACTIVE";
    mustChange = false;
    grant = false;
    const res = await endpoints.find((e) => e.name === name)!.handler(
      request(),
    );
    if (res.status !== 404) throw new Error("Expected 404, got " + res.status);
    await res.text();
  });
}
Deno.test("blocked requests never call GitHub", () => {
  if (githubCalls !== 0) throw new Error("GitHub authorization bypass");
});
Deno.test("restore fetch", () => {
  globalThis.fetch = originalFetch;
});
