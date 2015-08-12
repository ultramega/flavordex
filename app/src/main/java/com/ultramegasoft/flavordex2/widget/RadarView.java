package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.ultramegasoft.flavordex2.R;

/**
 * Custom view to render a simple radar graph with configurable values, labels,
 * and scale. Also supports editing method calls. Can be rotated with animation.
 *
 * @author Steve Guidetti
 */
public class RadarView extends View {
    private static final int COLOR_CIRCLE = 0xffcccccc;
    private static final int COLOR_SELECTED = 0xffefac1d;
    private static final int COLOR_LABEL = 0xffffffff;
    private static final int COLOR_POLYGON = 0xdd0066ff;
    private static final int COLOR_POLYGON_EDITABLE = 0xddff66ff;

    private static final String SUPER_STATE_KEY = "super_state";
    private static final String MAX_VALUE_KEY = "max_value";
    private static final String DATA_KEY = "data";
    private static final String LABELS_KEY = "labels";
    private static final String SELECTED_KEY = "selected";
    private static final String OFFSET_KEY = "offset";
    private static final String EDITABLE_KEY = "editable";

    private int mMaxValue = 5;
    private int[] mData;
    private String[] mLabels = new String[0];
    private boolean mHasData;

    private int mCenter;
    private int mScale;
    private double[] mAngles;
    private float[][][] mPoints;
    private boolean mCalculated = false;
    private int mSelected;
    private double mOffset;

    private boolean mEditable;

    private static final Paint sCirclePaint;
    private static final Paint sOuterCirclePaint;
    private static final Paint sLinePaint;
    private static final Paint sSelectedLinePaint;
    private static final Paint sLabelPaint;
    private static final Paint sSelectedLabelPaint;

    static {
        sCirclePaint = new Paint();
        sCirclePaint.setStyle(Paint.Style.STROKE);
        sCirclePaint.setStrokeWidth(2);
        sCirclePaint.setColor(COLOR_CIRCLE);
        sCirclePaint.setAntiAlias(true);

        sOuterCirclePaint = new Paint(sCirclePaint);
        sOuterCirclePaint.setStrokeWidth(3);

        sLinePaint = new Paint(sCirclePaint);
        sLinePaint.setStrokeWidth(1);

        sSelectedLinePaint = new Paint(sLinePaint);
        sSelectedLinePaint.setColor(COLOR_SELECTED);
        sSelectedLinePaint.setStrokeWidth(3);

        sLabelPaint = new Paint();
        sLabelPaint.setColor(COLOR_LABEL);
        sLabelPaint.setAntiAlias(true);

        sSelectedLabelPaint = new Paint(sLabelPaint);
        sSelectedLabelPaint.setColor(COLOR_SELECTED);
    }

    private Paint mFgPaint;
    private Paint mCenterPaint;

    private AnimationQueue mAnimationQueue;
    private boolean mIsAnimating;

    public RadarView(Context context) {
        this(context, null);
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFgPaint = new Paint(sLinePaint);
        mFgPaint.setColor(COLOR_POLYGON);
        mFgPaint.setStyle(Paint.Style.STROKE);
        mFgPaint.setStrokeWidth(5);
        mFgPaint.setStrokeJoin(Paint.Join.ROUND);

        mCenterPaint = new Paint();
        mCenterPaint.setColor(COLOR_CIRCLE);
        mCenterPaint.setAntiAlias(true);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RadarView);
        final String[] labels = a.getResources().getStringArray(
                a.getResourceId(R.styleable.RadarView_labels, 0));
        setLabels(labels);

