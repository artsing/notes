```
typedef struct ngx_buf_s  ngx_buf_t;
/**
 * Nginx缓冲区
 */
struct ngx_buf_s {
    u_char          *pos;           /* 待处理数据的开始标记  */
    u_char          *last;          /* 待处理数据的结尾标记 */
    off_t            file_pos;      /* 处理文件时，待处理的文件开始标记  */
    off_t            file_last;     /* 处理文件时，待处理的文件结尾标记  */

    u_char          *start;         /* 缓冲区开始的指针地址 */
    u_char          *end;           /* 缓冲区结尾的指针地址 */
    ngx_buf_tag_t    tag;           /* 缓冲区标记地址，是一个void类型的指针。 */
    ngx_file_t      *file;          /* 引用的文件 */
    ngx_buf_t       *shadow;


    /* the buf's content could be changed */

    unsigned         temporary:1;    /* 标志位，为1时，内存可修改 */

    /*
     * the buf's content is in a memory cache or in a read only memory
     * and must not be changed
     */
    unsigned         memory:1;      /* 标志位，为1时，内存只读 */

    /* the buf's content is mmap()ed and must not be changed */
    unsigned         mmap:1;        /* 标志位，为1时，mmap映射过来的内存，不可修改 */

    unsigned         recycled:1;    /* 标志位，为1时，可回收 */
    unsigned         in_file:1;     /* 标志位，为1时，表示处理的是文件 */
    unsigned         flush:1;       /* 标志位，为1时，表示需要进行flush操作 */
    unsigned         sync:1;        /* 标志位，为1时，表示可以进行同步操作，容易引起堵塞 */
    unsigned         last_buf:1;    /* 标志位，为1时，表示为缓冲区链表ngx_chain_t上的最后一块待处理缓冲区 */
    unsigned         last_in_chain:1;/* 标志位，为1时，表示为缓冲区链表ngx_chain_t上的最后一块缓冲区 */

    unsigned         last_shadow:1; /* 标志位，为1时，表示是否是最后一个影子缓冲区 */
    unsigned         temp_file:1;   /* 标志位，为1时，表示当前缓冲区是否属于临时文件 */

    /* STUB */ int   num;
};
```