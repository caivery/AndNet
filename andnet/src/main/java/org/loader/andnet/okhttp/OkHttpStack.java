package org.loader.andnet.okhttp;

import android.os.Handler;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.loader.andnet.net.AbsHttpStack;
import org.loader.andnet.net.Net;
import org.loader.andnet.net.RequestParams;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by qibin on 2015/11/29.
 */
public class OkHttpStack<T> extends AbsHttpStack<T> {

    private OkHttpClient mClient;
    private Handler mHandler;

    public OkHttpStack() {
        mClient = new OkHttpClient();
        mHandler = new Handler();
    }

    @Override
    public void get(final String url, final Net.Parser<T> parser,
                    final Net.Callback<T> callback,
                    final Object tag) {
        final Request request = new Request.Builder()
                .url(url).build();
        call(request, parser, callback, tag);
    }

    @Override
    public void post(final String url, final RequestParams params,
                     final Net.Parser<T> parser,
                     final Net.Callback<T> callback,
                     final Object tag) {
        MultipartBuilder builder = createRequestBody(params);
        Request request = new Request.Builder()
                .url(url).post(builder.build()).build();
        call(request, parser, callback, tag);
    }

    @Override
    public void post(final String url, final String json,
                     final Net.Parser<T> parser,
                     final Net.Callback<T> callback, final Object tag) {
        Request request = new Request.Builder()
                .url(url).post(createJsonBody(json)).build();
        call(request, parser, callback, tag);
    }

    @Override
    public void put(final String url, final RequestParams params,
                    final Net.Parser<T> parser,
                    final Net.Callback<T> callback,
                    final Object tag) {
        MultipartBuilder builder = createRequestBody(params);
        Request request = new Request.Builder()
                .url(url).put(builder.build()).build();
        call(request, parser, callback, tag);
    }

    @Override
    public void put(final String url, final String json,
                    final Net.Parser<T> parser,
                    final Net.Callback<T> callback, final Object tag) {
        Request request = new Request.Builder()
                .url(url).put(createJsonBody(json)).build();
        call(request, parser, callback, tag);
    }

    @Override
    public void delete(final String url, final Net.Parser<T> parser,
                       final Net.Callback<T> callback,
                       final Object tag) {
        final Request request = new Request.Builder()
                .url(url).delete().build();
        call(request, parser, callback, tag);
    }

    private RequestBody createJsonBody(String json) {
        RequestBody body = RequestBody.create(MediaType
                .parse("application/json;charset=utf-8"), json);
        return body;
    }

    private MultipartBuilder createRequestBody(RequestParams params) {
        MultipartBuilder builder = new MultipartBuilder()
                .type(MultipartBuilder.FORM);

        LinkedHashMap<String, String> map = params.get();
        for(Iterator<String> iterator=map.keySet().iterator();iterator.hasNext();) {
            String key = iterator.next();
            builder.addPart(Headers.of("Content-Disposition",
                    "form-data; name=\""+ key +"\""),
                    RequestBody.create(null, map.get(key)));
        }

        LinkedHashMap<String, File> files = params.files();
        for(Iterator<String> iterator=files.keySet().iterator();iterator.hasNext();) {
            String key = iterator.next();
            File file = files.get(key);
            builder.addPart(Headers.of("Content-Disposition",
                    "form-data; name=\"" + key + "\";filename=\""+ file.getName() +"\""),
                    RequestBody.create(MediaType.parse("application/octet-stream"), file));
        }

        return builder;
    }

    private void call(Request request, final Net.Parser<T> parser,
                      final Net.Callback<T> callback,
                      final Object tag) {
        Request.Builder builder = request.newBuilder();
        builder.tag(tag);
        LinkedHashMap<String, String> map = headers();
        if(map != null && !map.isEmpty()) {
            for(Iterator<String> iterator=map.keySet().iterator();iterator.hasNext();) {
                String key = iterator.next();
                builder.addHeader(key, map.get(key));
            }
            request = builder.build();
        }

        OkHttpClient client = mClient.clone();
        client.setConnectTimeout(20, TimeUnit.SECONDS);

        final Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, final IOException e) {
                final String msg = e.getMessage();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onError(callback, msg);
                    }
                });
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                final String resp = response.body().string();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onNetResponse(parser, callback, resp);
                    }
                });
            }
        });
    }

    public LinkedHashMap<String, String> headers() {
        // TODO add your custom headers here!
        return null;
    }

    @Override
    public void cancel(final Object tag) {
        mClient.cancel(tag);
    }
}
