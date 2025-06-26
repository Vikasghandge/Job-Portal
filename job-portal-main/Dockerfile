FROM node:18

WORKDIR /app
COPY package*.json ./

# Clean install dependencies
RUN npm ci --legacy-peer-deps

COPY . .

RUN npm run build

CMD ["npm", "start"]