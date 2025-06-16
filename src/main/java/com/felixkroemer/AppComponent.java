package com.felixkroemer;

import com.felixkroemer.dagger.DaggerModule;
import com.felixkroemer.watch.Watch;
import dagger.Component;

@Component(modules = {DaggerModule.class})
public interface AppComponent {
    Watch getWatch();
}
