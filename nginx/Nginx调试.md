#####概述：为了便于gdb调试linux配置时添加--with-debug选项
> ./configure  --with-debug

####一、单进程模式调试
##### 1、修改配置usr/local/nginx/conf/nginx.conf
######1）在配置文件首部添加配置项
> daemon off;
   master_process off;

#####2、使用gdb调试
######1）启动调试
>\#gdb nginx

######2) 在main函数处下断点并运行
>(gdb)b main
(gdb)r
######3) 查看代码
>(gdb)l
######4) 单步调试
>(gdb)n


####二、master-worker模式
##### 1、修改配置usr/local/nginx/conf/nginx.conf
######1）在配置文件首部添加配置项
> daemon on;
   master_process on;
   worker_process 1;

#####2、使用gdb调试
######1）启动调试
>\#gdb nginx

######2) 下断点并运行
>(gdb)b main
(gdb)b ngx_daemon
(gdb)b ngx_master_process_cycle
(gdb)r

######3) 指定跟踪模式
>(gdb)set follow-fork-mode child

######4)gdb调试指定进程
>\#gdb -p pid
或(gdb) attach pid