import { useCallback, useEffect, useState } from "react";
import { errorMessage } from "../api/errors";

/**
 * Loads data with `load()` whenever `key` changes and exposes { data, error, loading, reload }.
 * The result is tagged with the key it answers, so `loading` is derived (no setState inside the effect body)
 * and a slow response for an old key can never overwrite a newer one.
 */
export default function useQuery(load, key, fallbackError = "Cannot load the data.") {
  const [version, setVersion] = useState(0);
  const [result, setResult] = useState({ key: null, data: null, error: "" });
  const requestKey = `${key}#${version}`;

  useEffect(() => {
    let cancelled = false;
    load()
      .then((res) => !cancelled && setResult({ key: requestKey, data: res.data, error: "" }))
      .catch(
        (e) =>
          !cancelled &&
          setResult((previous) => ({ key: requestKey, data: previous.data, error: errorMessage(e, fallbackError) })),
      );
    return () => {
      cancelled = true;
    };
    // `load` is a new closure on every render; the key says when the request really changes
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data: result.data, error: result.error, loading: result.key !== requestKey, reload };
}
