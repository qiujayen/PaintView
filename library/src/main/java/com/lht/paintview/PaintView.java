package com.lht.paintview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lht.paintview.pojo.DrawPath;
import com.lht.paintview.pojo.DrawPoint;
import com.lht.paintview.pojo.DrawRect;
import com.lht.paintview.pojo.DrawShape;
import com.lht.paintview.pojo.DrawText;
import com.lht.paintview.pojo.StrokePaint;

import java.util.ArrayList;

/**
 * Created by lht on 16/10/17.
 */

public class PaintView extends View {

    private OnDrawListener mOnDrawListener;

    public interface OnDrawListener {
        void afterPaintInit(int viewWidth, int viewHeight);
        void afterEachPaint(ArrayList<DrawShape> drawShapes);
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    //View Size
    //View尺寸
    private boolean bInited = false;
    private int mWidth, mHeight;

    //Background Color
    //背景色
    private int mBgColor = Color.WHITE;
    //Paint List for Stroke
    //绘制笔迹Paint列表
    private ArrayList<StrokePaint> mPaintList = new ArrayList<>();

    // Paint for Text and Text Rectangle
    // 用于绘制文字和文字边框
    private StrokePaint mTextRectPaint;
    private DrawText mCurrentText;
    private DrawRect mCurrentTextRect;
    private boolean bTextDrawing = false, bTextDraging = false;
    //Paint List for Stroke
    //绘制文字Paint列表
    private ArrayList<StrokePaint> mTextPaintList = new ArrayList<>();

    //Background Image
    //背景图
    private Bitmap mBgBitmap = null;
    private int mBgPadding = 0;
    //Paint for Background
    //绘制背景图Paint
    private Paint mBgPaint;

    //Current Coordinate
    //当前坐标
    private float mCurrentX, mCurrentY;
    //Current Drawing Path
    //当前绘制路径
    private Path mCurrentPath;

    //Shape List(Path, Point and Text)
    //绘制列表(线、点和文字）
    private ArrayList<DrawShape> mDrawShapes = new ArrayList<>();
    private boolean bPathDrawing = false;

    //Gesture
    //手势
    private final static int SINGLE_FINGER = 1, DOUBLE_FINGER = 2;
    protected enum GestureState {
        NONE, DRAG, ZOOM
    }
    private GestureState mGestureState = GestureState.NONE;

    private boolean bGestureEnable = true;
    private float mScaleMax = 2f, mScaleMin = 0.5f;

    //Center Point of Two Fingers
    //当次两指中心点
    private float mCurrentCenterX, mCurrentCenterY;
    //当次两指间距
    private float mCurrentLength = 0;
    //当次位移
    private float mCurrentDistanceX, mCurrentDistanceY;
    //当次缩放
    private float mCurrentScale;

    //整体矩阵
    private Matrix mMainMatrix = new Matrix();
    private float[] mMainMatrixValues = new float[9];
    //当次矩阵
    private Matrix mCurrentMatrix = new Matrix();

    public PaintView(Context context) {
        super(context);
        init();
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setDrawingCacheEnabled(true);

        initPaint();
    }

    private void initPaint() {
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setDither(true);

        StrokePaint paint = new StrokePaint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);// 设置外边缘
        paint.setStrokeCap(Paint.Cap.ROUND);// 形状

        mPaintList.add(paint);

        StrokePaint textPaint = new StrokePaint(paint);
        textPaint.setStyle(Paint.Style.FILL);
        mTextPaintList.add(textPaint);

        mTextRectPaint = new StrokePaint(paint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!bInited) {
            mWidth = right - left;
            mHeight = bottom - top;

            resizeBitmap();

            bInited = true;

            mOnDrawListener.afterPaintInit(mWidth, mHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgColor);

        if (mBgBitmap != null) {
            canvas.drawBitmap(mBgBitmap, mMainMatrix, mBgPaint);
        }

        for (DrawShape shape : mDrawShapes) {
            shape.draw(canvas, mCurrentMatrix);
        }
    }

