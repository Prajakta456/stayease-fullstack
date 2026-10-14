# ---- build: production bundle ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY public public
COPY src src

# Baked in at build time. The default /api/v1 is served by nginx below, so the browser talks to a
# single origin and needs no CORS.
ARG REACT_APP_API_URL=/api/v1
ARG REACT_APP_SHOW_DEMO_LOGINS=false
ENV REACT_APP_API_URL=$REACT_APP_API_URL \
    REACT_APP_SHOW_DEMO_LOGINS=$REACT_APP_SHOW_DEMO_LOGINS
RUN npm run build

# ---- runtime: nginx serving the static files (runs as a non-root user on port 8080) ----
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD wget -q -O /dev/null http://localhost:8080/ || exit 1
