### 一、MySQL登录
1. 方式1
> mysql -u root -p
> 输入密码
2. 方式2
> mysql -uroot -p123456

### 二、数据库操作

###### 查看数据库
> show databases;

###### 创建数据库
> create database waf;

###### 删除数据库
> drop database waf;

###### 使用数据库
> use waf;

###### 数据库备份还原
> mysqldump -u root -p waf > ./waf.sql
>
### 三、表操作
###### 查看表
> show tables;

###### 创建表
> create table person(
    id int not null auto_increment,
    name varchar(100) not null,
    age int not null,
    primary key(id)
 ) engine=InnoDB default charset=utf8;

###### 删除表
> drop table person;

###### 插入数据
> insert into person(name, age) values ('张三', 25);

###### 更新数据
> update person set age=26 where name='张三';

###### 删除数据
> delete from person where name='张三';

###### 查找数据
> select * from person;


