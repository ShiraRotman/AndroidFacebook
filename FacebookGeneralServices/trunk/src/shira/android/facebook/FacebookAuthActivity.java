package shira.android.facebook;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.*;

public class FacebookAuthActivity extends Activity 
{
	public static final String FACEBOOK_AUTH_ACTION="shira.android.facebook.AUTH";
	public static final String PERMISSIONS_EXTRA_NAME="shira.android.facebook." +
			"PERMISSIONS";
	public static final String ACCESS_TOKEN_EXTRA_NAME="shira.android.facebook." +
			"ACCESS_TOKEN";
	public static final String ERROR_MESSAGE_EXTRA_NAME="shira.android.facebook." +
			"ERROR_MESSAGE";
	/*public static final String EXCEPTION_EXTRA_NAME="shira.android.facebook." +
			"EXCEPTION";*/
	
	private static final String AUTHORIZATION_URI="https://graph.facebook.com/" + 
			"oauth/authorize";
	private static final String REDIRECT_URI="https://www.facebook.com/connect/" + 
			"login_success.html";
	
	private WebView webView;
	private String facebookAppClientID;
	
	private class AuthWebViewClient extends WebViewClient
	{
		@Override 
		public boolean shouldOverrideUrlLoading(WebView webView,String urlAddress)
		{ return false; }
		
		@Override 
		public void onPageStarted(WebView webView,String urlAddress,Bitmap favicon)
		{
			if ((urlAddress!=null)&&(urlAddress.startsWith(REDIRECT_URI)))
			{
				webView.stopLoading();
				//Log.i("Facebook","URL: " + urlAddress);
				URL redirectionURL=null;
				try { redirectionURL=new URL(urlAddress); }
				//Not supposed to occur
				catch (MalformedURLException urlException) { }
				int resultCode; Intent resultIntent=new Intent();
				String accessToken=null;
				String reference=redirectionURL.getRef();
				//Log.i("Facebook","Reference: " + reference);
				if (reference!=null)
				{
					int tokenStartIndex="access_token=".length();
					/*Log.i("Facebook","Start: " + reference.substring(
							tokenStartIndex));*/ 
					if (tokenStartIndex<reference.length())
					{
						int tokenEndIndex=reference.indexOf("&",tokenStartIndex);
						if (tokenEndIndex==-1) tokenEndIndex=reference.length();
						accessToken=reference.substring(tokenStartIndex,
								tokenEndIndex);
						Log.i("Facebook","Access Token: " + accessToken);
					}
				}
				if (accessToken==null)
				{
					resultCode=RESULT_CANCELED;
					resultIntent.putExtra(ERROR_MESSAGE_EXTRA_NAME,"Could not " +
							"retrieve access token!");
				}
				else
				{
					resultCode=RESULT_OK;
					resultIntent.putExtra(ACCESS_TOKEN_EXTRA_NAME,accessToken);
				}
				setResult(resultCode,resultIntent);
				finish();
			} //end if urlAddress...
			super.onPageStarted(webView,urlAddress,favicon);
		} //end function
	} //end class
	
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		webView=new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new AuthWebViewClient());
		webView.setWebChromeClient(new WebChromeClient()
		{
			@Override 
			public void onProgressChanged(WebView webView,int newProgress)
			{ setProgress(newProgress*100); }
		});
		facebookAppClientID=getResources().getString(R.string.facebook_app_client_id);
		StringBuilder authAddressBuilder=new StringBuilder();
		authAddressBuilder.append(AUTHORIZATION_URI);
		authAddressBuilder.append("?client_id=");
		authAddressBuilder.append(facebookAppClientID);
		authAddressBuilder.append("&redirect_uri=");
		authAddressBuilder.append(REDIRECT_URI);
		String[] permissions=getIntent().getStringArrayExtra(PERMISSIONS_EXTRA_NAME);
		if (permissions!=null)
		{
			boolean firstPermission=true;
			for (String permission:permissions)
			{
				if ((permission!=null)&&(!permission.equals("")))
				{
					if (firstPermission)
					{
						authAddressBuilder.append("&scope=");
						firstPermission=false;
					}
					authAddressBuilder.append(permission);
					authAddressBuilder.append(",");
				}
			}
			if (!firstPermission) 
				authAddressBuilder.deleteCharAt(authAddressBuilder.length()-1);
		}
		authAddressBuilder.append("&display=touch&response_type=token");
		webView.loadUrl(authAddressBuilder.toString());
		setContentView(webView);
	}
}
