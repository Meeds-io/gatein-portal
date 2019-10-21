package org.exoplatform.settings.chromattic;

import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.OneToMany;
import org.chromattic.api.annotations.PrimaryType;

import java.util.Map;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
@PrimaryType(name = "stg:simplecontext")
public abstract class SimpleContextEntity extends ContextEntity {

  @OneToMany
  public abstract Map<String, ScopeEntity> getScopes();

  @Create
  protected abstract ScopeEntity create();

  public ScopeEntity getScope(String scopeName) {
    if (getScopes().containsKey(scopeName)) {
      return getScopes().get(scopeName);
    } else {
      ScopeEntity scope = create();
      getScopes().put(scopeName, scope);
      return scope;
    }
  }

}
