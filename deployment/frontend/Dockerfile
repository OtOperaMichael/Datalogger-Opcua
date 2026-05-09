# 直接基于 nginx 镜像
FROM nginx:1.25-alpine

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 删除默认页面，复制你的 dist 文件
RUN rm -rf /usr/share/nginx/html
COPY dist/ /usr/share/nginx/html/

# 暴露端口（可选）
EXPOSE 80

# 启动 nginx 前台运行
CMD ["nginx", "-g", "daemon off;"]
