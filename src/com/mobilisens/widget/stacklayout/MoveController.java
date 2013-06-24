package com.mobilisens.widget.stacklayout;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

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
	private boolean newMode = true;
	
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
		int upperChild = getUpperChild();
		if(upperChild<0)
			return;
		int viewReferenceIndex = getViewIndexTouchReference(upperChild);
		((StackViewContainer) layoutHolder.getChildAt(viewReferenceIndex)).movePanel(moveAmount);

		lastDirection = isMovingToRight(moveAmount);
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
    	if(newMode){

    		int upperChild = getUpperChild();
    		if(upperChild<0)
    			return;
    		int viewReferenceIndex = getViewIndexTouchReference(upperChild);
    		StackViewContainer child = ((StackViewContainer) layoutHolder.getChildAt(viewReferenceIndex));
    		child.animToNearestAnchor(velocity);
			currentInterceptedTouchedView = NO_CHILD_TOUCHED;
    	}else{
			int tooMuchRightDelta = 0;
			int tooMuchLeftDelta = 0;
			int notEnoughtRightDelta = 0;
			int notEnoughtLeftDelta = 0;
			int nbChild = layoutHolder.getChildCount();
			for (int i = 0; i < nbChild; i++) {
				View referenceMoveView = layoutHolder.getChildAt(i);
				StackLayoutParams referenceMoveViewLp = (StackLayoutParams) referenceMoveView.getLayoutParams();
				if(!referenceMoveViewLp.fixed){
					int underLeft = layoutHolder.getLeft();
					int underRight = layoutHolder.getRight();
					if(referenceMoveViewLp.underView!=null){
						StackViewContainer underReferenceMoveView = referenceMoveViewLp.underView;
						StackLayoutParams underReferenceMoveLp = (StackLayoutParams) underReferenceMoveView.getLayoutParams();
		
						underRight = underReferenceMoveLp.getContentViewPos() + underReferenceMoveView.getMeasuredWidthWithoutDecorView();
						underLeft = underReferenceMoveLp.getContentViewPos();
					}
		
					if(referenceMoveViewLp.getContentViewPos() > underRight && tooMuchRightDelta==0){
						tooMuchRightDelta += referenceMoveViewLp.getContentViewPos() - underRight;
					}
					if(tooMuchRightDelta!=0 && tooMuchLeftDelta==0){
						animViewLayout(i,tooMuchRightDelta, velocity);
					}
					Log.i(TAG, "tooMuchLeftDelta  before "+tooMuchLeftDelta);
					if(referenceMoveViewLp.getContentViewPos() < underLeft && tooMuchLeftDelta==0){
						tooMuchLeftDelta += referenceMoveViewLp.getContentViewPos() - underLeft;
					}
					Log.i(TAG, "tooMuchLeftDelta after"+tooMuchLeftDelta);
					if(tooMuchLeftDelta!=0 && tooMuchRightDelta==0){
						animViewLayout(i,tooMuchLeftDelta, velocity);
					}
	
					Log.i(TAG, "before toomuch evaluation");
					Log.i(TAG, "tooMuchLeftDelta "+tooMuchLeftDelta+ " tooMuchRightDelta "+tooMuchRightDelta);
					
					if(tooMuchLeftDelta==0  && tooMuchRightDelta==0 ){
						Log.i(TAG, "referenceMoveViewLp.left "+referenceMoveViewLp.getContentViewPos()+ " underLeft "+underLeft);
						
						if(lastDirection==RIGHT && notEnoughtRightDelta==0 && referenceMoveViewLp.getContentViewPos() != underLeft && referenceMoveViewLp.getContentViewPos() < underRight){
							notEnoughtRightDelta += referenceMoveViewLp.getContentViewPos() - underRight;
						}
						if(notEnoughtRightDelta!=0 && notEnoughtLeftDelta==0){
							animViewLayout(i,notEnoughtRightDelta, velocity);
						}
						if(lastDirection==LEFT && notEnoughtLeftDelta==0
		//							&& referenceMoveViewLp.left != underLeft 
								&& referenceMoveViewLp.getContentViewPos() > underLeft){
							notEnoughtLeftDelta += referenceMoveViewLp.getContentViewPos() - underLeft;
						}
						if(notEnoughtLeftDelta!=0 && notEnoughtRightDelta==0){
							animViewLayout(i,notEnoughtLeftDelta, velocity);
						}
					}
	
				
				}
			}
			currentInterceptedTouchedView = NO_CHILD_TOUCHED;
    	}
	}

	
	public void animViewLayout(final int i, int delta, int velocity) {
		final View view = layoutHolder.getChildAt(i);
		final StackLayoutParams lp = (StackLayoutParams) view.getLayoutParams();
		final ObjectAnimator animator = ObjectAnimator.ofInt(lp, "contentViewPos", lp.getContentViewPos()-delta);
		if(velocity!=0){
	        velocity = Math.abs(velocity);
	        long duration = Math.round(1000 * Math.abs((float)delta / velocity));
			duration = Math.min(duration, MAX_ANIMATION_DURATION);
			animator.setDuration(duration);
		}
		final int oldLeft = lp.getContentViewPos();
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
				
				oldLeftInside = lp.getContentViewPos();
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
