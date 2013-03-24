package com.mobilisens.widget.stacklayout;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
//import android.view.View;
import android.view.ViewConfiguration;

public class StackController {
	
	private final boolean DEBUG = false;
	
	private static String TAG = "StackController";
	private static final int INVALID_POINTER = -1;
	private OnMoveListener onMoveListener;
	private float initialMotionX;
	private float currentMotionX;
	private float lastMotionX;
	private float lastMotionY;
	private int activePointerId;
	private boolean isMoving;
	private static float touchSlop;
	private static int maximumVelocity;

    private VelocityTracker velocityTracker;

	public interface OnMoveListener{
		public void onStartMove();
		
		public void onMove (int moveAmount);
		public void onEndMove(int velocity);
	}
	
	public StackController (Context context, StackLayout stackLayout){
//		this.context = context;
		setOnMoveListener(stackLayout);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledPagingTouchSlop();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	public void setOnMoveListener(OnMoveListener moveListener){
		this.onMoveListener = moveListener;
	}

	public boolean onInterceptTouchEvent(MotionEvent event) {
		final int action = actionOfEvent(event);
		return treatInterceptedAction(action, event);
	}

	private int actionOfEvent(MotionEvent event) {
		return event.getAction() & MotionEvent.ACTION_MASK;
	}
	
	private boolean treatInterceptedAction(int action, MotionEvent event) {
		if(actionIsFinished(action)){
        	endMove(false);
			return false;
		}
		if(isNotNewGesture(action)){
			return true;
		}
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			treatInterceptedActionDown(event);
			break;
		case MotionEvent.ACTION_MOVE:
			treatInterceptedActionMove(event);
			break;
		}
		addMovementForVelocity(event);
		return isMoving;
	}

	private void addMovementForVelocity(MotionEvent event) {
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
		velocityTracker.addMovement(event);
	}

	private void treatInterceptedActionDown(MotionEvent event) {
		treatActionDown(event);
	}
	
	private void treatInterceptedActionMove(MotionEvent event) {
		if (activePointerId == INVALID_POINTER) {
            return;
        }
		treatActionMove(event);
	}

	private boolean isNotNewGesture(int action) {
		if (action != MotionEvent.ACTION_DOWN) {
            if (isMoving) {
                return true;
            }
		}
		return false;
	}

	private boolean actionIsFinished(int action){
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return true;
        }
        return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		final int action = actionOfEvent(event);
		addMovementForVelocity(event);
		return treatAction(action, event);
	}

	
	private boolean treatAction(int action, MotionEvent event) {
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			return treatActionDown(event);

		case MotionEvent.ACTION_MOVE:
			return treatActionMove(event);

        case MotionEvent.ACTION_UP:
			return treatActionUp(event);

        case MotionEvent.ACTION_POINTER_DOWN:
			return treatActionPointerDown(event);
			
        case MotionEvent.ACTION_POINTER_UP:
			return treatActionPointerUp(event);
			
		case MotionEvent.ACTION_CANCEL:
			return treatActionCancel(event);
		}
		return false;
	}

	private boolean treatActionDown(MotionEvent event) {
    	if(DEBUG)Log.i(TAG, "treatActionDown");
        currentMotionX = lastMotionX = initialMotionX = event.getX();
        lastMotionY = event.getY();
        activePointerId = event.getPointerId(0);
		return true;
	}
	
	private boolean treatActionMove(MotionEvent event) {
		if(DEBUG)Log.i(TAG, "treatActionMove");
		if(!isMoving){
	        if (shouldMove(event)) {
	        	startMove();
	        }else{
	        	isMoving = false;
	        	if(DEBUG)Log.i(TAG, "move under touchSlop or move verticaly");
	        }
		}
        if (isMoving){
        	performMove(event);
        }
		return true;
	}


	private boolean shouldMove(MotionEvent event) {
		final int pointerIndex = event.findPointerIndex(activePointerId);
		currentMotionX = event.getX(pointerIndex);
		final float xDiff = Math.abs(currentMotionX - lastMotionX);
        final float y = event.getY(pointerIndex);
        final float yDiff = Math.abs(y - lastMotionY);
        return xDiff > touchSlop && xDiff > yDiff;
	}

	private void startMove() {
    	isMoving = true;
    	lastMotionX = initialMotionX + (currentMotionX - initialMotionX > 0 ?  touchSlop :-touchSlop);

		 if (onMoveListener != null) {
			 onMoveListener.onStartMove();
		 }		
	}
	
	private void performMove(MotionEvent event) {
		final int pointerIndex = event.findPointerIndex(activePointerId);
		currentMotionX = event.getX(pointerIndex);
        final float deltaX = lastMotionX - currentMotionX;
        int minDep = 1;
        if(Math.abs(deltaX)>minDep){//TODO not work well hack for gutter touch , but must use android viewpager method for gutter touch

	        int moveAmount = (int) deltaX;
	        lastMotionX = currentMotionX;
	
			 if (onMoveListener != null && moveAmount!=0) {
				 onMoveListener.onMove(moveAmount);
			 }
			 
			lastMotionX += deltaX - moveAmount;
        }
	}

	private boolean treatActionUp(MotionEvent event) {
		if(DEBUG)Log.i(TAG, "treatActionUp");
    	endMove(true);
    	return true;
	}
	
	private void endMove(boolean useVelocity) {
		 if (isMoving) {
			 isMoving = false;

             activePointerId = INVALID_POINTER;

             int velocity = 0;
             if(useVelocity){
            	 final VelocityTracker velocityTracker = this.velocityTracker;
                 velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                 velocity = (int) velocityTracker.getXVelocity(activePointerId);
             }
             if (onMoveListener != null) {
				 onMoveListener.onEndMove(velocity);
			 }
			if (velocityTracker != null) {
				velocityTracker.recycle();
				velocityTracker = null;
			}
		 }
	}

	private boolean treatActionPointerDown(MotionEvent event) {
		if(DEBUG)Log.i(TAG, "treatActionPointerDown");
		final int index = event.getActionIndex();
		final float x = event.getX(index);
		lastMotionX = x;
	    activePointerId = event.getPointerId(index);
		return true;
	}

	private boolean treatActionPointerUp(MotionEvent event) {
//		if(DEBUG)Log.i(TAG, "treatActionPointerUp");
    	onSecondaryPointerUp(event);
        lastMotionX = event.getX(event.findPointerIndex(activePointerId));
    	return true;
	}
    private void onSecondaryPointerUp(MotionEvent event) {
        final int pointerIndex = event.getActionIndex();
        final int pointerId = event.getPointerId(pointerIndex);
        if (pointerId == activePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
//            lastMotionX = event.getX(newPointerIndex);
            activePointerId = event.getPointerId(newPointerIndex);
            if (velocityTracker != null) {
                velocityTracker.clear();
            }
        }
    }
    
	private boolean treatActionCancel(MotionEvent event) {
		if(DEBUG)Log.i(TAG, "treatActionCancel");
    	endMove(false);
		return true;
	}
	
	public int getCurrentPointerIndex(MotionEvent event){
		final int pointerIndex = event.findPointerIndex(activePointerId);
		return pointerIndex;
	}
}
