import React, { useEffect, useRef } from "react";

/**
 * Page title + actions. On navigation the title gets the focus and the document title is updated,
 * so screen-reader users hear which page they are on (a single-page app does not reload the document).
 */
export default function PageHeader({ title, subtitle, breadcrumb, actions }) {
  const heading = useRef(null);

  useEffect(() => {
    document.title = `${title} · eBank`;
    heading.current?.focus({ preventScroll: true });
  }, [title]);

  return (
    <div className="page-header">
      <div>
        {breadcrumb && <p className="breadcrumb">{breadcrumb}</p>}
        <h1 ref={heading} tabIndex={-1}>
          {title}
        </h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="actions">{actions}</div>}
    </div>
  );
}
