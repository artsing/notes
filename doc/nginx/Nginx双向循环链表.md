```
typedef struct ngx_queue_s  ngx_queue_t;

struct ngx_queue_s {
    ngx_queue_t  *prev; // 前一个节点
    ngx_queue_t  *next; // 后一个节点
}
```

```
// 初始化
#define ngx_queue_init(q)                                                     \
    (q)->prev = q;                                                            \
    (q)->next = q

// 置空
#define ngx_queue_empty(h)                                                    \
    (h == (h)->prev)

// 首部插入
#define ngx_queue_insert_head(h, x)                                           \
    (x)->next = (h)->next;                                                    \
    (x)->next->prev = x;                                                      \
    (x)->prev = h;                                                            \
    (h)->next = x


#define ngx_queue_insert_after   ngx_queue_insert_head

// 尾部插入
#define ngx_queue_insert_tail(h, x)                                           \
    (x)->prev = (h)->prev;                                                    \
    (x)->prev->next = x;                                                      \
    (x)->next = h;                                                            \
    (h)->prev = x

// 首节点
#define ngx_queue_head(h)                                                     \
    (h)->next

// 尾节点
#define ngx_queue_last(h)                                                     \
    (h)->prev

// 哨兵节点
#define ngx_queue_sentinel(h)                                                 \
    (h)

// 下一个节点
#define ngx_queue_next(q)                                                     \
    (q)->next

// 上一个节点
#define ngx_queue_prev(q)                                                     \
    (q)->prev


#if (NGX_DEBUG)
// 删除节点x
#define ngx_queue_remove(x)                                                   \
    (x)->next->prev = (x)->prev;                                              \
    (x)->prev->next = (x)->next;                                              \
    (x)->prev = NULL;                                                         \
    (x)->next = NULL

#else
// 删除节点x
#define ngx_queue_remove(x)                                                   \
    (x)->next->prev = (x)->prev;                                              \
    (x)->prev->next = (x)->next

#endif

// 拆分队列
#define ngx_queue_split(h, q, n)                                              \
    (n)->prev = (h)->prev;                                                    \
    (n)->prev->next = n;                                                      \
    (n)->next = q;                                                            \
    (h)->prev = (q)->prev;                                                    \
    (h)->prev->next = h;                                                      \
    (q)->prev = n;

// h节点前插入节点n
#define ngx_queue_add(h, n)                                                   \
    (h)->prev->next = (n)->next;                                              \
    (n)->next->prev = (h)->prev;                                              \
    (h)->prev = (n)->prev;                                                    \
    (h)->prev->next = h;

// 数据是通过地址相减来得到的
#define ngx_queue_data(q, type, link)                                         \
    (type *) ((u_char *) q - offsetof(type, link))

// 寻找中间元素
ngx_queue_t *ngx_queue_middle(ngx_queue_t *queue);
// 稳定的插入排序
void ngx_queue_sort(ngx_queue_t *queue,
    ngx_int_t (*cmp)(const ngx_queue_t *, const ngx_queue_t *));
```