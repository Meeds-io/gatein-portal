package org.exoplatform.portal.branding;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favicon implements Serializable {

  private static final long serialVersionUID = 2846145296185073958L;

  private String            uploadId;

  private long              size;

  private byte[]            data;

  private long              updatedDate;

}
