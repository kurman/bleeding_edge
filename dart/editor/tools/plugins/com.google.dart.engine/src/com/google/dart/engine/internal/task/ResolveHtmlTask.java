/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.task;

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.builder.HtmlUnitBuilder;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.ResolvableHtmlUnit;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ResolveHtmlTask} resolve a specific source as an HTML file.
 */
public class ResolveHtmlTask extends AnalysisTask {
  /**
   * The source to be resolved.
   */
  private Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime = -1L;

  /**
   * The element produced by resolving the source.
   */
  private HtmlElement element = null;

  /**
   * The resolution errors that were discovered while resolving the source.
   */
  private AnalysisError[] resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   */
  public ResolveHtmlTask(InternalAnalysisContext context, Source source) {
    super(context);
    this.source = source;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitResolveHtmlTask(this);
  }

  public HtmlElement getElement() {
    return element;
  }

  /**
   * Return the time at which the contents of the source that was parsed were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   * 
   * @return the time at which the contents of the source that was parsed were last modified
   */
  public long getModificationTime() {
    return modificationTime;
  }

  public AnalysisError[] getResolutionErrors() {
    return resolutionErrors;
  }

  /**
   * Return the source that was or is to be resolved.
   * 
   * @return the source was or is to be resolved
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    if (source == null) {
      return "resolve as html null source";
    }
    return "resolve as html " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    ResolvableHtmlUnit resolvableHtmlUnit = getContext().computeResolvableHtmlUnit(source);
    HtmlUnit unit = resolvableHtmlUnit.getCompilationUnit();
    if (unit == null) {
      throw new AnalysisException(
          "Internal error: computeResolvableHtmlUnit returned a value without a parsed HTML unit");
    }
    modificationTime = resolvableHtmlUnit.getModificationTime();
    HtmlUnitBuilder builder = new HtmlUnitBuilder(getContext());
    element = builder.buildHtmlElement(source, modificationTime, unit);
    resolutionErrors = builder.getErrorListener().getErrors(source);
  }
}
