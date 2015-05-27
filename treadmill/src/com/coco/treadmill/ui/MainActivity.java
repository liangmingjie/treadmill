package com.coco.treadmill.ui;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import butterknife.InjectView;
import butterknife.OnClick;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.lbsapi.panoramaview.PanoramaViewListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine.WalkingStep;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.coco.treadmill.R;
import com.coco.treadmill.base.BaseActivity;
import com.coco.treadmill.base.BaseApplication;
import com.coco.treadmill.utils.L;
import com.coco.treadmill.utils.T;

public class MainActivity extends BaseActivity implements
		BaiduMap.OnMapClickListener, OnGetRoutePlanResultListener,
		PanoramaViewListener, OnGetGeoCoderResultListener {

	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	private LocationMode mCurrentMode;
	BitmapDescriptor mCurrentMarker;

	MapView mMapView;

	BaiduMap mBaiduMap;
	private String mCity;
	// UI相关

	boolean isFirstLoc = true;// 是否首次定位
	private LatLng mStartPt;
	private LatLng mEndPt;

	private Marker mStartMarker;
	private Marker mEndMarker;

	@InjectView(R.id.customicon)
	Button customicon;
	private GeoCoder mGeoSearch;

	// 搜索相关
	RoutePlanSearch mSearch = null;
	RouteLine route = null;
	private Marker mRunPosMarker;
	private ArrayList<LatLng> myAllStep = new ArrayList();
	private BitmapDescriptor bd;
	private WalkingRouteOverlay mRouteOverlay;
	private float mTotalDistance;
	private float mAvgSpeed = 5;
	private int mAlyTime = 0;
	private float mAlyDistance = 0;
	AsyncTask<Void, LatLng, String> task;

	private PanoramaView mPanoView;
	private boolean isPanoViewMap = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 先初始化BMapManager
		BaseApplication app = (BaseApplication) this.getApplication();
		if (app.mBMapManager == null) {
			app.mBMapManager = new BMapManager(app);

			app.mBMapManager.init(new BaseApplication.MyGeneralListener());
		}
		setContentView(R.layout.activity_main);
		initMapDetail();

	}

	private void initMapDetail() {
		// TODO Auto-generated method stub
		mCurrentMode = LocationMode.NORMAL;
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15.0f));

		mCurrentMarker = BitmapDescriptorFactory
				.fromResource(R.drawable.icon_geo);
		mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(null,
				false, null));
		// 定位初始化
		mLocClient = new LocationClient(MainActivity.this);
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setIsNeedAddress(true);
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(1000);
		mLocClient.setLocOption(option);
		mLocClient.start();
		mBaiduMap.setOnMapClickListener(this);

		// 初始化搜索模块，注册事件监听
		mSearch = RoutePlanSearch.newInstance();
		mSearch.setOnGetRoutePlanResultListener(this);
		mGeoSearch = GeoCoder.newInstance();
		mGeoSearch.setOnGetGeoCodeResultListener(this);
		bd = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);

		// 全景
		mPanoView = (PanoramaView) findViewById(R.id.panorama);
		mPanoView.setPanoramaImageLevel(4);
		mPanoView.setPanoramaViewListener(this);
		mPanoView.setShowTopoLink(false);
		mPanoView.setZoomGestureEnabled(false);
		mPanoView.setRotateGestureEnabled(false);
		mPanoView.setPanorama("0100220000130817164838355J5");
	

	}

	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;
			// mCity = location.getCity();
			// T.showLong(mContext, mCity);
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			if (isFirstLoc) {
				isFirstLoc = false;
				mStartPt = new LatLng(location.getLatitude(),
						location.getLongitude());

				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				mBaiduMap.animateMapStatus(u);
				setStartPoint(ll);
			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	private void setStartPoint(LatLng latlng) {

		// TODO Auto-generated method stub
		if (mStartMarker != null) {
			mStartMarker.remove();
			mStartMarker = null;
		}

		MarkerOptions markeroptions = (new MarkerOptions())
				.position(latlng)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_geo))
				.zIndex(16).draggable(false);

		mStartMarker = (Marker) mBaiduMap.addOverlay(markeroptions);
		mPanoView.setPanorama(latlng.longitude, latlng.latitude);
	}

	private void setEndPoint(LatLng latlng)

	{

		if (mEndMarker != null) {
			mEndMarker.remove();
			mEndMarker = null;
		}

		MarkerOptions markeroptions = (new MarkerOptions()).position(latlng)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_en))
				.zIndex(16).draggable(true);

		mEndMarker = (Marker) mBaiduMap.addOverlay(markeroptions);

	}

	@OnClick(R.id.customicon)
	public void onClickCustomicon(Button button) {

		startSearchRoute();

	}

	@OnClick(R.id.muni)
	public void onClickmMoni(Button button) {

		startMuniRun();

	}

	private void startMuniRun() {
		// TODO Auto-generated method stub
		task = new AsyncTask<Void, LatLng, String>() {

			@Override
			protected String doInBackground(Void... params) {

				for (int i = 0; i < myAllStep.size(); i++) {

					try {
						publishProgress(myAllStep.get(i));

						if (isPanoViewMap) {
							Thread.sleep(3000);
						} else {
							Thread.sleep(300);
						}

					} catch (Exception e) {
						// TODO: handle exception
					}

				}
				publishProgress(mEndPt);
				return null;
			}

			@Override
			protected void onProgressUpdate(LatLng... values) {
				// drawStep(myAllStep.get(values[0]));
				if (isPanoViewMap) {
					mPanoView.setPanorama(values[0].longitude,
							values[0].latitude);
				} else {
					drawStep(values[0]);
				}

			}

			@Override
			protected void onPreExecute() {

			}

			@Override
			protected void onPostExecute(String result) {

				super.onPostExecute(result);

			}

		};
		task.execute();
	}

	private void startSearchRoute() {
		// TODO Auto-generated method stub

		if (mEndPt == null || mStartPt == null)
			return;
		if (mRouteOverlay != null)
			mRouteOverlay.removeFromMap();

		// PlanNode stNode = PlanNode.withCityNameAndPlaceName("北京", "龙泽");
		// PlanNode enNode = PlanNode.withCityNameAndPlaceName("北京", "西单");
		PlanNode stNode = null;
		PlanNode enNode = null;
		if (mStartPt == null) {
			// String s1 = mStartName;
			// stNode = null;
			// if (s1 != null) {
			// boolean flag1 = mStartName.equals("");
			// stNode = null;
			// if (!flag1)
			// stNode = PlanNode.withCityNameAndPlaceName(mCity,
			// mStartName);
			// }
		} else {
			stNode = PlanNode.withLocation(mStartPt);
		}
		if (mEndPt == null) {
			// String s = mEndName;
			// plannode1 = null;
			// if (s != null) {
			// boolean flag = mEndName.equals("");
			// plannode1 = null;
			// if (!flag)
			// plannode1 = PlanNode.withCityNameAndPlaceName(mCity,
			// mEndName);
			// }
		} else {
			enNode = PlanNode.withLocation(mEndPt);
		}
		if (mStartPt != null && mEndPt != null) {
			mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode)
					.to(enNode));
		} else {
			T.showLong(mContext, "请设置起始点");
		}

	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		mPanoView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		mPanoView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {

		mPanoView.destroy();
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}

	@Override
	public void onGetGeoCodeResult(GeoCodeResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoadPanoramBegin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoadPanoramaEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoadPanoramaError() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetTransitRouteResult(TransitRouteResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult result) {
		// TODO Auto-generated method stub
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
					.show();
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			// 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
			// result.getSuggestAddrInfo()
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			mBaiduMap.clear();
			route = result.getRouteLines().get(0);
			mRouteOverlay = new MyWalkingRouteOverlay(mBaiduMap);
			mBaiduMap.setOnMarkerClickListener(mRouteOverlay);
			// routeOverlay = overlay;
			mRouteOverlay.setData(result.getRouteLines().get(0));
			mRouteOverlay.addToMap();
			mRouteOverlay.zoomToSpan();
			mTotalDistance = route.getDistance();
			LatLng currentLanLang = null;
			myAllStep.clear();

			for (int i = 0; i < route.getAllStep().size(); i++) {

				// Iterator iter = ((WalkingStep) route.getAllStep().get(i))
				// .getWayPoints().iterator();

				List<LatLng> LatLngList = ((WalkingStep) route.getAllStep()
						.get(i)).getWayPoints();
				for (LatLng latlng1 : LatLngList) {

					if (currentLanLang != null) {
						double d1 = getDistance(latlng1, currentLanLang);
						if (d1 > 9.000000000000001E-005D) {
							int q = (int) (d1 / 9.000000000000001E-005D);
							double d2latitude = (latlng1.latitude - currentLanLang.latitude)
									/ q;
							double d3longitude = (latlng1.longitude - currentLanLang.longitude)
									/ q;
							for (int k = 1; k < q; k++) {
								LatLng LatLng2 = new LatLng(
										currentLanLang.latitude + d2latitude
												* k, currentLanLang.longitude
												+ d3longitude * k);
								myAllStep.add(LatLng2);
							}
						} else {
							myAllStep.add(latlng1);
						}
						currentLanLang = latlng1;
					} else {
						myAllStep.add(latlng1);
						currentLanLang = latlng1;
					}

					// if (latlng1 != null) {
					// if (currentLanLang != null) {
					//
					// double d1 = GeoUtil.getDistanceOfMeter(latlng1,
					// currentLanLang);
					//
					// if (d1 > 1) {
					// int q = (int) (d1) / 1;
					// double d2latitude = (latlng1.latitude -
					// currentLanLang.latitude)
					// / q;
					// double d3longitude = (latlng1.longitude -
					// currentLanLang.latitude)
					// / q;
					// for (int k = 0; k < q; k++) {
					// LatLng LatLng2 = new LatLng(
					// currentLanLang.latitude
					// + d2latitude * k,
					// currentLanLang.longitude
					// + d3longitude * k);
					// myAllStep.add(LatLng2);
					// }
					// } else {
					// myAllStep.add(latlng1);
					// }
					//
					// currentLanLang = latlng1;
					//
					// } else {
					// myAllStep.add(latlng1);
					// currentLanLang = latlng1;
					// }
					// }

				}

				// List<LatLng> lv = ((WalkingStep) route.getAllStep().get(i))
				// .getWayPoints();
				// // 每隔一米 添加一个坐标
				// if (lv.size() > 1) {
				// for (int j = 0; j < lv.size() - 1; j++) {
				//
				// if (lv.get(j + 1) != null && lv.get(j) != null) {
				// myAllStep.add(lv.get(j));
				// double d1 = GeoUtil.getDistanceOfMeter(
				// lv.get(j + 1), lv.get(j));
				// if (d1 > 1) {
				// int q = (int) (d1);
				// double d2latitude = (lv.get(j + 1).latitude - lv
				// .get(j).latitude) / q;
				// double d3longitude = (lv.get(j + 1).longitude - lv
				// .get(j).longitude) / q;
				// for (int k = 1; k < q; k++) {
				// LatLng LatLng2 = new LatLng(
				// lv.get(j).latitude + d2latitude * k,
				// lv.get(j).longitude + d3longitude
				// * k);
				// if (LatLng2 != null) {
				// myAllStep.add(LatLng2);
				// }
				// }
				// }
				// }
				// }
				// try {
				// if (lv.get(lv.size()) != null) {
				// myAllStep.add(lv.get(lv.size()));
				// }
				// } catch (Exception e) {
				// // TODO: handle exception
				// e.printStackTrace();
				// }
				//
				// } else if (lv.size() == 1) {
				// myAllStep.add(lv.get(0));
				// }
				// //////////////////////反编译出来的一个算法，组装一下看不懂 大概是这样

				// ///////////////////////////////
			}

			// double d = 0;
			// for (int i = 0; i < myAllStep.size() - 1; i++) {
			//
			// d += GeoUtil.getDistanceOfMeter(myAllStep.get(i),
			// myAllStep.get(i + 1));
			// //
			// L.i(getDistance(myAllStep.get(i),myAllStep.get(i+1))+"对比"+GeoUtil.getDistanceOfMeter(myAllStep.get(i),
			// // myAllStep.get(i+1)));
			// }

			// drawStep(myAllStep.get(myAllStep.size() - 10));

		}

	}

	public double getDistance(LatLng latlng, LatLng latlng1) {
		double d = latlng.latitude;
		double d1 = latlng1.latitude;
		double d2 = latlng.longitude;
		double d3 = latlng1.longitude;
		return Math.sqrt(Math.pow(d - d1, 2D) + Math.pow(d2 - d3, 2D));
	}

	public void drawStep(LatLng latlng) {
		if (mRunPosMarker != null) {
			mRunPosMarker.remove();
			mRunPosMarker = null;
		}
		MarkerOptions markeroptions = (new MarkerOptions()).position(latlng)
				.icon(bd).zIndex(16).draggable(false);
		mRunPosMarker = (Marker) mBaiduMap.addOverlay(markeroptions);

	}

	private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

		public MyWalkingRouteOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		@Override
		public BitmapDescriptor getStartMarker() {
			// if (useDefaultIcon) {
			return BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
			// }
			// return null;
		}

		@Override
		public BitmapDescriptor getTerminalMarker() {
			// if (useDefaultIcon) {
			return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
			// }
			// return null;
		}
	}

	/**
	 * 设置底图显示模式
	 * 
	 * @param view
	 */
	public void setMapMode(View view) {
		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
		case R.id.normal:
			if (checked) {
				isPanoViewMap = false;
				showhidePanoView(false);

				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			}

			break;
		case R.id.statellite:
			if (checked) {
				isPanoViewMap = false;
				showhidePanoView(false);

				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			}

			break;
		case R.id.statepanorama:
			if (checked) {
				isPanoViewMap = true;
				showhidePanoView(true);

				if (mStartPt != null) {
					mPanoView
							.setPanorama(mStartPt.longitude, mStartPt.latitude);
				} else {

				}
			}

			break;
		}
	}

	private void showhidePanoView(boolean flag) {
		android.widget.LinearLayout.LayoutParams layoutparams = (android.widget.LinearLayout.LayoutParams) mMapView
				.getLayoutParams();
		android.widget.LinearLayout.LayoutParams layoutparams1 = (android.widget.LinearLayout.LayoutParams) mPanoView
				.getLayoutParams();
		if (flag) {
			layoutparams.weight = 1.0F;
			layoutparams1.weight = 999F;
		} else {
			layoutparams.weight = 999F;
			layoutparams1.weight = 1.0F;
		}
		mMapView.setLayoutParams(layoutparams);
		mPanoView.setLayoutParams(layoutparams1);
	}

	@Override
	public void onMapClick(LatLng point) {
		// TODO Auto-generated method stub
		mBaiduMap.hideInfoWindow();
		mEndPt = point;
		LatLng ptCenter = point;
		// 反Geo搜索

		/*
		 * mSearch.geocode(new GeoCodeOption() .city("北京")
		 * .address("海淀区上地十街10号"));
		 */
		/*
		 * GeoPoint ptCenter = new GeoPoint((int) (point.latitude), (int)
		 * (point.longitude)); // 反Geo搜索 mSearch.reverseGeocode(ptCenter);
		 */
		setEndPoint(point);
	}

	@Override
	public boolean onMapPoiClick(MapPoi arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
