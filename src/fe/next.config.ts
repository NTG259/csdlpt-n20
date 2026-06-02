import type { NextConfig } from "next";

const defaultAllowedDevOrigins = [
  "localhost",
  "*.localhost",
  "127.0.0.1",
  "0.0.0.0",
  "host.docker.internal",
  "100.*.*.*",
  "**.ts.net",
]

function readCsvEnv(name: string, fallback: string[]) {
  const raw = process.env[name]

  if (!raw) {
    return fallback
  }

  return raw
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
}

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: readCsvEnv(
    "NEXT_ALLOWED_DEV_ORIGINS",
    defaultAllowedDevOrigins
  ),
  images: {
    dangerouslyAllowLocalIP:
      process.env.NEXT_IMAGE_ALLOW_LOCAL_IP !== "false",
  },
};

export default nextConfig;
