server.port=3333
eureka.instance.hostname=localhost
#不要向注册中心注册自己
eureka.client.register-with-eureka=false
#禁止检索服务
eureka.client.fetch-registry=false
eureka.server.enable-self-preservation=false
eureka.client.service-url.defaultZone=http://${eureka.instance.hostname}:${server.port}/eureka