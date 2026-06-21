# syntax=docker/dockerfile:1
# dist는 호스트에서 미리 빌드 (npm run build)
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
