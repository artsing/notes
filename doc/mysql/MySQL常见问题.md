1. 配置 MySQL 远程访问
> GRANT ALL PRIVILEGES ON *.* TO 'root'@'172.27.20.149' IDENTIFIED BY '123456' WITH GRANT OPTION;

> FLUSH PRIVILEGES;

2. MySQL 中文编码问题
> mysql> show variables like '%char%';

>mysql> set character_set_database=utf8;
>mysql> set character_set_server=utf8;

```
create table person(
    id int not null AUTO_INCREMENT,
    name varchar(100) not null,
    age int not null,
    primary key(id)
 ) engine=InnoDB default charset=utf8;
```
