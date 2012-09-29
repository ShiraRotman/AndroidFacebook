package shira.android.facebook.updaterfollower.search;

public class FacebookException extends Exception 
{
	private static final long serialVersionUID=2467939414003745382L;
	
	public FacebookException() { }
	public FacebookException(String message) { super(message); }
	/*In the future more fields may be added that represent the data returned 
	 *from queries in case of an error (such as code and type)*/
}
