package com.example.drawimagery;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PaletteView.Callback,Handler.Callback {

//    private MainCanvas mPaletteView;
//    private ProgressDialog mSaveProgressDlg;
//    private static final int MSG_SAVE_SUCCESS = 1;
//    private static final int MSG_SAVE_FAILED = 2;
//    private Handler mHandler;
    private int backgroundID = 0;

    private View mUndoView;
    private View mRedoView;
    private View mPenView;
    private View mEraserView;
    private View mClearView;
    private PaletteView mPaletteView;
    private ProgressDialog mSaveProgressDlg;
    private static final int MSG_SAVE_SUCCESS = 1;
    private static final int MSG_SAVE_FAILED = 2;
    private static final int MSG_SHARE_SUCCESS = 3;
    private static final int MSG_SHARE_FAILED = 4;
    private Handler mHandler;

    private int color_choose_a;
    private int color_choose_r;
    private int color_choose_g;
    private int color_choose_b;

    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPaletteView = findViewById(R.id.palette);
        mPaletteView.setCallback(this);

        int color_choose = mPaletteView.getPenColor();
        color_choose_a = Color.alpha(color_choose);
        color_choose_r = Color.red(color_choose);
        color_choose_g = Color.green(color_choose);
        color_choose_b = Color.blue(color_choose);

        mUndoView = findViewById(R.id.undo);
        mRedoView = findViewById(R.id.redo);
        mPenView = findViewById(R.id.pen);
        mPenView.setSelected(true);
        mEraserView = findViewById(R.id.eraser);
        mClearView = findViewById(R.id.clear);

        mUndoView.setOnClickListener(this);
        mRedoView.setOnClickListener(this);
        mPenView.setOnClickListener(this);
        mEraserView.setOnClickListener(this);
        mClearView.setOnClickListener(this);

        mUndoView.setEnabled(false);
        mRedoView.setEnabled(false);

        mHandler = new Handler(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //没有权限则申请权限
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_SHARE_SUCCESS);
        mHandler.removeMessages(MSG_SHARE_FAILED);
        mHandler.removeMessages(MSG_SAVE_FAILED);
        mHandler.removeMessages(MSG_SAVE_SUCCESS);
    }

    private void initSaveProgressDlg(){
        mSaveProgressDlg = new ProgressDialog(this);
        mSaveProgressDlg.setMessage("正在保存,请稍候...");
        mSaveProgressDlg.setCancelable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_SHARE_FAILED:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this,"分享失败",Toast.LENGTH_SHORT).show();
                break;
            case MSG_SHARE_SUCCESS:
                mSaveProgressDlg.dismiss();
                break;
            case MSG_SAVE_FAILED:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();
                break;
            case MSG_SAVE_SUCCESS:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this,"画板已保存于" + filePath,Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private static void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }

    private String saveImage(Bitmap bmp, int quality) {
        if (bmp == null) {
            return null;
        }
        File appDir = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (appDir == null) {
            return null;
        }
        String fileName = "绘意" + System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /** * 从Assets中读取图片 */
    private Bitmap getImageFromAssetsFile(String fileName){
        Bitmap image = null;
        AssetManager am = getResources().getAssets();
        try {
            InputStream is=am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();    }
        return image;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                if(mSaveProgressDlg==null){
                    initSaveProgressDlg();
                }
                mSaveProgressDlg.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int paints = mPaletteView.paintsAmount;
                        mPaletteView.paintsAmount = 0;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                mPaletteView.invalidate();
                                Bitmap bm = mPaletteView.buildBitmap();
                                mPaletteView.paintsAmount = paints;
                                String savedFile = saveImage(bm, 100);
                                if (savedFile != null) {
                                    scanFile(MainActivity.this, savedFile);
                                    filePath = savedFile;
                                    mHandler.obtainMessage(MSG_SAVE_SUCCESS).sendToTarget();
                                }else{
                                    mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
                                }
                            }
                        });
                    }
                }).start();
                break;
            case R.id.color_change:
                AlertDialog.Builder color_change_builder = new AlertDialog.Builder(this);
                AlertDialog color_change_dialog = color_change_builder.create();

                View color_change_dialogView = View.inflate(this, R.layout.alert_background, null);
                ColorPickerView color_picker = (ColorPickerView) color_change_dialogView.findViewById(R.id.color_picker);
                EditText preInputColor = (EditText)  color_change_dialogView.findViewById(R.id.preInputColor);
                ImageView preViewColor = (ImageView) color_change_dialogView.findViewById(R.id.preViewColor);
                SeekBar color_choose_seekBar_a = (SeekBar) color_change_dialogView.findViewById(R.id.color_choose_a);
                SeekBar color_choose_seekBar_r = (SeekBar) color_change_dialogView.findViewById(R.id.color_choose_r);
                SeekBar color_choose_seekBar_g = (SeekBar) color_change_dialogView.findViewById(R.id.color_choose_g);
                SeekBar color_choose_seekBar_b = (SeekBar) color_change_dialogView.findViewById(R.id.color_choose_b);
                preInputColor.setText(intToHex(color_choose_a) + intToHex(color_choose_r) + intToHex(color_choose_g) + intToHex(color_choose_b));
                color_picker.setOnColorBackListener(new ColorPickerView.OnColorBackListener() {//色环输入监听
                    @Override
                    public void onColorBack(int a, int r, int g, int b) {
                        color_choose_a = a;
                        color_choose_r = r;
                        color_choose_g = g;
                        color_choose_b = b;
                        color_choose_seekBar_a.setProgress(color_choose_a);
                        color_choose_seekBar_r.setProgress(color_choose_r);
                        color_choose_seekBar_g.setProgress(color_choose_g);
                        color_choose_seekBar_b.setProgress(color_choose_b);
                        mPenView.setSelected(true);
                        mEraserView.setSelected(false);
                        mPaletteView.setMode(PaletteView.Mode.DRAW);
                        mPaletteView.isDazzleColor = false;
                        preInputColor.setText(intToHex(color_choose_a) + intToHex(color_choose_r) + intToHex(color_choose_g) + intToHex(color_choose_b));
                    }
                });
                preInputColor.addTextChangedListener(new TextWatcher() {//颜色输入监听
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        try {
                            int color_input = Color.parseColor("#" + editable.toString());
                            color_choose_a = Color.alpha(color_input);
                            color_choose_r = Color.red(color_input);
                            color_choose_g = Color.green(color_input);
                            color_choose_b = Color.blue(color_input);
                            color_choose_seekBar_a.setProgress(color_choose_a);
                            color_choose_seekBar_r.setProgress(color_choose_r);
                            color_choose_seekBar_g.setProgress(color_choose_g);
                            color_choose_seekBar_b.setProgress(color_choose_b);
                            mPenView.setSelected(true);
                            mEraserView.setSelected(false);
                            mPaletteView.setMode(PaletteView.Mode.DRAW);
                            mPaletteView.isDazzleColor = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                preViewColor.setBackgroundColor(Color.argb(color_choose_a,color_choose_r,color_choose_g,color_choose_b));
                color_choose_seekBar_a.setProgress(color_choose_a);
                color_choose_seekBar_r.setProgress(color_choose_r);
                color_choose_seekBar_g.setProgress(color_choose_g);
                color_choose_seekBar_b.setProgress(color_choose_b);
                SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {//颜色条拖动监听
                    /*拖动条停止拖动时调用 */
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "拖动停止");
                    }
                    /*拖动条开始拖动时调用*/
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "开始拖动");
                    }
                    /* 拖动条进度改变时调用*/
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        switch(seekBar.getId()) {
                            case R.id.color_choose_a:
                                color_choose_a = progress;
                                break;
                            case R.id.color_choose_r:
                                color_choose_r = progress;
                                break;
                            case R.id.color_choose_g:
                                color_choose_g = progress;
                                break;
                            case R.id.color_choose_b:
                                color_choose_b = progress;
                                break;
                        }
                        mPaletteView.isDazzleColor = false;
                        preViewColor.setBackgroundColor(Color.argb(color_choose_a,color_choose_r,color_choose_g,color_choose_b));
                        mPaletteView.setPenColor(Color.argb(color_choose_a,color_choose_r,color_choose_g,color_choose_b));
                        preInputColor.setText(intToHex(color_choose_a) + intToHex(color_choose_r) + intToHex(color_choose_g) + intToHex(color_choose_b));
                    }
                };
                color_choose_seekBar_a.setOnSeekBarChangeListener(listener);
                color_choose_seekBar_r.setOnSeekBarChangeListener(listener);
                color_choose_seekBar_g.setOnSeekBarChangeListener(listener);
                color_choose_seekBar_b.setOnSeekBarChangeListener(listener);

                color_change_dialog.setView(color_change_dialogView);
                color_change_dialog.show();

                break;
            case R.id.paint_change:
                AlertDialog.Builder points_change_builder = new AlertDialog.Builder(this);
                AlertDialog points_change_dialog = points_change_builder.create();

                View points_change_dialogView = View.inflate(this, R.layout.alert_draw_points, null);

                TextView eraser_width_view = points_change_dialogView.findViewById(R.id.eraser_width_view);//橡皮粗细
                eraser_width_view.setText(String.valueOf(mPaletteView.getEraserSize()));
                SeekBar eraser_width_choose = (SeekBar) points_change_dialogView.findViewById(R.id.eraser_width_choose);
                eraser_width_choose.setProgress(mPaletteView.getEraserSize());
                eraser_width_choose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {//拖动监听
                    /*拖动条停止拖动时调用 */
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "拖动停止");
                    }
                    /*拖动条开始拖动时调用*/
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "开始拖动");
                    }
                    /* 拖动条进度改变时调用*/
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mPaletteView.setEraserSize(progress);
                        eraser_width_view.setText(String.valueOf(progress));
                    }
                });

                TextView paint_width_view = points_change_dialogView.findViewById(R.id.paint_width_view);//画笔粗细
                paint_width_view.setText(String.valueOf(mPaletteView.getPenSize()));
                SeekBar paint_width_choose = (SeekBar) points_change_dialogView.findViewById(R.id.paint_width_choose);
                paint_width_choose.setProgress(mPaletteView.getPenSize());
                paint_width_choose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {//拖动监听
                    /*拖动条停止拖动时调用 */
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "拖动停止");
                    }
                    /*拖动条开始拖动时调用*/
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "开始拖动");
                    }
                    /* 拖动条进度改变时调用*/
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mPaletteView.setPenRawSize(progress);
                        paint_width_view.setText(String.valueOf(progress));
                    }
                });

                TextView draw_points_view = points_change_dialogView.findViewById(R.id.draw_points_view);//画笔数
                draw_points_view.setText(String.valueOf(mPaletteView.paintsAmount));
                SeekBar draw_points_choose = (SeekBar) points_change_dialogView.findViewById(R.id.draw_points_choose);
                draw_points_choose.setProgress(mPaletteView.paintsAmount);
                draw_points_choose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {//拖动监听
                    /*拖动条停止拖动时调用 */
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "拖动停止");
                    }
                    /*拖动条开始拖动时调用*/
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.i("SeekBarActivity", "开始拖动");
                    }
                    /* 拖动条进度改变时调用*/
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mPaletteView.paintsAmount = progress;
                        draw_points_view.setText(String.valueOf(progress));
                    }
                });

                Switch mirror_switch = points_change_dialogView.findViewById(R.id.mirror_switch);//镜像
                mirror_switch.setChecked(mPaletteView.isMirrorDraw);
                mirror_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        mPaletteView.isMirrorDraw = b;
                    }
                });

                Switch dazzle_switch = points_change_dialogView.findViewById(R.id.dazzle_switch);//镜像
                dazzle_switch.setChecked(mPaletteView.isDazzleColor);
                dazzle_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        mPaletteView.isDazzleColor = b;
                        mPaletteView.setPenColor(Color.argb(color_choose_a,color_choose_r,color_choose_g,color_choose_b));
                    }
                });

                points_change_dialog.setView(points_change_dialogView);
                points_change_dialog.show();
                break;
            case R.id.share:
                if(mSaveProgressDlg==null){
                    initSaveProgressDlg();
                }
                mSaveProgressDlg.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int paints = mPaletteView.paintsAmount;
                        mPaletteView.paintsAmount = 0;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                mPaletteView.invalidate();
                                Bitmap bm = mPaletteView.buildBitmap();
                                mPaletteView.paintsAmount = paints;
                                String savedFile = saveImage(bm, 100);
                                if (savedFile != null) {
                                    scanFile(MainActivity.this, savedFile);
                                    filePath = savedFile;
                                    /** * 分享图片 */
                                    Intent share_intent = new Intent();
                                    share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
                                    share_intent.setType("image/*");  //设置分享内容的类型
                                    share_intent.putExtra(Intent.EXTRA_STREAM, filePath);
                                    //创建分享的Dialog
                                    share_intent.putExtra(Intent.EXTRA_SUBJECT, "分享");//添加分享内容标题
                                    share_intent = Intent.createChooser(share_intent, "分享");
                                    startActivity(share_intent);
                                    mHandler.obtainMessage(MSG_SHARE_SUCCESS).sendToTarget();
                                }else{
                                    mHandler.obtainMessage(MSG_SHARE_FAILED).sendToTarget();
                                }
                            }
                        });
                    }
                }).start();
                break;
            case R.id.about:
                final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
                //normalDialog.setIcon(R.drawable.buttom_yello);
                normalDialog.setTitle("关于绘意");
                normalDialog.setMessage("图案艺术：刘睿\n音乐甄选：吴越\n组长QQ：1104052058（刘）");
                normalDialog.setNegativeButton("返回", (dialog, which) -> dialog.dismiss());
                normalDialog.show();
                break;
            case R.id.background_change:
                if (ping()) {
                    new DownloadImageTask().execute("https://api.ixiaowai.cn/gqapi/gqapi.php");
                } else {
                    backgroundID++;
                    backgroundID %= 3;
                    switch (backgroundID) {
                        case 0:
                            mPaletteView.setBackground(getResources().getDrawable(R.drawable.bg1));
                            break;
                        case 1:
                            mPaletteView.setBackground(getResources().getDrawable(R.drawable.bg2));
                            break;
                        case 2:
                            mPaletteView.setBackground(getResources().getDrawable(R.drawable.bg3));
                            break;
                    }
                }
                break;
            case R.id.play:
                    mPaletteView.isRotatePath = true;
