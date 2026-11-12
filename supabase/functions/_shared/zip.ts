// ZIP32 stored entries, streaming with data descriptors; bounded by caller.
const encoder = new TextEncoder();
const table = Uint32Array.from({ length: 256 }, (_, n) => {
  for (let k = 0; k < 8; k++) n = n & 1 ? 0xedb88320 ^ (n >>> 1) : n >>> 1;
  return n >>> 0;
});
function header(size: number, values: [number, number, number][]) {
  const b = new Uint8Array(size);
  const v = new DataView(b.buffer);
  for (const [o, n, width] of values) {
    width === 2 ? v.setUint16(o, n, true) : v.setUint32(o, n >>> 0, true);
  }
  return b;
}
export async function* zipStream(
  items: { path: string; sha: string }[],
  open: (
    item: { path: string; sha: string },
  ) => Promise<ReadableStream<Uint8Array>>,
  maxBytes = 200 * 1024 * 1024,
): AsyncGenerator<Uint8Array> {
  const central: Uint8Array[] = [];
  let offset = 0, total = 0;
  for (const item of items) {
    const name = encoder.encode(item.path);
    const start = offset;
    const h = header(30, [[0, 0x04034b50, 4], [4, 20, 2], [6, 0x808, 2], [
      8,
      0,
      2,
    ], [26, name.length, 2]]);
    yield h;
    yield name;
    offset += h.length + name.length;
    let size = 0, crc = 0xffffffff;
    const reader = (await open(item)).getReader();
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        size += value.length;
        total += value.length;
        if (total > maxBytes) throw new Error("Archive too large");
        for (const byte of value) crc = table[(crc ^ byte) & 255] ^ (crc >>> 8);
        yield value;
        offset += value.length;
      }
    } finally {
      await reader.cancel().catch(() => {});
      reader.releaseLock();
    }
    crc = (crc ^ 0xffffffff) >>> 0;
    const desc = header(16, [[0, 0x08074b50, 4], [4, crc, 4], [8, size, 4], [
      12,
      size,
      4,
    ]]);
    yield desc;
    offset += 16;
    const c = header(46, [
      [0, 0x02014b50, 4],
      [4, 20, 2],
      [6, 20, 2],
      [8, 0x808, 2],
      [16, crc, 4],
      [20, size, 4],
      [24, size, 4],
      [28, name.length, 2],
      [42, start, 4],
    ]);
    central.push(c, name);
  }
  const centralStart = offset;
  for (const c of central) {
    yield c;
    offset += c.length;
  }
  yield header(22, [
    [0, 0x06054b50, 4],
    [8, items.length, 2],
    [10, items.length, 2],
    [12, offset - centralStart, 4],
    [16, centralStart, 4],
  ]);
}
export function generatorStream(
  iterator: AsyncGenerator<Uint8Array>,
  finished: (ok: boolean) => Promise<void>,
) {
  let ended = false;
  return new ReadableStream<Uint8Array>({
    async pull(c) {
      try {
        const n = await iterator.next();
        if (n.done) {
          ended = true;
          await finished(true);
          c.close();
        } else c.enqueue(n.value);
      } catch (e) {
        ended = true;
        await iterator.return(undefined).catch(() => {});
        await finished(false).catch(() => {});
        c.error(e);
      }
    },
    async cancel() {
      if (!ended) {
        ended = true;
        await iterator.return(undefined).catch(() => {});
        await finished(false);
      }
    },
  });
}
