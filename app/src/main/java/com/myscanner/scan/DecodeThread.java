package com.myscanner.scan;

import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.util.concurrent.CountDownLatch;

/**
 * 解码线程，使用CountDownLatch保证在线程启动后才能get到handler
 */
final class DecodeThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DecodeThread.class);
    private SoftReference<ActCodeScanner> sr;
    private DecodeHandler decodeHandler;
    private final CountDownLatch handlerInitLatch;

    DecodeThread(ActCodeScanner act) {
        sr = new SoftReference<>(act);
        handlerInitLatch = new CountDownLatch(1);
    }

    DecodeHandler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return decodeHandler;
    }

    @Override
    public void run() {
        logger.error("DecodeThread run");
        Looper.prepare();
        decodeHandler = new DecodeHandler(sr);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
