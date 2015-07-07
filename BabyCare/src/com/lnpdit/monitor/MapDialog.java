package com.lnpdit.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import com.company.Demo.DhPlayerActivity;
import com.lnpdit.babycare.R;
import com.lnpdit.util.TipItemizedOverlay;
import com.mapbar.android.maps.GeoPoint;
import com.mapbar.android.maps.ItemizedOverlay;
import com.mapbar.android.maps.MapActivity;
import com.mapbar.android.maps.MapController;
import com.mapbar.android.maps.MapView;
import com.mapbar.android.maps.MyLocationOverlay;
import com.mapbar.android.maps.OverlayItem;
import com.mapbar.android.maps.Projection;

/**
 * @Module com.mapbar.android.maps.demo.MapViewDemo
 * @description ��ͼ��ʾDemo
 * @author
 * @version 1.0
 * @created Dec 13, 2011
 */
public class MapDialog extends MapActivity implements OnClickListener {

	MapView mMapView;
	MapController mMapController;
	GeoPoint point;
	MyLocationOverlay me = null;
	private Button btnBack;
	TipItemizedOverlay mTipItemizedOverlay;
	Drawable mTipBackground;
	/**
	 * ����������View
	 */
	View popView;

	private TextView map_back;
	// ���ݼ���
	ArrayList deviceList;
	private String serverIp = "";// ��ȡ�������Ĺ���ƽ̨IP
	private String socketIp = "";// ��ȡ��������ת��IP
	private String userId;
	private String ifPtz;
	private String ifRecord;
	private String ifSnap;

	@Override
	/**
	 *��ʾ��ͼ�������������ſؼ�������MapController���Ƶ�ͼ�����ĵ㼰Zoom����
	 */
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		super.onCreate(savedInstanceState);

