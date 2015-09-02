package com.ultramegasoft.flavordex2.widget;

import android.annotation.SuppressLint;
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
     * Default color values
     */
    private static final int COLOR_CIRCLE = 0xffcccccc;
    private static final int COLOR_SELECTED = 0xffefac1d;
    private static final int COLOR_LABEL = 0xffffffff;
    private static final int COLOR_POLYGON = 0xdd0066ff;
    private static final int COLOR_POLYGON_INTERACTIVE = 0xddff66ff;

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
     * Whether the chart is in interactive mode
     */
    private boolean mInteractive;

    /**
     * Paint used for drawing the circles
     */
    private final Paint mCirclePaint;

    /**
     * Paint used for drawing the outer circle
     */
    private final Paint mOuterCirclePaint;

    /**
     * Paint used for drawing the center point
     */
    private final Paint mCenterPaint;

    /**
     * Paint used for drawing the lines
     */
    private final Paint mLinePaint;

    /**
     * Paint used for drawing the line for the selected item
     */
    private final Paint mSelectedLinePaint;

    /**
     * Paint used for drawing the labels
     */
    private final Paint mLabelPaint;

    /**
     * Paint used for drawing the label for the selected item
     */
    private final Paint mSelectedLabelPaint;

    /**
     * Paint used for drawing the polygon representing the data values
     */
    private final Paint mPolygonPaint;

    /**
     * Paint used for drawing the polygon representing the data values while in interactive mode
     */
    private final Paint mPolygonInteractivePaint;

    /**
     * Used to animate the chart in edit mode
     */
    private AnimationQueue mAnimationQueue;

    /**
     * Whether the chart is currently moving
     */
    private boolean mIsAnimating;

    /**
     * List of listeners added to this instance
     */
    private final ArrayList<RadarViewListener> mListeners = new ArrayList<>();

    /**
     * Interface for objects to listen for changes to RadarViews
     */
    public interface RadarViewListener {
        /**
         * Called when the data is changed.
         *
         * @param newData The new data
         */
        void onDataChanged(ArrayList<RadarHolder> newData);

        /**
         * Called when the selected item index is changed.
         *
         * @param index The index of the selected item
         * @param id    The id of the selected item
         * @param name  The name of the selected item
         * @param value The value of the selected item
         */
        void onSelectedItemChanged(int index, long id, String name, int value);

        /**
         * Called when the value of the selected item is changed.
         *
         * @param newValue The new value of the selected item
         */
        void onSelectedValueChanged(int newValue);

        /**
         * Called when the maximum item value is changed.
         *
         * @param maxValue The new maximum value
         */
        void onMaxValueChanged(int maxValue);

        /**
         * Called when the interactive status is changed.
         *
         * @param interactive Whether the RadarView is interactive
         */
        void onInteractiveModeChanged(boolean interactive);
    }

    public RadarView(Context context) {
        this(context, null, 0);
    }

    public RadarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RadarView);
        final int labelColor = a.getColor(R.styleable.RadarView_labelColor, COLOR_LABEL);
        final int circleColor = a.getColor(R.styleable.RadarView_circleColor, COLOR_CIRCLE);
        final int selectedColor = a.getColor(R.styleable.RadarView_selectedColor, COLOR_SELECTED);
        final int polygonColor = a.getColor(R.styleable.RadarView_polygonColor, COLOR_POLYGON);
        final int polygonColorInteractive = a.getColor(
                R.styleable.RadarView_polygonColorInteractive, COLOR_POLYGON_INTERACTIVE);
        a.recycle();

        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(2);
        mCirclePaint.setColor(circleColor);
        mCirclePaint.setAntiAlias(true);

        mOuterCirclePaint = new Paint(mCirclePaint);
        mOuterCirclePaint.setStrokeWidth(3);

        mCenterPaint = new Paint();
        mCenterPaint.setColor(circleColor);
        mCenterPaint.setAntiAlias(true);

        mLinePaint = new Paint(mCirclePaint);
        mLinePaint.setStrokeWidth(1);

        mSelectedLinePaint = new Paint(mLinePaint);
        mSelectedLinePaint.setColor(selectedColor);
        mSelectedLinePaint.setStrokeWidth(3);

        mLabelPaint = new Paint();
        mLabelPaint.setColor(labelColor);
        mLabelPaint.setAntiAlias(true);

        mSelectedLabelPaint = new Paint(mLabelPaint);
        mSelectedLabelPaint.setColor(selectedColor);

        mPolygonPaint = new Paint(mLinePaint);
        mPolygonPaint.setColor(polygonColor);
        mPolygonPaint.setStyle(Paint.Style.STROKE);
        mPolygonPaint.setStrokeWidth(5);
        mPolygonPaint.setStrokeJoin(Paint.Join.ROUND);

        mPolygonInteractivePaint = new Paint(mPolygonPaint);
        mPolygonInteractivePaint.setStrokeWidth(4);
        mPolygonInteractivePaint.setColor(polygonColorInteractive);
    }

    /**
     * Calculate and cache all the intersection points and label positions
     */
    private void calculatePoints() {
        mCenter = getWidth() / 2;
        mLabelPaint.setTextSize(mCenter / 12);
        mSelectedLabelPaint.setTextSize(mCenter / 8);

        // calculate padding based on widest label
        final Rect bounds = new Rect();

        mLabelPaint.getTextBounds("A", 0, 1, bounds);
        int vPadding = bounds.bottom - bounds.top;
        int hPadding = vPadding;

        if(mData != null) {
            for(RadarHolder item : mData) {
                mLabelPaint.getTextBounds(item.name, 0, item.name.length(), bounds);
                int width = bounds.right - bounds.left;
                if(width > hPadding) {
                    hPadding = width;
                }
            }
        }

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
     * Add a RadarViewListener to this RadarView.
     *
     * @param listener A RadarViewListener
     */
    public void addRadarViewListener(RadarViewListener listener) {
        if(listener == null) {
            return;
        }
        mListeners.add(listener);
    }

    /**
     * Remove a RadarViewListener from this RadarView.
     *
     * @param listener A RadarViewListener
     */
    public void removeRadarViewListener(RadarViewListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Set the color used for the labels.
     *
     * @param color The color hex value
     */
    public void setLabelColor(int color) {
        mLabelPaint.setColor(color);
    }

    /**
     * Set the color used for the labels.
     *
     * @return The color hex value
     */
    public int getLabelColor() {
        return mLabelPaint.getColor();
    }

    /**
     * Set the color used for the circles.
     *
     * @param color The color hex value
     */
    public void setCircleColor(int color) {
        mCirclePaint.setColor(color);
        mOuterCirclePaint.setColor(color);
        mCenterPaint.setColor(mInteractive ? mSelectedLinePaint.getColor() : color);
        mLinePaint.setColor(color);
    }

    /**
     * Get the color used for the circles.
     *
     * @return The color hex value
     */
    public int getCircleColor() {
        return mCirclePaint.getColor();
    }

    /**
     * Set the color used for the selected item.
     *
     * @param color The color hex value
     */
    public void setSelectedColor(int color) {
        mSelectedLabelPaint.setColor(color);
        mSelectedLinePaint.setColor(color);
        if(mInteractive) {
            mCenterPaint.setColor(color);
        }
    }

    /**
     * Get the color used for the selected item.
     *
     * @return The color hex value
     */
    public int getSelectedColor() {
        return mSelectedLabelPaint.getColor();
    }

    /**
     * Set the color used for the polygon.
     *
     * @param color The color hex value
     */
    public void setPolygonColor(int color) {
        mPolygonPaint.setColor(color);
    }

    /**
     * Get the color used for the polygon.
     *
     * @return The color hex value
     */
    public int getPolygonColor() {
        return mPolygonPaint.getColor();
    }

    /**
     * Set the color used for the polygon while in interactive mode.
     *
     * @param color The color hex value
     */
    public void setPolygonInteractiveColor(int color) {
        mPolygonInteractivePaint.setColor(color);
    }

    /**
     * Get the color used for the polygon while in interactive mode.
     *
     * @return The color hex value
     */
    public int getPolygonInteractiveColor() {
        return mPolygonInteractivePaint.getColor();
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
        if(mMaxValue == maxValue) {
            return;
        }

        mMaxValue = Math.max(0, maxValue);
        onMaxValueChanged(maxValue);
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
        final ArrayList<RadarHolder> data = new ArrayList<>();
        for(RadarHolder item : mData) {
            data.add(new RadarHolder(item.id, item.name, item.value));
        }
        return data;
    }

    /**
     * Set the data to render in this chart.
     *
     * @param data An array of data points contained in RadarHolders
     */
    public void setData(ArrayList<RadarHolder> data) {
        if(data != null) {
            mData = new ArrayList<>();
            for(RadarHolder item : data) {
                item.value = Math.max(0, item.value);
                item.value = Math.min(mMaxValue, item.value);
                mData.add(new RadarHolder(item.id, item.name, item.value));
            }
        } else {
            mData = null;
        }

        onDataChanged(data);
        mCalculated = false;
        invalidate();
    }

    /**
     * Is the chart in interactive mode?
     *
     * @return Whether the chart is interactive
     */
    public boolean isInteractive() {
        return mInteractive;
    }

    /**
     * Enable or disable interactive mode. The chart must have data to enable interactive mode.
     *
     * @param interactive Whether to enable interactive mode
     */
    public void setInteractive(boolean interactive) {
        if(mInteractive == interactive) {
            return;
        }

        if(interactive && hasData()) {
            mCenterPaint.setColor(mSelectedLinePaint.getColor());

            if(mAnimationQueue == null) {
                mAnimationQueue = new AnimationQueue();
            }

            mInteractive = true;
        } else {
            mCenterPaint.setColor(mCirclePaint.getColor());

            if(hasData()) {
                turnTo(0);
            }

            mInteractive = false;
        }

        onInteractiveModeChanged(interactive);
        invalidate();
    }

    /**
     * Turn the chart counter-clockwise.
     */
    public void turnCCW() {
        if(!mInteractive || mIsAnimating) {
            return;
        }
        turnTo((mSelected == mData.size() - 1) ? 0 : mSelected + 1);
    }

    /**
     * Turn the chart clockwise.
     */
    public void turnCW() {
        if(!mInteractive || mIsAnimating) {
            return;
        }
        turnTo((mSelected == 0) ? mData.size() - 1 : mSelected - 1);
    }

    /**
     * Turn the chart to a specific data point.
     *
     * @param key The data point to turn to
     */
    public void turnTo(int key) {
        if(!mInteractive || mIsAnimating) {
            return;
        }
        if(key < 0 || key >= mData.size()) {
            return;
        }
        mSelected = key;
        onSelectedItemChanged();
        turn();
    }

    /**
     * Get the index of the currently selected data point.
     *
     * @return The array index of the selected data point
     */
    public int getSelectedIndex() {
        return mSelected;
    }

    /**
     * Get the label for the currently selected data point.
     *
     * @return The name field of the selected data point
     */
    public String getSelectedName() {
        if(!hasData()) {
            return null;
        }
        return mData.get(mSelected).name;
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
     * @param value The value
     */
    public void setSelectedValue(int value) {
        if(!hasData()) {
            return;
        }
        value = Math.max(0, value);
        value = Math.min(mMaxValue, value);
        mData.get(mSelected).value = value;
        onSelectedValueChanged(value);
        invalidate();
    }

    /**
     * Trigger turn animation. This will animate the chart rotating to the selected point.
     */
    private void turn() {
        mAnimationQueue.animateOffset(Math.PI / mData.size() * 2 * mSelected);
    }

    /**
     * Notify all listeners that the data has changed.
     *
     * @param newData Te new data
     */
    private void onDataChanged(ArrayList<RadarHolder> newData) {
        for(RadarViewListener listener : mListeners) {
            listener.onDataChanged(newData);
        }
    }

    /**
     * Notify all listeners that the selected item index has changed.
     */
    private void onSelectedItemChanged() {
        final RadarHolder item = mData.get(mSelected);
        for(RadarViewListener listener : mListeners) {
            listener.onSelectedItemChanged(mSelected, item.id, item.name, item.value);
        }
    }

    /**
     * Notify all listeners that the value of the selected item has changed.
     *
     * @param newValue The new value of the selected item
     */
    private void onSelectedValueChanged(int newValue) {
        for(RadarViewListener listener : mListeners) {
            listener.onSelectedValueChanged(newValue);
        }
    }

    /**
     * Notify all listeners that the maximum item value has changed.
     *
     * @param maxValue The new maximum value
     */
    private void onMaxValueChanged(int maxValue) {
        for(RadarViewListener listener : mListeners) {
            listener.onMaxValueChanged(maxValue);
        }
    }

    /**
     * Notify all listeners that the interactive mode status has changed.
     *
     * @param interactive Whether the RadarView is interactive
     */
    private void onInteractiveModeChanged(boolean interactive) {
        for(RadarViewListener listener : mListeners) {
            listener.onInteractiveModeChanged(interactive);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int realSize = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(realSize, realSize);
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!mCalculated) {
            calculatePoints();
        }

        // draw circles
        for(int i = 1; i < mMaxValue; i++) {
            canvas.drawCircle(mCenter, mCenter, mScale * i, mCirclePaint);
        }
        canvas.drawCircle(mCenter, mCenter, mScale * mMaxValue, mOuterCirclePaint);

        if(!hasData()) {
            return;
        }

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
            if(mInteractive && mSelected == i) {
                linePaint = mSelectedLinePaint;
                labelPaint = mSelectedLabelPaint;
            } else {
                linePaint = mLinePaint;
                labelPaint = mLabelPaint;
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

            if(mInteractive && mSelected == i) {
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
        polyShape.getPaint().set(mInteractive ? mPolygonInteractivePaint : mPolygonPaint);
        polyShape.setBounds(0, 0, 200, 200);
        polyShape.draw(canvas);

        if(mInteractive) {
            final float[] selected = mPoints[mSelected][getSelectedValue()];
            canvas.drawCircle(selected[0], selected[1], 8, mSelectedLinePaint);
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
        bundle.putBoolean(STATE_EDITABLE, mInteractive);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle) {
            final Bundle bundle = (Bundle)state;

            mMaxValue = bundle.getInt(STATE_MAX_VALUE, mMaxValue);
            mData = bundle.getParcelableArrayList(STATE_DATA);
            mSelected = bundle.getInt(STATE_SELECTED, 0);
            mOffset = bundle.getDouble(STATE_OFFSET, 0.0);
            setInteractive(bundle.getBoolean(STATE_EDITABLE, false));

            super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER_STATE));
        } else {
            super.onRestoreInstanceState(state);
        }
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
