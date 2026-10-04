FROM node:22-alpine
WORKDIR /app
COPY package.json ./
COPY server ./server
COPY public ./public
ENV PORT=8080 HOST=0.0.0.0 DATA_DIR=/data
VOLUME /data
EXPOSE 8080
USER node
CMD ["node", "server/server.js"]
