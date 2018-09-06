package com.ctrip.framework.apollo.spring.boot;

import com.ctrip.framework.apollo.spring.config.ConfigPropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED)
@ConditionalOnMissingBean(PropertySourcesProcessor.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ApolloAutoConfiguration {

  @Bean
  public ConfigPropertySourcesProcessor configPropertySourcesProcessor() {
    return new ConfigPropertySourcesProcessor();
  }
}
