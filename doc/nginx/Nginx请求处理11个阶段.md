###### NGX_HTTP_POST_READ_PHASE:
 > 接收完请求头之后的第一个阶段，它位于uri重写之前，实际上很少有模块会注册在该阶段，默认的情况下，该阶段被跳过；
###### NGX_HTTP_SERVER_REWRITE_PHASE:
 > server级别的uri重写阶段，也就是该阶段执行处于server块内，location块外的重写指令，前面的章节已经说明在读取请求头的过程中nginx会根据host及端口找到对应的虚拟主机配置；
###### NGX_HTTP_FIND_CONFIG_PHASE:
> 寻找location配置阶段，该阶段使用重写之后的uri来查找对应的location，值得注意的是该阶段可能会被执行多次，因为也可能有location级别的重写指令；
###### NGX_HTTP_REWRITE_PHASE:
 > location级别的uri重写阶段，该阶段执行location基本的重写指令，也可能会被执行多次；
###### NGX_HTTP_POST_REWRITE_PHASE:
 > location级别重写的后一阶段，用来检查上阶段是否有uri重写，并根据结果跳转到合适的阶段；
######NGX_HTTP_PREACCESS_PHASE:
 > 访问权限控制的前一阶段，该阶段在权限控制阶段之前，一般也用于访问控制，比如限制访问频率，链接数等；
######NGX_HTTP_ACCESS_PHASE:
 > 访问权限控制阶段，比如基于ip黑白名单的权限控制，基于用户名密码的权限控制等；
######NGX_HTTP_POST_ACCESS_PHASE:
 > 访问权限控制的后一阶段，该阶段根据权限控制阶段的执行结果进行相应处理；
######NGX_HTTP_TRY_FILES_PHASE:
 > try_files指令的处理阶段，如果没有配置try_files指令，则该阶段被跳过；
######NGX_HTTP_CONTENT_PHASE:
 > 内容生成阶段，该阶段产生响应，并发送到客户端；
######NGX_HTTP_LOG_PHASE:
 > 日志记录阶段，该阶段记录访问日志。