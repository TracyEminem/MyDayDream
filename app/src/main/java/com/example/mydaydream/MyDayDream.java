package com.example.mydaydream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.mydaydream.utils.DepthPageTransformer;
import com.example.mydaydream.utils.FixedSpeedScroller;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyDayDream extends DreamService {
    private static final String TAG = "MyDayDream";

    private int currentItem = 0;
    String[] fileName = new String[0];
    AssetManager manager;
    private String[] imageUrls = null;

    AssetManager assetManager = null;
    ImageView imageView = null;
    Bitmap oldbmp = null;
    int time = 20 * 1000;// 切图时间
    Boolean flag = false;// 是否加载的是默认图片

    private static final String PATH_4_PICTURES = Environment.getExternalStorageDirectory().getPath() +"/Pictures/";
    //private static final String PATH_4_PICTURES = "/data/local/tmp/image/";
    //private static final String PATH_4_PICTURES = "/mnt/sacard/Pictures/";
    //private static final String PATH_4_PICTURES = "/data/media/0/Pictures";
    //private static final String PATH_4_PICTURES = "/storage/sdcard0/Pictures/";
    //private static final String PATH_4_PICTURES = "/storage/sdcard0/Pictures/";

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setContentView(R.layout.my_day_dream);
        Log.d(TAG, "start!");

        init();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        manager = null;
        imageUrls = null;
        fileName = null;

        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @SuppressWarnings("finally")
    public String[] getImageName(Context context, String loc) {
        String prefix = "";
        try {

            if (loc.equalsIgnoreCase("screen")) {
                manager = context.getAssets();
                fileName = manager.list(loc);
                // prefix = "assets://screen/";
                prefix = "screen/";
            } else {
                File f = new File(loc);
                fileName = f.list();
                prefix = PATH_4_PICTURES;//"/data/local/tmp/image/";
                f = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileName != null) {
                for (int i = 0; i < fileName.length; i++) {
                    fileName[i] = prefix + fileName[i];
                    Log.d(TAG, "prefix = " + prefix + ", fileName[" + i + "] = " + fileName[i]);
                }
            }
            return fileName;
        }
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
    }

    public void init() {
        imageUrls = getImageName(this, PATH_4_PICTURES);//"/data/local/tmp/image/");

        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.length; i++) {
            }
        }

        if (imageUrls == null || imageUrls.length == 0) {
            flag = true;
            imageUrls = getImageName(this, "screen");
            for (int i = 0; i < imageUrls.length; i++) {
            }
        }
        fileName = null;

        imageView = (ImageView) findViewById(R.id.iv_pb);
        assetManager = this.getAssets();

        setimage();
        handler.postDelayed(task, time);

    }

    public void setimage() {

        // 把历史的ImageView 图片对象（imageView)释放
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getBackground();
        if (bitmapDrawable != null) {
            Bitmap hisBitmap = bitmapDrawable.getBitmap();
            if (hisBitmap.isRecycled() == false) {
                hisBitmap.recycle();
            }
        }
        // 设置自动清理的参数使得Bitmap占用的内存不再是1280*720*4而是1280*720*2 一下子就减了一半

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;


        // 切换图片
        try {
            Bitmap bmp = null;

            if (flag) {
                if (imageUrls.length == 0) {
                    Log.d("hefang", "length = 0!!!!!!!!");
                    return;
                }
                InputStream in = assetManager.open(imageUrls[currentItem % imageUrls.length]);

                bmp = BitmapFactory.decodeStream(in, null, options);
                //要是内存还是占用太大就就直接自己改分辨率吧，会降低清晰度，但是大大减小内存
                // bmp=decodeSampledBitmapFromResource(in, 768,432);
            } else {
                Log.d(TAG, "uri:" + imageUrls[currentItem % imageUrls.length]);

                InputStream in = new FileInputStream(imageUrls[currentItem % imageUrls.length]);

                bmp = BitmapFactory.decodeStream(in, null, options);
                // bmp=decodeSampledBitmapFromResource(in, 960,540);
            }
            if (oldbmp == null) {
                oldbmp = bmp;
            }

            Drawable[] layers = new Drawable[2];
            layers[0] = new BitmapDrawable(oldbmp);
            layers[1] = new BitmapDrawable(bmp);
            oldbmp = bmp;

            if (currentItem == 0) {
                imageView.setImageBitmap(bmp);
            } else {
                TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(400);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler();

    private Runnable task = new Runnable() {
        public void run() {
            // TODOAuto-generated method stub
            handler.postDelayed(this, time);// 设置延迟时间
            // 需要执行的代码
            currentItem++;
            setimage();
        }
    };
	/*public static Bitmap decodeSampledBitmapFromResource(InputStream is, int reqWidth, int reqHeight) {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, options);
		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeStream(is, null, options);
	}
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		Log.d(TAG, "height:" + height + "   width" + width);
		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		Log.d(TAG, "height  inSampleSize:" + inSampleSize);
		return inSampleSize;
	}*/

}
