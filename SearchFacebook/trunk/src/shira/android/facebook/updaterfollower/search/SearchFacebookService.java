package shira.android.facebook.updaterfollower.search;

import java.io.*;
import java.net.*;
//import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

//import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.JSONParser;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

public class SearchFacebookService extends Service 
{
	/*public static final String EXECUTOR_OBJECT_KEY_NAME="shira.android." +
			"facebook.EXECUTOR";
	public static final String DEFUALT_ACCESS_TOKEN_KEY_NAME="shira.android." +
			"facebook.ACCESS_TOKEN";*/
	public static final String ACTION_SEARCH_FACEBOOK_SERVICE="shira.android." +
			"facebook.action.SEARCH";
	public static final String FAILED_FILTER_KEY_NAME="failed_filter";
	public static final Pattern limitPattern=Pattern.compile("\\&limit=\\d+");
	
	/*In the future this service may be expanded to use a custom Executor in 
	 *order to be able to handle more requests from more clients. In this case 
	 *the executor will have to be passed to the service as a parameter. 
	 *The only way to do that is to put it in the Intent sent in the startService
	 *method, but this will also change the behavior of the service, making it a 
	 *started service in addition to a bound service.*/
	private DefaultSearchExecutor searchTasksExecutor;
	private SearchFacebookServer searchServer;
	private JSONParser jsonParser;
	private String accessToken; //Not used at this time
	
	public static class ExtraCriteria
	{
		private Set<String> categories;
		/*More criteria options may be added in the future, and that's the 
		 *reason the only criterion for now is wrapped, so the interface with 
		 *the client won't have to change for every new option added.*/
		
		public ExtraCriteria(String[] categoriesArray) 
		{ setCategories(categoriesArray); }
		
		/*There is no getter function for the categories field because it's not 
		 *stored exactly as the caller supplied it (the array is converted into 
		 *a set).*/
		
		public void setCategories(String[] categoriesArray)
		{
			/*In order to guarantee matches, I'm using the assumption that every 
			 *Facebook page category has only the first letter capitalized, even 
			 *if the category is composed of more than one word. As of the time 
			 *of this programming, this assumption is true, but if it changes in 
			 *the future, this function will also have to change. Also, for now 
			 *only exact (but case-insensitive) matches are supported. This may 
			 *be expanded to handle prefix matches by using a prefix tree, or 
			 *more complex matches.*/
			if (categoriesArray==null) categories=null;
			else
			{
				if (categories==null) categories=new HashSet<String>();
				else categories.clear();
				for (String category:categoriesArray)
				{
					if ((category!=null)&&(!category.equals("")))
					{
						String casedCategory=category.substring(0,1).toUpperCase() + 
								(category.length()>1?category.substring(1).
								toLowerCase():"");
						Log.i("Facebook","Cased category: " + casedCategory); 
						categories.add(casedCategory);
					}
				}
			}
		} //end setCategories
	} //end ExtraCriteria
	
	public static interface SearchThresholdTester
	{
		//The limit includes additional filtering
		public int searchPreviousRecords(int limit,Long threshold);
		public int searchNextRecords(int limit,Long threshold);
	}
	
	public static interface SearchCompleteCallback
	{
		public void searchSucceeded(List<Map<String,Object>> result);
		public void searchFailed(Exception exception);
	}
	
	public class SearchFacebookServer extends Binder
	{
		public void searchFacebook(Map<String,String> searchQueryParamsMap,
				ExtraCriteria extraCriteria,SearchThresholdTester
				thresholdTester,SearchCompleteCallback searchCallback,
				Handler callbackHandler) 
		{
			if (searchQueryParamsMap==null)
				throw new NullPointerException("The map of parameters for the " + 
						"search query must be non-null!");
			if (searchCallback==null)
				throw new NullPointerException("The search callback must be non-null!");
			if ((!searchQueryParamsMap.containsKey("access_token"))&&
					(accessToken!=null))
				searchQueryParamsMap.put("access_token",accessToken);
			SearchFacebookService.SearchTask searchTask=new SearchTask(
					searchQueryParamsMap,extraCriteria,thresholdTester,
					searchCallback,callbackHandler);
			searchTasksExecutor.execute(searchTask);
		}
	}
	
