package com.mobilisens.widget.stacklayout;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.mobilisens.widget.stacklayout.StackLayout.StackLayoutParams;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

public class MoveController implements OnMoveListener {

	private final boolean DEBUG = false;
	private final String TAG = getClass().getSimpleName();

	private static final boolean RIGHT = true;
	private static final boolean LEFT = false;
	private static final long MAX_ANIMATION_DURATION = 400;
	private static final int NO_CHILD_TOUCHED = Integer.MAX_VALUE;
	
	private int currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	private boolean lastDirection;
	
	private StackLayout layoutHolder;
	
	public MoveController (StackLayout layoutHolder){
		this.layoutHolder = layoutHolder;
	}

	//OnMoveListener part
	@Override
	public void onStartMove() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onMove(int moveAmount) {
    	if(DEBUG)Log.i(TAG, "onMove "+moveAmount);
    	
    	int count = layoutHolder.getChildCount();
    	if(count<1)
    		return;
    	
    	int upperChild = count -1;
    	if(isMovingToRight(moveAmount)){
    		lastDirection = RIGHT;
    		int viewReferenceIndex = upperChild;
    		if(currentInterceptedTouchedView!=NO_CHILD_TOUCHED){
    			viewReferenceIndex = currentInterceptedTouchedView;
    		}
    		moveViewAndUpperViews(viewReferenceIndex, moveAmount);
			moveUnderViewsToRight(viewReferenceIndex);
    	}else{//moveLeft
    		lastDirection = LEFT;
    		for (int viewIndex = 0; viewIndex < count; viewIndex++) {
    			moveViewAtIndexToLeft(viewIndex, upperChild, moveAmount);
	    	}
    	}
	}

	private void moveViewAndUpperViews(int viewReferenceIndex, int moveAmount) {
		int count = layoutHolder.getChildCount();
		for(int viewIndex=viewReferenceIndex; viewIndex<count; viewIndex++){
			moveViewAtIndexToRight(viewIndex, moveAmount);
		}
	}
	
	private void moveViewAtIndexToRight(int viewIndex, int moveAmount) {
		View viewMoved = layoutHolder.getChildAt(viewIndex);
		StackLayoutParams viewMovedLp = (StackLayoutParams) viewMoved.getLayoutParams();
		if(!viewMovedLp.fixed){
			Rect dirty = new Rect(viewMovedLp.left-layoutHolder.getShadowWidth(), layoutHolder.getTop(), viewMovedLp.left, layoutHolder.getBottom());
			layoutHolder.invalidate(dirty);
			viewMovedLp.left -=moveAmount;
			layoutHolder.updateViewLayout(viewMoved, viewMovedLp);
		}
	}
	
	private void moveUnderViewsToRight(int touchedChildViewIndex) {
		View currentView = layoutHolder.getChildAt(touchedChildViewIndex);
		StackLayoutParams currentLp = (StackLayoutParams) currentView.getLayoutParams();
		if(currentLp.fixed)
			return;
		
		for (int i = touchedChildViewIndex-1; i>=0; i--){
			View underView = layoutHolder.getChildAt(i);
			StackLayoutParams underViewLp = (StackLayoutParams) underView.getLayoutParams();
			if(underViewLp.fixed)
				break;
			
			int underViewWidth = underView.getMeasuredWidth();
			if(currentLp.left>=underViewLp.left+underViewWidth){
				Rect dirty = new Rect(underViewLp.left-layoutHolder.getShadowWidth(), layoutHolder.getTop(),underViewLp.left, layoutHolder.getBottom());
				layoutHolder.invalidate(dirty);
				int newUnderLeftPos = currentLp.left - underViewWidth;
				underViewLp.left = newUnderLeftPos;
				layoutHolder.updateViewLayout(underView, underViewLp);
			}
			currentLp = underViewLp;
		}
			
	}

	private boolean moveViewAtIndexToLeft(int viewIndex, int upperChild, int moveAmount) {
		View viewMoved = layoutHolder.getChildAt(viewIndex);
		StackLayoutParams viewMovedLp = (StackLayoutParams) viewMoved.getLayoutParams();
		if(!viewMovedLp.fixed){
			Rect dirty = new Rect(viewMovedLp.left-layoutHolder.getShadowWidth(), layoutHolder.getTop(), viewMovedLp.left, layoutHolder.getBottom());
			layoutHolder.invalidate(dirty);
			viewMovedLp.left -=moveAmount;
			if(viewMovedLp.left<0 && viewIndex != upperChild)
				viewMovedLp.left=0;
			layoutHolder.updateViewLayout(viewMoved, viewMovedLp);
			return true;
		}
		return false;
	}
	
	private boolean isMovingToRight(int moveAmount) {
		if(moveAmount<0)
			return true;
		else
			return false;
	}

