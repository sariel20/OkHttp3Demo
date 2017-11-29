package lc.com.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView textView;

    private String mBaseUrl = "http://192.168.1.115:8080/okhttp/";
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
            .build();

    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.tv);
        iv = (ImageView) findViewById(R.id.iv);
    }

    /**
     * get 请求
     *
     * @param view
     */
    public void doGet(View view) {
        //发起请求  构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(mBaseUrl + "login?username=sariel&password=123").build();
        executeRequest(request);
    }

    /**
     * post请求
     *
     * @param view
     */
    public void doPost(View view) {
        //post构造formbody
        FormBody formBody = new FormBody.Builder()
                .add("username", "sariel")
                .add("password", "123")
                .build();

        //构造Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(mBaseUrl + "login")
                .post(formBody).build();

        executeRequest(request);
    }

    /**
     * post提交json
     *
     * @param view
     */
    public void doPostString(View view) {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("text/plain;chaset=utf-8"), "{username:sariel,password:123}");
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(
                mBaseUrl + "postString").post(requestBody)
                .build();

        executeRequest(request);
    }

    /**
     * post提交文件
     *
     * @param view
     */
    public void doPostFile(View view) {
        if (permissions()) return;

        File file = new File(
                Environment.getExternalStorageDirectory() + "/DCIM/Camera/",
                "IMG_20171119_124131.jpg");
        if (!file.exists()) {
            L.e(file.getAbsolutePath() + "not exist!");
            return;
        }

        //File MediaType 对照表 mime type
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(
                mBaseUrl + "postFile")
                .post(RequestBody.create(MediaType.parse("application/octet-stream")
                        , file))
                .build();

        executeRequest(request);
    }

    /**
     * post提交表单数据
     *
     * @param view
     */
    public void doUpload(View view) {
        if (permissions()) return;

        File file = new File(
                Environment.getExternalStorageDirectory() + "/DCIM/Camera/",
                "IMG_20171119_124131.jpg");
        if (!file.exists()) {
            L.e(file.getAbsolutePath() + "not exist!");
            return;
        }

        MultipartBody.Builder multipartBody = new MultipartBody.Builder();
        RequestBody requestBody =
                multipartBody.setType(MultipartBody.FORM)
                        .addFormDataPart("username", "sariel")
                        .addFormDataPart("password", "123")
                        .addFormDataPart("mPhoto", "head.jpg",
                                RequestBody.create(MediaType.parse("application/octet-stream")
                                        , file))
                        .build();

        CountingRequestBody countingRequestBody = new CountingRequestBody(requestBody, new CountingRequestBody.Listener() {
            @Override
            public void onRequestProgress(long byteWrited, long contentLength) {
                L.e(byteWrited + " / " + contentLength);
            }
        });

        //File MediaType 对照表 mime type
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(
                mBaseUrl + "uploadInfo")
                .post(countingRequestBody)
                .build();

        executeRequest(request);
    }

    /**
     * 下载文件
     *
     * @param view
     */
    public void doDownload(View view) {
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(mBaseUrl + "files/head.jpg").build();
        //将Request封装Call
        Call call = okHttpClient.newCall(request);
        //执行
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //错误回调
                L.e("onFailure" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse");

                final long total = response.body().contentLength();
                long sum = 0L;

                //取得返回值
                InputStream inputStream = response.body().byteStream();
                int len = 0;
                File file = new File(Environment.getExternalStorageDirectory(), "hyman.jpg");
                byte[] buf = new byte[128];
                FileOutputStream fos = new FileOutputStream(file);
                while ((len = inputStream.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    sum += len;
                    L.e(sum + " / " + total);
                    final long finalSum = sum;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(finalSum + " / " + total);
                        }
                    });
                }
                fos.flush();
                fos.close();
                inputStream.close();
            }
        });
    }

    /**
     * 服务器图片显示在imageview
     *
     * @param view
     */
    public void doDownloadImg(View view) {
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(mBaseUrl + "files/head.jpg").build();
        //将Request封装Call
        Call call = okHttpClient.newCall(request);
        //执行
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //错误回调
                L.e("onFailure" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse");
                //取得返回值
                InputStream inputStream = response.body().byteStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private boolean permissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return true;
                }
            }
        }
        return false;
    }


    private void executeRequest(Request request) {
        //将Request封装Call
        Call call = okHttpClient.newCall(request);
        //执行
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //错误回调
                L.e("onFailure" + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                L.e("onResponse");
                //取得返回值
                final String str = response.body().string();
                L.e(str);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(str);
                    }
                });

            }
        });
    }
}
