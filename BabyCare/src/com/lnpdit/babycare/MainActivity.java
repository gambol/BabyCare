package com.lnpdit.babycare;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.videolan.libvlc.VLCApplication;

import com.easemob.EMConnectionListener;
import com.easemob.EMError;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactListener;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMNotifier;
import com.easemob.chat.GroupChangeListener;
import com.easemob.chat.TextMessageBody;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.EMMessage.Type;
import com.easemob.util.HanziToPinyin;
import com.easemob.util.NetUtils;
import com.lnpdit.babycare.R;
import com.lnpdit.chat.ChatActivity;
import com.lnpdit.chat.ChatAllHistoryActivity;
import com.lnpdit.garden.GardenComActivity;
import com.lnpdit.photo.Constant;
import com.lnpdit.service.NewsPushService;
import com.lnpdit.sqllite.BBGJDB;
import com.lnpdit.sqllite.User;
import com.lnpdit.sqllite.UserDao;
import com.lnpdit.util.DownLoadManager;
import com.lnpdit.util.UpdataInfo;
import com.lnpdit.util.UpdataInfoParser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MainActivity extends TabActivity implements OnClickListener {
	public static String TAB_TAG_GARDEN = "garden";
	public static String TAB_TAG_MONITOR = "monitor";
	public static String TAB_TAG_MSG = "message";
	public static String TAB_TAB_MORE = "more";
	public static TabHost mTabHost;
	static final int COLOR1 = Color.parseColor("#838992");
	static final int COLOR2 = Color.parseColor("#b87721");
	ImageView mBut1, mBut2, mBut3, mBut4;
	TextView mCateText1, mCateText2, mCateText3, mCateText4;

	Intent mGardenIntent, mMonitorItent, mMsgIntent, mMoreIntent;
	private ChatAllHistoryActivity chatHistory;

	int mCurTabId = R.id.channel1;

	// Animation
	private Animation left_in, left_out;
	private Animation right_in, right_out;

	Bundle loginBundle;
	public static String ipString = "";
	public static String IP = "";
	private String userId;
	private String comId;
	private String realName;

	private LinearLayout bottomLayout;

	/** Called when the activity is first created. */
	// ����

	private NewMessageBroadcastReceiver msgReceiver;
	// δ����Ϣtextview
	private TextView unreadLabel;
	// �˺��ڱ𴦵�¼
	private boolean isConflict = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		loginBundle = this.getIntent().getExtras();
		ipString = loginBundle.getString("serverIp");
		IP = "http://" + ipString + ":7799";

		chatHistory = new ChatAllHistoryActivity();

		prepareAnim();
		prepareIntent();
		setupIntent();
		prepareView();
		prepareGarden();

		try {
			localVersion = getVersionName();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CheckVersionTask cv = new CheckVersionTask();
		new Thread(cv).start();

		// ����

		// ע��һ��������Ϣ��BroadcastReceiver
		msgReceiver = new NewMessageBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter(EMChatManager
				.getInstance().getNewMessageBroadcastAction());
		intentFilter.setPriority(3);
		registerReceiver(msgReceiver, intentFilter);

		// ע��һ��ack��ִ��Ϣ��BroadcastReceiver
		IntentFilter ackMessageIntentFilter = new IntentFilter(EMChatManager
				.getInstance().getAckMessageBroadcastAction());
		ackMessageIntentFilter.setPriority(3);
		registerReceiver(ackMessageReceiver, ackMessageIntentFilter);

		// ע��һ��������Ϣ��BroadcastReceiver
		// IntentFilter offlineMessageIntentFilter = new
		// IntentFilter(EMChatManager.getInstance()
		// .getOfflineMessageBroadcastAction());
		// registerReceiver(offlineMessageReceiver, offlineMessageIntentFilter);

		// ע��һ����������״̬��listener
		EMChatManager.getInstance().addConnectionListener(
				new MyConnectionListener());
		// ֪ͨsdk��UI �Ѿ���ʼ����ϣ�ע������Ӧ��receiver��listener, ���Խ���broadcast��
		EMChat.getInstance().setAppInited();

	}

	private void prepareGarden() {

		userId = loginBundle.getString("userId");// �û�ID
		comId = loginBundle.getString("comId");// �׶�԰ID
		realName = loginBundle.getString("realName");// �û�����

		// �ֻ���
		SharedPreferences sjbSsharedPreferences = getSharedPreferences(
				"BBGJ_UserInfo", Context.MODE_PRIVATE); // ˽������
		Editor sjbEditor = sjbSsharedPreferences.edit();// ��ȡ�༭��

		sjbEditor.putInt("userId", Integer.parseInt(userId));
		sjbEditor.putInt("comId", Integer.parseInt(comId));
		sjbEditor.putString("realName", realName);
		sjbEditor.commit();// �ύ�޸�

		try {
			BBGJDB tdd = new BBGJDB(this);
			tdd.cleardbusr();
			tdd.close();
			userLogin();
		} catch (Exception e) {
			// TODO: handle exception
		}

		boolean pushservicestate = isPushServiceWork();
		if (pushservicestate == false) {
			Intent i = new Intent(this, NewsPushService.class);
			this.startService(i);
		}

	}

	public boolean isPushServiceWork() {
		ActivityManager myManager = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30);
		for (int i = 0; i < runningService.size(); i++) {
			if (runningService.get(i).service.getClassName().toString()
					.equals("lnpdit.babycare.pushservice")) {
				return true;
			}
		}
		return false;
	}

	private void userLogin() {
		SharedPreferences share = getSharedPreferences("BBGJ_UserInfo",
				Activity.MODE_WORLD_READABLE);

		BBGJDB tdd = new BBGJDB(this);
		int userid = share.getInt("userId", 0);
		String username = share.getString("realName", "");
		String phonenum = "";
		ContentValues values = new ContentValues();
		values.put(tdd.USER_WEBID, userid);
		values.put(tdd.USER_IMSI, phonenum);
		values.put(tdd.USER_NAME, username);
		values.put(tdd.USER_PUSHID, "0");
		values.put(tdd.USER_VERSION, "0");
		values.put(tdd.USER_REFRESH_RATE, "10000");
		tdd.insertuser(values);
	}

	private void prepareAnim() {
		left_in = AnimationUtils.loadAnimation(this, R.anim.left_in);
		left_out = AnimationUtils.loadAnimation(this, R.anim.left_out);
		right_in = AnimationUtils.loadAnimation(this, R.anim.right_in);
		right_out = AnimationUtils.loadAnimation(this, R.anim.right_out);
	}

	private void prepareView() {
		bottomLayout = (LinearLayout) findViewById(R.id.mainbottom);
		mBut1 = (ImageView) findViewById(R.id.imageView1);
		mBut2 = (ImageView) findViewById(R.id.imageView2);
		mBut3 = (ImageView) findViewById(R.id.imageView3);
		mBut4 = (ImageView) findViewById(R.id.imageView4);
		findViewById(R.id.channel1).setOnClickListener(this);
		findViewById(R.id.channel2).setOnClickListener(this);
		findViewById(R.id.channel3).setOnClickListener(this);
		findViewById(R.id.channel4).setOnClickListener(this);
		mCateText1 = (TextView) findViewById(R.id.textView1);
		mCateText2 = (TextView) findViewById(R.id.textView2);
		mCateText3 = (TextView) findViewById(R.id.textView3);
		mCateText4 = (TextView) findViewById(R.id.textView4);

		unreadLabel = (TextView) findViewById(R.id.unread_msg_number);

	}

	private void prepareIntent() {

		mGardenIntent = new Intent(this, GardenPageActivity.class);
		mGardenIntent.putExtras(loginBundle);

		mMonitorItent = new Intent(this, FrontPageActivity.class);
		mMonitorItent.putExtras(loginBundle);

		mMsgIntent = new Intent(this, ChatAllHistoryActivity.class);
		mMsgIntent.putExtras(loginBundle);
		// mMsgIntent = new Intent(this, GardenComActivity.class);
		// mMsgIntent.putExtras(loginBundle);

		mMoreIntent = new Intent(this, MimePageActivity.class);
		mMoreIntent.putExtras(loginBundle);

	}

	private void setupIntent() {

		mTabHost = getTabHost();

		mTabHost.addTab(buildTabSpec(TAB_TAG_GARDEN, R.string.bottomtitle01,
				R.drawable.garden, mGardenIntent));

		mTabHost.addTab(buildTabSpec(TAB_TAG_MONITOR, R.string.bottomtitle02,
				R.drawable.monitor1, mMonitorItent));

		mTabHost.addTab(buildTabSpec(TAB_TAG_MSG, R.string.bottomtitle03,
				R.drawable.msg1, mMsgIntent));

		mTabHost.addTab(buildTabSpec(TAB_TAB_MORE, R.string.bottomtitle04,
				R.drawable.mime1, mMoreIntent));

	}

	public void showBottom() {
		bottomLayout.setVisibility(TabHost.VISIBLE);
	}

	public void hidBottom() {
		bottomLayout.setVisibility(TabHost.GONE);
	}

	private TabHost.TabSpec buildTabSpec(String tag, int resLabel, int resIcon,
			final Intent content) {
		return mTabHost
				.newTabSpec(tag)
				.setIndicator(getString(resLabel),
						getResources().getDrawable(resIcon))
				.setContent(content);
	}

	public static void setCurrentTabByTag(String tab) {
		mTabHost.setCurrentTabByTag(tab);
	}

	@Override
	public void onClick(View v) {

		// TODO Auto-generated method stub
		if (mCurTabId == v.getId()) {

			return;
		}

		mBut1.setImageResource(R.drawable.tab1_off);
		mBut2.setImageResource(R.drawable.tab2_off);
		mBut3.setImageResource(R.drawable.tab3_off);
		mBut4.setImageResource(R.drawable.tab4_off);
		mCateText1.setTextColor(COLOR1);
		mCateText2.setTextColor(COLOR1);
		mCateText3.setTextColor(COLOR1);
		mCateText4.setTextColor(COLOR1);
		int checkedId = v.getId();
		final boolean o;
		if (mCurTabId < checkedId)
			o = true;
		else
			o = false;
		if (o)
			mTabHost.getCurrentView().startAnimation(left_out);
		else
			mTabHost.getCurrentView().startAnimation(right_out);
		switch (checkedId) {

		case R.id.channel1:
			mTabHost.setCurrentTabByTag(TAB_TAG_GARDEN);
			mBut1.setImageResource(R.drawable.tab1_on);
			mCateText1.setTextColor(COLOR2);

			break;

		case R.id.channel2:
			mTabHost.setCurrentTabByTag(TAB_TAG_MONITOR);
			mBut2.setImageResource(R.drawable.tab2_on);
			mCateText2.setTextColor(COLOR2);

			break;
		case R.id.channel3:
			mTabHost.setCurrentTabByTag(TAB_TAG_MSG);
			mBut3.setImageResource(R.drawable.tab3_on);
			mCateText3.setTextColor(COLOR2);

			break;

		case R.id.channel4:
			mTabHost.setCurrentTabByTag(TAB_TAB_MORE);
			mBut4.setImageResource(R.drawable.tab4_on);
			mCateText4.setTextColor(COLOR2);

			break;
		default:
			break;
		}

		if (o)
			mTabHost.getCurrentView().startAnimation(left_in);
		else
			mTabHost.getCurrentView().startAnimation(right_in);
		mCurTabId = checkedId;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// ע���㲥������
		try {
			unregisterReceiver(msgReceiver);
		} catch (Exception e) {
		}
		try {
			unregisterReceiver(ackMessageReceiver);
		} catch (Exception e) {
		}
		// try {
		// unregisterReceiver(offlineMessageReceiver);
		// } catch (Exception e) {
		// }

		if (conflictBuilder != null) {
			conflictBuilder.create().dismiss();
			conflictBuilder = null;
		}

	}

	// /**
	// * ˢ��δ����Ϣ��
	// */
	// public void updateUnreadLabel() {
	// int count = getUnreadMsgCountTotal();
	// if (count > 0) {
	// unreadLabel.setText(String.valueOf(count));
	// unreadLabel.setVisibility(View.VISIBLE);
	// } else {
	// unreadLabel.setVisibility(View.INVISIBLE);
	// }
	// }
	//
	// /**
	// * ��ȡδ����Ϣ��
	// *
	// * @return
	// */
	// public int getUnreadMsgCountTotal() {
	// int unreadMsgCountTotal = 0;
	// unreadMsgCountTotal = EMChatManager.getInstance().getUnreadMsgsCount();
	// return unreadMsgCountTotal;
	// }

	/**
	 * ����Ϣ�㲥������
	 * 
	 * 
	 */
	private class NewMessageBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// ��ҳ���յ���Ϣ����ҪΪ����ʾδ����ʵ����Ϣ������Ҫ��chatҳ��鿴

			String from = intent.getStringExtra("from");
			// ��Ϣid
			String msgId = intent.getStringExtra("msgid");
			EMMessage message = EMChatManager.getInstance().getMessage(msgId);
			// 2014-10-22 �޸���ĳЩ�����ϣ�������ҳ��Է�����Ϣ����ʱ��������ʾ���ݵ�bug
			if (ChatActivity.activityInstance != null) {
				if (message.getChatType() == ChatType.GroupChat) {
					if (message.getTo().equals(
							ChatActivity.activityInstance.getToChatUsername()))
						return;
				} else {
					if (from.equals(ChatActivity.activityInstance
							.getToChatUsername()))
						return;
				}
			}

			// ע���㲥�����ߣ�������ChatActivity�л��յ�����㲥
			abortBroadcast();

			// ˢ��bottom bar��Ϣδ����
			// updateUnreadLabel();

		}
	}

	/**
	 * ��Ϣ��ִBroadcastReceiver
	 */
	private BroadcastReceiver ackMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			abortBroadcast();

			String msgid = intent.getStringExtra("msgid");
			String from = intent.getStringExtra("from");

			EMConversation conversation = EMChatManager.getInstance()
					.getConversation(from);
			if (conversation != null) {
				// ��message��Ϊ�Ѷ�
				EMMessage msg = conversation.getMessage(msgid);

				if (msg != null) {

					// 2014-11-5 �޸���ĳЩ�����ϣ�������ҳ��Է������Ѷ���ִʱ��������ʾ�Ѷ���bug
					if (ChatActivity.activityInstance != null) {
						if (msg.getChatType() == ChatType.Chat) {
							if (from.equals(ChatActivity.activityInstance
									.getToChatUsername()))
								return;
						}
					}

					msg.isAcked = true;
				}
			}

		}
	};

	/**
	 * ������ϢBroadcastReceiver sdk ��¼�󣬷�����������������Ϣ��client�����receiver����֪ͨUI
	 * ����Щ�˷�����������Ϣ UI ��������Ӧ�Ĳ��������������û���Ϣ
	 */
	// private BroadcastReceiver offlineMessageReceiver = new
	// BroadcastReceiver() {
	//
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// String[] users = intent.getStringArrayExtra("fromuser");
	// String[] groups = intent.getStringArrayExtra("fromgroup");
	// if (users != null) {
	// for (String user : users) {
	// System.out.println("�յ�user������Ϣ��" + user);
	// }
	// }
	// if (groups != null) {
	// for (String group : groups) {
	// System.out.println("�յ�group������Ϣ��" + group);
	// }
	// }
	// }
	// };

	private UserDao userDao;

	/**
	 * set head
	 * 
	 * @param username
	 * @return
	 */
	User setUserHead(String username) {
		User user = new User();
		user.setUsername(username);
		String headerName = null;
		if (!TextUtils.isEmpty(user.getNick())) {
			headerName = user.getNick();
		} else {
			headerName = user.getUsername();
		}
		if (username.equals(Constant.NEW_FRIENDS_USERNAME)) {
			user.setHeader("");
		} else if (Character.isDigit(headerName.charAt(0))) {
			user.setHeader("#");
		} else {
			user.setHeader(HanziToPinyin.getInstance()
					.get(headerName.substring(0, 1)).get(0).target.substring(0,
					1).toUpperCase());
			char header = user.getHeader().toLowerCase().charAt(0);
			if (header < 'a' || header > 'z') {
				user.setHeader("#");
			}
		}
		return user;
	}

	/**
	 * ���Ӽ���listener
	 * 
	 */
	private class MyConnectionListener implements EMConnectionListener {

		@Override
		public void onConnected() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// chatHistory.errorItem.setVisibility(View.GONE);
				}

			});
		}

		@Override
		public void onDisconnected(final int error) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (error == EMError.CONNECTION_CONFLICT) {
						// ��ʾ�ʺ��������豸��½dialog
						showConflictDialog();
					} else {
						// chatHistory.errorItem.setVisibility(View.VISIBLE);
						// if (NetUtils.hasNetwork(MainActivity.this))
						// chatHistory.errorText.setText("���Ӳ������������");
						// else
						// chatHistory.errorText.setText("��ǰ���粻���ã�������������");

					}
				}

			});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isConflict) {
			// updateUnreadLabel();
			EMChatManager.getInstance().activityResumed();
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(false);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private android.app.AlertDialog.Builder conflictBuilder;
	private boolean isConflictDialogShow;

	/**
	 * ��ʾ�ʺ��ڱ𴦵�¼dialog
	 */
	private void showConflictDialog() {
		isConflictDialogShow = true;
		VLCApplication.getInstance().logout();

		if (!MainActivity.this.isFinishing()) {
			// clear up global variables
			try {
				if (conflictBuilder == null)
					conflictBuilder = new android.app.AlertDialog.Builder(
							MainActivity.this);
				conflictBuilder.setTitle("����֪ͨ");
				conflictBuilder.setMessage(R.string.connect_conflict);
				conflictBuilder.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								conflictBuilder = null;
								finish();
								startActivity(new Intent(MainActivity.this,
										LoginActivity.class));
							}
						});
				conflictBuilder.setCancelable(false);
				conflictBuilder.create().show();
				isConflict = true;
			} catch (Exception e) {
				Log.e("###",
						"---------color conflictBuilder error" + e.getMessage());
			}

		}

	}

	// ������

	private final String TAG = this.getClass().getName();

	private final int UPDATA_NONEED = 0;
	private final int UPDATA_CLIENT = 1;
	private final int GET_UNDATAINFO_ERROR = 2;
	private final int SDCARD_NOMOUNTED = 3;
	private final int DOWN_ERROR = 4;

	private UpdataInfo info;
	private String localVersion;

	/*
	 * ��ȡ��ǰ����İ汾��
	 */
	private String getVersionName() throws Exception {
		// ��ȡpackagemanager��ʵ��
		PackageManager packageManager = getPackageManager();
		// getPackageName()���㵱ǰ��İ�����0�����ǻ�ȡ�汾��Ϣ
		PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(),
				0);
		return packInfo.versionName;
	}

	/*
	 * �ӷ�������ȡxml���������бȶ԰汾��
	 */
	public class CheckVersionTask implements Runnable {

		public void run() {
			try {

				// ����Դ�ļ���ȡ������ ��ַ
				String path = "";
				if (ipString.equals("218.60.13.9:7799")) {
					path = "http://" + ipString
							+ getResources().getString(R.string.url_update);
				} else {
					path = "http://" + ipString + ":7799"
							+ getResources().getString(R.string.url_update);
				}
				// ��װ��url�Ķ���
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setConnectTimeout(5000);
				InputStream is = conn.getInputStream();
				info = UpdataInfoParser.getUpdataInfo(is);
				System.out.println("VersionActivity----------->info = " + info);
				Double webVersion = Double.parseDouble(info.getVersion());
				Double phoneVersion = Double.parseDouble(localVersion);
				if (webVersion <= phoneVersion) {
					Log.i(TAG, "�汾����ͬ��������");
					Message msg = new Message();
					msg.what = UPDATA_NONEED;
					updateHandler.sendMessage(msg);
					// LoginMain();
				} else {
					Log.i(TAG, "�汾�Ų�ͬ ,��ʾ�û����� ");
					Message msg = new Message();
					msg.what = UPDATA_CLIENT;
					updateHandler.sendMessage(msg);
				}
			} catch (Exception e) {
				// ������
				Message msg = new Message();
				msg.what = GET_UNDATAINFO_ERROR;
				updateHandler.sendMessage(msg);
				e.printStackTrace();
			}
		}
	}

	Handler updateHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATA_NONEED:
				// Toast.makeText(getApplicationContext(), "������������°汾����������",
				// Toast.LENGTH_SHORT).show();
				// waitClose();
				break;
			case UPDATA_CLIENT:
				// �Ի���֪ͨ�û���������
				// Toast.makeText(getApplicationContext(), "����������°汾����������",
				// 1).show();
				showUpdataDialog();
				break;
			case GET_UNDATAINFO_ERROR:
				// ��������ʱ
				// Toast.makeText(getApplicationContext(), "��ȡ������������Ϣʧ��", 1)
				// .show();
				// waitClose();
				// LoginMain();
				break;
			case SDCARD_NOMOUNTED:
				// sdcard������
				Toast.makeText(getApplicationContext(), "SD��������", 1).show();
				break;
			case DOWN_ERROR:
				// ����apkʧ��
				Toast.makeText(getApplicationContext(), "�����°汾ʧ��", 1).show();
				// LoginMain();
				break;
			}
		}
	};

	/*
	 * 
	 * �����Ի���֪ͨ�û����³���
	 * 
	 * �����Ի���Ĳ��裺 1.����alertDialog��builder. 2.Ҫ��builder��������, �Ի��������,��ʽ,��ť
	 * 3.ͨ��builder ����һ���Ի��� 4.�Ի���show()����
	 */
	protected void showUpdataDialog() {
		AlertDialog.Builder builer = new Builder(this);
		builer.setTitle("�汾����");
		builer.setCancelable(false);
		builer.setMessage(info.getDescription());
		// ����ȷ����ťʱ�ӷ����������� �µ�apk Ȼ��װ
		builer.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "����apk,����");
				updateApk();
			}
		});
		// ����ȡ����ťʱ���е�¼
		builer.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				// LoginMain();
			}
		});
		AlertDialog dialog = builer.create();
		dialog.show();
	}

	/*
	 * �ӷ�����������APK
	 */
	protected void updateApk() {
		final ProgressDialog pd; // �������Ի���
		pd = new ProgressDialog(MainActivity.this);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage("�������ظ���");
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Message msg = new Message();
			msg.what = SDCARD_NOMOUNTED;
			updateHandler.sendMessage(msg);
		} else {
			pd.show();
			new Thread() {
				@Override
				public void run() {
					try {
						File file = DownLoadManager.getFileFromServer(
								info.getUrl(), pd);
						sleep(1000);
						installApk(file);
						pd.dismiss(); // �������������Ի���

					} catch (Exception e) {
						Message msg = new Message();
						msg.what = DOWN_ERROR;
						updateHandler.sendMessage(msg);
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	// ��װapk
	protected void installApk(File file) {
		Intent intent = new Intent();
		// ִ�ж���
		intent.setAction(Intent.ACTION_VIEW);
		// ִ�е���������
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		startActivity(intent);
	}

	boolean isExit = false;
	boolean hasExitTask = false;
	Timer exitTimer = new Timer();
	TimerTask exitTask = new TimerTask() {
		public void run() {
			isExit = false;
			hasExitTask = true;
		}
	};

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if (isExit) {
				exit();
			} else {
				isExit = true;
				Toast.makeText(this,
						getResources().getString(R.string.oneclickback),
						Toast.LENGTH_SHORT).show();
				if (!hasExitTask) {
					exitTimer.schedule(exitTask, 3000);
				}
			}
			return false;
		} else {
			return super.dispatchKeyEvent(event);
		}
	}

	/** �˳� */
	void exit() {
		finish();
		java.lang.System.exit(0);
	}

}