	private class SearchTask implements Runnable
	{
		private Map<String,String> searchQueryMap;
		private ExtraCriteria extraCriteria;
		private SearchThresholdTester thresholdTester;
		//private String accessToken;
		private SearchCompleteCallback searchCallback;
		private Handler callbackHandler;
		
		public SearchTask(Map<String,String> searchQueryMap,ExtraCriteria 
				extraCriteria,SearchThresholdTester thresholdTester,
				SearchCompleteCallback searchCallback,Handler callbackHandler)
		{
			this.searchQueryMap=searchQueryMap;
			this.extraCriteria=extraCriteria;
			this.thresholdTester=thresholdTester;
			//this.accessToken=accessToken;
			this.searchCallback=searchCallback;
			this.callbackHandler=callbackHandler;
		}
		
		@Override public void run() 
		{ 
			List<Map<String,Object>> resultDataArray=null;
			Exception thrownException=null;
			try
			{ 
				resultDataArray=searchFacebook(searchQueryMap,extraCriteria,
						thresholdTester); 
			}
			catch (Exception exception) { thrownException=exception; } 
			if (callbackHandler==null) 
				publishSearchOutput(resultDataArray,thrownException);
			else
			{
				final List<Map<String,Object>> resultDataArrayFinal=resultDataArray;
				final Exception thrownExceptionFinal=thrownException;
				callbackHandler.post(new Runnable()
				{
					public void run() 
					{ 
						publishSearchOutput(resultDataArrayFinal,
								thrownExceptionFinal);
					}
				});
			}
		}
		
		private void publishSearchOutput(List<Map<String,Object>> resultDataArray,
				Exception thrownException)
		{
			if (resultDataArray!=null)
				searchCallback.searchSucceeded(resultDataArray);
			else searchCallback.searchFailed(thrownException);
		}
	}
	
	private class DefaultSearchExecutor extends Thread implements Executor
	{
		private BlockingQueue<SearchTask> tasksQueue=new LinkedBlockingQueue
				<SearchTask>();
		private volatile boolean stopped=false;
		
		@Override public void execute(Runnable command) 
		{
			//Not really needed because this class is private
			if (stopped) return;
			else if (!(command instanceof SearchTask))
				throw new ClassCastException("The command to execute must be " +
						"a search task!");
			execute((SearchTask)command);
		}
		
		public void execute(SearchTask searchTask)
		//if (!tasksHandler.isAlive()) tasksHandler.start();
		{ if (!stopped) tasksQueue.add(searchTask); }
		
		@Override public void run()
		{
			while (!stopped)
			{
				SearchTask searchTask=null;
				try { searchTask=tasksQueue.take(); }
				catch (InterruptedException interruptedEx) { }
				if ((!stopped)&&(searchTask!=null)) searchTask.run();
			}
		}
		
		public void stopRunning()
		{
			/*Assuming all calls to this function are done from a single 
			 *thread*/
			if (!stopped)
			{
				stopped=true;
				interrupt();
			}
		}
		
		//public boolean isStopped() { return stopped; }
	}
	
	@Override public void onCreate()
	{
		super.onCreate();
		searchTasksExecutor=new DefaultSearchExecutor();
		searchServer=new SearchFacebookServer();
		jsonParser=new JSONParser();
		searchTasksExecutor.start();
	}
	
	@Override public void onDestroy()
	{
		searchTasksExecutor.stopRunning();
		super.onDestroy();
	}
	
	@Override public IBinder onBind(Intent intent) { return searchServer; }
	
