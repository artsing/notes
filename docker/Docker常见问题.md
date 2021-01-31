1. 如何配置国内镜像源

> vim /etc/docker/daemon.json
```
{
    "registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]
}
```

2. docker 启动容器报 WARNING: IPv4 forwarding is disabled. Networking will not work.

> vim /etc/sysctl.conf
   ```
   #配置转发
   net.ipv4.ip_forward=1

   #重启服务，让配置生效
   systemctl restart network

   #查看是否成功,如果返回为“net.ipv4.ip_forward = 1”则表示成功
   sysctl net.ipv4.ip_forward
   ```