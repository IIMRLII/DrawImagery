package com.example.drawimagery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

public class MainCanvas extends View {

    private int view_width = 0;          //屏幕的宽度
    private int view_height = 0;         //屏幕的高度
    Bitmap cacheBitmap = null;          //定义一个内存中的图片，该图片作为缓冲区
    Canvas cacheCanvas = null;          //定义cacheBitmap上的Canvas对象


    private Paint mPaintMouse;//鼠标拖尾画笔
    private Paint mPaintSigmoid;//鼠标拖尾画笔

    private boolean mouse_begin = false;//鼠标是否按下

    private float mouseCurrentX = 0;//当前鼠标位置X
    private float mouseCurrentY = 0;//当前鼠标位置Y
    Queue<Float> mouseX = new LinkedList<Float>();//保存鼠标轨迹X
    Queue<Float> mouseY = new LinkedList<Float>();//保存鼠标轨迹Y

    private int time = 0;//累加时间
    private int mouse_down_time = 0;//鼠标按下累加时间

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            time++;
            invalidate();//告诉主线程重新绘制
            if(mouse_begin) {
                mouse_down_time++;
            } else {
                mouse_down_time = 0;
            }
            if (mouseX.peek() != null) {
                boolean is_add_mouse = Math.abs(mouseX.peek() - mouseCurrentX) < 0.01;//鼠标不动时不记录坐标
                if (!is_add_mouse) {
                    mouseX.offer(mouseCurrentX);
                    mouseY.offer(mouseCurrentY);
                }
                if (mouseX.size() > 20 || is_add_mouse) {
                    mouseX.poll();
                    mouseY.poll();
                }
            } else if (mouse_begin) {
                mouseX.offer(mouseCurrentX);
                mouseY.offer(mouseCurrentY);
            }
            handler.postDelayed(this, 20);//每20ms循环一次，50fps
        }
    };

    public MainCanvas(Context context) {
        super(context);
    }

    public MainCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        view_width = getContext().getResources().getDisplayMetrics().widthPixels;       //获取屏幕的宽度
        view_height = getContext().getResources().getDisplayMetrics().heightPixels;     //获取屏幕的高度
        cacheBitmap = Bitmap.createBitmap(view_width,view_height, Bitmap.Config.ARGB_8888);      //创建一个与该View相同大小的缓存区
        cacheCanvas = new Canvas();         //创建一个新的画布
        cacheCanvas.setBitmap(cacheBitmap);     //在cacheCanvas上绘制cacheBitmap

        handler.postDelayed(runnable, 20);
        mPaintMouse = new Paint();//对画笔初始化
        mPaintMouse.setColor(Color.RED);//设置画笔颜色
        mPaintMouse.setStrokeWidth(10);//设置画笔宽度
        mPaintMouse.setAntiAlias(true);//设置抗锯齿

        mPaintSigmoid = new Paint();//对画笔初始化
        mPaintSigmoid.setColor(Color.RED);//设置画笔颜色
        mPaintSigmoid.setStrokeWidth(10);//设置画笔宽度
        mPaintSigmoid.setAntiAlias(true);//设置抗锯齿
    }

    /*
     * 调用saveBitmap（）方法将当前绘图保存为PNG图片*/
    public void saveImage(){
        try{
            saveBitmap("myPicture");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *保存绘制好的位图方法saveBitmap（）
     * 首先在SD卡上创建一个文件，然后创建一个文件输出流对象，调用Bitmap类的compress（）方法将绘图内容压缩为PNG歌是输出到创建的文件间输出流对象中
     * 最后将缓冲区的数据全部写出到输出流中，关闭文件输出流对象
     *  */
    private void saveBitmap(String fileName) throws IOException {
        String storage = Environment.getExternalStorageDirectory().getPath();
        Toast.makeText(getContext(),storage,Toast.LENGTH_SHORT).show();     //显示得到的储存路径
        File dirFile = new File(storage + "/DrawImagery");         //在storage/emulated/0目录下创建abcd文件夹
        dirFile.mkdir();                                        //创建一个新文件
        File file = new File(dirFile, System.currentTimeMillis() + ".jpg");
        FileOutputStream fileOS = new FileOutputStream(file);                           //创建一个文件输出流对象
        cacheBitmap.compress(Bitmap.CompressFormat.PNG,100,fileOS);             //将绘图内容压缩为PNG格式输出到输出流对象中，PNG为透明图片
        fileOS.flush();                 //将缓冲区中的数据全部写出道输出流中
        fileOS.close();                 //关闭文件输出流对象
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {//设置触摸事件，手指按下进行记录，手指抬起停止记录
        mouseCurrentX = event.getX();
        mouseCurrentY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mouse_begin = true;
                break;
            case MotionEvent.ACTION_UP:
                mouse_begin = false;
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFFFFFFF);       //设置背景颜色
        mouse_tail(canvas);//绘制鼠标拖尾

        sigmoid_curve(cacheCanvas);//绘制曲线

        Paint bmpPaint = new Paint();       //采用默认设置创建一个画笔
        canvas.drawBitmap(cacheBitmap,0,0,bmpPaint);        //绘制cacheBitmap
        canvas.save();                   //保存canvas的状态
        canvas.restore();                   //回复canvas之前的保存的状态，放置保存后对canvas执行的操作对后续的绘制有影响


    }

    private void sigmoid_curve(Canvas canvas) {
//        if (mouse_begin == true) {
            mPaintSigmoid.reset();

            int[] color = Bezier.rainBow((float)time % 300 / 300); //画笔同一颜色随时间渐变
            mPaintSigmoid.setColor(Color.argb(128, color[0], color[1], color[2]));

            float x1 = 0,x2 = 0,y1 = 0,y2 = 0;
            for(float i = -5; i <= 5; i += 0.05){
                float percent = (float)0.5 - Math.abs(i / 10);
                x1 = i;
                y1 = Sigmoid.sigmoid(i, Math.sin((double)time % 50 / 50 * 2 * Math.PI));
                x1 *= 50 + mouse_down_time * 3;
                y1 *= 50 + mouse_down_time * 3;
                if(i + 5 < 0.01){
                    x2 = x1;
                    y2 = y1;
                    continue;
                }
                mPaintSigmoid.setStrokeWidth(percent * 10);
                canvas.drawLine(x1 + mouseCurrentX, y1 + mouseCurrentY, x2 + mouseCurrentX, y2 + mouseCurrentY, mPaintSigmoid);
                x2 = x1;
                y2 = y1;
            }

//        }

    }

    private void mouse_tail(Canvas canvas) {
//        int[] color = Bezier.rainBow((float)time % 300 / 300); //画笔同一颜色随时间渐变
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
            int[] color = Bezier.rainBow((time + percent * 300) % 300 / 300); //画笔不同颜色随时间渐变
            mPaintMouse.setColor(Color.argb(255, color[0], color[1], color[2]));
            mPaintMouse.setStrokeWidth(percent * 20);
            canvas.drawLine(x1, y1, x2, y2, mPaintMouse);
            x2 = x1;
            y2 = y1;
            if (i == size - 1) canvas.drawLine(x1, y1, mouseCurrentX, mouseCurrentY, mPaintMouse);//连接最后一段与鼠标
        }
        int[] color = Bezier.rainBow((float)time % 300 / 300); //画笔不同颜色随时间渐变
        mPaintMouse.setColor(Color.argb(255, color[0], color[1], color[2]));
        canvas.drawCircle(mouseCurrentX, mouseCurrentY, 10, mPaintMouse);//绘制鼠标中心

    }

}
