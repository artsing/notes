```
-- 用户表
-- tb_user
-- id, role_id, org_id, username, password, name, email, locked, locked_time, pass_update_time, pass_expire_login, pass_history
create table tb_user (
	id varchar(36) not null,
	role_id int not null,
	org_id int not null,
	username varchar(64) not null,
	password varchar(64) not null,
	name varchar(255) not null,
	email varchar(255) not null,
	locked tinyint(1) not null,
	locked_time timestamp not null,
	pass_update_time timestamp not null,
	pass_expire_login int not null,
	pass_history varchar(1000) not null,

	primary key(id)
) engine=InnoDB default charset=utf8;

-- 角色表
-- tb_role
-- id, name, description
create table tb_role (
	id int not null auto_increment,
	type tinyint(8) not null,
	name varchar(255) not null,
	is_sys_role tinyint(1) not null,
	description varchar(1000) not null,

	primary key(id)
) engine=InnoDB default charset=utf8;

-- 组织表
-- tb_org
-- id, pid, name, description
create table tb_org (
    id int not null auto_increment,
    pid int not null,
	name varchar(255) not null,
	description varchar(1000) not null,

	primary key(id)
) engine=InnoDB default charset=utf8;

-- 资源表
-- tb_resource
-- id, pid, type, name, method, url
create table tb_resource (
	id int not null auto_increment,
	pid int not null,
	type tinyint(8) not null,
	name varchar(255) not null,
	method varchar(10) not null,
	url varchar(255) not null,

	primary key(id)
) engine=InnoDB default charset=utf8;

-- 权限表
-- tb_authority
-- id, role_id, resource_id
create table tb_authority (
	id int not null auto_increment,
	role_id int not null,
	resource_id int not null,

	primary key(id)
) engine=InnoDB default charset=utf8;

```
```
-- 资源表
insert into tb_resource(id, pid, type, name, method, url) values(7000,   0, 1, '组织机构', '', '');

insert into tb_resource(id, pid, type, name, method, url) values(7001, 7000, 1, '组织管理', '', '');
insert into tb_resource(id, pid, type, name, method, url) values(7002, 7000, 1, '角色管理', '', '');
insert into tb_resource(id, pid, type, name, method, url) values(7003, 7000, 1, '用户管理', '', '');

insert into tb_resource(id, pid, type, name, method, url)
	values(7011, 7001, 2, '获取组织列表', 'POST', '/org/organizations/page');
insert into tb_resource(id, pid, type, name, method, url)
	values(7021, 7001, 2, '新建组织', 'POST', '/org/organizations');
insert into tb_resource(id, pid, type, name, method, url)
	values(7031, 7001, 2, '编辑组织', 'PUT', '/org/organizations');
insert into tb_resource(id, pid, type, name, method, url)
	values(7041, 7001, 2, '删除组织', 'DELETE', '/org/organizations/{id}');

insert into tb_resource(id, pid, type, name, method, url)
	values(7012, 7002, 2, '获取角色列表', 'POST', '/org/roles/page');
insert into tb_resource(id, pid, type, name, method, url)
	values(7022, 7002, 2, '查看角色', 'GET', '/org/roles/{id}');
insert into tb_resource(id, pid, type, name, method, url)
	values(7032, 7002, 2, '新建角色', 'POST', '/org/roles');
insert into tb_resource(id, pid, type, name, method, url)
	values(7042, 7002, 2, '编辑角色', 'PUT', '/org/roles/{id}');
insert into tb_resource(id, pid, type, name, method, url)
	values(7052, 7002, 2, '删除角色', 'DELETE', '/org/roles/{id}');


insert into tb_resource(id, pid, type, name, method, url)
	values(7013, 7003, 2, '获取用户列表', 'POST', '/org/organizations/page');
insert into tb_resource(id, pid, type, name, method, url)
	values(7023, 7003, 2, '新建用户', 'POST', '/org/organizations');
insert into tb_resource(id, pid, type, name, method, url)
	values(7033, 7003, 2, '编辑用户', 'PUT', '/org/organizations/{id}');
insert into tb_resource(id, pid, type, name, method, url)
	values(7043, 7003, 2, '删除用户', 'DELETE', '/org/organizations/{id}');
insert into tb_resource(id, pid, type, name, method, url)
	values(7053, 7003, 2, '修改密码', 'PUT', '/org/organizations/{id}/password');

insert into tb_resource(id, pid, type, name, method, url) values(8000,   0, 1, '通告管理', '', '');

insert into tb_resource(id, pid, type, name, method, url)
	values(8010, 8000, 1, '获取发布通告列表', 'POST', '/nav/notices/publish/page');
insert into tb_resource(id, pid, type, name, method, url)
	values(8020, 8000, 1, '获取草稿通过列表', 'POST', '/nav/notices/trash/page');
insert into tb_resource(id, pid, type, name, method, url)
	values(8030, 8000, 1, '新建通告', 'POST', '/nav/notices');
insert into tb_resource(id, pid, type, name, method, url)
	values(8040, 8000, 1, '编辑通告', 'PUT', '/nav/notices/{id}');
insert into tb_resource(id, pid, type, name, method, url)
	values(8050, 8000, 1, '删除通告', 'PUT', '/nav/notices/{id}');
```
