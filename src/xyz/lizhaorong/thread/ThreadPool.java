package xyz.lizhaorong.thread;

public interface ThreadPool<Job extends Runnable> {
    /**
     * 执行一个Job
     * @param job runnable
     */
    void excute(Job job);

    /**
     * 关闭线程池
     */
    void shutdown();

    /**
     * 增加工作者数量
     * @param num 增加的数量
     */
    void addWorkers(int num);

    /**
     * 减少工作者数量
     * @param num 减少的数量
     */
    void removeWorkers(int num);

    /**
     * 获取正在等待执行的任务数量
     * @return 等待的数量
     */
    int getJobSize();


}

