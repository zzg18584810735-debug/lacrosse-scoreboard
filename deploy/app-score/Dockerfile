FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache nginx \
    && mkdir -p /run/nginx \
    && printf '%s\n' \
        'server {' \
        '    listen 80;' \
        '    server_name _;' \
        '    root /var/lib/nginx/html;' \
        '    index index.html;' \
        '    charset utf-8;' \
        '' \
        '    location /api/ {' \
        '        proxy_pass http://127.0.0.1:8080;' \
        '        proxy_http_version 1.1;' \
        '        proxy_set_header Host $host;' \
        '        proxy_set_header X-Real-IP $remote_addr;' \
        '        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;' \
        '        proxy_set_header X-Forwarded-Proto $scheme;' \
        '    }' \
        '' \
        '    location / {' \
        '        try_files $uri $uri/ /index.html;' \
        '    }' \
        '}' \
        > /etc/nginx/http.d/default.conf

WORKDIR /app
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"

COPY back/lacrosse-scoreboard.jar /app/lacrosse-scoreboard.jar
COPY frond/dist/ /var/lib/nginx/html/

EXPOSE 80
ENTRYPOINT ["sh", "-c", "nginx && exec java $JAVA_OPTS -jar /app/lacrosse-scoreboard.jar"]
