import type { NextConfig } from "next";

const configuredGatewayInternalUrl = process.env.GATEWAY_INTERNAL_URL?.replace(/\/$/, "");
const developmentGatewayInternalUrl =
  configuredGatewayInternalUrl ||
  (process.env.NODE_ENV !== "production" ? "http://localhost:8000" : undefined);

const nextConfig: NextConfig = {
  output: "standalone",
  eslint: {
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    if (!developmentGatewayInternalUrl) {
      return [];
    }
    return [
      {
        source: "/api/:path*",
        destination: `${developmentGatewayInternalUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
