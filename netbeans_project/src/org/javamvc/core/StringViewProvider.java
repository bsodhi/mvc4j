/*
Copyright 2015 Balwinder Sodhi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.javamvc.core;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *
 * A view provider which simply returns the string representation of the model.
 */
public class StringViewProvider implements ViewProvider {

    @Override
    public void init(ServletContext servletContext, ServletConfig servletConfig) {
        // Do nothing
    }

    /**
     * It simply returns the string representation of supplied model. Name of
     * the view is ignored.
     * @param view
     * @param model
     * @return
     * @throws IOException 
     */
    @Override
    public String renderView(String view, Object model) throws IOException {
        return ""+model;
    }
    
}