        a.recycle();
    }

    public RadarView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    /**
     * Calculate and cache all the intersection points and label positions
     */
    private void calculatePoints() {
        mCenter = getWidth() / 2;
        sLabelPaint.setTextSize(mCenter / 12);
        sSelectedLabelPaint.setTextSize(mCenter / 8);

        // calculate padding based on widest label
        int hPadding = 0;
        final String[] labels = mLabels;
        Rect bounds = new Rect();
        for (int i = 0; i < labels.length; i++) {
            sLabelPaint.getTextBounds(labels[i], 0, labels[i].length(), bounds);
            int width = bounds.right - bounds.left;
            if (width > hPadding) {
                hPadding = width;
            }
        }
        final int vPadding = bounds.bottom - bounds.top;

        final int radius = mCenter - hPadding;
        mScale = radius / mMaxValue;

        if (mLabels != null) {
            final int n = mLabels.length;
            mAngles = new double[n];
            mPoints = new float[n][mMaxValue + 2][2];

            // for each spoke
            for (int i = 0; i < n; i++) {
                final double offset = Math.PI / 2 + mOffset;
                mAngles[i] = (i * 2 * Math.PI / -n + offset);
                final double cos = Math.cos(mAngles[i]);
                final double sin = Math.sin(mAngles[i]);

                // intersection points
                for (int j = 0; j <= mMaxValue; j++) {
                    final int r = mScale * j;
                    final float x = (float) (mCenter + r * cos);
                    final float y = (float) (mCenter - r * sin);
                    mPoints[i][j] = new float[]{x, y};
                }

                // label positions
                mPoints[i][mMaxValue + 1][0] = (float) (mCenter + (radius + mScale / 3) * cos);
                mPoints[i][mMaxValue + 1][1] =
                        (float) (mCenter - (radius + mScale) * sin) + vPadding / 2;
            }
        }

        mCalculated = true;
    }

    public void setMaxValue(int maxValue) {
        if (maxValue > 0) {
            mMaxValue = maxValue;
            mCalculated = false;
            invalidate();
        }
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setData(int[] data) {
        if (data == null) {
            mData = new int[mLabels.length];
            mHasData = false;
        } else {
            for (int i = 0; i < data.length && i < mData.length; i++) {
                mData[i] = data[i];
            }
            mHasData = true;
        }
        invalidate();
    }

    public void setData(int key, int value) {
        if (mData != null && key < mData.length && value <= mMaxValue) {
            mData[key] = value;
            mHasData = true;
            invalidate();
        }
    }

    public int[] getData() {
        return mData.clone();
    }

    public int getData(int key) {
        if (mData != null && key < mData.length) {
            return mData[key];
        }
        return 0;
    }

    public void setLabels(String[] labels) {
        if (labels == null) {
            return;
        }
        mLabels = labels.clone();

        if (mData == null) {
            mData = new int[labels.length];
        }

        mCalculated = false;
        invalidate();
    }

    public void setEditable(boolean editable) {
        if (editable) {
            mFgPaint.setColor(COLOR_POLYGON_EDITABLE);
            mFgPaint.setStrokeWidth(4);

            mCenterPaint.setColor(COLOR_SELECTED);

            if (!mHasData) {
                mData = new int[mLabels.length];
            }

            if (mAnimationQueue == null) {
                mAnimationQueue = new AnimationQueue();
            }
        } else {
            mFgPaint.setColor(COLOR_POLYGON);
            mFgPaint.setStrokeWidth(5);

            mCenterPaint.setColor(COLOR_CIRCLE);

            turnTo(0);
        }
        mEditable = editable;
        invalidate();
    }

    public boolean isEditable() {
        return mEditable;
    }

    public int getSelected() {
        return mSelected;
    }

    /**
     * Turn counter-clockwise
     */
    public void turnCCW() {
        if (!mEditable || mIsAnimating) {
            return;
        }
        if (mSelected == mData.length - 1) {
            mSelected = 0;
        } else {
            mSelected++;
        }
        turn();
    }

    /**
     * Turn clockwise
     */
    public void turnCW() {
        if (!mEditable || mIsAnimating) {
            return;
        }
        if (mSelected == 0) {
            mSelected = mData.length - 1;
        } else {
            mSelected--;
        }
        turn();
    }

    /**
     * Turn to a specific spoke
     *
     * @param key spoke to turn to
     */
    public void turnTo(int key) {
        if (!mEditable || mIsAnimating) {
            return;
        }
        if (key < 0 || key >= mData.length) {
            return;
        }
        mSelected = key;
        turn();
    }

    /**
     * Increment the selected value
     *
     * @return new value
     */
    public int increaseSelected() {
        if (mEditable && mData[mSelected] < mMaxValue) {
            mData[mSelected]++;
            invalidate();
        }
        return mData[mSelected];
    }

    /**
     * Decrement the selected value
     *
     * @return new value
     */
    public int decreaseSelected() {
        if (mEditable && mData[mSelected] > 0) {
            mData[mSelected]--;
            invalidate();
        }
        return mData[mSelected];
    }

    public int getSelectedValue() {
        if (mData == null) {
            return 0;
        }
        return mData[mSelected];
    }

    public void setSelectedValue(int value) {
        setData(mSelected, value);
    }

    /**
     * Trigger turn animation
     */
    private void turn() {
        mAnimationQueue.animateOffset(Math.PI / mData.length * 2 * mSelected);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int realSize = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(realSize, realSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mCalculated) {
            calculatePoints();
        }

        // draw circles
        for (int i = 1; i < mMaxValue; i++) {
            canvas.drawCircle(mCenter, mCenter, mScale * i, sCirclePaint);
        }
        canvas.drawCircle(mCenter, mCenter, mScale * mMaxValue, sOuterCirclePaint);

        if (mData == null || mData.length < 1 || mLabels.length < 1) {
            return;
        }

        // start polygon
        final Path polygon = new Path();
        polygon.moveTo(mPoints[0][mData[0]][0], mPoints[0][mData[0]][1]);

        float x, y;
        for (int i = 0; i < mData.length; i++) {

            // set colors
            final Paint linePaint;
            final Paint labelPaint;
            if (mEditable && mSelected == i) {
                linePaint = sSelectedLinePaint;
                labelPaint = sSelectedLabelPaint;
            } else {
                linePaint = sLinePaint;
                labelPaint = sLabelPaint;
            }

            // draw spoke
            x = mPoints[i][mMaxValue][0];
            y = mPoints[i][mMaxValue][1];
            canvas.drawLine(mCenter, mCenter, x, y, linePaint);

            // draw label
            x = mPoints[i][mMaxValue + 1][0];
            y = mPoints[i][mMaxValue + 1][1];

            if (Math.abs(x - mCenter) < mScale) {
                labelPaint.setTextAlign(Paint.Align.CENTER);
            } else if (x > mCenter) {
                labelPaint.setTextAlign(Paint.Align.LEFT);
            } else {
                labelPaint.setTextAlign(Paint.Align.RIGHT);
            }

            if (mEditable && mSelected == i) {
                y -= mScale / 2;
            }

            canvas.drawText(mLabels[i], 0, mLabels[i].length(), x, y, labelPaint);

            // add point to polygon
            x = mPoints[i][mData[i]][0];
            y = mPoints[i][mData[i]][1];
            polygon.lineTo(x, y);
        }

        // finish and draw polygon
        polygon.close();

        final ShapeDrawable polyShape = new ShapeDrawable(new PathShape(polygon, 200, 200));
        polyShape.getPaint().set(mFgPaint);
        polyShape.setBounds(0, 0, 200, 200);
        polyShape.draw(canvas);

        if (mEditable) {
            final float[] selected = mPoints[mSelected][mData[mSelected]];
            canvas.drawCircle(selected[0], selected[1], 8, sSelectedLinePaint);
        }

        canvas.drawCircle(mCenter, mCenter, 6, mCenterPaint);
    }

    /**
     * Class to handle animations
     */
    private class AnimationQueue {
        private static final int DELAY_MS = 33;

        private long mStartTime;
        private double mDuration;
        private double mOriginalValue;
        private double mTargetValue;

        private final Handler mHandler = new Handler();

        private final Runnable mRunnable = new Runnable() {
            public void run() {
                double progress = (System.currentTimeMillis() - mStartTime) / mDuration;

                if (progress >= 1.0) {
                    mOffset = mTargetValue;
                    mIsAnimating = false;
                } else {
                    mOffset = mOriginalValue + (mTargetValue - mOriginalValue) * progress;
                    mHandler.postDelayed(mRunnable, DELAY_MS);
                }

                mCalculated = false;
                invalidate();
            }
        };

        /**
         * Animate the offset value
         *
         * @param target   target angle offset
         * @param duration duration in ms
         */
        public void animateOffset(double target, double duration) {
            mStartTime = System.currentTimeMillis();
            mDuration = duration;
            mOriginalValue = mOffset;

            // prevent angle overflow by rotating original value
            if (mOffset == 0.0 && target > 3.0) {
                mOriginalValue = Math.PI * 2;
            } else if (target == 0.0 && mOffset > 3.0) {
                mOriginalValue = mOffset - Math.PI * 2;
            }

            mTargetValue = target;

            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable, DELAY_MS);

            mIsAnimating = true;
        }

        /**
         * Animate the offset value
         *
         * @param target target angle offset
         */
        public void animateOffset(double target) {
            animateOffset(target, 400.0);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState());

        bundle.putInt(MAX_VALUE_KEY, mMaxValue);
        bundle.putIntArray(DATA_KEY, mData);
        bundle.putStringArray(LABELS_KEY, mLabels);
        bundle.putInt(SELECTED_KEY, mSelected);
        bundle.putDouble(OFFSET_KEY, mOffset);
        bundle.putBoolean(EDITABLE_KEY, mEditable);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;

            mMaxValue = bundle.getInt(MAX_VALUE_KEY, mMaxValue);
            mLabels = bundle.getStringArray(LABELS_KEY);
            mSelected = bundle.getInt(SELECTED_KEY, 0);
            mOffset = bundle.getDouble(OFFSET_KEY, 0.0);
            mHasData = true;
            if (mEditable = bundle.getBoolean(EDITABLE_KEY, false)) {
                setEditable(true);
            }

            mData = bundle.getIntArray(DATA_KEY);

            super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY));

            return;
        }

        super.onRestoreInstanceState(state);
    }
}