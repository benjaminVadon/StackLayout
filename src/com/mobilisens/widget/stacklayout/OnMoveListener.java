package com.mobilisens.widget.stacklayout;

public interface OnMoveListener{
	public void onStartMove();
	
	public void onMove (int moveAmount);
	public void onEndMove(int velocity);
}