	@Override
	public void onEndMove(int velocity) {
		int tooMuchRightDelta = 0;
		int tooMuchLeftDelta = 0;
		int notEnoughtRightDelta = 0;
		int notEnoughtLeftDelta = 0;
		int nbChild = layoutHolder.getChildCount();
		for (int i = 0; i < nbChild; i++) {
			View referenceMoveView = layoutHolder.getChildAt(i);
			StackLayoutParams referenceMoveViewLp = (StackLayoutParams) referenceMoveView.getLayoutParams();
			
			int underLeft = layoutHolder.getLeft();
			int underRight = layoutHolder.getRight();
			if(referenceMoveViewLp.underView!=null){
				View underReferenceMoveView = referenceMoveViewLp.underView;
				StackLayoutParams underReferenceMoveLp = (StackLayoutParams) underReferenceMoveView.getLayoutParams();

				underRight = underReferenceMoveLp.left + underReferenceMoveView.getMeasuredWidth();
				underLeft = underReferenceMoveLp.left;
			}

			if(referenceMoveViewLp.left > underRight && tooMuchRightDelta==0){
				tooMuchRightDelta += referenceMoveViewLp.left - underRight;
			}
			if(tooMuchRightDelta!=0 && tooMuchLeftDelta==0){
				animViewLayout(i,tooMuchRightDelta, velocity);
			}
			if(referenceMoveViewLp.left < 0 && tooMuchLeftDelta==0){
				tooMuchLeftDelta += referenceMoveViewLp.left ;
			}
			if(tooMuchLeftDelta!=0 && tooMuchRightDelta==0){
				animViewLayout(i,tooMuchLeftDelta, velocity);
			}
			
			
			if(tooMuchLeftDelta==0  && tooMuchRightDelta==0 ){
				if(lastDirection==RIGHT && notEnoughtRightDelta==0 && referenceMoveViewLp.left != underLeft && referenceMoveViewLp.left < underRight){
					notEnoughtRightDelta += referenceMoveViewLp.left - underRight;
				}
				if(notEnoughtRightDelta!=0 && notEnoughtLeftDelta==0){
					animViewLayout(i,notEnoughtRightDelta, velocity);
				}
				if(lastDirection==LEFT && notEnoughtLeftDelta==0
//							&& referenceMoveViewLp.left != underLeft 
						&& referenceMoveViewLp.left > underLeft){
					notEnoughtLeftDelta += referenceMoveViewLp.left - underLeft;
				}
				if(notEnoughtLeftDelta!=0 && notEnoughtRightDelta==0){
					animViewLayout(i,notEnoughtLeftDelta, velocity);
				}
			}
			
		}
		currentInterceptedTouchedView = NO_CHILD_TOUCHED;
	}

	
	public void animViewLayout(final int i, int delta, int velocity) {
		final View view = layoutHolder.getChildAt(i);
		final StackLayoutParams lp = (StackLayoutParams) view.getLayoutParams();
		final ObjectAnimator animator = ObjectAnimator.ofInt(lp, "left", lp.left-delta);
		if(velocity!=0){
	        velocity = Math.abs(velocity);
	        long duration = Math.round(1000 * Math.abs((float)delta / velocity));
			duration = Math.min(duration, MAX_ANIMATION_DURATION);
			animator.setDuration(duration);
		}
		final int oldLeft = lp.left;
		animator.addUpdateListener(new AnimatorUpdateListener() {
			int oldLeftInside = oldLeft;
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				//part for stoping animation if view is deleted during animation
				int count = layoutHolder.getChildCount();
				if(i>count-1){
					animator.cancel();
					return;
				}
				View currentViewAtPosI = layoutHolder.getChildAt(i);
				if(currentViewAtPosI!= view){
					animator.cancel();
					return;
				}

				if(DEBUG)Log.i(TAG, "clean,\tpos:"+oldLeftInside+"\t"+System.currentTimeMillis());
				Rect dirty = new Rect(oldLeftInside-layoutHolder.getShadowWidth(), layoutHolder.getTop(), oldLeftInside, layoutHolder.getBottom());
				layoutHolder.invalidate(dirty);
				
				
				oldLeftInside = lp.left;
				layoutHolder.updateViewLayout(view, lp);
				
			}
			
			
		});
		animator.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator arg0) {
				if(DEBUG)Log.i(TAG, "onAnimationStart "+System.currentTimeMillis());
				
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				Rect dirty = new Rect(lp.left-layoutHolder.getShadowWidth(), layoutHolder.getTop(), lp.left, layoutHolder.getBottom());
				layoutHolder.invalidate(dirty);
				if(DEBUG)Log.i(TAG, "onAnimationEnd "+System.currentTimeMillis());
			}
			
			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		animator.start();
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
				final View child = layoutHolder.getChildAt(i);
				if (isTransformedTouchPointInView(x, y, child)) {
					return i;
				}
			}
		}
		return NO_CHILD_TOUCHED;
	}
	
	
	private boolean isTransformedTouchPointInView(int x, int y, View child) {
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
