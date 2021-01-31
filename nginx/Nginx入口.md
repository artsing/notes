```
int ngx_cdecl
main(int argc, char *const *argv)
{
    ngx_buf_t        *b;
    ngx_log_t        *log;
    ngx_uint_t        i;
    ngx_cycle_t      *cycle, init_cycle;
    ngx_conf_dump_t  *cd;
    ngx_core_conf_t  *ccf;

	// 针对darwin, freebsd 的调试初始化
	// NGX_DEBUG_MALLOC宏有效时设置ngx_debug_malloc=1
	// ngx_slab_junk宏函数定义时会根据是否ngx_debug_malloc=1，
	// 来 调用ngx_memset(p, 0xA5, size)函数把内存值设为0xA5
    ngx_debug_init();

	// 保持系统调用错误号和对应的错误描述，保存在ngx_sys_errlist数组中；
	// 目的是避免每次调用streeror系统调用，从而提高效率。
	// ngx_sys_errlist数组在ngx_streeror中被使用，
	// 最终会被写错误日志的时候用到。
    if (ngx_strerror_init() != NGX_OK) {
        return 1;
    }

	// 获取nginx运行时的参数，根据参数设置对应的全局变量值
    if (ngx_get_options(argc, argv) != NGX_OK) {
        return 1;
    }


	// 启动参数有 -v -V -? -h 时会显示版本信息
    if (ngx_show_version) {
        ngx_show_version_info();

		// 不用测试配置文件时直接退出
        if (!ngx_test_config) {
            return 0;
        }
    }

	// 操作系统最大的socket数，将在ngx_os_init中设置该全局变量
    /* TODO */ ngx_max_sockets = -1;

	// 时间初始化
    ngx_time_init();

#if (NGX_PCRE)
	// 正则表达式相关初始化
    ngx_regex_init();
#endif
	// 进程id
    ngx_pid = ngx_getpid();
	// 父进程id
    ngx_parent = ngx_getppid();

	// 日志初始化：根据ngx_prefix路径前缀打开日志文件
    log = ngx_log_init(ngx_prefix);
    if (log == NULL) {
        return 1;
    }

    /* STUB */
#if (NGX_OPENSSL)
	// 安全套接层(SSL)相关初始化
    ngx_ssl_init(log);
#endif

    /*
     * init_cycle->log is required for signal handlers and
     * ngx_process_options()
     */
	// 把init_cycle那块空间清零初始化
    ngx_memzero(&init_cycle, sizeof(ngx_cycle_t));
    init_cycle.log = log;
    ngx_cycle = &init_cycle;

	// 创建init_cycle的内存池空间
    init_cycle.pool = ngx_create_pool(1024, log);
    if (init_cycle.pool == NULL) {
        return 1;
    }

	// 保存nginx启动参数
	// 保存argc, argv, environ
    if (ngx_save_argv(&init_cycle, argc, argv) != NGX_OK) {
        return 1;
    }
	// 根据options、宏定义初始化 cycle 中以下字段：
	// conf_file(配置文件 默认/usr/local/nginx/conf/nginx.conf),
	// prefix(路径前缀 默认/usr/local/nginx/),
	// conf_prefix(配置文件路径前缀 默认/usr/local/nignx/conf/),
	// conf_param(配置参数 例如这样启动nginx -g "daemon off;"
	//            命令中"daemon off;"为配置参数),
	// log.log_level 日志等级（测试配置文件时设置 nginx -t/T）
    if (ngx_process_options(&init_cycle) != NGX_OK) {
        return 1;
    }
	// 获取操作系统相关信息：
	// 1、高速缓存块(cacheline)大小
	// 2、内存页大小(pagesize)
	// 等
    if (ngx_os_init(log) != NGX_OK) {
        return 1;
    }

    /*
     * ngx_crc32_table_init() requires ngx_cacheline_size set in ngx_os_init()
     */
	// CRC32 校验表的初始化
    if (ngx_crc32_table_init() != NGX_OK) {
        return 1;
    }

    /*
     * ngx_slab_sizes_init() requires ngx_pagesize set in ngx_os_init()
     */
	// slab是Linux操作系统的一种内存分配机制
    ngx_slab_sizes_init();

	// 添加继承的套接字，从环境变量NGINX中获取监听套接字文件描述符
	// 套接字文件描述符是父进程使用execve执行时写入环境变量NGINX中的
    if (ngx_add_inherited_sockets(&init_cycle) != NGX_OK) {
        return 1;
    }

	// 预初始化模块数组，给index、name赋值
	// index 表示模块在数组中的位置
	// name 是模块的字符串名称
    if (ngx_preinit_modules() != NGX_OK) {
        return 1;
    }

	// 初始化cycle
    cycle = ngx_init_cycle(&init_cycle);
    if (cycle == NULL) {
        if (ngx_test_config) {
            ngx_log_stderr(0, "configuration file %s test failed",
                           init_cycle.conf_file.data);
        }

        return 1;
    }

	// 测试配置文件时
    if (ngx_test_config) {
        if (!ngx_quiet_mode) {
            ngx_log_stderr(0, "configuration file %s test is successful",
                           cycle->conf_file.data);
        }

        if (ngx_dump_config) {
            cd = cycle->config_dump.elts;

            for (i = 0; i < cycle->config_dump.nelts; i++) {

                ngx_write_stdout("# configuration file ");
                (void) ngx_write_fd(ngx_stdout, cd[i].name.data,
                                    cd[i].name.len);
                ngx_write_stdout(":" NGX_LINEFEED);

                b = cd[i].buffer;

                (void) ngx_write_fd(ngx_stdout, b->pos, b->last - b->pos);
                ngx_write_stdout(NGX_LINEFEED);
            }
        }

        return 0;
    }

	// nginx -s signal 运行时
 	// 发送信号 signal (quit, stop, reload, reopen)
 	// 发送信号使用 kill 系统调用
    if (ngx_signal) {
        return ngx_signal_process(cycle, ngx_signal);
    }

    ngx_os_status(cycle->log);

    ngx_cycle = cycle;

	// 从配置上下文结构中获取核心模块(core)配置信息
	// 配置上下文结构在 init_cycle 中解析配置文件前
	// 和解析时创建，在解析过程中赋值的
    ccf = (ngx_core_conf_t *) ngx_get_conf(cycle->conf_ctx, ngx_core_module);

    if (ccf->master && ngx_process == NGX_PROCESS_SINGLE) {
        ngx_process = NGX_PROCESS_MASTER;
    }

#if !(NGX_WIN32)
	// 初始化信号
	// 使用 sigaction 系统调用注册信号处理函数
    if (ngx_init_signals(cycle->log) != NGX_OK) {
        return 1;
    }

	// 判断是否把master变成daemon进程
    if (!ngx_inherited && ccf->daemon) {
		// 通过fork()创建子进程
		// 设置会话id setsid()
		// 重定向STDIN_FILENO、STDOUT_FILENO到/dev/null
        if (ngx_daemon(cycle->log) != NGX_OK) {
            return 1;
        }

        ngx_daemonized = 1;
    }

    if (ngx_inherited) {
        ngx_daemonized = 1;
    }

#endif
	// 创建pid文件，把master的进程id写入文件
    if (ngx_create_pidfile(&ccf->pid, cycle->log) != NGX_OK) {
        return 1;
    }
	// 日志重定向 把 stderr 重定向到日志文件
    if (ngx_log_redirect_stderr(cycle) != NGX_OK) {
        return 1;
    }

    if (log->file->fd != ngx_stderr) {
        if (ngx_close_file(log->file->fd) == NGX_FILE_ERROR) {
            ngx_log_error(NGX_LOG_ALERT, cycle->log, ngx_errno,
                          ngx_close_file_n " built-in log failed");
        }
    }

    ngx_use_stderr = 0;

	// 判断启动模式
	// 单进程模式，只有一个master进程,master同时处理业务
    if (ngx_process == NGX_PROCESS_SINGLE) {
        ngx_single_process_cycle(cycle);

	// 多进程模式，master进程和多个worker进程
	// master 管理进程不处理业务，处理信号管理worker进程
	// worker 工作进程处理业务(处理用户请求)
    } else {
        ngx_master_process_cycle(cycle);
    }

    return 0;
}
```
```
void
ngx_single_process_cycle(ngx_cycle_t *cycle)
{
    ngx_uint_t  i;
	// 设置环境变量到核心模块配置结构(ngx_core_conf_t)
    if (ngx_set_environment(cycle, NULL) == NULL) {
        /* fatal */
        exit(2);
    }

	// 执行模块“初始化进程(init_process)”回调函数
    for (i = 0; cycle->modules[i]; i++) {
        if (cycle->modules[i]->init_process) {
            if (cycle->modules[i]->init_process(cycle) == NGX_ERROR) {
                /* fatal */
                exit(2);
            }
        }
    }

    for ( ;; ) {
        ngx_log_debug0(NGX_LOG_DEBUG_EVENT, cycle->log, 0, "worker cycle");

		// 处理事件和定时器
        ngx_process_events_and_timers(cycle);


		// start-end 这块代码对信号进行分类处理
		// 信号处理函数(ngx_signal_handler)收到信号会设置对应全局变量
		// (ngx_terminate,ngx_quit, ngx_reconfigure, ngx_reopen) = 1

		// start
		// 对quit信号的处理
        if (ngx_terminate || ngx_quit) {

            for (i = 0; cycle->modules[i]; i++) {
                if (cycle->modules[i]->exit_process) {
                    cycle->modules[i]->exit_process(cycle);
                }
            }

            ngx_master_process_exit(cycle);
        }
		// 对 reload 信号的处理
        if (ngx_reconfigure) {
            ngx_reconfigure = 0;
            ngx_log_error(NGX_LOG_NOTICE, cycle->log, 0, "reconfiguring");

            cycle = ngx_init_cycle(cycle);
            if (cycle == NULL) {
                cycle = (ngx_cycle_t *) ngx_cycle;
                continue;
            }

            ngx_cycle = cycle;
        }
		// 对 reopen 信号的处理
        if (ngx_reopen) {
            ngx_reopen = 0;
            ngx_log_error(NGX_LOG_NOTICE, cycle->log, 0, "reopening logs");
            ngx_reopen_files(cycle, (ngx_uid_t) -1);
        }
		// end
    }
}
```
```
void
ngx_master_process_cycle(ngx_cycle_t *cycle)
{
    char              *title;
    u_char            *p;
    size_t             size;
    ngx_int_t          i;
    ngx_uint_t         n, sigio;
    sigset_t           set;
    struct itimerval   itv;
    ngx_uint_t         live;
    ngx_msec_t         delay;
    ngx_listening_t   *ls;
    ngx_core_conf_t   *ccf;


    sigemptyset(&set);
    sigaddset(&set, SIGCHLD);
    sigaddset(&set, SIGALRM);
    sigaddset(&set, SIGIO);
    sigaddset(&set, SIGINT);
    sigaddset(&set, ngx_signal_value(NGX_RECONFIGURE_SIGNAL));
    sigaddset(&set, ngx_signal_value(NGX_REOPEN_SIGNAL));
    sigaddset(&set, ngx_signal_value(NGX_NOACCEPT_SIGNAL));
    sigaddset(&set, ngx_signal_value(NGX_TERMINATE_SIGNAL));
    sigaddset(&set, ngx_signal_value(NGX_SHUTDOWN_SIGNAL));
    sigaddset(&set, ngx_signal_value(NGX_CHANGEBIN_SIGNAL));

    if (sigprocmask(SIG_BLOCK, &set, NULL) == -1) {
        ngx_log_error(NGX_LOG_ALERT, cycle->log, ngx_errno,
                      "sigprocmask() failed");
    }

    sigemptyset(&set);


    size = sizeof(master_process);

    for (i = 0; i < ngx_argc; i++) {
        size += ngx_strlen(ngx_argv[i]) + 1;
    }

    title = ngx_pnalloc(cycle->pool, size);
    if (title == NULL) {
        /* fatal */
        exit(2);
    }

    p = ngx_cpymem(title, master_process, sizeof(master_process) - 1);
    for (i = 0; i < ngx_argc; i++) {
        *p++ = ' ';
        p = ngx_cpystrn(p, (u_char *) ngx_argv[i], size);
    }

    ngx_setproctitle(title);


    ccf = (ngx_core_conf_t *) ngx_get_conf(cycle->conf_ctx, ngx_core_module);

	// 启动 worker 进程
    ngx_start_worker_processes(cycle, ccf->worker_processes, NGX_PROCESS_RESPAWN);

	// 启动缓存管理进程
    ngx_start_cache_manager_processes(cycle, 0);

    ngx_new_binary = 0;
    delay = 0;
    sigio = 0;
    live = 1;

    for ( ;; ) {
        if (delay) {
            if (ngx_sigalrm) {
                sigio = 0;
                delay *= 2;
                ngx_sigalrm = 0;
            }

            ngx_log_debug1(NGX_LOG_DEBUG_EVENT, cycle->log, 0,
                           "termination cycle: %M", delay);

            itv.it_interval.tv_sec = 0;
            itv.it_interval.tv_usec = 0;
            itv.it_value.tv_sec = delay / 1000;
            itv.it_value.tv_usec = (delay % 1000 ) * 1000;

            if (setitimer(ITIMER_REAL, &itv, NULL) == -1) {
                ngx_log_error(NGX_LOG_ALERT, cycle->log, ngx_errno,
                              "setitimer() failed");
            }
        }

        ngx_log_debug0(NGX_LOG_DEBUG_EVENT, cycle->log, 0, "sigsuspend");

		// 开启信号并pause等待信号
		// 因为之前对信号进行了屏蔽，这里set为空，会开启信号
        sigsuspend(&set);

        ngx_time_update();

        ngx_log_debug1(NGX_LOG_DEBUG_EVENT, cycle->log, 0,
                       "wake up, sigio %i", sigio);

        if (ngx_reap) {
            ngx_reap = 0;
            ngx_log_debug0(NGX_LOG_DEBUG_EVENT, cycle->log, 0, "reap children");

            live = ngx_reap_children(cycle);
        }

        if (!live && (ngx_terminate || ngx_quit)) {
            ngx_master_process_exit(cycle);
        }

        if (ngx_terminate) {
            if (delay == 0) {
                delay = 50;
            }

            if (sigio) {
                sigio--;
                continue;
            }

            sigio = ccf->worker_processes + 2 /* cache processes */;

            if (delay > 1000) {
                ngx_signal_worker_processes(cycle, SIGKILL);
            } else {
                ngx_signal_worker_processes(cycle,
                                       ngx_signal_value(NGX_TERMINATE_SIGNAL));
            }

            continue;
        }

        if (ngx_quit) {
            ngx_signal_worker_processes(cycle,
                                        ngx_signal_value(NGX_SHUTDOWN_SIGNAL));

            ls = cycle->listening.elts;
            for (n = 0; n < cycle->listening.nelts; n++) {
                if (ngx_close_socket(ls[n].fd) == -1) {
                    ngx_log_error(NGX_LOG_EMERG, cycle->log, ngx_socket_errno,
                                  ngx_close_socket_n " %V failed",
                                  &ls[n].addr_text);
                }
            }
            cycle->listening.nelts = 0;

            continue;
        }

        if (ngx_reconfigure) {
            ngx_reconfigure = 0;

            if (ngx_new_binary) {
                ngx_start_worker_processes(cycle, ccf->worker_processes,
                                           NGX_PROCESS_RESPAWN);
                ngx_start_cache_manager_processes(cycle, 0);
                ngx_noaccepting = 0;

                continue;
            }

            ngx_log_error(NGX_LOG_NOTICE, cycle->log, 0, "reconfiguring");

            cycle = ngx_init_cycle(cycle);
            if (cycle == NULL) {
                cycle = (ngx_cycle_t *) ngx_cycle;
                continue;
            }

            ngx_cycle = cycle;
            ccf = (ngx_core_conf_t *) ngx_get_conf(cycle->conf_ctx,
                                                   ngx_core_module);
            ngx_start_worker_processes(cycle, ccf->worker_processes,
                                       NGX_PROCESS_JUST_RESPAWN);
            ngx_start_cache_manager_processes(cycle, 1);

            /* allow new processes to start */
            ngx_msleep(100);

            live = 1;
            ngx_signal_worker_processes(cycle,
                                        ngx_signal_value(NGX_SHUTDOWN_SIGNAL));
        }

        if (ngx_restart) {
            ngx_restart = 0;
            ngx_start_worker_processes(cycle, ccf->worker_processes,
                                       NGX_PROCESS_RESPAWN);
            ngx_start_cache_manager_processes(cycle, 0);
            live = 1;
        }

        if (ngx_reopen) {
            ngx_reopen = 0;
            ngx_log_error(NGX_LOG_NOTICE, cycle->log, 0, "reopening logs");
            ngx_reopen_files(cycle, ccf->user);
            ngx_signal_worker_processes(cycle,
                                        ngx_signal_value(NGX_REOPEN_SIGNAL));
        }

        if (ngx_change_binary) {
            ngx_change_binary = 0;
            ngx_log_error(NGX_LOG_NOTICE, cycle->log, 0, "changing binary");
            ngx_new_binary = ngx_exec_new_binary(cycle, ngx_argv);
        }

        if (ngx_noaccept) {
            ngx_noaccept = 0;
            ngx_noaccepting = 1;
            ngx_signal_worker_processes(cycle,
                                        ngx_signal_value(NGX_SHUTDOWN_SIGNAL));
        }
    }
}
```