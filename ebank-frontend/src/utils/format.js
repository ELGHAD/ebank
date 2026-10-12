// Display formatting only. Amounts are never computed client-side for business purposes.
const moneyFormatter = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export const formatMoney = (value) => moneyFormatter.format(Number(value || 0));

export const formatDateTime = (iso) =>
  new Date(iso).toLocaleString("en-GB", { dateStyle: "medium", timeStyle: "short" });
