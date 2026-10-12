// The backend answers errors with RFC 7807 problem details: { status, detail, code, errors? }.

export const errorMessage = (err, fallback) => {
  if (!err?.response) {
    return "Cannot reach the server. Please try again.";
  }
  return err.response.data?.detail || fallback;
};

/** Field-level validation messages, e.g. { email: "Email must be a valid address" }. */
export const fieldErrors = (err) => err?.response?.data?.errors || {};
