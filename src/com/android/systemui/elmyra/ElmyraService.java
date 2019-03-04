package com.google.android.systemui.elmyra;

import android.content.Context;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import com.android.internal.logging.MetricsLogger;
import com.google.android.systemui.elmyra.actions.Action;
import com.google.android.systemui.elmyra.actions.Action.Listener;
import com.google.android.systemui.elmyra.feedback.FeedbackEffect;
import com.google.android.systemui.elmyra.gates.Gate;
import com.google.android.systemui.elmyra.sensors.GestureSensor;
import com.google.android.systemui.elmyra.sensors.GestureSensor.DetectionProperties;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ElmyraService {
    protected final Listener mActionListener = new C15821();
    private final List<Action> mActions;
    private final Context mContext;
    private final List<FeedbackEffect> mFeedbackEffects;
    protected final Gate.Listener mGateListener = new C15832();
    private final List<Gate> mGates;
    private final GestureSensor.Listener mGestureListener = new GestureListener(this, null);
    private final GestureSensor mGestureSensor;
    private Action mLastActiveAction;
    private final MetricsLogger mLogger;
    private final PowerManager mPowerManager;
    private final WakeLock mWakeLock;

    private Handler mHandler;

    /* renamed from: com.google.android.systemui.elmyra.ElmyraService$1 */
    class C15821 implements Listener {
        C15821() {
        }

        @Override
        public void onActionAvailabilityChanged(Action action) {
            updateSensorListener();
        }
    }

    /* renamed from: com.google.android.systemui.elmyra.ElmyraService$2 */
    class C15832 implements Gate.Listener {
        C15832() {
        }

        @Override
        public void onGateChanged(Gate gate) {
            updateSensorListener();
        }
    }

    private class GestureListener implements GestureSensor.Listener {

        private boolean mIsStillSqueezing;
        private DetectionProperties mDetectionProperties;

        private GestureListener() {
        }

        GestureListener(ElmyraService elmyraService, C15821 c15821) {
            this();
        }

        public void onGestureDetected(GestureSensor gestureSensor, DetectionProperties detectionProperties) {
            mWakeLock.acquire(2000);
            mDetectionProperties = detectionProperties;
            /*Action activeAction = updateActiveAction();
            if (activeAction != null) {
                activeAction.onTrigger(detectionProperties);
                mFeedbackEffects.forEach(feedbackEff -> feedbackEff.onResolve(detectionProperties));
            }*/
            mHandler.postDelayed(mLongSqueezeRunnable, 700);
        }

        private final Runnable mLongSqueezeRunnable = new Runnable() {
            @Override
            public void run() {
                Action activeAction = updateActiveAction();
                if (activeAction != null) {
                    mDetectionProperties.setLongSqueeze(mIsStillSqueezing);
                    activeAction.onTrigger(mDetectionProperties);
                    mFeedbackEffects.forEach(feedbackEff -> feedbackEff.onResolve(mDetectionProperties));
                }
            }
        };

        public void onGestureProgress(GestureSensor gestureSensor, float f, int i) {
            mIsStillSqueezing = f != 0;
            Action activeAction = updateActiveAction();
            if (activeAction != null) {
                activeAction.onProgress(f, i);
                mFeedbackEffects.forEach(feedbackEff -> feedbackEff.onProgress(f, i));
            }
        }
    }

    public ElmyraService(Context context, ServiceConfiguration serviceConfiguration) {
        mContext = context;
        mLogger = new MetricsLogger();
        mPowerManager = (PowerManager) mContext.getSystemService("power");
        mWakeLock = mPowerManager.newWakeLock(1, "Elmyra/ElmyraService");
        mHandler = new Handler();
        mActions = new ArrayList(serviceConfiguration.getActions());
        mActions.forEach(action -> action.setListener(mActionListener));

        mFeedbackEffects = new ArrayList(serviceConfiguration.getFeedbackEffects());

        mGates = new ArrayList(serviceConfiguration.getGates());
        mGates.forEach(gate -> gate.setListener(mGateListener));
        mGestureSensor = serviceConfiguration.getGestureSensor();
        if (mGestureSensor != null) {
            mGestureSensor.setGestureListener(mGestureListener);
        }
        updateSensorListener();
    }

    private Gate getBlockingGate() {
        for (Gate gate : mGates) {
            if (gate.isBlocking()) {
                return gate;
            }
        }
        // If we are here, we haven't found a blocking gate.
        return null;
    }

    private Action firstAvailableAction() {
        // TODO: put some logic as soon as we add more actions.
        return mActions.get(0);
    }

    private void startListening() {
        if (mGestureSensor != null && !mGestureSensor.isListening()) {
            mGestureSensor.startListening();
        }
    }

    private void stopListening() {
        if (mGestureSensor != null && mGestureSensor.isListening()) {
            mGestureSensor.stopListening();
            for (int i = 0; i < mFeedbackEffects.size(); i++) {
                mFeedbackEffects.get(i).onRelease();
            }
            Action updateActiveAction = updateActiveAction();
            if (updateActiveAction != null) {
                updateActiveAction.onProgress(0.0f, 0);
            }
        }
    }

    private Action updateActiveAction() {
        Action firstAvailableAction = firstAvailableAction();
        if (!(mLastActiveAction == null || firstAvailableAction == mLastActiveAction)) {
            mLastActiveAction.onProgress(0.0f, 0);
        }
        mLastActiveAction = firstAvailableAction;
        return firstAvailableAction;
    }

    protected void updateSensorListener() {
        Action updateActiveAction = updateActiveAction();
        if (updateActiveAction == null) {
            // Deactivate gates
            mGates.forEach(gate -> gate.deactivate());
            stopListening();
            return;
        }
        // Activate gates
        mGates.forEach(gate -> gate.activate());
        Gate blockingGate = getBlockingGate();
        if (blockingGate != null) {
            stopListening();
            return;
        }
        startListening();
    }
}