    public ArrayList<DrawShape> getDrawShapes() {
        return mDrawShapes;
    }

    public void setDrawShapes(ArrayList<DrawShape> mDrawShapes) {
        this.mDrawShapes = mDrawShapes;
    }

    public enum TextGravity {
        FREE, CENTER, CENTER_HORIZONTAL, CENTER_VERTICAL
    }

    /**
     * 添加文字
     * @param text
     * @param x
     * @param y
     */
    public void addText(String text, float x, float y, TextGravity gravity) {
        Rect textRect = measureText(text);

        switch (gravity) {
            case CENTER:
                x = (mWidth - textRect.width()) / 2;
                y = (mHeight + textRect.height()) / 2;
                break;
            case CENTER_HORIZONTAL:
                x = (mWidth - textRect.width()) / 2;
                break;
            case CENTER_VERTICAL:
                y = (mHeight + textRect.height()) / 2;
                break;
        }

        DrawText drawText = new DrawText(x, y, getCurrentTextPaint());
        drawText.setText(text);
        mDrawShapes.add(drawText);
        invalidate();
    }

    /**
     * 测量文字
     * @param text
     * @return rect.width() for text width, rect.height() for text height
     */
    public Rect measureText(String text) {
        Rect rect = new Rect();
        Paint paint = new Paint(getCurrentPaint());
        paint.setTextSize(getCurrentTextPaint().getActualTextSize());
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * Text painting start
     * 开始绘制文字
     * @param x coordinate of bottom left corner
     * @param y 左下角坐标
     */
    public void startText(float x, float y) {
        bTextDrawing = true;
        mCurrentText = new DrawText(getCurrentTextPaint());
        //文字初始坐标位于view中心
        mCurrentText.setCoordinate(x, y);

        mCurrentTextRect = new DrawRect(mCurrentText.getTextBoundRect(), mTextRectPaint);

        mDrawShapes.add(mCurrentText);
        mDrawShapes.add(mCurrentTextRect);
        invalidate();
    }

    /**
     * Text painting start at screen center point
     * 于屏幕重心开始绘制文字
     */
    public void startText() {
        startText(mWidth / 2, mHeight / 2);
    }

    /**
     * When text is inputting
     * 文字输入中
     * @param text
     */
    public void changeText(String text) {
        mCurrentText.setText(text);
        mCurrentTextRect.setRect(mCurrentText.getTextBoundRect());

        invalidate();
    }

    /**
     * Text painting finish
     * 结束绘制文字
     */
    public void endText() {
        bTextDrawing = false;
        //删除文字边框
        undo();
    }

    public boolean isGestureEnable() {
        return bGestureEnable;
    }

    /**
     * 设置手势是否可用
     * @param gestureEnable
     */
    public void setGestureEnable(boolean gestureEnable) {
        this.bGestureEnable = gestureEnable;
    }

    /**
     * 设置缩放上限，默认为2
     * @param scaleMax
     */
    public void setScaleMax(float scaleMax) {
        this.mScaleMax = scaleMax;
    }

    /**
     * 设置缩放下限，默认为0.5
     * @param scaleMin
     */
    public void setScaleMin(float scaleMin) {
        this.mScaleMin = scaleMin;
    }

    /**
     * Undo
     * 撤销
     * @return is Undo still available 是否还能撤销
     */
    public boolean undo() {
        if (mDrawShapes != null && mDrawShapes.size() > 0) {
            mDrawShapes.remove(mDrawShapes.size() - 1);
            invalidate();
        }

        if (mOnDrawListener != null) {
            mOnDrawListener.afterEachPaint(mDrawShapes);
        }

        return mDrawShapes != null && mDrawShapes.size() > 0;
    }

    /**
     * Set background color
     * 设置背景颜色
     * @param color 0xaarrggbb
     */
    public void setBackgroundColor(int color) {
        mBgColor = color;
    }

    /**
     * Set paint color
     * 设置画笔颜色
     * @param color 0xaarrggbb
     */
    public void setColor(int color) {
        StrokePaint paint = new StrokePaint(getCurrentPaint());
        paint.setColor(color);
        mPaintList.add(paint);
    }

    /**
     * Set stroke width
     * 设置画笔宽度
     * @param width
     */
    public void setStrokeWidth(int width) {
        StrokePaint paint = new StrokePaint(getCurrentPaint());
        paint.setStrokeWidth(width);
        mPaintList.add(paint);
    }

    /**
     * Set text color
     * 设置文字颜色
     * @param color 0xaarrggbb
     */
    public void setTextColor(int color) {
        StrokePaint paint = new StrokePaint(getCurrentTextPaint());
        paint.setColor(color);
        mTextPaintList.add(paint);

        mTextRectPaint.setColor(color);
    }

    /**
     * Set text size
     * 设置文字大小
     * @param size
     */
    public void setTextSize(int size) {
        StrokePaint paint = new StrokePaint(getCurrentTextPaint());
        paint.setTextSize(size);
        mTextPaintList.add(paint);
    }

    /**
     * 获取绘制结果图
     * @param isViewOnly true for just inside the view,
     *                   false for whole bitmap in original scale and transition
     * @return paint result 绘制结果图
     */
    public Bitmap getBitmap(boolean isViewOnly) {
        Bitmap result;
        if (isViewOnly) {
            destroyDrawingCache();
            result = getDrawingCache();
        }
        else {
            result = Bitmap.createBitmap(mBgBitmap.getWidth(),
                    mBgBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            Canvas canvas = new Canvas();
            canvas.setBitmap(result);

            canvas.drawColor(mBgColor);

            setBitmapPosition(mBgBitmap.getWidth(), mBgBitmap.getHeight(), matrix);
            if (mBgBitmap != null) {
                canvas.drawBitmap(mBgBitmap, matrix, mBgPaint);
            }

            mMainMatrix.invert(matrix);
            for (DrawShape shape : mDrawShapes) {
                shape.clone(1).draw(canvas, matrix);
            }
        }
        return result;
    }

    /**
     * Set background image
     * 设置背景图
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap, int padding) {
        mBgBitmap = bitmap;
        mBgPadding = padding;
    }

    /**
     * Set background image
     * 设置背景图
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        mBgBitmap = bitmap;
    }

    private void resizeBitmap() {
        if (mBgBitmap == null) {
            return;
        }

        if (mBgBitmap.getWidth() > mWidth - mBgPadding * 2 ||
                mBgBitmap.getHeight() > mHeight - mBgPadding * 2) {
            mBgBitmap = zoomBitmap(mBgBitmap,
                    mWidth - mBgPadding * 2,
                    mHeight - mBgPadding * 2);
        }

        setBitmapPosition(mWidth, mHeight, mMainMatrix);
    }

    private Bitmap zoomBitmap(Bitmap bm, int newWidth , int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        float scale = scaleWidth > scaleHeight ? scaleHeight : scaleWidth;

        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

    private void setBitmapPosition(int width, int height, Matrix matrix) {
        float left = (width - mBgBitmap.getWidth()) / 2;
        float top = (height - mBgBitmap.getHeight()) / 2;
        //缩放后
        if (mBgBitmap.getWidth() < width && mBgBitmap.getHeight() < height) {
            matrix.setTranslate(left, top);
        }
        else if (mBgBitmap.getWidth() < height) {
            matrix.setTranslate(left, 0);
        }
        else if (mBgBitmap.getHeight() < height) {
            matrix.setTranslate(0, top);
        }
    }

    /**
     * 获得当前笔迹
     */
    private StrokePaint getCurrentPaint() {
        return mPaintList.get(mPaintList.size() - 1);
    }

    /**
     * 获得当前文字笔迹
     */
    private StrokePaint getCurrentTextPaint() {
        return mTextPaintList.get(mTextPaintList.size() - 1);
    }

    /**
     * 缩放所有笔迹
     */
    private void scaleStrokeWidth(float scale) {
        for (StrokePaint paint: mPaintList) {
            paint.setScale(paint.getScale() * scale);
        }
        for (StrokePaint paint: mTextPaintList) {
            paint.setScale(paint.getScale() * scale);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mGestureState = GestureState.NONE;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            //文字不在输入时，多点按下
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!bTextDrawing && bGestureEnable) {
                    doubleFingerDown(event);
                }
                break;
            //单点按下
            case MotionEvent.ACTION_DOWN:
                touchDown(x, y);
                break;
            //移动
            case MotionEvent.ACTION_MOVE:
                //文字不在输入时，单点移动
                if (event.getPointerCount() == SINGLE_FINGER) {
                    touchMove(x, y);
                }
                //文字不在输入时，多点移动
                else if (event.getPointerCount() == DOUBLE_FINGER && !bTextDrawing && bGestureEnable) {
                    doubleFingerMove(event);
                }
                break;
            //单点抬起
            case MotionEvent.ACTION_UP:
                touchUp(x, y);
                break;
        }

        switch (mGestureState) {
            case DRAG:
//                setDragMode();
                mMainMatrix.postTranslate(mCurrentDistanceX, mCurrentDistanceY);
                mCurrentMatrix.setTranslate(mCurrentDistanceX, mCurrentDistanceY);
                break;
            case ZOOM:
//                setZoomMode();
                mMainMatrix.postScale(mCurrentScale, mCurrentScale,
                        mCurrentCenterX, mCurrentCenterY);
                mCurrentMatrix.setScale(mCurrentScale, mCurrentScale,
                        mCurrentCenterX, mCurrentCenterY);
                scaleStrokeWidth(mCurrentScale);
                break;
            case NONE:
                mCurrentMatrix.reset();
                break;
        }

        mMainMatrix.getValues(mMainMatrixValues);

        invalidate();
        return true;
    }

    private void touchDown(float x, float y) {
        mCurrentX = x;
        mCurrentY = y;

        if (bTextDrawing) {
            bTextDraging = mCurrentText.isInTextRect(mCurrentX, mCurrentY);
        }
    }

    private void touchMove(float x, float y) {
        final float previousX = mCurrentX;
        final float previousY = mCurrentY;

        mCurrentX = x;
        mCurrentY = y;

        final float dx = Math.abs(x - previousX);
        final float dy = Math.abs(y - previousY);

        //两点之间的距离大于等于3时，生成贝塞尔绘制曲线
        if (dx >= 3 || dy >= 3) {
            if (bTextDrawing) {
                if (bTextDraging) {
                    mCurrentText.setCoordinate(mCurrentX, mCurrentY);
                    mCurrentTextRect.setRect(mCurrentText.getTextBoundRect());
                }
            }
            else {
                if (!bPathDrawing) {
                    mCurrentPath = new Path();
                    mCurrentPath.moveTo(previousX, previousY);
                    mDrawShapes.add(
                            new DrawPath(mCurrentPath, getCurrentPaint()));
                    bPathDrawing = true;
                }

                //设置贝塞尔曲线的操作点为起点和终点的一半
                float cX = (mCurrentX + previousX) / 2;
                float cY = (mCurrentY + previousY) / 2;

                //二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
                mCurrentPath.quadTo(previousX, previousY, cX, cY);
            }
        }
    }

    private void touchUp(float x, float y) {
        //不在输入文字和绘制笔迹，而是点击时
        if (!bTextDrawing && !bPathDrawing && x == mCurrentX && y == mCurrentY) {
            mDrawShapes.add(
                    new DrawPoint(x, y, getCurrentPaint()));
        }

        //在输入文字时，变更文字坐标
        if (bTextDrawing && bTextDraging) {
            bTextDraging = false;
        }

        bPathDrawing = false;

        if (mOnDrawListener != null) {
            mOnDrawListener.afterEachPaint(mDrawShapes);
        }
    }

    //两点按下
    private void doubleFingerDown(MotionEvent event) {
        mCurrentCenterX = (event.getX(0) + event.getX(1)) / 2;
        mCurrentCenterY = (event.getY(0) + event.getY(1)) / 2;

        mCurrentLength = getDistance(event);
    }

    //两点移动
    private void doubleFingerMove(MotionEvent event) {
        //当前中心点
        float curCenterX = (event.getX(0) + event.getX(1)) / 2;
        float curCenterY = (event.getY(0) + event.getY(1)) / 2;

        //当前两点间距离
        float curLength = getDistance(event);

        //拖动
        if (Math.abs(mCurrentLength - curLength) < 5) {
            mGestureState = GestureState.DRAG;
            mCurrentDistanceX = curCenterX - mCurrentCenterX;
            mCurrentDistanceY = curCenterY - mCurrentCenterY;
        }
        //放大 || 缩小
        else if (mCurrentLength < curLength || mCurrentLength > curLength){
            mGestureState = GestureState.ZOOM;
            mCurrentScale = curLength / mCurrentLength;

            //放大缩小临界值判断
            float toScale = mMainMatrixValues[Matrix.MSCALE_X] * mCurrentScale;
            if (toScale > mScaleMax || toScale < mScaleMin) {
                mCurrentScale = 1;
            }
        }

        mCurrentCenterX = curCenterX;
        mCurrentCenterY = curCenterY;

        mCurrentLength = curLength;
    }

    /**
     * 获取两个触控点之间的距离
     * @param event
     * @return 两个触控点之间的距离
     */
    private float getDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return (float)Math.sqrt(x * x + y * y);
    }

