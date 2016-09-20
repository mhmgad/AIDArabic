package mpi.aida.graph.similarity.context;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;

import mpi.aida.data.ExternalEntitiesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntitiesContext {
  private static final Logger logger = 
      LoggerFactory.getLogger(EntitiesContext.class);
  
  protected Entities entities;
  protected ExternalEntitiesContext externalContext;
  protected EntitiesContextSettings settings;

  public EntitiesContext(Entities entities, EntitiesContextSettings settings) throws Exception {
    this(entities, new ExternalEntitiesContext(), settings);
  }

  public EntitiesContext(Entities entities, ExternalEntitiesContext externalContext,
                         EntitiesContextSettings settings) throws Exception {
    this.entities = entities;
    this.externalContext = externalContext;
    this.settings = settings;

    long beginTime = System.currentTimeMillis();

    setupEntities(entities);

    long runTime = (System.currentTimeMillis() - beginTime) / 1000;
    logger.debug("Done setting up " + this + ": " + runTime + "s");
  }

  public void setEntities(Entities entities) throws Exception {
    this.entities = entities;
    setupEntities(entities);
  }

  public Entities getEntities() {
    return entities;
  }

  public abstract int[] getContext(Entity entity);

  protected abstract void setupEntities(Entities entities) throws Exception;
  
  public String toString() {
    return getIdentifier();
  }
  
  public String getIdentifier() {
    return this.getClass().getSimpleName();
  }
}
