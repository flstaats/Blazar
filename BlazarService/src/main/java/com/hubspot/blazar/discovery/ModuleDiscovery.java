package com.hubspot.blazar.discovery;

import com.hubspot.blazar.base.GitInfo;
import com.hubspot.blazar.base.Module;
import com.hubspot.blazar.github.GitHubProtos.PushEvent;

import java.io.IOException;
import java.util.Set;

public interface ModuleDiscovery {
  boolean allowDuplicates();
  boolean shouldRediscover(GitInfo gitInfo, PushEvent pushEvent) throws IOException;
  Set<Module> discover(GitInfo gitInfo) throws IOException;
}