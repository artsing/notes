一、下载安装包
>wget https://dl.google.com/go/go1.11.linux-amd64.tar.gz

二、解压该文件
>tar -C /usr/local -xzf go1.11.linux-amd64.tar.gz

三、创建链接
>ln -s /usr/local/go/bin/go /usr/local/bin/go
>ln -s /usr/local/go/bin/godoc /usr/local/bin/godoc
>ln -s /usr/local/go/bin/gofmt /usr/local/bin/gofmt

三.、配置环境变量
1. 打开配置文件
>vim /etc/profile

2. 添加环境变量

```
# go 根目录
export GOROOT=/usr/local/go
# go 插件目录和项目根目录
export GOPATH=/root/projects/goPlugins:/root/projects/goHome
# 添加插件bin环境变量
export PATH=$PATH:/root/projects/goPlugins/bin      
```

3. 添加目录
>mkdir /root/projects
>mkdir /root/projects/goPlugins /root/projects/goHome
>cd /root/projects/goPlugins/
>mkdir src pkg bin
>cd /root/projects/goHome
>mkdir src pkg bin

4. 让配置生效
>source /etc/profile 

四、查看go版本
>go version 