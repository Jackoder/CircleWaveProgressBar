package com.jackoder.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Jackoder on 2015/4/3.
 */
public class CircleWaveProgressBar extends View {

    private final int DEFAULT_ABOVE_WAVE_COLOR = 0x00ff00;
    private final int DEFAULT_BELOW_WAVE_COLOR = 0x88ff00;

    private static final int PROGRESS_MAX = 100;
    private static final int PROGRESS_DEFAULT = 0;

    private static final int DEFAULT_WAVE_HEIGHT = 20;

    private static final int WIDTH_RATE_LARGE = 1;
    private static final int WIDTH_RATE_MIDDLE = 2;
    private static final int WIDTH_RATE_SMALL = 3;

    private static final int HZ_FAST = 15;
    private static final int HZ_NORMAL = 10;
    private static final int HZ_SLOW = 5;

    private final float X_SPACE = 20;
    private final double PI2 = 2 * Math.PI;

    private Path mAboveWavePath = new Path();
    private Path mBelowWavePath = new Path();
    private Paint mAboveWavePaint = new Paint();
    private Paint mBelowWavePaint = new Paint();
    private Paint mPaint = new Paint();
    private float mAboveOffset = 0.0f;
    private float mBelowOffset = 0.0f;

    private RefreshProgressRunnable mRefreshProgressRunnable;
    private int     left, right, bottom;
    private double  omega;
    private int     mWaveWidth;
    private int     mWaveTopHeight;
    private float   mMaxRight;

    /**
     * the top wave's color
     */
    private int     mAboveWaveColor;
    /**
     * the bottom wave's color
     */
    private int     mBelowWaveColor;
    /**
     * progress
     */
    private int     mProgress = 0;
    /**
     * wave's spacing
     */
    private int     mWaveHeight;
    /**
     * determine the wave's width, the larger the width, the more animation smooth
     */
    private int     mWaveWidthRate;
    /**
     * period of redraw
     */
    private float   mWaveHz;
    /**
     * center progress text' font color
     */
    private int     mProgressTextColor;
    /**
     * center progress text's font size
     */
    private float   mProgressTextSize;
    /**
     * whether center progress text is visible
     */
    private boolean mProgressTextVisible;
    /**
     * circle stroke color
     */
    private int     strokeColor;
    /**
     * circle stroke width
     */
    private float   strokeWidth;

    /**
     * disgusting device
     */
    public static final String MODEL_GALAXY_S3 = "SAMSUNG-SGH-I747";
    public static final String MODEL_GALAXY_S4 = "SAMSUNG-SGH-I337";


