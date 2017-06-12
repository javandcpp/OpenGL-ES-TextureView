//
//  javandoc
//

package com.guagua.player;

import android.graphics.SurfaceTexture;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class StreamingPlayerEGLHelper {

	public static final int OPENGL_ES_VERSION_1x = 1;
	public static final int OPENGL_ES_VERSION_2 = 2;
	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	private static final int EGL_OPENGL_ES2_BIT = 4;

	private static final String TAG = "EGLHelper >>>";

	private EGL10 egl;
	private EGLDisplay eglDisplay;
	private EGLConfig eglConfig;
	private EGLContext eglContext;
	private EGLSurface eglSurface;

	public StreamingPlayerEGLHelper() {
	}

	public boolean initialize(SurfaceTexture holder, int glesVersion) {
		
		if (OPENGL_ES_VERSION_1x != glesVersion && OPENGL_ES_VERSION_2 != glesVersion) {
			throw new IllegalArgumentException("GL ES version has to be one of " + OPENGL_ES_VERSION_1x + " and " + OPENGL_ES_VERSION_2);
		}

		egl = (EGL10) EGLContext.getEGL();
		if (null == egl) {
			return false;
		}
		// Fetch the default display
		eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		if (null == eglDisplay || EGL10.EGL_NO_DISPLAY == eglDisplay) {
			destroy();
			return false;
		}

		// Initialize EGL
		int[] version = new int[2];
		if (egl.eglInitialize(eglDisplay, version)) {
		}
		else {
			destroy();
			return false;
		}
		// Choose an EGLConfig
		int[] attrList;
		if (OPENGL_ES_VERSION_1x == glesVersion) {
			attrList = new int[] { //
			EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, //
					EGL10.EGL_RED_SIZE, 8, //
					EGL10.EGL_GREEN_SIZE, 8, //
					EGL10.EGL_BLUE_SIZE, 8, //
					EGL10.EGL_DEPTH_SIZE, 16, //
					EGL10.EGL_NONE //
			};
		}
		else {			
			attrList = new int[] { //
//                  EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, //
			EGL10.EGL_RED_SIZE, 8, //
					EGL10.EGL_GREEN_SIZE, 8, //
					EGL10.EGL_BLUE_SIZE, 8, //
//                  EGL10.EGL_DEPTH_SIZE, 16, //
					EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, //
					EGL10.EGL_NONE //
			};
		}
		EGLConfig[] configOut = new EGLConfig[1];
		int[] configNumOut = new int[1];
		if (egl.eglChooseConfig(eglDisplay, attrList, configOut, 1, configNumOut) && 1 == configNumOut[0]) {
			eglConfig = configOut[0];
		}
		else {
			int error = egl.eglGetError();
			destroy();
			return false;
		}

		// Create rendering context
		int[] contextAttrs;
		if (OPENGL_ES_VERSION_1x == glesVersion) {
			contextAttrs = null;
		}
		else {
			contextAttrs = new int[] { EGL_CONTEXT_CLIENT_VERSION, OPENGL_ES_VERSION_2, //
					EGL10.EGL_NONE };
		}
		eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
		if (null == eglContext || EGL10.EGL_NO_CONTEXT == eglContext) {
			destroy();
			return false;
		}

		// Create window surface
		try {
//			egl.eglc
			eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, holder, null);
			if (null == eglSurface || EGL10.EGL_NO_SURFACE == eglSurface) {
				destroy();
				return false;
			}
		}
		catch (Exception e) {
			destroy();
			return false;
		}

		// Bind rendering context to surface
		if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
			destroy();
			return false;
		}

		return true;
	}

	public void destroy() {
		if (null == egl) {
			return;
		}

		if (null != eglDisplay && EGL10.EGL_NO_DISPLAY != eglDisplay) {

			// Unbind context<-->surface
			egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

			// Destroy surface
			if (null != eglSurface && EGL10.EGL_NO_SURFACE != eglSurface) {
				egl.eglDestroySurface(eglDisplay, eglSurface);
			}

			// Destroy context
			if (null != eglContext && EGL10.EGL_NO_CONTEXT != eglContext) {
				egl.eglDestroyContext(eglDisplay, eglContext);
			}

			// Disconnect display
			egl.eglTerminate(eglDisplay);
		}

		eglDisplay = null;
		eglSurface = null;
		eglContext = null;
		eglConfig = null;
		egl = null;
	}

	public void swapBuffers() {
		egl.eglSwapBuffers(eglDisplay, eglSurface);
	}
}