	private List<Map<String,Object>> searchFacebook(Map<String,String> 
			searchQueryMap,ExtraCriteria extraCriteria,SearchThresholdTester 
			thresholdTester) throws Exception
	{
		List<Map<String,Object>> resultDataList=new ArrayList<Map<String,Object>>();
		String queryPath=searchQueryMap.remove("path");
		if (queryPath==null) queryPath="search";
		StringBuilder queryStringBuilder=new StringBuilder();
		queryStringBuilder.append("?");
		for (String queryParam : searchQueryMap.keySet())
		{
			queryStringBuilder.append(queryParam);
			queryStringBuilder.append("=");
			queryStringBuilder.append(searchQueryMap.get(queryParam));
			queryStringBuilder.append("&");
		}
		/*If there are no search parameters, the "?" will be deleted and the 
		 *address will have no query string, which is still legal*/
		queryStringBuilder.deleteCharAt(queryStringBuilder.length()-1);
		String queryAddress="https://graph.facebook.com/" + queryPath + 
				queryStringBuilder.toString();
		Log.i("Facebook",queryAddress);
		String origPrevAddress=null,origNextAddress=null;
		StringBuilder continueAddress=new StringBuilder();
		StringBuilder prevAddress=null,nextAddress=null;
		boolean isSearchingNext=false;
		do
		{
			HttpsURLConnection httpsConnection=null;
			BufferedReader responseStream=null;
			List<Map<String,Object>> resultDataForPage;
			try
			{
				URL url=new URL(queryAddress);
				URLConnection connection=url.openConnection();
				if (!(connection instanceof HttpsURLConnection))
					throw new IOException("Could not open connection to Facebook " +
							"Graph API! Invalid HTTPS connection!");
				httpsConnection=(HttpsURLConnection)connection;
				Log.i("Facebook","Address: " + queryAddress);
				int responseCode=httpsConnection.getResponseCode();
				//Log.i("Facebook",httpsConnection.getResponseMessage());
				if (responseCode==-1) 
				{
					throw new IOException("Invalid HTTP response! Access token " + 
							"might be missing / invalid?");
				}
				else if (responseCode!=HttpURLConnection.HTTP_OK)
				{
					throw new IOException("HTTP response received from the server " +
							"has a status code different from 200 (OK). Valid " +
							"response could not be retrieved.\n (Response code: " + 
							responseCode + ", Response message: " + httpsConnection.
							getResponseMessage() + "\nCheck if your access " + 
							"token exists and is still valid.");
				}
				/*responseStream=new FacebookResponseLineReader(new InputStreamReader(
						httpsConnection.getInputStream()));*/
				if (origPrevAddress==null)
				{
					prevAddress=continueAddress;
					nextAddress=new StringBuilder();
				}
				/*else if (isSearchingNext) nextAddress=continueAddress;
				else prevAddress=continueAddress;*/
				resultDataForPage=analyzeFacebookResponse(searchQueryMap,
						httpsConnection.getInputStream(),extraCriteria,
						thresholdTester,prevAddress,nextAddress);
			}
			finally
			{
				if (responseStream!=null)
				{
					try { responseStream.close(); }
					catch (IOException ioException) { }
				}
				if (httpsConnection!=null) httpsConnection.disconnect();
			}
			resultDataList.addAll(resultDataForPage);
			if (origPrevAddress==null)
			{
				origPrevAddress=prevAddress.toString();
				origNextAddress=nextAddress.toString();
			}
			Log.i("Facebook","Next address: " + nextAddress.toString());
			Log.i("Facebook","Prev address: " + prevAddress.toString());
			if ((continueAddress.length()==0)&&(!isSearchingNext))
			{
				isSearchingNext=true;
				prevAddress=nextAddress; //Temporary buffer
				nextAddress=continueAddress;
				queryAddress=origNextAddress;
			}
			else queryAddress=continueAddress.toString();
			queryAddress=queryAddress.replace("\\/","/");
		} while (queryAddress.length()>0);
		return resultDataList;
	}
	
