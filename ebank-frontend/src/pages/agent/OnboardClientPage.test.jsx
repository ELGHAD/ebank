import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import OnboardClientPage from "./OnboardClientPage";
import * as agentService from "../../services/agentService";
import { agentUser, renderAt } from "../../test/renderWithProviders";

const routes = <Route path="/admin/clients/new" element={<OnboardClientPage />} />;

const fillRequired = async (user) => {
  await user.type(await screen.findByLabelText(/First name/), "Sara");
  await user.type(screen.getByLabelText(/Last name/), "Demo");
  await user.type(screen.getByLabelText(/Email/), "sara@test.ebank");
  await user.type(screen.getByLabelText(/Identity reference/), "CIN-9");
};

describe("OnboardClientPage", () => {
  it("validates required fields before calling the server", async () => {
    const onboard = vi.spyOn(agentService, "onboardClient");
    const user = userEvent.setup();
    renderAt("/admin/clients/new", routes, agentUser);

    await user.click(await screen.findByRole("button", { name: "Create client" }));

    expect(screen.getByText("First name is required.")).toBeInTheDocument();
    expect(screen.getByLabelText(/Email/)).toHaveAttribute("aria-invalid", "true");
    expect(onboard).not.toHaveBeenCalled();
  });

  it("shows the one-time password once, with a link to the new client", async () => {
    vi.spyOn(agentService, "onboardClient").mockResolvedValue({
      data: { client: { id: 12, firstName: "Sara", lastName: "Demo" }, username: "sara@test.ebank", temporaryPassword: "Tmp-Secret-123" },
    });
    const user = userEvent.setup();
    renderAt("/admin/clients/new", routes, agentUser);

    await fillRequired(user);
    await user.click(screen.getByRole("button", { name: "Create client" }));

    expect(await screen.findByText("Tmp-Secret-123")).toBeInTheDocument();
    expect(screen.getByText(/shown only once/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open an account for this client" })).toHaveAttribute("href", "/admin/clients/12");
    expect(agentService.onboardClient).toHaveBeenCalledWith(expect.objectContaining({ birthDate: null, address: null }));
  });

  it("shows server-side field errors and conflicts", async () => {
    vi.spyOn(agentService, "onboardClient").mockRejectedValue({
      response: { status: 409, data: { code: "EMAIL_TAKEN", detail: "A user with this email already exists" } },
    });
    const user = userEvent.setup();
    renderAt("/admin/clients/new", routes, agentUser);

    await fillRequired(user);
    await user.click(screen.getByRole("button", { name: "Create client" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("A user with this email already exists");
  });
});
