/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.tasks.config;

import com.intellij.openapi.util.Comparing;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.impl.TaskManagerImpl;
import com.intellij.util.containers.ContainerUtil;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@State(
  name = "RecentTaskRepositories",
  storages = {
    @Storage(
      file = StoragePathMacros.APP_CONFIG + "/other.xml"
    )}
)
public class RecentTaskRepositories implements PersistentStateComponent<Element> {

  private final Set<TaskRepository> myRepositories = Sets.newHashSet(HASHING_STRATEGY);

  private static final HashingStrategy<TaskRepository> HASHING_STRATEGY = new HashingStrategy<TaskRepository>() {
    public int hashCode(TaskRepository object) {
      return object.getUrl() == null ? 0 : object.getUrl().hashCode();
    }

    public boolean equals(TaskRepository o1, TaskRepository o2) {
      return Comparing.equal(o1.getUrl(), o2.getUrl());
    }
  };

  public static RecentTaskRepositories getInstance() {
    return ServiceManager.getService(RecentTaskRepositories.class);
  }

  public Set<TaskRepository> getRepositories() {
    return Sets.newHashSet(ContainerUtil.findAll(myRepositories, (Condition<TaskRepository>) repository -> !StringUtil.isEmptyOrSpaces(repository.getUrl())), HASHING_STRATEGY);
  }

  public void addRepositories(Collection<TaskRepository> repositories) {
    Collection<TaskRepository> old = new ArrayList<TaskRepository>(myRepositories);
    myRepositories.clear();
    if (doAddReps(repositories)) return;
    doAddReps(old);
  }

  private boolean doAddReps(Collection<TaskRepository> repositories) {
    for (TaskRepository repository : repositories) {
      if (!StringUtil.isEmptyOrSpaces(repository.getUrl())) {
        if (myRepositories.size() == 10) {
          return true;
        }
        myRepositories.add(repository);
      }
    }
    return false;
  }

  public Element getState() {
    return XmlSerializer.serialize(myRepositories.toArray(new TaskRepository[myRepositories.size()]));
  }

  public void loadState(Element state) {
    myRepositories.clear();
    myRepositories.addAll(TaskManagerImpl.loadRepositories(state));
  }
}
