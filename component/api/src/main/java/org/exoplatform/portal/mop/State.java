package org.exoplatform.portal.mop;

import java.io.Serializable;

import lombok.Data;

@Data
public class State implements Serializable {

  private static final long serialVersionUID = 2528981865909385112L;

  private final String      name;

  private final String      description;

  public State(String name, String description) {
    this.name = name;
    this.description = description;
  }
}
