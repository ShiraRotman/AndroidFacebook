package shira.android.facebook.updaterfollower.search;

import java.util.*;

import android.app.Activity;
import android.content.*;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class FacebookResultsAdapter extends BaseAdapter implements Filterable 
{
	public static final int DEFAULT_MAX_RESULTS=SearchFacebookFilter.
			MAX_RESULTS_IN_PAGE;
	public static final int PAGE_VIEW_TYPE=0;
	
	private SearchFacebookService.SearchFacebookServer searchFacebookServer;
	private SearchFacebookService.ExtraCriteria extraCriteria;
	private ServiceConnection serviceConnection;
	private ServiceBindingHandler serviceBindingHandler;
	private SearchFacebookFilter searchFilter;
	private List<PageResult> results;
	private Activity containerActivity;
	//private String[] categories;
	private String accessToken;
	private int maxResults;
	
	/*The queries that are sent to Facebook from this adapter only ask for a  
	 *few fields of Facebook pages that match the criteria. Therefore, they 
	 *don't require an access token. If in the future this adapter is expanded 
	 *to build its contents from other types of queries, support for retrieving 
	 *an access token will have to be added. However, I can't use the activity 
	 *we have to start the authentication activity, because it doesn't support 
	 *handling the result. Thus, I'll have to start an intermediate activity 
	 *instead, which will retrieve the access token from the authentication 
	 *activity and update the adapter. While an access token is been retrieved,
	 *the search results will be empty, until the query succeeds. The access 
	 *token is still defined here though, for future use.*/
	
	public static class PageResult
	{
		long id;
		String name;
		String category;
		/*Originally, I planned to also include the picture of the page, but 
		 *this would require downloading the pictures for all the results 
		 *every time the search text changes, which might occur in small 
		 *durations, especially if it's taken from user input and updated every 
		 *key press. I could create a cache of already downloaded pictures, 
		 *either in memory or storage, but that would take a lot of space, and 
		 *in mobile environments the resources are very limited.*/
		
		PageResult() { }
		PageResult(long id,String name,String category)
		{ this.id=id; this.name=name; this.category=category; }
		
		public long getId() { return id; }
		public String getName() { return name; }
		public String getCategory() { return category; }
		
		@Override public String toString() { return name; }
	}
	
	private class ServiceBindingHandler extends AbsActivityLifecycleCallbacks
	{
		@Override public void onActivityStarted(Activity activity) 
		{ 
			if (containerActivity==activity)
			{
				Intent serviceIntent=new Intent(containerActivity,
						SearchFacebookService.class);
				if (serviceConnection==null) 
				{
					serviceConnection=new ServiceConnection()
					{
						@Override
						public void onServiceConnected(ComponentName name,
								IBinder service) 
						{ 
							searchFacebookServer=(SearchFacebookService.
									SearchFacebookServer)service; 
						}

						@Override
						public void onServiceDisconnected(ComponentName name) 
						{ 
							searchFacebookServer=null; 
							results=null;
						}
					};
				}
				activity.bindService(serviceIntent,serviceConnection,Context.
						BIND_AUTO_CREATE);
			} //end if containerActivity==activity
		} //end onActivityStarted
		
		@Override public void onActivityStopped(Activity activity) 
		{ 
			if (containerActivity==activity)
				containerActivity.unbindService(serviceConnection);
		}
		
		@Override public void onActivityDestroyed(Activity activity) 
		{
			if (containerActivity==activity)
			{
				containerActivity.getApplication().unregisterActivityLifecycleCallbacks(
						serviceBindingHandler);
				containerActivity=null;
			}
		}
	} //end ServiceBindingHandler
	
	private class SearchFacebookFilter extends Filter
	{
		public static final int MAX_RESULTS_IN_PAGE=6; 
		
		private SearchThresholdTester thresholdTester;
		private SearchCompleteCallback searchCallback;
		
		private class SearchThresholdTester implements SearchFacebookService.
				SearchThresholdTester 
		{
			private int remainingResults;
			
			public SearchThresholdTester(int maxResults)
			{ remainingResults=maxResults; }
			
			public void reset(int maxResults) { remainingResults=maxResults; }
			
			@Override 
			public int searchPreviousRecords(int limit,Long threshold) 
			{ return 0; }
			
			@Override public int searchNextRecords(int limit,Long threshold) 
			{
				remainingResults-=limit;
				int newLimit=Math.min(remainingResults,MAX_RESULTS_IN_PAGE);
				Log.i("Facebook","New limit: " + newLimit);
				return newLimit;
			}
		} //end SearchThresholdTester
		
		private class SearchCompleteCallback implements SearchFacebookService.
				SearchCompleteCallback
		{
			public FilterResults filterResults;
			
			@Override 
			public void searchSucceeded(List<Map<String,Object>> result) 
			{ handleSearchCompletion(result,result.size()); }

			@Override
			public void searchFailed(Exception exception) 
			{ handleSearchCompletion(exception,-1); }
			
			private synchronized void handleSearchCompletion(Object output,
					int count)
			{
				filterResults=new FilterResults();
				filterResults.values=output;
				filterResults.count=count;
				this.notify();
			}
		}
		
		@Override 
		protected FilterResults performFiltering(CharSequence constraint) 
		{
			if ((constraint==null)||(constraint.equals(""))) return null; 
			Map<String,String> searchParamsMap=new HashMap<String,String>();
			searchParamsMap.put("q",constraint.toString());
			searchParamsMap.put("type","page");
			searchParamsMap.put("limit",String.valueOf(Math.min(maxResults,
					MAX_RESULTS_IN_PAGE)));
			if (accessToken!=null) 
				searchParamsMap.put("access_token",accessToken);
			if (thresholdTester==null) 
				thresholdTester=new SearchThresholdTester(maxResults);
			else thresholdTester.reset(maxResults);
			if (searchCallback==null) 
				searchCallback=new SearchCompleteCallback();
			/*Might cause a race condition if the service connectivity changes 
			 *at the same time the interface object is used by the client!*/
			if (searchFacebookServer!=null)
			{
				searchCallback.filterResults=null;
				searchFacebookServer.searchFacebook(searchParamsMap,
						extraCriteria,thresholdTester,searchCallback,null);
				synchronized (searchCallback)
				{
					while (searchCallback.filterResults==null)
					{
						try { searchCallback.wait(); }
						catch (InterruptedException intException) { }
					}
				}
				return searchCallback.filterResults;
			}
			else return null;
		}

		@Override
		protected void publishResults(CharSequence constraint,FilterResults 
				filterResults) 
		{
			if ((filterResults==null)||(filterResults.count==-1)) results=null;
			else
			{
				List<Map<String,Object>> resultsDataList=(List<Map<String,Object>>)
						filterResults.values;
				if ((resultsDataList==null)||(resultsDataList.size()==0)) 
					results=null;
				else
				{
					results=new ArrayList<PageResult>(resultsDataList.size());
					for (Map<String,Object> resultData:resultsDataList)
					{
						PageResult pageResult=new PageResult();
						pageResult.id=Long.valueOf(resultData.get("id").toString());
						pageResult.name=resultData.get("name").toString();
						pageResult.category=resultData.get("category").toString();
						results.add(pageResult);
					}
				}
			} //end else (if filterResults.count==-1)
			notifyDataSetChanged();
		} //end publishResults
	} //end performFiltering
	
	public FacebookResultsAdapter(Activity containerActivity)
	{ this(containerActivity,null,DEFAULT_MAX_RESULTS); }
	
	public FacebookResultsAdapter(Activity containerActivity,String[] categories,
			int maxResults)
	{
		setContainerActivity(containerActivity);
		setCategories(categories);
		setMaxResults(maxResults);
		searchFilter=new SearchFacebookFilter();
	}
	
	public Activity getContainerActivity() { return containerActivity; }
	
	public void setContainerActivity(Activity containerActivity)
	{
		if (containerActivity==null)
			throw new NullPointerException("The activity in which the view " +
					"that uses this adapter is contained must be supplied!");
		if (this.containerActivity!=null)
		{
			this.containerActivity.getApplication().unregisterActivityLifecycleCallbacks(
					serviceBindingHandler);
		}
		else if (serviceBindingHandler==null)
			serviceBindingHandler=new ServiceBindingHandler();
		containerActivity.getApplication().registerActivityLifecycleCallbacks(
				serviceBindingHandler);
		this.containerActivity=containerActivity;
	}
	
	//public String[] getCategories() { return categories.clone(); }
	
	/*There is no getter function for the categories field because it's not 
	 *stored as the caller supplied it (it's passed to the extraCriteria field
	 *and dismissed).*/
	
	/*Not safe to call while there are filter requests that have not yet been 
	 *completed*/
	public void setCategories(String[] categories)
	{ 
		//this.categories=categories.clone();
		if (extraCriteria==null) 
			extraCriteria=new SearchFacebookService.ExtraCriteria(categories);
		else extraCriteria.setCategories(categories);
	}
	
	public int getMaxResults() { return maxResults; }
	
	public void setMaxResults(int maxResults)
	{
		if (maxResults<=0)
			throw new IllegalArgumentException("The maximum search results to " +
					"retrieve must be greater than 0!");
		this.maxResults=maxResults;
	}
	
	@Override public int getCount() { return (results!=null?results.size():0); }
	@Override public int getViewTypeCount() { return 1; }
	@Override public boolean hasStableIds() { return true; }
	@Override public boolean isEmpty() { return (results.size()==0); }
	@Override public boolean areAllItemsEnabled() { return true; }
	
	private void validatePosition(int position)
	{
		if (results==null)
			throw new IllegalArgumentException("The data set is empty!");
		if ((position<0)||(position>results.size()))
			throw new IllegalArgumentException("The position of the item in " + 
					"the data set must be between 0 and " + results.size());
	}
	
	@Override public PageResult getItem(int position) 
	{
		validatePosition(position); 
		return results.get(position);
	}

	@Override public long getItemId(int position) 
	{ return getItem(position).getId(); }

	@Override
	public int getItemViewType(int position) 
	{
		validatePosition(position);
		return PAGE_VIEW_TYPE;
	}
	
	@Override public boolean isEnabled(int position) 
	{
		validatePosition(position);
		return true;
	}
	
	@Override
	public View getView(int position,View convertView,ViewGroup parent) 
	{
		validatePosition(position);
		PageResult pageResult=results.get(position);
		View pageFilterResultView;
		if (convertView==null)
		{
			/*pageFilterResultView=new PageFilterResultView(containerActivity,
					pageResult);*/
			LayoutInflater inflater=(LayoutInflater)containerActivity.
					getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			pageFilterResultView=inflater.inflate(R.layout.page_filter_result,null);
		}
		else pageFilterResultView=convertView;
		/*{
			pageFilterResultView=(PageFilterResultView)convertView;
			pageFilterResultView.updatePageResultData(pageResult);
		}*/
		((TextView)pageFilterResultView.findViewById(R.id.page_name_text)).
				setText(pageResult.name);
		((TextView)pageFilterResultView.findViewById(R.id.page_category_text)).
				setText(pageResult.category);
		return pageFilterResultView;
	}
	
	@Override public Filter getFilter() { return searchFilter; }
}
