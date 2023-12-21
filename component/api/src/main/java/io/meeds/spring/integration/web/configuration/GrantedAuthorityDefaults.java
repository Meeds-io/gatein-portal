package io.meeds.spring.integration.web.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.security.access.annotation.Jsr250MethodSecurityMetadataSource;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@SuppressWarnings("deprecation")
public class GrantedAuthorityDefaults implements BeanPostProcessor, PriorityOrdered {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // remove this if you are not using JSR-250
    if (bean instanceof Jsr250MethodSecurityMetadataSource securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof DefaultMethodSecurityExpressionHandler securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof DefaultWebSecurityExpressionHandler securityBean) {
      securityBean.setDefaultRolePrefix(null);
    }
    if (bean instanceof SecurityContextHolderAwareRequestFilter securityBean) {
      securityBean.setRolePrefix("");
    }
    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

}
