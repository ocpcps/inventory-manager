package com.osstelecom.db.inventory.manager.operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ResourceLocationDao;
import com.osstelecom.db.inventory.manager.events.ResourceLocationCreatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

@Service
public class ResourceLocationManager extends Manager {

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ResourceLocationDao resourceLocationDao;

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    /**
     * Created a Resource Location
     *
     * @param resource
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public ResourceLocation createResourceLocation(ResourceLocation resource) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, ArangoDaoException {
        String timerId = startTimer("createResourceLocation");
        try {
            lockManager.lock();
            Boolean useUpsert = false;
            if (resource.getKey() == null) {
                resource.setKey(this.getUUID());
            } else {
                useUpsert = true;
            }
            resource.setAtomId(resource.getDomain().addAndGetId());

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(resource);
            dynamicRuleSession.evalResource(resource, "I", this);

            DocumentCreateEntity<ResourceLocation> result;
            if (useUpsert) {
                result = this.resourceLocationDao.upsertResource(resource);
            } else {
                result = this.resourceLocationDao.insertResource(resource);
            }

            resource.setKey(result.getKey());
            resource.setRevisionId(result.getRev());
            //
            // Aqui de Fato Criou o ResourceLocation
            //
            ResourceLocationCreatedEvent event = new ResourceLocationCreatedEvent(resource);
            this.eventManager.notifyResourceEvent(event);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public ResourceLocation findResourceLocation(String name, String nodeAdrress, String className, String domainName) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        String timerId = startTimer("findResourceLocation");
        try {
            lockManager.lock();
            Domain domain = domainManager.getDomain(domainName);
            return resourceLocationDao.findResourceLocation(name, nodeAdrress, className, domain);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Called when a Resource Location is created
     *
     * @param resourceLocation
     */
    @Subscribe
    public void onResourceLocationCreatedEvent(ResourceLocationCreatedEvent resourceLocation) {

    }

}
