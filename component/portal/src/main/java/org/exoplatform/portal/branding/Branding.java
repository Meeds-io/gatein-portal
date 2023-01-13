package org.exoplatform.portal.branding;

import java.io.Serializable;
import java.util.*;

import lombok.Data;

@Data
public class Branding implements Serializable {

  private static final long   serialVersionUID = 625471892955717717L;

  private String              companyName;

  private String              siteName;

  private String              companyLink;

  private String              topBarTheme;

  private Logo                logo;

  private Favicon             favicon;

  private Map<String, String> themeColors      = new HashMap<>();

  private long                lastUpdatedTime;

}
