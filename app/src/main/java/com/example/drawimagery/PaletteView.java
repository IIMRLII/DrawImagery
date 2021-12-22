package com.example.drawimagery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by wensefu on 17-3-21.
 */
public class PaletteView extends View {

    public static int initPaintsAmount = 8;
    public static int initDrawSize = 3;
    public static int initEraserSize = 10;

    public static int paintsAmount;

    public static boolean isMirrorDraw = true;
    public static boolean isDazzleColor = false;

    public static Matrix matrix = new Matrix();//矩阵变换

    private Paint mPaintMouse;//鼠标拖尾画笔
    private boolean isMouseDown = false;//鼠标是否按下

    private float mouseTouchX = 0;
    private float mouseTouchY = 0;
    private float mouseCurrentX = 0;//当前鼠标位置X
    private float mouseCurrentY = 0;//当前鼠标位置Y
    Queue<Float> mouseX = new LinkedList<Float>();//保存鼠标轨迹X
    Queue<Float> mouseY = new LinkedList<Float>();//保存鼠标轨迹Y

    private int time = 0;//累加时间
    private int mouse_down_time = 0;//鼠标按下累加时间

    private Paint mPaint;
    private Path mPath;
    private Path mPathMirror;
    private float mLastX;
    private float mLastY;
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;

    public float canvasWidth;
    public float canvasHeight;
    public static float canvasCenterX;
    public static float canvasCenterY;

    private static final int MAX_CACHE_STEP = 20;//最多保存20步

    public List<DrawingInfo> mDrawingList;
    public List<DrawingInfo> mRemovedList;

    private Xfermode mXferModeClear;
    private Xfermode mXferModeDraw;
    private int mDrawSize;
    private int mEraserSize;
    private int mMouseAlpha = 188;

    private boolean mCanEraser;

    private Callback mCallback;

    public enum Mode {
        DRAW,
        ERASER
    }

