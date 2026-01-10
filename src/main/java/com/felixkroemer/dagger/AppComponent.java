package com.felixkroemer.dagger;

import com.felixkroemer.watch.Watch;
import dagger.Component;

@Component(modules = {DaggerModule.class})
public interface AppComponent {
  Watch getWatch();
}
