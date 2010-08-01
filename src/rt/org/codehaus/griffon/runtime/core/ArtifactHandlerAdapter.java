/*
 * Copyright 2009-2010 the original author or authors.
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

package org.codehaus.griffon.runtime.core;

import griffon.core.ArtifactInfo;
import griffon.core.ArtifactHandler;
import griffon.core.GriffonClass;
import griffon.core.GriffonApplication;
import griffon.util.GriffonNameUtils;

/**
 * Base implementation of the ArtifactHandler interface.
 *
 * @author Andres Almiray
 *
 * @since 0.9.1
 */
public abstract class ArtifactHandlerAdapter implements ArtifactHandler {
    private final String type;
    private final String trailing;
    private final GriffonApplication app;

    private static final Class[] GRIFFON_CLASS_CTOR_ARGS = new Class[]{GriffonApplication.class, ArtifactInfo.class};
    private ArtifactInfo[] artifacts = new ArtifactInfo[0];
    private GriffonClass[] classes = new GriffonClass[0];

    public ArtifactHandlerAdapter(GriffonApplication app, String type, String trailing) {
	    this.app = app;
        this.type = type;
        this.trailing = trailing == null ? "" : trailing;
    }

    public String getType() {
        return type;
    }

    public String getTrailing() {
        return trailing;
    }

    public void initialize(ArtifactInfo[] artifacts) {
        this.artifacts = new ArtifactInfo[artifacts.length];
        System.arraycopy(artifacts, 0, this.artifacts, 0, artifacts.length);
        classes = new GriffonClass[artifacts.length];
        for(int i = 0; i < artifacts.length; i++) {
	        classes[i] = newGriffonClassInstance(artifacts[i].getClazz());
        }
    }

    protected abstract GriffonClass newGriffonClassInstance(Class clazz);

    /**
     * Returns true if the target Class is a class artifact
     * handled by this object.<p>
     * This implementation performs an equality check on class.name
     */
    public boolean isArtifact(Class clazz) {
        for(ArtifactInfo artifact: artifacts) {
            if(artifact.getClazz().getName().equals(clazz.getName())) return true;
        }
        return false;
    }

    public boolean isArtifact(GriffonClass clazz) {
        for(GriffonClass griffonClass: classes) {
            if(griffonClass.equals(clazz)) return true;
        }
        return false;
    }

    public GriffonClass[] getClasses() {
        return classes;
    }

    public GriffonClass getClassFor(Class clazz) {
	    if(clazz == null) return null;
        return getClassFor(clazz.getName());
    }

    public GriffonClass getClassFor(String fqnClassName) {
	    if(GriffonNameUtils.isBlank(fqnClassName)) return null;
        for(GriffonClass griffonClass: classes) {
            if(griffonClass.getClazz().getName().equals(fqnClassName)) return griffonClass;
        }
        return null;
    }

    public GriffonClass findClassFor(String propertyName) {	
	    if(GriffonNameUtils.isBlank(propertyName)) return null;
	
        String simpleName = propertyName;

        int lastDot = propertyName.lastIndexOf(".");
        if(lastDot > -1) {
	        simpleName = simpleName.substring(lastDot + 1);
        }

        if(simpleName.length() == 1) {
            simpleName = simpleName.toUpperCase();
        } else {
            simpleName = simpleName.substring(0, 1).toUpperCase() + simpleName.substring(1);
        }

        if(!simpleName.endsWith(trailing)) {
	        simpleName += trailing;
        }

        for(GriffonClass griffonClass: classes) {
            if(griffonClass.getClazz().getSimpleName().equals(simpleName)) return griffonClass;
        }

        return null;
    }

    public GriffonApplication getApp() {
        return app;
    }
}