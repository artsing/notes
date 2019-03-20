配置文件路径 /etc/my.cnf
```
# 服务器的配置块
[mysqld]
# 服务器id
server_id=1001

# 二进制日志前缀名
log_bin=mysql-bin
# 二进制日志格式  行
binlog-format=ROW
# 二进制日志存储时间(一周失效)
expire-logs-days=7

# 缓存大小配置
# ...

# 字符集
character-set-server=utf8

#
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES

#客户端的配置块
[mysql]

```