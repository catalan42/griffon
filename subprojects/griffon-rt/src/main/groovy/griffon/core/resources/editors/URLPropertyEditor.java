/*
 * Copyright 2010-2012 the original author or authors.
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

package griffon.core.resources.editors;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Andres Almiray
 * @since 1.1.0
 */
public class URLPropertyEditor extends AbstractPropertyEditor {
    public void setAsText(String value) throws IllegalArgumentException {
        setValue(value);
    }

    public void setValue(Object value) {
        if (null == value) return;
        if (value instanceof CharSequence) {
            handleAsString(String.valueOf(value));
        } else if (value instanceof File) {
            handleAsFile((File) value);
        } else if (value instanceof URL) {
            super.setValue(value);
        } else {
            throw illegalValue(value, URL.class);
        }
    }

    private void handleAsString(String str) {
        try {
            super.setValue(new URL(str));
        } catch (MalformedURLException e) {
            throw illegalValue(str, URL.class, e);
        }
    }

    private void handleAsFile(File file) {
        try {
            super.setValue(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw illegalValue(file, URL.class);
        }
    }
}
