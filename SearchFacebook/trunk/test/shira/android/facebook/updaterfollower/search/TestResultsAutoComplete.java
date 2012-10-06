package shira.android.facebook.updaterfollower.search;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;

public class TestResultsAutoComplete extends Activity 
{
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		FacebookResultsAutoComplete resultsAutoComplete=new 
				FacebookResultsAutoComplete(this);
		LayoutParams layoutParams=new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		setContentView(resultsAutoComplete,layoutParams);
	}
}
