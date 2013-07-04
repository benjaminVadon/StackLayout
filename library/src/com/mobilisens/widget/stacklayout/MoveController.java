package com.mobilisens.widget.stacklayout;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import com.mobilisens.widget.stacklayout.StackLayout.StackLayoutParams;

public class MoveController implements OnMoveListener {

	private final boolean DEBUG = false;
	private final String TAG = getClass().getSimpleName();

	private static final int NO_CHILD_TOUCHED = Integer.MAX_VALUE;
	
	private int currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	
	private StackLayout layoutHolder;
	
	public MoveController (StackLayout layoutHolder){
		this.layoutHolder = layoutHolder;
	}

	//OnMoveListener part
	@Override
	public void onStartMove() {
	}
	
	@Override
	public void onMove(int moveAmount) {
    	if(DEBUG){Log.i(TAG, "onMove "+moveAmount);}
		int upperChild = getUpperChild();
		if(upperChild<0)
			return;
		int viewReferenceIndex = getViewIndexTouchReference(upperChild);
		((StackViewContainer) layoutHolder.getChildAt(viewReferenceIndex)).movePanel(moveAmount);
	}

	private int getUpperChild() {
    	int nbChild = layoutHolder.getChildCount();
    	return nbChild -1;
	}

	private int getViewIndexTouchReference(int upperChild) {
		int viewReferenceIndex = upperChild;
		if(currentInterceptedTouchedView!=NO_CHILD_TOUCHED){
			boolean viewIsFixed = ((StackLayoutParams)layoutHolder.getChildAt(currentInterceptedTouchedView).getLayoutParams()).fixed;
			if(!viewIsFixed){
				viewReferenceIndex = currentInterceptedTouchedView;
			}
		}
		return viewReferenceIndex;
	}

	public static boolean isMovingToRight(int moveAmount) {
		if(moveAmount<0)
			return true;
		else
			return false;
	}

	@Override
	public void onEndMove(int velocity) {
		StackViewContainer unAnchoredPanel = getUnAnchoredPanel();
		if(unAnchoredPanel==null){
			int upperChild = getUpperChild();
			if(upperChild<0)
				return;
			int viewReferenceIndex = getViewIndexTouchReference(upperChild);
			unAnchoredPanel = ((StackViewContainer) layoutHolder.getChildAt(viewReferenceIndex));
		}
		
		unAnchoredPanel.animToNearestAnchor(velocity);
		currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	}

	private StackViewContainer getUnAnchoredPanel() {
		StackViewContainer result = null;
		for(int i=0; i<layoutHolder.getChildCount(); i++){
			StackViewContainer child = (StackViewContainer) layoutHolder.getChildAt(i);
			if(((StackLayoutParams)child.getLayoutParams()).isUnAnchored()){
				result = child;
				break;
			}
		}
		return result;
	}

	public void setCurrentInterceptedTouchedView(MotionEvent event) {
		currentInterceptedTouchedView = getCurrentInterceptedTouchedView(event);
	}

	private int getCurrentInterceptedTouchedView(MotionEvent event) {
		final int childrenCount = layoutHolder.getChildCount();
		if (childrenCount != 0) {
			int actionIndex = getCurrentPointerIndex(event);
			final int x = (int) event.getX(actionIndex);
			final int y = (int) event.getY(actionIndex);

			for (int i = childrenCount - 1; i >= 0; i--) {
				final StackViewContainer child = (StackViewContainer) layoutHolder.getChildAt(i);
				if (isTransformedTouchPointInView(x, y, child)) {
					return i;
				}
			}
		}
		return NO_CHILD_TOUCHED;
	}
	
	private boolean isTransformedTouchPointInView(int x, int y, StackViewContainer child) {
        final Rect frame = new Rect();
        child.getHitRect(frame);
        return frame.contains(x, y);
	}
	
	private int getCurrentPointerIndex(MotionEvent event){
		int activePointerId = layoutHolder.getActivePointerId();
		final int pointerIndex = event.findPointerIndex(activePointerId);
		return pointerIndex;
	}

}
