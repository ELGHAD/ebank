import React from "react";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import UsageMeter from "./UsageMeter";

describe("UsageMeter", () => {
  it("exposes the value to assistive technology", () => {
    render(<UsageMeter label="clients" used={10} limit={25} />);
    const meter = screen.getByRole("meter", { name: "clients" });
    expect(meter).toHaveAttribute("aria-valuenow", "10");
    expect(meter).toHaveAttribute("aria-valuemax", "25");
    expect(meter).toHaveAttribute("aria-valuetext", "10 of 25");
    expect(screen.getByText("10 / 25")).toBeInTheDocument();
  });

  it("states the severity in words, not only with color", () => {
    const { rerender } = render(<UsageMeter label="clients" used={21} limit={25} />);
    expect(screen.getByText("Near limit")).toBeInTheDocument();
    expect(screen.getByRole("meter")).toHaveAttribute("aria-valuetext", "21 of 25, near limit");

    rerender(<UsageMeter label="clients" used={25} limit={25} />);
    expect(screen.getByText("Limit reached")).toBeInTheDocument();
  });

  it("shows unlimited quotas without a bar", () => {
    render(<UsageMeter label="clients" used={900} limit={null} />);
    expect(screen.queryByRole("meter")).not.toBeInTheDocument();
    expect(screen.getByText(/unlimited/)).toBeInTheDocument();
  });
});
