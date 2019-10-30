package com.myscanner.crash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.myscanner.R;

/**
 * @author 尉迟涛
 * create time : 2019/10/30 10:42
 * description :
 */
public class ActCrashInfo extends Activity {

    public static void crashJump(Context context, Throwable ex) {
        Intent intent = new Intent(context, ActCrashInfo.class);
        intent.putExtra("ex", ex);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_crash_info);

        TextView tx = findViewById(R.id.tx);
        Throwable ex = (Throwable) getIntent().getSerializableExtra("ex");

        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n\n");
        StackTraceElement[] steList = ex.getStackTrace();
        for (StackTraceElement ste : steList) {
            sb.append(ste).append("\n");
        }
        tx.setText(sb.toString());
    }
}
