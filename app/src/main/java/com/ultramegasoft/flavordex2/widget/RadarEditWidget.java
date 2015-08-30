package com.ultramegasoft.flavordex2.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ultramegasoft.flavordex2.R;

import java.util.ArrayList;

/**
 * A widget for interacting with a RadarView.
 *
 * @author Steve Guidetti
 */
public class RadarEditWidget extends LinearLayout {
    /**
     * The target RadarView to interact with
     */
    private RadarView mRadarView;

    /**
     * The listener for button clicks, i any
     */
    private OnButtonClickListener mListener;

    /**
     * The listener used to be notified of changes to the target RadarView
     */
    private RadarView.RadarViewListener mRadarViewListener;

    /**
     * Views from the widget's layout
     */
    private TextView mTxtItemName;
    private SeekBar mSeekBar;
    private RelativeLayout mButtonBar;

    /**
     * Interface for listeners for button bar clicks
     */
    public interface OnButtonClickListener {
        /**
         * Called when the save button is clicked
         */
        void onSave();

        /**
         * Called when the cancel button is clicked
         */
        void onCancel();
    }

    public RadarEditWidget(Context context) {
        this(context, null, 0);
    }

    public RadarEditWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarEditWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.radar_edit_widget, this);

        final Resources res = getResources();
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        final int padding = (int)res.getDimension(R.dimen.rew_padding);
        setPadding(padding, padding, padding, padding);

        mTxtItemName = (TextView)findViewById(R.id.rew_current_item);
        mSeekBar = (SeekBar)findViewById(R.id.rew_slider);
        mButtonBar = (RelativeLayout)findViewById(R.id.rew_button_bar);

        applyAttrs(attrs);

        findViewById(R.id.rew_button_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBack();
            }
        });
        findViewById(R.id.rew_button_forward).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onForward();
            }
        });

        findViewById(R.id.rew_button_save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave();
            }
        });
        findViewById(R.id.rew_button_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });
    }

    /**
     * Apply the XML attributes.
     *
     * @param attrs The AttributeSet from the constructor
     */
    private void applyAttrs(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RadarEditWidget);

        setShowButtonBar(a.getBoolean(R.styleable.RadarEditWidget_showButtonBar, false));

        final float textSize = a.getDimension(R.styleable.RadarEditWidget_textSize, 0);
        if(textSize > 0) {
            mTxtItemName.setTextSize(textSize);
        }

        mTxtItemName.setTextColor(a.getColor(R.styleable.RadarEditWidget_textColor, 0xFFFFFFFF));

        a.recycle();
    }

    /**
     * Set up the SeekBar widget.
     */
    private void setupSeekBar() {
        if(mRadarView != null) {
            final int scale = 100 / mRadarView.getMaxValue();

            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(fromUser) {
                        final int value = Math.round(progress / scale);
                        mRadarView.setSelectedValue(value);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            mSeekBar.setKeyProgressIncrement(scale);
            mSeekBar.setProgress(mRadarView.getSelectedValue() * scale);
            mSeekBar.setEnabled(mRadarView.isInteractive());
        } else {
            mSeekBar.setOnSeekBarChangeListener(null);
        }
    }

    /**
     * Create a listener for changes to the target RadarView.
     */
    private void setupRadarViewListener() {
        mRadarViewListener = new RadarView.RadarViewListener() {
            @Override
            public void onDataChanged(ArrayList<RadarHolder> newData) {
                if(newData == null || newData.isEmpty()) {
                    onSelectedItemChanged(0, 0, null, 0);
                    return;
                }
                final int index = mRadarView.getSelectedIndex();
                final RadarHolder item = newData.get(index);
                onSelectedItemChanged(index, item.id, item.name, item.value);
            }

            @Override
            public void onSelectedItemChanged(int index, long id, String name, int value) {
                setName(name);
                setValue(value);
            }

            @Override
            public void onSelectedValueChanged(int newValue) {
            }

            @Override
            public void onMaxValueChanged(int maxValue) {
                setupSeekBar();
            }

            @Override
            public void onInteractiveModeChanged(boolean interactive) {
                mSeekBar.setEnabled(interactive);
            }
        };
    }

    /**
     * Set the listener for button clicks.
     *
     * @param listener An OnButtonClickListener object
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mListener = listener;
    }

    /**
     * Set the target RadarView to interact with.
     *
     * @param radarView A RadarView to interact with
     */
    public void setTarget(RadarView radarView) {
        if(mRadarView != null) {
            mRadarView.removeRadarViewListener(mRadarViewListener);
        }

        mRadarView = radarView;
        setupSeekBar();

        if(radarView != null) {
            if(mRadarViewListener == null) {
                setupRadarViewListener();
            }
            radarView.addRadarViewListener(mRadarViewListener);
            setValue(radarView.getSelectedValue());
            setName(radarView.getSelectedName());
        }
    }

    /**
     * Show or hide the button bar.
     *
     * @param showButtonBar Whether to display the button bar
     */
    public void setShowButtonBar(boolean showButtonBar) {
        mButtonBar.setVisibility(showButtonBar ? VISIBLE : GONE);
    }

    /**
     * Get the current status of the button bar.
     *
     * @return Whether the button bar is showing
     */
    public boolean getShowButtonBar() {
        return mButtonBar.getVisibility() == VISIBLE;
    }

    /**
     * Set the value of the SeekBar.
     *
     * @param value The new value
     */
    private void setValue(int value) {
        if(mRadarView != null) {
            mSeekBar.setProgress((100 / mRadarView.getMaxValue()) * value);
        }
    }

    /**
     * Set the value of the name TextView.
     *
     * @param name The new value
     */
    private void setName(String name) {
        mTxtItemName.setText(name);
    }

    /**
     * Called when the back button is clicked. Tells the target RadarView to rotate clockwise.
     */
    private void onBack() {
        if(mRadarView != null) {
            mRadarView.turnCW();
        }
    }

    /**
     * Called when the forward button is clicked. Tels the target RadarView to rotate
     * counter-clockwise.
     */
    private void onForward() {
        if(mRadarView != null) {
            mRadarView.turnCCW();
        }
    }

    /**
     * Notify the listener that the save button was clicked.
     */
    private void onSave() {
        if(mListener != null) {
            mListener.onSave();
        }
    }

    /**
     * Notify the listener that the cancel button was clicked.
     */
    private void onCancel() {
        if(mListener != null) {
            mListener.onCancel();
        }
    }
}
