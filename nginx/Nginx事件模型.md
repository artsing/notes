注：以epoll 为例讲解Nginx的事件模型
```
#include <sys/epoll.h>
int epoll_create(int size);
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);
int epoll_wait(int epfd, struct epoll_event * events, int maxevents, int timeout);
```
1、创建epoll(epoll_create)  ngx_event.c/ngx_event_process_init:619/module->actions.init(ngx_epoll_module.c/ngx_epoll_init)

2、添加事件(epoll_ctl)
 添加accept事件 ngx_event.c/ngx_event_process_init/

3、分发事件(epoll_wait)
/event/ngx_event.c/ngx_process_events_and_timers:242/ngx_process_events（event/modules/ngx_epoll_module.c/ngx_epoll_process_events）

4、处理事件(call handler)
/event/ngx_event.c/ngx_process_events_and_timers:249:259/ngx_event_process_posted/调用事件的handler

#####*正常处理http连接到请求的事件“处理”流程*
**（区分 监听fd的读写事件、连接fd的读写事件、请求的读写事件处理函数）**
事件handler注册
添加accept事件（监听fd的读事件）handler:ngx_event_accept
||
V
ngx_event_accept最后会调用ngx_listening_t的handler:ngx_http_init_connection（注册连接fd的读事件, 设置读事件handler:ngx_http_wait_request_handler/ 写事件 handler:ngx_http_empty_handler）
```
http/ngx_http.c/ngx_http_block:333:    if (ngx_http_optimize_servers(cf, cmcf, cmcf->ports) != NGX_OK)
http/ngx_http.c/ngx_http_optimize_servers:1415:        if (ngx_http_init_listening(cf, &port[p]) != NGX_OK)
http/ngx_http.c/ngx_http_init_listening:1661:    ls = ngx_http_add_listening(cf, &addr[i]);
http/ngx_http.c/ngx_http_add_listening:1718:    ls->handler = ngx_http_init_connection;
```
||
V
ngx_http_init_request中会修改连接fd的读事件handler:ngx_http_process_request_line,并调用该函数，没有处理完会等待下一次事件到来
||
V
在 ngx_http_process_request_line中读完请求行后悔修改连接fd的读事件为handler:ngx_http_process_request_headers
||
V
ngx_http_process_request_headers中最后会修改读事件handler为ngx_http_process_request;
||
V
ngx_http_process_request最后会设置 `连接` 的读/写事件的handler为*ngx_http_request_handler*、设置`请求`的读事件函数为ngx_http_block_reading 并调用ngx_http_handler（http/ngx_http_core_module.c） 去设置`请求`的write_event_handler设置为ngx_http_core_run_phases并调用ngx_http_core_run_phases函数
`连接的事件处理函数（ngx_http_request_handler）会调用请求的事件处理函数`
```
static void
ngx_http_request_handler(ngx_event_t *ev)
{
    ...

    if (ev->write) {
        r->write_event_handler(r);

    } else {
        r->read_event_handler(r);
    }

    ngx_http_run_posted_requests(c);
}
```
||
V
ngx_http_core_run_phases会执行http请求处理的11阶段
在11阶段中七个阶段中会发送响应头（ngx_http_send_header）和响应体(ngx_http_output_filter)两个函数中调用所有过滤模块

