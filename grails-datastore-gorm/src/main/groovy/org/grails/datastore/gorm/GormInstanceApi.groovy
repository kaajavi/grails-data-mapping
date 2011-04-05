/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.validation.ValidationException
import org.springframework.datastore.mapping.proxy.EntityProxy
import static org.springframework.datastore.mapping.validation.ValidatingEventListener.*

/**
 * Instance methods of the GORM API.
 *
 * @author Graeme Rocher
 * @param <D> the entity/domain class
 */
class GormInstanceApi<D> extends AbstractGormApi<D> {

    Class<Exception> validationException = ValidationException

    GormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Proxy aware instanceOf implementation.
     */
    boolean instanceOf(o, Class cls) {
        if (o instanceof EntityProxy) {
            return cls.isInstance(o.getTarget())
        }
        return cls.isInstance(o)
    }

    /**
     * Upgrades an existing persistence instance to a write lock
     * @return The instance
     */
    D lock(D instance) {
        datastore.currentSession.lock(instance)
        return instance
    }

    /**
     * Locks the instance for updates for the scope of the passed closure
     *
     * @param callable The closure
     * @return The result of the closure
     */
    def mutex(D instance, Closure callable) {
        def session = datastore.currentSession
        try {
            session.lock(instance)
            callable?.call()
        }
        finally {
            session.unlock(instance)
        }
    }

    /**
     * Refreshes the state of the current instance
     * @param instance The instance
     * @return The instance
     */
    D refresh(D instance) {
        datastore.currentSession.refresh instance
        return instance
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    D save(D instance) {
        save(instance, Collections.emptyMap())
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    D merge(D instance) {
        save(instance, Collections.emptyMap())
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    D merge(D instance, Map params) {
        save(instance, params)
    }

    /**
     * Save method that takes a boolean which indicates whether to perform validation or not
     *
     * @param instance The instance
     * @param validate Whether to perform validation
     *
     * @return The instance or null if validation fails
     */
    D save(D instance, boolean validate) {
        save(instance, [validate: validate])
    }

    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    D save(D instance, Map params) {
        final session = datastore.currentSession
        boolean hasErrors = false
        boolean validate = params?.containsKey("validate") ? params.validate : true
        if (instance.respondsTo('validate') && validate) {
            session.setAttribute(instance, SKIP_VALIDATION_ATTRIBUTE, false)
            hasErrors = !instance.validate()
        }
        else {
            session.setAttribute(instance, SKIP_VALIDATION_ATTRIBUTE, true)
            instance.clearErrors()
        }

        if (hasErrors) {
            if (params?.failOnError) {
                throw validationException.newInstance( "Validation error occured during call to save()", instance.errors)
            }
            return null
        }

        session.persist(instance)
        if (params?.flush) {
            session.flush()
        }
        return instance
    }

    /**
     * Returns the objects identifier
     */
    Serializable ident(D instance) {
        instance[persistentEntity.getIdentity().name]
    }

    /**
     * Attaches an instance to an existing session. Requries a session-based model
     * @param instance The instance
     * @return
     */
    D attach(D instance) {
        datastore.currentSession.attach(instance)
        instance
    }

    /**
     * No concept of session-based model so defaults to true
     */
    boolean isAttached(D instance) {
        datastore.currentSession.contains(instance)
    }

    /**
     * Discards any pending changes. Requires a session-based model.
     */
    void discard(D instance) {
        datastore.currentSession.clear(instance)
    }

    /**
     * Deletes an instance from the datastore
     * @param instance The instance to delete
     */
    void delete(D instance) {
        delete(instance, Collections.emptyMap())
    }

    /**
     * Deletes an instance from the datastore
     * @param instance The instance to delete
     */
    void delete(D instance, Map params) {
        final session = datastore.currentSession
        session.delete(instance)
        if (params?.flush) {
            session.flush()
        }
    }
}
