/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method (typically a JavaBean setter method) as being 'required': that is,
 * the setter method must be configured to be dependency-injected with a value.
 *
 * <p>Please do consult the javadoc for the {@link RequiredAnnotationBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author Rob Harrop
 * @since 2.0
 * @see RequiredAnnotationBeanPostProcessor
 * @deprecated as of 5.1, in favor of using constructor injection for required settings
 * (or a custom {@link org.springframework.beans.factory.InitializingBean} implementation)
 */
// 将方法（通常是 JavaBean setter 方法）标记为 'required'
// 即“必需”：也就是说，为使用的值进行依赖注入 setter 方法必须配置。
//
// 请务必查阅有关 RequiredAnnotationBeanPostProcessor 类的 javadoc（默认情况下，该类检查是否存在此注释）。
//已弃用
//从 5.1 开始，赞成使用构造函数注入进行所需的设置（或自定义org.springframework.beans.factory.InitializingBean 实现）
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Required {

}
