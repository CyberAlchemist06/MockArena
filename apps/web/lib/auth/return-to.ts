export function safeReturnTo(value: string | null | undefined, fallback = "/candidate"): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.includes("\\")) return fallback;
  try {
    const parsed = new URL(value, "http://mockarena.local");
    return parsed.origin === "http://mockarena.local" ? `${parsed.pathname}${parsed.search}${parsed.hash}` : fallback;
  } catch {
    return fallback;
  }
}
