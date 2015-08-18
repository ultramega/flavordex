package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
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

import java.util.ArrayList;

/**
 * Custom view to render a simple radar graph with configurable values, labels, and scale. Also
 * supports editing method calls. Can be rotated with animation.
 *
 * @author Steve Guidetti
 */
public class RadarView extends View {
    /**
     * Keys for saving the state of the view
     */
    private static final String STATE_SUPER_STATE = "super_state";
    private static final String STATE_MAX_VALUE = "max_value";
    private static final String STATE_DATA = "data";
    private static final String STATE_SELECTED = "selected";
    private static final String STATE_OFFSET = "offset";
    private static final String STATE_EDITABLE = "editable";

    /**
     * Static color values
     */
    private static final int COLOR_CIRCLE = 0xffcccccc;
    private static final int COLOR_SELECTED = 0xffefac1d;
    private static final int COLOR_LABEL = 0xffffffff;
    private static final int COLOR_POLYGON = 0xdd0066ff;
    private static final int COLOR_POLYGON_EDITABLE = 0xddff66ff;

    /**
     * Static paints
     */
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

    /**
     * The maximum value any data point can have
     */
    private int mMaxValue = 5;

    /**
     * The data to render
     */
    private ArrayList<RadarHolder> mData;

    /**
     * The offset of the center point from the edges in pixels
     */
    private int mCenter;

    /**
     * The distance between values in pixels
     */
    private int mScale;

    /**
     * The pre-calculated coordinates of each intersection of spoke and value
     */
    private float[][][] mPoints;

    /**
     * Whether the coordinates have been calculated
     */
    private boolean mCalculated = false;

    /**
     * The array index of the currently selected data point
     */
    private int mSelected;

    /**
     * The current angle offset of the chart
     */
    private double mOffset;

    /**
     * Whether the chart is in edit mode
     */
    private boolean mEditable;

    /**
     * Paint used for drawing the polygon representing the data values
     */
    private Paint mFgPaint;

    /**
     * Paint used for drawing the center point
     */
    private Paint mCenterPaint;

    /**
     * Used to animate the chart in edit mode
     */
    private AnimationQueue mAnimationQueue;

    /**
     * Whether the chart is currently moving
     */
    private boolean mIsAnimating;

    public RadarView(Context context) {
        this(context, null, 0);
    }

    public RadarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mFgPaint = new Paint(sLinePaint);
        mFgPaint.setColor(COLOR_POLYGON);
        mFgPaint.setStyle(Paint.Style.STROKE);
        mFgPaint.setStrokeWidth(5);
        mFgPaint.setStrokeJoin(Paint.Join.ROUND);

        mCenterPaint = new Paint();
        mCenterPaint.setColor(COLOR_CIRCLE);
        mCenterPaint.setAntiAlias(true);
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
        Rect bounds = new Rect();
        for(RadarHolder item : mData) {
            sLabelPaint.getTextBounds(item.name, 0, item.name.length(), bounds);
            int width = bounds.right - bounds.left;
            if(width > hPadding) {
                hPadding = width;
            }
        }
        final int vPadding = bounds.bottom - bounds.top;

        final int radius = mCenter - hPadding;
        mScale = radius / mMaxValue;

        if(mData != null) {
            final int n = mData.size();
            double[] angles = new double[n];
            mPoints = new float[n][mMaxValue + 2][2];

            // for each spoke
            for(int i = 0; i < n; i++) {
                final double offset = Math.PI / 2 + mOffset;
                angles[i] = (i * 2 * Math.PI / -n + offset);
                final double cos = Math.cos(angles[i]);
                final double sin = Math.sin(angles[i]);

                // intersection points
                for(int j = 0; j <= mMaxValue; j++) {
                    final int r = mScale * j;
                    final float x = (float)(mCenter + r * cos);
                    final float y = (float)(mCenter - r * sin);
                    mPoints[i][j] = new float[] {x, y};
                }

                // label positions
                mPoints[i][mMaxValue + 1][0] = (float)(mCenter + (radius + mScale / 3) * cos);
                mPoints[i][mMaxValue + 1][1] =
                        (float)(mCenter - (radius + mScale) * sin) + vPadding / 2;
            }
        }

