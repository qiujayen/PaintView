package com.lht.paintview.pojo;

import android.graphics.Canvas;
import android.graphics.Matrix;

import java.io.Serializable;


/**
 * Created by lht on 16/10/17.
 */

public abstract class DrawShape implements Serializable {
    StrokePaint paint;

    public abstract void draw(Canvas canvas, Matrix matrix);
}
