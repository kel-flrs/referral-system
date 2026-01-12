FROM node:20-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat openssl
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./
COPY packages/shared/package.json ./packages/shared/
COPY packages/database/package.json ./packages/database/
COPY apps/api/package.json ./apps/api/

# Install dependencies
RUN npm ci

# Build stage
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Generate Prisma Client
RUN npx prisma generate --schema=./packages/database/prisma/schema.prisma

# Build packages
RUN npm run build --workspace=@referral-system/shared
RUN npm run build --workspace=@referral-system/database
RUN npm run build --workspace=@referral-system/api

# Production stage
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 apiuser

# Copy built files
COPY --from=builder --chown=apiuser:nodejs /app/apps/api/dist ./apps/api/dist
COPY --from=builder --chown=apiuser:nodejs /app/packages/shared/dist ./packages/shared/dist
COPY --from=builder --chown=apiuser:nodejs /app/packages/database/dist ./packages/database/dist
COPY --from=builder --chown=apiuser:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=apiuser:nodejs /app/packages/database/prisma ./packages/database/prisma

# Copy package.json files
COPY --from=builder --chown=apiuser:nodejs /app/package.json ./
COPY --from=builder --chown=apiuser:nodejs /app/apps/api/package.json ./apps/api/
COPY --from=builder --chown=apiuser:nodejs /app/packages/shared/package.json ./packages/shared/
COPY --from=builder --chown=apiuser:nodejs /app/packages/database/package.json ./packages/database/

USER apiuser

EXPOSE 3222

WORKDIR /app/apps/api

CMD ["node", "dist/index.js"]
