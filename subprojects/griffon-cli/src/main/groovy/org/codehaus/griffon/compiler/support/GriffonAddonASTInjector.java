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

import griffon.core.GriffonApplication;
import org.codehaus.griffon.ast.AbstractASTTransformation;
import org.codehaus.griffon.ast.ResourcesAwareASTTransformation;
import org.codehaus.griffon.ast.ThreadingAwareASTTransformation;
import org.codehaus.griffon.runtime.util.GriffonApplicationHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codehaus.griffon.ast.GriffonASTUtils.*;
import static org.codehaus.groovy.ast.ClassHelper.*;

/**
 * @author Andres Almiray
 * @since 0.9.1
 */
public class GriffonAddonASTInjector extends AbstractASTInjector {
    private static final ClassNode GRIFFON_APPLICATION_TYPE = makeClassSafe(GriffonApplication.class);
    private static final ClassNode GAH_TYPE = makeClassSafe(GriffonApplicationHelper.class);
    private static final ClassNode LOGGER_TYPE = makeClassSafe(Logger.class);
    private static final ClassNode LOGGER_FACTORY_TYPE = makeClassSafe(LoggerFactory.class);
    public static final String APP = "app";

    public void inject(ClassNode classNode, String artifactType) {
        addDefaultConstructor(classNode);

        // GriffonApplication getApp()
        // void setApp(GriffonApplication app)
        classNode.addProperty(APP, ACC_PUBLIC, GRIFFON_APPLICATION_TYPE, null, null, null);

        // Object newInstance()
        classNode.addMethod(new MethodNode(
                "newInstance",
                ACC_PUBLIC,
                makeClassSafe(OBJECT_TYPE),
                params(
                        param(makeClassSafe(CLASS_Type), "clazz"),
                        param(STRING_TYPE, "type")),
                NO_EXCEPTIONS,
                returns(call(
                        GAH_TYPE,
                        "newInstance",
                        vars(APP, "clazz", "type")))
        ));

        String loggerCategory = "griffon.addon." + classNode.getName();
        FieldNode loggerField = classNode.addField(
                "this$logger",
                ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
                LOGGER_TYPE,
                call(
                        LOGGER_FACTORY_TYPE,
                        "getLogger",
                        args(constx(loggerCategory)))
        );

        // Logger getLog()
        classNode.addMethod(new MethodNode(
                "getLog",
                ACC_PUBLIC,
                LOGGER_TYPE,
                NO_PARAMS,
                NO_EXCEPTIONS,
                returns(field(loggerField))
        ));

        ThreadingAwareASTTransformation.apply(classNode);
        ResourcesAwareASTTransformation.apply(classNode);
    }

    public static void addDefaultConstructor(ClassNode classNode) {
        boolean noArgsConstructorDeclared = false;
        for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
            if (constructor.getParameters().length == 0) {
                noArgsConstructorDeclared = true;
                break;
            }
        }
        if (!noArgsConstructorDeclared) {
            classNode.addConstructor(new ConstructorNode(
                    ACC_PUBLIC,
                    block(
                            stmnt(ctor(ClassNode.SUPER, args(
                                    AbstractASTTransformation.applicationInstance()
                            )))
                    )
            ));
        }
    }
}
