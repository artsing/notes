#一、安装vsftpd及ftp命令

>yum install vsftpd -y

>yum install ftp -y

#二、配置
允许root登录：去除 user_lsit和ftpusers 中的root
>setsebool allow_ftpd_full_access 1

启动ftp服务
>service vsftpd start

我的连接
> ftp 172.16.139.132
root
tsing

常见问题：
Q: 传输的文件损坏
A: 使用 binary 模式传输
> binary

Q: >put ~/Downloads/go1.11.linux-amd64.tar.gz
local: /Users/artsing/Downloads/go1.11.linux-amd64.tar.gz remote: /Users/artsing/Downloads/go1.11.linux-amd64.tar.gz
200 PORT command successful. Consider using PASV.
553 Could not create file.
A: 没有权限创建目录，命令修改为
>put ~/Downloads/go1.11.linux-amd64.tar.gz /root/go.tar.gz


Ubuntu系统：
>sudo apt-get install vsftpd

![image.png](https://upload-images.jianshu.io/upload_images/1697198-6f29a45b86aa6fcb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/680)



