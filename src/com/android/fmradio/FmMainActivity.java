/*
 * Copyright (C) 2014,2022 The Android Open Source Project
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

package com.android.fmradio;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.ComponentActivity;
import androidx.core.view.WindowCompat;

import com.android.fmradio.FmStation.Station;
import com.android.fmradio.dialogs.FmFavoriteEditDialog;
import com.android.fmradio.ui.FmMainUiBridge;
import com.android.fmradio.ui.FmUiStateHolder;
import com.android.fmradio.views.FmScroller;
import com.android.fmradio.views.FmSnackBar;
import com.android.fmradio.views.FmScroller.EventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This class interact with user, provide FM basic function.
 */
public class FmMainActivity extends ComponentActivity implements FmFavoriteEditDialog.EditFavoriteListener {
    // Logging
    private static final String TAG = "FmMainActivity";

    // Request code
    private static final int REQUEST_CODE_FAVORITE = 1;

    public static final int REQUEST_CODE_RECORDING = 2;

    private static final int PERMISSION_REQUEST_POWER_ON = 100;

    // Extra for result of request REQUEST_CODE_RECORDING
    public static final String EXTRA_RESULT_STRING = "result_string";

    // FM
    private static final String FM = "FM";

    // UI views
    private TextView mTextStationName = null;

    private TextView mTextStationValue = null;

    // RDS text view
    private TextView mTextRds = null;

    private TextView mActionBarTitle = null;

    private TextView mNoEarPhoneTxt = null;

    private ImageButton mButtonDecrease = null;

    private ImageButton mButtonPrevStation = null;

    private ImageButton mButtonNextStation = null;

    private ImageButton mButtonIncrease = null;

    private ImageButton mButtonAddToFavorite = null;

    private ImageButton mButtonPlay = null;

    private ImageView mNoHeadsetImgView = null;

    private View mNoHeadsetImgViewWrap = null;

    private LinearLayout mMainLayout = null;

    private RelativeLayout mNoHeadsetLayout = null;

    private LinearLayout mNoEarphoneTextLayout = null;

    private LinearLayout mBtnPlayInnerContainer = null;

    private LinearLayout mBtnPlayContainer = null;

    // Menu items
    private MenuItem mMenuItemStationlList = null;

    private MenuItem mMenuItemHeadset = null;

    private MenuItem mMenuItemStartRecord = null;

    private MenuItem mMenuItemRecordList = null;

    // State variables
    private boolean mIsServiceStarted = false;

    private boolean mIsServiceBinded = false;

    private boolean mIsTune = false;

    private boolean mIsDisablePowerMenu = false;

    private boolean mIsActivityForeground = true;

    private int mCurrentStation = FmUtils.DEFAULT_STATION;

    private final FmUiStateHolder mUiState = new FmUiStateHolder();

    // Instance variables
    private FmService mService = null;

    private Context mContext = null;

    private Toast mToast = null;

    private FragmentManager mFragmentManager = null;

    private AudioManager mAudioManager = null;

    private FmScroller mScroller;

    private FmScroller.EventListener mEventListener;

    // Service listener
    private FmListener mFmRadioListener = new FmListener() {
        @Override
        public void onCallBack(Bundle bundle) {
            int flag = bundle.getInt(FmListener.CALLBACK_FLAG);
            if (flag == FmListener.MSGID_FM_EXIT) {
                mHandler.removeCallbacksAndMessages(null);
            }

            // remove tag message first, avoid too many same messages in queue.
            Message msg = mHandler.obtainMessage(flag);
            msg.setData(bundle);
            mHandler.removeMessages(flag);
            mHandler.sendMessage(msg);
        }
    };

