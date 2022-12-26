package org.exoplatform.services.organization.mock;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.commons.utils.ListAccess;

public class InMemoryListAccess<T> implements ListAccess<T> {

  private Class<T> modelClass;

  private List<T>  values;

  private T[]      defaultResult;

  @SuppressWarnings("unchecked")
  public InMemoryListAccess(List<T> values, T[] defaultResult) {
    this.values = values;
    this.defaultResult = defaultResult;
    this.modelClass = CollectionUtils.isEmpty(this.values) ? null
                                                           : (Class<T>) this.values.get(0).getClass();
  }

  @SuppressWarnings("unchecked")
  public T[] load(int index, int length) {
    if (modelClass == null || index >= values.size()) {
      return defaultResult;
    }
    if (index + length > values.size()) {
      length = values.size() - index;
    }
    return values.subList(index, index + length)
                 .toArray((T[]) java.lang.reflect.Array.newInstance(modelClass, values.size()));
  }

  public int getSize() {
    return values.size();
  }

}
