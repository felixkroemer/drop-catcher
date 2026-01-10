package com.felixkroemer;

import com.felixkroemer.dagger.AppComponent;
import com.felixkroemer.watch.Watch;

public class DropCatcher {

  private static final AppComponent appComponent = DaggerAppComponent.create();

  public static void main(String[] args) {
    Watch watch = appComponent.getWatch();
    watch.watch();
  }
}
