
# # 运行 serve 命令提供静态文件
# CMD ["serve", "-s", "build", "-l", "3000"]


FROM node:18

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm install

COPY . .

# 构建
RUN npm run build

# 安装 serve 工具来提供静态文件
RUN npm install -g serve

EXPOSE 3000
CMD ["serve", "-s", "dist"]
