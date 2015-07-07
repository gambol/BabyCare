package com.lnpdit.babycare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.videolan.libvlc.VLCApplication;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMGroupManager;
import com.easemob.util.EMLog;
import com.easemob.util.HanziToPinyin;
import com.lnpdit.photo.Constant;
import com.lnpdit.sqllite.User;
import com.lnpdit.sqllite.UserDao;
import com.lnpdit.util.ConnectionDetector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class LoadingActivity extends Activity {
	private SharedPreferences sp;

	public static final String SETTING_INFOS = "SETTING_Infos";
	public static final String SERIP = "serverIp";
	public static final String NAME = "name";
	public static final String PASS = "pass";
	public static final String REMEMBER = "remember";
	public static final String AUTOLOG = "autolog";

	private String serverIp = "";
	private String socketIp = "";
	private String name = "";
	private String pass = "";
	private String remember = "";

	Handler handler;

	// ��½���ؼ�ʱ��
	private int timerCount = 0;
	private Timer timer;
	private boolean isWaiting = true;
	private Thread threadLoading;
	private Thread webLoading;

	// ��½״̬
	Boolean status = false;
	// �������״̬
	Boolean isInternetPresent = false;
	ConnectionDetector cd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_page);

		sp = getSharedPreferences(SETTING_INFOS, 0);

		handler = new Handler();

		cd = new ConnectionDetector(getApplicationContext());
		isInternetPresent = cd.isConnectingToInternet();
		if (!isInternetPresent) {
			Toast.makeText(this, getResources().getString(R.string.loginerror),
					Toast.LENGTH_SHORT).show();
			return;
		} else {

			if (sp != null) {

				socketIp = sp.getString(SERIP, "");
				name = sp.getString(NAME, "");
				pass = sp.getString(PASS, "");
				remember = sp.getString(REMEMBER, "");

				if (remember.equals("yes")) {
					timer = new Timer();
					threadLoading = new Thread(waitThread);
					threadLoading.start();
					webLoading = new Thread(newWebThread);
					webLoading.start();
				} else {
					Intent intent = new Intent(LoadingActivity.this,
							LoginActivity.class);
					startActivity(intent);
					finish();
				}

			} else {
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						Intent intent = new Intent(LoadingActivity.this,
								LoginActivity.class);
						startActivity(intent);
						finish();
					}
				}, 2000);
			}
		}
	}

	private String getPhoneNumberType() {

		try {

			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			// imsi �����ƶ��û�ʶ����
			String imsi = tm.getSubscriberId();

			if (imsi != null) {
				if (imsi.startsWith("46000") || imsi.startsWith("46002")
						|| imsi.startsWith("46007")) {
					// �й��ƶ�
					Log.v("��Ӫ��:", "�й��ƶ�" + " " + imsi);
					return "1";
				} else if (imsi.startsWith("46001") || imsi.startsWith("46006")) {
					// �й���ͨ
					Log.v("��Ӫ��:", "�й���ͨ" + " " + imsi);
					return "2";
				} else if (imsi.startsWith("46003") || imsi.startsWith("46005")) {
					// �й�����
					Log.v("��Ӫ��:", "�й�����" + " " + imsi);
					return "3";
				} else {
					// �޷��ж�
					Log.v("��Ӫ��", "�޷��ж���Ӫ����Ϣ" + " " + imsi);
					return "0";
				}

			} else {
				// imsiΪ��
				return "0";
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "0";
		}
	}

	// �ȴ���ʾ�����
	private void TimerCount(Timer t) {
		if (status) {
			t.cancel();
			// handler.post(runnableHidProgress);
		} else {
			timerCount++;
			if (timerCount > 40) {
				t.cancel();
				runOnUiThread(runnableRunOver);
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

	// �µĵ�¼����

	Runnable newWebThread = new Runnable() {
		public void run() {
			try {
				String NAMESPACE = "MobileNewspaper";
				String METHOD_NAME = "NewLogin";
				String serverIp = "";

				serverIp = socketIp;

				String URL = "";
				if (serverIp.equals("218.60.13.9:7799")) {
					URL = "http://" + serverIp
							+ getResources().getString(R.string.url_server);
				} else {
					URL = "http://" + serverIp + ":7799"
							+ getResources().getString(R.string.url_server);
				}
				String SOAP_ACTION = "MobileNewspaper/NewLogin";

				SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
				rpc.addProperty("userName", name);
				rpc.addProperty("passWord", pass);

				SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
						SoapEnvelope.VER11);
				envelope.bodyOut = rpc;
				envelope.dotNet = true;
				envelope.setOutputSoapObject(rpc);

				HttpTransportSE ht = new HttpTransportSE(URL);

				ht.debug = true;
				ht.call(SOAP_ACTION, envelope);
				// Object object = envelope.getResponse();
				// System.out.print("rpc:");
				// System.out.println(rpc);
				//
				// String result = object.toString();
				String yysString = getPhoneNumberType();
				if (yysString.equals("0")) {

				}
				SoapObject login = (SoapObject) envelope.getResponse();

				int count = login.getPropertyCount();

				String userId = "0";
				String comId = "0";
				String realName = "0";
				String empId = "0";

				String ifExist = "0";
				String ifBbyy = "1";
				String ifBbgj = "1";
				String ifKqgl = "1";
				String ifBbhy = "1";

				String ifVideo = "0";
				String ifPtz = "0";
				String ifRecord = "0";
				String ifSnap = "0";
				String ifMap = "0";

				String ifFavor = "0";
				String ifDistance = "0";
				String ifKinder = "0";
				String ifUpload = "0";
				String ifNews = "0";
				String yys = "0";
				String ifPay = "0";
				String endDate = "";
				String payStatus = "0";
				String ifonly = "0";
				String userType = "�ҳ�";
				String kinderName = "С��ͯ�׶�԰";

				ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
				for (int i = 0; i < count; i++) {
					SoapObject soapchilds = (SoapObject) login.getProperty(i);

					try {

						ifExist = soapchilds.getProperty("Id").toString();
						ifVideo = soapchilds.getProperty("Video").toString();
						ifPtz = soapchilds.getProperty("Userptz").toString();
						ifRecord = soapchilds.getProperty("Record").toString();
						ifSnap = soapchilds.getProperty("Snap").toString();
						ifMap = soapchilds.getProperty("Map").toString();
						yys = soapchilds.getProperty("Yys").toString();
						ifFavor = soapchilds.getProperty("Favor").toString();
						ifDistance = soapchilds.getProperty("Distance")
								.toString();
						ifKinder = soapchilds.getProperty("Kinder").toString();
						ifUpload = soapchilds.getProperty("Upload").toString();
						ifNews = soapchilds.getProperty("News").toString();
						ifonly = soapchilds.getProperty("Status").toString();
						ifPay = soapchilds.getProperty("Pay").toString();
						endDate = soapchilds.getProperty("Enddate").toString();
						payStatus = soapchilds.getProperty("PayStatus")
								.toString();
						userId = soapchilds.getProperty("UserId").toString();
						userType = soapchilds.getProperty("KinderType")
								.toString();
						kinderName = soapchilds.getProperty("KinderName")
								.toString();
						comId = soapchilds.getProperty("ComId").toString();
						realName = soapchilds.getProperty("RealName")
								.toString();
						empId = soapchilds.getProperty("EmpId").toString();
						ifBbyy = soapchilds.getProperty("Bbyy").toString();
						ifBbgj = soapchilds.getProperty("Bbgj").toString();
						ifKqgl = soapchilds.getProperty("Kqgl").toString();
						ifBbhy = soapchilds.getProperty("Bbhy").toString();
					} catch (Exception e) {
						// TODO: handle exception
						ifBbyy = "1";
						ifBbgj = "1";
						ifKqgl = "1";
						ifBbhy = "1";
					}
					if (ifExist.equals("0")) {
						runOnUiThread(runnableWaitting);
						return;

					}

					if (ifExist.equals("2")) {
						runOnUiThread(lockWaitting);
						return;
					}

					if (!yys.equals("0")) { // ���������Ӫ�̣����ж�
						if (yysString.equals("0")) {
							runOnUiThread(noYys);
							return;
						}

						if (!yysString.equals(yys)) {
							runOnUiThread(wrongYys);
							return;
						}
					}

					String ID = soapchilds.getProperty("Id").toString();
					String devName = soapchilds.getProperty("DevName")
							.toString();
					String chName = soapchilds.getProperty("ChName").toString();
					String devId = soapchilds.getProperty("DevId").toString();
					String ip = soapchilds.getProperty("Ip").toString();
					String port = soapchilds.getProperty("DevPort").toString();
					String chNo = soapchilds.getProperty("ChNo").toString();
					String listCount = soapchilds.getProperty("ListCount")
							.toString();
					String listNo = soapchilds.getProperty("ListNo").toString();
					String width = soapchilds.getProperty("Width").toString();
					String height = soapchilds.getProperty("Height").toString();
					String longitude = soapchilds.getProperty("Longitude")
							.toString();
					String latitude = soapchilds.getProperty("Latitude")
							.toString();
					String adapterId = soapchilds.getProperty("AdapterId")
							.toString();
					String ptz = soapchilds.getProperty("Ptz").toString();
					String zoom = "0";
					String talk = "0";
					String rtsp = "0";
					String devUserName = "admin";
					String devPassWord = "admin";
					String type = "DH";
					try {
						zoom = soapchilds.getProperty("Zoom").toString();
						talk = soapchilds.getProperty("Talk").toString();
						rtsp = soapchilds.getProperty("Rtsp").toString();
						devUserName = soapchilds.getProperty("Username")
								.toString();
						devPassWord = soapchilds.getProperty("Password")
								.toString();
						type = soapchilds.getProperty("Type").toString();
					} catch (Exception e) {
						// TODO: handle exception
					}
					String stayLine = soapchilds.getProperty("Stayline")
							.toString();

					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("ID", ID); // ID
					map.put("devName", devName);// �б���ʾ���豸���ƣ����硰չ�����豸��
					map.put("chName", chName); // ͨ�����ƣ�д������ͷ1������
					map.put("devId", devId); // �豸���кţ��������Ҫ��ÿ���豸��ͬ��������д�롰DHPZB2LN26800002������
					map.put("ip", ip); // �����������IP����59.46.115.85,������д�롰200.20.32.200������
					map.put("port", port); //
					map.put("chNo", chNo);// ��1����
					map.put("listCount", listCount);// �豸����
					map.put("listNo", listNo);// ���豸�е�˳��
					map.put("width", width); // д352
					map.put("height", height); // д288
					map.put("longitude", longitude);// ���ȣ������Ǹ�չ���ľ���
					map.put("latitude", latitude);// γ�ȣ������Ǹ�չ����γ��
					map.put("adapterId", adapterId);// ����ID
					map.put("stayLine", stayLine);// ��1
					map.put("ptz", ptz);// �Ƿ�֧����̨�����֧�֣���1����֧�֣���0
					map.put("zoom", zoom);// �Ƿ�֧����̨�����֧�֣���1����֧�֣���0
					map.put("talk", talk);// �Ƿ�֧����̨�����֧�֣���1����֧�֣���0
					map.put("rtsp", rtsp);// �Ƿ�֧����̨�����֧�֣���1����֧�֣���0
					map.put("devUserName", devUserName);
					map.put("devPassWord", devPassWord);
					map.put("type", type);
					listItem.add(map);
				}

				// // ����sdk��½������½���������
				// EMChatManager.getInstance().login(name, pass,
				// new EMCallBack() {
				//
				// @Override
				// public void onSuccess() {
				//
				// // ��½�ɹ��������û�������
				// VLCApplication.getInstance().setUserName(
				// name);
				// VLCApplication.getInstance().setPassword(
				// pass);
				// // runOnUiThread(new Runnable() {
				// // public void run() {
				// // pd.setMessage("���ڻ�ȡ���Ѻ�Ⱥ���б�...");
				// // }
				// // });
				// try {
				// // ** ��һ�ε�¼����֮ǰlogout�󣬼������б���Ⱥ�ͻػ�
				// // ** manually load all local groups and
				// // conversations in case we are auto login
				// EMGroupManager.getInstance()
				// .loadAllGroups();
				// EMChatManager.getInstance()
				// .loadAllConversations();
				//
				// // demo�м򵥵Ĵ����ÿ�ε�½��ȥ��ȡ����username���������Լ������������
				// List<String> usernames = EMContactManager
				// .getInstance()
				// .getContactUserNames();
				// EMLog.d("roster", "contacts size: "
				// + usernames.size());
				// Map<String, User> userlist = new HashMap<String, User>();
				// for (String username : usernames) {
				// User user = new User();
				// user.setUsername(username);
				// setUserHearder(username, user);
				// userlist.put(username, user);
				// }
				// // ���user"������֪ͨ"
				// User newFriends = new User();
				// newFriends
				// .setUsername(Constant.NEW_FRIENDS_USERNAME);
				// newFriends.setNick("������֪ͨ");
				// newFriends.setHeader("");
				// userlist.put(Constant.NEW_FRIENDS_USERNAME,
				// newFriends);
				// // ���"Ⱥ��"
				// User groupUser = new User();
				// groupUser
				// .setUsername(Constant.GROUP_USERNAME);
				// groupUser.setNick("Ⱥ��");
				// groupUser.setHeader("");
				// userlist.put(Constant.GROUP_USERNAME,
				// groupUser);
				//
				// // �����ڴ�
				// VLCApplication.getInstance()
				// .setContactList(userlist);
				// // ����db
				// UserDao dao = new UserDao(
				// LoadingActivity.this);
				// List<User> users = new ArrayList<User>(
				// userlist.values());
				// dao.saveContactList(users);
				//
				// // ��ȡȺ���б�(Ⱥ����ֻ��groupid��groupname�ļ���Ϣ),sdk���Ⱥ����뵽�ڴ��db��
				// EMGroupManager.getInstance()
				// .getGroupsFromServer();
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// boolean updatenick = EMChatManager
				// .getInstance().updateCurrentUserNick(
				// VLCApplication.currentUserNick);
				// if (!updatenick) {
				// EMLog.e("LoginActivity",
				// "update current user nick fail");
				// }
				// }
				//
				// @Override
				// public void onProgress(int progress, String status) {
				//
				// }
				//
				// @Override
				// public void onError(int code, final String message) {
				//
				// // runOnUiThread(new Runnable() {
				// // public void run() {
				// // pd.dismiss();
				// // Toast.makeText(getApplicationContext(),
				// // "��¼ʧ��: " + message, 0).show();
				// //
				// // }
				// // });
				// }
				// });
				// Intent intent = new Intent(Landing.this,
				// ShowView.class);// ΪIntent������Ҫ��������

				Intent intent = new Intent(LoadingActivity.this,
						MainActivity.class);// ΪIntent������Ҫ��������
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList("videoList", (ArrayList) listItem);
				bundle.putString("userId", userId);
				bundle.putString("comId", comId);
				bundle.putString("realName", realName);
				bundle.putString("empId", empId);
				bundle.putString("username", name);
				bundle.putString("password", pass);
				bundle.putString("socketIp", socketIp);
				bundle.putString("serverIp", serverIp);
				bundle.putString("ifBbyy", ifBbyy);
				bundle.putString("ifBbgj", ifBbgj);
				bundle.putString("ifKqgl", ifKqgl);
				bundle.putString("ifBbhy", ifBbhy);
				bundle.putString("ifVideo", ifVideo);
				bundle.putString("ifPtz", ifPtz);
				bundle.putString("ifRecord", ifRecord);
				bundle.putString("ifSnap", ifSnap);
				bundle.putString("ifMap", ifMap);
				bundle.putString("ifFavor", ifFavor);
				bundle.putString("ifDistance", ifDistance);
				bundle.putString("ifKinder", ifKinder);
				bundle.putString("ifUpload", ifUpload);
				bundle.putString("ifNews", ifNews);
				bundle.putString("ifPay", ifPay);
				bundle.putString("endDate", endDate);
				bundle.putString("payStatus", payStatus);
				bundle.putString("userType", userType);
				bundle.putString("kinderName", kinderName);
				// bundle.putString("name", nameString);
				// bundle.putString("passWord", passWordString);
				intent.putExtras(bundle);
				// handler.post(runnableHidProgress);

				status = true;
				startActivity(intent);
				LoadingActivity.this.finish();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	// ������ʾ
	Runnable runnableWaitting = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(LoadingActivity.this, "�û�����������������֤�����룡",
					Toast.LENGTH_LONG).show();
		}

	};

	// ������ʾ
	Runnable lockWaitting = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(LoadingActivity.this, "���û��ѱ����������ܵ�¼��",
					Toast.LENGTH_LONG).show();
		}

	};

	// �޷���ȡ��Ӫ����ʾ
	Runnable noYys = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(LoadingActivity.this, "�޷���ȡ��Ӫ����Ϣ�����ܵ�¼��",
					Toast.LENGTH_LONG).show();
		}

	};

	// ��Ӫ�̲�������ʾ
	Runnable wrongYys = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(LoadingActivity.this, "������Ӫ����Ϣ�����ϣ����ܵ�¼��",
					Toast.LENGTH_LONG).show();
		}

	};

	// ��ʱ��ʾ
	Runnable runnableRunOver = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(LoadingActivity.this, "���粻�ȶ������Ժ����ԣ�",
					Toast.LENGTH_SHORT).show();
		}

	};

	// ���ŵ�¼

	/**
	 * ����hearder���ԣ�����ͨѶ�ж���ϵ�˰�header������ʾ���Լ�ͨ���Ҳ�ABCD...��ĸ�����ٶ�λ��ϵ��
	 * 
	 * @param username
	 * @param user
	 */
	protected void setUserHearder(String username, User user) {
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

	/** ���ؼ����ؼ��� */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
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
		}
		return false;
	}

	/** �˳� */
	void exit() {
		finish();
		java.lang.System.exit(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		sp = null;
	}

}
