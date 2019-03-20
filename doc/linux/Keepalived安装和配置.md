####一、安装环境
硬件环境
| 主机   |      IP       |网卡|
|:----------:|:---------:|:------:|
| 主|  172.16.139.132| eno16777736|
| 备|    172.16.139.134| ens33|


软件环境
| 操作系统   |      Web服务器  |端口    |
|:----------:|:-------------:|:----------:|
| Centos 7|  SpringBoot 程序 | 7001 |

希望虚拟的IP地址
> 172.16.139.100

####二、安装和配置

1. 下载安装包 1.4.0 版本 http://www.keepalived.org/download.html
2. tar -zxvf keepalived-1.4.0.tar.gz
3. yum install openssl-devel
图片
4. ./configure --prefix=/usr/local/keepalived
5. make && make install
6. 拷贝文件
````
[root@localhost ~]# cp keepalived-1.4.0/keepalived/etc/init.d/keepalived    /etc/init.d/ 
[root@localhost ~]# mkdir /etc/keepalived
[root@localhost ~]# cp keepalived-1.4.0/keepalived/etc/sysconfig/keepalived   /etc/sysconfig/  
[root@localhost ~]# ln -s /usr/local/keepalived/sbin/keepalived  /usr/sbin/
````
7. vim /etc/keepalived/keepalived.conf
主：
```
! Configuration File for keepalived

global_defs {
   notification_email {
     acassen@firewall.loc
     failover@firewall.loc
     sysadmin@firewall.loc
   }
   notification_email_from Alexandre.Cassen@firewall.loc
   smtp_server 192.168.200.1
   smtp_connect_timeout 30
   router_id LVS_DEVEL
   vrrp_skip_check_adv_addr
   vrrp_garp_interval 0
   vrrp_gna_interval 0
}

vrrp_instance VI_1 {
    state MASTER
    interface eno16777736
    virtual_router_id 51
    priority 100
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass 1111
    }
    virtual_ipaddress {
        172.16.139.100
    }
}

virtual_server 172.16.139.100 7001 {
    delay_loop 6
    lb_algo rr
    lb_kind NAT
    persistence_timeout 50
    protocol TCP

    real_server 172.16.139.132 7001 {
        weight 1
    }

    real_server 172.16.139.134 7001 {
        weight 1
    }

}
```
备(在主的配置基础上修改)：
```
interface ens33
state BACKUP
priority 50
```
7. 启动 keepalived -f /etc/keepalived/keepalived.conf
8. 让 服务器(tomcat )认为自己是172.16.139.100 否则会丢包
>ifconfig lo:0 172.16.139.100 netmask 255.0.0.0 up

 ![error.png](https://upload-images.jianshu.io/upload_images/1697198-0821ede160b3c1eb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/520)
