/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;
import static android.os.UserHandle.USER_CURRENT;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.LabeledSeekBarPreference;
import com.android.settingslib.search.SearchIndexable;

/**
 * A fragment to include all the settings related to Gesture Navigation mode.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class GestureNavigationSettingsFragment extends DashboardFragment {

    public static final String TAG = "GestureNavigationSettingsFragment";

    public static final String GESTURE_NAVIGATION_SETTINGS =
            "com.android.settings.GESTURE_NAVIGATION_SETTINGS";

    private static final String LEFT_EDGE_SEEKBAR_KEY = "gesture_left_back_sensitivity";
    private static final String RIGHT_EDGE_SEEKBAR_KEY = "gesture_right_back_sensitivity";
    private static final String GESTURE_BAR_LENGTH_KEY = "gesture_navbar_length";
    private static final String GESTURE_BAR_RADIUS_KEY = "gesture_navbar_radius";
    private static final String KEY_BACK_HEIGHT = "gesture_back_height";

    private static final String LONG_OVERLAY_PKG = "com.custom.overlay.systemui.gestural.long";
    private static final String MEDIUM_OVERLAY_PKG = "com.custom.overlay.systemui.gestural.medium";
    private static final String HIDDEN_OVERLAY_PKG = "com.custom.overlay.systemui.gestural.hidden";

    private static final String LOW_RADIUS_PKG = "com.custom.overlay.systemui.gestural.radius.low";
    private static final String VERY_LOW_RADIUS_PKG = "com.custom.overlay.systemui.gestural.radius.verylow";
    private static final String HIDDEN_RADIUS_PKG = "com.custom.overlay.systemui.gestural.radius.hidden";

    private IOverlayManager mOverlayService;

    private WindowManager mWindowManager;
    private BackGestureIndicatorView mIndicatorView;

    private float[] mBackGestureInsetScales;
    private float mDefaultBackGestureInset;
    private float[] mBackGestureHeightScales = { 0f, 1f, 2f, 3f };
    private int mCurrentRightWidth;
    private int mCurrentLefttWidth;

    public GestureNavigationSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIndicatorView = new BackGestureIndicatorView(getActivity());
        mWindowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final Resources res = getActivity().getResources();
        mDefaultBackGestureInset = res.getDimensionPixelSize(
                com.android.internal.R.dimen.config_backGestureInset);
        mBackGestureInsetScales = getFloatArray(res.obtainTypedArray(
                com.android.internal.R.array.config_backGestureInsetScales));

        initSeekBarPreference(LEFT_EDGE_SEEKBAR_KEY);
        initSeekBarPreference(RIGHT_EDGE_SEEKBAR_KEY);
        initSeekBarPreference(GESTURE_BAR_RADIUS_KEY);
        initSeekBarPreference(GESTURE_BAR_LENGTH_KEY);
        initSeekBarPreference(KEY_BACK_HEIGHT);
    }

    @Override
    public void onResume() {
        super.onResume();

        mWindowManager.addView(mIndicatorView, mIndicatorView.getLayoutParams(
                getActivity().getWindow().getAttributes()));
    }

    @Override
    public void onPause() {
        super.onPause();

        mWindowManager.removeView(mIndicatorView);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.gesture_navigation_settings;
    }

    @Override
    public int getHelpResource() {
        // TODO(b/146001201): Replace with gesture navigation help page when ready.
        return R.string.help_uri_default;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_GESTURE_NAV_BACK_SENSITIVITY_DLG;
    }

    private void initSeekBarPreference(final String key) {
        final LabeledSeekBarPreference pref = getPreferenceScreen().findPreference(key);
        pref.setContinuousUpdates(true);

        String settingsKey;

        switch(key) {
            case LEFT_EDGE_SEEKBAR_KEY:
                settingsKey = Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT;
                break;
            case RIGHT_EDGE_SEEKBAR_KEY:
                settingsKey = Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT;
                break;
            case GESTURE_BAR_LENGTH_KEY:
                settingsKey = Settings.Secure.GESTURE_NAVBAR_LENGTH;
                break;
            case GESTURE_BAR_RADIUS_KEY:
                settingsKey = Settings.Secure.GESTURE_NAVBAR_RADIUS;
                break;
            case KEY_BACK_HEIGHT:
                settingsKey = Settings.System.BACK_GESTURE_HEIGHT;
                break;
            default:
                settingsKey = "";
                break;
        }
        float initScale = 0;
        if (settingsKey != "") {
            initScale = Settings.Secure.getFloat(
                  getContext().getContentResolver(), settingsKey, settingsKey == GESTURE_BAR_LENGTH_KEY ? 0.0f : 1.0f);
        }

        // needed if we just change the height
        float currentWidthScale = Settings.Secure.getFloat(
                getContext().getContentResolver(), Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT, 1.0f);
        mCurrentRightWidth = (int) (mDefaultBackGestureInset * currentWidthScale);
        currentWidthScale = Settings.Secure.getFloat(
                getContext().getContentResolver(), Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT, 1.0f);
        mCurrentLefttWidth = (int) (mDefaultBackGestureInset * currentWidthScale);

        if (key == KEY_BACK_HEIGHT) {
            mBackGestureInsetScales = mBackGestureHeightScales;
            initScale = Settings.System.getInt(
                    getContext().getContentResolver(), settingsKey, 0);
        }

        // Find the closest value to initScale
        float minDistance = Float.MAX_VALUE;
        int minDistanceIndex = key == GESTURE_BAR_LENGTH_KEY ? (int) initScale : -1;
        if (key != GESTURE_BAR_LENGTH_KEY) {
            for (int i = 0; i < mBackGestureInsetScales.length; i++) {
                float d = Math.abs(mBackGestureInsetScales[i] - initScale);
                if (d < minDistance) {
                    minDistance = d;
                    minDistanceIndex = i;
                }
            }
        }

        pref.setProgress(minDistanceIndex);

        if (key == GESTURE_BAR_RADIUS_KEY) {
            pref.setOnPreferenceChangeListener((p, v) -> {
                switch((int) v) {
                    case 0:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabled(HIDDEN_RADIUS_PKG, false, USER_CURRENT);
                            mOverlayService.setEnabled(VERY_LOW_RADIUS_PKG, false, USER_CURRENT);
                            mOverlayService.setEnabled(LOW_RADIUS_PKG, false, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 1:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabledExclusiveInCategory(HIDDEN_RADIUS_PKG, USER_CURRENT);
                            mOverlayService.setEnabled(VERY_LOW_RADIUS_PKG, false, USER_CURRENT);
                            mOverlayService.setEnabled(LOW_RADIUS_PKG, false, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 2:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabledExclusiveInCategory(VERY_LOW_RADIUS_PKG, USER_CURRENT);
                            mOverlayService.setEnabled(LOW_RADIUS_PKG, false, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 3:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabledExclusiveInCategory(LOW_RADIUS_PKG, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    }
               Settings.Secure.putFloat(getContext().getContentResolver(), settingsKey, (int) v);
               return true;
             });

        } else if (key != GESTURE_BAR_LENGTH_KEY) {
            pref.setOnPreferenceChangeListener((p, v) -> {
                if (key != KEY_BACK_HEIGHT) {
                    final int width = (int) (mDefaultBackGestureInset * mBackGestureInsetScales[(int) v]);
                    mIndicatorView.setIndicatorWidth(width, key == LEFT_EDGE_SEEKBAR_KEY);
                    if (key == LEFT_EDGE_SEEKBAR_KEY) {
                        mCurrentLefttWidth = width;
                    } else {
                        mCurrentRightWidth = width;
                    }
                } else {
                    final int heightScale = (int) (mBackGestureInsetScales[(int) v]);
                    mIndicatorView.setIndicatorHeightScale(heightScale);
                    // dont use updateViewLayout else it will animate
                    mWindowManager.removeView(mIndicatorView);
                    mWindowManager.addView(mIndicatorView, mIndicatorView.getLayoutParams(
                            getActivity().getWindow().getAttributes()));
                    // peek the indicators
                    mIndicatorView.setIndicatorWidth(mCurrentRightWidth, false);
                    mIndicatorView.setIndicatorWidth(mCurrentLefttWidth, true);
                }
                return true;
            });

            pref.setOnPreferenceChangeStopListener((p, v) -> {
                final float scale = mBackGestureInsetScales[(int) v];
                if (key == KEY_BACK_HEIGHT) {
                    mIndicatorView.setIndicatorWidth(0, false);
                    mIndicatorView.setIndicatorWidth(0, true);
                    Settings.System.putInt(getContext().getContentResolver(), settingsKey, (int) scale);
                } else {
                    mIndicatorView.setIndicatorWidth(0, key == LEFT_EDGE_SEEKBAR_KEY);
                    Settings.Secure.putFloat(getContext().getContentResolver(), settingsKey, scale);
                }
                return true;
            });
        } else {
            pref.setOnPreferenceChangeListener((p, v) -> {
                switch((int) v) {
                    case 0:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(HIDDEN_OVERLAY_PKG, USER_CURRENT);
                            mOverlayService.setEnabled(LONG_OVERLAY_PKG, false, USER_CURRENT);
                            mOverlayService.setEnabled(MEDIUM_OVERLAY_PKG, false, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 1:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabled(LONG_OVERLAY_PKG, false, USER_CURRENT);
                            mOverlayService.setEnabled(MEDIUM_OVERLAY_PKG, false, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 2:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabledExclusiveInCategory(MEDIUM_OVERLAY_PKG, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                    case 3:
                        try {
                            mOverlayService.setEnabledExclusiveInCategory(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
                            mOverlayService.setEnabledExclusiveInCategory(LONG_OVERLAY_PKG, USER_CURRENT);
                        } catch (RemoteException re) {
                            throw re.rethrowFromSystemServer();
                        }
                        break;
                }
                Settings.Secure.putFloat(getContext().getContentResolver(), settingsKey, (int) v);
                return true;
            });
        }
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, 1.0f);
        }
        array.recycle();
        return floatArray;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.gesture_navigation_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SystemNavigationPreferenceController.isGestureAvailable(context);
                }
            };

}
