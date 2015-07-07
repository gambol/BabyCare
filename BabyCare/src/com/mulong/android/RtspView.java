package com.mulong.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import com.lnpdit.file.FileUtil;
import com.lnpdit.photo.ZoomState;
import com.lnpdit.util.ByteInfo;
import com.lnpdit.util.ComUtil;

import android.R.integer;
import android.R.string;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Toast;


public class RtspView extends View implements Runnable, Observer {

	Bitmap mBitQQ = null;

	Bitmap mSCBitmap = null;

	int width; // �˴��趨��ʼ�ֱ���
	int height;

	int workmode = -1;

	// dst address parameter
	String dstip;
	int dstPort;
	Context pcontext;

	// ��ʱ�����豸��
	String tempString;
	String recordName;
	int byteFlag = 1;
	// ͨ����
	int chNo;
	// ����ID
	int adapterID;

	public int screenHeight;
	public int screenWidth;
	// **********��Ļ�������****************
	public int resolutionW = 352;// ��
	public int resolutionH = 288;// ��
	Bitmap videoBit = Bitmap.createBitmap(resolutionW, resolutionH,
			Config.RGB_565);
	Bitmap resizedBitmap;
	// ��������ͼƬ�õ�matrix����
	Matrix matrix = new Matrix();
	// ���������ʣ��³ߴ��ԭʼ�ߴ�
	private float leftDis;
	private float topOffset;

	// ��Ƶ���Ų���

	private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private final Rect mRectSrc = new Rect();
	private final Rect mRectDst = new Rect();
	private float mAspectQuotient;

	private ZoomState mState;
	// ************************************

	byte[] mPixel = new byte[width * height * 2];

	ByteBuffer buffer = ByteBuffer.wrap(mPixel);

	int mTrans = 0x0F0F0F0F;

	String PathFileName;

	public native int InitDecoder(int width, int height);

	public native int UninitDecoder();

	public native int DecoderNal(byte[] in, int insize, byte[] out);

	// new func by wu
	public native int InputData(byte[] in, int len);

	public native int Decode(byte[] out);

	public native int ChangeBuffer();

	public native int InitAudioDecoder();

	public native int CloseAudioDecoder();

	public native int AudioDecode(byte[] in, int insize, int outsize);

	public native int AudioEncode(byte[] in, int insize, byte[] out, int outsize);

	static Socket socket;

	Boolean viewStatus = false;

	Boolean stopStatusBoolean = true;

	Boolean isHeart = true;

	public ComUtil util;

	byte[] wavAudio;
	int wavLength;

	public void InitVideoParam(String file, int w, int h, int flag) {
		PathFileName = file;
		width = w;
		height = h;

		byteFlag = flag;
		util = new ComUtil();

	}

	public void PlayVideo(int sWidth, int sHeight) {

		screenWidth = sWidth;
		screenHeight = sHeight;

		mPixel = new byte[width * height * 2];

		buffer = ByteBuffer.wrap(mPixel);

		int i = mPixel.length;

		for (i = 0; i < mPixel.length; i++) {
			mPixel[i] = (byte) 0x00;
		}
		new Thread(this).start();
	}

	public void setContext(Context context) {
		pcontext = context;

	}

	public RtspView(Context context) {
		super(context);
		pcontext = context;
		setFocusable(true);

	}

	// ��ò鿴״̬
	public Boolean getViewStatus() {
		return viewStatus;
	}

	// �˳�ʱ����״̬
	public void setStopStatus(Boolean status) {
		stopStatusBoolean = status;
		ifThisVideo = !status;
	}

	public void setViewStatusFalse() {
		viewStatus = false;
	}

	// �Ƿ�������
	public void setHeart(Boolean status) {
		isHeart = status;
	}

	public Bitmap snapBitmap() {
		final int viewWidth = getWidth();
		final int viewHeight = getHeight();
		return zoomImage(resizedBitmap, viewWidth, viewHeight);
	}

