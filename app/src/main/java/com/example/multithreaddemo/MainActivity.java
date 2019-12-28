package com.example.multithreaddemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private static final String DOWNLOAD_URL = "https://c-ssl.duitang.com/uploads/item/201208/30/20120830173930_PBfJE.thumb.700_0.jpeg";
  private Button thread, yibu, handler, asnctask, other;
  private ProgressBar progressBar;
  private TextView textView;
  private ImageView imageView;
  private CalculateThread calculateThread;
  private MyAsyncTack myAsyncTack;


  private MyHandler myHandler = new MyHandler(this);
  private MyUIHandler uiHandler = new MyUIHandler(this);

  private static final int START_NUM = 100;
  private static final int ADDING_NUM = 101;
  private static final int ENDING_NUM = 102;
  private static final int CANCEL_NUM = 103;

  private static final int MSG_SHOW_PROGRESS = 11;
  private static final int MES_SHOW_IMAGE = 12;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    progressBar = findViewById(R.id.pb_bar);
    progressBar.setOnClickListener(this);
    textView = findViewById(R.id.tv_time);
    textView.setOnClickListener(this);

    thread = findViewById(R.id.btn_thread);
    thread.setOnClickListener(this);

    yibu = findViewById(R.id.btn_yibu);
    yibu.setOnClickListener(this);

    handler = findViewById(R.id.btn_handler);
    handler.setOnClickListener(this);

    asnctask = findViewById(R.id.btn_task);
    asnctask.setOnClickListener(this);

    other = findViewById(R.id.btn_other);
    other.setOnClickListener(this);

    imageView = findViewById(R.id.img);
    imageView.setOnClickListener(this);


  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_thread:
        calculateThread = new CalculateThread();
        calculateThread.start();
        break;
      case R.id.btn_handler:
        new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
        break;
      case R.id.btn_yibu:
        myAsyncTack = new MyAsyncTack(this);
        myAsyncTack.execute(100);
        break;
      case R.id.btn_task:
        new DownloadImage(this).execute(DOWNLOAD_URL);
        break;
      case R.id.btn_other:
        runOnUiThread(new Runnable() {
          @Override public void run() {
            other.setText("runOnUiThread方式更新");
            textView.setText("runOnUiThread方式更新TextView的内容");
          }
        });
        break;

    }

  }

  static class MyHandler extends Handler {
    private WeakReference<Activity> ref;

    public MyHandler(Activity activity) {
      this.ref = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      MainActivity activity = (MainActivity) ref.get();
      if (activity == null) {
        return;
      }
      switch (msg.what) {
        case START_NUM:
          activity.progressBar.setVisibility(View.VISIBLE);
          break;
        case ADDING_NUM:
          activity.progressBar.setProgress(msg.arg1);
          activity.textView.setText("计算完成" + msg.arg1 + "%");
          break;
        case ENDING_NUM:
          activity.progressBar.setVisibility(View.GONE);
          activity.textView.setText("计算已完成，结果为" + msg.arg1);
          activity.myHandler.removeCallbacks(activity.calculateThread);
          break;
        case CANCEL_NUM:
          activity.progressBar.setProgress(0);
          activity.progressBar.setVisibility(View.GONE);
          activity.textView.setText("计算已取消");
          break;
      }

    }
  }

  static class MyUIHandler extends Handler {
    private WeakReference<Activity> ref;

    public MyUIHandler(Activity activity) {
      this.ref = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      MainActivity activity = (MainActivity) ref.get();
      if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
        removeCallbacksAndMessages(null);
        return;
      }
      switch (msg.what) {
        case MSG_SHOW_PROGRESS:
          activity.progressBar.setVisibility(View.VISIBLE);
          break;
        case MES_SHOW_IMAGE:
          activity.progressBar.setVisibility(View.GONE);
          activity.imageView.setImageBitmap((Bitmap) msg.obj);
          break;
      }

    }
  }

  private class DownloadImageFetcher implements Runnable{
    private String imgUrl;
    public DownloadImageFetcher(String strUrl){
      this.imgUrl = strUrl;
    }

    @Override public void run() {
      InputStream in = null;
      uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();
      try {
        URL url = new URL(imgUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        in = connection.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(in);

        Message msg = uiHandler.obtainMessage();
        msg.what = MES_SHOW_IMAGE;
        msg.obj = bitmap;
        uiHandler.sendMessage(msg);
      } catch (IOException e) {
        e.printStackTrace();
      }finally {
        if (in != null){
          try {
            in.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

    }
  }

  class CalculateThread extends Thread {
    @Override
    public void run() {
      int result = 0;
      boolean isCancel = true;
      myHandler.sendEmptyMessage(START_NUM);

      for (int i = 0; i <= 100; i++) {
        try {
          Thread.sleep(100);
          result += i;
        } catch (InterruptedException e) {
          e.printStackTrace();
          isCancel = true;
          break;
        }
        if (i % 5 == 0) {
          Message msg = Message.obtain();
          msg.what = ADDING_NUM;
          msg.arg1 = i;
          myHandler.sendMessage(msg);
        }
      }
      if (!isCancel) {
        Message msg = myHandler.obtainMessage();
        msg.what = ENDING_NUM;
        msg.arg1 = result;
        myHandler.sendMessage(msg);
      }
    }
  }
  static class MyAsyncTack extends AsyncTask<Integer,Integer,Integer>{
    private WeakReference<AppCompatActivity> ref;
    public  MyAsyncTack(AppCompatActivity activity){
      this.ref = new WeakReference<>(activity);
    }

    @Override
    protected Integer doInBackground(Integer... params) {
      int sleep = params[0];
      int result = 0;
      for (int i = 0; i<101;i++){
        try {
          Thread.sleep(sleep);
          result += i;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (i % 5 == 0){
          publishProgress(i);
        }
        if (isCancelled()){
          break;
        }
      }
      return result;
    }


    @Override protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
      MainActivity activity = (MainActivity) this.ref.get();
      activity.progressBar.setVisibility(values[0]);
      activity.textView.setText("计算已完成" + values[0] + "%");
    }

    @Override protected void onPostExecute(Integer result) {
      super.onPostExecute(result);
      MainActivity activity = (MainActivity) this.ref.get();
      activity.textView.setText("已计算完成，结果为：" + result);
      activity.progressBar.setVisibility(View.GONE);
    }

    @Override protected void onCancelled() {
      super.onCancelled();
      MainActivity activity = (MainActivity) this.ref.get();
      activity.textView.setText("计算已取消");
      activity.progressBar.setProgress(0);
      activity.progressBar.setVisibility(View.GONE);

    }
  }

  private class DownloadImage extends AsyncTask<String,Bitmap,Bitmap> {
    private WeakReference<AppCompatActivity> ref;

    public DownloadImage(AppCompatActivity activity) {
      this.ref = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      MainActivity activity = (MainActivity) this.ref.get();
      activity.progressBar.setVisibility(View.VISIBLE);

    }

    @Override
    protected Bitmap doInBackground(String... params) {
      String url = params[0];
      return downloadImage(url);
    }



    private Bitmap downloadImage(String strUrl) {
      InputStream stream = null;
      Bitmap bitmap = null;
      MainActivity activity = (MainActivity) this.ref.get();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        URL url = new URL(strUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int totalLen = connection.getContentLength();
        if (totalLen == 0) {
          activity.progressBar.setProgress(0);
        }
        if (connection.getResponseCode() == 200) {
          stream = connection.getInputStream();
          int len = -1;
          int progress = 0;
          byte[] tmps = new byte[1024];
          while ((len = stream.read(tmps)) != -1) {
            progress += len;
            activity.progressBar.setProgress(progress);
            bos.write(tmps, 0, len);
          }
          bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());
        }

      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (stream != null) {
          try {
            stream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      return bitmap;

    }
    @Override
    protected void onPostExecute(Bitmap bitmap) {
      super.onPostExecute(bitmap);
      MainActivity activity = (MainActivity) this.ref.get();
      if (bitmap != null) ;
      activity.imageView.setImageBitmap(bitmap);
    }



  }

}

