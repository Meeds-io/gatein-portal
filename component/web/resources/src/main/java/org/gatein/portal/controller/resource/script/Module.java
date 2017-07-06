/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.portal.controller.resource.script;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import org.exoplatform.commons.utils.CompositeReader;
import org.exoplatform.commons.utils.PropertyResolverReader;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public abstract class Module {

    /** Our logger. */
    private static final Logger log = LoggerFactory.getLogger(Module.class);

    /** . */
    public static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return locale.equals(Locale.ENGLISH) ? null : Locale.ENGLISH;
        }
    };

    /** . */
    protected ScriptResource resource;

    /** . */
    protected final String contextPath;

    /** . */
    protected int priority;

    Module(ScriptResource resource, String contextPath, int priority) {
        this.resource = resource;
        this.contextPath = contextPath;
        this.priority = priority;
    }

    public static class Remote extends Module {

        /** . */
        private final String uri;

        Remote(ScriptResource resource, String contextPath, String uri, int priority) {
            super(resource, contextPath, priority);

            //
            this.uri = uri;
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public String getURI() {
            return uri;
        }
    }

    public static class Local extends Module {
        /** . */
        private final Content[] contents;

        /** . */
        private final String resourceBundle;

        /** . */
        private final Map<QualifiedName, String> parameters;

        private final boolean minify;

        Local(ScriptResource resource, String contextPath, String path, String resourceBundle, int priority) {
            this(resource, contextPath, new Content[] { new Content(path) }, resourceBundle, priority);
        }

        Local(ScriptResource resource, String contextPath, Content[] contents, String resourceBundle, int priority) {
            this(resource, contextPath, contents, resourceBundle, priority, true);
        }

        Local(ScriptResource resource, String contextPath, Content[] contents, String resourceBundle, int priority, boolean minify) {
            super(resource, contextPath, priority);

            //
            Map<QualifiedName, String> parameters = new HashMap<QualifiedName, String>();
            parameters.put(WebAppController.HANDLER_PARAM, "script");
            parameters.put(ResourceRequestHandler.RESOURCE_QN, resource.getId().getName());
            parameters.put(ResourceRequestHandler.SCOPE_QN, resource.getId().getScope().name());

            //
            if (contents == null) {
                throw new IllegalArgumentException("contents must be not null");
            }
            this.contents = contents;
            this.parameters = parameters;
            this.resourceBundle = resourceBundle;
            this.minify = minify;
        }

        public String getPath() {
            for (Content ct : contents) {
                if (ct.isPath()) {
                    return ct.getSource();
                }
            }
            return null;
        }

        public Content[] getContents() {
            return contents;
        }

        public String getResourceBundle() {
            return resourceBundle;
        }

        public Map<QualifiedName, String> getParameters() {
            return parameters;
        }

        public boolean isMinify() {
            return minify;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getURI() {
            return contextPath + getPath();
        }

        /**
         *
         * @param locale the desired locale, if null, <code>Locale.ENGLISH</code> will be used
         * @param scriptLoader the script loader
         * @param bundleLoader the bundle loader
         * @return a reader for the resource or null if the resource cannot be resolved
         */
        public Reader read(Locale locale, ServletContext scriptLoader, ClassLoader bundleLoader) {
            List<Reader> readers = new LinkedList<Reader>();
            for (Content content : contents) {
                if (content.isPath()) {
                    Reader script = getScript(content.getSource(), locale, scriptLoader, bundleLoader);
                    if (script != null) {
                        readers.add(script);
                    } else {
                        log.warn("File not found: " + content.getSource());
                    }
                } else {
                    readers.add(new StringReader("\n" + content.getSource() + "\n"));
                }
            }

            if (readers.size() > 0) {
                return new CompositeReader(readers);
            } else {
                return null;
            }
        }

        private Reader getScript(String pt, Locale locale, ServletContext scriptLoader, ClassLoader bundleLoader) {
            InputStream in = scriptLoader.getResourceAsStream(pt);
            if (in != null) {
                Reader reader = new InputStreamReader(in);
                if (resourceBundle != null) {
                    if (locale == null) {
                        locale = Locale.ENGLISH;
                    }

                    //
                    log.debug("About to load a bundle for locale " + locale + " and bundle " + resourceBundle);
                    final ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle, locale, bundleLoader, CONTROL);
                    if (bundle != null) {
                        log.debug("Found bundle " + bundle + " for locale " + locale + " and bundle " + resourceBundle);
                        reader = new PropertyResolverReader(reader) {
                            @Override
                            protected String resolve(String name) {
                                try {
                                    String val = bundle.getString(name);
                                    return escapeJavascriptStringLiteral(val);
                                } catch (MissingResourceException e) {
                                    // Need to use logging
                                    log.debug("Could not resolve property " + name + " when filtering JS");
                                    return "";
                                }
                            }
                        };
                    }
                }
                return reader;
            }
            return null;
        }

        public static class Content {
            private String source;
            private boolean isPath;

            public Content(String source) {
                this(source, true);
            }

            public Content(String content, boolean isPath) {
                this.source = content;
                this.isPath = isPath;
            }

            public String getSource() {
                return source;
            }

            public boolean isPath() {
                return isPath;
            }
        }
    }

    public ScriptResource getResource() {
        return resource;
    }

    public abstract boolean isRemote();

    public abstract String getURI();

    public String getContextPath() {
        return contextPath;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Escape simple and double quotes chars.
     *
     * @param s the string to escape
     * @return the escaped string
     */
    private static String escapeJavascriptStringLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\'') {
                sb.append("\\\'");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();

    }
}
