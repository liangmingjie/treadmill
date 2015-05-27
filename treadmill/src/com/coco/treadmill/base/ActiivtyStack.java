package com.coco.treadmill.base;

import java.util.Stack;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class ActiivtyStack {
	private Stack<Fragment> mActivityStack;
	private FragmentManager fm;

	public ActiivtyStack(FragmentManager fm) {
		this.fm = fm;
		mActivityStack = new Stack<Fragment>();
	}

	// 退出栈顶Activity
	public void popActivity(Fragment activity) {
		if (activity != null) {
			mActivityStack.remove(activity);
			fm.popBackStack();
			// mActivityStack.pop();
			activity = null;
		}
	}

	// 获得当前栈顶Activity
	public Fragment currentActivity() {
		Fragment activity = mActivityStack.lastElement();
		// Activity activity = mActivityStack.pop();
		return activity;
	}

	// 将当前Activity推入栈中
	public void pushActivity(Fragment activity) {
		mActivityStack.add(activity);
		// mActivityStack.push(activity);
	}

	// 退出栈中所有Activity
	public void popAllActivityExceptOne(Class<Fragment> cls) {
		while (true) {
			Fragment activity = currentActivity();
			if (activity == null) {
				break;
			}
			if (activity.getClass().equals(cls)) {
				break;
			}
			popActivity(activity);
		}
	}

	public int getFragmentSize() {
		return mActivityStack.size();
	}
}
