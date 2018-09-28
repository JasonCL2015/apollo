/**  
 * All rights Reserved, Designed By www.maihaoche.com
 * 
 * @Package com.ctrip.framework.apollo.spring.boot
 * @author: 三帝（sandi@maihaoche.com）
 * @date:   2018年9月5日 下午10:01:53
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved. 
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
package com.ctrip.framework.apollo.spring.boot;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ResourceUtils;

import java.util.List;

/**   
 * <p> TODO(这里用一句话描述这个类的作用) </p>
 *   
 * @author: 三帝（sandi@maihaoche.com） 
 * @date: 2018年9月5日 下午10:01:53   
 * @since V1.0 
 */
public class MhcEnvironmentPostProcessor implements EnvironmentPostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(MhcEnvironmentPostProcessor.class);
	  private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
	  private static final String[] APOLLO_SYSTEM_PROPERTIES = {"app.id", ConfigConsts.APOLLO_CLUSTER_KEY,
	      "apollo.cacheDir", ConfigConsts.APOLLO_META_KEY};

	  private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector
	      .getInstance(ConfigPropertySourceFactory.class);
	  
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
			logger.info("configurableEnvironment :" + environment.getPropertySources());
			logger.info("######### initialize context on MhcEnvironmentPostProcessor");
			initializeSystemProperty(environment);

		    String enabled = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "false");
		    if (!Boolean.valueOf(enabled)) {
//		      logger.debug("Apollo bootstrap config is not enabled for context {}, see property: ${{}}", context, PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
		      return;
		    }
//		    logger.debug("Apollo bootstrap config is enabled for context {}", context);

		    if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
		      //already initialized
		      return;
		    }

		    String namespaces = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, ConfigConsts.NAMESPACE_APPLICATION);
		    logger.debug("Apollo bootstrap namespaces: {}", namespaces);
		    List<String> namespaceList = NAMESPACE_SPLITTER.splitToList(namespaces);

		    CompositePropertySource composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
		    for (String namespace : namespaceList) {
		      Config config = ConfigService.getConfig(namespace);

		      composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
		    }

		    environment.getPropertySources().addFirst(composite);
		    reinitializeLoggingSystem(environment);
	}
	
	/**
	   * Learning from {@see org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration} which is in spring-cloud.
	   * @param environment
	   */
	  private void reinitializeLoggingSystem(ConfigurableEnvironment environment) {
	    String logConfig = environment.resolvePlaceholders("${logging.config:}");
	    LogFile logFile = LogFile.get(environment);
	    LoggingSystem system = LoggingSystem.get(LoggingSystem.class.getClassLoader());
	    try {
	      ResourceUtils.getURL(logConfig).openStream().close();
	      // Three step initialization that accounts for the clean up of the logging
	      // context before initialization. Spring Boot doesn't initialize a logging
	      // system that hasn't had this sequence applied (since 1.4.1).
	      system.cleanUp();
	      system.beforeInitialize();
	      system.initialize(new LoggingInitializationContext(environment), logConfig, logFile);
	    } catch (Exception ex) {
	      logger.warn("Logging config file location '" + logConfig
	              + "' cannot be opened and will be ignored");
	    }
	  }

	  /**
	   * To fill system properties from environment config
	   */
	  void initializeSystemProperty(ConfigurableEnvironment environment) {
	    for (String propertyName : APOLLO_SYSTEM_PROPERTIES) {
	      fillSystemPropertyFromEnvironment(environment, propertyName);
	    }
	  }

	  private void fillSystemPropertyFromEnvironment(ConfigurableEnvironment environment, String propertyName) {
	    if (System.getProperty(propertyName) != null) {
	      return;
	    }

	    String propertyValue = environment.getProperty(propertyName);

	    if (Strings.isNullOrEmpty(propertyValue)) {
	      return;
	    }

	    System.setProperty(propertyName, propertyValue);
	  }

}
