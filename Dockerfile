FROM mcr.microsoft.com/playwright:v1.56.1-noble
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
WORKDIR /app
COPY gateway/package.json ./package.json
RUN npm install --omit=dev --no-audit --no-fund
COPY gateway/ ./
EXPOSE 8787
CMD ["node", "server.js"]
