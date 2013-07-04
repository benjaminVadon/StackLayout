package com.mobilisens.widget.stacklayout;

import com.mobilisens.widget.stacklayout.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

public class StackLayout extends ViewGroup{
	
	private final static boolean DEBUG = false;
	private final String LOG_TAG = getClass().getSimpleName();

	private TouchController touchController;
	private MoveController moveController;
	private boolean childrenNeedPosClean = false;
	private int parentWidth = 0;

	private static final LayoutAnimationController addChildAnimationController;
	private static final LayoutAnimationController removeChildAnimationController;

	private static final int MIN_VELOCITY_TO_JUMP = 2000;
	
	static {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(500);
        set.addAnimation(animation);

        addChildAnimationController = new LayoutAnimationController(set);
        
         
         AnimationSet removeSet = new AnimationSet(true);

         animation = new AlphaAnimation(1.0f, 0.0f);
         animation.setDuration(250);
         removeSet.addAnimation(animation);
         animation = new TranslateAnimation(
                 Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 1.0f,
                 Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f
             );
         animation.setDuration(250);
         removeSet.addAnimation(animation);


         removeChildAnimationController = new LayoutAnimationController(removeSet);
	}
	
	public StackLayout(Context context) {
		super(context);
		initStackLayout(context);
	}
	
