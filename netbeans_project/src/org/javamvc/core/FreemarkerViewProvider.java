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

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * A view provider based on Freemarker template engine. The templates are
 * expected to be stored in some path under the web root folder of the application.
 * Extension of template files can be configured via servlet init parameter
 * named <code>template.extension</code>. If not configured then .html is taken
 * as default extension of templates.
 * 
 * @author Balwinder Sodhi
 */
public class FreemarkerViewProvider implements ViewProvider {

    private static Configuration cfg = new Configuration();
    private String templateExtension;
    
    @Override
    public void init(ServletContext servletContext, ServletConfig servletConfig) {
        templateExtension = servletConfig.getInitParameter("template.extension");
        if (null == templateExtension) {
            templateExtension = ".html";
        }
        // Specify the data source where the template files come from. Here I set a
        // plain directory for it, but non-file-system are possible too:
        //cfg.setDirectoryForTemplateLoading(new File("."));
        //cfg.setClassForTemplateLoading(this.getClass(), "/");
        cfg.setServletContextForTemplateLoading(servletContext, "/");

        // Specify how templates will see the data-model. This is an advanced topic...
        // for now just use this:
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        // Set your preferred charset template files are stored in. UTF-8 is
        // a good choice in most applications:
        cfg.setDefaultEncoding("UTF-8");

        // Sets how errors will appear. Here we assume we are developing HTML pages.
        // For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

        // At least in new projects, specify that you want the fixes that aren't
        // 100% backward compatible too (these are very low-risk changes as far as the
        // 1st and 2nd version number remains):
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20
    }

    @Override
    public String renderView(String view, Object model) throws IOException {
        Template temp = cfg.getTemplate("/"+view+templateExtension);
        StringWriter writer = new StringWriter();
        try {
            temp.process(model, writer);
        } catch (TemplateException ex) {
            throw new IOException("Could not process template. ", ex);
        }
        return writer.toString();
    }

}
