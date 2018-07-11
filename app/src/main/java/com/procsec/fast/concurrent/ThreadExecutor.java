package com.procsec.fast.concurrent;

public class ThreadExecutor {
    /**
     * Number of processor cores available
     */
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();


    public static void execute(Runnable command) {
        new LowThread(command).start();
    }
}
