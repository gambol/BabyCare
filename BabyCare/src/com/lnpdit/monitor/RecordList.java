package com.lnpdit.monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

import com.company.Demo.RecordPlayActivity;
import com.lnpdit.babycare.R;
import com.lnpdit.file.FileActivityHelper;
import com.lnpdit.file.FileAdapter;
import com.lnpdit.file.FileInfo;
import com.lnpdit.file.FileUtil;

public class RecordList extends ListActivity implements OnClickListener {
	// private TextView _filePath;
	private List<FileInfo> _files;
	private String _rootPath = FileUtil.getSDPath();
	// private String _currentPath = _rootPath;
	String _currentPath = "";
	private final String TAG = "Main";
	private final int MENU_RENAME = Menu.FIRST;
	private final int MENU_COPY = Menu.FIRST + 3;
	private final int MENU_MOVE = Menu.FIRST + 4;
	private final int MENU_DELETE = Menu.FIRST + 5;
	private final int MENU_INFO = Menu.FIRST + 6;

	private TextView recordlist_back;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recordlist);

		recordlist_back = (TextView) findViewById(R.id.recordlist_back);
		recordlist_back.setOnClickListener(this);

		_currentPath = checkRecordPath();

		// _filePath = (TextView) findViewById(R.id.file_path);

		// ��ȡ��ǰĿ¼���ļ��б�
		viewFiles(_currentPath);

		// �󶨳����¼�
		// getListView().setOnItemLongClickListener(_onItemLongClickListener);

		// ע�������Ĳ˵�
		registerForContextMenu(getListView());
	}

	/** �����Ĳ˵� **/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = null;

		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return;
		}

		FileInfo f = _files.get(info.position);
		menu.setHeaderTitle(f.Name);
		// menu.add(0, MENU_RENAME, 1, getString(R.string.file_rename));
		// menu.add(0, MENU_COPY, 2, getString(R.string.file_copy));
		// menu.add(0, MENU_MOVE, 3, getString(R.string.file_move));
		menu.add(0, MENU_DELETE, 1, getString(R.string.file_delete));
		menu.add(0, MENU_INFO, 2, getString(R.string.file_info));
	}

	/** �����Ĳ˵��¼����� **/
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		FileInfo fileInfo = _files.get(info.position);
		final File f = new File(fileInfo.Path);
		switch (item.getItemId()) {
		case MENU_RENAME:
			FileActivityHelper.renameFile(RecordList.this, f,
					renameFileHandler);
			return true;
		case MENU_DELETE:

			new AlertDialog.Builder(RecordList.this)
					.setTitle("��ʾ")
					.setMessage("ȷ��ɾ����?")
					.setPositiveButton("ȷ��",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									FileUtil.deleteFile(f);
									viewFiles(_currentPath);
								}
							})
					.setNegativeButton("ȡ��",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0,
										int arg1) {
									// TODO Auto-generated method stub

								}
							}).show();

			return true;
		case MENU_INFO:
			FileActivityHelper.viewFileInfo(RecordList.this, f);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/** �б�����¼����� **/
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		FileInfo f = _files.get(position);

		if (f.IsDirectory) {
			viewFiles(f.Path);
		} else {
			if (f.Path.contains("dav")) {
				Intent intent = new Intent(RecordList.this,
						RecordPlayActivity.class);
				intent.putExtra("path", f.Path);

				startActivity(intent);

			} else if (f.Path.contains("record") || f.Path.contains("R")) {
				playRecord(f.Path);

			}
		}
	}

	private void playRecord(String videoPath) {
		String strend = "";
		if (videoPath.toLowerCase().endsWith(".mp4")) {
			strend = "mp4";
		} else if (videoPath.toLowerCase().endsWith(".3gp")) {
			strend = "3gpp";
		} else if (videoPath.toLowerCase().endsWith(".mov")) {
			strend = "mov";
		} else if (videoPath.toLowerCase().endsWith(".wmv")) {
			strend = "wmv";
		} else if (videoPath.toLowerCase().endsWith(".asf")) {
			strend = "x-ms-asf";
		} else if (videoPath.toLowerCase().endsWith(".avi")) {
			strend = "x-msvideo";
		}

		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			Uri uri = Uri.parse("file://" + videoPath);
			intent.setDataAndTypeAndNormalize(uri, "video/" + strend);
			startActivity(intent);
		} catch (Exception e) {
			// TODO: handle exception
			Toast toast = Toast.makeText(RecordList.this, "�밲װ��Ƶ��������ۿ�¼���ļ���",
					Toast.LENGTH_SHORT);

		}

		// Intent it = new Intent(Intent.ACTION_VIEW);
		// Uri uri = Uri.parse(videoPath);
		// it.setDataAndType(uri, "video/mp4");
		// startActivity(it);
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
		case R.id.recordlist_back:

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

	/** ��ȡ��PasteFile���ݹ�����·�� **/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Activity.RESULT_OK == resultCode) {
			Bundle bundle = data.getExtras();
			if (bundle != null && bundle.containsKey("CURRENTPATH")) {
				viewFiles(bundle.getString("CURRENTPATH"));
			}
		}
	}

	/** ��ȡ��Ŀ¼�������ļ� **/
	private void viewFiles(String filePath) {
		ArrayList<FileInfo> tmp = FileActivityHelper.getFiles(
				RecordList.this, filePath);
		if (tmp != null) {
			// �������
			if (_files != null) {
				_files.clear();
			}

			_files = tmp;
			// ���õ�ǰĿ¼
			_currentPath = filePath;
			// _filePath.setText(filePath);
			// ������
			setListAdapter(new FileAdapter(this, _files));
		}
	}

	/** �����¼����� **/
	/**
	 * private OnItemLongClickListener _onItemLongClickListener = new
	 * OnItemLongClickListener() {
	 * 
	 * @Override public boolean onItemLongClick(AdapterView<?> parent, View
	 *           view, int position, long id) { Log.e(TAG, "position:" +
	 *           position); return true; } };
	 **/

	/** ���ļ� **/
	private void openFile(String path) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);

		File f = new File(path);
		String type = FileUtil.getMIMEType(f.getName());
		intent.setDataAndType(Uri.fromFile(f), type);
		startActivity(intent);
	}

	/** �������ص�ί�� **/
	private final Handler renameFileHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0)
				viewFiles(_currentPath);
		}
	};

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
}
