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

package org.gatein.portal.controller.resource;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.CharsetTextEncoder;
import org.exoplatform.commons.utils.CompositeReader;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.commons.utils.TextEncoder;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.gatein.common.io.IOTools;

import java.io.*;
/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class ScriptLoader implements Loader<ScriptKey, ScriptResult, ControllerContext> {

    public ScriptResult retrieve(ControllerContext context, ScriptKey key) throws Exception {
        JavascriptConfigService service = (JavascriptConfigService) PortalContainer.getComponent(JavascriptConfigService.class);

        //
        CompositeReader script = service.getCompositeScript(key.id, key.locale);
        String sourceName = key.id.getScope() + "/" + key.id.getName() + ".js";

        //
        if (script != null) {
            Reader result;
            //
            if (key.minified) {
                CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
                CompilerOptions options = new CompilerOptions();
                options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
                level.setOptionsForCompilationLevel(options);

                List<Reader> minifiedReaders = new ArrayList<>();
                if (CompositeReader.isMinify(script)) {
                    ScriptResult.Error error = minify(sourceName, script, minifiedReaders);
                    if (error != null) {
                        return error;
                    }
                } else {
                    List<Reader> readers = script.getCompounds();
                    for (Reader reader : readers) {
                        if (script.isMinify(reader)) {
                            ScriptResult.Error error = minify(sourceName, reader, minifiedReaders);
                            if (error != null) {
                                return error;
                            }
                        } else {
                            minifiedReaders.add(reader);
                        }
                    }
                }
                result = new CompositeReader(minifiedReaders);
            } else {
                result = script;
            }

            // Encode data
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                TextEncoder encoder = CharsetTextEncoder.getUTF8();
                char[] buffer = new char[256];
                for (int l = result.read(buffer); l != -1; l = result.read(buffer)) {
                    encoder.encode(buffer, 0, l, out);
                }
                return new ScriptResult.Resolved(out.toByteArray());
            } finally {
                Safe.close(result);
            }
        }

        //
        return ScriptResult.NOT_FOUND;
    }

    private ScriptResult.Error minify(String sourceName, Reader reader, List<Reader> minifiedReaders) throws IOException {
        CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        CompilerOptions options = new CompilerOptions();
        level.setOptionsForCompilationLevel(options);

        StringWriter code = new StringWriter();
        IOTools.copy(reader, code);
        JSSourceFile[] inputs = new JSSourceFile[] { JSSourceFile.fromCode(sourceName, code.toString()) };

        com.google.javascript.jscomp.Compiler compiler = new Compiler();
        compiler.setErrorManager(new LoggerErrorManager(java.util.logging.Logger.getLogger(ResourceRequestHandler.class
            .getName())));
        Result res = compiler.compile(new JSSourceFile[0], inputs, options);
        if (res.success) {
            minifiedReaders.add(new StringReader(compiler.toSource()));
            return null;
        } else {
            StringBuilder msg = new StringBuilder("Handle me gracefully JS errors\n");
            for (JSError error : res.errors) {
                msg.append(error.sourceName).append(":").append(error.lineNumber).append(" ").append(error.description)
                    .append("\n");
            }
            return new ScriptResult.Error(msg.toString());
        }
    }

}
