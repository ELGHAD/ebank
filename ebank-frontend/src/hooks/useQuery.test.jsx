import React, { useState } from "react";
import { describe, expect, it } from "vitest";
import { act, render, screen } from "@testing-library/react";
import useQuery from "./useQuery";

/** A promise the test resolves by hand, to control response order. */
const deferred = () => {
  let resolve;
  const promise = new Promise((r) => (resolve = r));
  return { promise, resolve };
};

function Probe({ requests }) {
  const [key, setKey] = useState("a");
  const { data, loading } = useQuery(() => requests[key].promise, key);
  return (
    <>
      <button onClick={() => setKey("b")}>switch</button>
      <p>{loading ? "loading" : `data=${data}`}</p>
    </>
  );
}

describe("useQuery", () => {
  it("ignores a slow response for a key that is no longer current", async () => {
    const requests = { a: deferred(), b: deferred() };
    render(<Probe requests={requests} />);
    expect(screen.getByText("loading")).toBeInTheDocument();

    act(() => screen.getByText("switch").click());
    await act(async () => requests.b.resolve({ data: "B" }));
    expect(screen.getByText("data=B")).toBeInTheDocument();

    // The answer for the old key arrives late: it must not overwrite B
    await act(async () => requests.a.resolve({ data: "A" }));
    expect(screen.getByText("data=B")).toBeInTheDocument();
  });
});
