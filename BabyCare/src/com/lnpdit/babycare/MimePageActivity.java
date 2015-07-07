package com.lnpdit.babycare;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.lnpdit.garden.GardenReplyManageActivity;
import com.lnpdit.sqllite.BBGJDB;
import com.lnpdit.util.DownLoadManager;
import com.lnpdit.util.UpdataInfo;
import com.lnpdit.util.UpdataInfoParser;
import com.lnpdit.util.passwordInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MimePageActivity extends passwordInterface implements
		OnClickListener {

	private TextView txtRealName;
	private TextView txtPhoneNumber;
	private TextView txtGardenName;

	private RelativeLayout mime_layout_04;
	private RelativeLayout mime_layout_05;
	private RelativeLayout mime_layout_06;
	private Button exitBtn;

	public static final String SETTING_INFOS = "SETTING_Infos";
	public static final String SERIP = "serverIp";
	public static final String NAME = "name";
	public static final String PASS = "pass";
	public static final String REMEMBER = "remember";
	public static final String AUTOLOG = "autolog";

	Bundle loginBundle;
	private static String userName = "";
	private static String realName = "";
	private static String ipString = "";
	private static String gardenName = "";

	int user_state;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mimepage);

		init();

		mime_layout_04 = (RelativeLayout) findViewById(R.id.mime_layout_04);
		mime_layout_04.setOnClickListener(this);

		mime_layout_05 = (RelativeLayout) findViewById(R.id.mime_layout_05);
		mime_layout_05.setOnClickListener(this);

		mime_layout_06 = (RelativeLayout) findViewById(R.id.mime_layout_06);
		mime_layout_06.setOnClickListener(this);

		exitBtn = (Button) findViewById(R.id.exit_login_btn);
		exitBtn.setOnClickListener(this);
	}

	private void init() {
		loginBundle = this.getIntent().getExtras();

		ipString = loginBundle.getString("serverIp").toString();
		userName = loginBundle.getString("username").toString();
		realName = loginBundle.getString("realName").toString();
		gardenName = loginBundle.getString("kinderName").toString();

		txtRealName = (TextView) findViewById(R.id.realname);
		txtPhoneNumber = (TextView) findViewById(R.id.phonenum);
		txtGardenName = (TextView) findViewById(R.id.garden_text);

		txtRealName.setText(realName);
		txtPhoneNumber.setText(userName);
		txtGardenName.setText(gardenName);

		int res = checkUser();

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {

		// case R.id.mime_layout_01:
		//
		// Intent mimeIntent=new Intent(MimePageActivity.this,
		// TestActivity.class);
		// startActivity(mimeIntent);
		//
		// break;

		case R.id.mime_layout_04:

			if (user_state == 0) {
				Intent intent = new Intent();
				intent.setClass(MimePageActivity.this, LoginActivity.class);
				startActivity(intent);
				Toast.makeText(this, "���Ƚ��е�¼����", Toast.LENGTH_SHORT).show();
			} else {
				Intent intent = new Intent();
				intent.setClass(MimePageActivity.this,
						GardenReplyManageActivity.class);
				BBGJDB tdd = new BBGJDB(this);
				Cursor cursor = tdd.selectuser();
				cursor.moveToFirst();
				intent.putExtra("ID", cursor.getString(1)).toString();
				startActivity(intent);
				break;
			}

			break;

		case R.id.mime_layout_05:

			// ������
			try {
				showWait(getResources().getString(R.string.updating));
				localVersion = getVersionName();
				CheckVersionTask cv = new CheckVersionTask();
				new Thread(cv).start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;

		case R.id.mime_layout_06:

			Intent aboutIntent = new Intent(MimePageActivity.this,
					AboutActivity.class);
			startActivity(aboutIntent);

			break;

		case R.id.exit_login_btn:

			new AlertDialog.Builder(this)
					.setMessage("ȷ���˳���¼��")
					.setPositiveButton("ȷ��",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									Intent intent = new Intent(
											MimePageActivity.this,
											LoginActivity.class);
									startActivity(intent);
									finish();
								}

							}).setNegativeButton("ȡ��", null).create().show();
			break;

		default:
			break;
		}
	}

	private int checkUser() {
		BBGJDB tdd = new BBGJDB(this);
		Cursor cursor = tdd.selectuser();
		if (cursor.getCount() == 0) {
			user_state = 0;
		} else {
			cursor.moveToFirst();
			String usr_name = cursor.getString(2);
			user_state = 1;
		}
		return user_state;
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
					handler.sendMessage(msg);
					// LoginMain();
				} else {
					Log.i(TAG, "�汾�Ų�ͬ ,��ʾ�û����� ");
					Message msg = new Message();
					msg.what = UPDATA_CLIENT;
					handler.sendMessage(msg);
				}
			} catch (Exception e) {
				// ������
				Message msg = new Message();
				msg.what = GET_UNDATAINFO_ERROR;
				handler.sendMessage(msg);
				e.printStackTrace();
			}
		}
	}

	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATA_NONEED:
				Toast.makeText(getApplicationContext(), "������������°汾����������",
						Toast.LENGTH_SHORT).show();
				waitClose();
				break;
			case UPDATA_CLIENT:
				// �Ի���֪ͨ�û���������
				// Toast.makeText(getApplicationContext(), "��������������~",
				// 1).show();
				showUpdataDialog();
				waitClose();
				break;
			case GET_UNDATAINFO_ERROR:
				// ��������ʱ
				Toast.makeText(getApplicationContext(), "��ȡ������������Ϣʧ��", 1)
						.show();
				waitClose();
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
				downLoadApk();
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
	protected void downLoadApk() {
		final ProgressDialog pd; // �������Ի���
		pd = new ProgressDialog(MimePageActivity.this);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage("�������ظ���");
		pd.setCanceledOnTouchOutside(false);
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Message msg = new Message();
			msg.what = SDCARD_NOMOUNTED;
			handler.sendMessage(msg);
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
						handler.sendMessage(msg);
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

}
