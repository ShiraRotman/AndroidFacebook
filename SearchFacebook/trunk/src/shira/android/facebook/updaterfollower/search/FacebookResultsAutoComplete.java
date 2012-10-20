package shira.android.facebook.updaterfollower.search;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
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
