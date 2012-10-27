package shira.android.facebook.updaterfollower.search;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

public class FacebookResultsAutoComplete extends FrameLayout
{
	private static final String SUPER_STATE_KEY_NAME="shira.android.SUPER_STATE";
	private static final String CATEGORIES_KEY_NAME="shira.android.facebook." +
			"CATEGORIES";
	
	private AutoCompleteTextView resultsAutoComplete;
	private FacebookResultsAdapter resultsAdapter;
	private String[] categories;
	private int listPosition=AdapterView.INVALID_POSITION;
	
	/*This listener lets the component follow changes to the selected item in 
	 *the list. It also handles touch mode, when there's actually no "selection"
	 *(instead, there are item clicks). In the future this can be used to build 
	 *a more general component that directly inherits from AutoCompleteTextView 
	 *and adds this functionality.*/
	private class ListPositionChangeListener implements TextWatcher,AdapterView.
			OnItemClickListener,AdapterView.OnItemSelectedListener
	{
		@Override
		public void onItemSelected(AdapterView<?> parent,View view,int position,
				long id) 
		{ listPosition=position; }

		@Override public void onNothingSelected(AdapterView<?> parent) 
		{ listPosition=AdapterView.INVALID_POSITION; }

		@Override
		public void onItemClick(AdapterView<?> parent,View view,int position,
				long id) 
		{ 
			listPosition=position;
			Log.i("Auto","Position: " + position);
		}

		@Override public void afterTextChanged(Editable s) 
		{ 
			listPosition=AdapterView.INVALID_POSITION;
			Log.i("Auto","Nothing");
		}

		@Override
		public void beforeTextChanged(CharSequence s,int start,int count,
				int after) { }

		@Override
		public void onTextChanged(CharSequence s,int start,int before,
				int count) { }
	}
	
	public FacebookResultsAutoComplete(Context context)
	{ this(context,(String[])null); }
	
	public FacebookResultsAutoComplete(Context context,String[] categories)
	{ this(context,categories,FacebookResultsAdapter.DEFAULT_MAX_RESULTS); }
	
	public FacebookResultsAutoComplete(Context context,AttributeSet attributes)
	{ this(context,attributes,0); }
	
	public FacebookResultsAutoComplete(Context context,AttributeSet attributes,
			int defStyle)
	{
		super(context,attributes,defStyle);
		initialize(context);
		TypedArray attributesArray=context.obtainStyledAttributes(attributes,
				R.styleable.ResultsAutoComplete,defStyle,defStyle);
		CharSequence[] categoriesCS=attributesArray.getTextArray(R.styleable.
				ResultsAutoComplete_categories);
		int maxResults=attributesArray.getInt(R.styleable.ResultsAutoComplete_max_results,
				FacebookResultsAdapter.DEFAULT_MAX_RESULTS);
		attributesArray.recycle();
		if (categoriesCS!=null)
		{
			String[] categories=new String[categoriesCS.length];
			for (int index=0;index<categoriesCS.length;index++)
				categories[index]=categoriesCS[index].toString();
			setCategories(categories);
		}
		setMaxResults(maxResults);
	}
	
	public FacebookResultsAutoComplete(Context context,String[] categories,
			int maxResults)
	{
		super(context);
		initialize(context);
		setCategories(categories);
		setMaxResults(maxResults);
	}
	
	private void initialize(Context context)
	{
		if (!(context instanceof Activity))
			throw new ClassCastException("Only activities are supported as " +
					"context objects for this view!");
		resultsAutoComplete=(AutoCompleteTextView)inflate(context,R.layout.
				results_auto_complete,null);
		resultsAdapter=new FacebookResultsAdapter((Activity)context);
		resultsAutoComplete.setAdapter(resultsAdapter);
		/*ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.
				MATCH_PARENT);*/
		ListPositionChangeListener listPositionListener=new ListPositionChangeListener();
		resultsAutoComplete.setOnItemClickListener(listPositionListener);
		resultsAutoComplete.setOnItemSelectedListener(listPositionListener);
		resultsAutoComplete.addTextChangedListener(listPositionListener);
		this.addView(resultsAutoComplete); //,layoutParams);
	}
	
	public String[] getCategories() 
	{ return (categories!=null?categories.clone():null); }
	
