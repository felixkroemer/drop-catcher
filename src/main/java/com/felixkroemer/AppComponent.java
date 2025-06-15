package com.felixkroemer;

import com.felixkroemer.watch.Watch;
import dagger.Component;

@Component
public interface AppComponent {
    Watch getWatch();
}
