// Generated code from Butter Knife. Do not modify!
package com.coco.treadmill.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;

public class MainActivity$$ViewInjector {
  public static void inject(Finder finder, final com.coco.treadmill.ui.MainActivity target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131230722, "field 'customicon' and method 'onClickCustomicon'");
    target.customicon = (android.widget.Button) view;
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onClickCustomicon((android.widget.Button) p0);
        }
      });
    view = finder.findRequiredView(source, 2131230723, "method 'onClickmMoni'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onClickmMoni((android.widget.Button) p0);
        }
      });
  }

  public static void reset(com.coco.treadmill.ui.MainActivity target) {
    target.customicon = null;
  }
}