    // Button click listeners on UI
    private final View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int viewId = v.getId();
            if (viewId == R.id.button_add_to_favorite) {
                updateFavoriteStation();
            } else if (viewId == R.id.button_decrease) {
                tuneStation(FmUtils.computeDecreaseStation(mCurrentStation));
            } else if (viewId == R.id.button_increase) {
                tuneStation(FmUtils.computeIncreaseStation(mCurrentStation));
            } else if (viewId == R.id.button_prevstation) {
                seekStation(mCurrentStation, false); // false: previous station
            } else if (viewId == R.id.button_nextstation) {
                seekStation(mCurrentStation, true); // true: previous station
            } else if (viewId == R.id.play_button) {
                if (mService.getPowerStatus() == FmService.POWER_UP) {
                    powerDownFm();
                } else {
                    powerUpFm();
                }
            } else {
                Log.d(TAG, "mButtonClickListener.onClick, invalid view id");
            }
        }
    };

    /**
     * Main thread handler to update UI
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,
                    "mHandler.handleMessage, what = " + msg.what + ",hashcode:"
                            + mHandler.hashCode());
            Bundle bundle;
            switch (msg.what) {

                case FmListener.MSGID_POWERUP_FINISHED:
                    bundle = msg.getData();

                    boolean isPowerup = (mService.getPowerStatus() == FmService.POWER_UP);
                    int station = bundle.getInt(FmListener.KEY_TUNE_TO_STATION);
                    mCurrentStation = station;
                    refreshStationUI(station);
                    if (isPowerup) {
                        refreshImageButton(true);
                        refreshPopupMenuItem(true);
                        refreshActionMenuItem(true);
                    } else {
                        showToast(getString(R.string.not_available));
                    }
                    // if not powerup success, refresh power to enable.
                    refreshPlayButton(true);
                    break;

                case FmListener.MSGID_SWITCH_ANTENNA:
                    bundle = msg.getData();
                    boolean hasAntenna = bundle.getBoolean(FmListener.KEY_IS_SWITCH_ANTENNA);
                    setMenuItemAudioIcon(!hasAntenna);
                    refreshMenuItemAudio(hasAntenna);
                    break;

                case FmListener.MSGID_POWERDOWN_FINISHED:
                    if (mUiState != null) {
                        mUiState.setSeeking(false);
                    }
                    bundle = msg.getData();
                    refreshImageButton(false);
                    refreshActionMenuItem(false);
                    refreshPopupMenuItem(false);
                    refreshPlayButton(true);
                    break;

                case FmListener.MSGID_TUNE_FINISHED:
                    if (mUiState != null) {
                        mUiState.setSeeking(false);
                    }
                    bundle = msg.getData();
                    boolean isTune = bundle.getBoolean(FmListener.KEY_IS_TUNE);
                    boolean isPowerUp = (mService.getPowerStatus() == FmService.POWER_UP);

                    // tune finished, should make power enable
                    mIsDisablePowerMenu = false;
                    float frequency = bundle.getFloat(FmListener.KEY_TUNE_TO_STATION);
                    mCurrentStation = FmUtils.computeStation(frequency);
                    // After tune to station finished, refresh favorite button and
                    // other button status.
                    refreshStationUI(mCurrentStation);
                    // tune fail,should resume button status
                    if (!isTune) {
                        Log.d(TAG, "mHandler.tune: " + isTune);
                        refreshActionMenuItem(isPowerUp);
                        refreshImageButton(isPowerUp);
                        refreshPopupMenuItem(isPowerUp);
                        refreshPlayButton(true);
                        return;
                    }
                    refreshImageButton(true);
                    refreshActionMenuItem(true);
                    refreshPopupMenuItem(true);
                    refreshPlayButton(true);
                    break;

                case FmListener.MSGID_FM_EXIT:
                    finish();
                    break;

                case FmListener.LISTEN_RDSSTATION_CHANGED:
                    bundle = msg.getData();
                    int rdsStation = bundle.getInt(FmListener.KEY_RDS_STATION);
                    refreshStationUI(rdsStation);
                    break;

                case FmListener.LISTEN_PS_CHANGED:
                    String stationName = FmStation.getStationName(mContext, mCurrentStation);
                    mUiState.setStationName(stationName);
                    if (mTextStationName != null) {
                        mTextStationName.setText(stationName);
                    }
                    if (mScroller != null) {
                        mScroller.notifyAdatperCurrentItemRDSChanged();
                    }
                    break;

                case FmListener.LISTEN_RT_CHANGED:
                    bundle = msg.getData();
                    String rtString = bundle.getString(FmListener.KEY_RT_INFO);
                    mUiState.setRadioText(rtString);
                    if (mTextRds != null) {
                        mTextRds.setText(rtString);
                    }
                    if (mScroller != null) {
                        mScroller.notifyAdatperCurrentItemRDSChanged();
                    }
                    break;

                case FmListener.LISTEN_SPEAKER_MODE_CHANGED:
                    bundle = msg.getData();
                    boolean isSpeakerMode = bundle.getBoolean(FmListener.KEY_IS_SPEAKER_MODE);
                    setMenuItemAudioIcon(isSpeakerMode);
                    refreshMenuItemAudio(!isSpeakerMode);
                    break;

                case FmListener.LISTEN_RECORDSTATE_CHANGED:
                    if (mService != null) {
                        mService.updatePlayingNotification();
                        int recState = mService.getRecorderState();
                        mUiState.setRecording(recState == FmRecorder.STATE_RECORDING);
                        mUiState.setRecordingState(recState);
                        if (recState == FmRecorder.STATE_RECORDING) {
                            startRecordingProgressUpdates();
                        } else {
                            stopRecordingProgressUpdates();
                            String defaultName = mService.getRecordingName();
                            if (defaultName != null) {
                                mUiState.setRecordingName(defaultName);
                            }
                            mUiState.reloadRecordings(mContext);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private final Runnable mRecordingProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mService != null && mUiState != null && mService.getRecorderState() == FmRecorder.STATE_RECORDING) {
                mUiState.setRecordDuration(mService.getRecordTime());
                mUiState.setRecordSize(mService.getFileSize());
                mHandler.postDelayed(this, 500);
            }
        }
    };

    private void startRecordingProgressUpdates() {
        mHandler.removeCallbacks(mRecordingProgressRunnable);
        mHandler.post(mRecordingProgressRunnable);
    }

    private void stopRecordingProgressUpdates() {
        mHandler.removeCallbacks(mRecordingProgressRunnable);
        if (mService != null && mUiState != null) {
            mUiState.setRecordDuration(mService.getRecordTime());
            mUiState.setRecordSize(mService.getFileSize());
        }
    }

    // When call bind service, it will call service connect. register call back
    // listener and initial device
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * called by system when bind service
         *
         * @param className component name
         * @param service service binder
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((FmService.ServiceBinder) service).getService();
            if (null == mService) {
                Log.e(TAG, "onServiceConnected, mService is null");
                finish();
                return;
            }

            mService.registerFmRadioListener(mFmRadioListener);
            mService.setFmMainActivityForeground(mIsActivityForeground);
            if (FmRecorder.STATE_RECORDING != mService.getRecorderState()) {
                mService.removeNotification();
            }
            mUiState.setFrequency(FmUtils.computeFrequency(mCurrentStation));
            mUiState.setPlaying(mService.getPowerStatus() == FmService.POWER_UP);
            mUiState.setSpeakerUsed(mService.isSpeakerUsed());
            int recState = mService.getRecorderState();
            mUiState.setRecording(recState == FmRecorder.STATE_RECORDING);
            mUiState.setRecordingState(recState);
            if (recState == FmRecorder.STATE_RECORDING) {
                startRecordingProgressUpdates();
            }
            mUiState.reloadRecordings(mContext);
            mUiState.reloadFavorites(mContext);
            if (!mService.isServiceInited()) {
                mService.initService(mCurrentStation);
                if (FmPreferences.isAutoplayEnabled(mContext)) {
                    powerUpFm();
                } else {
                    mUiState.setPlaying(false);
                }
            } else {
                if (mService.isDeviceOpen()) {
                    // tune to station during changing language,we need to tune
                    // again when service bind success
                    if (mIsTune) {
                        tuneStation(mCurrentStation);
                        mIsTune = false;
                    }
                    updateCurrentStation();
                    updateMenuStatus();
                } else {
                    // Normal case will not come here
                    // Need to exit FM for this case
                    exitService();
                    finish();
                }
            }
        }

        /**
         * When unbind service will call this method
         *
         * @param className The component name
         */
        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private class NoHeadsetAlpaOutListener implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
            if (!isAntennaAvailable()) {
                return;
            }
            changeToMainLayout();
            cancelMainAnimation();
            if (mMainLayout != null) {
                Animation anim = AnimationUtils.loadAnimation(mContext,
                        R.anim.main_alpha_in);
                mMainLayout.startAnimation(anim);
            }
            if (mBtnPlayContainer != null) {
                Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.floatbtn_alpha_in);
                mBtnPlayContainer.startAnimation(anim);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (mNoHeadsetImgViewWrap != null) {
                mNoHeadsetImgViewWrap.setElevation(0);
            }
        }
    }

    private class NoHeadsetAlpaInListener implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
            if (isAntennaAvailable()) {
                return;
            }
            changeToNoHeadsetLayout();
            cancelNoHeadsetAnimation();
            if (mNoHeadsetLayout != null) {
                Animation anim = AnimationUtils.loadAnimation(mContext,
                        R.anim.noeaphone_alpha_in);
                mNoHeadsetLayout.startAnimation(anim);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    }

    /**
     * Update the favorite UI state
     */
    private void updateFavoriteStation() {
        if (FmStation.isFavoriteStation(mContext, mCurrentStation)) {
            FmStation.removeFromFavorite(mContext, mCurrentStation);
            mUiState.setFavorite(false);
            if (mButtonAddToFavorite != null) {
                mButtonAddToFavorite.setImageResource(R.drawable.btn_fm_favorite_off_selector);
            }
            if (mScroller != null) {
                mScroller.onRemoveFavorite();
            }
            String stationName = FmStation.getStationName(mContext, mCurrentStation);
            mUiState.setStationName(stationName);
            if (mTextStationName != null) {
                mTextStationName.setText(stationName);
            }
        } else {
            if (FmStation.isStationExist(mContext, mCurrentStation)) {
                FmStation.addToFavorite(mContext, mCurrentStation);
            } else {
                ContentValues values = new ContentValues(2);
                values.put(Station.FREQUENCY, mCurrentStation);
                values.put(Station.IS_FAVORITE, true);
                FmStation.insertStationToDb(mContext, values);
            }
            mUiState.setFavorite(true);
            if (mButtonAddToFavorite != null) {
                mButtonAddToFavorite.setImageResource(R.drawable.btn_fm_favorite_on_selector);
            }
            if (mScroller != null) {
                mScroller.onAddFavorite();
            }
        }
        mUiState.reloadFavorites(mContext);
    }

    /**
     * Called when the activity is first created, initial variables
     *
     * @param savedInstanceState The saved bundle in onSaveInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mFragmentManager = getFragmentManager();
        mContext = getApplicationContext();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mCurrentStation = FmStation.getCurrentStation(mContext);

        // Attach Material 3 Expressive Compose UI
        View composeView = FmMainUiBridge.createPlayerComposeView(this, mUiState);
        setContentView(composeView);
        setupUiCallbacks();
        refreshStationUI(mCurrentStation);
    }

    private void setupUiCallbacks() {
        mUiState.setOnPlayPauseListener(() -> {
            if (mService != null && mService.getPowerStatus() == FmService.POWER_UP) {
                powerDownFm();
            } else {
                powerUpFm();
            }
        });
        mUiState.setOnPrevStationListener(() -> {
            seekStation(mCurrentStation, false);
        });
        mUiState.setOnNextStationListener(() -> {
            seekStation(mCurrentStation, true);
        });
        mUiState.setOnTuneDownListener(() -> {
            tuneStation(FmUtils.computeDecreaseStation(mCurrentStation));
        });
        mUiState.setOnTuneUpListener(() -> {
            tuneStation(FmUtils.computeIncreaseStation(mCurrentStation));
        });
        mUiState.setOnFrequencySeekListener(freq -> {
            int station = FmUtils.computeStation(freq);
            tuneStation(station);
        });
        mUiState.setOnToggleFavoriteListener(() -> {
            updateFavoriteStation();
        });
        mUiState.setOnToggleSpeakerListener(() -> {
            if (mService != null) {
                boolean currentSpeaker = mService.isSpeakerUsed();
                setSpeakerPhoneOn(!currentSpeaker);
                setMenuItemAudioIcon(!currentSpeaker);
            }
        });
        mUiState.setOnRecordListener(() -> {
            mUiState.setRecordingSheetVisible(true);
            mUiState.reloadRecordings(mContext);
        });
        mUiState.setOnStartRecordingListener(() -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_POWER_ON);
                return;
            }
            if (mService != null) {
                mService.startRecordingAsync();
            }
        });
        mUiState.setOnStopRecordingListener(() -> {
            if (mService != null) {
                mService.stopRecordingAsync();
            }
        });
        mUiState.setOnSaveRecordingListener(name -> {
            if (mService != null) {
                mService.saveRecordingAsync(name);
                Toast.makeText(mContext, R.string.toast_record_saved, Toast.LENGTH_SHORT).show();
                mUiState.reloadRecordings(mContext);
            }
        });
        mUiState.setOnDiscardRecordingListener(() -> {
            if (mService != null) {
                mService.saveRecordingAsync(null);
                Toast.makeText(mContext, R.string.toast_record_not_saved, Toast.LENGTH_SHORT).show();
            }
        });
        mUiState.setOnDeleteRecordingListener(item -> {
            FmMainUiBridge.deleteRecording(mContext, item);
            mUiState.reloadRecordings(mContext);
        });
        mUiState.setOnStationSelectListener(selectedFreq -> {
            if (mService != null && mService.getPowerStatus() == FmService.POWER_UP) {
                tuneStation(selectedFreq);
            } else {
                mCurrentStation = selectedFreq;
                powerUpFm();
            }
        });
    }

    @Override
    public void editFavorite(int stationFreq, String name) {
        FmStation.updateStationToDb(mContext, stationFreq, name);
        if (mCurrentStation == stationFreq) {
            String stationName = FmStation.getStationName(mContext, mCurrentStation);
            mUiState.setStationName(stationName);
            if (mTextStationName != null) {
                mTextStationName.setText(stationName);
            }
        }
        mUiState.reloadFavorites(mContext);
        if (mScroller != null) {
            mScroller.notifyAdapterChange();
        }
        String title = getString(R.string.toast_station_renamed);
        FmSnackBar.make(FmMainActivity.this, title, null, null,
                FmSnackBar.DEFAULT_DURATION).show();
    }

    /**
     * Display the rename dialog
     *
     * @param frequency The display frequency
     */
    public void showRenameDialog(int frequency) {
        if (mService != null) {
            String name = FmStation.getStationName(mContext, frequency);
            FmFavoriteEditDialog newFragment = FmFavoriteEditDialog.newInstance(name, frequency);
            newFragment.show(mFragmentManager, "TAG_EDIT_FAVORITE");
            mFragmentManager.executePendingTransactions();
        }
    }

    /**
     * Go to station list activity
     */
    private void enterStationList() {
        if (mService != null) {
            // AMS change the design for background start
            // activity. need check app is background in app code
            if (mService.isActivityForeground()) {
                Intent intent = new Intent();
                intent.setClass(FmMainActivity.this, FmFavoriteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_FAVORITE);
            }
        }
    }

    /**
     * Refresh the favorite button with the given station, if the station
     * is favorite station, show favorite icon, else show non-favorite icon.
     *
     * @param station The station frequency
     */
    private void refreshStationUI(int station) {
        if (FmUtils.isFirstTimePlayFm(mContext)) {
            Log.d(TAG, "refreshStationUI, set station value null when it is first time ");
            return;
        }
        mCurrentStation = station;
        mUiState.setFrequency(FmUtils.computeFrequency(station));

        boolean isFav = FmStation.isFavoriteStation(mContext, station);
        mUiState.setFavorite(isFav);
        if (mButtonAddToFavorite != null) {
            mButtonAddToFavorite.setImageResource(isFav ?
                    R.drawable.btn_fm_favorite_on_selector :
                    R.drawable.btn_fm_favorite_off_selector);
        }
        if (mTextStationValue != null) {
            mTextStationValue.setText(FmUtils.formatStation(station));
        }

        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            String stationName = "";
            String radioText = "";
            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(
                        Station.CONTENT_URI,
                        FmStation.COLUMNS,
                        Station.FREQUENCY + "=?",
                        new String[] { String.valueOf(station) },
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    stationName = cursor.getString(cursor.getColumnIndex(Station.STATION_NAME));
                    if (TextUtils.isEmpty(stationName)) {
                        stationName = cursor.getString(cursor.getColumnIndex(Station.PROGRAM_SERVICE));
                    }
                    radioText = cursor.getString(cursor.getColumnIndex(Station.RADIO_TEXT));
                }
            } catch (Exception e) {
                Log.e(TAG, "refreshStationUI query error: " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            final String finalStationName = stationName;
            final String finalRadioText = radioText;
            runOnUiThread(() -> {
                if (mCurrentStation == station) {
                    mUiState.setStationName(finalStationName);
                    mUiState.setRadioText(finalRadioText);
                    mUiState.reloadFavorites(mContext);

                    if (mTextStationName != null) {
                        mTextStationName.setText(finalStationName);
                    }
                    if (mTextRds != null) {
                        mTextRds.setText(finalRadioText);
                    }
                }
            });
        });
    }

    /**
     * Start and bind service, reduction variable values if configuration changed
     */
    @Override
    public void onStart() {
        super.onStart();
        // check layout onstart
        if (isAntennaAvailable()) {
            changeToMainLayout();
        } else {
            changeToNoHeadsetLayout();
        }

        // Should start FM service first.
        if (null == startService((new Intent()).setClass(this, FmService.class))) {
            Log.e(TAG, "onStart, cannot start FM service");
            return;
        }

        mIsServiceStarted = true;
        mIsServiceBinded = bindService((new Intent()).setClass(this, FmService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);

        if (!mIsServiceBinded) {
            Log.e(TAG, "onStart, cannot bind FM service");
            finish();
            return;
        }
    }

    /**
     * Refresh UI, when stop search, dismiss search dialog,
     * pop up recording dialog if FM stopped when recording in
     * background
     */
    @Override
    public void onResume() {
        super.onResume();
        mIsActivityForeground = true;
        if (mScroller != null) {
            mScroller.onResume();
        }
        mUiState.reloadFavorites(mContext);
        if (null == mService) {
            Log.d(TAG, "onResume, mService is null");
            return;
        }
        mService.setFmMainActivityForeground(mIsActivityForeground);
        if (FmRecorder.STATE_RECORDING != mService.getRecorderState()) {
            mService.removeNotification();
        }
        updateMenuStatus();
    }

    /**
     * When activity is paused call this method, indicate activity
     * enter background if press exit, power down FM
     */
    @Override
    public void onPause() {
        mIsActivityForeground = false;
        if (null != mService) {
            mService.setFmMainActivityForeground(mIsActivityForeground);
        }
        if (mScroller != null) {
            mScroller.onPause();
        }
        super.onPause();
    }

    /**
     * Called when activity enter stopped state,
     * unbind service, if exit pressed, stop service
     */
    @Override
    public void onStop() {
        if (null != mService) {
            mService.setNotificationClsName(FmMainActivity.class.getName());
            mService.updatePlayingNotification();
        }
        if (mIsServiceBinded) {
            unbindService(mServiceConnection);
            mIsServiceBinded = false;
        }
        super.onStop();
    }

    /**
     * W activity destroy, unregister broadcast receiver and remove handler message
     */
    @Override
    public void onDestroy() {
        // Remove all the handle message
        mHandler.removeCallbacksAndMessages(null);
        if (mService != null) {
            mService.unregisterFmRadioListener(mFmRadioListener);
        }
        mFmRadioListener = null;
        if (mScroller != null) {
            mScroller.closeAdapterCursor();
            mScroller.unregisterListener(mEventListener);
        }
        super.onDestroy();
    }

    /**
     * Create options menu
     *
     * @param menu The option menu
     * @return true or false indicate need to handle other menu item
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fm_action_bar, menu);
        mMenuItemStationlList = menu.findItem(R.id.fm_station_list);
        mMenuItemHeadset = menu.findItem(R.id.fm_headset);
        mMenuItemStartRecord = menu.findItem(R.id.fm_start_record);
        mMenuItemRecordList = menu.findItem(R.id.fm_record_list);
        return true;
    }

    /**
     * Prepare options menu
     *
     * @param menu The option menu
     * @return true or false indicate need to handle other menu item
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (null == mService) {
            Log.d(TAG, "onPrepareOptionsMenu, mService is null");
            return true;
        }
        int powerStatus = mService.getPowerStatus();
        boolean isPowerUp = (powerStatus == FmService.POWER_UP);
        boolean isPowerdown = (powerStatus == FmService.POWER_DOWN);
        boolean isSeeking = mService.isSeeking();
        boolean isSpeakerUsed = mService.isSpeakerUsed();
        // if fm power down by other app, should enable power menu, make it to
        // powerup.
        refreshActionMenuItem(isSeeking ? false : isPowerUp);
        refreshPopupMenuItem(isSeeking ? false : isPowerUp);
        refreshImageButton(isSeeking ? false : isPowerUp);
        refreshPlayButton(isSeeking ? false
                : (isPowerUp || (isPowerdown && !mIsDisablePowerMenu)));
        setMenuItemAudioIcon(isSpeakerUsed);
        return true;
    }

    /**
     * Handle event when option item selected
     *
     * @param item The clicked item
     * @return true or false indicate need to handle other menu item or not
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.fm_station_list) {
            refreshImageButton(false);
            refreshActionMenuItem(false);
            refreshPopupMenuItem(false);
            refreshPlayButton(false);
            // Show favorite activity.
            enterStationList();
        } else if (itemId == R.id.earphone_menu) {
            setSpeakerPhoneOn(false);
            mMenuItemHeadset.setIcon(R.drawable.btn_fm_headset_selector);
            invalidateOptionsMenu();
        } else if (itemId == R.id.speaker_menu) {
            setSpeakerPhoneOn(true);
            mMenuItemHeadset.setIcon(R.drawable.btn_fm_speaker_selector);
            invalidateOptionsMenu();
        } else if (itemId == R.id.fm_start_record || itemId == R.id.fm_record_list) {
            if (mUiState != null) {
                mUiState.setRecordingSheetVisible(true);
                mUiState.reloadRecordings(mContext);
            }
        } else {
            Log.e(TAG, "onOptionsItemSelected, invalid options menu item.");
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check whether antenna is available
     *
     * @return true or false indicate antenna available or not
     */
    private boolean isAntennaAvailable() {
        return true; // force wireless
    }

    /**
     * When on activity result, tune to station which is from station list
     *
     * @param requestCode The request code
     * @param resultCode The result code
     * @param data The intent from station list
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode) {
            if (REQUEST_CODE_RECORDING == requestCode) {
                final Uri playUri = data.getData();
                boolean isSaved = playUri != null;
                String title = data.getStringExtra(EXTRA_RESULT_STRING);
                String action = null;
                FmSnackBar.OnActionTriggerListener listener = null;

                if (isSaved) {
                    action = FmMainActivity.this.getString(R.string.toast_listen);
                    listener = new FmSnackBar.OnActionTriggerListener() {
                        @Override
                        public void onActionTriggered() {
                            Intent playMusicIntent = new Intent(Intent.ACTION_VIEW);
                            try {
                                playMusicIntent.setComponent(new ComponentName(
                                        "com.android.fmradio.recordings",
                                        "com.android.fmradio.recordings.PlayRecording"));
                                playMusicIntent.putExtra("path", playUri.toString());
                                playMusicIntent.putExtra("type", "audio/mpeg");
                                startActivity(playMusicIntent);
                            } catch (ActivityNotFoundException e2) {
                                // No activity respond
                                Log.d(TAG,"onActivityResult, no activity "
                                        + "respond play record file intent");
                            }
                        }
                    };
                }
                FmSnackBar.make(FmMainActivity.this, title, action, listener,
                        FmSnackBar.DEFAULT_DURATION).show();
            } else if (REQUEST_CODE_FAVORITE == requestCode) {
                int iStation =
                        data.getIntExtra(FmFavoriteActivity.ACTIVITY_RESULT, mCurrentStation);
                // Tune to this station.
                mCurrentStation = iStation;
                // if tune from station list, we should disable power menu,
                // especially for power down state
                mIsDisablePowerMenu = true;
                Log.d(TAG, "onActivityForReult:" + mIsDisablePowerMenu);
                if (null == mService) {
                    Log.d(TAG, "onActivityResult, mService is null");
                    mIsTune = true;
                    return;
                }
                tuneStation(iStation);
            } else {
                Log.e(TAG, "onActivityResult, invalid requestcode.");
                return;
            }
        }

        boolean isFav = FmStation.isFavoriteStation(mContext, mCurrentStation);
        if (mButtonAddToFavorite != null) {
            mButtonAddToFavorite.setImageResource(isFav ?
                    R.drawable.btn_fm_favorite_on_selector :
                    R.drawable.btn_fm_favorite_off_selector);
        }
        String name = FmStation.getStationName(mContext, mCurrentStation);
        if (mTextStationName != null) {
            mTextStationName.setText(name);
        }
        if (mUiState != null) {
            mUiState.setFavorite(isFav);
            mUiState.setStationName(name);
            mUiState.reloadFavorites(mContext);
        }
        refreshStationUI(mCurrentStation);
    }

    /**
     * Power up FM
     */
    private void powerUpFm() {
        refreshImageButton(false);
        refreshActionMenuItem(false);
        refreshPopupMenuItem(false);
        refreshPlayButton(false);
        int recordAudioPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        List<String> mPermissionStrings = new ArrayList<String>();
        boolean mRequest = false;

        if (recordAudioPermission != PackageManager.PERMISSION_GRANTED) {
            mPermissionStrings.add(Manifest.permission.RECORD_AUDIO);
            mRequest = true;
        }
        if (mRequest == true) {
            String[] mPermissionList = new String[mPermissionStrings.size()];
            mPermissionList = mPermissionStrings.toArray(mPermissionList);
            requestPermissions(mPermissionList, PERMISSION_REQUEST_POWER_ON);
            return;
        }
        mService.setRecordingPermission(true);
        mService.powerUpAsync(FmUtils.computeFrequency(mCurrentStation));
    }

    /**
     * Power down FM
     */
    private void powerDownFm() {
        if (mUiState != null) {
            mUiState.setSeeking(false);
        }
        refreshImageButton(false);
        refreshActionMenuItem(false);
        refreshPopupMenuItem(false);
        refreshPlayButton(false);
        mService.powerDownAsync();
    }

    private void setSpeakerPhoneOn(boolean isSpeaker) {
        if (isSpeaker) {
            mService.setSpeakerPhoneOn(true);
        } else {
            mService.setSpeakerPhoneOn(false);
        }
    }

    /**
     * Tune a station
     *
     * @param station The tune station
     */
    private void tuneStation(final int station) {
        refreshImageButton(false);
        refreshActionMenuItem(false);
        refreshPopupMenuItem(false);
        refreshPlayButton(false);
        mService.tuneStationAsync(FmUtils.computeFrequency(station));
    }

    /**
     * Seek station according current frequency and direction
     *
     * @param station The seek start station
     * @param direction The seek direction
     */
    private void seekStation(final int station, boolean direction) {
        // If the seek AsyncTask has been executed and not canceled, cancel it
        // before start new.
        if (mUiState != null) {
            mUiState.setSeeking(true);
        }
        refreshImageButton(false);
        refreshActionMenuItem(false);
        refreshPopupMenuItem(false);
        refreshPlayButton(false);
        mService.seekStationAsync(FmUtils.computeFrequency(station), direction);
    }

    private void refreshImageButton(boolean enabled) {
        if (mButtonDecrease != null) mButtonDecrease.setEnabled(enabled);
        if (mButtonPrevStation != null) mButtonPrevStation.setEnabled(enabled);
        if (mButtonNextStation != null) mButtonNextStation.setEnabled(enabled);
        if (mButtonIncrease != null) mButtonIncrease.setEnabled(enabled);
        if (mButtonAddToFavorite != null) mButtonAddToFavorite.setEnabled(enabled);
    }

    // Refresh action menu except power menu
    private void refreshActionMenuItem(boolean enabled) {
        // action menu
        if (null != mMenuItemStationlList) {
            // if power down by other app, should disable station list, over
            // menu
            mMenuItemStationlList.setEnabled(enabled);
        }
        refreshMenuItemAudio(enabled);
    }

    // Refresh play/stop float button
    private void refreshPlayButton(boolean enabled) {
        // action menu
        boolean isPowerUp = (mService != null && mService.getPowerStatus() == FmService.POWER_UP);
        mUiState.setPlaying(isPowerUp);
        if (mButtonPlay != null) {
            mButtonPlay.setEnabled(enabled);
            mButtonPlay.setImageResource((isPowerUp
                    ? R.drawable.btn_fm_stop_selector
                    : R.drawable.btn_fm_start_selector));
        }
        if (mScroller != null) {
            mScroller.refreshPlayIndicator(mCurrentStation, isPowerUp);
        }
    }

    private void refreshPopupMenuItem(boolean enabled) {
        if (null != mMenuItemStationlList) {
            mMenuItemStartRecord.setEnabled(enabled);
        }
    }

    /**
     * Called when back pressed
     */
    @Override
    public void onBackPressed() {
        // exit fm, disable all button
        if ((null != mService) && (mService.getPowerStatus() == FmService.POWER_DOWN)) {
            refreshImageButton(false);
            refreshActionMenuItem(false);
            refreshPopupMenuItem(false);
            refreshPlayButton(false);
            exitService();
            return;
        }
        super.onBackPressed();
    }

    private void showToast(CharSequence text) {
        if (null == mToast) {
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        }
        mToast.setText(text);
        mToast.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Exit FM service
     */
    private void exitService() {
        if (mIsServiceBinded) {
            unbindService(mServiceConnection);
            mIsServiceBinded = false;
        }

        if (mIsServiceStarted) {
            stopService(new Intent(FmMainActivity.this, FmService.class));
            mIsServiceStarted = false;
        }
    }

    /**
     * Update current station according service station
     */
    private void updateCurrentStation() {
        // get the frequency from service, set frequency in activity, UI,
        // database
        // same as the frequency in service
        int freq = mService.getFrequency();
        if (FmUtils.isValidStation(freq)) {
            if (mCurrentStation != freq) {
                mCurrentStation = freq;
                FmStation.setCurrentStation(mContext, mCurrentStation);
                refreshStationUI(mCurrentStation);
            }
        }
    }

    /**
     * Update menu status, and animation
     */
    private void updateMenuStatus() {
        int powerStatus = mService.getPowerStatus();
        boolean isPowerUp = (powerStatus == FmService.POWER_UP);
        boolean isDuringPowerup = (powerStatus == FmService.DURING_POWER_UP);
        boolean isSeeking = mService.isSeeking();
        boolean isPowerdown = (powerStatus == FmService.POWER_DOWN);
        boolean isSpeakerUsed = mService.isSpeakerUsed();
        boolean fmStatus = (isSeeking || isDuringPowerup);
        // when seeking, all button should disabled,
        // else should update as origin status
        refreshImageButton(fmStatus ? false : isPowerUp);
        refreshPopupMenuItem(fmStatus ? false : isPowerUp);
        refreshActionMenuItem(fmStatus ? false : isPowerUp);
        // if fm power down by other app, should enable power button
        // to powerup.
        Log.d(TAG, "updateMenuStatus.mIsDisablePowerMenu: " + mIsDisablePowerMenu);
        refreshPlayButton(fmStatus ? false
                : (isPowerUp || (isPowerdown && !mIsDisablePowerMenu)));
        setMenuItemAudioIcon(isSpeakerUsed);
    }

    private void setMenuItemAudioIcon(final boolean isSpeakerUsed) {
        mUiState.setSpeakerUsed(isSpeakerUsed);
        if (null != mMenuItemHeadset) {
            mMenuItemHeadset.setIcon(isSpeakerUsed ?
                    R.drawable.btn_fm_speaker_selector :
                    R.drawable.btn_fm_headset_selector);
        }
    }

    private void refreshMenuItemAudio(final boolean enabled) {
        boolean canRoute = false;
        if (mService != null) {
            canRoute = enabled && (mService.isHeadSetIn() || mService.isBluetoothHeadsetInUse());
        }
        mUiState.setAudioRoutingAvailable(canRoute);
        if (null != mMenuItemHeadset) {
            // If BT headset is in use or preferred device for media strategy is neither speaker nor
            // headset (e.g., USB audio headset), need to disable speaker/earphone switching menu.
            final int preferredDevice = mService != null ? mService.getPreferredDeviceForMediaStrategy() : AudioDeviceInfo.TYPE_UNKNOWN;
            mMenuItemHeadset.setEnabled(enabled &&
                    (mService != null && mService.isHeadSetIn()) &&
                    (!mService.isBluetoothHeadsetInUse() &&
                     (preferredDevice == AudioDeviceInfo.TYPE_UNKNOWN ||
                      (preferredDevice == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                      preferredDevice == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                      preferredDevice == AudioDeviceInfo.TYPE_WIRED_HEADSET) ||
                      // If neither speaker or headset, ensure the preferred device (e.g., USB
                      // audio headset) is disconnected
                      !mService.isAudioDeviceAvailable(preferredDevice))));
        }
    }

    private void initUiComponent() {
        mTextRds = (TextView) findViewById(R.id.station_rds);
        mTextStationValue = (TextView) findViewById(R.id.station_value);
        mButtonAddToFavorite = (ImageButton) findViewById(R.id.button_add_to_favorite);
        mTextStationName = (TextView) findViewById(R.id.station_name);
        mButtonDecrease = (ImageButton) findViewById(R.id.button_decrease);
        mButtonIncrease = (ImageButton) findViewById(R.id.button_increase);
        mButtonPrevStation = (ImageButton) findViewById(R.id.button_prevstation);
        mButtonNextStation = (ImageButton) findViewById(R.id.button_nextstation);

        mTextRds.setTextIsSelectable(true);
        mTextRds.setSelected(true);

        // put favorite button here since it might be used very early in
        // changing recording mode
        mCurrentStation = FmStation.getCurrentStation(mContext);
        refreshStationUI(mCurrentStation);

        // l new
        mMainLayout = (LinearLayout) findViewById(R.id.main_view);
        mNoHeadsetLayout = (RelativeLayout) findViewById(R.id.no_headset);
        mNoEarphoneTextLayout = (LinearLayout) findViewById(R.id.no_bottom);
        mBtnPlayContainer = (LinearLayout) findViewById(R.id.play_button_container);
        mBtnPlayInnerContainer = (LinearLayout) findViewById(R.id.play_button_inner_container);
        mButtonPlay = (ImageButton) findViewById(R.id.play_button);
        mNoEarPhoneTxt = (TextView) findViewById(R.id.no_eaphone_text);
        mNoHeadsetImgView = (ImageView) findViewById(R.id.no_headset_img);
        mNoHeadsetImgViewWrap = findViewById(R.id.no_middle);
        // main ui layout params
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle("");
    }

    private void registerButtonClickListener() {
        if (mButtonAddToFavorite != null) mButtonAddToFavorite.setOnClickListener(mButtonClickListener);
        if (mButtonDecrease != null) mButtonDecrease.setOnClickListener(mButtonClickListener);
        if (mButtonIncrease != null) mButtonIncrease.setOnClickListener(mButtonClickListener);
        if (mButtonPrevStation != null) mButtonPrevStation.setOnClickListener(mButtonClickListener);
        if (mButtonNextStation != null) mButtonNextStation.setOnClickListener(mButtonClickListener);
        if (mButtonPlay != null) mButtonPlay.setOnClickListener(mButtonClickListener);
    }

    /**
     * play main animation
     */
    private void playMainAnimation() {
        if (null == mService || mMainLayout == null) {
            return;
        }
        if (mMainLayout.isShown()) {
            return;
        }
        if (mNoEarPhoneTxt != null) {
            Animation animation = AnimationUtils.loadAnimation(mContext,
                    R.anim.noeaphone_alpha_out);
            mNoEarPhoneTxt.startAnimation(animation);
        }
        if (mNoHeadsetImgView != null) {
            Animation animation = AnimationUtils.loadAnimation(mContext,
                    R.anim.noeaphone_alpha_out);
            mNoHeadsetImgView.startAnimation(animation);
        }
        if (mNoEarphoneTextLayout != null) {
            Animation animation = AnimationUtils.loadAnimation(mContext,
                    R.anim.noeaphone_translate_out);
            animation.setAnimationListener(new NoHeadsetAlpaOutListener());
            mNoEarphoneTextLayout.startAnimation(animation);
        }
    }

    /**
     * clear main layout animation
     */
    private void cancelMainAnimation() {
        if (mNoEarPhoneTxt != null) mNoEarPhoneTxt.clearAnimation();
        if (mNoHeadsetImgView != null) mNoHeadsetImgView.clearAnimation();
        if (mNoEarphoneTextLayout != null) mNoEarphoneTextLayout.clearAnimation();
    }

    /**
     * play change to no headset layout animation
     */
    private void playNoHeadsetAnimation() {
        if (null == mService || mNoHeadsetLayout == null) {
            return;
        }
        if (mNoHeadsetLayout.isShown()) {
            return;
        }
        if (mMainLayout != null) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.main_alpha_out);
            mMainLayout.startAnimation(animation);
            animation.setAnimationListener(new NoHeadsetAlpaInListener());
        }
        if (mBtnPlayContainer != null) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.main_alpha_out);
            mBtnPlayContainer.startAnimation(animation);
        }
    }

    /**
     * clear no headset layout animation
     */
    private void cancelNoHeadsetAnimation() {
        if (mMainLayout != null) mMainLayout.clearAnimation();
        if (mBtnPlayContainer != null) mBtnPlayContainer.clearAnimation();
    }

    /**
     * change to main layout
     */
    private void changeToMainLayout() {
        if (mNoEarphoneTextLayout != null) mNoEarphoneTextLayout.setVisibility(View.GONE);
        if (mNoHeadsetImgView != null) mNoHeadsetImgView.setVisibility(View.GONE);
        if (mNoHeadsetImgViewWrap != null) mNoHeadsetImgViewWrap.setVisibility(View.GONE);
        if (mNoHeadsetLayout != null) mNoHeadsetLayout.setVisibility(View.GONE);
        if (mMainLayout != null) mMainLayout.setVisibility(View.VISIBLE);
        if (mBtnPlayContainer != null) mBtnPlayContainer.setVisibility(View.VISIBLE);
    }

    /**
     * change to no headset layout
     */
    private void changeToNoHeadsetLayout() {
        if (mMainLayout != null) mMainLayout.setVisibility(View.GONE);
        if (mBtnPlayContainer != null) mBtnPlayContainer.setVisibility(View.GONE);
        if (mNoEarphoneTextLayout != null) mNoEarphoneTextLayout.setVisibility(View.VISIBLE);
        if (mNoHeadsetImgView != null) mNoHeadsetImgView.setVisibility(View.VISIBLE);
        if (mNoHeadsetImgViewWrap != null) mNoHeadsetImgViewWrap.setVisibility(View.VISIBLE);
        if (mNoHeadsetLayout != null) mNoHeadsetLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        boolean granted = true;
        boolean mShowPermission = true;
        if (permissions.length <= 0 || grantResults.length <= 0) {
            Log.d(TAG, "permission length not sufficient");
            showToast(getString(R.string.missing_required_permission));
            return;
        }
        if (requestCode == PERMISSION_REQUEST_POWER_ON) {
            granted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if (!granted) {
                mShowPermission = shouldShowRequestPermissionRationale(permissions[0]);
            }
            Log.i(TAG, "<onRequestPermissionsResult> Power on fm granted" + granted);
            if (granted == true) {
                if (mService != null) {
                    mService.setRecordingPermission(true);
                    powerUpFm();
                }
            } else if (!mShowPermission) {
                showToast(getString(R.string.missing_required_permission));
            }
        }
    }
}
