####一、安装Apache和PHP
#####1、安装httpd、php、mysql
>yum -y install httpd
yum -y install php
yum -y install php-fpm
yum -y install mysql
yum -y install php-mysql

#####2、安装mysql-server
>wget http://dev.mysql.com/get/mysql-community-release-el7-5.noarch.rpm
rpm -ivh mysql-community-release-el7-5.noarch.rpm
yum install mysql-community-server

#####3、安装php扩展包
>yum -y install php-gd php-xml php-mbstring php-ldap php-pear php-xmlrpc php-devel

#####4、安装mysql扩展包
>yum -y install mysql-connector-odbc mysql-devel libdbi-dbd-mysql

#####5、设置开机启动
>chkconfig httpd on
chkconfig mysqld on

#####6、重启mysqld和httpd、启动php-fpm
>service mysqld restart
service php-fpm start
service httpd restart

#####7、设置mysql密码
>mysql –u root
set password for 'root'@'localhost' =password('123456');
quit;

#####8、查看端口占用情况
>netstat -tunlp

#####9、在/var/www/html目录新建test.php文件
>vi test.php

输入测试代码：
```
<?php
phpinfo()
?>
```
####二、设置Nginx反向代理
#####1、修改httpd默认端口
>vim /etc/httpd/conf/httpd.conf
把Listen 80改成
```
Listen 8080
```
#####2、重启httpd服务
>service httpd restart
#####3、修改Nginx配置 nginx.conf
"\#新增的内容" 为配置文件修改的地方
```
      1
      2 #user  nobody;
      3
      4 #worker_processes  1;
      5 daemon off;
      6 master_process off;
      7
      8 #error_log  logs/error.log;
      9 #error_log  logs/error.log  notice;
     10 #error_log  logs/error.log  info;
     11 error_log  logs/error.log  debug;
     12
     13 #pid        logs/nginx.pid;
     14
     15
     16
     17
     18
     19 events {
     20     worker_connections  1024;
     21 }
     22
     23
     24 http {
     25     include       mime.types;
     26     default_type  application/octet-stream;
     27
     28     #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
     29     #                  '$status $body_bytes_sent "$http_referer" '
     30     #                  '"$http_user_agent" "$http_x_forwarded_for"';
     31
     32     #access_log  logs/access.log  main;
     33
     34     sendfile        on;
     35     #tcp_nopush     on;
     36
     37     #keepalive_timeout  0;
     38     keepalive_timeout  65;
     39
     40     #gzip  on;
     41
     42     # 新增加的内容
     43     #---------------------------------------------------
     44     upstream mysvr {
     45        server 172.27.20.150:8080; # Apache httpd 网站的IP地址和端口
     46     }
     47     #----------------------------------------------------
    48     server {
     49         listen       80;
     50         server_name  localhost;
     51         modsecurity on;
     52
     53         #charset koi8-r;
     54
     55         #access_log  logs/host.access.log  main;
     56
     57         location / {
     58             modsecurity_rules_file /usr/local/nginx/conf/modsec_includes.conf;
     59             root   html;
     60             index  index.html index.htm;
     61         }
     62
     63         #error_page  404              /404.html;
     64
     65         # redirect server error pages to the static page /50x.html
     66         #
     67         error_page   500 502 503 504  /50x.html;
     68         location = /50x.html {
     69             root   html;
     70         }
     71
     72        # 新增加的内容
     73        #--------------------------------------------------
     74         location ~ \.php$ {
     75             proxy_pass   http://mysvr;  # 44 行添加的upstream 名字
     76         }
     77         #--------------------------------------------------
     78         # pass the PHP scripts to FastCGI server listening on 127.0.0.1:9000
     79         #
     80         #location ~ \.php$ {
     81         #    root           /var/www/html;
     82         #    fastcgi_pass   127.0.0.1:8080;
     83         #    fastcgi_index  test.php;
     84         #    fastcgi_param  SCRIPT_FILENAME  /scripts$fastcgi_script_name;
     85         #    include        fastcgi_params;
     86         #}
     87
     88         # deny access to .htaccess files, if Apache's document root
     89         # concurs with nginx's one
     90         #
     91         #location ~ /\.ht {
     92         #    deny  all;
     93         #}
     94     }
     95
     96
     97     # another virtual host using mix of IP-, name-, and port-based configuration
     98     #
     99     #server {
    100     #    listen       8000;
    101     #    listen       somename:8080;
    102     #    server_name  somename  alias  another.alias;
    103
    104     #    location / {
    105     #        root   html;
    106     #        index  index.html index.htm;
    107     #    }
    108     #}
    109
    110
    111     # HTTPS server
    112     #
    113     #server {
    114     #    listen       443 ssl;
    115     #    server_name  localhost;
    116
    117     #    ssl_certificate      cert.pem;
    118     #    ssl_certificate_key  cert.key;
    119
    120     #    ssl_session_cache    shared:SSL:1m;
    121     #    ssl_session_timeout  5m;
    122
    123     #    ssl_ciphers  HIGH:!aNULL:!MD5;
    124     #    ssl_prefer_server_ciphers  on;
    125
    126     #    location / {
    127     #        root   html;
    128     #        index  index.html index.htm;
    129     #    }
    130     #}
    131
    132 }

```
#####3、在浏览器中测试
>http://ip address/test.php