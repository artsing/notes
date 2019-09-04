package com.demo.cluster;

import com.demo.cluster.recovery.RecoveredAssignments;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;


/**
 * 主节点
 * @author artsing
 */
public class Master implements Watcher, Closeable {
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(Master.class);
    
    /**
     * Master 状态
     */
    enum MasterStates {
        /**
         * 运行（初始状态）
         */
        RUNNING,

        /**
         * 已选举
         */
        ELECTED,

        /**
         * 未选举
         */
        NOT_ELECTED
    }

    /**
     * 状态
     */
    private volatile MasterStates state = MasterStates.RUNNING;

    /**
     * 随机数
     */
    private Random random = new Random(this.hashCode());

    /**
     * zookeeper
     */
    private ZooKeeper zk;

    /**
     * 主机端口
     */
    private String hostPort;

    /**
     * 服务器 id
     */
    private String serverId = UUID.randomUUID().toString();

    /**
     * 会话已经连接标志
     * 已连接：true
     * 未连接：false
     */
    private volatile boolean connected = false;

    /**
     * 会话失效标志
     * 已失效：true
     * 未失效：false
     */
    private volatile boolean expired = false;

    /**
     * 任务缓存
     */
    private ChildrenCache tasksCache;

    /**
     * workers 缓存
     */
    private ChildrenCache workersCache;
    
    /**
     * 创建 Master 实例
     * 
     * @param hostPort 主机端口
     */
    public Master(String hostPort) {
        this.hostPort = hostPort;
    }
    
    
    /**
     * 创建 zookeeper 会话
     * 
     * @throws IOException io 异常
     */
    public void startZK() throws IOException {
        zk = new ZooKeeper(hostPort, 600000, this);
    }
    
    /**
     * 处理会话状态变化
     * 
     * @param event 会话事件
     */
    public void process(WatchedEvent event) {
        LOG.info("Processing event: " + event.toString());
        if(event.getType() == Event.EventType.None){
            switch (event.getState()) {
                case SyncConnected:
                    connected = true;
                    break;
                case Disconnected:
                    connected = false;
                    break;
                case Expired:
                    expired = true;
                    connected = false;

                    LOG.error("Session expiration");

                    try {
                        startZK();
                    } catch (IOException e) {
                        LOG.debug(e.toString());
                    }

                default:
                    break;
            }
        }
    }

    /**
     * 关闭 zookeeper 会话
     *
     * @throws InterruptedException 中断异常
     */
    private void stopZK() throws InterruptedException {
        zk.close();
    }

    /**
     * 获取状态
     * @return 状态
     */
    public MasterStates getState() {
        return state;
    }
    
    
    /**
     * 启动后创建必要的 zookeeper 节点
     */
    private void bootstrap(){
        createZookeeperNode("/workers", new byte[0]);
        createZookeeperNode("/assign", new byte[0]);
        createZookeeperNode("/tasks", new byte[0]);
        createZookeeperNode("/status", new byte[0]);
    }

    /**
     * 创建 zookeeper 节点
     * @param path 路径
     * @param data 数据
     */
    private void createZookeeperNode(String path, byte[] data){
        LOG.info("create zookeeper node {}", path);
        zk.create(path, 
                data, 
                Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                createZookeeperNodeCallback,
                data);
    }

    /**
     * 创建 zookeeper 节点回调函数
     */
    private StringCallback createZookeeperNodeCallback = new StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                /*
                 * Try again. Note that registering again is not a problem.
                 * If the znode has already been created, then we get a
                 * NODEEXISTS event back.
                 */
                createZookeeperNode(path, (byte[]) ctx);
                
                break;
            case OK:
                LOG.info("Parent created");
                
                break;
            case NODEEXISTS:
                LOG.warn("Parent already registered: " + path);
                
                break;
            default:
                LOG.error("Something went wrong: ", 
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };
        
    /**
     * 检查 Zookeeper 会话是否创建
     * 
     * @return boolean
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * 检查 ZooKeeper 会话是否失效.
     * 
     * @return boolean
     */
    private boolean isExpired() {
        return expired;
    }



    /**
     * 检查 master 是否存在
     */
    private void masterExists() {
        zk.exists("/master", 
                masterExistsWatcher, 
                masterExistsCallback, 
                null);
    }
    
