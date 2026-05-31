package com.androlua;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.luajava.LuaException;
import com.luajava.LuaFunction;
import com.luajava.LuaObject;

public class LuaDrawable extends Drawable implements LuaGcable {

    private final LuaContext mContext;
    private final LuaObject mDraw;

    private final Paint mPaint;
    private LuaFunction mOnDraw;

    private boolean mGc = false;


    public LuaDrawable(LuaFunction func) {
        mDraw = func;
        mPaint = new Paint();
        mContext = mDraw.getLuaState().getContext();
        if (mContext instanceof LuaActivity) {
            ((LuaActivity) mContext).regGc(this);
        }
    }

    @Override
    public void draw(Canvas p1) {
        if (mGc) return;
        try {
            if (mOnDraw == null) {
                Object r = mDraw.call(p1, mPaint, this);
                if (r != null && r instanceof LuaFunction)
                    mOnDraw = (LuaFunction) r;
            }
            if (mOnDraw != null) {
                mOnDraw.call(p1);
            }
        } catch (LuaException e) {
            if (!mGc) {
                mContext.sendError("onDraw", e);
            }
        }
    }

    @Override
    public void setAlpha(int p1) {
        mPaint.setAlpha(p1);
    }

    @Override
    public void setColorFilter(ColorFilter p1) {
        mPaint.setColorFilter(p1);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public Paint getPaint() {
        return mPaint;
    }

    @Override
    public void gc() {
        mGc = true;
        mOnDraw = null;
    }

    @Override
    public boolean isGc() {
        return mGc;
    }
}
