package com.lnpdit.babycare;



import android.support.v4.app.FragmentActivity;

import com.easemob.chat.EMChatManager;

public class BaseActivity extends FragmentActivity{
	@Override
	protected void onResume() {
		super.onResume();
		//onresumeʱ��ȡ��notification��ʾ
		EMChatManager.getInstance().activityResumed();
	}
}
