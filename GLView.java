//
//	javandoc
//
package com.guagua.player;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.guagua.mediafilter.LibProcess;

public class GLView extends TextureView implements SurfaceHolder.Callback, TextureView.SurfaceTextureListener {
	private static final String TAG = "StreamVideoSurfaceView";

	private RtpMobilePlayer player = null;
	private boolean isSurfaceAvailable = false;
	private GLRunnable m_glRunnable;
	private int micIndex = 0;
	public byte[] videoBuffer = null;
	public byte[] rgbBuffer = null;
	
	private int vWidth = 0;
	private int vHeight = 0;

	
	public int showWidth = 0;
	public int showHeight = 0;

	//
	public int renderCount = 0;
	public long lastRenderTime = 0;
	private Handler testHandler = new Handler();
	private int showType;

	public RtpVideoSurfaceView(Context context) {
		super(context);
//		setZOrderOnTop(true);
//		setZOrderMediaOverlay(true);
//		getHolder().addCallback(this);
		setSurfaceTextureListener(this);
//		getHolder().setFormat(PixelFormat.TRANSPARENT);
	}

	public RtpVideoSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
//		setZOrderOnTop(true);
//		setZOrderMediaOverlay(true);
//		getHolder().addCallback(this);
		setSurfaceTextureListener(this);
//		getHolder().setFormat(PixelFormat.TRANSPARENT);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	public void setStreamingPlayer(RtpMobilePlayer tempPlayer) {
		player = tempPlayer;
		if(isSurfaceAvailable()) {
			ensureGLThread();
		}
	}

	public void setStreamingPlayer(int micIndex, RtpMobilePlayer tempPlayer, int width, int height,int showType) {
		this.micIndex = micIndex;
		this.vWidth = width;
		this.vHeight = height;
		this.showType=showType;
		videoBuffer = new byte[width * height * 3 / 2];
		rgbBuffer = new byte[width * height * 4];
		player = tempPlayer;
		if(isSurfaceAvailable()) {
			ensureGLThread();
		}
	}

	public void setViewSize(int width, int height) {
		showWidth = width;
		showHeight = height;
	}

	public void ensureGLThread() {
		if(m_glRunnable == null&&null!=player) {
			m_glRunnable = new GLRunnable(player);
			Thread glThread = new Thread(m_glRunnable, "GL_Thread");
			glThread.start();
		}
	}

	public boolean isSurfaceAvailable() {
		return isSurfaceAvailable;
	}

	/**
	 * This method is part of the SurfaceHolder.Callback interface, and is not
	 * normally called or subclassed by clients of GLSurfaceView.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
//		log("surface Created ");
		isSurfaceAvailable = true;
		restoreVideoResource();
	}

	/**
	 * This method is part of the SurfaceHolder.Callback interface, and is not
	 * normally called or subclassed by clients of GLSurfaceView.
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("surface","surface destory");
		isSurfaceAvailable = false;
		releaseVideoResource();
	}

	/**
	 * This method is part of the SurfaceHolder.Callback interface, and is not
	 * normally called or subclassed by clients of GLSurfaceView.
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, final int w, final int h) {
		if(player != null) {
			if(showWidth != 0 && showHeight != 0) {

				Log.d("TAG","showType:"+showType);
				player.setViewSize((byte)0, micIndex, showWidth, showHeight,showType);
			}
		}
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        isSurfaceAvailable = true;
        restoreVideoResource();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if(player != null) {
            if(showWidth != 0 && showHeight != 0) {

                Log.d("TAG","showType:"+showType);
                player.setViewSize((byte)0, micIndex, showWidth, showHeight,showType);
            }
        }
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        isSurfaceAvailable = false;
        releaseVideoResource();
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

	}

	private class GLRunnable implements Runnable {
		private StreamingPlayerEGLHelper eglHelper;
		private boolean bExit = false;
		private RtpMobilePlayer player;
		private SurfaceHolder mholder = null;
		//private boolean bHaveAudio = false;
		private Paint paint = null;

		public GLRunnable(RtpMobilePlayer player) {
			this.player = player;
		}

		private void initPaint() {
			paint = new Paint();
			paint.setColor(Color.RED);
		}





		public void run() {
			// EGL initialization
			if (null==player){
				return;
			}
			eglHelper = new StreamingPlayerEGLHelper();

			// Initialize GL until the surface is ready
			double lastTm = System.currentTimeMillis();

			while (!isSurfaceAvailable()) {
				if(bExit) {
					//m_waitThreadStart.release();
					return;
				}

				try {
//					LogUtils.i(TAG, "GLRunnable sleep");
					Thread.sleep(1, 10);
				} catch ( InterruptedException e ) {
				}
			}

			if(isSurfaceAvailable()) {
				if(paint == null) {
					initPaint();
				}
//				SurfaceHolder holder = getHolder();
                SurfaceTexture surfaceTexture = getSurfaceTexture();
                //mholder = holder;
				if(eglHelper.initialize(surfaceTexture, StreamingPlayerEGLHelper.OPENGL_ES_VERSION_2)) {
					// RenderingEngineAdapter adapter = (RenderingEngineAdapter)
					// renderingEngine;
					// adapter.renderingEngine =
					// RenderingEngine.createEngine2();
				} else if(eglHelper.initialize(surfaceTexture, StreamingPlayerEGLHelper.OPENGL_ES_VERSION_1x)) {
					// RenderingEngineAdapter adapter = (RenderingEngineAdapter)
					// renderingEngine;
					// adapter.renderingEngine =
					// RenderingEngine.createEngine1();
//					log("EGL initialized for GL ES version 1.x");
				} else {
//					log("Error initializing EGL. GL thread terminating...");
					eglHelper = null;
					//m_waitThreadStart.release();
					return;
				}
				// gl_initialize();
			} else {
//				log("EGL initialized for GL ES : surfaceHolder error !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}

//			log("start rendering");
			while (!bExit && isSurfaceAvailable()) {
				// //当缓存的player 被回收的情况下退出
				// if (player != null &&
				// checkPlayerExit(player.getPlayerFlag())) {
				// player.stop();
				// }
				if(player == null || player.isPlayerStop()) {
					stopGLThread();
					continue;
				}

				int sleepTime = 0;

				if (micIndex == 0 && false) {// 测试返回视频数据
					if (videoBuffer != null) {
						sleepTime = player.getVideoData((byte) 0, micIndex,
								videoBuffer, videoBuffer.length);
						LibProcess.ImageConver(videoBuffer, videoBuffer.length, rgbBuffer, rgbBuffer.length, vWidth, vHeight, LibProcess.COLOR_CONVERT_I420_TO_RGBA);

//						// 保存数据到文件
//						FileOutputStream os;
//						try {
//
//							File file = new File("/mnt/sdcard/GGLog/videoBuffer.rgba");
//							if (!file.exists()) {
//								file.createNewFile();
//							}
//
//							if(sleepTime>=0){
//								os = new FileOutputStream(file, true);
//								os.write(rgbBuffer);
//								os.close();
//							}
//						} catch (Exception e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}

					}
				} else {
					sleepTime = player.renderVideo((byte) 0, micIndex);
				}

				if(false) {
					if(sleepTime != -1) {
						renderCount++;
					}
					if(renderCount % 1000 == 0) {
						final long curTime = System.currentTimeMillis();
						final long useTime = curTime -lastRenderTime;
						lastRenderTime = curTime;
					}

				}
				double currentTm = System.currentTimeMillis();
				double elapsedTm = currentTm - lastTm;
/*				if(elapsedTm/1000 > 1)
				{
					try {
						TestSavePng();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					lastTm = currentTm;
				}
				*/
				//Log.d("testrender", "will render:" + player.getPlayerFlag());
				//boolean bHaveAudio = player.isHaveAudio((short) micIndex);
				//log("mic index " + micIndex + "bHaveAudio " + bHaveAudio);
				//DrawText(bHaveAudio);
				if(sleepTime == -1) {
					//不需要渲染，视频播放器没找到
					try {
						Thread.sleep(50);
					} catch ( Exception e ) {
					}
				}else{
					try {
						Thread.sleep(25);
					} catch ( Exception e ) {
					}
				}

			}// for Main loop

