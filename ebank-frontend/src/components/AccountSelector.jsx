export default function AccountSelector({ accounts, selectedId, onChange }) {
  return (
    <div style={{ marginBottom: 15 }}>
      <label style={{ marginRight: 10 }}>Choose account:</label>
      <select
        value={selectedId || ""}
        onChange={(e) => onChange(Number(e.target.value))}
      >
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {a.rib} (#{a.id})
          </option>
        ))}
      </select>
    </div>
  );
}