        mCalculated = true;
    }

    /**
     * Get the maximum value any data point can have.
     *
     * @return The maximum value
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Set the maximum value any data point can have.
     *
     * @param maxValue The maximum value
     */
    public void setMaxValue(int maxValue) {
        mMaxValue = Math.max(0, maxValue);
        mCalculated = false;
        invalidate();
    }

    /**
     * Does this chart have any data?
     *
     * @return Whether the chart has data
     */
    public boolean hasData() {
        return mData != null && mData.size() > 0;
    }

    /**
     * Get the data currently being rendered in this chart.
     *
     * @return An array of data points contained in RadarHolders
     */
    public ArrayList<RadarHolder> getData() {
        if(!hasData()) {
            return null;
        }
        return new ArrayList<>(mData);
    }

    /**
     * Set the data to render in this chart.
     *
     * @param data An array of data points contained in RadarHolders
     */
    public void setData(ArrayList<RadarHolder> data) {
        if(data != null) {
            for(RadarHolder item : data) {
                item.value = Math.max(0, item.value);
                item.value = Math.min(mMaxValue, item.value);
            }
            mData = new ArrayList<>(data);
        } else {
            mData = null;
        }

        mCalculated = false;
        invalidate();
    }

    /**
     * Is the chart in edit mode?
     *
     * @return Whether the chart is in edit mode
     */
    public boolean isEditable() {
        return mEditable;
    }

    /**
     * Enable or disable edit mode. The chart must have data to enable edit mode.
     *
     * @param editable True to enable edit mode
     */
    public void setEditable(boolean editable) {
        if(editable && hasData()) {
            mFgPaint.setColor(COLOR_POLYGON_EDITABLE);
            mFgPaint.setStrokeWidth(4);

            mCenterPaint.setColor(COLOR_SELECTED);

            if(mAnimationQueue == null) {
                mAnimationQueue = new AnimationQueue();
            }

            mEditable = true;
        } else {
            mFgPaint.setColor(COLOR_POLYGON);
            mFgPaint.setStrokeWidth(5);

            mCenterPaint.setColor(COLOR_CIRCLE);

            if(hasData()) {
                turnTo(0);
            }

            mEditable = false;
        }

        invalidate();
    }

    /**
     * Get the index of the currently selected data point.
     *
     * @return The array index of the selected data point
     */
    public int getSelected() {
        return mSelected;
    }

    /**
     * Turn the chart counter-clockwise.
     */
    public void turnCCW() {
        if(!mEditable || mIsAnimating) {
            return;
        }
        if(mSelected == mData.size() - 1) {
            mSelected = 0;
        } else {
            mSelected++;
        }
        turn();
    }

    /**
     * Turn the chart clockwise.
     */
    public void turnCW() {
        if(!mEditable || mIsAnimating) {
            return;
        }
        if(mSelected == 0) {
            mSelected = mData.size() - 1;
        } else {
            mSelected--;
        }
        turn();
    }

    /**
     * Turn the chart to a specific data point.
     *
     * @param key The data point to turn to
     */
    public void turnTo(int key) {
        if(!mEditable || mIsAnimating) {
            return;
        }
        if(key < 0 || key >= mData.size()) {
            return;
        }
        mSelected = key;
        turn();
    }

    /**
     * Get the value of the currently selected data point.
     *
     * @return The value of the selected data point
     */
    public int getSelectedValue() {
        if(!hasData()) {
            return 0;
        }
        return mData.get(mSelected).value;
    }

    /**
     * Set the value of the currently selected data point.
     *
     * @param value
     */
    public void setSelectedValue(int value) {
        if(!hasData()) {
            return;
        }
        value = Math.max(0, value);
        value = Math.min(mMaxValue, value);
        mData.get(mSelected).value = value;
        invalidate();
    }

    /**
     * Trigger turn animation. This will animate the chart rotating to the selected point.
     */
    private void turn() {
        mAnimationQueue.animateOffset(Math.PI / mData.size() * 2 * mSelected);
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

        if(!hasData()) {
            return;
        }

        if(!mCalculated) {
            calculatePoints();
        }

        // draw circles
        for(int i = 1; i < mMaxValue; i++) {
            canvas.drawCircle(mCenter, mCenter, mScale * i, sCirclePaint);
        }
        canvas.drawCircle(mCenter, mCenter, mScale * mMaxValue, sOuterCirclePaint);

        RadarHolder item = mData.get(0);

        // start polygon
        final Path polygon = new Path();
        polygon.moveTo(mPoints[0][item.value][0], mPoints[0][item.value][1]);

        float x, y;
        for(int i = 0; i < mData.size(); i++) {
            item = mData.get(i);

            // set colors
            final Paint linePaint;
            final Paint labelPaint;
            if(mEditable && mSelected == i) {
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

            if(Math.abs(x - mCenter) < mScale) {
                labelPaint.setTextAlign(Paint.Align.CENTER);
            } else if(x > mCenter) {
                labelPaint.setTextAlign(Paint.Align.LEFT);
            } else {
                labelPaint.setTextAlign(Paint.Align.RIGHT);
            }

            if(mEditable && mSelected == i) {
                y -= mScale / 2;
            }

            canvas.drawText(item.name, 0, item.name.length(), x, y, labelPaint);

            // add point to polygon
            x = mPoints[i][item.value][0];
            y = mPoints[i][item.value][1];
            polygon.lineTo(x, y);
        }

        // finish and draw polygon
        polygon.close();

        final ShapeDrawable polyShape = new ShapeDrawable(new PathShape(polygon, 200, 200));
        polyShape.getPaint().set(mFgPaint);
        polyShape.setBounds(0, 0, 200, 200);
        polyShape.draw(canvas);

        if(mEditable) {
            final float[] selected = mPoints[mSelected][getSelectedValue()];
            canvas.drawCircle(selected[0], selected[1], 8, sSelectedLinePaint);
        }

        canvas.drawCircle(mCenter, mCenter, 6, mCenterPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPER_STATE, super.onSaveInstanceState());

        bundle.putInt(STATE_MAX_VALUE, mMaxValue);
        bundle.putParcelableArrayList(STATE_DATA, mData);
        bundle.putInt(STATE_SELECTED, mSelected);
        bundle.putDouble(STATE_OFFSET, mOffset);
        bundle.putBoolean(STATE_EDITABLE, mEditable);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle) {
            final Bundle bundle = (Bundle)state;

            mMaxValue = bundle.getInt(STATE_MAX_VALUE, mMaxValue);
            mSelected = bundle.getInt(STATE_SELECTED, 0);
            mOffset = bundle.getDouble(STATE_OFFSET, 0.0);
            if(mEditable = bundle.getBoolean(STATE_EDITABLE, false)) {
                setEditable(true);
            }

            mData = bundle.getParcelableArrayList(STATE_DATA);

            super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER_STATE));

            return;
        }

        super.onRestoreInstanceState(state);
    }

    /**
     * Class to handle animations.
     */
    private class AnimationQueue {
        /**
         * Time in milliseconds between frames
         */
        private static final int DELAY_MS = 33;
        /**
         * Provides a queue for animations
         */
        private final Handler mHandler = new Handler();
        /**
         * The time the current animation started
         */
        private long mStartTime;
        /**
         * The duration of the current animation
         */
        private double mDuration;
        /**
         * The offset before the current animation started
         */
        private double mOriginalValue;
        /**
         * The offset after the current animation ends
         */
        private double mTargetValue;
        /**
         * Runnable to handle animation
         */
        private final Runnable mRunnable = new Runnable() {
            public void run() {
                double progress = (System.currentTimeMillis() - mStartTime) / mDuration;

                if(progress >= 1.0) {
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
         * Animate the offset value.
         *
         * @param target   Target angle offset
         * @param duration Duration in milliseconds
         */
        public void animateOffset(double target, double duration) {
            mStartTime = System.currentTimeMillis();
            mDuration = duration;
            mOriginalValue = mOffset;

            // prevent angle overflow by rotating original value
            if(mOffset == 0.0 && target > 3.0) {
                mOriginalValue = Math.PI * 2;
            } else if(target == 0.0 && mOffset > 3.0) {
                mOriginalValue = mOffset - Math.PI * 2;
            }

            mTargetValue = target;

            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable, DELAY_MS);

            mIsAnimating = true;
        }

        /**
         * Animate the offset value, with a duration of 400ms.
         *
         * @param target Target angle offset
         */
        public void animateOffset(double target) {
            animateOffset(target, 400.0);
        }
    }
}
