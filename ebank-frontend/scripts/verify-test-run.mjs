// Fails the build if Vitest did not run every test file present on disk.
// Observed once in seven runs on Windows right after `npm ci`: Vitest reported success after collecting
// only 7 of 14 files. A quality gate must not pass silently with half the tests, so this cross-check
// compares the JSON report with an independent listing of *.test.js(x) files.
import { readFileSync, readdirSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));
const report = JSON.parse(readFileSync(join(root, "test-results", "vitest.json"), "utf8"));

const onDisk = readdirSync(join(root, "src"), { recursive: true })
  .filter((f) => /\.test\.jsx?$/.test(f))
  .map((f) => join("src", f).split(sep).join("/"))
  .sort();
const ran = new Set(report.testResults.map((r) => relative(root, r.name).split(sep).join("/")));
const missing = onDisk.filter((f) => !ran.has(f));

if (missing.length > 0 || report.numTotalTests === 0) {
  console.error(`Vitest ran ${ran.size} of ${onDisk.length} test files. Not run:\n  ${missing.join("\n  ")}`);
  process.exit(1);
}
console.log(`Verified: all ${onDisk.length} test files ran (${report.numTotalTests} tests).`);
