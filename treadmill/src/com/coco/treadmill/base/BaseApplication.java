package com.coco.treadmill.base;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.MKGeneralListener;

import com.baidu.mapapi.SDKInitializer;

public class BaseApplication extends Application {
	private static Context mContext;
	public static float sScale;
	public static int sWidthDp;
	public static int sWidthPix;
	public BMapManager mBMapManager = null;

	private static BaseApplication mInstance;

	public static BaseApplication getInstance() {
		return mInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SDKInitializer.initialize(this);
		initEngineManager(this);
		mContext = this;
		mInstance = this;
		sScale = getResources().getDisplayMetrics().density;
		sWidthPix = getResources().getDisplayMetrics().widthPixels;
		sWidthDp = (int) (sWidthPix / sScale);
		initImageLoader(this);

	}

	public void initEngineManager(Context context) {
		if (mBMapManager == null) {
			mBMapManager = new BMapManager(context);
		}

		if (!mBMapManager.init(new MyGeneralListener())) {
			Toast.makeText(
					BaseApplication.getInstance().getApplicationContext(),
					"BMapManager  初始化错误!", Toast.LENGTH_LONG).show();
		}
	}

	// 常用事件监听，用来处理通常的网络错误，授权验证错误等
	public static class MyGeneralListener implements MKGeneralListener {

		@Override
		public void onGetPermissionState(int iError) {
			// 非零值表示key验证未通过
			if (iError != 0) {
//				// 授权Key错误：
//				Toast.makeText(
//						BaseApplication.getInstance().getApplicationContext(),
//						"请在AndoridManifest.xml中输入正确的授权Key,并检查您的网络连接是否正常！error: "
//								+ iError, Toast.LENGTH_LONG).show();
			} else {
//				Toast.makeText(
//						BaseApplication.getInstance().getApplicationContext(),
//						"key认证成功", Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * initial ImageLoader
	 * 
	 * @param context
	 */
	private static void initImageLoader(Context context) {
		// ImageLoaderConfiguration config = new
		// ImageLoaderConfiguration.Builder(
		// context).threadPriority(Thread.NORM_PRIORITY - 2)
		// .denyCacheImageMultipleSizesInMemory()
		// .diskCacheFileNameGenerator(new Md5FileNameGenerator())
		// .diskCacheSize(50 * 1024 * 1024)
		// // 50 Mb
		// .diskCacheFileCount(300)
		// // .imageDownloader(new MyImageDownloader(context))
		// .tasksProcessingOrder(QueueProcessingType.LIFO)
		// // .writeDebugLogs() // Remove for release app
		// .diskCacheExtraOptions(sWidthPix / 3, sWidthPix / 3, null)
		// .build();
		//
		// ImageLoader.getInstance().init(config);

	}

}
