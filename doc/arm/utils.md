### 常用工具
#
#### minicom (mac os)

1.安装
> brew install minicom


2.进入配置菜单

>minicom -s 

```
USB串口设备： /dev/tty.usbserial
波特率： 115200 
```
3. 启动
> minicom

#
#### tftp (arm linux)

1.下载文件
> tftp -g -r hello 172.16.0.1
```
tftp [option] ... host [port]

[option]
    -g get 获取
    -r remote file 远端文件 
host 主机地址

[port] 端口号

```


