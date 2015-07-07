package com.lnpdit.garden;

import android.os.Bundle;
import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import android.widget.TextView;
import android.view.KeyEvent;

import com.lnpdit.babycare.MainActivity;
import com.lnpdit.babycare.R;

public class GardenFoodActivity extends Activity implements OnClickListener {

	private TextView gardenfood_back;
	private WebView web_food;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.gardenfood);

		gardenfood_back = (TextView) findViewById(R.id.gardenfood_back);
		gardenfood_back.setOnClickListener(this);

		SharedPreferences share = getSharedPreferences("BBGJ_UserInfo",
				Activity.MODE_WORLD_READABLE);
		int comId = share.getInt("comId", 0);
		web_food = (WebView) findViewById(R.id.web_food);
		web_food.getSettings().setJavaScriptEnabled(true);
		web_food.getSettings().setPluginState(PluginState.ON);
		web_food.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		web_food.getSettings().setAllowFileAccess(true);
		web_food.getSettings().setDefaultTextEncodingName("UTF-8");
		web_food.getSettings().setLoadWithOverviewMode(true);
		web_food.getSettings().setUseWideViewPort(true);
		web_food.loadUrl(MainActivity.IP + "/mobile/foodWeb.aspx?ComId="
				+ comId);
		web_food.setWebViewClient(new WebViewClient() {
			// ����������û���ͼ�㿪ҳ���ϵ�ĳ������ʱ������
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url != null) {
					// ������������Ŀ��ҳ���������������
					view.loadUrl(url);
					// ���������url����Ŀ����ַ��������ȡĿ����ҳ���������������HTTP��API����ҳ��������
				}
				// ����true��ʾͣ���ڱ�WebView������ת��ϵͳ���������
				return true;
			}
		});

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
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.gardenfood_back:

			this.finish();

			break;

		default:
			break;
		}
	}

	/** �ض��巵�ؼ��¼� **/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// ����back����
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
