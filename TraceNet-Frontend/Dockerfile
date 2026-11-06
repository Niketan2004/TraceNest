# ==============================================================================
# TraceNest Frontend Dockerfile
# Node.js 20 Alpine container for TanStack Start / Vite
# ==============================================================================

FROM node:20-alpine

WORKDIR /app

# Copy dependency configuration files
COPY package.json package-lock.json ./

# Install npm dependencies
RUN npm ci || npm install

# Copy application source code
COPY . .

# Expose Vite dev server port
EXPOSE 5173

# Configuration defaults
ENV HOST=0.0.0.0
ENV PORT=5173
# SSR server-to-server URL (Node.js hits the backend container directly)
ENV INTERNAL_API_URL=http://backend:8080/api/v1

# Run development server — host/port already set via vite.config.ts
CMD ["npm", "run", "dev"]