    private Mode mMode = Mode.DRAW;

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            time++;
            mouseCurrentX = mouseTouchX;
            mouseCurrentY = mouseTouchY;
            if(isMouseDown) {
                mouse_down_time++;
            } else {
                mouse_down_time = 0;
            }
            int size = mouseX.size();
            if (isMouseDown) {
                mouseX.offer(mouseCurrentX);
                mouseY.offer(mouseCurrentY);
            }
            if (size > 20 || !isMouseDown) {
                mouseX.poll();
                mouseY.poll();
            }
            invalidate();//告诉主线程重新绘制
            handler.postDelayed(this, 20);//每20ms循环一次，50fps
        }
    };

    public PaletteView(Context context) {
        super(context);
        init();
    }

    public PaletteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaletteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {//获取画面宽高
        super.onWindowFocusChanged(hasFocus);

        canvasWidth = getWidth();
        canvasHeight = getHeight();
        canvasCenterX = canvasWidth / 2;
        canvasCenterY = canvasHeight / 2;
    }

    public interface Callback {
        void onUndoRedoStatusChanged();
    }

    public void setCallback(Callback callback){
        mCallback = callback;
    }

    private void init() {
        setDrawingCacheEnabled(true);

        paintsAmount = initPaintsAmount;
        matrix.setScale (-1.0F, 1.0F);//水平对称

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFilterBitmap(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawSize = initDrawSize;
        mEraserSize = initEraserSize;
        mPaint.setStrokeWidth(mDrawSize);
        mPaint.setColor(Color.argb(50 + (int)(Math.random() * 205),(int)(Math.random() * 255),(int)(Math.random() * 255),(int)(Math.random() * 255)));
        mXferModeDraw = new PorterDuffXfermode(PorterDuff.Mode.SRC);
        mXferModeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mPaint.setXfermode(mXferModeDraw);

        mPaintMouse = new Paint();//对画笔初始化
        mPaintMouse.setColor(Color.RED);//设置画笔颜色
        mPaintMouse.setStrokeWidth(10);//设置画笔宽度
        mPaintMouse.setAntiAlias(true);//设置抗锯齿

        handler.postDelayed(runnable, 20);

    }

    private void initBuffer(){
        mBufferBitmap = Bitmap.createBitmap((int)canvasWidth, (int)canvasHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }

    private abstract static class DrawingInfo {
        Paint paint;
        int paints_amount = paintsAmount;
        boolean mirror_draw = isMirrorDraw;

        abstract void draw(Canvas canvas);
    }

    private static class PathDrawingInfo extends DrawingInfo{
        Path path;
        Path pathMirror;

        @Override
        void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(canvasCenterX, canvasCenterY);
            for(int i = 0;i < paints_amount;i++) {
                canvas.drawPath(path, paint);
                canvas.rotate(360 / (float)paints_amount);
            }
            canvas.restore();

            if (mirror_draw) {
                canvas.save();
                canvas.translate(canvasCenterX, canvasCenterY);
                path.transform(matrix, pathMirror);//镜像对称
                for(int i = 0;i < paints_amount;i++) {
                    canvas.drawPath(path, paint);
                    canvas.rotate(360 / (float)paints_amount);
                }
                canvas.restore();
            }
        }
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            if (mMode == Mode.DRAW) {
                mPaint.setXfermode(mXferModeDraw);
                mPaint.setStrokeWidth(mDrawSize);
            } else {
                mPaint.setXfermode(mXferModeClear);
                mPaint.setStrokeWidth(mEraserSize);
            }
        }
    }

    public void setEraserSize(int size) {
        mEraserSize = size;
        if (mMode == Mode.ERASER) mPaint.setStrokeWidth(mEraserSize);
    }

    public void setPenRawSize(int size) {
        mDrawSize = size;
        if(mMode == Mode.DRAW) mPaint.setStrokeWidth(mDrawSize);
    }

    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    private void reDraw(){
        if (mDrawingList != null) {
            mBufferBitmap.eraseColor(Color.TRANSPARENT);
            for (DrawingInfo drawingInfo : mDrawingList) {
                drawingInfo.draw(mBufferCanvas);
            }
            invalidate();
        }
    }

    public int getPenColor(){
        return mPaint.getColor();
    }

    public int getPenSize(){
        return mDrawSize;
    }

    public int getEraserSize(){
        return mEraserSize;
    }

    public void setPenAlpha(int alpha){
        mMouseAlpha = alpha;
        if(mMode == Mode.DRAW){
            mPaint.setAlpha(alpha);
        }
    }

    public int getPenAlpha(){
        return mMouseAlpha;
    }

    public boolean canRedo() {
        return mRemovedList != null && mRemovedList.size() > 0;
    }

    public boolean canUndo(){
        return mDrawingList != null && mDrawingList.size() > 0;
    }

    public void redo() {//重做
        int size = mRemovedList == null ? 0 : mRemovedList.size();
        if (size > 0) {
            DrawingInfo info = mRemovedList.remove(size - 1);
            mDrawingList.add(info);
            mCanEraser = true;
            reDraw();
            if (mCallback != null) {
                mCallback.onUndoRedoStatusChanged();
            }
        }
    }

    public void undo() {//撤销
        int size = mDrawingList == null ? 0 : mDrawingList.size();
        if (size > 0) {
            DrawingInfo info = mDrawingList.remove(size - 1);
            if (mRemovedList == null) mRemovedList = new ArrayList<>(MAX_CACHE_STEP);
            if (size == 1) mCanEraser = false;
            mRemovedList.add(info);
            reDraw();
            if (mCallback != null) {
                mCallback.onUndoRedoStatusChanged();
            }
        }
    }

    public void clear() {
        if (mBufferBitmap != null) {
            if (mDrawingList != null) mDrawingList.clear();
            if (mRemovedList != null) mRemovedList.clear();
            mCanEraser = false;
            mBufferBitmap.eraseColor(Color.TRANSPARENT);
            invalidate();
            if (mCallback != null) {
                mCallback.onUndoRedoStatusChanged();
            }
        }
    }

    public Bitmap buildBitmap() {
        Bitmap bm = getDrawingCache();
        Bitmap result = Bitmap.createBitmap(bm);
        destroyDrawingCache();
        return result;
    }

    private void saveDrawingPath(){
        if (mDrawingList == null) {//初始化绘制操作列表
            mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
        } else if (mDrawingList.size() == MAX_CACHE_STEP) {
            mDrawingList.remove(0);
        }
        Path cachePath = new Path(mPath);
        Paint cachePaint = new Paint(mPaint);
        PathDrawingInfo info = new PathDrawingInfo();
        info.path = cachePath;
        info.paint = cachePaint;
        info.paints_amount = paintsAmount;
        info.mirror_draw = isMirrorDraw;
        mDrawingList.add(info);
        mCanEraser = true;
        if (mCallback != null) {
            mCallback.onUndoRedoStatusChanged();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBufferBitmap != null) {
            canvas.drawBitmap(mBufferBitmap, 0, 0, null);
        }
        mouse_tail(canvas);//绘制鼠标拖尾
    }

    @SuppressWarnings("all")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled()){
            return false;
        }
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        final float x = event.getX();
        final float y = event.getY();
        float coordinateX = x - canvasCenterX;
        float coordinateY = y - canvasCenterY;
        mouseTouchX = coordinateX;
        mouseTouchY = coordinateY;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isMouseDown = true;
                mLastX = coordinateX;
                mLastY = coordinateY;
                if (mPath == null) mPath = new Path();
                if (mPathMirror == null) mPathMirror = new Path();
                mPath.moveTo(coordinateX, coordinateY);
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.quadTo(mLastX, mLastY, (coordinateX + mLastX) / 2, (coordinateY + mLastY) / 2);
                if (mBufferBitmap == null) initBuffer();
                if (mMode == Mode.ERASER && !mCanEraser) break;//橡皮擦就不画

                if (isDazzleColor) {
                    int[] color = Bezier.rainBow((float)time % 300 / 300);
                    mPaint.setColor(Color.argb(255, color[0], color[1], color[2]));
                }
                mBufferCanvas.save();
                mBufferCanvas.translate(canvasCenterX, canvasCenterY);
                for (int i = 0;i < paintsAmount; i++) {
//                    mPaint.setColor(Color.argb(255,255,255,0));
//                    mPaint.setStrokeWidth(20);
//                    mBufferCanvas.drawPath(mPath, mPaint);
//                    mPaint.setColor(Color.argb(255,0,128,255));
//                    mPaint.setStrokeWidth(12);
                    mBufferCanvas.drawPath(mPath, mPaint);
                    mBufferCanvas.rotate(360 / (float)paintsAmount);
                }
                mBufferCanvas.restore();

                if (isMirrorDraw) {
                    mBufferCanvas.save();
                    mBufferCanvas.translate(canvasCenterX, canvasCenterY);
                    for (int j = 0; j < paintsAmount; j++) {
                        mPath.transform(matrix, mPathMirror);
                        mBufferCanvas.drawPath(mPathMirror, mPaint);
                        mBufferCanvas.rotate(360 / (float)paintsAmount);
                    }
                    mBufferCanvas.restore();
                }

