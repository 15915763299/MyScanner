package com.myscanner.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.myscanner.R;
import com.myscanner.utils.DisplayUtils;


/**
 * 扫码框遮挡界面
 */
public class MashCodeView extends View {

    private Paint paint;

    //灰色背景
    private int finderMaskColor;
    private Rect topRect, bottomRect, rightRect, leftRect, middleRect;

    //记录View的宽高，用于计算扫描框在原图中的位置与大小
    private int measuredWidth;
    private int measuredHeight;

    //四个角上的边框
    private int strokeColor;
    private int CORNER_STROKE_WIDTH;    //宽度
    private int CORNER_STROKE_LENGTH;   //长度

    //扫描线
    private Rect lineRect;
    private Bitmap line;

    //中间滑动线的位置
    private int slidePosition;
    //中间那条线每次刷新移动的距离
    private static final int SPEEN_DISTANCE = 5;
    //扫描框中的中间线的宽度
    private static final int MIDDLE_LINE_WIDTH = 6;
    //扫描框中的中间线的与扫描框左右的间隙
    private static final int MIDDLE_LINE_PADDING = 5;
    //提示文字与扫码框的间距
    private int TIP_MARGIN_TOP;
    //动画刷新间隔
    private static final int ANIMATION_DELAY = 20;

    public MashCodeView(Context context) {
        super(context);
        init(context);
    }

    public MashCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        finderMaskColor = ContextCompat.getColor(context, R.color.background_shadow);

        topRect = new Rect();
        bottomRect = new Rect();
        rightRect = new Rect();
        leftRect = new Rect();
        middleRect = new Rect();
        lineRect = new Rect();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getContext(), R.mipmap.ic_scan_line);
        if (bitmapDrawable != null) {
            line = bitmapDrawable.getBitmap();
        }

        strokeColor = ContextCompat.getColor(context, R.color.blue);
        CORNER_STROKE_WIDTH = DisplayUtils.dip2px(context, 3);
        int tenDP = DisplayUtils.dip2px(context, 10);
        CORNER_STROKE_LENGTH = tenDP * 2;
        TIP_MARGIN_TOP = tenDP * 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        int scanSize = measuredWidth * 4 / 5; //扫描框的大小
        int left = (measuredWidth - scanSize) / 2;
        int top = (measuredHeight - scanSize) * 2 / 5;
        middleRect.set(
                left,                   //left
                top,                    //top
                left + scanSize,        //right
                top + scanSize          //bottom
        );

        leftRect.set(0, middleRect.top, middleRect.left, middleRect.bottom);
        topRect.set(0, 0, measuredWidth, middleRect.top);
        rightRect.set(middleRect.right, middleRect.top, measuredWidth, middleRect.bottom);
        bottomRect.set(0, middleRect.bottom, measuredWidth, measuredHeight);

        slidePosition = middleRect.top;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(finderMaskColor);
        canvas.drawRect(leftRect, paint);
        canvas.drawRect(topRect, paint);
        canvas.drawRect(rightRect, paint);
        canvas.drawRect(bottomRect, paint);

        //边框
        paint.setColor(strokeColor);
        canvas.drawRect(middleRect.left, middleRect.top, middleRect.left + CORNER_STROKE_WIDTH,
                middleRect.top + CORNER_STROKE_LENGTH, paint);
        canvas.drawRect(middleRect.left, middleRect.top, middleRect.left + CORNER_STROKE_LENGTH,
                middleRect.top + CORNER_STROKE_WIDTH, paint);
        canvas.drawRect(middleRect.right - CORNER_STROKE_WIDTH, middleRect.top, middleRect.right,
                middleRect.top + CORNER_STROKE_LENGTH, paint);
        canvas.drawRect(middleRect.right - CORNER_STROKE_LENGTH, middleRect.top, middleRect.right,
                middleRect.top + CORNER_STROKE_WIDTH, paint);
        canvas.drawRect(middleRect.left, middleRect.bottom - CORNER_STROKE_LENGTH,
                middleRect.left + CORNER_STROKE_WIDTH, middleRect.bottom, paint);
        canvas.drawRect(middleRect.left, middleRect.bottom - CORNER_STROKE_WIDTH,
                middleRect.left + CORNER_STROKE_LENGTH, middleRect.bottom, paint);
        canvas.drawRect(middleRect.right - CORNER_STROKE_LENGTH, middleRect.bottom - CORNER_STROKE_WIDTH,
                middleRect.right, middleRect.bottom, paint);
        canvas.drawRect(middleRect.right - CORNER_STROKE_WIDTH, middleRect.bottom - CORNER_STROKE_LENGTH,
                middleRect.right, middleRect.bottom, paint);

        canvas.drawLine(middleRect.left, middleRect.top, middleRect.right, middleRect.top, paint);
        canvas.drawLine(middleRect.right, middleRect.top, middleRect.right, middleRect.bottom, paint);
        canvas.drawLine(middleRect.right, middleRect.bottom, middleRect.left, middleRect.bottom, paint);
        canvas.drawLine(middleRect.left, middleRect.bottom, middleRect.left, middleRect.top, paint);

        //扫描线条
        slidePosition += SPEEN_DISTANCE;
        if (slidePosition >= middleRect.bottom) {
            slidePosition = middleRect.top;
        }

        lineRect.set(
                middleRect.left + MIDDLE_LINE_PADDING,
                slidePosition - MIDDLE_LINE_WIDTH / 2,
                middleRect.right - MIDDLE_LINE_PADDING,
                slidePosition + MIDDLE_LINE_WIDTH / 2
        );
        if (line != null) {
            canvas.drawBitmap(line, null, lineRect, paint);
        }

        //画扫描框以下的字
        paint.setColor(Color.WHITE);
        paint.setTextSize(DisplayUtils.sp2px(getContext(), 13));
        //paint.setAlpha(0x40);//25%透明度
        //paint.setTypeface(Typeface.create("System", Typeface.BOLD));
        String tip = getResources().getString(R.string.scan_code_tip);
        float textWidth = paint.measureText(tip);
        canvas.drawText(
                tip,
                (measuredWidth - textWidth) / 2,
                middleRect.bottom + TIP_MARGIN_TOP,
                paint
        );

        //仅仅刷新扫描框的内容，其它地方不刷新
        postInvalidateDelayed(
                ANIMATION_DELAY,
                middleRect.left,
                middleRect.top,
                middleRect.right,
                middleRect.bottom
        );
    }

    /**
     * 根据图片size求出矩形框在图片所在位置
     */
    public Rect getScanImageRect(int w, int h) {
        Rect rect = new Rect();
        float tempWidth = w / (float) measuredWidth;
        float tempHeight = h / (float) measuredHeight;
        rect.left = (int) (middleRect.left * tempWidth);
        rect.right = (int) (middleRect.right * tempWidth);
        rect.top = (int) (middleRect.top * tempHeight);
        rect.bottom = (int) (middleRect.bottom * tempHeight);
        return rect;
    }

}
