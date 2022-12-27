package org.exoplatform.services.organization.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.commons.utils.ListAccess;

public class InMemoryListAccess<T> implements ListAccess<T> {

  private Class<T> modelClass;

  private List<T>  values;

  private T[]      defaultResult;

  @SuppressWarnings("unchecked")
  public InMemoryListAccess(List<T> values, T[] defaultResult) {
    this.defaultResult = defaultResult;
    List<T> retrievedValues = values == null ? Collections.emptyList()
                                             : values.stream().filter(Objects::nonNull).toList();
    if (CollectionUtils.isNotEmpty(values)) {
      T firstElement = retrievedValues.get(0);
      if (firstElement instanceof Cloneable) {
        this.values = values.stream().map(ObjectUtils::clone).filter(Objects::nonNull).toList();
      } else {
        this.values = new ArrayList<>(values);
      }
      this.modelClass = (Class<T>) firstElement.getClass();
    }
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
