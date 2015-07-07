package com.company.Demo;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.company.NetSDK.*;
import com.company.PlaySDK.BasicSurfaceView;
import com.company.PlaySDK.IPlaySDK;
import com.lnpdit.babycare.R;
import com.lnpdit.photo.SimpleZoomListener;
import com.lnpdit.photo.ZoomState;
import com.lnpdit.sqllite.ToDoDB;
import com.lnpdit.util.ShakeDetector;
import com.lnpdit.util.ShakeListener;
import com.lnpdit.util.TrafficMonitoring;
import com.lnpdit.util.passwordInterface;

import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnTouchListener;

import android.R.string;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DhPlayerActivity extends passwordInterface implements
		OnClickListener {
	// Button m_btStartRecord;
	// Button m_btStopRecord;
	//
	// Button m_btUp;
	// Button m_btDown;
	// Button m_btRight;
	// Button m_btLeft;
	// Button m_btLUp;
	// Button m_btRUp;
	// Button m_btLDown;
	// Button m_btRDown;
	// Button m_btMore;
	//
	// Spinner m_spSpeed;
	//
	private static final String[] m_strSpeed = { "Speed:1", "Speed:2",
			"Speed:3", "Speed:4", "Speed:5", "Speed:6", "Speed:7", "Speed:8" };
	byte m_bSpeed = 5;

	BasicSurfaceView m_PlayView;

	TestRealDataCallBackEx m_callback = new TestRealDataCallBackEx();

	static int nPort = 1;
	static IPlaySDK playsdk = new IPlaySDK();

	static long m_loginHandle = 0;
	static NET_DEVICEINFO deviceInfo;

	// ԭ�й��ܶ���

	// ��Ƶ���ؼ�ʱ��
	private int timerCount = 0;
	private Timer timer;
	private boolean isWaiting;
	private Thread threadLoading;
	private boolean isAlive = true;

	// ��̨��ť
	private Button upButton;
	private Button downButton;
	private Button leftButton;
	private Button rightButton;
	private Button zoominButton;
	private Button zoomoutButton;
	private Button focusinButton;
	private Button focusoutButton;
	private Button talkButton;
	private Button favorButton;
	String port = "";
	String ptz = "";
	String zoom = "";
	String talk = "";
	private String ifRecord;
	private String ifSnap;
	private String ifFavor = "0";
	private String userId;
	private String webId;
	private String videoString;

	String serverIp = "";// ��ȡ�������Ĺ���ƽ̨IP
	String socketIp = "";// ��ȡ��������ת��IP
	int chCount = 0;
	int chNo = 0;
	int adapterID = 0;
	int listNo = 0;
	int listCount = 0;

	int byteFlag = 5;
	int noPtzFlag = 20;
	int ptzFlag = 5;

	int width; // �˴��趨��ʼ�ֱ���
	int height;
	static int screenHeight;
	static int screenWidth;

	// ���ݿ�
	ToDoDB db;

	ProgressBar progressBar;
	TextView progressTextView;

	// ͼ�����Ų���
	// private Bitmap bitmap;
	private ZoomState mZoomState;
	private SimpleZoomListener mZoomListener;

	String strIp = "";
	String strPort = "";
	String strUser = "";
	String strPassword = "";

	int playType = 1;// ��������

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dhplayer);

		// ��ֹ����
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		progressBar = (ProgressBar) findViewById(R.id.dhloadprocess);
		progressTextView = (TextView) findViewById(R.id.dhprogresstext);

		Display display = getWindowManager().getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();

		getVideoIntent();

		// �ȴ���ʾ��
		loadingFlag = true;
		timer = new Timer();
		isWaiting = true;
		progressTextView.setText("��Ƶ�����У����Ժ�...");
		threadLoading = new Thread(waitThread);
		threadLoading.start();

		DeviceDisConnect disConnect = new DeviceDisConnect();
		INetSDK.Init(disConnect);

		NET_PARAM stNetParam = new NET_PARAM();
		stNetParam.nWaittime = 10000;
		INetSDK.SetNetworkParam(stNetParam);

		// 0x034111000 == 3.41.11000
		int dbVersion = INetSDK.GetSDKVersion();

		m_PlayView = (BasicSurfaceView) findViewById(R.id.view_PlayWindow);
		m_PlayView.Init(nPort);

		if (m_loginHandle != 0) {
			INetSDK.Logout(m_loginHandle);
			m_loginHandle = 0;
		}

		deviceInfo = new NET_DEVICEINFO();
		final Integer error = Integer.valueOf(0);

		strIp = getIntent().getStringExtra("ip");
		strPort = getIntent().getStringExtra("port");
		strUser = getIntent().getStringExtra("devUserName");
		strPassword = getIntent().getStringExtra("devPassWord");

		Thread dhThread = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					Thread.sleep(1500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				int connectTimes = 0;

				while (connectTimes < 5) {
					try {
						if (isAlive == true) {
							m_loginHandle = INetSDK.LoginEx(strIp,
									Integer.parseInt(strPort), strUser,
									strPassword, 20, null, deviceInfo, error);

							System.out.println("attempt:" + (connectTimes + 1));
						}
					} catch (Exception e) {
						// TODO: handle exception
					}

					if (m_loginHandle != 0) {
						// ToolKits.showMessage(this, "Login Success!");

						DeviceReConnect reConnect = new DeviceReConnect();
						INetSDK.SetAutoReconnect(reConnect);

						DeviceSubDisConnect subDisConnect = new DeviceSubDisConnect();
						INetSDK.SetSubconnCallBack(subDisConnect);

						INetSDK.SetConnectTime(500, 3);
						connectTimes = 10;
					} else {
						// ToolKits.showErrorMessage(this,
						// "Login Fail.");
						connectTimes++;
					}
				}
				if (connectTimes != 10 && isAlive == true) {
					handler.post(runnableHidProgress);
					handler.post(connectFailed);
				}

				holder = m_PlayView.getHolder();
				holder.addCallback(new Callback() {

					public void surfaceCreated(SurfaceHolder holder) {
						Log.d("[playsdk]surface", "surfaceCreated");
					}

					public void surfaceChanged(SurfaceHolder holder,
							int format, int width, int height) {
						Log.d("[playsdk]surface", "surfaceChanged");
					}

					public void surfaceDestroyed(SurfaceHolder holder) {
						Log.d("[playsdk]surface", "surfaceDestroyed");
					}
				});
				if (StartRealPlay() == true) {
					m_callback = new TestRealDataCallBackEx();
					if (lRealHandle != 0) {
						// Set Real Data Call Back Object
						INetSDK.SetRealDataCallBackEx(lRealHandle, m_callback,
								1);
					}
				}
			}
		});
		dhThread.start();

		handler.sendEmptyMessageDelayed(0, 1000);
	}

	private void getVideoIntent() {
		// ԭ����Ƶ���Ų�����ȡ
		socketIp = this.getIntent().getStringExtra("socketIp");// ��ȡ���������豸�˿�
		port = this.getIntent().getStringExtra("port");// ��ȡ���������豸�˿�

		userId = this.getIntent().getStringExtra("userId").toString();

		width = Integer.parseInt(this.getIntent().getStringExtra("width"));// ��ȡ����������Ƶ���
		height = Integer.parseInt(this.getIntent().getStringExtra("height"));// ��ȡ����������Ƶ�߶�

		if (width < 705) {
			playType = SDK_RealPlayType.SDK_RType_Realplay_1;
		} else if (width > 704) {
			playType = SDK_RealPlayType.SDK_RType_Realplay;

		}

		ptz = this.getIntent().getStringExtra("ptz");// ��ȡ�Ƿ�֧����̨
		zoom = this.getIntent().getStringExtra("zoom");// ��ȡ�Ƿ�֧����̨
		talk = this.getIntent().getStringExtra("talk");// ��ȡ�Ƿ�֧����̨
		chCount = Integer
				.parseInt(this.getIntent().getStringExtra("listCount"));// ��ȡͨ������
		chNo = Integer.parseInt(this.getIntent().getStringExtra("chNo"));// ��ȡͨ��ID
		adapterID = Integer.parseInt(this.getIntent().getStringExtra(
				"adapterId"));// ��ȡ����ID

		listNo = Integer.parseInt(this.getIntent().getStringExtra("listNo"));// ��ȡ���豸������
		listCount = Integer.parseInt(this.getIntent().getStringExtra(
				"listCount"));// ��ȡ�豸����

		ifRecord = this.getIntent().getStringExtra("ifRecord").toString();
		ifSnap = this.getIntent().getStringExtra("ifSnap").toString();

		videoString = this.getIntent().getStringExtra("devId");

		if (ptz.equals("1")) {
			byteFlag = ptzFlag;
		} else {
			byteFlag = noPtzFlag;
		}

		// ���ݿ�
		db = new ToDoDB(this);
		Cursor cur = db.select_favor();
		while (cur.moveToNext()) {
			int webIndex = cur.getColumnIndex("DATA_WEBID");
			webId = cur.getString(webIndex);
			int userIndex = cur.getColumnIndex("DATA_USERID");
			String webUserId = cur.getString(userIndex);
			int devIndex = cur.getColumnIndex("DATA_DEVID");
			String favorDevId = cur.getString(devIndex);
			int chIndex = cur.getColumnIndex("DATA_CHNO");
			String favorChNo = cur.getString(chIndex);
			if (favorDevId.equals(videoString)
					&& favorChNo.equals(Integer.toString(chNo))
					&& webUserId.equals(userId)) {
				ifFavor = "1";
			}
		}

	}

	// ����Ƶ����Ӱ�ť��Layout
	Runnable runnableDrawWidget = new Runnable() {
		@Override
		public void run() {
			drawWidget();
		}

	};

	// ��ʾprogressBar
	Runnable runnableShowProgress = new Runnable() {
		@Override
		public void run() {
			progressBar.setVisibility(ProgressBar.VISIBLE);
			progressTextView.setVisibility(TextView.VISIBLE);
		}

	};
	// ����progressBar
	Runnable runnableHidProgress = new Runnable() {
		@Override
		public void run() {
			progressBar.setVisibility(ProgressBar.GONE);
			progressTextView.setVisibility(TextView.GONE);
		}

	};

	// ¼��ʼ��ʾ
	Runnable recordWaitting = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(DhPlayerActivity.this, "��ʼ¼��", Toast.LENGTH_LONG)
					.show();
		}

	};

	// ¼�������ʾ
	Runnable recordStop = new Runnable() {
		@Override
		public void run() {
			// Toast.makeText(ShowVideo.this, "¼���ѱ�����" + recordPath,
			// Toast.LENGTH_LONG).show();
			Toast.makeText(DhPlayerActivity.this, "¼���ļ��ѱ���", Toast.LENGTH_LONG)
					.show();
		}

	};

	// �豸����ʧ��
	Runnable connectFailed = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(DhPlayerActivity.this, "�豸����ʧ�ܣ�", Toast.LENGTH_SHORT)
					.show();
			isWaiting = false;
		}

	};

	Boolean status = false;

	// �ȴ���ʾ�����
	private void TimerCount(Timer t) {

		if (isAlive) {
			int PLAYGetCurrentFrameRate = (int) playsdk
					.PLAYGetCurrentFrameRate(1);
			System.out.println("PLAYGetCurrentFrameRate:"
					+ PLAYGetCurrentFrameRate);

			if (m_loginHandle != 0 && PLAYGetCurrentFrameRate != 0) {

				new Thread() {
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						handler.post(runnableHidProgress);
						isWaiting = false;
						loadingFlag = false;

						new Thread() {
							public void run() {
								handler.post(runnableDrawWidget);
							}
						}.start();

						// ��ʼ���ζ�
						// shakeDetector();
					}
				}.start();
				t.cancel();
				handlerTraffic.postDelayed(runnable, 1000);

			} else {
				if (loadingFlag) {
					timerCount++;
					if (timerCount > 240) {
						t.cancel();
						// waitClose();

						handler.post(runnableHidProgress);
						if (isWaiting) {
							try {
								isWaiting = false;
								loadingFlag = false;
								new Thread() {
									public void run() {
										handler.post(runnableWaitting);
									}
								}.start();
								// ��ʼ���ζ�
								// shakeDetector();
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
						}
					}
				} else {
					t.cancel();
				}
			}
		}

	}

	private void TimerOut() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				TimerCount(timer);
			}
		};
		timer.schedule(timerTask, 0, 1000);
	}

	Runnable waitThread = new Runnable() {
		public void run() {
			if (isWaiting) {
				TimerOut();
			}
		}
	};

	// ��ʱ��ʾ
	Runnable runnableWaitting = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(DhPlayerActivity.this, "��������ԭ����ʱ�޷���ȡ��Ƶ�����Ժ����ԣ�",
					Toast.LENGTH_SHORT).show();
		}

	};

	private void drawWidget() {

		try {

			// btnSize.setVisibility(Button.VISIBLE);
			// btnPlayPause.setVisibility(Button.VISIBLE);

			Display display = getWindowManager().getDefaultDisplay();
			final LayoutInflater inflater = LayoutInflater.from(this);
			//
			// // ��ʾ��̨��ͷ
			LinearLayout layout = (LinearLayout) inflater.inflate(
					R.layout.arrow, null).findViewById(R.id.arrowlayout);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					(int) display.getWidth(), (int) display.getHeight(), 200);
			addContentView(layout, params);

			upButton = (Button) findViewById(R.id.btnUp);
			upButton.setOnClickListener(this);
			upButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.uppress);
						// vv.videoUP();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.up);
						// vv.videoUPStop();
					}

					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_UP_CONTROL, (byte) 0,
							m_bSpeed);
				}
			});
			downButton = (Button) findViewById(R.id.btnDown);
			downButton.setOnClickListener(this);
			downButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.downpress);
						// vv.videoDOWN();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.down);
						// vv.videoDOWNStop();
					}

					// return false;
					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_DOWN_CONTROL, (byte) 0,
							m_bSpeed);
				}
			});
			leftButton = (Button) findViewById(R.id.btnLeft);
			leftButton.setOnClickListener(this);
			leftButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.leftpress);
						// vv.videoLEFT();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.left);
						// vv.videoLEFTStop();
					}

					// return false;
					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_LEFT_CONTROL, (byte) 0,
							m_bSpeed);
				}
			});
			rightButton = (Button) findViewById(R.id.btnRight);
			rightButton.setOnClickListener(this);
			rightButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.rightpress);
						// vv.videoRIGHT();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.right);
						// vv.videoRIGHTStop();
					}

					// return false;
					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_RIGHT_CONTROL,
							(byte) 0, m_bSpeed);
				}
			});

			if (ptz.equals("1")) {
				upButton.setVisibility(Button.VISIBLE);
				downButton.setVisibility(Button.VISIBLE);
				leftButton.setVisibility(Button.VISIBLE);
				rightButton.setVisibility(Button.VISIBLE);
			} else if (ptz.equals("0")) {
				upButton.setVisibility(Button.GONE);
				downButton.setVisibility(Button.GONE);
				leftButton.setVisibility(Button.GONE);
				rightButton.setVisibility(Button.GONE);
			}
			//
			// ��ʾ������
			LinearLayout layoutSD = (LinearLayout) inflater.inflate(
					R.layout.bottomsd, null).findViewById(R.id.bottomlayout);
			LinearLayout.LayoutParams paramsSD = new LinearLayout.LayoutParams(
					(int) display.getWidth(), (int) display.getHeight(), 200);
			addContentView(layoutSD, paramsSD);

			// �ϸ�ͨ����ť
			Button lastButton = (Button) findViewById(R.id.last);
			lastButton.setVisibility(Button.GONE);
			lastButton.setOnClickListener(this);
			lastButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.lastpress_icon);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.last_icon);
					}

					return false;
				}
			});

			// �¸�ͨ����ť
			Button nextButton = (Button) findViewById(R.id.next);
			nextButton.setVisibility(Button.GONE);
			nextButton.setOnClickListener(this);
			nextButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.nextpress_icon);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.next_icon);
					}

					return false;
				}
			});

			// ��ͣ���Ű�ť
			Button playButton = (Button) findViewById(R.id.play);
			playButton.setOnClickListener(this);
			playButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					// if (mLibVLC.isPlaying()) {
					// if (event.getAction() == MotionEvent.ACTION_DOWN) {
					// // ����Ϊ����ʱ�ı���ͼƬ
					// v.setBackgroundResource(R.drawable.icon_pausepress);
					// } else if (event.getAction() == MotionEvent.ACTION_UP) {
					// // ��Ϊ̧��ʱ��ͼƬ
					// v.setBackgroundResource(R.drawable.icon_pause);
					// }
					// } else {
					// if (event.getAction() == MotionEvent.ACTION_DOWN) {
					// // ����Ϊ����ʱ�ı���ͼƬ
					// v.setBackgroundResource(R.drawable.playbtn_iconpress);
					// } else if (event.getAction() == MotionEvent.ACTION_UP) {
					// // ��Ϊ̧��ʱ��ͼƬ
					// v.setBackgroundResource(R.drawable.playbtn_icon);
					// }
					// }

					return false;

				}
			});

			// �䱶��
			zoominButton = (Button) findViewById(R.id.zoomin);
			if (zoom.equals("0")) {
				zoominButton.setVisibility(Button.GONE);
			}
			zoominButton.setOnClickListener(this);
			zoominButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.zoom_inpress);
						// vv.videoZoomIn();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.zoom_in);
						// vv.videoZoomInStop();
					}

					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_ZOOM_ADD_CONTROL,
							(byte) 0, m_bSpeed);
				}
			});
			// �䱶��
			zoomoutButton = (Button) findViewById(R.id.zoomout);
			if (zoom.equals("0")) {
				zoomoutButton.setVisibility(Button.GONE);
			}
			zoomoutButton.setOnClickListener(this);
			zoomoutButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.zoom_outpress);
						// vv.videoZoomOut();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.zoom_out);
						// vv.videoZoomOutStop();
					}

					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_ZOOM_DEC_CONTROL,
							(byte) 0, m_bSpeed);
				}
			});
			// �佹��
			focusinButton = (Button) findViewById(R.id.focusin);
			if (zoom.equals("0")) {
				focusinButton.setVisibility(Button.GONE);
			}
			focusinButton.setOnClickListener(this);
			focusinButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.focus_inpress);
						// vv.videoFocusIn();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.focus_in);
						// vv.videoFocusInStop();
					}

					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_FOCUS_ADD_CONTROL,
							(byte) 0, m_bSpeed);
				}
			});
			// �佹��
			focusoutButton = (Button) findViewById(R.id.focusout);
			if (zoom.equals("0")) {
				focusoutButton.setVisibility(Button.GONE);
			}
			focusoutButton.setOnClickListener(this);
			focusoutButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.focus_outpress);
						// vv.videoFocusOut();
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.focus_out);
						// vv.videoFocusOutStop();
					}

					return PTZControl(event, chNo,
							SDK_PTZ_ControlType.SDK_PTZ_FOCUS_DEC_CONTROL,
							(byte) 0, m_bSpeed);
				}
			});

			// ������ť
			Button snapButton = (Button) findViewById(R.id.snap);
			if (ifSnap.equals("0")) {
				snapButton.setVisibility(Button.GONE);
			}
			snapButton.setOnClickListener(this);
			snapButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.snapbtn_iconpress);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.snapbtn_icon);
					}

					return false;
				}
			});

			// ¼��ť
			Button recordButton = (Button) findViewById(R.id.record);
			if (ifRecord.equals("0")) {
				recordButton.setVisibility(Button.GONE);
			}
			recordButton.setOnClickListener(this);
			recordButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						// v.setBackgroundResource(R.drawable.icon_recordpress);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						// v.setBackgroundResource(R.drawable.icon_record);
					}

					return false;
				}
			});

			// �Խ���ť
			talkButton = (Button) findViewById(R.id.talk);
			if (talk.equals("0")) {
				talkButton.setVisibility(Button.GONE);
			}
			talkButton.setOnClickListener(this);
			talkButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						// v.setBackgroundResource(R.drawable.talking);
						// new Thread() {
						// public void run() {
						// handler.post(talkStart);
						// }
						// }.start();

					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						// v.setBackgroundResource(R.drawable.talk);
						// new Thread() {
						// public void run() {
						// handler.post(talkEnd);
						// }
						// }.start();
					}

					return false;
				}
			});
			// �ղؼа�ť
			favorButton = (Button) findViewById(R.id.favor);
			if (ifFavor.equals("1")) {
				favorButton.setBackgroundResource(R.drawable.icon_favorpress);
			} else if (ifFavor.equals("0")) {
				favorButton.setBackgroundResource(R.drawable.icon_favor);
			}
			favorButton.setOnClickListener(this);
			favorButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						// if (ifFavor.equals("1")) {
						// v.setBackgroundResource(R.drawable.icon_favor);
						// } else if (ifFavor.equals("0")) {
						// v.setBackgroundResource(R.drawable.icon_favorpress);
						// }
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						// if (ifFavor.equals("1")) {
						// v.setBackgroundResource(R.drawable.icon_favorpress);
						// } else if (ifFavor.equals("0")) {
						// v.setBackgroundResource(R.drawable.icon_favor);
						// }
					}

					return false;
				}
			});

			// ���ذ�ť
			Button backButton = (Button) findViewById(R.id.bottomback);
			backButton.setOnClickListener(this);
			backButton.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// ����Ϊ����ʱ�ı���ͼƬ
						v.setBackgroundResource(R.drawable.icon_backpress);
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						// ��Ϊ̧��ʱ��ͼƬ
						v.setBackgroundResource(R.drawable.icon_back);
					}

					return false;
				}
			});

		} catch (Exception e) {
			// TODO: handle exception
			if (e != null) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.play:
			if (playBoolean) {
				// mLibVLC.pause();
				playsdk.PLAYStop(nPort);
				// playsdk.PLAYPause(nPort, (short) 0);
				v.setBackgroundResource(R.drawable.playbtn_icon);
			} else {
				playsdk.PLAYPlay(nPort);
				v.setBackgroundResource(R.drawable.icon_pause);
			}

			playBoolean = playOrPause(playBoolean);
			break;
		// ץ�İ�ť
		case R.id.snap:
			if (playBoolean) {
				String strFolder = checkSnapPath();
				// takeSnapShot(strFolder, screenWidth, screenHeight);
				SimpleDateFormat formatter = new SimpleDateFormat(
						"yyyyMMdd-HHmmss");
				Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
				String str = strFolder + "Snap" + formatter.format(curDate)
						+ ".jpg";

				int result = playsdk.PLAYCatchResizePic(nPort, str, 1920, 1080,
						1);
				// int result = playsdk.PLAYCatchPicEx(nPort, str, 4);
				if (result == 1) {

					Toast.makeText(this, "ץ�ĳɹ���", Toast.LENGTH_SHORT).show();
				} else if (result == 1) {

					Toast.makeText(this, "ץ��ʧ�ܣ������ԣ�", Toast.LENGTH_SHORT)
							.show();
				}
			} else {
				Toast.makeText(this, "��Ƶ��ֹͣ���޷�ץ�ģ�", Toast.LENGTH_SHORT).show();
			}
			break;
		// ¼��ť
		case R.id.record:

			if (playBoolean) {
				if (!recordBoolean) {

					SimpleDateFormat formatter = new SimpleDateFormat(
							"yyyy-MM-dd-HHmmss");
					Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��

					String recordPath = checkRecordPath() + "vlc-record-"
							+ formatter.format(curDate) + ".dav";
					// mLibVLC.videoRecordStart(recordPath);

					// int result = playsdk.PLAYStartDataRecord(nPort,
					// recordPath,
					// 2);
					// if (result == 1) {
					// handler.post(recordWaitting);
					// v.setBackgroundResource(R.drawable.icon_recordpress);
					// }
					StartRecord(recordPath);
					handler.post(recordWaitting);
					v.setBackgroundResource(R.drawable.icon_recordpress);

					// Toast.makeText(this, "���ڴ���¼���ļ�",
					// Toast.LENGTH_SHORT).show();
				} else {
					StopRecord();
					handler.post(recordStop);
					v.setBackgroundResource(R.drawable.icon_record);
					//
					// playsdk.

					// int result = playsdk.PLAYStopDataRecord(nPort);
					// if (result == 1) {
					// new Thread() {
					// public void run() {
					// handler.post(recordStop);
					// }
					// }.start();
					// v.setBackgroundResource(R.drawable.icon_record);
					// }
				}
				recordBoolean = recordOrNot(recordBoolean);
			} else {
				Toast.makeText(this, "��Ƶ��ֹͣ���޷�¼��", Toast.LENGTH_SHORT).show();
			}

			break;
		// �Խ���ť
		case R.id.talk:

			break;
		// �ղذ�ť
		case R.id.favor: {
			if (ifFavor.equals("1")) {
				ifFavor = "0";
				v.setBackgroundResource(R.drawable.icon_favor);

				deleteFavor mThread = new deleteFavor();
				Thread thread = new Thread(mThread);
				thread.start();

				// Toast.makeText(this, "ȡ���ղأ�", Toast.LENGTH_SHORT).show();
			} else if (ifFavor.equals("0")) {
				ifFavor = "1";
				v.setBackgroundResource(R.drawable.icon_favorpress);

				AddFavor mThread = new AddFavor();
				Thread thread = new Thread(mThread);
				thread.start();

				// Toast.makeText(this, "����ղأ�", Toast.LENGTH_SHORT).show();
			}
		}
			break;
		// ���ذ�ť
		case R.id.bottomback:

			if (!recordBoolean) {
				// waitClose();

				handler.post(runnableHidProgress);
				isAlive = false;// �Ƿ�ۿ���Ƶ

				if (m_loginHandle != 0) {
					StopRealPlay();
					INetSDK.Logout(m_loginHandle);
					m_loginHandle = 0;
				}
				finish();
			} else {
				Toast.makeText(this, "����¼���У������˳���", Toast.LENGTH_SHORT).show();
			}
			break;
		}

	}

	public void StartRecord(String path) {
		if (bRecordFlag == true) {
			return;
		} else {
		}

		bRecordFlag = true;

		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}

		INetSDK.SaveRealData(lRealHandle, path);
	}

	public void StopRecord() {
		if (bRecordFlag == false) {
			return;
		} else {
		}

		bRecordFlag = false;

		INetSDK.StopSaveRealData(lRealHandle);
	}

	private Boolean ifFling = true;

	public void goToZoomPage() {
		handler.sendEmptyMessage(0);
	}

	public void goToSwicherPage() {
		handler.sendEmptyMessage(1);
	}

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {
		// @Override
		// public boolean onDoubleTap(MotionEvent e) {
		//
		// if (ifFling) {
		// goToZoomPage();
		// ifFling = false;
		// } else {
		// goToSwicherPage();
		// ifFling = true;
		// }
		// return ifFling;
		// }

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			if (!isWaiting && !recordBoolean) { // ��������л�һ����Ƶ������¼������Ӧҡ�Ρ�
				if (ifFling) {
					try {
						if (e1.getX() - e2.getX() > 120) {
							if (!recordBoolean) {
								switchDev("next");
							} else {
								new Thread() {
									public void run() {
										// handler.post(recording);
									}
								}.start();
							}
							return true;
						} else if (e1.getX() - e2.getX() < -120) {
							if (!recordBoolean) {
								switchDev("last");
							} else {
								new Thread() {
									public void run() {
										// handler.post(recording);
									}
								}.start();
							}
							return true;
						}
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}

				}
			}
			return true;
		}
	}

	// ҡ��
	private ShakeDetector mShakeDetector;
	private ShakeListener mShaker;
	private Timer switchTimer;
	private boolean isSwitching = true;
	private Thread swiThread;
	private int switchTimerCount = 0;
	private String switchRtspUrl = "";

	private void switchDev(String flag) {

		Bundle bundle = DhPlayerActivity.this.getIntent().getExtras();
		ArrayList devList = bundle.getParcelableArrayList("videoList");
		if (devList.size() == 1) {
			return;
		}

		if (flag.equals("last")) {
			try {

				String nameString = "";

				isWaiting = true;
				loadingFlag = true;

				switchTimerCount = 0;

				status = false;
				// surfaceView.draw(canvas);

				if (m_loginHandle != 0) {
					StopRealPlay();
					INetSDK.Logout(m_loginHandle);
					m_loginHandle = 0;
				}

				Thread.sleep(500);

				// if (chNo.length() == 1) {
				// chNo = "0" + chNo;
				// }

				// ѭ��������һ���豸��Ϣ
				int i = 0;
				for (i = 0; i < devList.size(); i++) {
					StringBuffer sb = new StringBuffer("");
					HashMap<String, Object> map = (HashMap<String, Object>) devList
							.get(i);
					Integer no = Integer.parseInt(map.get("listNo").toString());
					if (listNo == 1) {
						if (no == listCount) {
							String online = map.get("stayLine").toString();
							if (online.equals("1")) {
								listNo = no;
								nameString = map.get("chName").toString();
								ptz = map.get("ptz").toString();
								zoom = map.get("zoom").toString();
								talk = map.get("talk").toString();
								width = Integer.parseInt(map.get("width")
										.toString());// ��ȡ����������Ƶ���
								height = Integer.parseInt(map.get("height")
										.toString());// ��ȡ����������Ƶ�߶�
								adapterID = Integer.parseInt(map.get(
										"adapterId").toString());// ��ȡ����ID
								chNo = Integer.parseInt(map.get("chNo")
										.toString());// ��ȡͨ��ID
								videoString = map.get("devId").toString();// ��ȡ�豸���к�

								strIp = map.get("ip").toString();
								strPort = map.get("port").toString();
								strUser = map.get("devUserName").toString();
								strPassword = map.get("devPassWord").toString();

								break;
							} else {
								listNo = no;
								i = -1;
							}
						}
					} else {
						if (no == listNo - 1) {
							String online = map.get("stayLine").toString();
							if (online.equals("1")) {
								listNo = no;
								nameString = map.get("chName").toString();
								ptz = map.get("ptz").toString();
								zoom = map.get("zoom").toString();
								talk = map.get("talk").toString();
								width = Integer.parseInt(map.get("width")
										.toString());// ��ȡ����������Ƶ���
								height = Integer.parseInt(map.get("height")
										.toString());// ��ȡ����������Ƶ�߶�
								adapterID = Integer.parseInt(map.get(
										"adapterId").toString());// ��ȡ����ID
								chNo = Integer.parseInt(map.get("chNo")
										.toString());// ��ȡͨ��ID
								videoString = map.get("devId").toString();// ��ȡ�豸���к�

								strIp = map.get("ip").toString();
								strPort = map.get("port").toString();
								strUser = map.get("devUserName").toString();
								strPassword = map.get("devPassWord").toString();
								break;
							} else {
								listNo = no;
								i = -1;
							}
						}
					}

				}

				if (ptz.equals("1")) {
					upButton.setVisibility(Button.VISIBLE);
					downButton.setVisibility(Button.VISIBLE);
					leftButton.setVisibility(Button.VISIBLE);
					rightButton.setVisibility(Button.VISIBLE);
				} else if (ptz.equals("0")) {
					upButton.setVisibility(Button.GONE);
					downButton.setVisibility(Button.GONE);
					leftButton.setVisibility(Button.GONE);
					rightButton.setVisibility(Button.GONE);
				}

				if (zoom.equals("1")) {
					zoominButton.setVisibility(Button.VISIBLE);
					zoomoutButton.setVisibility(Button.VISIBLE);
					focusinButton.setVisibility(Button.VISIBLE);
					focusoutButton.setVisibility(Button.VISIBLE);
				} else if (zoom.equals("0")) {
					zoominButton.setVisibility(Button.GONE);
					zoomoutButton.setVisibility(Button.GONE);
					focusinButton.setVisibility(Button.GONE);
					focusoutButton.setVisibility(Button.GONE);
				}
				if (talk.equals("1")) {
					talkButton.setVisibility(Button.VISIBLE);
				} else if (talk.equals("0")) {
					talkButton.setVisibility(Button.GONE);
				}

				if (width < 705) {
					playType = SDK_RealPlayType.SDK_RType_Realplay_1;
				} else if (width > 704 && width < 1300) {
					playType = SDK_RealPlayType.SDK_RType_Realplay;

				} else {
					playType = SDK_RealPlayType.SDK_RType_Realplay;
				}

				switchTimer = new Timer();

				// showWait("�л����豸:" + nameString + "");
				progressTextView.setText("�л����豸:" + nameString);
				handler.post(runnableShowProgress);
				swiThread = new Thread(switchThread);
				swiThread.start();

				if (ptz.equals("1")) {
					byteFlag = ptzFlag;
				} else {
					byteFlag = noPtzFlag;
				}

				// m_PlayView.Init(nPort);
				// deviceInfo = new NET_DEVICEINFO();
				final Integer error = Integer.valueOf(0);

				DeviceDisConnect disConnect = new DeviceDisConnect();
				INetSDK.Init(disConnect);

				NET_PARAM stNetParam = new NET_PARAM();
				stNetParam.nWaittime = 10000;
				INetSDK.SetNetworkParam(stNetParam);

				Thread dhThread = new Thread(new Runnable() {
					@Override
					public void run() {

						try {
							Thread.sleep(1500);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						int connectTimes = 0;

						while (connectTimes < 5) {
							try {
								m_loginHandle = INetSDK.LoginEx(strIp,
										Integer.parseInt(strPort), strUser,
										strPassword, 20, null, deviceInfo,
										error);
								System.out.println("attempt:"
										+ (connectTimes + 1));
							} catch (Exception e) {
								// TODO: handle exception
							}

							if (m_loginHandle != 0) {
								// ToolKits.showMessage(this, "Login Success!");

								DeviceReConnect reConnect = new DeviceReConnect();
								INetSDK.SetAutoReconnect(reConnect);

								DeviceSubDisConnect subDisConnect = new DeviceSubDisConnect();
								INetSDK.SetSubconnCallBack(subDisConnect);

								INetSDK.SetConnectTime(500, 3);
								connectTimes = 10;
							} else {
								// ToolKits.showErrorMessage(this,
								// "Login Fail.");
								connectTimes++;
							}
						}
						if (connectTimes != 10) {
							handler.post(runnableHidProgress);
							handler.post(connectFailed);
						}

						holder = m_PlayView.getHolder();
						holder.addCallback(new Callback() {

							public void surfaceCreated(SurfaceHolder holder) {
								Log.d("[playsdk]surface", "surfaceCreated");
							}

							public void surfaceChanged(SurfaceHolder holder,
									int format, int width, int height) {
								Log.d("[playsdk]surface", "surfaceChanged");
							}

							public void surfaceDestroyed(SurfaceHolder holder) {
								Log.d("[playsdk]surface", "surfaceDestroyed");
							}
						});
						if (StartRealPlay() == true) {
							m_callback = new TestRealDataCallBackEx();
							if (lRealHandle != 0) {
								// Set Real Data Call Back Object
								INetSDK.SetRealDataCallBackEx(lRealHandle,
										m_callback, 1);
							}
						}
					}
				});
				dhThread.start();

				ifFavor = "0";
				// ���ݿ�
				Cursor cur = db.select_favor();
				while (cur.moveToNext()) {
					int webIndex = cur.getColumnIndex("DATA_WEBID");
					webId = cur.getString(webIndex);
					int userIndex = cur.getColumnIndex("DATA_USERID");
					String webUserId = cur.getString(userIndex);
					int devIndex = cur.getColumnIndex("DATA_DEVID");
					String favorDevId = cur.getString(devIndex);
					int chIndex = cur.getColumnIndex("DATA_CHNO");
					String favorChNo = cur.getString(chIndex);
					if (favorDevId.equals(videoString)
							&& favorChNo.equals(Integer.toString(chNo))
							&& webUserId.equals(userId)) {
						ifFavor = "1";
					}
				}
				if (ifFavor.equals("1")) {
					favorButton
							.setBackgroundResource(R.drawable.icon_favorpress);
				} else if (ifFavor.equals("0")) {
					favorButton.setBackgroundResource(R.drawable.icon_favor);
				}

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else if (flag.equals("next")) {

			try {
				final String rtspUrl = "";
				String nameString = "";
				recordBoolean = false;
				// vv = new VView(ShowVideo.this);
				isWaiting = true;
				loadingFlag = true;
				switchTimerCount = 0;

				status = false;
				// surfaceView.draw(canvas);

				if (m_loginHandle != 0) {
					StopRealPlay();
					INetSDK.Logout(m_loginHandle);
					m_loginHandle = 0;
				}

				Thread.sleep(500);

				// ѭ��������һ���豸��Ϣ
				int i = 0;
				for (i = 0; i < devList.size(); i++) {
					StringBuffer sb = new StringBuffer("");
					HashMap<String, Object> map = (HashMap<String, Object>) devList
							.get(i);
					Integer no = Integer.parseInt(map.get("listNo").toString());
					if (listNo == listCount) {
						if (no == 1) {
							String online = map.get("stayLine").toString();
							if (online.equals("1")) {
								listNo = no;
								nameString = map.get("chName").toString();
								ptz = map.get("ptz").toString();
								zoom = map.get("zoom").toString();
								talk = map.get("talk").toString();
								width = Integer.parseInt(map.get("width")
										.toString());// ��ȡ����������Ƶ���
								height = Integer.parseInt(map.get("height")
										.toString());// ��ȡ����������Ƶ�߶�
								adapterID = Integer.parseInt(map.get(
										"adapterId").toString());// ��ȡ����ID
								chNo = Integer.parseInt(map.get("chNo")
										.toString());// ��ȡͨ��ID
								videoString = map.get("devId").toString();// ��ȡ�豸���к�

								strIp = map.get("ip").toString();
								strPort = map.get("port").toString();
								strUser = map.get("devUserName").toString();
								strPassword = map.get("devPassWord").toString();
								break;
							} else {
								listNo = no;
								i = -1;
							}
						}
					} else {
						if (no == listNo + 1) {
							String online = map.get("stayLine").toString();
							if (online.equals("1")) {
								listNo = no;
								nameString = map.get("chName").toString();
								ptz = map.get("ptz").toString();
								zoom = map.get("zoom").toString();
								talk = map.get("talk").toString();
								width = Integer.parseInt(map.get("width")
										.toString());// ��ȡ����������Ƶ���
								height = Integer.parseInt(map.get("height")
										.toString());// ��ȡ����������Ƶ�߶�
								adapterID = Integer.parseInt(map.get(
										"adapterId").toString());// ��ȡ����ID
								chNo = Integer.parseInt(map.get("chNo")
										.toString());// ��ȡͨ��ID
								videoString = map.get("devId").toString();// ��ȡ�豸���к�

								strIp = map.get("ip").toString();
								strPort = map.get("port").toString();
								strUser = map.get("devUserName").toString();
								strPassword = map.get("devPassWord").toString();
								break;
							} else {
								listNo = no;
								i = -1;
							}
						}
					}

				}

				if (ptz.equals("1")) {
					upButton.setVisibility(Button.VISIBLE);
					downButton.setVisibility(Button.VISIBLE);
					leftButton.setVisibility(Button.VISIBLE);
					rightButton.setVisibility(Button.VISIBLE);
				} else if (ptz.equals("0")) {
					upButton.setVisibility(Button.GONE);
					downButton.setVisibility(Button.GONE);
					leftButton.setVisibility(Button.GONE);
					rightButton.setVisibility(Button.GONE);
				}

				if (zoom.equals("1")) {
					zoominButton.setVisibility(Button.VISIBLE);
					zoomoutButton.setVisibility(Button.VISIBLE);
					focusinButton.setVisibility(Button.VISIBLE);
					focusoutButton.setVisibility(Button.VISIBLE);
				} else if (zoom.equals("0")) {
					zoominButton.setVisibility(Button.GONE);
					zoomoutButton.setVisibility(Button.GONE);
					focusinButton.setVisibility(Button.GONE);
					focusoutButton.setVisibility(Button.GONE);
				}
				if (talk.equals("1")) {
					talkButton.setVisibility(Button.VISIBLE);
				} else if (talk.equals("0")) {
					talkButton.setVisibility(Button.GONE);
				}

				if (width < 705) {
					playType = SDK_RealPlayType.SDK_RType_Realplay_1;
				} else if (width > 704 && width < 1300) {
					playType = SDK_RealPlayType.SDK_RType_Realplay;

				} else {
					playType = SDK_RealPlayType.SDK_RType_Realplay;
				}

				switchTimer = new Timer();
				// showWait("�л����豸:" + nameString + "");
				progressTextView.setText("�л����豸:" + nameString);
				handler.post(runnableShowProgress);
				swiThread = new Thread(switchThread);
				swiThread.start();

				if (ptz.equals("1")) {
					byteFlag = ptzFlag;
				} else {
					byteFlag = noPtzFlag;
				}

				// m_PlayView.Init(nPort);
				// deviceInfo = new NET_DEVICEINFO();
				final Integer error = Integer.valueOf(0);

				DeviceDisConnect disConnect = new DeviceDisConnect();
				INetSDK.Init(disConnect);

				NET_PARAM stNetParam = new NET_PARAM();
				stNetParam.nWaittime = 10000;
				INetSDK.SetNetworkParam(stNetParam);

				Thread dhThread = new Thread(new Runnable() {
					@Override
					public void run() {

						try {
							Thread.sleep(1500);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						int connectTimes = 0;

						while (connectTimes < 5) {
							try {
								m_loginHandle = INetSDK.LoginEx(strIp,
										Integer.parseInt(strPort), strUser,
										strPassword, 20, null, deviceInfo,
										error);
								System.out.println("attempt:"
										+ (connectTimes + 1));
							} catch (Exception e) {
								// TODO: handle exception
							}

							if (m_loginHandle != 0) {
								// ToolKits.showMessage(this, "Login Success!");

								DeviceReConnect reConnect = new DeviceReConnect();
								INetSDK.SetAutoReconnect(reConnect);

								DeviceSubDisConnect subDisConnect = new DeviceSubDisConnect();
								INetSDK.SetSubconnCallBack(subDisConnect);

								INetSDK.SetConnectTime(500, 3);
								connectTimes = 10;
							} else {
								// ToolKits.showErrorMessage(this,
								// "Login Fail.");
								connectTimes++;
							}
						}
						if (connectTimes != 10) {
							handler.post(runnableHidProgress);
							handler.post(connectFailed);
						}
						holder = m_PlayView.getHolder();
						holder.addCallback(new Callback() {

							public void surfaceCreated(SurfaceHolder holder) {
								Log.d("[playsdk]surface", "surfaceCreated");
							}

							public void surfaceChanged(SurfaceHolder holder,
									int format, int width, int height) {
								Log.d("[playsdk]surface", "surfaceChanged");
							}

							public void surfaceDestroyed(SurfaceHolder holder) {
								Log.d("[playsdk]surface", "surfaceDestroyed");
							}
						});
						if (StartRealPlay() == true) {
							m_callback = new TestRealDataCallBackEx();
							if (lRealHandle != 0) {
								// Set Real Data Call Back Object
								INetSDK.SetRealDataCallBackEx(lRealHandle,
										m_callback, 1);
							}
						}
					}
				});
				dhThread.start();
				ifFavor = "0";
				// ���ݿ�
				Cursor cur = db.select_favor();
				while (cur.moveToNext()) {
					int webIndex = cur.getColumnIndex("DATA_WEBID");
					webId = cur.getString(webIndex);
					int userIndex = cur.getColumnIndex("DATA_USERID");
					String webUserId = cur.getString(userIndex);
					int devIndex = cur.getColumnIndex("DATA_DEVID");
					String favorDevId = cur.getString(devIndex);
					int chIndex = cur.getColumnIndex("DATA_CHNO");
					String favorChNo = cur.getString(chIndex);
					if (favorDevId.equals(videoString)
							&& favorChNo.equals(Integer.toString(chNo))
							&& webUserId.equals(userId)) {
						ifFavor = "1";
					}
				}
				if (ifFavor.equals("1")) {
					favorButton
							.setBackgroundResource(R.drawable.icon_favorpress);
				} else if (ifFavor.equals("0")) {
					favorButton.setBackgroundResource(R.drawable.icon_favor);
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}

	// �ζ���Ƶ�л�
	private void shakeDetector() {
		// �ζ�
		mShaker = new ShakeListener(this);
		mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
			public void onShake() {

				Bundle bundle = DhPlayerActivity.this.getIntent().getExtras();
				ArrayList devList = bundle.getParcelableArrayList("videoList");
				if (!isWaiting && !recordBoolean) { // ��������л�һ����Ƶ������¼������Ӧҡ�Ρ�
					try {
						System.out.println("shake");
						final String rtspUrl = "";
						String nameString = "";
						recordBoolean = false;
						// vv = new VView(ShowVideo.this);
						isWaiting = true;
						loadingFlag = true;
						switchTimerCount = 0;

						status = false;
						// surfaceView.draw(canvas);

						if (m_loginHandle != 0) {
							StopRealPlay();
							INetSDK.Logout(m_loginHandle);
							m_loginHandle = 0;
						}

						Thread.sleep(500);

						// ѭ��������һ���豸��Ϣ
						int i = 0;
						for (i = 0; i < devList.size(); i++) {
							StringBuffer sb = new StringBuffer("");
							HashMap<String, Object> map = (HashMap<String, Object>) devList
									.get(i);
							Integer no = Integer.parseInt(map.get("listNo")
									.toString());
							if (listNo == listCount) {
								if (no == 1) {
									String online = map.get("stayLine")
											.toString();
									if (online.equals("1")) {
										listNo = no;
										nameString = map.get("chName")
												.toString();
										ptz = map.get("ptz").toString();
										zoom = map.get("zoom").toString();
										talk = map.get("talk").toString();
										width = Integer.parseInt(map.get(
												"width").toString());// ��ȡ����������Ƶ���
										height = Integer.parseInt(map.get(
												"height").toString());// ��ȡ����������Ƶ�߶�
										adapterID = Integer.parseInt(map.get(
												"adapterId").toString());// ��ȡ����ID
										chNo = Integer.parseInt(map.get("chNo")
												.toString());// ��ȡͨ��ID
										videoString = map.get("devId")
												.toString();// ��ȡ�豸���к�

										strIp = map.get("ip").toString();
										strPort = map.get("port").toString();
										strUser = map.get("devUserName")
												.toString();
										strPassword = map.get("devPassWord")
												.toString();
										break;
									} else {
										listNo = no;
										i = -1;
									}
								}
							} else {
								if (no == listNo + 1) {
									String online = map.get("stayLine")
											.toString();
									if (online.equals("1")) {
										listNo = no;
										nameString = map.get("chName")
												.toString();
										ptz = map.get("ptz").toString();
										zoom = map.get("zoom").toString();
										talk = map.get("talk").toString();
										width = Integer.parseInt(map.get(
												"width").toString());// ��ȡ����������Ƶ���
										height = Integer.parseInt(map.get(
												"height").toString());// ��ȡ����������Ƶ�߶�
										adapterID = Integer.parseInt(map.get(
												"adapterId").toString());// ��ȡ����ID
										chNo = Integer.parseInt(map.get("chNo")
												.toString());// ��ȡͨ��ID
										videoString = map.get("devId")
												.toString();// ��ȡ�豸���к�

										strIp = map.get("ip").toString();
										strPort = map.get("port").toString();
										strUser = map.get("devUserName")
												.toString();
										strPassword = map.get("devPassWord")
												.toString();
										break;
									} else {
										listNo = no;
										i = -1;
									}
								}
							}

						}

						if (ptz.equals("1")) {
							upButton.setVisibility(Button.VISIBLE);
							downButton.setVisibility(Button.VISIBLE);
							leftButton.setVisibility(Button.VISIBLE);
							rightButton.setVisibility(Button.VISIBLE);
						} else if (ptz.equals("0")) {
							upButton.setVisibility(Button.GONE);
							downButton.setVisibility(Button.GONE);
							leftButton.setVisibility(Button.GONE);
							rightButton.setVisibility(Button.GONE);
						}

						if (zoom.equals("1")) {
							zoominButton.setVisibility(Button.VISIBLE);
							zoomoutButton.setVisibility(Button.VISIBLE);
							focusinButton.setVisibility(Button.VISIBLE);
							focusoutButton.setVisibility(Button.VISIBLE);
						} else if (zoom.equals("0")) {
							zoominButton.setVisibility(Button.GONE);
							zoomoutButton.setVisibility(Button.GONE);
							focusinButton.setVisibility(Button.GONE);
							focusoutButton.setVisibility(Button.GONE);
						}
						if (talk.equals("1")) {
							talkButton.setVisibility(Button.VISIBLE);
						} else if (talk.equals("0")) {
							talkButton.setVisibility(Button.GONE);
						}

						if (width < 705) {
							playType = SDK_RealPlayType.SDK_RType_Realplay_1;
						} else if (width > 704 && width < 1300) {
							playType = SDK_RealPlayType.SDK_RType_Realplay;

						} else {
							playType = SDK_RealPlayType.SDK_RType_Realplay;
						}

						switchTimer = new Timer();
						// showWait("�л����豸:" + nameString + "");
						progressTextView.setText("�л����豸:" + nameString);
						handler.post(runnableShowProgress);
						swiThread = new Thread(switchThread);
						swiThread.start();

						if (ptz.equals("1")) {
							byteFlag = ptzFlag;
						} else {
							byteFlag = noPtzFlag;
						}

						// m_PlayView.Init(nPort);
						// deviceInfo = new NET_DEVICEINFO();
						final Integer error = Integer.valueOf(0);

						DeviceDisConnect disConnect = new DeviceDisConnect();
						INetSDK.Init(disConnect);

						NET_PARAM stNetParam = new NET_PARAM();
						stNetParam.nWaittime = 10000;
						INetSDK.SetNetworkParam(stNetParam);

						Thread dhThread = new Thread(new Runnable() {
							@Override
							public void run() {

								try {
									Thread.sleep(1500);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								int connectTimes = 0;

								while (connectTimes < 5) {
									try {
										m_loginHandle = INetSDK.LoginEx(strIp,
												Integer.parseInt(strPort),
												strUser, strPassword, 20, null,
												deviceInfo, error);
										System.out.println("attempt:"
												+ (connectTimes + 1));
									} catch (Exception e) {
										// TODO: handle exception
									}

									if (m_loginHandle != 0) {
										// ToolKits.showMessage(this,
										// "Login Success!");

										DeviceReConnect reConnect = new DeviceReConnect();
										INetSDK.SetAutoReconnect(reConnect);

										DeviceSubDisConnect subDisConnect = new DeviceSubDisConnect();
										INetSDK.SetSubconnCallBack(subDisConnect);

										INetSDK.SetConnectTime(500, 3);
										connectTimes = 10;
									} else {
										// ToolKits.showErrorMessage(this,
										// "Login Fail.");
										connectTimes++;
									}
								}
								if (connectTimes != 10) {
									handler.post(runnableHidProgress);
									handler.post(connectFailed);
								}

								holder = m_PlayView.getHolder();
								holder.addCallback(new Callback() {

									public void surfaceCreated(
											SurfaceHolder holder) {
										Log.d("[playsdk]surface",
												"surfaceCreated");
									}

									public void surfaceChanged(
											SurfaceHolder holder, int format,
											int width, int height) {
										Log.d("[playsdk]surface",
												"surfaceChanged");
									}

									public void surfaceDestroyed(
											SurfaceHolder holder) {
										Log.d("[playsdk]surface",
												"surfaceDestroyed");
									}
								});
								if (StartRealPlay() == true) {
									m_callback = new TestRealDataCallBackEx();
									if (lRealHandle != 0) {
										// Set Real Data Call Back Object
										INetSDK.SetRealDataCallBackEx(
												lRealHandle, m_callback, 1);
									}
								}
							}
						});
						dhThread.start();

						ifFavor = "0";
						// ���ݿ�
						Cursor cur = db.select_favor();
						while (cur.moveToNext()) {
							int webIndex = cur.getColumnIndex("DATA_WEBID");
							webId = cur.getString(webIndex);
							int userIndex = cur.getColumnIndex("DATA_USERID");
							String webUserId = cur.getString(userIndex);
							int devIndex = cur.getColumnIndex("DATA_DEVID");
							String favorDevId = cur.getString(devIndex);
							int chIndex = cur.getColumnIndex("DATA_CHNO");
							String favorChNo = cur.getString(chIndex);
							if (favorDevId.equals(videoString)
									&& favorChNo.equals(Integer.toString(chNo))
									&& webUserId.equals(userId)) {
								ifFavor = "1";
							}
						}
						if (ifFavor.equals("1")) {
							favorButton
									.setBackgroundResource(R.drawable.icon_favorpress);
						} else if (ifFavor.equals("0")) {
							favorButton
									.setBackgroundResource(R.drawable.icon_favor);
						}
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}
		});
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			handler.sendEmptyMessageDelayed(0, 1000);

			if (msg.what == 0) {
				mZoomState = new ZoomState();
				mZoomListener = new SimpleZoomListener();
				mZoomListener.setZoomState(mZoomState);
				mZoomListener.setmGestureDetector(new GestureDetector(
						new MyGestureListener()));
				m_PlayView.setOnTouchListener(mZoomListener);

				resetZoomState();
			} else if (msg.what == 1) {
				mZoomState = new ZoomState();
				mZoomListener = new SimpleZoomListener();
				mZoomListener.setZoomState(mZoomState);
				mZoomListener.setmGestureDetector(new GestureDetector(
						new MyGestureListener()));
				m_PlayView.setOnTouchListener(mZoomListener);
			}
		}
	};

	private void resetZoomState() {
		mZoomState.setPanX(0.5f);
		mZoomState.setPanY(0.5f);
		mZoomState.setZoom(1f);
		mZoomState.notifyObservers();
	}

	long lastVlcTime = 0;
	long vlcTime = 0;

	// �л���ʾ�����
	private void SwitchTimerCount(Timer t) {

		if (isAlive) {
			int PLAYGetCurrentFrameRate = (int) playsdk
					.PLAYGetCurrentFrameRate(1);
			System.out.println("PLAYGetCurrentFrameRate:"
					+ PLAYGetCurrentFrameRate);

			if (m_loginHandle != 0 && PLAYGetCurrentFrameRate != 0) {

				// waitClose();

				new Thread() {
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						handler.post(runnableHidProgress);
						isWaiting = false;
						loadingFlag = false;
					}
				}.start();
				t.cancel();
			} else {
				if (loadingFlag) {
					switchTimerCount++;
					if (switchTimerCount > 240) {
						t.cancel();
						// waitClose();

						handler.post(runnableHidProgress);
						if (isWaiting) {
							// showWait("���粻�ȶ�����Ƶ������Ӧ");
							// try {
							// Thread.sleep(3000);
							// waitClose();
							// isWaiting = false;
							// loadingFlag = false;
							// } catch (InterruptedException e) {
							// e.printStackTrace();
							// }

							isWaiting = false;
							loadingFlag = false;
							new Thread() {
								public void run() {
									handler.post(runnableWaitting);
								}
							}.start();
						}
					}
				} else {
					t.cancel();
				}
			}
		}
	}

	private void SwitchTimerOut() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				SwitchTimerCount(switchTimer);
			}
		};
		switchTimer.schedule(timerTask, 0, 1000);
	}

	Runnable switchThread = new Runnable() {
		public void run() {
			if (isSwitching) {
				SwitchTimerOut();
			}
		}
	};

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		// �˳����������˳�����Ϊture
		if (isAlive) {

			// waitClose();
			if (recordBoolean) {

				StopRecord();
				System.out.println("videoRecordStop");
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			handler.post(runnableHidProgress);
			System.out.println("onStoprunnableHidProgress");
			isWaiting = false;
			isAlive = false;// �Ƿ�ۿ���Ƶ

			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {

			if (!recordBoolean) {
				// �˳����������˳�����Ϊture

				// waitClose();

				handler.post(runnableHidProgress);

				System.out.println("runnableHidProgress");
				isWaiting = false;
				isAlive = false;// �Ƿ�ۿ���Ƶ

				if (m_loginHandle != 0) {
					StopRealPlay();
					INetSDK.Logout(m_loginHandle);
					m_loginHandle = 0;
				}

				System.out.println("stop");

				finish();
				System.out.println("finish");

			} else {
				Toast.makeText(this, "����¼���У������˳���", Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	Boolean playBoolean = true;
	Boolean talkBoolean = false;
	Boolean recordBoolean = false;
	static Boolean isRecoding = false;

	private Boolean playOrPause(Boolean bl) {
		if (bl == true)
			return false;
		else
			return true;
	}

	private Boolean recordOrNot(Boolean bl) {
		if (bl == true)
			return false;
		else
			return true;
	}

	// ����ղ���ʾ
	Runnable addFavorRunnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(DhPlayerActivity.this, "����ղأ�", Toast.LENGTH_LONG)
					.show();
		}

	};
	// ɾ���ղ���ʾ
	Runnable deleteFavorRunnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(DhPlayerActivity.this, "ɾ���ղأ�", Toast.LENGTH_LONG)
					.show();
		}

	};

	private class AddFavor implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub

			String webAddId = "0";

			// String NAMESPACE = "MobileNewspaper";
			// String METHOD_NAME = "AddFavor";
			// String URL = "http://" + dstip
			// + getResources().getString(R.string.url_server);
			// String SOAP_ACTION = "MobileNewspaper/AddFavor";
			//
			// SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
			// rpc.addProperty("userId", userId);
			// rpc.addProperty("devId", videoString);
			// rpc.addProperty("chno", chNo);
			//
			// SoapSerializationEnvelope envelope = new
			// SoapSerializationEnvelope(
			// SoapEnvelope.VER11);
			// envelope.bodyOut = rpc;
			// envelope.dotNet = true;
			// envelope.setOutputSoapObject(rpc);
			// HttpTransportSE ht = new HttpTransportSE(URL);
			// ht.debug = true;
			// try {
			// ht.call(SOAP_ACTION, envelope);
			// Object push_soap = envelope.getResponse();
			// webAddId = push_soap.toString().trim();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (XmlPullParserException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			ContentValues values = new ContentValues();
			values.put("DATA_WEBID", webAddId);
			values.put("DATA_USERID", userId);
			values.put("DATA_DEVID", videoString);
			values.put("DATA_CHNO", chNo);
			db.insert_favor(values);
			new Thread() {
				public void run() {
					handler.post(addFavorRunnable);
				}
			}.start();
		}

	}

	private class deleteFavor implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub

			db.DeleteFavorById(userId, videoString, Integer.toString(chNo));

			// String NAMESPACE = "MobileNewspaper";
			// String METHOD_NAME = "DeleteFavor";
			// String URL = "http://" + dstip
			// + getResources().getString(R.string.url_server);
			// String SOAP_ACTION = "MobileNewspaper/DeleteFavor";
			//
			// SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
			// rpc.addProperty("favorId", webId);
			//
			// SoapSerializationEnvelope envelope = new
			// SoapSerializationEnvelope(
			// SoapEnvelope.VER11);
			// envelope.bodyOut = rpc;
			// envelope.dotNet = true;
			// envelope.setOutputSoapObject(rpc);
			// HttpTransportSE ht = new HttpTransportSE(URL);
			// ht.debug = true;
			// try {
			// ht.call(SOAP_ACTION, envelope);
			// Object push_soap = envelope.getResponse();
			// String result = push_soap.toString().trim();
			// String result_text = "";
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (XmlPullParserException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			new Thread() {
				public void run() {
					handler.post(deleteFavorRunnable);
				}
			}.start();
		}

	}

	public class DeviceDisConnect implements CB_fDisConnect {
		@Override
		public void invoke(long lLoginID, String pchDVRIP, int nDVRPort) {
			ToolKits.writeLog("Device " + pchDVRIP + " DisConnect!");
			return;
		}
	}

	public class DeviceReConnect implements CB_fHaveReConnect {
		@Override
		public void invoke(long lLoginID, String pchDVRIP, int nDVRPort) {
			ToolKits.writeLog("Device " + pchDVRIP + " ReConnect!");
		}
	}

	public class DeviceSubDisConnect implements CB_fSubDisConnect {
		@Override
		public void invoke(int emInterfaceType, boolean bOnline,
				long lOperateHandle, long lLoginID) {

			ToolKits.writeLog("Device SubConnect DisConnect");
		}
	}

	public class TestRealDataCallBackEx implements CB_fRealDataCallBackEx {

		boolean bRecordFlag = true;

		@Override
		public void invoke(long lRealHandle, int dwDataType, byte[] pBuffer,
				int dwBufSize, int param) {
			if (0 == dwDataType) {
				playsdk.PLAYInputData(nPort, pBuffer, pBuffer.length);
			}
		}
	}

	public boolean PTZControl(MotionEvent event, int nChn, int nControl,
			byte param1, byte param2) {
		int nAction = event.getAction();
		if ((nAction != MotionEvent.ACTION_DOWN)
				&& (nAction != MotionEvent.ACTION_UP)) {
			return false;
		}

		boolean zRet = INetSDK.SDKPTZControl(m_loginHandle, nChn, nControl,
				param1, param2, (byte) 0, nAction == MotionEvent.ACTION_UP);

		return false;
	}

	public class LiveButtonsListener implements OnClickListener {

		public void onClick(View btClick) {
			// if( btClick == m_btMore )
			// {
			// ToolKits.showMessage(LiveActivity.this, "Hit More");
			// //Jump to another Activity to more ptz operation
			// }
			// else if (btClick == m_btStartRecord )
			// {
			// StartRecord();
			// }
			// else if(btClick == m_btStopRecord)
			// {
			// StopRecord();
			// }
		}

	}

	public class TestRealDataAndDisConn implements CB_fRealDataCallBackEx,
			CB_fRealPlayDisConnect {
		@Override
		public void invoke(long lRealHandle, int dwDataType, byte[] pBuffer,
				int dwBufSize, int param) {
		}

		@Override
		public void invoke(long lOperateHandle, int dwEventType) {
			ToolKits.writeLog("TestpfAudioDataCallBack");
		}
	}

	static long lRealHandle = 0;

	boolean bRecordFlag = false;
	private SurfaceHolder holder;

	public boolean StartRealPlay() {
		lRealHandle = INetSDK.RealPlayEx(m_loginHandle, chNo, playType);

		if (lRealHandle == 0) {
			// ToolKits.showErrorMessage(this, "Real Play Failed");
			return false;
		}

		boolean bOpenRet = playsdk.PLAYOpenStream(nPort, null, 0, 1024 * 100) == 0 ? false
				: true;
		if (bOpenRet) {
			boolean bPlayRet = playsdk.PLAYPlay(nPort) == 0 ? false : true;
			if (bPlayRet) {
				playsdk.PLAYSetDelayTime(nPort, 300, 600);
				boolean bSuccess = playsdk.PLAYPlaySoundShare(nPort) == 0 ? false
						: true;
				if (!bSuccess) {
					playsdk.PLAYStop(nPort);
					playsdk.PLAYCloseStream(nPort);
					return false;
				}
			} else {
				playsdk.PLAYCloseStream(nPort);
				return false;
			}
		} else {
			return false;
		}

		return true;
	}

	public void StopRealPlay() {
		playsdk.PLAYStop(nPort);
		playsdk.PLAYStopSoundShare(nPort);
		playsdk.PLAYCloseStream(nPort);

		INetSDK.StopRealPlay(lRealHandle);
		INetSDK.Logout(m_loginHandle);
		INetSDK.Cleanup();
		lRealHandle = 0;
	}

	@Override
	protected void onDestroy() {
		if (lRealHandle != 0) {
			StopRealPlay();
		}

		super.onDestroy();
	}

	// ץ��

	// public boolean takeSnapShot(String file, int width, int height) {
	//
	// // return mLibVLC.takeSnapShot(0, file, width, height);
	// }

	// ���浽sdcard
	private static String checkSnapPath() {
		String strFolder = Environment.getExternalStorageDirectory().toString()
				+ "/MLVideo/";

		File file = new File(strFolder);

		if (!file.exists()) {
			file.mkdir();
		}

		strFolder = Environment.getExternalStorageDirectory().toString()
				+ "/MLVideo/Snap/";

		file = new File(strFolder);

		if (!file.exists()) {
			file.mkdir();
		}
		return strFolder;
	}

	private static String checkRecordPath() {
		String strFolder = Environment.getExternalStorageDirectory().toString()
				+ "/MLVideo/";

		File file = new File(strFolder);

		if (!file.exists()) {
			file.mkdir();
		}

		strFolder = Environment.getExternalStorageDirectory().toString()
				+ "/MLVideo/Record/";

		file = new File(strFolder);

		if (!file.exists()) {
			file.mkdir();
		}
		return strFolder;
	}

	// ����
	long old_totalRx = 0;
	long old_totalTx = 0;
	long totalRx = 0;
	long totalTx = 0;
	long totalFlow = 0;

	int count = 0;

	private Handler handlerTraffic = new Handler();

	private Runnable runnable = new Runnable() {
		public void run() {
			this.update();
			handler.postDelayed(this, 1000);// ���1��
			old_totalRx = TrafficStats.getTotalRxBytes();
			old_totalTx = TrafficStats.getTotalTxBytes();
		}

		void update() {
			// ˢ��msg������
			TextView textTraffic = (TextView) findViewById(R.id.traffic);

			String nowTraffic;
			totalRx = TrafficStats.getTotalRxBytes();
			totalTx = TrafficStats.getTotalTxBytes();
			long mrx = totalRx - old_totalRx;
			old_totalRx = totalRx;
			long mtx = totalTx - old_totalTx;
			old_totalTx = totalTx;

			if (count > 0) {
				try {
					totalFlow += mrx;
					textTraffic.setText(TrafficMonitoring.convertTraffic(mrx)
							+ "/s");
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
			count++;
		}

	};

}