		// ��ֹ����
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.mapviewdemo);

		mMapView = (MapView) findViewById(R.id.mapViewDemo);
		mMapView.setBuiltInZoomControls(false); // �����������õ����ſؼ�
		mMapController = mMapView.getController(); // �õ�mMapView�Ŀ���Ȩ,�����������ƺ�����ƽ�ƺ�����
		point = new GeoPoint((int) (41.727126 * 1E6), (int) (123.476027 * 1E6)); // �ø����ľ�γ�ȹ���һ��GeoPoint����λ��΢��
		// (�� *
		// 1E6)
		mMapController.setCenter(point); // ���õ�ͼ���ĵ�
		mMapController.setZoom(10); // ���õ�ͼzoom����

		mTipBackground = getResources().getDrawable(
				R.drawable.tip_pointer_button_top);
		mTipItemizedOverlay = new TipItemizedOverlay(this, mTipBackground);
		mMapView.getOverlays().add(mTipItemizedOverlay);

		map_back = (TextView) findViewById(R.id.map_back);
		map_back.setOnClickListener(this);

		drawPoint();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void drawPoint() {
		Bundle bundle = this.getIntent().getExtras();
		deviceList = bundle.getParcelableArrayList("videoList");
		serverIp = bundle.getString("serverIp");
		socketIp = bundle.getString("socketIp");
		userId = this.getIntent().getStringExtra("userId").toString();

		Drawable marker = getResources().getDrawable(R.drawable.marker); // �õ���Ҫ���ڵ�ͼ�ϵ���Դ
		marker.setBounds(0, 0, marker.getIntrinsicWidth(),
				marker.getIntrinsicHeight()); // Ϊmaker����λ�úͱ߽�
		mMapView.getOverlays().add(new OverItemT(marker, MapDialog.this));
	}

	class OverItemT extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> GeoList = new ArrayList<OverlayItem>();
		private Drawable marker;

		public OverItemT(Drawable marker, Context context) {
			super(boundCenterBottom(marker));

			this.marker = marker;
			GeoPoint p1 = null;
			int i = 0;
			for (i = 0; i < deviceList.size(); i++) {
				HashMap<String, Object> map = (HashMap<String, Object>) deviceList
						.get(i);
				if (map.get("stayLine").toString().equals("1")) {
					String Name = map.get("chName").toString();
					Double Longitude = Double.parseDouble(map.get("longitude")
							.toString());
					Double Latitude = Double.parseDouble(map.get("latitude")
							.toString());

					// �ø����ľ�γ�ȹ���GeoPoint����λ��΢�� (�� * 1E6)
					p1 = new GeoPoint((int) (Latitude * 1E6),
							(int) (Longitude * 1E6));

					// ����OverlayItem��������������Ϊ��item��λ�ã������ı�������Ƭ��
					GeoList.add(new OverlayItem(p1, Name, ""));
				}
			}

			// String Name = "�������";
			// Double Longitude = 123.4902729;
			// Double Latitude = 41.727126;
			//
			// // �ø����ľ�γ�ȹ���GeoPoint����λ��΢�� (�� * 1E6)
			// p1 = new GeoPoint((int) (Latitude * 1E6), (int) (Longitude *
			// 1E6));
			//
			// // ����OverlayItem��������������Ϊ��item��λ�ã������ı�������Ƭ��
			// GeoList.add(new OverlayItem(p1, Name, ""));
			//
			// Name = "�׶�԰����";
			// Longitude = 123.405893;
			// Latitude = 41.76713;
			//
			// // �ø����ľ�γ�ȹ���GeoPoint����λ��΢�� (�� * 1E6)
			// p1 = new GeoPoint((int) (Latitude * 1E6), (int) (Longitude *
			// 1E6));
			//
			// // ����OverlayItem��������������Ϊ��item��λ�ã������ı�������Ƭ��
			// GeoList.add(new OverlayItem(p1, Name, ""));

			if (p1 != null) {
				mMapController.setCenter(p1); // ���õ�ͼ���ĵ�
			}
			populate(); // createItem(int)��������item��һ���������ݣ��ڵ�����������ǰ�����ȵ����������
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

			// Projection�ӿ�������Ļ���ص�����ϵͳ�͵�����澭γ�ȵ�����ϵͳ֮��ı任
			Projection projection = mapView.getProjection();
			for (int index = size() - 1; index >= 0; index--) { // ����GeoList
				OverlayItem overLayItem = getItem(index); // �õ�����������item

				String title = overLayItem.getTitle();
				// �Ѿ�γ�ȱ任�������MapView���Ͻǵ���Ļ��������
				Point point = projection.toPixels(overLayItem.getPoint(), null);
				Paint paintText = new Paint();
				paintText.setColor(Color.BLACK);
				paintText.setTextSize(15);
				// �����ı�
				// canvas.drawText(title, point.x, point.y - 25, paintText);
			}
			super.draw(canvas, mapView, shadow);
			// ����һ��durable�߽磬ʹ�ã�0��0�������drawable�ײ����һ�����ĵ�һ������
			boundCenterBottom(marker);
		}

		@Override
		protected OverlayItem createItem(int i) {
			// TODO Auto-generated method stub
			return GeoList.get(i);
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return GeoList.size();
		}

		@Override
		// ��������¼�
		protected boolean onTap(int i) {
			setFocus(GeoList.get(i));
			mTipItemizedOverlay.clean();
			mTipItemizedOverlay.addOverlay(GeoList.get(i));
			return true;
		}
	}

	public final static String TAG = "VLC/VideoPlayerActivity";

	private LibVLC mLibVLC = null;

	public void StartView(Context context, OverlayItem item) {

		try {

			Bundle bundle = ((Activity) context).getIntent().getExtras();
			deviceList = bundle.getParcelableArrayList("videoList");
			serverIp = bundle.getString("serverIp");
			socketIp = bundle.getString("socketIp");
			ifPtz = bundle.getString("ifPtz");
			ifSnap = bundle.getString("ifSnap");
			ifRecord = bundle.getString("ifRecord");
			userId = "0";
			try {
				userId = bundle.getString("userId");
			} catch (Exception e) {
				// TODO: handle exception
			}

			int i = 0;
			for (i = 0; i < deviceList.size(); i++) {
				HashMap<String, Object> map = (HashMap<String, Object>) deviceList
						.get(i);
				String deviceNameString = map.get("chName").toString();
				final String rtsp = map.get("rtsp").toString();
				final String devType = map.get("devType").toString();

				String titleString = item.getTitle();

				if (deviceNameString.equals(titleString)) {

					if (devType.equals("DHZL")) {
						Intent intent = new Intent(context,
								DhPlayerActivity.class);
						// onNewIntent(intent);
						intent.putExtra("devName", map.get("devName")
								.toString());
						intent.putExtra("chName", map.get("chName").toString());
						intent.putExtra("devId", map.get("devId").toString());
						intent.putExtra("ip", map.get("ip").toString());
						intent.putExtra("port", map.get("port").toString());
						intent.putExtra("devUserName", map.get("devUserName")
								.toString());
						intent.putExtra("devPassWord", map.get("devPassWord")
								.toString());
						intent.putExtra("chNo", map.get("chNo").toString());
						intent.putExtra("listCount", map.get("listCount")
								.toString());
						intent.putExtra("listNo", map.get("listNo").toString());
						intent.putExtra("width", map.get("width").toString());
						intent.putExtra("height", map.get("height").toString());
						intent.putExtra("adapterId", map.get("adapterId")
								.toString());
						if (ifPtz.equals("1")) { // ���Ȩ����������̨������� ʵ������ж�
							intent.putExtra("ptz", map.get("ptz").toString());
							intent.putExtra("zoom", map.get("zoom").toString());
							intent.putExtra("talk", map.get("talk").toString());
						} else {// ���Ȩ������û����̨����0
							intent.putExtra("ptz", "0");
							intent.putExtra("zoom", "0");
							intent.putExtra("talk", "0");
						}
						intent.putExtra("userId", userId);
						intent.putExtra("socketIp", socketIp);
						intent.putExtra("ifRecord", ifRecord);
						intent.putExtra("ifSnap", ifSnap);

						intent.putExtras(bundle);

						context.startActivity(intent);
					} else {

						if (rtsp.equals("0")) {

							Intent intent = new Intent(context, ShowVideo.class);
							// onNewIntent(intent);
							intent.putExtra("devName", map.get("devName")
									.toString());
							intent.putExtra("chName", map.get("chName")
									.toString());
							intent.putExtra("devId", map.get("devId")
									.toString());
							intent.putExtra("ip", map.get("ip").toString());
							intent.putExtra("port", map.get("port").toString());
							intent.putExtra("chNo", map.get("chNo").toString());
							intent.putExtra("listCount", map.get("listCount")
									.toString());
							intent.putExtra("listNo", map.get("listNo")
									.toString());
							intent.putExtra("width", map.get("width")
									.toString());
							intent.putExtra("height", map.get("height")
									.toString());
							intent.putExtra("adapterId", map.get("adapterId")
									.toString());
							if (ifPtz.equals("1")) { // ���Ȩ����������̨������� ʵ������ж�
								intent.putExtra("ptz", map.get("ptz")
										.toString());
								intent.putExtra("zoom", map.get("zoom")
										.toString());
								intent.putExtra("talk", map.get("talk")
										.toString());
							} else {// ���Ȩ������û����̨����0
								intent.putExtra("ptz", "0");
								intent.putExtra("zoom", "0");
								intent.putExtra("talk", "0");
							}
							intent.putExtra("userId", userId);
							intent.putExtra("socketIp", socketIp);
							intent.putExtra("ifRecord", ifRecord);
							intent.putExtra("ifSnap", ifSnap);

							intent.putExtras(bundle);
							context.startActivity(intent);
						} else if (rtsp.equals("1")) {

							Intent intent = new Intent(context,
									VideoPlayerActivity.class);

							try {
								mLibVLC = Util.getLibVlcInstance();
							} catch (LibVlcException e) {
								e.printStackTrace();
							}
							String chNo = map.get("chNo").toString();
							// if (chNo.length() == 1) {
							// chNo = "0" + chNo;
							// }
							int addOneNo = Integer.parseInt(chNo) + 1;
							String rtspUrl = "";
							if (socketIp.contains(".net:")
									|| socketIp.contains(".com:")) {
								rtspUrl = "rtsp://" + socketIp + "/"
										+ map.get("devId").toString() + "_"
										+ Integer.toString(addOneNo);
							} else {
								rtspUrl = "rtsp://" + socketIp + ":554/"
										+ map.get("devId").toString() + "_"
										+ Integer.toString(addOneNo);
							}
							// String rtspUrl =
							// "rtsp://admin:12345@200.20.36.105/h264/ch1/sub/av_stream";
							intent.putExtra("rtspUrl", rtspUrl);
							intent.putExtra("devName", map.get("devName")
									.toString());
							intent.putExtra("chName", map.get("chName")
									.toString());
							intent.putExtra("devId", map.get("devId")
									.toString());
							intent.putExtra("ip", map.get("ip").toString());
							intent.putExtra("port", map.get("port").toString());
							intent.putExtra("chNo", map.get("chNo").toString());
							intent.putExtra("listCount", map.get("listCount")
									.toString());
							intent.putExtra("listNo", map.get("listNo")
									.toString());
							intent.putExtra("width", map.get("width")
									.toString());
							intent.putExtra("height", map.get("height")
									.toString());
							intent.putExtra("adapterId", map.get("adapterId")
									.toString());
							if (ifPtz.equals("1")) { // ���Ȩ����������̨������� ʵ������ж�
								intent.putExtra("ptz", map.get("ptz")
										.toString());
								intent.putExtra("zoom", map.get("zoom")
										.toString());
								intent.putExtra("talk", map.get("talk")
										.toString());
							} else {// ���Ȩ������û����̨����0
								intent.putExtra("ptz", "0");
								intent.putExtra("zoom", "0");
								intent.putExtra("talk", "0");
							}
							intent.putExtra("userId", userId);
							intent.putExtra("socketIp", socketIp);
							intent.putExtra("ifRecord", ifRecord);
							intent.putExtra("ifSnap", ifSnap);

							intent.putExtras(bundle);
							context.startActivity(intent);

						} else if (rtsp.equals("2")) {
							Intent intent = new Intent(context,
									DhPlayerActivity.class);
							// onNewIntent(intent);
							intent.putExtra("devName", map.get("devName")
									.toString());
							intent.putExtra("chName", map.get("chName")
									.toString());
							intent.putExtra("devId", map.get("devId")
									.toString());
							intent.putExtra("ip", map.get("ip").toString());
							intent.putExtra("port", map.get("port").toString());
							intent.putExtra("devUserName",
									map.get("devUserName").toString());
							intent.putExtra("devPassWord",
									map.get("devPassWord").toString());
							intent.putExtra("chNo", map.get("chNo").toString());
							intent.putExtra("listCount", map.get("listCount")
									.toString());
							intent.putExtra("listNo", map.get("listNo")
									.toString());
							intent.putExtra("width", map.get("width")
									.toString());
							intent.putExtra("height", map.get("height")
									.toString());
							intent.putExtra("adapterId", map.get("adapterId")
									.toString());
							if (ifPtz.equals("1")) { // ���Ȩ����������̨������� ʵ������ж�
								intent.putExtra("ptz", map.get("ptz")
										.toString());
								intent.putExtra("zoom", map.get("zoom")
										.toString());
								intent.putExtra("talk", map.get("talk")
										.toString());
							} else {// ���Ȩ������û����̨����0
								intent.putExtra("ptz", "0");
								intent.putExtra("zoom", "0");
								intent.putExtra("talk", "0");
							}
							intent.putExtra("userId", userId);
							intent.putExtra("socketIp", socketIp);
							intent.putExtra("ifRecord", ifRecord);
							intent.putExtra("ifSnap", ifSnap);

							intent.putExtras(bundle);
							context.startActivity(intent);

						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {

		case R.id.map_back:
			this.finish();
			break;
		}
	}

	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			this.finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
