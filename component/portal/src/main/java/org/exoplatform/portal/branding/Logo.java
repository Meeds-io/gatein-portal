package org.exoplatform.portal.branding;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Logo implements Serializable {

  private static final long serialVersionUID = 5444110675143558828L;

  private String uploadId;

  private long   size;

  private byte[] data;

  private long updatedDate;

}
