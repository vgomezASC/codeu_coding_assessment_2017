// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codeu.codingchallenge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MyJSON implements JSON {

  // This could, of course, hold any sort of non-JSON "primitive," but
  // for the sake of this exercise, only strings will be allowed.
  private Map<String, String> strings = new HashMap<String, String>();

  private Map<String, JSON> objects = new HashMap<String, JSON>();

  @Override
  public JSON getObject(String name) {
    return objects.get(name);
  }

  @Override
  public JSON setObject(String name, JSON value) {
    objects.put(name, value);
    if (strings.containsKey(name))
      strings.remove(name);
    return this;
  }

  @Override
  public String getString(String name) {
    return strings.get(name);
  }

  @Override
  public JSON setString(String name, String value) {
    strings.put(name, value);
    if (objects.containsKey(name))
      objects.remove(name);
    return this;
  }

  @Override
  public void getObjects(Collection<String> names) {
    Set<String> objectKeys = objects.keySet();
    for (String key : objectKeys)
      names.add(key);
  }

  @Override
  public void getStrings(Collection<String> names) {
    Set<String> stringKeys = strings.keySet();
    for (String key : stringKeys)
      names.add(key);
  }
  
  /**
   * Recursively counts the number of keys inside a JSON object.
   * @param object Any JSON object
   * @return The total number of keys inside the JSON object
   */
  public static int count(JSON object) {
    int count = 0;

    Collection<String> names = new ArrayList<String>();
    object.getObjects(names);
    Debug.println(names.toString());
    for(String name : names) {
      count++;
      count += count(object.getObject(name));
    }

    names = new ArrayList<String>();
    object.getStrings(names);
    Debug.println(names.toString());
    count += names.size();

    return count;
  }
}
