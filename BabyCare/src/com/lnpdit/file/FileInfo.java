package com.lnpdit.file;

import com.lnpdit.babycare.R;

/** ��ʾһ���ļ�ʵ�� **/
public class FileInfo {
	public String Name;
	public String Path;
	public long Size;
	public boolean IsDirectory = false;
	public int FileCount = 0;
	public int FolderCount = 0;

	public int getIconResourceId() {
		if (IsDirectory) {
			return R.drawable.record;
		}
		return R.drawable.record;
	}
}