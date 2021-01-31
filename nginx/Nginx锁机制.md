###### 一、原子操作
ngx_atomic_cmp_set

```
/*
 * "cmpxchgl  r, [m]":
 *
 *     if (eax == [m]) {
 *         zf = 1;
 *         [m] = r;
 *     } else {
 *         zf = 0;
 *         eax = [m];
 *     }
 *
 *
 * The "r" means the general register.
 * The "=a" and "a" are the %eax register.
 * Although we can return result in any register, we use "a" because it is
 * used in cmpxchgl anyway.  The result is actually in %al but not in %eax,
 * however, as the code is inlined gcc can test %al as well as %eax,
 * and icc adds "movzbl %al, %eax" by itself.
 *
 * The "cc" means that flags were changed.
 */

static ngx_inline ngx_atomic_uint_t
ngx_atomic_cmp_set(ngx_atomic_t *lock, ngx_atomic_uint_t old,
    ngx_atomic_uint_t set)
{
    u_char  res;

    __asm__ volatile (

    "    lock;               "
    "    cmpxchgl  %3, %1;   "
    "    sete      %0;       "

    : "=a" (res) : "m" (*lock), "a" (old), "r" (set) : "cc", "memory");

    return res;
}
```
二、自旋锁
```

/*
 * Copyright (C) Igor Sysoev
 * Copyright (C) Nginx, Inc.
 */


#include <ngx_config.h>
#include <ngx_core.h>


void
ngx_spinlock(ngx_atomic_t *lock, ngx_atomic_int_t value, ngx_uint_t spin)
{

#if (NGX_HAVE_ATOMIC_OPS)

    ngx_uint_t  i, n;

    for ( ;; ) {

        if (*lock == 0 && ngx_atomic_cmp_set(lock, 0, value)) {
            return;
        }

        if (ngx_ncpu > 1) {

            for (n = 1; n < spin; n <<= 1) {

                for (i = 0; i < n; i++) {
                    ngx_cpu_pause();
                }

                if (*lock == 0 && ngx_atomic_cmp_set(lock, 0, value)) {
                    return;
                }
            }
        }

        ngx_sched_yield();
    }

#else

#if (NGX_THREADS)

#error ngx_spinlock() or ngx_atomic_cmp_set() are not defined !

#endif

#endif

}

```

三、读写锁
`uint值 0表示没有上锁，-1表示读写锁，其他表示读锁个数`
```

/*
 * Copyright (C) Ruslan Ermilov
 * Copyright (C) Nginx, Inc.
 */


#include <ngx_config.h>
#include <ngx_core.h>


#if (NGX_HAVE_ATOMIC_OPS)


#define NGX_RWLOCK_SPIN   2048
#define NGX_RWLOCK_WLOCK  ((ngx_atomic_uint_t) -1)


void
ngx_rwlock_wlock(ngx_atomic_t *lock)
{
    ngx_uint_t  i, n;

    for ( ;; ) {

        if (*lock == 0 && ngx_atomic_cmp_set(lock, 0, NGX_RWLOCK_WLOCK)) {
            return;
        }

        if (ngx_ncpu > 1) {

            for (n = 1; n < NGX_RWLOCK_SPIN; n <<= 1) {

                for (i = 0; i < n; i++) {
                    ngx_cpu_pause();
                }

                if (*lock == 0
                    && ngx_atomic_cmp_set(lock, 0, NGX_RWLOCK_WLOCK))
                {
                    return;
                }
            }
        }

        ngx_sched_yield();
    }
}


void
ngx_rwlock_rlock(ngx_atomic_t *lock)
{
    ngx_uint_t         i, n;
    ngx_atomic_uint_t  readers;

    for ( ;; ) {
        readers = *lock;

        if (readers != NGX_RWLOCK_WLOCK
            && ngx_atomic_cmp_set(lock, readers, readers + 1))
        {
            return;
        }

        if (ngx_ncpu > 1) {

            for (n = 1; n < NGX_RWLOCK_SPIN; n <<= 1) {

                for (i = 0; i < n; i++) {
                    ngx_cpu_pause();
                }

                readers = *lock;

                if (readers != NGX_RWLOCK_WLOCK
                    && ngx_atomic_cmp_set(lock, readers, readers + 1))
                {
                    return;
                }
            }
        }

        ngx_sched_yield();
    }
}


void
ngx_rwlock_unlock(ngx_atomic_t *lock)
{
    ngx_atomic_uint_t  readers;

    readers = *lock;

    if (readers == NGX_RWLOCK_WLOCK) {
        *lock = 0;
        return;
    }

    for ( ;; ) {

        if (ngx_atomic_cmp_set(lock, readers, readers - 1)) {
            return;
        }

        readers = *lock;
    }
}


#else

#if (NGX_HTTP_UPSTREAM_ZONE || NGX_STREAM_UPSTREAM_ZONE)

#error ngx_atomic_cmp_set() is not defined!

#endif

#endif

```
四、文件锁
```
ngx_err_t
ngx_trylock_fd(ngx_fd_t fd)
{
    struct flock  fl;

    ngx_memzero(&fl, sizeof(struct flock));
    fl.l_type = F_WRLCK; // 加锁
    fl.l_whence = SEEK_SET;
   // F_SETLK 不睡眠
    if (fcntl(fd, F_SETLK, &fl) == -1) {
        return ngx_errno;
    }

    return 0;
}


ngx_err_t
ngx_lock_fd(ngx_fd_t fd)
{
    struct flock  fl;

    ngx_memzero(&fl, sizeof(struct flock));
    fl.l_type = F_WRLCK; // 加锁
    fl.l_whence = SEEK_SET;
    // F_SETLKW 睡眠
    if (fcntl(fd, F_SETLKW, &fl) == -1) {
        return ngx_errno;
    }

    return 0;
}


ngx_err_t
ngx_unlock_fd(ngx_fd_t fd)
{
    struct flock  fl;

    ngx_memzero(&fl, sizeof(struct flock));
    fl.l_type = F_UNLCK; // 释放锁
    fl.l_whence = SEEK_SET;

    if (fcntl(fd, F_SETLK, &fl) == -1) {
        return  ngx_errno;
    }

    return 0;
}
```
五、互斥锁