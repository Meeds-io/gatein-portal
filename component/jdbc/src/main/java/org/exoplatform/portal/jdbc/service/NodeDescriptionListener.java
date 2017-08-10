package org.exoplatform.portal.jdbc.service;

import org.exoplatform.portal.jdbc.entity.NodeEntity;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class NodeDescriptionListener extends Listener<NodeEntity, String> {

  private DescriptionService service;
  
  public NodeDescriptionListener(DescriptionService service) {
    this.service = service;
  }

  @Override
  public void onEvent(Event<NodeEntity, String> event) throws Exception {
    service.setDescription(event.getData(), null);
  }

}
