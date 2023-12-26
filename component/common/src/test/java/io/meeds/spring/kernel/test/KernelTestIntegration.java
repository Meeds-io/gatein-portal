package io.meeds.spring.kernel.test;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.spi.ComponentAdapter;

import io.meeds.spring.kernel.annotation.Exclude;

/**
 * A class to let Spring context initialized once the
 * {@link PortalContainer}finishes startup to bring all defined Kernel Services
 * into Spring Context in order to be able to use annotations to inject Kernel
 * services
 */
public class KernelTestIntegration {

  private static final Logger LOG = LoggerFactory.getLogger(KernelTestIntegration.class);

  private KernelTestIntegration() {
  }

  public static void registerKernelComponentsAsSpringBeans(PortalContainer portalContainer,
                                                           BeanDefinitionRegistry beanRegistry) {
    LOG.info("Injecting Kernel components into Spring context as beans");
    injectKernelComponentsAsBeans(beanRegistry, portalContainer);
  }

  public static void registerSpringBeanAsKernelComponent(PortalContainer container,
                                                         BeanDefinitionRegistry beanRegistry,
                                                         String beanName,
                                                         Object bean) {
    registerSpringBeansAsKernelComponents(container, beanRegistry, new String[] { beanName }, name -> bean);
  }

  private static void registerSpringBeansAsKernelComponents(PortalContainer portalContainer,
                                                            BeanDefinitionRegistry beanRegistry,
                                                            String[] beanDefinitionNames,
                                                            Function<String, Object> getBeanFunction) {
    Stream.of(beanDefinitionNames)
          .filter(beanName -> portalContainer.getComponentAdapter(beanName) == null)
          .forEach(beanName -> {
            BeanDefinition beanDefinition = getBeanDefinition(beanRegistry, beanName);
            if (beanDefinition != null) {
              String beanClassName = beanDefinition.getBeanClassName();
              Object bean = getBeanFunction.apply(beanName);
              Class<?> beanClass = getBeanClass(portalContainer, beanClassName, bean);
              if (beanClass != null && portalContainer.getComponentInstanceOfType(beanClass) == null) {
                portalContainer.registerComponentInstance(beanClass, bean);
              }
            }
          });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void injectKernelComponentsAsBeans(BeanDefinitionRegistry beanRegistry, PortalContainer portalContainer) {
    List<Class> beanClasses = portalContainer.getComponentAdapters()
                                             .stream()
                                             .map(adapter -> beanNameToClass(adapter, portalContainer))
                                             .filter(Objects::nonNull)
                                             .distinct()
                                             .toList();

    Set<String> kernelBeanNames = new HashSet<>();
    beanClasses.stream()
               // Avoid having two instances inheriting from the same API
               // interface to not get NoUniqueBeanDefinitionException
               .filter(c -> {
                 Class ec = beanClasses.stream()
                                       .filter(oc -> !oc.equals(c) && oc.isAssignableFrom(c))
                                       .findFirst()
                                       .orElse(null);
                 if (ec != null || kernelBeanNames.contains(c.getName())) {
                   return false;
                 } else {
                   kernelBeanNames.add(c.getName());
                   return true;
                 }
               })
               .forEach(beanClassName -> registerBean(portalContainer, beanRegistry, beanClassName));
  }

  private static Class<?> registerBean(PortalContainer portalContainer, BeanDefinitionRegistry beanRegistry, Class<?> beanClass) {
    RootBeanDefinition beanDefinition = createBeanDefinition(beanClass, portalContainer);
    beanRegistry.registerBeanDefinition(beanClass.getName(), beanDefinition);
    return beanClass;
  }

  private static <T> RootBeanDefinition createBeanDefinition(Class<T> keyClass, PortalContainer portalContainer) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(keyClass,
                                                               () -> portalContainer.getComponentInstanceOfType(keyClass));
    beanDefinition.setLazyInit(true);
    beanDefinition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
    return beanDefinition;
  }

  private static Class<?> getBeanClass(PortalContainer portalContainer,
                                       String beanClassName,
                                       Object bean) {
    try {
      if (StringUtils.isBlank(beanClassName) || bean == null) {
        return null;
      }
      Class<?> beanClass = portalContainer.getPortalClassLoader().loadClass(beanClassName);
      if (bean.getClass().isAssignableFrom(beanClass)
          && (isBeanClassValid(beanClass) || isBeanClassValid(bean.getClass()))
          && !isConfigurationBean(beanClass)
          && !isConfigurationBean(bean.getClass())
          && (isServiceBean(beanClass) || isServiceBean(bean.getClass()))
          && (!isServiceBeanExcluded(beanClass) || !isServiceBeanExcluded(bean.getClass()))) {
        if (isServiceBean(bean.getClass())) {
          return bean.getClass();
        } else {
          return beanClass;
        }
      } else {
        return null;
      }
    } catch (ClassNotFoundException e) {
      LOG.warn("Ignore registering Spring Bean '{}' as Kernel service since it's class is not found in shared ClassLoader",
               beanClassName);
      return null;
    }
  }

  private static BeanDefinition getBeanDefinition(BeanDefinitionRegistry beanRegistry, String beanName) {
    return beanRegistry.containsBeanDefinition(beanName) ? beanRegistry.getBeanDefinition(beanName) : null;
  }

  private static boolean isConfigurationBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Configuration.class) || beanClass.isAnnotationPresent(SpringBootApplication.class);
  }

  private static boolean isBeanClassValid(Class<?> beanClass) {
    return !StringUtils.contains(beanClass.getName(), "org.springframework")
           && !StringUtils.startsWith(beanClass.getName(), "java")
           && !StringUtils.startsWith(beanClass.getName(), "jdk")
           && !StringUtils.contains(beanClass.getName(), "$");
  }

  private static boolean isServiceBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Service.class);
  }

  private static boolean isServiceBeanExcluded(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Exclude.class);
  }

  @SuppressWarnings("rawtypes")
  private static Class beanNameToClass(ComponentAdapter adapter, PortalContainer portalContainer) {
    Object key = adapter.getComponentKey();
    if (key instanceof Class<?> keyClass) {
      return keyClass;
    } else {
      if (key instanceof String keyString) {
        try {
          return portalContainer.getPortalClassLoader().loadClass(keyString);
        } catch (ClassNotFoundException e) {
          return adapter.getComponentImplementation();
        }
      } else {
        return key.getClass();
      }
    }
  }

}
