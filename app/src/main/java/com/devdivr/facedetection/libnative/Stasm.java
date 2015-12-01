package com.devdivr.facedetection.libnative;

public class Stasm {
	public static native int[] FindFaceLandmarks(float ratioW, float ratioH, String path);
}
