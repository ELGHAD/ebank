import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import ChangePasswordPage from "./ChangePasswordPage";
import api from "../api/axiosClient";
import { clientUser, renderAt } from "../test/renderWithProviders";

const routes = [
  <Route key="p" path="/password" element={<ChangePasswordPage />} />,
  <Route key="c" path="/client" element={<p>Client area</p>} />,
];

const fill = async (current, next, confirm) => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText("Temporary password"), current);
  await user.type(screen.getByLabelText(/New password/), next);
  await user.type(screen.getByLabelText("Confirm new password"), confirm);
  await user.click(screen.getByRole("button", { name: "Save password" }));
};

const temporaryUser = { ...clientUser, passwordChangeRequired: true };

describe("ChangePasswordPage", () => {
  it("validates length and confirmation before calling the server", async () => {
    const post = vi.spyOn(api, "post");
    renderAt("/password", routes, temporaryUser);

    await fill("temp", "short", "different");

    expect(screen.getByText(/at least 12 characters\./)).toBeInTheDocument();
    expect(screen.getByText("The passwords do not match.")).toBeInTheDocument();
    expect(post).not.toHaveBeenCalled();
  });

  it("changes the password and continues to the client area", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: { expiresIn: 3600, user: clientUser } });
    renderAt("/password", routes, temporaryUser);

    await fill("temporary-pass", "my-own-strong-pass", "my-own-strong-pass");

    expect(await screen.findByText("Client area")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/auth/password", {
      currentPassword: "temporary-pass",
      newPassword: "my-own-strong-pass",
    });
  });

  it("shows the server's reason", async () => {
    vi.spyOn(api, "post").mockRejectedValue({
      response: { status: 400, data: { code: "WRONG_CURRENT_PASSWORD", detail: "The current password is incorrect" } },
    });
    renderAt("/password", routes, temporaryUser);

    await fill("wrong", "my-own-strong-pass", "my-own-strong-pass");

    expect(await screen.findByRole("alert")).toHaveTextContent("The current password is incorrect");
  });
});
