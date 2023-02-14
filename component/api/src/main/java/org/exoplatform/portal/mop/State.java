package org.exoplatform.portal.mop;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class State implements Serializable {

  public static final State NULL_OBJECT      = new State(null, null);

  private static final long serialVersionUID = 2528981865909385112L;

  private final String      name;

  private final String      description;

  public boolean isNull() {
    return name == null && description == null;
  }

}