//                invalidate();
                mLastX = coordinateX;
                mLastY = coordinateY;
                break;
            case MotionEvent.ACTION_UP:
                isMouseDown = false;
                if (mMode == Mode.DRAW || mCanEraser) saveDrawingPath();
                mPath.reset();
                break;
        }
        return true;
    }

    private void mouse_tail(Canvas canvas) {//鼠标拖尾
        int[] color;
//        color = Bezier.rainBow((float)time % 300 / 300); //画笔同一颜色随时间渐变
        int size = mouseX.size();
        float x1 = 0,x2 = 0,y1 = 0,y2 = 0;

        for (int i = 0; i < size; i++) {
            float percent = (float)i / size;
            float res[] = Bezier.bezier((LinkedList)mouseX, (LinkedList)mouseY, percent);

            x1 = res[0];
            y1 = res[1];
            if(i == 0){
                x2 = x1;
                y2 = y1;
                continue;
            }

            canvas.save();
            canvas.translate(canvasCenterX, canvasCenterY);
            color = Bezier.rainBow((time + percent * 300) % 300 / 300); //画笔不同颜色随时间渐变
            mPaintMouse.setColor(Color.argb(mMouseAlpha, color[0], color[1], color[2]));
            mPaintMouse.setStrokeWidth(percent * 20);
            for(int j = 0; j < paintsAmount; j++) {
                canvas.drawLine(x1, y1, x2, y2, mPaintMouse);
                if (i == size - 1) canvas.drawLine(x1, y1, mouseCurrentX, mouseCurrentY, mPaintMouse);//连接最后一段与鼠标
                canvas.rotate(360 / (float)paintsAmount);
            }
            canvas.restore();

            if (isMirrorDraw) {
                canvas.save();
                canvas.translate(canvasCenterX, canvasCenterY);
                color = Bezier.rainBow((time + percent * 300) % 300 / 300); //画笔不同颜色随时间渐变
                mPaintMouse.setColor(Color.argb(mMouseAlpha, color[0], color[1], color[2]));
                mPaintMouse.setStrokeWidth(percent * 20);
                for(int j = 0; j < paintsAmount; j++) {
                    canvas.drawLine(-x1, y1, -x2, y2, mPaintMouse);
                    if (i == size - 1) canvas.drawLine(-x1, y1, -mouseCurrentX, mouseCurrentY, mPaintMouse);//连接最后一段与鼠标
                    canvas.rotate(360 / (float)paintsAmount);
                }

                canvas.restore();
            }

            x2 = x1;
            y2 = y1;
        }

        canvas.save();//绘制鼠标中心
        canvas.translate(canvasCenterX, canvasCenterY);
        color = Bezier.rainBow((float)time % 300 / 300); //画笔不同颜色随时间渐变
        mPaintMouse.setColor(Color.argb(128, color[0], color[1], color[2]));
        for(int j = 0; j < paintsAmount; j++) {
            canvas.drawCircle(mouseCurrentX, mouseCurrentY, 10, mPaintMouse);//绘制鼠标中心
            canvas.rotate(360 / (float)paintsAmount);
        }
        canvas.restore();

        if (isMirrorDraw) {
            canvas.save();
            canvas.translate(canvasCenterX, canvasCenterY);
            for(int j = 0; j < paintsAmount; j++) {
                canvas.drawCircle(-mouseCurrentX, mouseCurrentY, 10, mPaintMouse);//绘制鼠标中心
                canvas.rotate(360 / (float)paintsAmount);
            }
            canvas.restore();
        }
    }
}
