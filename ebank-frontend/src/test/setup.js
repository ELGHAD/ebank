import "@testing-library/jest-dom/vitest";
import { cleanup, configure } from "@testing-library/react";
import { afterEach } from "vitest";

// findBy*/waitFor default to 1s, which is too tight on a loaded machine or CI runner (observed flakiness)
configure({ asyncUtilTimeout: 5000 });

// Testing Library only auto-cleans when test globals are enabled; register it explicitly.
afterEach(() => {
  cleanup();
});
