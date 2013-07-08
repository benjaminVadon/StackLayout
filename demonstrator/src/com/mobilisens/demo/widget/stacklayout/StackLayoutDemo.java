package com.mobilisens.demo.widget.stacklayout;

import com.mobilisens.widget.stacklayout.StackLayout;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class StackLayoutDemo extends Activity{

	

	private static final int NB_IN_LIST = 100;
	private static final String DEFAULT_MAX_CHILD = "5";
	protected final String LOG_TAG = getClass().getSimpleName();
	private StackLayout stackLayout;
	private final int nbBasePanel = 1;
	private TextView nbChildInfo;
	private SeekBar nbChild;
	private ListAdapter listAdapter;
	protected int maxNbChild;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stack_layout_demo);
		stackLayout = (StackLayout)findViewById(R.id.stackLayout);
		initFirstPanel();
	}

	private void initFirstPanel() {
		setNbChildLogic();
		
		nbChildInfo = (TextView) findViewById(R.id.nbChildInfo);
		nbChildInfo.setText(getString(R.string.configurator_nbChild, 0));
		
		setMaxChildLogic();
	}

	private void setNbChildLogic() {
		nbChild = (SeekBar) findViewById(R.id.nbChild);

		OnSeekBarChangeListener nbChildChangeListener = new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int currentNbChild = stackLayout.getChildCount()-nbBasePanel;
				if(progress>currentNbChild){
					for (int i = currentNbChild; i < progress; i++) {
						addPanelToStackLayout();
					}
				}else if(progress<currentNbChild){
					for (int i = currentNbChild; i > progress; i--) {
						removePanelToStackLayout();
					}
				}
				nbChildInfo.setText(getString(R.string.configurator_nbChild, progress));
			}
		};
		nbChild.setOnSeekBarChangeListener(nbChildChangeListener);
	}

	private void setMaxChildLogic() {
		EditText maxChild = (EditText)findViewById(R.id.maxChild);
		OnEditorActionListener maxChildChangeListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
		        if (actionId == EditorInfo.IME_ACTION_GO 
		        || actionId == EditorInfo.IME_ACTION_DONE) {
		        	String value = v.getText().toString();
		        	maxNbChild = (value.length()!=0)? Integer.valueOf(value):100;
	        		nbChild.setMax(maxNbChild);
		            handled = true;
		        }
		        return handled;
			}
		};
		maxChild.setOnEditorActionListener(maxChildChangeListener);//addTextChangedListener(maxChildChangeWatcher);
		maxChild.setText(DEFAULT_MAX_CHILD);
		maxChild.onEditorAction(EditorInfo.IME_ACTION_DONE);
	}

	


	private void addPanelToStackLayout() {
		int indexInStack = stackLayout.getChildCount();
		String anchors;
		if(indexInStack<2){
			anchors =  "1.;1.";
		}else{
			anchors =  "0.;0.5;1.";
		}
		stackLayout.addView(buildSimpleList(indexInStack), indexInStack, stackLayout.generateLayoutParams(false, true, anchors, 1));
	}

	private View buildSimpleList(final int indexInStack) {
		ListView list = new ListView(getApplicationContext());
		int nbPanel = stackLayout.getChildCount()-1;
		list.setBackgroundColor(Color.GRAY- (0x80808 * nbPanel)%Color.GRAY);

		if(listAdapter==null){
			buildListAdapter();
		}
		
		list.setAdapter(listAdapter);
		list.setOnItemClickListener(buildOnItemClickListener(indexInStack));
		return list;
	}

	private OnItemClickListener buildOnItemClickListener(final int indexInStack) {
		return new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(indexInStack<maxNbChild){
					int currentNbPanels = stackLayout.getChildCount()-1;
					if(currentNbPanels>indexInStack){
						nbChild.setProgress(indexInStack);
					}
					nbChild.incrementProgressBy(1);
				}
			}
		};
		
	}


	private void buildListAdapter() {
		String[] objects = new String[NB_IN_LIST];
		for(int i=0; i<objects.length; i++){
			objects[i] = "element "+i;
		}
		listAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.standard_row, R.id.textContent, objects);
	}

	private void removePanelToStackLayout() {
		stackLayout.removeViewAt(stackLayout.getChildCount()-1);
	}
}