//                    if (mPaletteView.mMode == PaletteView.Mode.DRAW || mPaletteView.mCanEraser) mPaletteView.saveDrawingPath();
                    for (PaletteView.DrawingInfo drawingInfo : mPaletteView.mDrawingList) {
                            drawingInfo.degree = (float) (Math.random() - 0.5F) * 2;
                        }
                break;
            case R.id.pause:
                    mPaletteView.isRotatePath = false;
                break;
        }
        return true;
    }

    public static final boolean ping() {

        String result = null;
        try {
            String ip = "www.baidu.com";// ping 的地址，可以换成任何一种可靠的外网
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);// ping网址3次
            // ping的状态
            int status = p.waitFor();
            if (status == 0) {
                result = "success";
                return true;
            } else {
                result = "failed";
            }
        } catch (IOException e) {
            result = "IOException";
        } catch (InterruptedException e) {
            result = "InterruptedException";
        } finally {
            Log.d("----result---", "result = " + result);
        }
        return false;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Drawable> {
        protected Drawable doInBackground(String... urls) {
            return loadImageFromNetwork(urls[0]);
        }

        protected void onPostExecute(Drawable result) {
            mPaletteView.setBackground(result);
        }
    }

    private Drawable loadImageFromNetwork(String imageUrl)
    {
        Drawable drawable = null;
        try {
            // 可以在这里通过文件名来判断，是否本地有此图片
            drawable = Drawable.createFromStream(new URL(imageUrl).openStream(), "image.jpg");
        } catch (IOException e) {
            Log.d("test", e.getMessage());
        }
        if (drawable == null) {
            Log.d("test", "null drawable");
        } else {
            Log.d("test", "not null drawable");
        }

        return drawable ;
    }

    @Override
    public void onUndoRedoStatusChanged() {
        mUndoView.setEnabled(mPaletteView.canUndo());
        mRedoView.setEnabled(mPaletteView.canRedo());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.undo:
                mPaletteView.undo();
                break;
            case R.id.redo:
                mPaletteView.redo();
                break;
            case R.id.pen:
                v.setSelected(true);
                mEraserView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.DRAW);
                break;
            case R.id.eraser:
                v.setSelected(true);
                mPenView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.ERASER);
                break;
            case R.id.clear:
                final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
                //normalDialog.setIcon(R.drawable.buttom_yello);
                normalDialog.setTitle("清屏");
                normalDialog.setMessage("您确定要清空屏幕吗?");
                normalDialog.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                normalDialog.setPositiveButton("确定", (dialog, which) -> {
                    mPaletteView.clear();
                    Toast.makeText(MainActivity.this,"清屏成功",Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
                normalDialog.show();
                break;
        }
    }

    private static String intToHex(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char []b = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        while(n != 0){
            sb = sb.append(b[n % 16]);
            n = n / 16;
        }
        a = sb.reverse().toString();
        if(a == "")a = "00";
        return a;
    }

}