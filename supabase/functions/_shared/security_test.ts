import { admin, password, safePath, username } from "./core.ts";
import { zipStream } from "./zip.ts";
function expectThrow(fn: () => unknown) {
  let threw = false;
  try {
    fn();
  } catch {
    threw = true;
  }
  if (!threw) throw new Error("Expected rejection");
}
Deno.test("reject traversal paths", () => {
  for (
    const p of [
      "../secret",
      "/etc/passwd",
      "x/../y",
      "x\\y",
      "x//y",
      "x:y",
      "x\u0000",
    ]
  ) expectThrow(() => safePath(p));
});
Deno.test("allow Unicode paths", () => {
  if (safePath("مجلد/ملف.pdf") !== "مجلد/ملف.pdf") throw new Error();
});
Deno.test("validate credentials and role", () => {
  expectThrow(() => username("ADMIN@x"));
  expectThrow(() => password("123"));
  expectThrow(() => admin({ role: "USER" }));
  admin({ role: "ADMIN" });
});
Deno.test("stream ZIP records", async () => {
  const chunks = [];
  for await (
    const c of zipStream(
      [{ path: "hello.txt", sha: "x" }],
      async () => new Blob(["hello"]).stream(),
    )
  ) chunks.push(c);
  const b = new Uint8Array(
    await new Blob(chunks.map((c) => new Uint8Array(c))).arrayBuffer(),
  );
  const v = new DataView(b.buffer);
  if (
    v.getUint32(0, true) !== 0x04034b50 ||
    v.getUint32(b.length - 22, true) !== 0x06054b50
  ) throw new Error("Invalid ZIP");
});
