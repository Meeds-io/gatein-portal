package org.exoplatform.services.organization.mock;

import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;

public class InMemoryListAccess<T> implements ListAccess<T> {

  private final List<T> values;

  public InMemoryListAccess(List<T> values) {
    this.values = values;
  }

  @SuppressWarnings("unchecked")
  public T[] load(int index, int length) {
    if (index >= values.size()) {
      return (T[]) Collections.emptyList().toArray();
    }
    if (index + length > values.size()) {
      length = values.size() - index;
    }
    return (T[]) values.subList(index, index + length).toArray();
  }

  public int getSize() {
    return values.size();
  }

}
