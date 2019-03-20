我们在Linux系统中，如果要使用关系型数据库的话，基本都是用的mysql，而且以往7以下版本的centos

系统都是默认的集成有mysql。然而对于现在最新的centos7系统来说，已经不支持mysql数据库，它默认

内部集成了maridb，如果我们想要使用 mysql 的话，就要先将原来的maridb卸载掉，不然会引起冲突。



注：这里说的冲突是指我们用rpm包的方式安装mysql会产生错误：mariadb-libs is obsoleted by

mysql**。然而笔者使用源码包进行安装时，却并没有这样的错误。



命令：rpm -qa |grep maridb



查看我们的系统中安装了哪些关于maridb的包



命令：rpm -e ***



卸载掉系统中那些关于maridb的包，当然这里我们也可以用 yum remove -y maridb



然后我们进行mysql的rpm包的安装：



rpm -ivh mysql-community-client-5.7.10-1.el7.x86_64.rpm mysql-community-common-5.7.10-1.el7.x86_64.rpm mysql-community-devel-5.7.10-1.el7.x86_64.rpm mysql-community-libs-5.7.10-1.el7.x86_64.rpm mysql-community-server-5.7.10-1.el7.x86_64.rpm



注意：笔者在这里安装的时候出现了许多的依赖关系，需要将这些包一起安装