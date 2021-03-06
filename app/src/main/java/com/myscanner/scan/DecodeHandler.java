package com.myscanner.scan;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.myscanner.R;
import com.myscanner.utils.FormatUtils;
import com.myscanner.utils.SoundUtils;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;

final class DecodeHandler extends Handler {

    private static final Logger logger = LoggerFactory.getLogger(DecodeHandler.class);

    private SoftReference<ActCodeScanner> sr;

    DecodeHandler(SoftReference<ActCodeScanner> sr) {
        this.sr = sr;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == R.id.decode) {
            if (sr != null && sr.get() != null) {
                ActCodeScanner act = sr.get();
                decode((Image) msg.obj, act.getScanner(), act.getSoundUtils(), act.getActivityHandler());
            }
        }
    }

    private void decode(Image data, ImageScanner scanner, SoundUtils soundUtils, ActivityHandler activityHandler) {
        String result = null;
        //解码，返回值为0代表失败，>0表示成功
        int scanResultCode = scanner.scanImage(data);

        if (scanResultCode != 0) {
            playBeepSoundAndVibrate(soundUtils);
            SymbolSet symbols = scanner.getResults();
            for (Symbol sym : symbols) {
                //一般只有一个
                result = sym.getData() + " " + FormatUtils.getCodeName(sym.getType());
            }
            logger.error("BarCode -----> " + result);
        }

        Message message;
        if (!TextUtils.isEmpty(result)) {
            message = activityHandler.obtainMessage(R.id.decode_succeeded, result);
        } else {
            message = activityHandler.obtainMessage(R.id.decode_finish);
        }
        message.sendToTarget();
    }

    @SuppressLint("MissingPermission")
    private void playBeepSoundAndVibrate(SoundUtils soundUtils) {
        if (soundUtils != null) {
            soundUtils.playSound(0, SoundUtils.SINGLE_PLAY);
        }
    }

}
