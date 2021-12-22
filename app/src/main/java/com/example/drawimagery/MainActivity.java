package com.example.drawimagery;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PaletteView.Callback,Handler.Callback {

//    private MainCanvas mPaletteView;
//    private ProgressDialog mSaveProgressDlg;
//    private static final int MSG_SAVE_SUCCESS = 1;
//    private static final int MSG_SAVE_FAILED = 2;
//    private Handler mHandler;

    private View mUndoView;
    private View mRedoView;
    private View mPenView;
    private View mEraserView;
    private View mClearView;
    private PaletteView mPaletteView;
    private ProgressDialog mSaveProgressDlg;
    private static final int MSG_SAVE_SUCCESS = 1;
    private static final int MSG_SAVE_FAILED = 2;
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

        mPaletteView = (PaletteView) findViewById(R.id.palette);
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

    private static String saveImage(Bitmap bmp, int quality) {
        if (bmp == null) {
            return null;
        }
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
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
                        Bitmap bm = mPaletteView.buildBitmap();
                        String savedFile = saveImage(bm, 100);
                        if (savedFile != null) {
                            scanFile(MainActivity.this, savedFile);
                            filePath = savedFile;
                            mHandler.obtainMessage(MSG_SAVE_SUCCESS).sendToTarget();
                        }else{
                            mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
                        }
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
                //色环输入监听
                color_picker.setOnColorBackListener((a, r, g, b) -> {
                    color_choose_a = a;
                    color_choose_r = r;
                    color_choose_g = g;
                    color_choose_b = b;
                    color_choose_seekBar_a.setProgress(color_choose_a);
                    color_choose_seekBar_r.setProgress(color_choose_r);
                    color_choose_seekBar_g.setProgress(color_choose_g);
                    color_choose_seekBar_b.setProgress(color_choose_b);
                    preInputColor.setText(intToHex(color_choose_a) + intToHex(color_choose_r) + intToHex(color_choose_g) + intToHex(color_choose_b));
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
            case R.id.points_change:
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
        }
        return true;
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
                mPaletteView.clear();
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