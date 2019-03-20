##### 1、信号
```
#include <stdio.h>
#include <signal.h>

static void hk_signal_handler(int signo);

int main(int args, char **argv) {
    signal(SIGINT, hk_signal_handler);

    if(args > 1) {
        kill(atoi(argv[1]), SIGINT);
        return 0;
    }

    switch(fork()) {
        case -1: return -1;
        case 0: break;
        default: exit(0);
    }

    if(setsid() == -1) {
        return -1;
    }

    umask(0);


    for(;;) {
        pause();
    }
    return 0;
}

static void hk_signal_handler(int signo) {
  printf(" %d  %s\n", getpid(), "exit");
}
```
##### 2、管道
```
#include <stdio.h>
#include <sys/socket.h>

int main() {
	int fd[2];
	int ret;
	struct iovec   iov[1];
	struct msghdr  msg;
	char buf[100] = {0};


	ret = socketpair(AF_LOCAL, SOCK_STREAM, 0, fd);
	if(ret == -1){
        printf("socketpair error\n");
        return 1;
    }

	bzero(&msg, sizeof(msg));
	iov[0].iov_base = buf;
	iov[0].iov_len = sizeof(buf);
	msg.msg_name = NULL;
	msg.msg_namelen = 0;
	msg.msg_iov = iov;
	msg.msg_iovlen = 1;


	switch(fork()) {
	case -1: {
		printf("fork error\n");
		return -1;
	}

	case 0: {
		ret = recvmsg(fd[0], &msg, 0);
		if (ret == -1) {
			printf("recvmsg error");
		}

		printf("[%d] receive: %s\n",getpid(), buf);
		break;
	}
	default: {

		strcpy(buf, "Hello World");

		ret = sendmsg(fd[0], &msg, 0);
		if (ret == -1) {
			printf("sendmsg err\n");
			return -1;
		}

		printf("[%d] send: %s\n",getpid(), buf);
		wait();
		break;
	}
	}

	return 0;
}

```
3、共享内存
```
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/mman.h>

typedef struct {
    u_char *addr;
    size_t size;
} shm_t;

int main(){
    shm_t share;
    char buf[] = "Hello World\n";
    share.size = 1024;
    size_t cur = 0;

    share.addr = (u_char *) mmap(NULL, share.size,
        PROT_READ|PROT_WRITE, MAP_ANON|MAP_SHARED, -1, 0);

    if (share.addr == MAP_FAILED) {
        printf("err: mmap error\n");
        return 0;
    }

    switch(fork()) {
        case -1: {
            printf("err: fork error\n");
            break;
        }
        case 0: {
            memcpy(share.addr, buf, sizeof(buf));
            break;
        }
        default: {
            sleep(1);
            write(STDOUT_FILENO, share.addr, sizeof(buf));
            if(munmap((void *) share.addr, share.size)==-1) {
                printf("err: munmap error\n");
            }
            break;
        }
    }
    return 0;
}

```