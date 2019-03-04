package com.google.android.systemui.elmyra.sensors;

import java.util.Random;

public interface GestureSensor extends Sensor {

    public interface Listener {
        void onGestureDetected(GestureSensor gestureSensor, DetectionProperties detectionProperties);

        void onGestureProgress(GestureSensor gestureSensor, float f, int i);
    }

    public static class DetectionProperties {
        final long mActionId = new Random().nextLong();
        final boolean mHapticConsumed;
        final boolean mHostSuspended;
        boolean mLongSqueeze;

        public DetectionProperties(boolean z, boolean z2, boolean longSqueeze) {
            mHapticConsumed = z;
            mHostSuspended = z2;
            mLongSqueeze = longSqueeze;
        }

        public long getActionId() {
            return mActionId;
        }

        public boolean isHapticConsumed() {
            return mHapticConsumed;
        }

        public boolean isHostSuspended() {
            return mHostSuspended;
        }

        public boolean isLongSqueeze() {
            return mLongSqueeze;
        }

        public void setLongSqueeze(boolean longSqueeze) {
            mLongSqueeze = longSqueeze;
        }
    }

    void setGestureListener(Listener listener);
}
