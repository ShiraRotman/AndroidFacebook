package shira.android.facebook.updaterfollower.search;

import java.util.*;

import shira.android.facebook.FacebookAuthActivity;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class TestSearchFacebookService extends Activity
{
	private static final int FACEBOOK_AUTH_REQUEST_CODE=1;
	
	private ServiceConnection serviceConnection;
	private SearchFacebookService.SearchFacebookServer searchFacebookServer;
	private SearchFacebookService.ExtraCriteria extraCriteria;
	private SearchThresholdTester thresholdTester;
	private SearchFacebookService.SearchCompleteCallback completeListener;
	private String accessToken;
	
	private class SearchFacebookClickListener implements View.OnClickListener
	{
		public void onClick(View view)
		{
			Map<String,String> searchParamsMap=new HashMap<String,String>();
			searchParamsMap.put("access_token",accessToken);
			EditText queryParamEdit=(EditText)findViewById(R.id.query_edit);
			searchParamsMap.put("q",queryParamEdit.getEditableText().toString());
			queryParamEdit=(EditText)findViewById(R.id.type_edit);
			searchParamsMap.put("type",queryParamEdit.getEditableText().toString());
			queryParamEdit=(EditText)findViewById(R.id.category_edit);
			String categoriesInput=queryParamEdit.getEditableText().toString();
			if (categoriesInput!=null)
			{
				String[] categories=categoriesInput.split(",");
				if (extraCriteria==null)
				{
					//categoriesSet=new HashSet<String>();
					extraCriteria=new SearchFacebookService.ExtraCriteria(
							categories);
				}
				//else categoriesSet=extraCriteria.categories;
				else extraCriteria.setCategories(categories);
			}
			if (thresholdTester==null) 
				thresholdTester=new SearchThresholdTester(); 
			else thresholdTester.resetTimes();
			if (completeListener==null)
				completeListener=new SearchCompleteListener();
			if (searchFacebookServer!=null) 
			{
				searchFacebookServer.searchFacebook(searchParamsMap,
						extraCriteria,thresholdTester,completeListener,null);
			}
		}
	}
	
	private class SearchThresholdTester implements SearchFacebookService.
			SearchThresholdTester
	{
		private int nextTimes=0,prevTimes=0;
		
		public void resetTimes() { nextTimes=0; prevTimes=0; }
		
		@Override public int searchPreviousRecords(int limit,Long threshold) 
		{ return ((++prevTimes)<=4?25:0); }
		//{ return false; }
		
		@Override public int searchNextRecords(int limit,Long threshold) 
		{ return ((++nextTimes)<=3?25:0); }
		//{ return false; }
	}
	
	private class SearchCompleteListener implements SearchFacebookService.
			SearchCompleteCallback
	{
		@Override public void searchSucceeded(List<Map<String,Object>> result)
		{
			StringBuilder resultTextBuilder=new StringBuilder();
			for (Map<String,Object> resultElement:result)
				for (String propertyName:resultElement.keySet())
				{
					resultTextBuilder.append(propertyName);
					resultTextBuilder.append(": ");
					resultTextBuilder.append(resultElement.get(propertyName)
							.toString());
				}
			TextView resultTextView=(TextView)findViewById(R.id.result_text);
			resultTextView.setText(resultTextBuilder.toString());
		}

		@Override public void searchFailed(Exception exception) 
		{ throw new RuntimeException(exception); }
	}
	
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_search_service);
		Button searchButton=(Button)findViewById(R.id.search_button);
		searchButton.setOnClickListener(new SearchFacebookClickListener());
		searchButton.setEnabled(false);
		Intent authIntent=new Intent(FacebookAuthActivity.FACEBOOK_AUTH_ACTION);
		startActivityForResult(authIntent,FACEBOOK_AUTH_REQUEST_CODE);
	}
	
	@Override 
	protected void onActivityResult(int requestCode,int resultCode,Intent data)
	{
		if (requestCode==FACEBOOK_AUTH_REQUEST_CODE)
		{
			if (resultCode==RESULT_OK)
			{
				accessToken=data.getStringExtra(FacebookAuthActivity.
						ACCESS_TOKEN_EXTRA_NAME);
				Log.i("Facebook","Token: " + accessToken);
				Button searchButton=(Button)findViewById(R.id.search_button);
				searchButton.setEnabled(true);
			}
			else if (resultCode==RESULT_CANCELED)
			{
				String errorMessage=data.getStringExtra(FacebookAuthActivity.
						ERROR_MESSAGE_EXTRA_NAME);
				if (errorMessage==null) errorMessage="";
				throw new RuntimeException(errorMessage);
			}
		}
		else super.onActivityResult(requestCode,resultCode,data);
	}
	
	@Override protected void onStart()
	{
		super.onStart();
		Intent serviceIntent=new Intent(this,SearchFacebookService.class);
		if (serviceConnection==null) serviceConnection=new ServiceConnection()
		{
			@Override 
			public void onServiceConnected(ComponentName name,IBinder service) 
			{ 
				searchFacebookServer=(SearchFacebookService.SearchFacebookServer)
						service; 
			}

			@Override public void onServiceDisconnected(ComponentName name) 
			{ searchFacebookServer=null; }
		};
		bindService(serviceIntent,serviceConnection,BIND_AUTO_CREATE);
	}
	
	@Override protected void onStop()
	{
		super.onStop();
		unbindService(serviceConnection);
		searchFacebookServer=null;
	}
}
