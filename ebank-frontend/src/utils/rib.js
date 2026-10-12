// Client-side mirror of the backend's synthetic RIB format: "EB" + 2 check digits + 20 digits (MOD 97-10).
// Only a convenience to catch typos early; the backend validates again.

export const normalizeRib = (value) => value.replace(/\s+/g, "").toUpperCase();

export const isValidRib = (value) => {
  const rib = normalizeRib(value || "");
  if (!/^EB\d{22}$/.test(rib)) return false;
  // Move the first 4 characters to the end, letters to numbers (E=14, B=11), then mod 97 must be 1
  const rearranged = rib.slice(4) + "1411" + rib.slice(2, 4);
  let remainder = 0;
  for (const digit of rearranged) {
    remainder = (remainder * 10 + Number(digit)) % 97;
  }
  return remainder === 1;
};
