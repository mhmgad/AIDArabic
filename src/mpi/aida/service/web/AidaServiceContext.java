package mpi.aida.service.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import mpi.aida.cachepreloader.EntityKeyphrasePreloader;
import mpi.aida.config.AidaConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AidaServiceContext implements ServletContextListener {

  Logger logger_ = LoggerFactory.getLogger(AidaServiceContext.class);
  
  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
  }

  @Override
  public void contextInitialized(ServletContextEvent arg0) {
    if (AidaConfig.getBoolean(AidaConfig.PRELOAD_ENITTY_CONTEXTS)) {
      EntityKeyphrasePreloader.cacheAllEntities();    
    }
  }
}