	/***
	 * ͼƬ�����ŷ���
	 * 
	 * @param bgimage
	 *            ��ԴͼƬ��Դ
	 * @param newWidth
	 *            �����ź���
	 * @param newHeight
	 *            �����ź�߶�
	 * @return
	 */
	public static Bitmap zoomImage(Bitmap bgimage, double newWidth,
			double newHeight) {
		// ��ȡ���ͼƬ�Ŀ�͸�
		float width = bgimage.getWidth();
		float height = bgimage.getHeight();
		// ��������ͼƬ�õ�matrix����
		Matrix matrix = new Matrix();
		// ������������
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// ����ͼƬ����
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
				(int) height, matrix, true);
		return bitmap;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		try {
			buffer.rewind();
			// buffer.position(0);
			videoBit.copyPixelsFromBuffer(buffer);
			// System.out.print("resolutionW is ");
			// System.out.println(resolutionW);
			// System.out.print("resolutionH is ");
			// System.out.println(resolutionH);
			// System.out.print("videoBit is ");
			// System.out.println(videoBit);
			// System.out.print("matrix is ");
			// System.out.println(matrix);
			resizedBitmap = Bitmap.createBitmap(videoBit, 0, 0, resolutionW,
					resolutionH, matrix, true);

			if (resizedBitmap != null && mState != null) {
				final int viewWidth = getWidth();
				final int viewHeight = getHeight();
				final int bitmapWidth = resizedBitmap.getWidth();
				final int bitmapHeight = resizedBitmap.getHeight();

				final float panX = mState.getPanX();
				final float panY = mState.getPanY();
				final float zoomX = mState.getZoomX(mAspectQuotient)
						* viewWidth / bitmapWidth;
				final float zoomY = mState.getZoomY(mAspectQuotient)
						* viewHeight / bitmapHeight;

				// Setup source and destination rectangles
				// ����ٶ�ͼƬ�ĸߺͿ�������ʾ����ĸߺͿ��������������������
				mRectSrc.left = (int) (panX * bitmapWidth - viewWidth
						/ (zoomX * 2));
				mRectSrc.top = (int) (panY * bitmapHeight - viewHeight
						/ (zoomY * 2));
				mRectSrc.right = (int) (mRectSrc.left + viewWidth / zoomX);
				mRectSrc.bottom = (int) (mRectSrc.top + viewHeight / zoomY);
				mRectDst.left = getLeft();
				mRectDst.top = getTop();
				mRectDst.right = getRight();
				mRectDst.bottom = getBottom();

				// Adjust source rectangle so that it fits within the source
				// image.
				// ���ͼƬ����С����ʾ������ߣ������С�����������ƶ�������������������������������������߽�
				if (mRectSrc.left < 0) {
					mRectDst.left += -mRectSrc.left * zoomX;
					mRectSrc.left = 0;
				}
				if (mRectSrc.right > bitmapWidth) {
					mRectDst.right -= (mRectSrc.right - bitmapWidth) * zoomX;
					mRectSrc.right = bitmapWidth;
				}
				if (mRectSrc.top < 0) {
					mRectDst.top += -mRectSrc.top * zoomY;
					mRectSrc.top = 0;
				}
				if (mRectSrc.bottom > bitmapHeight) {
					mRectDst.bottom -= (mRectSrc.bottom - bitmapHeight) * zoomY;
					mRectSrc.bottom = bitmapHeight;
				}
				canvas.drawBitmap(resizedBitmap, mRectSrc, mRectDst, mPaint);
			}
		} catch (Exception e) {
			// TODO: handle exception
			// System.out.print(" drawerror is ");
			// System.out.println(e.toString());
			e.printStackTrace();
		}
	}

	public void setZoomState(ZoomState state) {
		if (mState != null) {
			mState.deleteObserver(this);
		}
		mState = state;
		mState.addObserver(this);
		invalidate();
	}

	public void update(Observable observable, Object data) {
		invalidate();
	}

	private void calculateAspectQuotient() {
		if (resizedBitmap != null) {
			mAspectQuotient = (((float) resizedBitmap.getWidth()) / resizedBitmap
					.getHeight()) / (((float) getWidth()) / getHeight());
		}
	}

	public void setImage(Bitmap bitmap) {
		resizedBitmap = bitmap;
		calculateAspectQuotient();
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		calculateAspectQuotient();
	}

	public void run() {
		if (workmode == 1)
			runnet();
		else if (workmode == 2) {
			runrecord();
		} else
			runfile();
	}

	// �ҵ���ͷ
	public int findHeader(byte[] buff) {
		int i;
		byte[] tempByte;
		int ulPDUID; // PDU ID
		ComUtil util = new ComUtil();

		tempByte = new byte[4];
		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 12];
		}
		ulPDUID = util.bytesToInt(tempByte);
		return ulPDUID;
	}

	// ��ȡ��Ƶ����
	public byte[] receiveStFrameData(byte[] buff, int length) {
		int i;
		byte[] returnBuffer;

		returnBuffer = new byte[length];
		for (i = 0; i < length; i++) {
			returnBuffer[i] = buff[i + 4];
		}
		return returnBuffer;
	}

	// ��ȡӦ�𳤶�
	public int receiveAnswerLength(byte[] buff) {
		int i;
		byte[] tempByte;
		int ulPDUDataLen; // PDU���ݳ���(����Э��ͷ)
		ComUtil util = new ComUtil();

		tempByte = new byte[4];
		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 16];
		}
		ulPDUDataLen = util.bytesToInt(tempByte);

		return ulPDUDataLen;
	}

	// ��ȡ��Ƶ���ݳ���
	public int receiveStFrameDataLength(byte[] buff) {
		int i;
		byte[] tempByte;
		int ulPDUDataLen; // PDU���ݳ���(����Э��ͷ)
		ComUtil util = new ComUtil();

		tempByte = new byte[4];
		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 16];
		}
		ulPDUDataLen = util.bytesToInt(tempByte);

		int buffLength = ulPDUDataLen - 12;
		return buffLength;
	}

	// ��ȡ��Ƶ���ݳ���
	public int receiveRadioDataLength(byte[] buff) {
		int i;
		byte[] tempByte;
		int ulPDUDataLen; // PDU���ݳ���(����Э��ͷ)
		ComUtil util = new ComUtil();

		tempByte = new byte[4];
		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 16];
		}
		ulPDUDataLen = util.bytesToInt(tempByte);
		return ulPDUDataLen;
	}

	// ����������Ŀǰδʹ��
	public int receiveBuffer(byte[] buff) {
		int i;
		int j;
		byte[] tempByte;
		byte[] code;
		String codeNoString;
		int ulPDUFlag; // PDU��־λ
		int ulReservered1; // �����ֶΣ�Ĭ��Ϊ0
		int ulReservered2; // �����ֶΣ�Ĭ��Ϊ0
		int ulPDUID; // PDU ID
		int ulPDUDataLen; // PDU���ݳ���(����Э��ͷ)
		int ulResult;// ���
		int ulAdapterID;
		int ulDeviceSNLen;
		int clDeviceSN;
		int ulChannelID;
		ComUtil util = new ComUtil();

		tempByte = new byte[4];
		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i];
		}
		ulPDUFlag = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 4];
		}
		ulReservered1 = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 8];
		}
		ulReservered2 = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 12];
		}
		ulPDUID = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 16];
		}
		ulPDUDataLen = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 20];
		}
		ulResult = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 24];
		}
		ulAdapterID = util.bytesToInt(tempByte);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 28];
		}
		ulDeviceSNLen = util.bytesToInt(tempByte);

		code = new byte[ulDeviceSNLen];
		for (i = 0; i < ulDeviceSNLen; i++) {
			code[i] = buff[i + 32];
		}
		codeNoString = new String(code);

		for (i = 0; i < 4; i++) {
			tempByte[i] = buff[i + 32 + ulDeviceSNLen];
		}
		ulChannelID = util.bytesToInt(tempByte);

		return ulResult;
	}

	// �Ƿ��ں�̨����
	public Boolean isBackRunning = false;

	public void setBackRunning(Boolean bool) {
		isBackRunning = bool;
	}

	byte[] recordBuffer;

	// ������
	Queue<ByteInfo> byteQueue = new LinkedList<ByteInfo>();
	ByteInfo info;
	int byteCount;
	boolean ifThisVideo; // ˵�����������Ƿ񻹼������ţ�


	public void runnet() {
		BufferedReader in;
		// PrintWriter out;
		int totallen = 0, count = 0;

		try {
			socket = new Socket(dstip, dstPort);
		} catch (IOException e) {
			// System.out.print("we get exception receive len is ");
			// System.out.println(totallen);
			// System.out.print("we get totoal frame is  ");
			// System.out.println(count);
			if (e != null) {
				System.out.println(e.getMessage());
			}
			// e.printStackTrace();
		}
	}

	ProgressDialog dialog;

	int audioFlag = 1;

	// ���Ż�����
	private class PlayQueue implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while (ifThisVideo) {
					int queueSize = byteQueue.size();
					// System.out.println("queueSize:" + queueSize);
					ByteInfo queueGetInfo;
					byte[] queueGetBuff;
					int queueGetLength;
					if (byteCount > byteFlag) {
						if (queueSize > 0) {
							// ����buff��������֡���ͣ�����Ƶ���ݵĳ��Ȼ�ȡ����Ƶ���ݣ�Ҳ���ǰ�֡����ȥ��
							queueGetInfo = byteQueue.poll();
							queueGetBuff = queueGetInfo.getStream();
							queueGetLength = queueGetInfo.getLength();
							try {
								InputData(queueGetBuff, queueGetLength);
							} catch (Exception e) {
								// TODO: handle exception
							}

							while (true) {
								try {
									int type = Decode(mPixel);
									if (type < 0) {
										break;
									}
									if (type == 5 || type == 1) {
										postInvalidate();
										// �����ʾͼ����viewStatus=true
										viewStatus = true;
										if (type == 1) {
											recordBuffer = queueGetBuff;
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							try {
								ChangeBuffer();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {

							// new Thread() {
							// public void run() {
							// handler.post(netWaiting);
							// }
							// }.start();
							// System.out.println("queueSize:" + queueSize);

						}

					}
				}
				UninitDecoder();
				byteQueue.clear();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		}
	};

	public void runfile() {
		try {
			InputStream is = null;
			FileInputStream fileIS = null;

			int totallen = 0;
			int bytesRead = 0;

			byte[] SockBuf = new byte[2048];

			try {
				fileIS = new FileInputStream(PathFileName);
			} catch (IOException e) {
				// System.out.print("can't open ");
				// System.out.println(PathFileName);
				return;
			}

			InitDecoder(width, height);
			int count = 0;
			long startTime = System.nanoTime();

			while (!Thread.currentThread().isInterrupted()) {
				try {
					bytesRead = fileIS.read(SockBuf, 0, 1024);
				} catch (IOException e) {
				}

				if (bytesRead <= 0)
					break;

				InputData(SockBuf, bytesRead);
				totallen += bytesRead;

				while (true) {
					try {
						int type = Decode(mPixel);
						if (type < 0)
							break;
						if (type == 5 || type == 1) {
							count++;
							postInvalidate();
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				ChangeBuffer();

			}
			// try {
			// if (fileIS != null)
			// fileIS.close();
			// if (is != null)
			// is.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// Log.d("", "end program");
			long endTime = System.nanoTime();
			// System.out.print("Done! Total frames is ");
			// System.out.println(count);
			float totalTime = (endTime - startTime) / 1000000000.0f;
			// System.out.print(" Total used time is ");
			// System.out.println(totalTime);
			float fps = count / totalTime;
			// System.out.print(" fps is ");
			// System.out.println(fps);
			//
			WriteToFile("Total frames is ");
			WriteToFile(String.valueOf(count));
			WriteToFile("Total used time is ");
			WriteToFile(String.valueOf(totalTime));
			WriteToFile("fps is ");
			WriteToFile(String.valueOf(fps));

			UninitDecoder();
		} catch (Exception e) {
			// TODO: handle exception
			// System.out.print(" runfileerror is ");
			// System.out.println(e.toString());
			e.printStackTrace();
		}
	}

	// �����㷨
	public void VideoControl(long time) {
		String formatStrEnd = "";
		long DateTemp1 = new Date().getTime();
		SimpleDateFormat bartDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		formatStrEnd = bartDateFormat.format(time);

	}

	// ���socket
	public Socket checkSocket() {
		return socket;
	}

	// �ر�socket
	public void closeSocket() {
		try {
			if (socket != null) {
				if (resizedBitmap != null && !resizedBitmap.isRecycled())
					resizedBitmap.recycle();
				socket.shutdownInput();
				socket.shutdownOutput();
				socket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void NetTest(String dip, int dPort, String vString, int no, int aid) {
		dstip = dip;
		dstPort = dPort;

		workmode = 1;

		tempString = vString;
		chNo = no;
		adapterID = aid;
	}

	public void NetTestRecord(String dip, int dPort, String vString, int no,
			int aid, String name) {
		dstip = dip;
		dstPort = dPort;

		workmode = 2;

		tempString = vString;
		chNo = no;
		adapterID = aid;
		recordName = name;
	}

	// write result to config file
	public int WriteToFile(String content) {
		FileOutputStream fileIS = null;

		try {
			fileIS = new FileOutputStream("/sdcard/result.conf", true);

			fileIS.write(content.getBytes());
			// StringBuffer text = new StringBuffer();
			// while ((tempStr = br.readLine())!= null )
			// System.out.print(tempStr);
			// text.append(tempStr);

			fileIS.close();
		} catch (IOException e) {
			return 0;
		}

		return 0;
	}

	public void holderCtrol(String direction, int channelNum) {
		try {
			if (socket != null) {
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				PrintWriter pw = new PrintWriter(out);
				pw.write(direction);
				pw.flush();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	public void sendByte(byte[] bit) {
		try {
			if (socket != null) {
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.write(bit);
				out.flush();
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	// ����Э��
	// ��Ƶ��ʼЭ��
	public void videoBegin() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[32 + code.length];
			// for (i = 0; i < head.length; i++) {
			// bit[i] = head[i];
			// }
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			// PDU���ݳ���
			tempByte = util.longToBytes(12 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ͨ��ID
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			sendByte(bit);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��ƵֹͣЭ��
	public void videoStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[32 + code.length];
			// for (i = 0; i < head.length; i++) {
			// bit[i] = head[i];
			// }
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(2);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			// PDU���ݳ���
			tempByte = util.longToBytes(12 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ͨ��ID
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			sendByte(bit);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ����Э��
	// �Խ���ʼЭ��
	public void radioBegin() {

		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[32 + code.length];
			// for (i = 0; i < head.length; i++) {
			// bit[i] = head[i];
			// }
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			// PDU���ݳ���
			tempByte = util.longToBytes(12 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ͨ��ID
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			sendByte(bit);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ����
	public void heartBeat() {
		int i;
		byte[] bit;
		byte[] tempByte;
		try {
			bit = new byte[24];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(3000);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			sendByte(bit);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����
	public void videoUP() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����ֹͣ
	public void videoUPStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����
	public void videoDOWN() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(2);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����ֹͣ
	public void videoDOWNStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(2);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����
	public void videoLEFT() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(3);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����ֹͣ
	public void videoLEFTStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(3);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����
	public void videoRIGHT() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ��̨����ֹͣ
	public void videoRIGHTStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �䱶��
	public void videoZoomIn() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(11);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �䱶��ֹͣ
	public void videoZoomInStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(11);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �䱶��
	public void videoZoomOut() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(12);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �䱶��ֹͣ
	public void videoZoomOutStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(12);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �佹��
	public void videoFocusIn() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(13);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �佹��ֹͣ
	public void videoFocusInStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(13);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �佹��
	public void videoFocusOut() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(14);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��̨��ʼ
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// �佹��ֹͣ
	public void videoFocusOutStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			bit = new byte[44 + code.length];
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(4);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			tempByte = util.longToBytes(24 + code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ��̨ͨ��
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + j] = tempByte[i];
			}
			// ��̨����
			tempByte = util.longToBytes(14);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + j] = tempByte[i];
			}
			// ��̨�ٶ�
			tempByte = util.longToBytes(5);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 36 + j] = tempByte[i];
			}
			// ��ֹ̨ͣ
			tempByte = util.longToBytes(1);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 40 + j] = tempByte[i];
			}
			sendByte(bit);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ¼��ʼЭ��
	public void recordBegin() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] name;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			name = recordName.getBytes("UTF8");
			bit = new byte[36 + code.length + name.length];
			// for (i = 0; i < head.length; i++) {
			// bit[i] = head[i];
			// }
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(103);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			// PDU���ݳ���
			tempByte = util.longToBytes(16 + code.length + name.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ͨ��ID
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + code.length] = tempByte[i];
			}
			// ¼�����Ƴ���
			tempByte = util.longToBytes(name.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + code.length] = tempByte[i];
			}
			// ¼������
			for (i = 0; i < name.length; i++) {
				bit[i + 36 + code.length] = name[i];
			}
			sendByte(bit);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ¼��ֹͣЭ��
	public void recordStop() {
		int i;
		int j;
		// String codeno = "ML2";
		String codeno = tempString;
		byte[] bit;
		byte[] code;
		byte[] name;
		byte[] tempByte;
		try {
			code = codeno.getBytes("UTF8");
			name = recordName.getBytes("UTF8");
			bit = new byte[36 + code.length + name.length];
			// for (i = 0; i < head.length; i++) {
			// bit[i] = head[i];
			// }
			tempByte = util.longToBytes(0);
			for (i = 0; i < tempByte.length; i++) {
				bit[i] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 4] = tempByte[i];
			}
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 8] = tempByte[i];
			}
			tempByte = util.longToBytes(106);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 12] = tempByte[i];
			}
			// PDU���ݳ���
			tempByte = util.longToBytes(16 + code.length + name.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 16] = tempByte[i];
			}
			// ����ID
			tempByte = util.longToBytes(adapterID);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 20] = tempByte[i];
			}
			tempByte = util.longToBytes(code.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 24] = tempByte[i];
			}
			for (j = 0; j < code.length; j++) {
				bit[i + 24 + j] = code[j];
			}
			// ͨ��ID
			tempByte = util.longToBytes(chNo);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 28 + code.length] = tempByte[i];
			}
			// ¼�����Ƴ���
			tempByte = util.longToBytes(name.length);
			for (i = 0; i < tempByte.length; i++) {
				bit[i + 32 + code.length] = tempByte[i];
			}
			// ¼������
			for (i = 0; i < name.length; i++) {
				bit[i + 36 + code.length] = name[i];
			}
			sendByte(bit);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void init(int widthFrame, int heightFrame) {
		try {
			InitDecoder(widthFrame, heightFrame);

			resolutionW = widthFrame;
			resolutionH = heightFrame;
			videoBit = Bitmap.createBitmap(resolutionW, resolutionH,
					Config.RGB_565);

			mPixel = new byte[widthFrame * heightFrame * 2];

			buffer = ByteBuffer.wrap(mPixel);

			int i = mPixel.length;

			for (i = 0; i < mPixel.length; i++) {
				mPixel[i] = (byte) 0x00;
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void runrecord() {
		BufferedReader in;
		// PrintWriter out;
		int totallen = 0, count = 0;
		int flag = 0;// ��һ�λ�ȡ����ȸ߶Ƚ��г�ʼ����

		try {
			socket = new Socket(dstip, dstPort);

			recordBegin();

			// ��������
			heartTimer = new Timer();

			Thread hThread = new Thread(heartThread);
			hThread.start();

			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			// out = new PrintWriter(socket.getOutputStream(), true);
			// // out.println("1jkl");

			byte[] buff;// ��������ƻ���
			byte[] headBuffer = new byte[20];// ����Э��ͷ
			byte[] buffer; // ������
			InputStream is = null; // socket������Ҫ��
			BufferedInputStream bis = null;// ���ն�����������Ҫ�ģ���ȡ���յ��Ķ���������BufferedInputStream��
			// FileOutputStream fos = null; //
			// �ļ�������Ҫ�ģ�FileOutputStream�������������ڴ�д��Ӳ��
			is = socket.getInputStream();// ��ȡ����socket����
			bis = new BufferedInputStream(is);// ���￪ʼ���ն�����������
			// ��¼���뷵��-1
			int falseCount = 0;
			int changeCount = 0;
			int inputCount = 0;
			int decodeCount = 0;

			while (true) {

				try {

					// if (!isBackRunning) {
					int len = bis.read(headBuffer);// �������յ��Ķ������������뻺��buff�����ر��ν��յ��Ķ��������ĳ���,������Ϊ�㣬����-1
					System.out.print("length of bis");
					System.out.println(len);

					if (socket.isClosed()) {
						if (!stopStatusBoolean)// �ж��Ƿ��˳������ԣ����δ�˳���������
						{
							socket.close();
							is = null;
							socket = new Socket(dstip, dstPort);
							recordBegin();
							is = socket.getInputStream();// ��ȡ����socket����
							bis = new BufferedInputStream(is);// ���￪ʼ���ն�����������
							len = bis.read(headBuffer);//
						} else {
							break;
						}
					}

					if (-1 == len)// len==-12h,û�������ˣ�����
					{
						break;
					}

					// �ж�ͷ�����Ƿ�Ϊ20��������ǣ�������ȡ
					while (len != 20 && len != -1) {
						byte[] tempBuffer = new byte[20 - len];
						int tempLen = bis.read(tempBuffer);
						for (int i = 0; i < tempLen; i++) {
							headBuffer[len + i] = tempBuffer[i];
						}
						len = len + tempLen;
					}

					// ͨ��Э��ͷ�ҵ�PDUID
					int pduID = findHeader(headBuffer);
					System.out.print("pduID is ");
					System.out.println(pduID);

					switch (pduID) {
					case 104: {
						// �����Ӧ��Ӧ�ý��ⲿ�����ݶ�����
						byte[] answerBuffer = new byte[4];// ����ֵ
						int answerLength = bis.read(answerBuffer);
						while (answerLength != 4) {
							byte[] tempBuffer = new byte[4 - answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						int status = util.bytesToInt(answerBuffer);
						// System.out.print("answer status is : ");
						// System.out.println(status);

						// if (status == 0) {
						// ��ȡ���� ID
						byte[] temp = new byte[4];
						answerLength = bis.read(temp);
						while (answerLength != 4) {
							byte[] tempBuffer = new byte[4 - answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						int shipeiID = util.bytesToInt(temp);
						// System.out.print("shipeiID is : ");
						// System.out.println(shipeiID);

						// ��ȡ ���кų���
						temp = new byte[4];
						answerLength = bis.read(temp);
						while (answerLength != 4) {
							byte[] tempBuffer = new byte[4 - answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						int codeLength = util.bytesToInt(temp);
						// System.out.print("codeLength is : ");
						// System.out.println(codeLength);

						temp = new byte[codeLength];
						answerLength = bis.read(temp);
						while (answerLength != codeLength) {
							byte[] tempBuffer = new byte[codeLength
									- answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						String code = new String(temp);
						// System.out.print("code is : ");
						// System.out.println(code);

						// ��ȡ�豸ͨ��ID
						temp = new byte[4];
						answerLength = bis.read(temp);
						while (answerLength != 4) {
							byte[] tempBuffer = new byte[4 - answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						int tongdaoId = util.bytesToInt(temp);
						// System.out.print("tongdaoId is : ");
						// System.out.println(tongdaoId);

						// ��ȡ¼�����Ƴ���
						temp = new byte[4];
						answerLength = bis.read(temp);
						while (answerLength != 4) {
							byte[] tempBuffer = new byte[4 - answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						int recordLength = util.bytesToInt(temp);
						// System.out.print("recordLength is : ");
						// System.out.println(recordLength);

						temp = new byte[recordLength];
						answerLength = bis.read(temp);
						while (answerLength != recordLength) {
							byte[] tempBuffer = new byte[recordLength
									- answerLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								answerBuffer[answerLength + i] = tempBuffer[i];
							}
							answerLength = answerLength + tempLen;
						}
						String rName = new String(temp);
						// System.out.print("recordName is : ");
						// System.out.println(rName);

						// }

					}
						break;
					// case 3: {
					// // ͨ��Э��ͷ��ȡ����Ƶ���ݵĳ���
					// InitDecoder(width, height);
					// int dataLength = receiveStFrameDataLength(headBuffer);
					//
					// // ��ȡ֡����
					// buff = new byte[4];
					//
					// int typeLength = bis.read(buff);
					// while (typeLength != 4) {
					// byte[] tempBuffer = new byte[4 - typeLength];
					// int tempLen = bis.read(tempBuffer);
					// for (int i = 0; i < tempLen; i++) {
					// buff[typeLength + i] = tempBuffer[i];
					// }
					// typeLength = typeLength + tempLen;
					// }
					// int typeInt = util.bytesToInt(buff);
					// System.out.print("typeInt is : ");
					// System.out.println(typeInt);
					//
					// // ��ȡframeRate
					// byte[] temp = new byte[4];
					//
					// int rateLength = bis.read(temp);
					// while (rateLength != 4) {
					// byte[] tempBuffer = new byte[4 - rateLength];
					// int tempLen = bis.read(tempBuffer);
					// for (int i = 0; i < tempLen; i++) {
					// temp[rateLength + i] = tempBuffer[i];
					// }
					// rateLength = rateLength + tempLen;
					// }
					// int frameRate = util.bytesToInt(temp);
					// System.out.print("frameRate is : ");
					// System.out.println(frameRate);
					//
					// // ��ȡ frameIndex
					// temp = new byte[4];
					//
					// int indexLength = bis.read(temp);
					// while (indexLength != 4) {
					// byte[] tempBuffer = new byte[4 - indexLength];
					// int tempLen = bis.read(tempBuffer);
					// for (int i = 0; i < tempLen; i++) {
					// temp[indexLength + i] = tempBuffer[i];
					// }
					// indexLength = indexLength + tempLen;
					// }
					// int frameIndex = util.bytesToInt(temp);
					// System.out.print("frameIndex is : ");
					// System.out.println(frameIndex);
					//
					// // Ȼ���ȡ֡����
					// byte[] receiveBuff = new byte[dataLength];
					// int tempDataLen = bis.read(receiveBuff);
					//
					// // �ж����ݳ����Ƿ�ΪdataLength��������ǣ�������ȡ
					// while (tempDataLen != dataLength) {
					// byte[] tempBuffer = new byte[dataLength
					// - tempDataLen];
					// int tempLen = bis.read(tempBuffer);
					// for (int i = 0; i < tempLen; i++) {
					// receiveBuff[tempDataLen + i] = tempBuffer[i];
					// }
					// tempDataLen = tempDataLen + tempLen;
					// }
					//
					// // ����buff��������֡���ͣ�����Ƶ���ݵĳ��Ȼ�ȡ����Ƶ���ݣ�Ҳ���ǰ�֡����ȥ��
					//
					// try {
					// InputData(receiveBuff, dataLength);
					// } catch (Exception e) {
					// // TODO: handle exception
					// inputCount++;
					// System.out.print("inputCount is :");
					// System.out.println(inputCount);
					// }
					// totallen += dataLength;
					//
					// while (true) {
					// try {
					// int type = Decode(mPixel);
					// if (type < 0) {
					// falseCount++;
					// System.out.print("falseCount is :");
					// System.out.println(falseCount);
					// break;
					// }
					// if (type == 5 || type == 1) {
					// count++;
					// postInvalidate();
					// // �����ʾͼ����viewStatus=true
					// viewStatus = true;
					// if (type == 1) {
					// recordBuffer = receiveBuff;
					// }
					// }
					// } catch (Exception e) {
					// // TODO: handle exception
					// decodeCount++;
					// System.out.print("decodeCount is :");
					// System.out.println(decodeCount);
					// }
					// }
					// try {
					// ChangeBuffer();
					// } catch (Exception e) {
					// // TODO: handle exception
					// changeCount++;
					// System.out.print("changeCount is :");
					// System.out.println(changeCount);
					// }
					//
					// }
					// break;
					case 3: {
						// ͨ��Э��ͷ��ȡ����Ƶ���ݵĳ���
						int dataLength = receiveStFrameDataLength(headBuffer);

						// ��ȡ֡����
						buff = new byte[4];

						int typeLength = bis.read(buff);
						while (typeLength != 4) {
							byte[] tempBuffer = new byte[4 - typeLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								buff[typeLength + i] = tempBuffer[i];
							}
							typeLength = typeLength + tempLen;
						}
						int typeInt = util.bytesToInt(buff);
						// System.out.print("typeInt is : ");
						// System.out.println(typeInt);

						// ��ȡframeRate
						byte[] temp = new byte[4];

						int rateLength = bis.read(temp);
						while (rateLength != 4) {
							byte[] tempBuffer = new byte[4 - rateLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								temp[rateLength + i] = tempBuffer[i];
							}
							rateLength = rateLength + tempLen;
						}
						int frameRate = util.bytesToInt(temp);
						System.out.print("frameRate is : ");
						System.out.println(frameRate);

						// ��ȡ frameIndex
						temp = new byte[4];

						int indexLength = bis.read(temp);
						while (indexLength != 4) {
							byte[] tempBuffer = new byte[4 - indexLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								temp[indexLength + i] = tempBuffer[i];
							}
							indexLength = indexLength + tempLen;
						}
						int frameIndex = util.bytesToInt(temp);
						System.out.print("frameIndex is : ");
						System.out.println(frameIndex);

						// ��ȡ ��Ƶ���
						temp = new byte[4];

						int widthLength = bis.read(temp);
						while (widthLength != 4) {
							byte[] tempBuffer = new byte[4 - widthLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								temp[widthLength + i] = tempBuffer[i];
							}
							widthLength = widthLength + tempLen;
						}
						int widthFrame = util.bytesToInt(temp);
						// System.out.print("widthFrame is : ");
						// System.out.println(widthFrame);

						// ��ȡ ��Ƶ�߶�
						temp = new byte[4];

						int heightLength = bis.read(temp);
						while (heightLength != 4) {
							byte[] tempBuffer = new byte[4 - heightLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								temp[heightLength + i] = tempBuffer[i];
							}
							heightLength = heightLength + tempLen;
						}
						int heightFrame = util.bytesToInt(temp);
						// System.out.print("heightFrame is : ");
						// System.out.println(heightFrame);

						if (flag == 0) {
							init(widthFrame, heightFrame);
							flag++;
						}

						// Ȼ���ȡ֡����
						byte[] receiveBuff = new byte[dataLength];
						int tempDataLen = bis.read(receiveBuff);

						// �ж����ݳ����Ƿ�ΪdataLength��������ǣ�������ȡ
						while (tempDataLen != dataLength) {
							byte[] tempBuffer = new byte[dataLength
									- tempDataLen];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								receiveBuff[tempDataLen + i] = tempBuffer[i];
							}
							tempDataLen = tempDataLen + tempLen;
						}

						// ����buff��������֡���ͣ�����Ƶ���ݵĳ��Ȼ�ȡ����Ƶ���ݣ�Ҳ���ǰ�֡����ȥ��

						try {
							InputData(receiveBuff, dataLength);
						} catch (Exception e) {
							// TODO: handle exception
							inputCount++;
							// System.out.print("inputCount is :");
							// System.out.println(inputCount);
						}
						totallen += dataLength;

						while (true) {
							try {
								int type = Decode(mPixel);
								if (type < 0) {
									falseCount++;
									// System.out.print("falseCount is :");
									// System.out.println(falseCount);
									break;
								}
								if (type == 5 || type == 1) {
									count++;
									postInvalidate();
									// �����ʾͼ����viewStatus=true
									viewStatus = true;
									if (type == 1) {
										recordBuffer = receiveBuff;
									}
								}
							} catch (Exception e) {
								// TODO: handle exception
								decodeCount++;
								// System.out.print("decodeCount is :");
								// System.out.println(decodeCount);
							}
						}
						try {
							ChangeBuffer();
						} catch (Exception e) {
							// TODO: handle exception
							changeCount++;
							// System.out.print("changeCount is :");
							// System.out.println(changeCount);
						}

					}
						break;
					case 3000: {
						// �����������int�����ݶ�����

						byte[] intBuffer = new byte[4];// ����ֵ
						int intLength = bis.read(intBuffer);
						while (intLength != 4) {
							byte[] tempBuffer = new byte[4 - intLength];
							int tempLen = bis.read(tempBuffer);
							for (int i = 0; i < tempLen; i++) {
								intBuffer[intLength + i] = tempBuffer[i];
							}
							intLength = intLength + tempLen;
						}
						// System.out.print("this int is :");
						// System.out.println(util.bytesToInt(intBuffer));
					}
						break;
					default:
						int a = 0;
						a++;
						is = null;
						bis = null;
						is = socket.getInputStream();// ��ȡ����socket����
						bis = new BufferedInputStream(is);// ���￪ʼ���ն�����������

						break;
					}
					// }

				} catch (Exception e) {
					// TODO: handle exception
					if (e != null) {
						System.out.println(e.getMessage());
					}
				}
			}
			// out.close();
			in.close();
			socket.close();
			// System.out.print("End of Receive");
			// System.out.println(totallen);
			// System.out.print("we get totoal frame is  ");
			// System.out.println(count);
			// System.out.print("inputCount is :");
			// System.out.println(inputCount);
			// System.out.print("falseCount is :");
			// System.out.println(falseCount);
			// System.out.print("changeCount is :");
			// System.out.println(changeCount);
		} catch (IOException e) {
			// System.out.print("we get exception receive len is ");
			// System.out.println(totallen);
			System.out.print("we get totoal frame is  ");
			System.out.println(count);
			if (e != null) {
				System.out.println(e.getMessage());
			}
			// e.printStackTrace();
		}

		UninitDecoder();
	}

	// ¼�񷽷�
	static Boolean isRecording = false;
	static FileOutputStream fos = null;
	private Timer timer;
	File file;
	private int timerCount = 0;

	public void record() {
		timerCount = 0;
		timer = new Timer();
		String strFolder = checkRecordPath();

		SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd-HHmmss");
		Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
		String fileNameString = "¼��" + formatter.format(curDate) + "W" + width
				+ "H" + height;
		try {
			fos = new FileOutputStream(strFolder + fileNameString);
			file = new File(strFolder + fileNameString);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isRecording = true;

		Thread timeThread = new Thread(waitThread);
		timeThread.start();

		Thread record = new Thread(recordThread);
		record.start();
	}

	public String stopRecord() {
		isRecording = false;
		try {
			// fos.flush();
			fos.close();
			if (timerCount < 3) {
				FileUtil.deleteFile(file);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return file.toString();
	}

	// ���浽sdcard

	Runnable recordThread = new Runnable() {
		public void run() {
			byte[] bitsame = null;

			try {
				while (isRecording) {
					if (!recordBuffer.equals(bitsame)) {
						if (null != fos) {
							fos.write(recordBuffer);
							fos.flush();
							bitsame = recordBuffer;
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	// ¼���ʱ
	private void TimerCount(Timer t) {
		timerCount++;
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
			if (isRecording) {
				TimerOut();
			}
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

	// ��������
	private Timer heartTimer;
	private int heartCount = 0;

	// ������ʱ
	private void heartTimerCount(Timer t) {
		if (isHeart) {
			heartBeat();
			System.out.println("this is a HeartBeat!!!!!!!!! ");
		} else {
			t.cancel();
		}
	}

	private void heartTimerOut() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				heartTimerCount(heartTimer);
			}
		};
		heartTimer.schedule(timerTask, 0, 5000);
	}

	Runnable heartThread = new Runnable() {
		public void run() {
			heartTimerOut();
		}
	};

}
