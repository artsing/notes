注：单台设备上模拟主备
一、eureka_server1配置
```
spring.application.name=eureka_server

server.port=3333
eureka.instance.hostname=localhost
eureka.instance.lease-renewal-interval-in-seconds=30
eureka.instance.lease-expiration-duration-in-seconds=30
eureka.instance.prefer-ip-address=true

eureka.client.service-url.defaultZone=http://localhost:5555/eureka
eureka.server.peer-eureka-nodes-update-interval-ms=30000
```
二、eureka_server2配置
```
spring.application.name=eureka_server

server.port=5555
eureka.instance.hostname=localhost
eureka.instance.lease-renewal-interval-in-seconds=30
eureka.instance.lease-expiration-duration-in-seconds=30
eureka.instance.prefer-ip-address=true

eureka.client.service-url.defaultZone=http://localhost:3333/eureka
eureka.server.peer-eureka-nodes-update-interval-ms=30000
```