    private StatCallback masterExistsCallback = new StatCallback() {
        public void processResult(int rc, String path, Object ctx, Stat stat){
            switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                masterExists();
                
                break;
            case OK:
                break;
            case NONODE:
                state = MasterStates.RUNNING;
                runForMaster();
                LOG.info("It sounds like the previous master is gone, " +
                    		"so let's run for master again."); 
                
                break;
            default:     
                checkMaster();
                break;
            }
        }
    };
    
    private Watcher masterExistsWatcher = new Watcher(){
        public void process(WatchedEvent e) {
            if(e.getType() == EventType.NodeDeleted) {
                assert "/master".equals( e.getPath() );
                
                runForMaster();
            }
        }
    };
    
    private void takeLeadership() {
        LOG.info("Going for list of workers");
        getWorkers();
        
        (new RecoveredAssignments(zk)).recover(new RecoveredAssignments.RecoveryCallback() {
            public void recoveryComplete(int rc, List<String> tasks) {
                if(rc == RecoveredAssignments.RecoveryCallback.FAILED) {
                    LOG.error("Recovery of assigned tasks failed.");
                } else {
                    LOG.info( "Assigning recovered tasks" );
                    getTasks();
                }
            }
        });
    }
    
    /*
     * Run for master. To run for master, we try to create the /master znode,
     * with masteCreateCallback being the callback implementation. 
     * In the case the create call succeeds, the client becomes the master.
     * If it receives a CONNECTIONLOSS event, then it needs to check if the 
     * znode has been created. In the case the znode exists, it needs to check
     * which server is the master.
     */
    
    /**
     * 创建 master 获取领导权
     */
    private void runForMaster() {
        LOG.info("Running for master");
        zk.create("/master", 
                serverId.getBytes(), 
                Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                masterCreateCallback,
                null);
    }

    /**
     * 创建 master 回调
     */
    private StringCallback masterCreateCallback = new StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    checkMaster();

                    break;
                case OK:
                    state = MasterStates.ELECTED;
                    takeLeadership();

                    break;
                case NODEEXISTS:
                    state = MasterStates.NOT_ELECTED;
                    masterExists();

                    break;
                default:
                    state = MasterStates.NOT_ELECTED;
                    LOG.error("Something went wrong when running for master.",
                            KeeperException.create(Code.get(rc), path));
            }
            LOG.info("I'm " + (state == MasterStates.ELECTED ? "" : "not ") + "the leader " + serverId);
        }
    };


    /**
     * 检查 master
     */
    private void checkMaster() {
        zk.getData("/master", false, masterCheckCallback, null);
    }

    /**
     * 检查 master 回调
     */
    private DataCallback masterCheckCallback = new DataCallback() {
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    checkMaster();

                    break;
                case NONODE:
                    runForMaster();

                    break;
                case OK:
                    if( serverId.equals( new String(data) ) ) {
                        state = MasterStates.ELECTED;
                        takeLeadership();
                    } else {
                        state = MasterStates.NOT_ELECTED;
                        masterExists();
                    }

                    break;
                default:
                    LOG.error("Error when reading data.",
                            KeeperException.create(Code.get(rc), path));
            }
        }
    };
    
    
    /**
     * This method is here for testing purposes.
     * 
     * @return size Size of the worker list
     */
    public int getWorkersSize(){
        if(workersCache == null) {
            return 0;
        } else {
            return workersCache.getList().size();
        }
    }


    /**
     * 获取 workers
     */
    private void getWorkers(){
        zk.getChildren("/workers", 
                workersChangeWatcher, 
                workersGetChildrenCallback, 
                null);
    }

    /**
     * workers 变化监测点
     */
    private Watcher workersChangeWatcher = new Watcher() {
        public void process(WatchedEvent e) {
            if(e.getType() == EventType.NodeChildrenChanged) {
                assert "/workers".equals( e.getPath() );

                getWorkers();
            }
        }
    };
    
    private ChildrenCallback workersGetChildrenCallback = new ChildrenCallback() {

        public void processResult(int rc, String path, Object ctx, List<String> children){
            switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                getWorkers();
                break;
            case OK:
                LOG.info("Successfully got a list of workers: "
                        + children.size() 
                        + " workers");
                reassignAndSet(children);
                break;
            default:
                LOG.error("getChildren failed",  
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };
    
    /*
     *******************
     *******************
     * Assigning tasks.*
     *******************
     *******************
     */
    
    private void reassignAndSet(List<String> children){
        List<String> toProcess;
        
        if(workersCache == null) {
            workersCache = new ChildrenCache(children);
            toProcess = null;
        } else {
            LOG.info( "Removing and setting" );
            toProcess = workersCache.removedAndSet( children );
        }
        
        if(toProcess != null) {
            for(String worker : toProcess){
                getAbsentWorkerTasks(worker);
            }
        }
    }
    
    private void getAbsentWorkerTasks(String worker){
        zk.getChildren("/assign/" + worker, false, workerAssignmentCallback, null);
    }
    
    private ChildrenCallback workerAssignmentCallback = new ChildrenCallback() {

        public void processResult(int rc, String path, Object ctx, List<String> children){
            switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                getAbsentWorkerTasks(path);
                
                break;
            case OK:
                LOG.info("Successfully got a list of assignments: "
                        + children.size() 
                        + " tasks");
                
                /*
                 * Reassign the tasks of the absent worker.  
                 */
                
                for(String task: children) {
                    getDataReassign(path + "/" + task, task);                    
                }
                break;
            default:
                LOG.error("getChildren failed",  KeeperException.create(Code.get(rc), path));
            }
        }
    };
    
    /*
     ************************************************
     * Recovery of tasks assigned to absent worker. * 
     ************************************************
     */
    
    /**
     * Get reassigned task data.
     * 
     * @param path Path of assigned task
     * @param task Task name excluding the path prefix
     */
    private void getDataReassign(String path, String task) {
        zk.getData(path, 
                false, 
                getDataReassignCallback, 
                task);
    }
    
    /**
     * Context for recreate operation.
     *
     */
    class RecreateTaskCtx {
        String path; 
        String task;
        byte[] data;
        
        RecreateTaskCtx(String path, String task, byte[] data) {
            this.path = path;
            this.task = task;
            this.data = data;
        }
    }

    /**
     * Get task data reassign callback.
     */
    private DataCallback getDataReassignCallback = new DataCallback() {

        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)  {
            switch(Code.get(rc)) {
            case CONNECTIONLOSS: {
                getDataReassign(path, (String) ctx);

                break;
            }
            case OK:
                recreateTask(new RecreateTaskCtx(path, (String) ctx, data));
                
                break;
            default:
                LOG.error("Something went wrong when getting data ",
                        KeeperException.create(Code.get(rc)));
            }
        }
    };
    
    /**
     * Recreate task znode in /tasks
     * 
     * @param ctx Recreate text context
     */
    private void recreateTask(RecreateTaskCtx ctx) {
        zk.create("/tasks/" + ctx.task,
                ctx.data,
                Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                recreateTaskCallback,
                ctx);
    }
    
    /**
     * Recreate znode callback
     */
    private StringCallback recreateTaskCallback = new StringCallback() {

        public void processResult(int rc, String path, Object ctx, String name) {
            switch(Code.get(rc)) {
            case CONNECTIONLOSS:
                recreateTask((RecreateTaskCtx) ctx);
       
                break;
            case OK:
                deleteAssignment(((RecreateTaskCtx) ctx).path);
                
                break;
            case NODEEXISTS:
                LOG.info("Node exists already, but if it hasn't been deleted, " +
                		"then it will eventually, so we keep trying: " + path);
                recreateTask((RecreateTaskCtx) ctx);
                
                break;
            default:
                LOG.error("Something wwnt wrong when recreating task", 
                        KeeperException.create(Code.get(rc)));
            }
        }
    };
    
    /**
     * Delete assignment of absent worker
     * 
     * @param path Path of znode to be deleted
     */
    private void deleteAssignment(String path){
        zk.delete(path, -1, taskDeletionCallback, null);
    }
    
    private VoidCallback taskDeletionCallback = new VoidCallback() {

        public void processResult(int rc, String path, Object rtx){
            switch(Code.get(rc)) {
                case CONNECTIONLOSS: {
                    deleteAssignment(path);
                    break;
                }
                case OK: {
                    LOG.info("Task correctly deleted: " + path);
                    break;
                }
                default: {
                    LOG.error("Failed to delete task data" +
                        KeeperException.create(Code.get(rc), path));
                }
            }
        }
    };
    
    /*
     ******************************************************
     ******************************************************
     * Methods for receiving new tasks and assigning them.*
     ******************************************************
     ******************************************************
     */
      
    private Watcher tasksChangeWatcher = new Watcher() {

        public void process(WatchedEvent e) {
            if(e.getType() == EventType.NodeChildrenChanged) {
                assert "/tasks".equals( e.getPath() );
                
                getTasks();
            }
        }
    };
    
    private void getTasks(){
        zk.getChildren("/tasks", 
                tasksChangeWatcher, 
                tasksGetChildrenCallback, 
                null);
    }
    
    private ChildrenCallback tasksGetChildrenCallback = new ChildrenCallback() {

        public void processResult(int rc, String path, Object ctx, List<String> children){
            switch(Code.get(rc)) {
            case CONNECTIONLOSS:
                getTasks();
                
                break;
            case OK:
                List<String> toProcess;
                if(tasksCache == null) {
                    tasksCache = new ChildrenCache(children);
                    
                    toProcess = children;
                } else {
                    toProcess = tasksCache.addedAndSet( children );
                }
                
                if(toProcess != null){
                    assignTasks(toProcess);
                } 
                
                break;
            default:
                LOG.error("getChildren failed.",  
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };

    /**
     * 分配任务
     * @param tasks 任务
     */
    private void assignTasks(List<String> tasks) {
        for(String task : tasks){
            getTaskData(task);
        }
    }

    /**
     * 获取任务数据
     * @param task 任务
     */
    private void getTaskData(String task) {
        zk.getData("/tasks/" + task, 
                false, 
                taskDataCallback, 
                task);
    }
    
    private DataCallback taskDataCallback = new DataCallback() {

        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)  {
            switch(Code.get(rc)) {
            case CONNECTIONLOSS:
                getTaskData((String) ctx);
                
                break;
            case OK:
                /*
                 * Choose worker at random.
                 */
                List<String> list = workersCache.getList();
                String designatedWorker = list.get(random.nextInt(list.size()));
                
                /*
                 * Assign task to randomly chosen worker.
                 */
                String assignmentPath = "/assign/" + 
                        designatedWorker + 
                        "/" + ctx;
                LOG.info( "Assignment path: " + assignmentPath );
                createAssignment(assignmentPath, data);
                
                break;
            default:
                LOG.error("Error when trying to get task data.", 
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };
    
    private void createAssignment(String path, byte[] data){
        zk.create(path, 
                data, 
                Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                assignTaskCallback, 
                data);
    }
    
    private StringCallback assignTaskCallback = new StringCallback() {

        public void processResult(int rc, String path, Object ctx, String name) {
            switch(Code.get(rc)) {
            case CONNECTIONLOSS:
                createAssignment(path, (byte[]) ctx);
                
                break;
            case OK:
                LOG.info("Task assigned correctly: " + name);
                deleteTask(name.substring( name.lastIndexOf("/") + 1));
                
                break;
            case NODEEXISTS: 
                LOG.warn("Task already assigned");
                
                break;
            default:
                LOG.error("Error when trying to assign task.", 
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };

    /**
     * 分配完任务删除任务
     * @param name 名称
     */
    private void deleteTask(String name){
        zk.delete("/tasks/" + name, -1, taskDeleteCallback, null);
    }
    
    private VoidCallback taskDeleteCallback = new VoidCallback(){

        public void processResult(int rc, String path, Object ctx){
            switch (Code.get(rc)) {
            case CONNECTIONLOSS:
                deleteTask(path);
                
                break;
            case OK:
                LOG.info("Successfully deleted " + path);
                
                break;
            case NONODE:
                LOG.info("Task has been deleted already");
                
                break;
            default:
                LOG.error("Something went wrong here, " + 
                        KeeperException.create(Code.get(rc), path));
            }
        }
    };
    
    /**
     * 关闭 ZooKeeper 会话
     */
    public void close() {
        if(zk != null) {
            try{
                zk.close();
            } catch (InterruptedException e) {
                LOG.warn( "Interrupted while closing ZooKeeper session.", e );
            }
        }
    }
    
    /**
     * Main method providing an example of how to run the master.
     * 
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        Master m = new Master("localhost");
        m.startZK();

        while(!m.isConnected()){
            Thread.sleep(100);
        }


        // bootstrap() creates some necessary znodes.
        m.bootstrap();


        // now runs for master.
        m.runForMaster();
        
        while(!m.isExpired()){
            Thread.sleep(1000);
        }   

        m.stopZK();
    }    
}
