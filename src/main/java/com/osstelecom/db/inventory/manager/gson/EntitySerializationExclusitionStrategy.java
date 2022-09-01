/*
 * Copyright 2014 Lucas Nishimura - lucas.nishimura@gmail.com.
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
package com.osstelecom.db.inventory.manager.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class EntitySerializationExclusitionStrategy implements ExclusionStrategy {

    private Map<String, String> serialized = new HashMap<>();

    public EntitySerializationExclusitionStrategy() {
    }

    @Override
    public boolean shouldSkipField(FieldAttributes fa) {

        Expose expose = fa.getAnnotation(Expose.class);
        GsonField hide = fa.getAnnotation(GsonField.class);

        if (hide != null) {
            return true;
        }
        if (expose != null) {
            return !expose.serialize();
        } else {
            return false;
        }
    }

    @Override
    public boolean shouldSkipClass(Class<?> type) {
        return false;
    }

}
