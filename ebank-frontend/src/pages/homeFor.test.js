import { describe, expect, it } from "vitest";
import { homeFor } from "./LoginPage";

describe("homeFor", () => {
  it("sends each role to its own area, most privileged role first", () => {
    expect(homeFor({ roles: ["PLATFORM_ADMIN"] })).toBe("/platform");
    expect(homeFor({ roles: ["ORG_ADMIN"] })).toBe("/org");
    expect(homeFor({ roles: ["AGENT"] })).toBe("/admin");
    expect(homeFor({ roles: ["CLIENT"] })).toBe("/client");
    expect(homeFor({ roles: ["AGENT", "ORG_ADMIN"] })).toBe("/org");
  });

  it("always sends a user with a temporary password to the password change first", () => {
    expect(homeFor({ roles: ["ORG_ADMIN"], passwordChangeRequired: true })).toBe("/change-password");
  });
});
