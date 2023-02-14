package org.exoplatform.portal.mop.storage.listener;

import org.exoplatform.portal.jdbc.entity.NodeEntity;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class NodeDescriptionListener extends Listener<NodeEntity, Object> {

  private DescriptionStorage service;
  
  public NodeDescriptionListener(DescriptionStorage service) {
    this.service = service;
  }

  @Override
  public void onEvent(Event<NodeEntity, Object> event) throws Exception {
    service.setDescription(event.getData() == null ? null : event.getData().toString(), null);
  }

}
