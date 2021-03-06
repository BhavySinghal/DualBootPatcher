/*
 * Copyright (C) 2014  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.chenxiaolong.multibootpatcher.patcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.chenxiaolong.dualbootpatcher.R;
import com.github.chenxiaolong.multibootpatcher.nativelib.LibMbp.Device;
import com.github.chenxiaolong.multibootpatcher.nativelib.LibMbp.PatchInfo;

import java.util.ArrayList;

public class MainOptsCW implements PatcherUIListener {
    protected static interface MainOptsListener {
        public void onDeviceSelected(Device device);

        public void onPresetSelected(PatchInfo info);
    }

    private CardView vCard;
    private Spinner vDeviceSpinner;
    private LinearLayout vUnsupportedContainer;
    private Spinner vPresetSpinner;
    private TextView vPresetName;
    private LinearLayout vCustomOptsContainer;
    private CheckBox vDeviceCheckBox;
    private CheckBox vHasBootImageBox;
    private LinearLayout vBootImageContainer;
    private EditText vBootImageText;

    private Context mContext;
    private PatcherConfigState mPCS;
    private MainOptsListener mListener;

    private ArrayAdapter<String> mDeviceAdapter;
    private ArrayList<String> mDevices = new ArrayList<>();
    private ArrayAdapter<String> mPresetAdapter;
    private ArrayList<String> mPresets = new ArrayList<>();

    public MainOptsCW(Context context, PatcherConfigState pcs, CardView card,
                      MainOptsListener listener) {
        mContext = context;
        mPCS = pcs;
        mListener = listener;

        vCard = card;
        vDeviceSpinner = (Spinner) card.findViewById(R.id.spinner_device);
        vUnsupportedContainer = (LinearLayout) card.findViewById(R.id.unsupported_container);
        vPresetSpinner = (Spinner) card.findViewById(R.id.spinner_preset);
        vPresetName = (TextView) card.findViewById(R.id.preset_name);
        vCustomOptsContainer = (LinearLayout) card.findViewById(R.id.customopts_container);
        vDeviceCheckBox = (CheckBox) card.findViewById(R.id.customopts_devicecheck);
        vHasBootImageBox = (CheckBox) card.findViewById(R.id.customopts_hasbootimage);
        vBootImageContainer = (LinearLayout) card.findViewById(R.id.customopts_bootimage_container);
        vBootImageText = (EditText) card.findViewById(R.id.customopts_bootimage);

        initDevices();
        initPresets();
        initActions();
    }

    private void initDevices() {
        mDeviceAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, android.R.id.text1, mDevices);
        mDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vDeviceSpinner.setAdapter(mDeviceAdapter);

        vDeviceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onDeviceSelected(PatcherUtils.sPC.getDevices()[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initPresets() {
        mPresetAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item,
                android.R.id.text1, mPresets);
        mPresetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vPresetSpinner.setAdapter(mPresetAdapter);

        vPresetSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onPresetSelected(position);

                if (mListener != null) {
                    mListener.onPresetSelected(getPreset());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initActions() {
        // Ugly hack to prevent the text box from keeping its focus
        vBootImageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo
                        .IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN && event
                        .getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    vBootImageText.clearFocus();
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService
                            (Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(vBootImageText.getApplicationWindowToken(), 0);
                }
                return false;
            }
        });

        vHasBootImageBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setClickable(false);
                onHasBootImageChecked(isChecked);
            }
        });
    }

    @Override
    public void onCardCreate() {
        // Must rebuild from the saved state
        switch (mPCS.mState) {
        case PatcherConfigState.STATE_INITIAL:
        case PatcherConfigState.STATE_FINISHED:
            // Don't show unsupported file options initially or when patching is finished
            collapseUnsupportedOpts();

            // Don't show preset name by default
            vPresetName.setVisibility(View.GONE);

            // But when we're finished, make sure views are re-enabled
            if (mPCS.mState == PatcherConfigState.STATE_FINISHED) {
                setEnabled(true);
            }

            break;

        case PatcherConfigState.STATE_CHOSE_FILE:
        case PatcherConfigState.STATE_PATCHING:
            // Show unsupported file options if file isn't supported
            if (mPCS.mSupported) {
                collapseUnsupportedOpts();
            } else {
                expandUnsupportedOpts();
            }

            // Show custom options if we're not using a preset
            if (mPCS.mPatchInfo == null) {
                expandCustomOpts();
            } else {
                collapseCustomOpts();
            }

            // But if we're patching, then every option must be disabled
            if (mPCS.mState == PatcherConfigState.STATE_PATCHING) {
                setEnabled(false);
            }

            break;
        }
    }

    @Override
    public void onRestoreCardState(Bundle savedInstanceState) {
    }

    @Override
    public void onSaveCardState(Bundle outState) {
    }

    @Override
    public void onChoseFile() {
        // Show custom options if the file is unsupported
        if (mPCS.mSupported) {
            animateCollapseUnsupportedOpts();
        } else {
            animateExpandUnsupportedOpts();
        }
    }

    @Override
    public void onStartedPatching() {
        // Disable all views while patching
        setEnabled(false);
    }

    @Override
    public void onFinishedPatching() {
        // Enable all views after patching and hide custom options
        setEnabled(true);
        animateCollapseUnsupportedOpts();

        reset();
    }

    /**
     * Refresh the list of supported devices from libmbp.
     */
    public void refreshDevices() {
        mDevices.clear();
        for (Device device : PatcherUtils.sPC.getDevices()) {
            mDevices.add(String.format("%s (%s)", device.getId(), device.getName()));
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    /**
     * Refresh the list of available presets from libmbp.
     */
    public void refreshPresets() {
        mPresets.clear();

        // Default to using custom options
        mPresets.add(mContext.getString(R.string.preset_custom));
        for (PatchInfo info : mPCS.mPatchInfos) {
            mPresets.add(info.getId());
        }

        mPresetAdapter.notifyDataSetChanged();
    }

    /**
     * Callback for the preset spinner. Animates the showing and hiding of the custom opts views.
     *
     * @param position Selected position of preset spinner
     */
    private void onPresetSelected(int position) {
        if (position == 0) {
            //vPresetName.setText(mContext.getString(R.string.preset_custom_desc));
            animateExpandCustomOpts();
        } else {
            vPresetName.setText(mPCS.mPatchInfos[position - 1].getName());
            animateCollapseCustomOpts();
        }
    }

    /**
     * Callback for the "has boot image" checkbox. Animates the showing and hiding of the boot image
     * option views.
     *
     * @param isChecked Whether the checkbox is checked
     */
    private void onHasBootImageChecked(boolean isChecked) {
        if (isChecked) {
            animateExpandBootImageOpts();
        } else {
            animateCollapseBootImageOpts();
        }
    }

    /**
     * Create an Animator that animates the changing of a view's height.
     *
     * @param view View to animate
     * @param start Starting height
     * @param end Ending height
     * @return Animator object
     */
    public static ValueAnimator createHeightAnimator(final View view, final int start,
                                                     final int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = (Integer) valueAnimator.getAnimatedValue();
                view.setLayoutParams(params);
            }
        });
        return animator;
    }

    /**
     * Create an Animator that expands a ViewGroup. When the Animator starts, the visibility of the
     * ViewGroup will be set to View.VISIBLE and the layout height will grow from 0px to the
     * measured height, as determined by measureView() and view.getMeasuredHeight(). When the
     * Animator completes, the layout height mode will be set to wrap_content.
     *
     * @param view View to expand
     * @return Animator object
     */
    private static Animator createExpandLayoutAnimator(final View view) {
        measureView(view);

        Animator animator = createHeightAnimator(view, 0, view.getMeasuredHeight());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setHeightWrapContent(view);
            }
        });

        return animator;
    }

    /**
     * Create an Animator that collapses a ViewGroup. When the Animator completes, the visibility
     * of the ViewGroup will be set to View.GONE and the layout height mode will be set to
     * wrap_content.
     *
     * @param view View to collapse
     * @return Animator object
     */
    private static Animator createCollapseLayoutAnimator(final View view) {
        ValueAnimator animator = createHeightAnimator(view, view.getHeight(), 0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);

                setHeightWrapContent(view);
            }
        });
        return animator;
    }

    /**
     * Expand the unsupported file options without using an animation. This should only be used
     * when setting the initial UI state (eg. hiding a view after an orientation change).
     */
    private void expandUnsupportedOpts() {
        vUnsupportedContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Collapse the unsupported file options without using an animation. This should only be used
     * when setting the initial UI state (eg. hiding a view after an orientation change).
     */
    private void collapseUnsupportedOpts() {
        vUnsupportedContainer.setVisibility(View.GONE);
    }

    /**
     * Animate the expanding of the unsupported file options. This should only be called from
     * onChoseFile() when an unsupported file is chosen.
     *
     * No animation will occur if the layout is already expanded.
     */
    private void animateExpandUnsupportedOpts() {
        if (vUnsupportedContainer.getVisibility() == View.VISIBLE) {
            return;
        }

        Animator animator = createExpandLayoutAnimator(vUnsupportedContainer);
        animator.start();
    }

    /**
     * Animate the collapsing of the unsupported file options. This should only be called from
     * onChoseFile() when a supported file is chosen.
     *
     * No animation will occur if the layout is already collapsed.
     */
    private void animateCollapseUnsupportedOpts() {
        if (vUnsupportedContainer.getVisibility() == View.GONE) {
            return;
        }

        Animator animator = createCollapseLayoutAnimator(vUnsupportedContainer);
        animator.start();
    }

    /**
     * Expand the custom options without using an animation. This should only be used when
     * setting the initial UI state (eg. hiding a view after an orientation change).
     */
    private void expandCustomOpts() {
        vCustomOptsContainer.setVisibility(View.VISIBLE);
        vPresetName.setVisibility(View.GONE);
    }

    /**
     * Collapse the custom options without using an animation. This should only be used when
     * setting the initial UI state (eg. hiding a view after an orientation change).
     */
    private void collapseCustomOpts() {
        vCustomOptsContainer.setVisibility(View.GONE);
        vPresetName.setVisibility(View.VISIBLE);
    }

    /**
     * Animate the expanding of the custom options. This should only be called if the user chooses
     * to use custom options (as opposed to a preset).
     *
     * No animation will occur if the layout is already expanded.
     */
    private void animateExpandCustomOpts() {
        if (vCustomOptsContainer.getVisibility() == View.VISIBLE) {
            return;
        }

        Animator animator = createCollapseLayoutAnimator(vPresetName);
        Animator animator2 = createExpandLayoutAnimator(vCustomOptsContainer);
        AnimatorSet set = new AnimatorSet();
        set.play(animator2).after(animator);
        set.start();
    }

    /**
     * Animate the collapsing of the custom options. This should only be called if the user chooses
     * a valid preset.
     *
     * No animation will occur if the layout is already collapsed.
     */
    private void animateCollapseCustomOpts() {
        if (vCustomOptsContainer.getVisibility() == View.GONE) {
            return;
        }

        Animator animator = createCollapseLayoutAnimator(vCustomOptsContainer);
        Animator animator2 = createExpandLayoutAnimator(vPresetName);
        AnimatorSet set = new AnimatorSet();
        set.play(animator2).after(animator);
        set.start();
    }

    /**
     * Animate the expanding of the boot image options. This should only be called if the user
     * clicks on the "has boot image" checkbox.
     *
     * No animation will occur if the layout is already expanded.
     */
    private void animateExpandBootImageOpts() {
        if (vBootImageContainer.getVisibility() == View.VISIBLE) {
            return;
        }

        Animator animator = createExpandLayoutAnimator(vBootImageContainer);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vHasBootImageBox.setClickable(true);
            }
        });
        animator.start();
    }

    /**
     * Animate the collapsing of the boot image options. This should only be called if the user
     * clicks on the "has boot image" checkbox.
     *
     * No animation will occur if the layout is already collapsed.
     */
    private void animateCollapseBootImageOpts() {
        if (vBootImageContainer.getVisibility() == View.GONE) {
            return;
        }

        Animator animator = createCollapseLayoutAnimator(vBootImageContainer);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vHasBootImageBox.setClickable(true);
            }
        });
        animator.start();
    }

    /**
     * Measure the dimensions of a view before it is drawn (eg. if visibility is View.GONE).
     *
     * @param view View
     */
    private static void measureView(View view) {
        View parent = (View) view.getParent();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth() -
                        parent.getPaddingLeft() - parent.getPaddingRight(),
                View.MeasureSpec.AT_MOST);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
    }

    /**
     * Set a layout's height param to wrap_content.
     *
     * @param view ViewGroup/layout
     */
    private static void setHeightWrapContent(View view) {
        LayoutParams params = view.getLayoutParams();
        params.height = LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(params);
    }

    /**
     * Get the PatchInfo object corresponding to the selected preset
     *
     * @return PatchInfo object if preset is used. Otherwise, null
     */
    private PatchInfo getPreset() {
        int pos = vPresetSpinner.getSelectedItemPosition();

        if (pos == 0) {
            // Custom options
            return null;
        } else {
            return mPCS.mPatchInfos[pos - 1];
        }
    }

    /**
     * Check whether device checks should be removed
     *
     * @return False (!!) if device checks should be removed
     */
    public boolean isDeviceCheckEnabled() {
        return !vDeviceCheckBox.isChecked();
    }

    /**
     * Check whether "has boot image" is checked
     *
     * @return Whether the "has boot image" checkbox is checked
     */
    public boolean isHasBootImageEnabled() {
        return vHasBootImageBox.isChecked();
    }

    /**
     * Get the user-entered boot image string
     *
     * @return Boot image filename
     */
    public String getBootImage() {
        String bootImage = vBootImageText.getText().toString().trim();
        if (bootImage.isEmpty()) {
            return null;
        } else {
            return bootImage;
        }
    }

    /**
     * Enable or disable this card
     *
     * @param enabled Whether the card should be enabled or disabled
     */
    public void setEnabled(boolean enabled) {
        setEnabledRecursive(vCard, enabled);
    }

    /**
     * Enable or disable a view and all of its subviews.
     *
     * @param view View or layout
     * @param enabled Whether the views should be enabled or disabled
     */
    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                setEnabledRecursive(child, enabled);
            }
        }
    }

    /**
     * Reset options to sane defaults after a file has been patched
     */
    private void reset() {
        vPresetSpinner.setSelection(0);
        vDeviceCheckBox.setChecked(false);
        vHasBootImageBox.setChecked(true);
        vBootImageText.setText("");
    }
}