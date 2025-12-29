import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Use Bun's native modules
  serverExternalPackages: ['bun'],

  // Optimize for Bun runtime
  experimental: {
    serverActions: {
      bodySizeLimit: '2mb',
    },
  },

  // Image optimization
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**',
      },
    ],
  },
};

export default nextConfig;
