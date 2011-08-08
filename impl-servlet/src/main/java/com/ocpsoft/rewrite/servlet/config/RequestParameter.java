/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
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
package com.ocpsoft.rewrite.servlet.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.ocpsoft.rewrite.EvaluationContext;
import com.ocpsoft.rewrite.servlet.config.parameters.DefaultBindable;
import com.ocpsoft.rewrite.servlet.config.parameters.ParameterBinding;
import com.ocpsoft.rewrite.servlet.config.parameters.binding.Bindings;
import com.ocpsoft.rewrite.servlet.config.parameters.binding.Evaluation;
import com.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;
import com.ocpsoft.rewrite.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class RequestParameter extends HttpCondition
{
   private final String name;

   private final Pattern nameRegex;
   private final Pattern value;

   @SuppressWarnings({ "rawtypes", "unchecked" })
   private final DefaultBindable<?, ParameterBinding> bindable = new DefaultBindable();

   private RequestParameter(final String name, final String nameRegex, final String valueRegex)
   {
      // TODO Refactor this to two different objects internally
      Assert.notNull(nameRegex, "Parameter name pattern cannot be null.");
      Assert.notNull(valueRegex, "Parameter value pattern cannot be null.");
      this.name = name;
      this.nameRegex = Pattern.compile(nameRegex);
      this.value = Pattern.compile(valueRegex);

      this.bindsTo(Evaluation.property(nameRegex));
   }

   public RequestParameter bindsTo(final ParameterBinding binding)
   {
      this.bindable.bindsTo(binding);
      return this;
   }

   public RequestParameter attemptBindTo(final ParameterBinding binding)
   {
      this.bindable.attemptBindTo(binding);
      return this;
   }

   /**
    * Return a {@link RequestParameter} condition that matches against both parameter name and values.
    * 
    * @param nameRegex Regular expression matching the parameter name
    * @param valueRegex Regular expression matching the parameter value
    */
   public static RequestParameter matches(final String nameRegex, final String valueRegex)
   {
      return new RequestParameter(null, nameRegex, valueRegex);
   }

   /**
    * Return a {@link RequestParameter} condition that matches only against the existence of a parameter with the given
    * name. The parameter value is ignored.
    * 
    * @param name The parameter name
    */
   public static RequestParameter exists(final String name)
   {
      return new RequestParameter(name, name, ".*");
   }

   /**
    * Return a {@link RequestParameter} condition that matches only against the existence of a parameter with a name
    * matching the given pattern. The parameter value is ignored.
    * 
    * @param nameRegex Regular expression matching the parameter name
    */
   public static RequestParameter existsMatching(final String nameRegex)
   {
      return new RequestParameter(null, nameRegex, ".*");
   }

   /**
    * Return a {@link RequestParameter} condition that matches only against the existence of a parameter with value
    * matching the given pattern. The parameter name is ignored.
    * 
    * @param valueRegex Regular expression matching the parameter value
    */
   public static RequestParameter valueExists(final String valueRegex)
   {
      return new RequestParameter(null, ".*", valueRegex);
   }

   @Override
   public boolean evaluateHttp(final HttpServletRewrite event, final EvaluationContext context)
   {
      String matchedParameter = null;

      List<String> values = new ArrayList<String>();

      HttpServletRequest request = event.getRequest();
      for (String parameter : Collections.list(request.getParameterNames()))
      {
         if (name != null)
         {
            if (name.equals(parameter))
            {
               matchedParameter = name;
               values.addAll(Arrays.asList(request.getParameterValues(matchedParameter)));
               break;
            }
         }
         else if (nameRegex.matcher(parameter).matches() && matchesValue(request, parameter))
         {
            matchedParameter = parameter;
            values.addAll(Arrays.asList(request.getParameterValues(matchedParameter)));
         }
      }

      if (matchedParameter != null)
      {
         Bindings.evaluateCondition(event, context, bindable, values.toArray(new String[] {}));
         return true;
      }

      return false;
   }

   private boolean matchesValue(final HttpServletRequest request, final String parameter)
   {
      for (String contents : Arrays.asList(request.getParameterValues(parameter)))
      {
         if (value.matcher(contents).matches())
         {
            return true;
         }
      }
      return false;
   }
}