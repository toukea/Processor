package com.istat.freedev.processor.interfaces;

import android.os.Handler;
import android.os.Looper;

import com.istat.freedev.processor.utils.ProcessTools;

public interface RunnableDispatcher {
    void dispatch(Runnable runnable, long delay);

    void cancel(Runnable runnable);

    void release();

    RunnableDispatcher DEFAULT = new RunnableDispatcher() {

        private RunnableDispatcher findSuitableDispatcher() {
            return ProcessTools.isAndroidOs() ? ANDROID_HANDLER_RUNNER : SIMPLE_RUNNER;
        }

        @Override
        public void dispatch(Runnable runnable, long delay) {
            findSuitableDispatcher().dispatch(runnable, delay);
        }

        @Override
        public void cancel(Runnable runnable) {
            findSuitableDispatcher().cancel(runnable);
        }

        @Override
        public void release() {
            findSuitableDispatcher().release();
        }
    };

    RunnableDispatcher ANDROID_HANDLER_RUNNER = new RunnableDispatcher() {
        Handler handler = ProcessTools.isAndroidOs()? new Handler(Looper.getMainLooper()) : new Handler();

        @Override
        public void dispatch(Runnable runnable, long delay) {
            if (runnable != null) {
                if (delay <= 0) {
                    handler.post(runnable);
                } else {
                    handler.postDelayed(runnable, delay);
                }
            }
        }

        @Override
        public void cancel(Runnable runnable) {
            handler.removeCallbacks(runnable);
        }

        @Override
        public void release() {
            handler.removeCallbacksAndMessages(null);
        }
    };

    RunnableDispatcher SIMPLE_RUNNER = new RunnableDispatcher() {
        @Override
        public void dispatch(Runnable runnable, long delay) {
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void cancel(Runnable runnable) {

        }

        @Override
        public void release() {

        }
    };
}
