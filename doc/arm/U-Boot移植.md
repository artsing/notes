### 快速入门
    # 准备文件
      1. u-boot-1.1.6.tar.bz2
      2. u-boot-1.1.6_jz2440.patch
    # 解压打补丁
      tar xvf u-boot-1.1.6.tar.bz2
      cd u-boot-1.1.6
      patch -p1 < ../u-boot-1.1.6/u-boot-1.1.6_jz2440.patch
    # 配置编译
      make 100ask2440_config
      make