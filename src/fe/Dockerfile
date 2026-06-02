FROM oven/bun:1-alpine AS deps

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

FROM deps AS build

WORKDIR /app

ARG NEXT_PUBLIC_MAIN_API_URL=http://localhost:8080
ARG NEXT_PUBLIC_NORTH_API_URL=http://localhost:8081
ARG NEXT_PUBLIC_SOUTH_API_URL=http://localhost:8082
ARG NEXT_ALLOWED_DEV_ORIGINS=localhost,*.localhost,127.0.0.1,0.0.0.0,host.docker.internal,100.*.*.*,**.ts.net
ARG NEXT_IMAGE_ALLOW_LOCAL_IP=true

ENV NEXT_PUBLIC_MAIN_API_URL=$NEXT_PUBLIC_MAIN_API_URL
ENV NEXT_PUBLIC_NORTH_API_URL=$NEXT_PUBLIC_NORTH_API_URL
ENV NEXT_PUBLIC_SOUTH_API_URL=$NEXT_PUBLIC_SOUTH_API_URL
ENV NEXT_ALLOWED_DEV_ORIGINS=$NEXT_ALLOWED_DEV_ORIGINS
ENV NEXT_IMAGE_ALLOW_LOCAL_IP=$NEXT_IMAGE_ALLOW_LOCAL_IP

COPY . .
RUN bun run build

FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

COPY --from=build /app/public ./public
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/.next/standalone ./

EXPOSE 3000

CMD ["node", "server.js"]
