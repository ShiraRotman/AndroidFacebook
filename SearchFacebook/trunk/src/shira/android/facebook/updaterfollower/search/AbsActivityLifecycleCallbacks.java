package shira.android.facebook.updaterfollower.search;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public abstract class AbsActivityLifecycleCallbacks implements Application.
		ActivityLifecycleCallbacks  
{
	protected AbsActivityLifecycleCallbacks() { }
	
	@Override public void onActivityDestroyed(Activity activity) { }
	@Override public void onActivityPaused(Activity activity) { }
	@Override public void onActivityResumed(Activity activity) { }
	@Override public void onActivityStarted(Activity activity) { }
	@Override public void onActivityStopped(Activity activity) { }
	
	@Override
	public void onActivityCreated(Activity activity,Bundle savedInstanceState) { }
	
	@Override
	public void onActivitySaveInstanceState(Activity activity,Bundle outState) { }
}
