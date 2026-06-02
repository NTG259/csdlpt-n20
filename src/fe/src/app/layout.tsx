import type { Metadata } from "next"
import { Geist, Geist_Mono } from "next/font/google"

import { Footer } from "@/components/shared/footer"
import { Header } from "@/components/shared/header"

import "./globals.css"
import { Providers } from "./providers"

const geistSans = Geist({
  variable: "--font-sans",
  subsets: ["latin"],
})

const geistMono = Geist_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
})

export const metadata: Metadata = {
  title: "SiteMain FE",
  description: "Frontend scaffold for the distributed ecommerce demo",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html
      lang="vi"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col bg-background text-foreground">
        <Providers>
          <Header />
          <div className="flex min-h-0 flex-1 flex-col">{children}</div>
          <Footer />
        </Providers>
      </body>
    </html>
  )
}
