import "./globals.css";
import { Providers } from "./providers";

export const metadata = { title: "MockArena", description: "Practice real assessment flows." };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Providers>{children}</Providers></body></html>;
}
