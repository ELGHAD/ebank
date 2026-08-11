export default function OperationsTable({ operations }) {
  if (!operations || operations.length === 0) return <p>No operations.</p>;

  return (
    <table border="1" cellPadding="10" style={{ marginTop: 10, width: "100%" }}>
      <thead>
        <tr>
          <th>Label</th>
          <th>Type</th>
          <th>Date</th>
          <th>Amount</th>
        </tr>
      </thead>
      <tbody>
        {operations.map((op, idx) => (
          <tr key={idx}>
            <td>{op.label || op.description || "-"}</td>
            <td>{op.type || op.operationType}</td>
            <td>{op.date || op.operationDate}</td>
            <td>{op.amount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