//			log("GL thread begin exiting...");
			// gl_uninitialize();

			if(player != null) {
				player.stopRenderVideo((byte)0, micIndex);//停止渲染视频
//				log("GL stop render");
			}
			if(eglHelper != null) {
//				log("GL thread begin destroy EGL...");
				eglHelper.destroy();
				eglHelper = null;
//				log("GL thread EGL destroyed.");
			}

//			log("GL thread finsih exit.");
		}

		public boolean stop() {
			bExit = true;
			return true;
		}
	}

	public void restoreVideoResource() {
//		log("restoreVideoResource");

		if(player != null) {
			byte abyDevType = 0;
			short asMicIndex = (short) micIndex;
			byte abyMicType = 0;
			player.setDevPlayState(abyMicType, asMicIndex, abyDevType, true);
//			player.enableVideoChannel(true);
			// player.enableAudioChannel(true);
		}

		if(player != null) {
			ensureGLThread();
		}
	}

	public void releaseVideoResource() {
		stopGLThread();
	}

	public void stopGLThread() {
		if(m_glRunnable != null) {
			if(m_glRunnable.stop()) {
//				log("GLThread exit OK.");
			}
			m_glRunnable = null;
		}
//		if (player != null) {
//			//以前这段语句放在run的结尾，并且通过handler在主线程执行，这样当连接重试时（停止前一个线程，开启新的线程），
//			//新线程开始渲染后，老线程才触发enableVideoChannel(false)，导致黑屏
//			player.enableVideoChannel(false);
//		}
	}

	private void log(String msg) {
//		int playerId = player != null ? player.getPlayerFlag() : -1;
	}
}
