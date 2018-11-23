package com.istat.freedev.processor.interfaces;

public interface RunnableDispatcher {
    void dispatch(Runnable runnable, int delay);

    void cancel(Runnable runnable);

    void release();
}
