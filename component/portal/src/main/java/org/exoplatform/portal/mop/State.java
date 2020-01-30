package org.exoplatform.portal.mop;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * The composite state of the {@code Described} mixin.
 */
public class State implements Serializable {

  private static final long serialVersionUID = 2528981865909385112L;

  /** . */
  private final String name;

  /** . */
  private final String description;

  public State(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof State))
      return false;

    State state = (State) o;

    return (StringUtils.equals(name, state.name) && StringUtils.equals(description, state.description));

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Description[name=" + name + ",description=" + description + "]";
  }
}