    public CircleWaveProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleWaveProgressBar, R.attr.waveViewStyle, 0);
        mAboveWaveColor = attributes.getColor(R.styleable.CircleWaveProgressBar_above_wave_color, DEFAULT_ABOVE_WAVE_COLOR);
        mBelowWaveColor = attributes.getColor(R.styleable.CircleWaveProgressBar_below_wave_color, DEFAULT_BELOW_WAVE_COLOR);
        mProgress = attributes.getColor(R.styleable.CircleWaveProgressBar_progress, PROGRESS_DEFAULT);
        mWaveHeight = attributes.getDimensionPixelSize(R.styleable.CircleWaveProgressBar_wave_height, DEFAULT_WAVE_HEIGHT);
        mWaveWidthRate = attributes.getInt(R.styleable.CircleWaveProgressBar_wave_width_rate, WIDTH_RATE_MIDDLE);
        mWaveHz = attributes.getInt(R.styleable.CircleWaveProgressBar_wave_hz, HZ_NORMAL);
        mProgressTextColor = attributes.getColor(R.styleable.CircleWaveProgressBar_progressTextColor, 40);
        mProgressTextSize = attributes.getDimension(R.styleable.CircleWaveProgressBar_progressTextSize, HZ_NORMAL);
        mProgressTextVisible = attributes.getBoolean(R.styleable.CircleWaveProgressBar_progressTextVisible, false);
        strokeWidth = attributes.getDimension(R.styleable.CircleWaveProgressBar_strokeWidth, 0);
        strokeColor = attributes.getColor(R.styleable.CircleWaveProgressBar_strokeColor, 0xfff);
        initializePainters();
    }

    public CircleWaveProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initializePainters() {
        if ((Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                || (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT  && (MODEL_GALAXY_S3.equals(Build.MODEL) || MODEL_GALAXY_S4.equals(Build.MODEL)))) {
            // clipPath only available on hardware for 18+, and not S3/S4 with 4.4.2
            setLayerType(LAYER_TYPE_SOFTWARE, null);
//            Log.d("accelerated", "result: " + isHardwareAccelerated());
        }
        mAboveWavePaint.setColor(mAboveWaveColor);
        mAboveWavePaint.setStyle(Paint.Style.FILL);
        mAboveWavePaint.setAntiAlias(true);
        mBelowWavePaint.setColor(mBelowWaveColor);
        mBelowWavePaint.setStyle(Paint.Style.FILL);
        mBelowWavePaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        int width = getWidth();
        if (width > getHeight()) {
            width = getHeight();
        }
        Path path = new Path();
        path.reset();
        path.addCircle(width / 2, width / 2, width / 2, Path.Direction.CCW);
        canvas.clipPath(path);
        canvas.drawColor(Color.DKGRAY);
        canvas.drawPath(mBelowWavePath, mBelowWavePaint);
        canvas.drawPath(mAboveWavePath, mAboveWavePaint);

        if (mProgressTextVisible) {
            mPaint.setColor(mProgressTextColor);
            mPaint.setTextSize(mProgressTextSize);
            mPaint.setFakeBoldText(false);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(String.valueOf(mProgress),
                    (getWidth() - mPaint.measureText(String.valueOf(mProgress))) / 2,
                    (getHeight() - (mPaint.getFontMetrics().descent + mPaint.getFontMetrics().ascent)) / 2, mPaint);
        }

        if (strokeWidth != 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(20);
            mPaint.setColor(strokeColor);
            mPaint.setAntiAlias(true);
            canvas.drawCircle(width / 2, width / 2, width / 2, mPaint);
        }
        canvas.restore();
    }

    private void calculatePath() {
        mAboveWavePath.reset();
        mBelowWavePath.reset();
        getWaveOffset();
        float y;
        mAboveWavePath.moveTo(left, bottom);
        for (float x = 0; x <= mMaxRight; x += X_SPACE) {
            y = (float) (mWaveHeight * Math.sin(omega * (x + mAboveOffset)) + mWaveTopHeight);
            mAboveWavePath.lineTo(x, y);
        }
        mAboveWavePath.lineTo(right, bottom);
        mBelowWavePath.moveTo(left, bottom);
        for (float x = 0; x <= mMaxRight; x += X_SPACE) {
            y = (float) (mWaveHeight * Math.sin(omega * (x + mBelowOffset)) + mWaveTopHeight);
            mBelowWavePath.lineTo(x, y);
        }
        mBelowWavePath.lineTo(right, bottom);
    }

    public void setProgress(int progress) {
        this.mProgress = progress > 100 ? 100 : PROGRESS_MAX;
        computeWaveToTop();
    }

    private void startWave() {
        if (getWidth() != 0) {
            int width = getWidth();
            mWaveWidth = 2 * width * mWaveWidthRate;
            left = 0;
            right = getWidth();
            bottom = getHeight();
            mMaxRight = right + X_SPACE;
            omega = PI2 / mWaveWidth;
            mBelowOffset = mWaveWidth * 0.4f;
            computeWaveToTop();
        }
    }

    private void computeWaveToTop() {
        mWaveTopHeight = (int) (getHeight() * (1 - mProgress / 100f));
    }

    private void getWaveOffset() {
        if (mAboveOffset > Float.MAX_VALUE - mWaveHz) {
            mAboveOffset = 0;
        } else {
            mAboveOffset += mWaveHz;
        }
        if (mBelowOffset > Float.MAX_VALUE - mWaveHz) {
            mBelowOffset = 0;
        } else {
            mBelowOffset += mWaveHz;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (View.GONE == visibility) {
            removeCallbacks(mRefreshProgressRunnable);
        } else {
            removeCallbacks(mRefreshProgressRunnable);
            mRefreshProgressRunnable = new RefreshProgressRunnable();
            post(mRefreshProgressRunnable);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            if (mWaveWidth == 0) {
                startWave();
            }
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (CircleWaveProgressBar.this) {
                long start = System.currentTimeMillis();
                calculatePath();
                invalidate();
                long gap = 20 - (System.currentTimeMillis() - start);
                postDelayed(this, gap < 0 ? 0 : gap);
            }
        }
    }

}
