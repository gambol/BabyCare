/*****************************************************************************
 * VLCApplication.java
 *****************************************************************************
 * Copyright © 2010-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.videolan.libvlc;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.videolan.vlc.MediaDatabase;

import com.easemob.EMConnectionListener;
import com.easemob.EMError;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMChatOptions;
import com.easemob.chat.EMMessage;
import com.easemob.chat.OnNotificationClickListener;
import com.easemob.chat.EMMessage.ChatType;
import com.lnpdit.babycare.MainActivity;
import com.lnpdit.chat.ChatActivity;
import com.lnpdit.sqllite.DbOpenHelper;
import com.lnpdit.sqllite.User;
import com.lnpdit.sqllite.UserDao;
import com.lnpdit.util.PreferenceUtils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

public class VLCApplication extends Application {
	public final static String TAG = "VLC/VLCApplication";
	private static VLCApplication instance;

	public final static String SLEEP_INTENT = "org.videolan.vlc.SleepIntent";

	// ����

	public static Context applicationContext;
	// login user name
	public final String PREF_USERNAME = "username";
	private String userName = null;
	// login password
	private static final String PREF_PWD = "pwd";
	private String password = null;
	private Map<String, User> contactList;
	/**
	 * ��ǰ�û�nickname,Ϊ��ƻ�����Ͳ���userid�����ǳ�
	 */
	public static String currentUserNick = "";

	@Override
	public void onCreate() {
		super.onCreate();

		// CrashHandler crashHandler = CrashHandler.getInstance();
		// crashHandler.init(this);

		// Are we using advanced debugging - locale?

		int pid = android.os.Process.myPid();
		String processAppName = getAppName(pid);
		// ���ʹ�õ��ٶȵ�ͼ������������remote service�ĵ������⣬���if�жϲ�����
		if (processAppName == null || processAppName.equals("")) {
			// workaround for baidu location sdk
			// �ٶȶ�λsdk����λ����������һ�������Ľ��̣�ÿ�ζ�λ����������ʱ�򣬶������application::onCreate
			// �����µĽ��̡�
			// �����ŵ�sdkֻ��Ҫ���������г�ʼ��һ�Ρ� ������⴦���ǣ������pid �Ҳ�����Ӧ��processInfo
			// processName��
			// ���application::onCreate �Ǳ�service ���õģ�ֱ�ӷ���
			return;
		}

		applicationContext = this;
		instance = this;
		// ��ʼ������SDK,һ��Ҫ�ȵ���init()
		EMChat.getInstance().init(applicationContext);
		EMChat.getInstance().setDebugMode(true);
		Log.d("EMChat Demo", "initialize EMChat SDK");
		// debugmode��Ϊtrue�󣬾��ܿ���sdk��ӡ��log��

		// ��ȡ��EMChatOptions����
		EMChatOptions options = EMChatManager.getInstance().getChatOptions();
		// Ĭ����Ӻ���ʱ���ǲ���Ҫ��֤�ģ��ĳ���Ҫ��֤
		options.setAcceptInvitationAlways(false);
		// Ĭ�ϻ����ǲ�ά�����ѹ�ϵ�б�ģ����app�������ŵĺ��ѹ�ϵ���������������Ϊtrue
		options.setUseRoster(true);
		// �����յ���Ϣ�Ƿ�������Ϣ֪ͨ(����������ʾ)��Ĭ��Ϊtrue
		options.setNotifyBySoundAndVibrate(PreferenceUtils.getInstance(
				applicationContext).getSettingMsgNotification());
		// �����յ���Ϣ�Ƿ���������ʾ��Ĭ��Ϊtrue
		options.setNoticeBySound(PreferenceUtils
				.getInstance(applicationContext).getSettingMsgSound());
		// �����յ���Ϣ�Ƿ��� Ĭ��Ϊtrue
		options.setNoticedByVibrate(PreferenceUtils.getInstance(
				applicationContext).getSettingMsgVibrate());
		// ����������Ϣ�����Ƿ�����Ϊ���������� Ĭ��Ϊtrue
		options.setUseSpeaker(PreferenceUtils.getInstance(applicationContext)
				.getSettingMsgSpeaker());
		// ����notification��Ϣ���ʱ����ת��intentΪ�Զ����intent
		options.setOnNotificationClickListener(new OnNotificationClickListener() {

			@Override
			public Intent onNotificationClick(EMMessage message) {
				Intent intent = new Intent(applicationContext,
						ChatActivity.class);
				ChatType chatType = message.getChatType();
				if (chatType == ChatType.Chat) { // ������Ϣ
					intent.putExtra("userId", message.getFrom());
					intent.putExtra("chatType", ChatActivity.CHATTYPE_SINGLE);
				} else { // Ⱥ����Ϣ
							// message.getTo()ΪȺ��id
					intent.putExtra("groupId", message.getTo());
					intent.putExtra("chatType", ChatActivity.CHATTYPE_GROUP);
				}
				return intent;
			}
		});
		// ����һ��connectionlistener�����˻��ظ���½
		EMChatManager.getInstance().addConnectionListener(
				new MyConnectionListener());

		// // ȡ��ע�ͣ�app�ں�̨��������Ϣ��ʱ��״̬������Ϣ��ʾ�����Լ�д��
		// options.setNotifyText(new OnMessageNotifyListener() {
		//
		// @Override
		// public String onNewMessageNotify(EMMessage message) {
		// // ���Ը���message��������ʾ��ͬ����(�ɲο�΢�Ż�qq)��demo�򵥵ĸ�����ԭ������ʾ
		// return "��ĺû���" + message.getFrom() + "������һ����ϢŶ";
		// }
		//
		// @Override
		// public String onLatestMessageNotify(EMMessage message, int
		// fromUsersNum, int messageNum) {
		// return fromUsersNum + "�����ѣ�������" + messageNum + "����Ϣ";
		// }
		//
		// @Override
		// public String onSetNotificationTitle(EMMessage message) {
		// //�޸ı���
		// return "����notification";
		// }
		//
		//
		// });


		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String p = pref.getString("set_locale", "");
		if (p != null && !p.equals("")) {
			Locale locale;
			// workaround due to region code
			if (p.equals("zh-TW")) {
				locale = Locale.TRADITIONAL_CHINESE;
			} else if (p.startsWith("zh")) {
				locale = Locale.CHINA;
			} else if (p.equals("pt-BR")) {
				locale = new Locale("pt", "BR");
			} else if (p.equals("bn-IN") || p.startsWith("bn")) {
				locale = new Locale("bn", "IN");
			} else {
				/**
				 * Avoid a crash of java.lang.AssertionError: couldn't
				 * initialize LocaleData for locale if the user enters
				 * nonsensical region codes.
				 */
				if (p.contains("-"))
					p = p.substring(0, p.indexOf('-'));
				locale = new Locale(p);
			}
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		instance = this;

		// Initialize the database soon enough to avoid any race condition and
		// crash
		MediaDatabase.getInstance(this);
	}

	public static VLCApplication getInstance() {
		return instance;
	}

	/**
	 * Called when the overall system is running low on memory
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "System is running low on memory");

	}

	/**
	 * @return the main context of the Application
	 */
	public static Context getAppContext() {
		return instance;
	}

	/**
	 * @return the main resources from the Application
	 */
	public static Resources getAppResources() {
		if (instance == null)
			return null;
		return instance.getResources();
	}

	/**
	 * ��ȡ�ڴ��к���user list
	 *
	 * @return
	 */
	public Map<String, User> getContactList() {
		if (getUserName() != null && contactList == null) {
			UserDao dao = new UserDao(applicationContext);
			// ��ȡ���غ���user list���ڴ�,�����Ժ��ȡ����list
			contactList = dao.getContactList();
		}
		return contactList;
	}

	/**
	 * ���ú���user list���ڴ���
	 *
	 * @param contactList
	 */
	public void setContactList(Map<String, User> contactList) {
		this.contactList = contactList;
	}

	public void setStrangerList(Map<String, User> List) {

	}

	/**
	 * ��ȡ��ǰ��½�û���
	 *
	 * @return
	 */
	public String getUserName() {
		if (userName == null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
			userName = preferences.getString(PREF_USERNAME, null);
		}
		return userName;
	}

	/**
	 * ��ȡ����
	 *
	 * @return
	 */
	public String getPassword() {
		if (password == null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
			password = preferences.getString(PREF_PWD, null);
		}
		return password;
	}

	/**
	 * �����û���
	 *
	 * @param user
	 */
	public void setUserName(String username) {
		if (username != null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
			SharedPreferences.Editor editor = preferences.edit();
			if (editor.putString(PREF_USERNAME, username).commit()) {
				userName = username;
			}
		}
	}

	/**
	 * �������� �����ʵ������ ֻ��demo��ʵ�ʵ�Ӧ������Ҫ��password ���ܺ���� preference ����sdk
	 * �ڲ����Զ���¼��Ҫ�����룬�Ѿ����ܴ洢��
	 *
	 * @param pwd
	 */
	public void setPassword(String pwd) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		SharedPreferences.Editor editor = preferences.edit();
		if (editor.putString(PREF_PWD, pwd).commit()) {
			password = pwd;
		}
	}

	/**
	 * �˳���¼,�������
	 */
	public void logout() {
		// �ȵ���sdk logout��������app���Լ�������
		EMChatManager.getInstance().logout();
		DbOpenHelper.getInstance(applicationContext).closeDB();
		// reset password to null
		setPassword(null);
		setContactList(null);

	}

	private String getAppName(int pID) {
		String processName = null;
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List l = am.getRunningAppProcesses();
		Iterator i = l.iterator();
		PackageManager pm = this.getPackageManager();
		while (i.hasNext()) {
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
			try {
				if (info.pid == pID) {
					CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
					// Log.d("Process", "Id: "+ info.pid +" ProcessName: "+
					// info.processName +"  Label: "+c.toString());
					// processName = c.toString();
					processName = info.processName;
					return processName;
				}
			} catch (Exception e) {
				// Log.d("Process", "Error>> :"+ e.toString());
			}
		}
		return processName;
	}

	class MyConnectionListener implements EMConnectionListener {
		@Override
		public void onDisconnected(int error) {
			if (error == EMError.CONNECTION_CONFLICT) {
				Intent intent = new Intent(applicationContext, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("conflict", true);
				startActivity(intent);
			}

		}

		@Override
		public void onConnected() {
		}
	}
}
