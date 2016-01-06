package com.hugolee.progressplus;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Hugo on 16/1/4.
 */
public class ProgressPlus extends View {
    private final static String BUNDLE_PROGRESS = "BUNDLE_PROGRESS";
    private final static String INSTANCE_STATE = "INSTANCE_STATE";


    private float mCurrentProgress;//当前进度

    private Paint mArcProgressPaint;//画圆弧
    private Paint mArcBGPaint;//画圆弧
    private Paint mPointPaint;//画小白点

    private float mMinSize;
    private float mRingWidth;//进度条宽度
    private float mInnerRadius;//内圈宽度
    private float mOuterRadius;//外圈宽度
    private float mCenter;//圆心

    private RectF mArcRect;

    private boolean mTouchable;

    //前景色
    private SweepGradient mSweepGradient;
    private int color_start;
    private int color_end;
    int[] colors;

    //背景色
    private int color_bg;

    private ArcProgressAnimation animation;


    public ProgressPlus(Context context) {
        this(context, null);
    }

    public ProgressPlus(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressPlus(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressPlus);
        color_bg = a.getColor(R.styleable.ProgressPlus_bgColor, getResources().getColor(R.color.bgColor));
        color_start = a.getColor(R.styleable.ProgressPlus_progressColor1, getResources().getColor(R.color.progressColor1));
        color_end = a.getColor(R.styleable.ProgressPlus_progressColor2, getResources().getColor(R.color.progressColor2));
        mTouchable = a.getBoolean(R.styleable.ProgressPlus_touchable, true);
        colors = new int[]{color_start, color_end, color_start};
        mRingWidth = a.getDimensionPixelSize(R.styleable.ProgressPlus_ringWidth, getResources().getDimensionPixelSize(R.dimen.ringWidth));
        a.recycle();
        initPainters();
    }

    private void initPainters() {
        mArcProgressPaint = new Paint();
        mArcProgressPaint.setStyle(Paint.Style.STROKE);
        mArcProgressPaint.setAntiAlias(true);
        mArcProgressPaint.setStrokeCap(Paint.Cap.ROUND);

        mPointPaint = new Paint();
        mPointPaint.setAntiAlias(true);
        mPointPaint.setColor(Color.WHITE);

        mArcBGPaint = new Paint();
        mArcBGPaint.setAntiAlias(true);
        mArcBGPaint.setStyle(Paint.Style.STROKE);
        mArcBGPaint.setColor(color_bg);
    }

    private void initSize() {
        mCenter = (mMinSize - getPaddingRight() - getPaddingLeft()) / 2 + getPaddingLeft();
        mOuterRadius = (mMinSize - getPaddingLeft() - getPaddingRight() - 2 * mRingWidth) / 2;
        mInnerRadius = mOuterRadius - mRingWidth / 2;

        mArcRect = new RectF(mCenter - mOuterRadius, mCenter - mOuterRadius, mCenter + mOuterRadius, mCenter + mOuterRadius);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        canvas.rotate(90, mCenter, mCenter);

        int progressDegree = getRadian();

        // draw background arc,must draw it first!
        mArcBGPaint.setStrokeWidth(mRingWidth);
        canvas.drawArc(mArcRect, progressDegree, 360 - progressDegree, false, mArcBGPaint);
        canvas.save();

        // draw progress arc
        mArcProgressPaint.setStrokeWidth(mRingWidth);
        float position = (float) progressDegree / 360f;
        float[] positions = {0.0f, 2.0f * position / 3.0f, position};
        mSweepGradient = new SweepGradient(mCenter, mCenter, colors, positions);
        if (mCurrentProgress < 20) {
            mArcProgressPaint.setColor(color_start);
            mArcProgressPaint.setShader(null);
        } else {
            mArcProgressPaint.setShader(mSweepGradient);
        }
        canvas.drawArc(mArcRect, 0, progressDegree, false, mArcProgressPaint);
        canvas.save();


        //draw little white point,has offset
        float pointRadian = (float) Math.toRadians((progressDegree - 5) % 360);
        //fix position nearing start point
        if (progressDegree == 360 || progressDegree < 5) {
            pointRadian = (float) Math.toRadians(0);
        }
        canvas.drawCircle((float) (Math.cos(pointRadian) * (mInnerRadius + mRingWidth / 2)) + mCenter,
                                 (float) (Math.sin(pointRadian) * (mInnerRadius + mRingWidth / 2)) + mCenter,
                                 mRingWidth / 4,
                                 mPointPaint);

        //reset canvas
        canvas.restore();
    }

    /**
     * get radian by current progress
     *
     * @return
     */
    private int getRadian() {
        float progress = fixProgress(mCurrentProgress);
        return (int) (progress / 100 * 360);
    }

    private float fixProgress(float p) {
        if (p < 0) {
            p = 0;
        }
        if (p > 100) {
            p = 100;
        }
        return p;
    }

    public void setProgress(int p) {
        setProgress(p, true);
    }

    public void setProgress(int p, boolean isAnim) {
        p = (int) fixProgress(p);
        if (p == mCurrentProgress) {
            return;
        }
        //no anim
        if (!isAnim) {
            mCurrentProgress = p;
            invalidate();
            return;
        }
        //cancel previous anim
        clearAnimation();
        //set mCurrentProgress with anim
        startAnimation(animation = new ArcProgressAnimation(p));
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Integer.MAX_VALUE;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than width
            height = Math.min(width, heightSize);
        } else {
            height = width;
        }
        mMinSize = Math.min(width, height);
        initSize();
        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (!mTouchable) {
                    return false;
                }
                //cancel animation first
                if (animation != null) {
                    clearAnimation();
                }
                float x = event.getX();
                float y = event.getY();
                double distance = Math.sqrt((x - mCenter) * (x - mCenter) + (y - mCenter) * (y - mCenter));
                //touch outside the ring
                if (distance < mInnerRadius - 50 || distance > mInnerRadius + mRingWidth + 50) {
                    return false;
                }
                //disable parents' touch event
                ViewParent vp = getParent();
                while (vp != null) {
                    vp.requestDisallowInterceptTouchEvent(true);
                    vp = vp.getParent();
                }
                double degree = Math.atan2((y - mCenter), x - mCenter);
                setProgress((int) (((degree / Math.PI * 180.0 + 270.0) % 360.0) / 360.0 * 100.0), false);
                return true;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        initPainters();
        initSize();
        if (state instanceof Bundle) {
            setProgress((int) ((Bundle) state).getFloat(BUNDLE_PROGRESS, 0.0f));
            super.onRestoreInstanceState(((Bundle) state).getParcelable(INSTANCE_STATE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    //
    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(BUNDLE_PROGRESS, mCurrentProgress);
        return bundle;
    }

    public class ArcProgressAnimation extends Animation {

        private float oldProgress;
        private float newProgress;

        public ArcProgressAnimation(float newProgress) {
            this.oldProgress = mCurrentProgress;
            this.newProgress = newProgress;
            setDuration((long) (Math.abs(newProgress - oldProgress) * 36));
            setInterpolator(new AccelerateDecelerateInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            mCurrentProgress = (oldProgress + ((newProgress - oldProgress) * interpolatedTime));
            invalidate();
        }
    }

}
