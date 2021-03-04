package com.istat.freedev.processor.interfaces;

public interface RunnableDispatcher {
    void dispatch(Runnable runnable, long delay);

    void cancel(Runnable runnable);

    void release();
}