    private void setDragMode() {
        mMainMatrix.getValues(mMainMatrixValues);

        float imageLeft = mMainMatrixValues[Matrix.MTRANS_X];
        float imageRight =
                mMainMatrixValues[Matrix.MTRANS_X] + mBgBitmap.getWidth() * mMainMatrixValues[Matrix.MSCALE_X];
        float imageTop = mMainMatrixValues[Matrix.MTRANS_Y];
        float imageBtm =
                mMainMatrixValues[Matrix.MTRANS_Y] + mBgBitmap.getHeight() * mMainMatrixValues[Matrix.MSCALE_Y];

        if (imageLeft + mCurrentDistanceX >= 0 ||
                imageRight + mCurrentDistanceX <= mWidth) {
            mCurrentDistanceX = 0;
        }

        if (imageTop + mCurrentDistanceY >= 0 ||
                imageBtm + mCurrentDistanceY <= mHeight) {
            mCurrentDistanceY = 0;
        }
    }

    private void setZoomMode() {
        float imageLeft = mMainMatrixValues[Matrix.MTRANS_X];
        float imageRight =
                mMainMatrixValues[Matrix.MTRANS_X] + mBgBitmap.getWidth() * mMainMatrixValues[Matrix.MSCALE_X];
        float imageTop = mMainMatrixValues[Matrix.MTRANS_Y];
        float imageBtm =
                mMainMatrixValues[Matrix.MTRANS_Y] + mBgBitmap.getHeight() * mMainMatrixValues[Matrix.MSCALE_Y];

        if (imageLeft == 0) {
            mCurrentCenterX = 0;
        }
        else if (imageRight == mWidth) {
            mCurrentCenterX = mWidth;
        }
        else {
            mCurrentCenterX = mWidth / 2;
        }

        if (imageTop >= 0) {
            mCurrentCenterY = 0;
        }
        else if (imageBtm <= mHeight) {
            mCurrentCenterY = mHeight;
        }
        else {
            mCurrentCenterY = mHeight / 2;
        }
    }

}
