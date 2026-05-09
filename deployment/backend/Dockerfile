# 使用 Tomcat 10.1.x 官方镜像 (基于 JDK 17 或 11)
# 注意：Tomcat 10.1 通常要求 JDK 11+
FROM tomcat:10.1.50-jdk17-temurin

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 删除默认应用
RUN rm -rf /usr/local/tomcat/webapps/*

# 复制 WAR 包
COPY backend-1.0.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