	public void setCategories(String[] categories)
	{
		this.categories=(categories!=null?categories.clone():null);
		resultsAdapter.setCategories(this.categories);
	}
	
	public int getMaxResults() { return resultsAdapter.getMaxResults(); }
	public void setMaxResults(int maxResults) 
	{ resultsAdapter.setMaxResults(maxResults); }
	
	public String getTextForAutoCompletion()
	{ return resultsAutoComplete.getText().toString(); }
	public void setTextForAutoCompletion(String text)
	{ resultsAutoComplete.setText(text); }
	
	/*Since AutoCompleteTextView inherits only from EditText and (probably) 
	 *wraps an AdapterView for adding the suggestions list, there's no direct 
	 *way to call the methods AdapterView supplies for getting information on 
	 *the items in the list. The functions added here fill the gap, and also 
	 *handle the touch mode issue of no item "selection", by using the current 
	 *position in the list as being tracked by the ListPositionChangeListener 
	 *(see above). This functionality will also be transferred to the custom 
	 *component enhancing AutoCompleteTextView.*/
	
	public int getCount() { return resultsAdapter.getCount(); }
	
	public Object getItemAtPosition(int position)
	{ return resultsAdapter.getItem(position); }
	
	public long getItemIdAtPosition(int position)
	{ return resultsAdapter.getItemId(position); }
	
	/*The functions dealing with the current list position use a slightly 
	 *different convention in their names than the counterpart functions in the 
	 *AdapterView class (namely, replacing the term "selected" with "current").
	 *This is done in order to distinguish the meaning and functionality between
	 *the 2 sets. The functions in AdapterView work with the "selected" item in 
	 *the list, and thus can't be used when the device is in touch mode, since 
	 *there's no item "selection". Contrary to them, the functions in this class,
	 *as was mentioned above, can also handle touch mode by using the tracked 
	 *position (it's used for all modes), and thus have different names. 
	 *The functions that deal with an arbitrary position or are not dependent of 
	 *a position at all (defined above) work the same way for the 2 classes, so 
	 *they use the same names.*/
	
	public Object getCurrentItem() 
	{
		if (listPosition!=AdapterView.INVALID_POSITION) 
			return resultsAdapter.getItem(listPosition);
		else return null;
	}
	
	public long getCurrentItemId()
	{
		if (listPosition!=AdapterView.INVALID_POSITION) 
			return resultsAdapter.getItemId(listPosition);
		else return AdapterView.INVALID_ROW_ID;
	}
	
	public int getCurrentItemPosition() { return listPosition; }
	
	public View getCurrentView() 
	{
		if (listPosition!=AdapterView.INVALID_POSITION)
			return resultsAdapter.getView(listPosition,null,null);
		else return null;
	}
	
	/*The name of the setter function for the list position is slightly 
	 *different in convention than its counterpart (setSelection), not only by 
	 *replacing "selection" with "current", but also by adding a few more words 
	 *to make the meaning clearer.*/
	public void setCurrentItemPosition(int listPosition)
	{
		if (((listPosition<0)||(listPosition>=resultsAdapter.getCount()))&&
				(listPosition!=AdapterView.INVALID_POSITION))
			throw new IllegalArgumentException("The position in the suggestions " +
					"list must be between 0 and the number of suggestions, " + 
					"which right now stands on " + resultsAdapter.getCount() +
					", or alternatively, set to the value of " + AdapterView.
					INVALID_POSITION + ", which indicates no current position " +
					"(this is the only valid value if there are no suggestions)");
		this.listPosition=listPosition;
	}
	
	@Override protected Parcelable onSaveInstanceState()
	{
		Bundle bundle=new Bundle();
		bundle.putParcelable(SUPER_STATE_KEY_NAME,super.onSaveInstanceState());
		bundle.putStringArray(CATEGORIES_KEY_NAME,categories);
		return bundle;
	}
	
	@Override protected void onRestoreInstanceState(Parcelable state)
	{
		if (state instanceof Bundle)
		{
			Bundle bundle=(Bundle)state;
			super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY_NAME));
			categories=bundle.getStringArray(CATEGORIES_KEY_NAME);
		}
		else super.onRestoreInstanceState(state);
	}
}
