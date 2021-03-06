/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.compiler.support;

import griffon.core.Observable;
import groovy.beans.BindableASTTransformation;
import groovy.beans.VetoableASTTransformation;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import static org.codehaus.griffon.ast.GriffonASTUtils.*;
import static org.codehaus.groovy.ast.ClassHelper.*;

/**
 * @author Andres Almiray
 * @since 0.9.1
 */
public class ObservableASTInjector extends AbstractASTInjector {
    private static final Logger LOG = LoggerFactory.getLogger(ObservableASTInjector.class);
    private static final ClassNode OBSERVABLE_TYPE = makeClassSafe(Observable.class);

    private static final String LISTENER = "listener";
    private static final String NAME = "name";

    public void inject(ClassNode classNode, String artifactType) {
        if (!classNode.implementsInterface(OBSERVABLE_TYPE)) {
            classNode.addInterface(OBSERVABLE_TYPE);
        }

        if (isBindableOrVetoable(classNode)) return;

        for (FieldNode fieldNode : classNode.getFields()) {
            if (isBindableOrVetoable(fieldNode)) return;
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Injecting " + OBSERVABLE_TYPE.getName() + " behavior to " + classNode.getName());

        ClassNode pcsClassNode = makeClassSafe(PropertyChangeSupport.class);
        ClassNode pclClassNode = makeClassSafe(PropertyChangeListener.class);
        ClassNode pceClassNode = makeClassSafe(PropertyChangeEvent.class);

        //String pcsFieldName = "this$propertyChangeSupport";

        // add field:
        // protected final PropertyChangeSupport this$propertyChangeSupport = new java.beans.PropertyChangeSupport(this)
        FieldNode pcsField = injectField(classNode,
                "this$propertyChangeSupport",
                ACC_FINAL | ACC_PROTECTED,
                pcsClassNode,
                ctor(pcsClassNode, args(VariableExpression.THIS_EXPRESSION)));

        // add method:
        // void addPropertyChangeListener(listener) {
        //     pcs.addPropertyChangeListener(listener)
        //  }
        injectMethod(classNode,
                new MethodNode(
                        "addPropertyChangeListener",
                        ACC_PUBLIC,
                        VOID_TYPE,
                        params(param(pclClassNode, LISTENER)),
                        NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "addPropertyChangeListener",
                                args(var(LISTENER))))));

        // add method:
        // void addPropertyChangeListener(name, listener) {
        //     pcs.addPropertyChangeListener(name, listener)
        //  }
        injectMethod(classNode,
                new MethodNode(
                        "addPropertyChangeListener",
                        ACC_PUBLIC,
                        VOID_TYPE,
                        params(
                                param(STRING_TYPE, NAME),
                                param(pclClassNode, LISTENER)),
                        NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "addPropertyChangeListener",
                                args(var(NAME), var(LISTENER))))));

        // add method:
        // void removePropertyChangeListener(listener) {
        //    return pcs.removePropertyChangeListener(listener);
        // }
        injectMethod(classNode,
                new MethodNode(
                        "removePropertyChangeListener",
                        ACC_PUBLIC,
                        VOID_TYPE,
                        params(param(pclClassNode, LISTENER)),
                        NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "removePropertyChangeListener",
                                args(var(LISTENER))))));

        // add method:
        // void removePropertyChangeListener(name, listener) {
        //    return pcs.removePropertyChangeListener(name, listener);
        // }
        injectMethod(classNode,
                new MethodNode(
                        "removePropertyChangeListener",
                        ACC_PUBLIC,
                        VOID_TYPE,
                        params(
                                param(STRING_TYPE, NAME),
                                param(pclClassNode, LISTENER)), NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "removePropertyChangeListener",
                                args(var(NAME), var(LISTENER))))));

        // add method:
        // void firePropertyChange(String name, Object oldValue, Object newValue) {
        //     pcs.firePropertyChange(name, oldValue, newValue)
        //  }
        injectMethod(classNode,
                new MethodNode(
                        "firePropertyChange",
                        ACC_PROTECTED,
                        VOID_TYPE,
                        params(
                                param(STRING_TYPE, NAME),
                                param(makeClassSafe(OBJECT_TYPE), "oldValue"),
                                param(OBJECT_TYPE, "newValue")),
                        NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "firePropertyChange",
                                args(var(NAME), var("oldValue"), var("newValue"))))));

        // add method:
        // void firePropertyChange(PropertyChangeEvent event) {
        //     pcs.firePropertyChange(event)
        //  }
        injectMethod(classNode,
                new MethodNode(
                        "firePropertyChange",
                        ACC_PROTECTED,
                        VOID_TYPE,
                        params(param(pceClassNode, "event")),
                        NO_EXCEPTIONS,
                        stmnt(call(
                                field(pcsField),
                                "firePropertyChange",
                                args(var("event"))))));

        // add method:
        // PropertyChangeListener[] getPropertyChangeListeners() {
        //   return pcs.getPropertyChangeListeners
        // }
        injectMethod(classNode,
                new MethodNode(
                        "getPropertyChangeListeners",
                        ACC_PUBLIC,
                        pclClassNode.makeArray(),
                        NO_PARAMS,
                        NO_EXCEPTIONS,
                        returns(call(
                                field(pcsField),
                                "getPropertyChangeListeners",
                                NO_ARGS))));

        // add method:
        // PropertyChangeListener[] getPropertyChangeListeners(String name) {
        //   return pcs.getPropertyChangeListeners(name)
        // }
        injectMethod(classNode,
                new MethodNode(
                        "getPropertyChangeListeners",
                        ACC_PUBLIC,
                        pclClassNode.makeArray(),
                        params(param(STRING_TYPE, NAME)),
                        NO_EXCEPTIONS,
                        returns(call(
                                field(pcsField),
                                "getPropertyChangeListeners",
                                args(var(NAME))))));
    }

    private static boolean isBindableOrVetoable(AnnotatedNode node) {
        if (VetoableASTTransformation.hasVetoableAnnotation(node) ||
                BindableASTTransformation.hasBindableAnnotation(node)) {
            return true;
        }
        return false;
    }
}