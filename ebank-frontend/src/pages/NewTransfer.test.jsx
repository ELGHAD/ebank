import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import NewTransfer from "./NewTransfer";
import * as clientService from "../services/clientService";
import { clientSession, renderAt } from "../test/renderWithProviders";

const SOURCE = { id: 7, rib: "EB3777744714656519320252", currency: "MAD", balance: 500, status: "ACTIVE" };
const RECIPIENT_RIB = "EB3094712094552729193874";

const routes = <Route path="/client/transfer" element={<NewTransfer />} />;

const fillForm = async (user, { rib = RECIPIENT_RIB, amount = "120.50" } = {}) => {
  await user.type(await screen.findByLabelText("Recipient RIB"), rib);
  await user.type(screen.getByLabelText(/Amount/), amount);
  await user.type(screen.getByLabelText(/Description/), "Rent");
};

describe("NewTransfer", () => {
  beforeEach(() => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({ data: [SOURCE] });
  });

  it("reviews, confirms and sends the transfer with an idempotency key", async () => {
    vi.spyOn(clientService, "createTransfer").mockResolvedValue({
      data: { id: 42, sourceAccountId: 7, destinationRib: RECIPIENT_RIB, amount: 120.5, currency: "MAD", balanceAfter: 379.5 },
    });
    const user = userEvent.setup();
    renderAt("/client/transfer", routes, clientSession);

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Review transfer" }));
    expect(screen.getByRole("heading", { name: "Confirm this transfer" })).toBeInTheDocument();
    expect(clientService.createTransfer).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: "Confirm transfer" }));

    expect(await screen.findByText("Transfer completed.")).toBeInTheDocument();
    expect(screen.getByText("379.50 MAD")).toBeInTheDocument();
    const [payload, key] = clientService.createTransfer.mock.calls[0];
    expect(payload).toEqual({ sourceAccountId: 7, destinationRib: RECIPIENT_RIB, amount: "120.50", description: "Rent" });
    expect(key).toMatch(/^[0-9a-f-]{32,36}$/);
  });

  it("catches a mistyped RIB and an excessive amount before calling the server", async () => {
    const createTransfer = vi.spyOn(clientService, "createTransfer");
    const user = userEvent.setup();
    renderAt("/client/transfer", routes, clientSession);

    await fillForm(user, { rib: RECIPIENT_RIB.slice(0, -1) + "5", amount: "600" });
    await user.click(screen.getByRole("button", { name: "Review transfer" }));

    expect(screen.getByText(/This RIB is not valid/)).toBeInTheDocument();
    expect(screen.getByText(/exceeds the available balance/)).toBeInTheDocument();
    expect(createTransfer).not.toHaveBeenCalled();
  });

  it("retries a network failure with the same idempotency key", async () => {
    const createTransfer = vi.spyOn(clientService, "createTransfer")
      .mockRejectedValueOnce(new Error("Network Error"))
      .mockResolvedValueOnce({
        data: { id: 43, sourceAccountId: 7, destinationRib: RECIPIENT_RIB, amount: 120.5, currency: "MAD", balanceAfter: 379.5 },
      });
    const user = userEvent.setup();
    renderAt("/client/transfer", routes, clientSession);

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Review transfer" }));
    await user.click(screen.getByRole("button", { name: "Confirm transfer" }));
    expect(await screen.findByText(/You can safely retry/)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("Transfer completed.")).toBeInTheDocument();

    const keys = createTransfer.mock.calls.map(([, key]) => key);
    expect(keys[0]).toBe(keys[1]);
  });

  it("shows the server's reason when the transfer is refused and starts a new attempt", async () => {
    const createTransfer = vi.spyOn(clientService, "createTransfer").mockRejectedValue({
      response: { status: 422, data: { code: "INSUFFICIENT_FUNDS", detail: "Insufficient funds" } },
    });
    const user = userEvent.setup();
    renderAt("/client/transfer", routes, clientSession);

    await fillForm(user);
    await user.click(screen.getByRole("button", { name: "Review transfer" }));
    await user.click(screen.getByRole("button", { name: "Confirm transfer" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Insufficient funds");
    await user.click(screen.getByRole("button", { name: "Review transfer" }));
    await user.click(screen.getByRole("button", { name: "Confirm transfer" }));
    const keys = createTransfer.mock.calls.map(([, key]) => key);
    expect(keys[0]).not.toBe(keys[1]);
  });
});
