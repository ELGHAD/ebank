import React from "react";

/** Previous / next navigation for a PageResponse ({ page, totalPages, totalElements }). */
export default function Pagination({ data, onPageChange, disabled, label = "items" }) {
  if (!data) {
    return null;
  }
  if (data.totalPages <= 1) {
    return (
      <p className="pagination">
        {data.totalElements} {label}
      </p>
    );
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="btn btnSmall"
        disabled={disabled || data.page === 0}
        onClick={() => onPageChange(data.page - 1)}
      >
        Previous
      </button>
      <span>
        Page {data.page + 1} of {data.totalPages} ({data.totalElements} {label})
      </span>
      <button
        type="button"
        className="btn btnSmall"
        disabled={disabled || data.page + 1 >= data.totalPages}
        onClick={() => onPageChange(data.page + 1)}
      >
        Next
      </button>
    </nav>
  );
}
