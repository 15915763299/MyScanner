/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myscanner.scan;

import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.util.concurrent.CountDownLatch;

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
