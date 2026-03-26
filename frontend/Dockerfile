############################
# Desafio Fullstack – Dockerfile do Frontend Angular
# Multi-stage: build Angular → serve com Nginx
############################

# ---- Estágio 1: Build Angular ----
FROM node:20-alpine AS builder

WORKDIR /app

# Copia dependências primeiro (cache eficiente do Docker)
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copia todo o código-fonte e builda para produção
COPY . .
RUN npm run build

# ---- Estágio 2: Nginx para servir os arquivos estáticos ----
FROM nginx:1.25-alpine

# Remove configuração padrão do Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copia configuração customizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia os arquivos buildados do Angular
COPY --from=builder /app/dist/desafio-frontend/browser /usr/share/nginx/html

# Porta 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
