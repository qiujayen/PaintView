package com.lht.paintview.pojo;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Parcel;

/**
 * Created by lht on 16/11/10.
 */

public class DrawRect extends DrawShape {

    private Rect rect;

    public DrawRect(Rect rect, SerializablePaint paint) {
        this.rect = rect;
        this.paint = paint;
    }

    private DrawRect(Parcel in) {
        paint = (SerializablePaint)in.readSerializable();
        rect = in.readParcelable(Rect.class.getClassLoader());
    }

    public void setRect(Rect r) {
        rect.set(r);
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix) {
        canvas.drawRect(rect, paint);
    }

    @Override
    public DrawShape clone(float scale) {
        SerializablePaint clonePaint = new SerializablePaint(paint);
        clonePaint.setScale(scale);

        return new DrawRect(new Rect(rect), clonePaint);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(paint);
        dest.writeParcelable(rect, PARCELABLE_WRITE_RETURN_VALUE);
    }

    // Parcelable CREATOR class
    public static final Creator<DrawRect> CREATOR = new Creator<DrawRect>() {
        @Override
        public DrawRect createFromParcel(Parcel in) {
            return new DrawRect(in);
        }

        @Override
        public DrawRect[] newArray(int size) {
            return new DrawRect[size];
        }
    };
}
