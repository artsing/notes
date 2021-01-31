一、启动SSH服务
>service start sshd

二、连接服务
>ssh root@172.16.139.139

问题：
```
$ ssh root@172.16.139.139
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
Someone could be eavesdropping on you right now (man-in-the-middle attack)!
It is also possible that a host key has just been changed.
The fingerprint for the ECDSA key sent by the remote host is
SHA256:8hgJ8jpcBr1tm6HS72FpXwMrjba8MQqlqYJQLPB/Qf4.
Please contact your system administrator.
Add correct host key in /Users/wangdong/.ssh/known_hosts to get rid of this message.
Offending ECDSA key in /Users/wangdong/.ssh/known_hosts:26
ECDSA host key for 172.16.139.139 has changed and you have requested strict checking.
Host key verification failed.
```
解决：删除  /User/artsing/.ssh/know_hosts 中
```
172.16.139.13 ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBD8HsaXnpzRmMps0fvsGHvGrCEhlx8Uvqdfg1xF5oFcE0Rd7T8NSJY2gsggv1J9YLZ1YKkVhV8+Ee//CCQrO7O4=
```