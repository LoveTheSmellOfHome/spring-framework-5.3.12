/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Class-level annotation that indicates to register instances of a class
 * with a JMX server, corresponding to the {@code ManagedResource} attribute.
 *
 * <p><b>Note:</b> This annotation is marked as inherited, allowing for generic
 * management-aware base classes. In such a scenario, it is recommended to
 * <i>not</i> specify an object name value since this would lead to naming
 * collisions in case of multiple subclasses getting registered.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedResource
 */
// 类级注解，指示向 JMX 服务器注册类的实例，对应于 {@code ManagedResource} 属性
// <p><b>注意：<b>这个注解被标记为继承的，允许通用的管理感知基类。在这种情况下，建议<i>不要<i>指定对象名称值，
// 因为这会在注册多个子类的情况下导致命名冲突。
// JMX:Java 管理扩展，Java Manged Extension,JMX 本身实现是非常复杂的，这个注解将 JMX 的复杂给大大简化了，
// 它提供了许多抽象概念，如：DynamicBean,DynamicMBean,MXBean,模型Bean,OpenMBean 等
// Spring 擅长将 API 封装，并且将复杂问题简单化
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedResource {

	/**
	 * Alias for the {@link #objectName} attribute, for simple default usage.
	 */
	// {@link objectName} 属性的别名，用于简单的默认用法
	@AliasFor("objectName")
	String value() default "";

	@AliasFor("value")
	String objectName() default "";

	String description() default "";

	int currencyTimeLimit() default -1;

	boolean log() default false;

	String logFile() default "";

	String persistPolicy() default "";

	int persistPeriod() default -1;

	String persistName() default "";

	String persistLocation() default "";

}