	private List<Map<String,Object>> analyzeFacebookResponse(Map<String,String> 
			searchQueryMap,InputStream responseStream,ExtraCriteria 
			extraCriteria,SearchThresholdTester thresholdTester,StringBuilder 
			prevAddress,StringBuilder nextAddress) throws Exception
	{
		/*Object responseObject=new ObjectMapper().readValue(responseStream,
				Object.class);*/
		Object responseObject=jsonParser.parse(new InputStreamReader(
				responseStream));
		if ((responseObject instanceof Boolean)&&(!(Boolean)responseObject)) 
			throw new FacebookException("false");
		Map<String,Object> responseObjectMap=(Map<String,Object>)responseObject;
		Map<String,Object> errorObject=(Map<String,Object>)responseObjectMap.
				get("error");
		if (errorObject!=null) 
			throw new FacebookException(errorObject.toString());
		List<Map<String,Object>> responseDataArray=(List<Map<String,Object>>)
				responseObjectMap.get("data");
		if (responseDataArray==null)
		{
			responseDataArray=new ArrayList<Map<String,Object>>();
			responseDataArray.add(responseObjectMap);
			/*In the future may not return immediately if additional processing 
			 *is required, such as filtering*/
			return responseDataArray;
		}
		Map<String,Object> pagingObjectMap=(Map<String,Object>)responseObjectMap.
				get("paging");
		
		//Apply additional filtering by the extra criteria
		boolean filtered=false;
		String queriedType=searchQueryMap.get("type");
		if ((queriedType!=null)&&(queriedType.equals("page")))
		{
			Set<String> pageCategories=extraCriteria.categories;
			if ((pageCategories!=null)&&(!pageCategories.isEmpty())&&
					(responseDataArray.get(0).containsKey("category")))
			{
				for (Map<String,Object> pageObjectMap:responseDataArray)
				{
					String category=pageObjectMap.get("category").toString();
					Log.i("Facebook","Category: " + category);
					if (!pageCategories.contains(category))
					{
						//Log.i("Facebook","Failed!");
						pageObjectMap.put(FAILED_FILTER_KEY_NAME,null);
						filtered=true;
					}
				}
			}
		}
		List<Map<String,Object>> filteredDataArray;
		if (filtered)
		{
			filteredDataArray=new ArrayList<Map<String,Object>>();
			for (Map<String,Object> objectMap:responseDataArray)
			{
				if (!objectMap.containsKey(FAILED_FILTER_KEY_NAME))
					filteredDataArray.add(objectMap);
			}
		}
		else filteredDataArray=responseDataArray;
		/*Log.i("Facebook","Original size: " + responseDataArray.size());
		Log.i("Facebook","Filtered size: " + filteredDataArray.size());*/
		
		//Handle paging
		nextAddress.setLength(0);
		prevAddress.setLength(0);
		if (pagingObjectMap!=null)
		{
			int limit;
			Object resultAddressObject=pagingObjectMap.get("next");
			if (resultAddressObject!=null)
			{
				String resultAddress=resultAddressObject.toString();
				Log.i("Facebook","Next result: " + resultAddress);
				long threshold=extractThreshold(resultAddress,"offset");
				if (threshold==-1) 
					threshold=extractThreshold(resultAddress,"until");
				//For cursor-based pagination, there's no numeric threshold
				Long thresholdWrapper=(threshold>-1?threshold:null);
				limit=thresholdTester.searchNextRecords(filteredDataArray.
						size(),thresholdWrapper);
				if (limit>0)
				{
					nextAddress.append(limitPattern.matcher(resultAddress).
							replaceFirst("&limit=" + limit));
				}
				Log.i("Facebook","Next: " + nextAddress);
			}
			resultAddressObject=pagingObjectMap.get("previous");
			if (resultAddressObject!=null)
			{
				String resultAddress=resultAddressObject.toString();
				Log.i("Facebook","Prev result: " + resultAddress);
				long threshold=extractThreshold(resultAddress,"offset");
				if (threshold==-1) 
					threshold=extractThreshold(resultAddress,"since");
				Long thresholdWrapper=(threshold>-1?threshold:null);
				limit=thresholdTester.searchPreviousRecords(filteredDataArray.
						size(),thresholdWrapper);
				if (limit>0)
				{
					prevAddress.append(limitPattern.matcher(resultAddress).
							replaceFirst("&limit=" + limit));
				}
				Log.i("Facebook","Prev: " + prevAddress);
			}
		} //end if pagingObjectMap!=null
		return filteredDataArray;
	}
	
	private static long extractThreshold(String address,String paramName)
	{
		String paramString="&" + paramName + "=";
		int paramIndex=address.indexOf(paramString);
		if (paramIndex>-1)
		{
			int startIndex=paramIndex+paramString.length();
			int endIndex=address.indexOf('&',startIndex);
			if (endIndex==-1) endIndex=address.length();
			return Long.valueOf(address.substring(startIndex,endIndex));
		}
		else return -1;
	}
}