	public StackLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initStackLayout(context);
	}

	private void initStackLayout(Context context){
		moveController = new MoveController(this);
		touchController = new TouchController(context, moveController);
		this.setSaveEnabled(true);
		int id = getId();
		if(id==View.NO_ID){
			setId(R.id.stackLayout);
		}
	}

	@Override
	public void addView(View viewToAdd, int index, android.view.ViewGroup.LayoutParams params) {
		View decorView = new View(getContext());
		decorView.setBackgroundResource(R.drawable.photo_shadow);
		addView(viewToAdd, decorView, index, params);
		//no more processing after this line
	}
	
	public void addView(View viewToAdd, View decorView, int index, android.view.ViewGroup.LayoutParams params) {
		if(viewToAdd==null){
			Log.e(LOG_TAG, "You need to provide at least a content view");
			return;
		}
		int count = getChildCount();
		index = checkAndGetIndex(index, count);
		count = removeAllUselessView(index, count);
		
		if(viewToAdd instanceof StackViewContainer){
			if(decorView!=null){
				Log.e(LOG_TAG, "Your decorView won't be used because you already provide a StackViewContainer");
			}
			addStackViewContainer((StackViewContainer)viewToAdd, generateLayoutParams(params), index);
		}else{
			addViewInStackViewContainer(viewToAdd, decorView, index, params);
		}
	}
	
	private int checkAndGetIndex(int index, int count) {
		if(index==-1){
			index = count;
		}
		return index;
	}
	
	private int removeAllUselessView(int index, int count) {
		int finalCount = count;
		if (index >= 0 && index < count) {
			for (int i = index; i < count; i++) {
				View deletedView = getChildAt(i);
				deletedView.setAnimation(removeChildAnimationController.getAnimationForView(deletedView));
			}
			int nbViewToRemove = count - index;
			removeViews(index, nbViewToRemove);
			finalCount -= nbViewToRemove;
		}
		return finalCount;
	}


	private void addViewInStackViewContainer(View viewToAdd, View decorView, int index, LayoutParams params) {
		StackViewContainer viewContainer = buildViewContainer(params);
		viewContainer.addContentAndDecorViews(viewToAdd, decorView);
		addStackViewContainer(viewContainer, (StackLayoutParams) viewContainer.getLayoutParams(), index);
	}

	private StackViewContainer buildViewContainer(LayoutParams params) {
		StackViewContainer viewContainer = new StackViewContainer(getContext());
		StackLayoutParams containerLayoutParams = generateLayoutParams(params);
		viewContainer.setLayoutParams(containerLayoutParams);
		return viewContainer;
	}
	
    private void addStackViewContainer(StackViewContainer stackViewContainer, StackLayoutParams params, int index) {
    	stackViewContainer.setIndexInParent(index);
    	StackLayoutParams containerParams = buildStackViewContainerLayoutParams(stackViewContainer, params);
    	
    	
    	final int nbChildBeforeAdd = getChildCount();
		if(nbChildBeforeAdd!=0){

			if(nbChildBeforeAdd>0){
				Animation animation = addChildAnimationController.getAnimation();
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						((StackViewContainer)getChildAt(nbChildBeforeAdd-1)).animToLeftAnchor();
						animation.setAnimationListener(null);
					}
				});
			}

			stackViewContainer.setAnimation(addChildAnimationController.getAnimationForView(stackViewContainer));
		}
		super.addView(stackViewContainer, index, containerParams);
	}

	
	private StackLayoutParams buildStackViewContainerLayoutParams(StackViewContainer stackViewContainer, StackLayoutParams stackContainerParams) {
		return stackContainerParams;
	}

    
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		if(DEBUG){Log.i(LOG_TAG, "onMeasure ");}
		int count = getChildCount();

        int maxHeight = getSuggestedMinimumHeight();
        int maxWidth = getSuggestedMinimumWidth();

        setMeasuredDimension(getDefaultSize(maxWidth, widthMeasureSpec), getDefaultSize(maxHeight, heightMeasureSpec));

        for (int i = 0; i < count; i++) {
            final StackViewContainer child = (StackViewContainer) getChildAt(i);
            final StackLayoutParams lp = (StackLayoutParams) child.getLayoutParams();
            int childWidthMeasureSpec;
            int childHeightMeasureSpec;
            
            if (lp.width == StackLayoutParams.MATCH_PARENT) {
            	int childWidth = getMeasuredWidth();
            	if(lp.bestWidthFromParent){
            		childWidth = (getMeasuredWidth()<getMeasuredHeight())?getMeasuredWidth():getMeasuredHeight();
            		if(childWidth!=parentWidth){
            			parentWidth = childWidth;
            			childrenNeedPosClean = true;
            		}
            		childWidth = (int) ((childWidth * 2.) /3.);
            	}
        		childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,0, lp.width);
            }
            
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            
        }
        childrenNeedPosClean = false;
    }
	
	public boolean childrenNeedPosClean(){
		return childrenNeedPosClean;
	}
	
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final StackLayoutParams lp = (StackLayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft = lp.getViewPos();
                int childTop = top;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                
            }
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
    	if(DEBUG)Log.i(LOG_TAG, "onInterceptTouchEvent "+event.toString());
		if (getChildCount()>0) {
			boolean result = touchController.onInterceptTouchEvent(event);
			if(result){
				moveController.setCurrentInterceptedTouchedView(event);
			}
			return result;
		}
		return false;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
    	if(DEBUG){Log.i(LOG_TAG, "onTouchEvent");}
		if (getChildCount()>0) {
			return touchController.onTouchEvent(event);
		}
		return false;
	}


	public int getActivePointerId() {
		return touchController.getActivePointerId();
	}
	
	
	//Layout Params part
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof StackLayoutParams;
	}
	
	@Override
	protected StackLayoutParams generateDefaultLayoutParams() {
		return new StackLayoutParams(StackLayoutParams.MATCH_PARENT, StackLayoutParams.MATCH_PARENT);
	}
	
	public StackLayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent) {
		StackLayoutParams params = new StackLayoutParams(StackLayoutParams.MATCH_PARENT, StackLayoutParams.MATCH_PARENT);
		params.bestWidthFromParent = bestWidthFromParent;
		params.fixed = fixed;
		return params;
	}
	
	public StackLayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent, String anchorsString) {
		StackLayoutParams params = generateLayoutParams(fixed, bestWidthFromParent);
		params.initAnchors(anchorsString);
		return params;
	}

	public StackLayoutParams generateLayoutParams(boolean fixed, boolean bestWidthFromParent, String anchorsString, int anchorIndexForOpen) {
		StackLayoutParams params = generateLayoutParams(fixed, bestWidthFromParent, anchorsString);
		params.initAnchorIndexForOpen(anchorIndexForOpen);
		return params;
	}
	
	@Override
	public StackLayoutParams generateLayoutParams(AttributeSet attrs) {
		return new StackLayoutParams(getContext(), attrs);
	}
	
	@Override
	protected StackLayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		if(p==null)
			return generateDefaultLayoutParams();
		if (checkLayoutParams(p))
			return (StackLayoutParams) p;
		else
			return new StackLayoutParams(p.width, p.height);
	}

	public static class StackLayoutParams extends ViewGroup.LayoutParams {
		
		private final String TAG = getClass().getSimpleName();
		private static final int POS_NOT_SET = Integer.MIN_VALUE;
		
		boolean bestWidthFromParent = false;
		boolean fixed = false;
		int needShadow = 0;
		private int[] anchors = new int[1];
		private float[] anchorsRef = new float[]{0.f};
		private int anchorIndexForOpen = -1;
		
		private int contentViewPos = POS_NOT_SET;
		private int underPos;
		private int underWidth;
		private int decorViewWidth = 0;
		
		
		public StackLayoutParams(int w, int h) {
			super(w, h);
		}

		public StackLayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StackLayoutParams);
			try {
				bestWidthFromParent = a.getBoolean(R.styleable.StackLayoutParams_best_width_from_parent, false);
				fixed = a.getBoolean(R.styleable.StackLayoutParams_fixed, false);
				
				needShadow = a.getInt(R.styleable.StackLayoutParams_need_shadow, 0);
				String anchorsString = a.getString(R.styleable.StackLayoutParams_anchorsList);
				if(anchorsString!=null && !anchorsString.isEmpty()){
					initAnchors(anchorsString);
				}
				anchorIndexForOpen = a.getInt(R.styleable.StackLayoutParams_anchor_index_for_open, -1);
				
			} finally {
				a.recycle();
			}
			
		}

		private void initAnchors(String anchorsString) {
			String[] anchorsStringList = anchorsString.split(";");
			anchorsRef = new float[anchorsStringList.length];
			anchors = new int[anchorsStringList.length];
			for(int i=0; i<anchorsStringList.length; i++){
				anchorsRef[i] = Float.parseFloat(anchorsStringList[i]);
			}
		}

		private void initAnchorIndexForOpen(int anchorIndexForOpen){
			this.anchorIndexForOpen = anchorIndexForOpen;
		}
		
		public void changeContentViewPos(int moveAmount, boolean hasUpperChild) {
			if(contentViewPos!=POS_NOT_SET){
				if(hasUpperChild){
					int diff = getContentViewPos() - getLeftAnchor();
					if(diff<=moveAmount){
						moveAmount = diff;
					}
				}
				contentViewPos -= moveAmount;
			}
		}

		public boolean isUnAnchored() {
			for(int i=0; i<anchors.length; i++){
				if(anchors[i]==getContentViewPos())
					return false;
			}
			return true;
		}

		//used by objectAnimator for anim panel
		public void setContentViewPos(int pos){
			contentViewPos = pos;
		}
		
		public int getContentViewPos(){
			return contentViewPos;
		}

		public int getViewPos(){
			return contentViewPos-decorViewWidth;
		}
		
		public void setDecorViewWidth(int viewWidth) {
			decorViewWidth = viewWidth;
		}

		public int getDecorViewWidth() {
			return decorViewWidth;
		}
		
		public boolean isViewPosSet() {
			return contentViewPos != POS_NOT_SET;
		}

		public int howMuchShouldMoveFromUpper(int moveAmount, StackLayoutParams upperParams) {
			if(MoveController.isMovingToRight(moveAmount)){
				int diffToBringUnderPanel = upperParams.getRightAnchor() - upperParams.getContentViewPos();
				if(diffToBringUnderPanel<=0){
					return diffToBringUnderPanel;
				}else{
					return 0;
				}
			}else{
				int stopToLeftAnchors = getContentViewPos() - getLeftAnchor();
				if(stopToLeftAnchors<=0)
					return stopToLeftAnchors;
				if(stopToLeftAnchors-moveAmount<=0)
					return stopToLeftAnchors;
				return moveAmount;
			}
			
		}

		private int getLeftAnchor() {
			return anchors[0];
		}
		
		public int getRightAnchor() {
			return anchors[anchors.length-1];
		}

		public int getDistanceToNearestAnchor() {
			if(DEBUG){Log.i(TAG, "getDistanceToNearestAnchor");}
			int result = Integer.MAX_VALUE;
			for(int i=0; i<anchors.length; i++){
				int value = getContentViewPos()-anchors[i];
				if(Math.abs(result)>= Math.abs(value)){
					result = value;
				}
				if(DEBUG){Log.i(TAG, "DistanceToNearestAnchor " +result);}
			}
			return result;
		}

		public int getDistanceToLeftAnchor() {
			return getContentViewPos()-getLeftAnchor();
		}

		public int getDistanceToNearestAnchor(int velocity) {
			int anchor;
			if(velocity>0){
				anchor = getNextAnchor(velocity);
			}else{
				anchor = getPreviousAnchor(velocity);
			}
			
			return getContentViewPos()-anchor;
		}

		private int getNextAnchor(int velocity) {
			int result = getRightAnchor();
			if(!(Math.abs(velocity)>MIN_VELOCITY_TO_JUMP)){
				for(int i=anchors.length-1; i>=0; i--){
					if( anchors[i]<getContentViewPos())
						break;
					else
						result = anchors[i];
				}
			}
			return result;
		}

		private int getPreviousAnchor(int velocity) {
			int result = getLeftAnchor();
			if(!(Math.abs(velocity)>MIN_VELOCITY_TO_JUMP)){
				for(int i=0; i<anchors.length; i++){
					if( anchors[i]>getContentViewPos())
						break;
					else
						result = anchors[i];
				}
			}
			return result;
		}

		public void setUnderPos(int underPos) {
			setUnderPosAndWidth(underPos, 0);
		}

		public void setUnderPosAndWidth(int underPos, int underWidth) {
			this.underPos = underPos;
			this.underWidth = underWidth;
			resetAnchors();
			buildContentViewPos();
		}

		private void buildContentViewPos() {
			if(anchorIndexForOpenIsSet()){
				contentViewPos = anchors[anchorIndexForOpen];
			}else{
				contentViewPos = underPos+underWidth;
			}
		}

		public void updateUnderPos(int underPos){
			this.underPos = underPos;
			resetAnchors();
		}
		
		private void resetAnchors() {
			for(int i=0; i<anchorsRef.length; i++){
				anchors[i]=(int)(anchorsRef[i]*underWidth)+underPos;
			}
		}
		
		public boolean anchorIndexForOpenIsSet(){
			if(anchorIndexForOpen!=-1 && anchorIndexForOpen<anchorsRef.length)
				return true;
			else
				return false;
		}
	}
}
