```
// 字符串类型
typedef struct {
    size_t      len;  // 长度
    u_char     *data; // 字符数据
} ngx_str_t;
```
```
// 键值对类型
ypedef struct {
    ngx_str_t   key;
    ngx_str_t   value;
} ngx_keyval_t;
```

```
typedef struct {
    unsigned    len:28;

    unsigned    valid:1;
    unsigned    no_cacheable:1;
    unsigned    not_found:1;
    unsigned    escape:1;

    u_char     *data;
} ngx_variable_value_t
```