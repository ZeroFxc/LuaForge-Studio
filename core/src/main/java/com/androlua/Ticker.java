package com.androlua;

import android.os.Handler;
import android.os.Message;

public class Ticker implements LuaGcable {
    private Handler mHandler;

    private Ticker.OnTickListener mOnTickListener;

    private Thread mThread;

    private long mPeriod = 1000;

    private boolean mEnabled = true;

    private boolean isRun = false;

    private long mLast;

    private long mOffset;

    private boolean mGc = false;


    public Ticker() {
        init();
    }

    private void init() {
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (mOnTickListener != null && !mGc) {
                    try {
                        mOnTickListener.onTick();
                    } catch (Exception e) {
                        e.printStackTrace();
                        mGc = true;
                        isRun = false;
                        mOnTickListener = null;
                    }
                }
            }
        };
        mThread = new Thread() {
            @Override
            public void run() {
                isRun = true;
                while (isRun && !mGc && mHandler != null) {
                    long now = System.currentTimeMillis();
                    if (!mEnabled)
                        mLast = now - mOffset;
                    if (now - mLast >= mPeriod) {
                        mLast = now;
                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(0);
                        }
                    }

                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
    }

    public void setPeriod(long period) {
        mLast = System.currentTimeMillis();
        mPeriod = period;
    }

    public long getPeriod() {
        return mPeriod;
    }


    public void setInterval(long period) {
        mLast = System.currentTimeMillis();
        mPeriod = period;
    }

    public long getInterval() {
        return mPeriod;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!enabled)
            mOffset = System.currentTimeMillis() - mLast;
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    public void setOnTickListener(OnTickListener ltr) {
        mOnTickListener = ltr;
    }

    public void start() {
        mThread.start();
    }

    public void stop() {
        isRun = false;
    }

    public boolean isRun() {
        return isRun;
    }

    @Override
    public void gc() {
        mGc = true;
        isRun = false;
        mOnTickListener = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean isGc() {
        return mGc;
    }


    public interface OnTickListener {
        void onTick();
    }
}
