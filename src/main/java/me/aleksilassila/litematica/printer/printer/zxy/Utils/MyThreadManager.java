package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import java.util.ArrayList;

public class MyThreadManager {
    public static final ArrayList<Thread> myThread = new ArrayList<>();

    public static void createThread(String threadName,Thread thread1){
        if (myThread.stream().noneMatch(thread2 -> threadName.equals(thread2.getName()))) {
            myThread.add(thread1);
            thread1.setName(threadName);
            thread1.start();
        }
    }
    public static void stopThread(String threadName){
        for (Thread thread : myThread) {
            if (threadName.equals(thread.getName())) {
                thread.interrupt();
            }
        }
    }
    public static void stopThreadAll(){
        myThread.forEach(Thread::interrupt);
    }
}
