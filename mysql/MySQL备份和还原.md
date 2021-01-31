// 新建一个slave, 在slave上远程备份master数据库
// 还原数据到slave数据库中
// 启动slave, 之后自动从master复制数据
```
import java.io.IOException;

/**
 * MySQL数据库备份/还原
 */
public class MySQLUtil {
    public static boolean exec()  {
        try {
            // 备份
            Process process = Runtime.getRuntime().exec("mysqldump --opt -hlocalhost --user=root --password=123456 --lock-all-tables=true --result-file=/Users/artsing/backup.sql --default-character-set=utf8 dmp --master-data=1");
            // 还原
            //Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "mysql -uroot -p123456 dmp</Users/artsing/backup.sql"});
            if (process.waitFor() == 0) {// 0 表示线程正常终止。
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void main(String[] args) throws InterruptedException {
        if (exec()) {
            System.out.println("数据库备份/还原成功！！！");
        } else {
            System.out.println("数据库备份/还原失败！！！");
        }

    }
}
```

判断目录是否存在，自动加目录分隔符'/'
```
        File saveFile = new File(savePath);
        if (!saveFile.exists()) {// 如果目录不存在
            saveFile.mkdirs();// 创建文件夹
        }
        if (!savePath.endsWith(File.separator)) {
            savePath = savePath + File.separator;
        }
```

注意：Runtime.getRuntime().exec(s) 中不能有 < > | 等重定向符号