注：eureka服务地址localhost:3333/eureka
>应用ID: demo-service
实例ID: demo-service-1

一、注册服务实例
>POST  localhost:3333/eureka/apps/demo-service
Content-Type:application/json
```
{
    "instance": {
        "instanceId": "demo-service-1",
        "hostName": "localhost",
        "app": "demo-service",
        "ipAddr": "172.27.20.149",
        "vipAddress": "demo-service",
        "status": "UP",
        "port": {
            "$": 8080,
            "@enabled": "true"

        },
        "securePort": {
            "$": 443,
            "@enabled": "False"

        },
        "homePageUrl":  "http://172.27.20.149:8080/home",
        "statusPageUrl": "http://172.27.20.149:8080/status",
        "healthCheckUrl": "http://172.27.20.149:8080/check",
        "dataCenterInfo": {
            "@class": "com.netflix.appinfo.MyDataCenterInfo",
            "name": "MyOwn"
        },
        "leaseInfo": {
            "renewalIntervalInSecs": 30,
            "durationInSecs": 90
        }
    }
}
```
二、获取服务实例
1、获取应用列表
>GET  localhost:3333/eureka/apps
Accept:application/json

2、获取实例
>GET localhost:3333/eureka/instances/demo-service-1
Accept:application/json

3、根据虚ip获取实例
>GET localhost:3333/eureka/vips/demo-service
Accept:application/json

三、发送心跳包
>PUT localhost:3333/eureka/apps/demo-service/demo-service-1

四、设置实例状态
>PUT  localhost:3333/eureka/apps/demo-service/demo-service-1/status?value=OUT_OF_SERVICE

>PUT localhost:3333/eureka/apps/demo-service/demo-service-1/status?value=UP

>DELETE localhost:3333/eureka/apps/demo-service/demo-service-1/status?value=UP

(The value=UP is optional, it is used as a suggestion for the fallback status due to removal of the override)

五、注销服务实例
>DELETE localhost:3333/eureka/apps/demo-service/demo-service-1