/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation that marks a method as a listener for application events.
 *
 * <p>If an annotated method supports a single event type, the method may
 * declare a single parameter that reflects the event type to listen to.
 * If an annotated method supports multiple event types, this annotation
 * may refer to one or more supported event types using the {@code classes}
 * attribute. See the {@link #classes} javadoc for further details.
 *
 * <p>Events can be {@link ApplicationEvent} instances as well as arbitrary
 * objects.
 *
 * <p>Processing of {@code @EventListener} annotations is performed via
 * the internal {@link EventListenerMethodProcessor} bean which gets
 * registered automatically when using Java config or manually via the
 * {@code <context:annotation-config/>} or {@code <context:component-scan/>}
 * element when using XML config.
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they
 * do, the result of the method invocation is sent as a new event. If the
 * return type is either an array or a collection, each element is sent
 * as a new individual event.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <h3>Exception Handling</h3>
 * <p>While it is possible for an event listener to declare that it
 * throws arbitrary exception types, any checked exceptions thrown
 * from an event listener will be wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}
 * since the event publisher can only handle runtime exceptions.
 *
 * <h3>Asynchronous Listeners</h3>
 * <p>If you want a particular listener to process events asynchronously, you
 * can use Spring's {@link org.springframework.scheduling.annotation.Async @Async}
 * support, but be aware of the following limitations when using asynchronous events.
 *
 * <ul>
 * <li>If an asynchronous event listener throws an exception, it is not propagated
 * to the caller. See {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} for more details.</li>
 * <li>Asynchronous event listener methods cannot publish a subsequent event by returning a
 * value. If you need to publish another event as the result of the processing, inject an
 * {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher}
 * to publish the event manually.</li>
 * </ul>
 *
 * <h3>Ordering Listeners</h3>
 * <p>It is also possible to define the order in which listeners for a
 * certain event are to be invoked. To do so, add Spring's common
 * {@link org.springframework.core.annotation.Order @Order} annotation
 * alongside this event listener annotation.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see EventListenerMethodProcessor
 * @see org.springframework.transaction.event.TransactionalEventListener
 * @since 4.2
 */
// 将方法标记为应用程序事件监听器的注解。
//
// <p>如果带注解的方法支持单个事件类型，则该方法可以声明一个反映要监听的事件类型的参数。
// 如果带注解的方法支持多种事件类型，则此注解可以使用 {@code classes} 属性引用一种或多种受支持的事件类型。
// 有关更多详细信息，请参阅 {@link #classes} javadoc。
//
// <p>事件可以是 {@link ApplicationEvent} 实例以及任意对象。
//
// <p>{@code @EventListener} 注解的处理通过内部 {@link EventListenerMethodProcessor} bean 执行，
// 该 bean 在使用 Java 配置时自动注册或通过 {@code <context:annotation-config>} 或 {@code 手动注册使用
// XML 配置时的 <context:component-scan>} 元素。
//
// <p>带注解的方法可能具有非 {@code void} 返回类型。当他们这样做时，方法调用的结果作为一个新事件发送。
// 如果返回类型是数组或集合，则每个元素都作为一个新的单独事件发送。
//
// <p>这个注解可以用作<em>元注解<em>来创建自定义<em>组合注解<em>。
// <h3>异常处理<h3>
// <p>虽然事件监听器可以声明它抛出任意异常类型，但从事件监听器抛出的任何已检查异常都将包装在
// {@link java.lang.reflect. UndeclaredThrowableException UndeclaredThrowableException} 因为事件发布者只能处理运行时异常。
//
// <h3>Asynchronous Listeners<h3>
// <p>如果你想要一个特定的listener异步处理事件，你可以使用Spring的
// {@link org.springframework.scheduling.annotation.Async @Async}支持，但要注意以下限制使用异步事件时。
//
// <ul> <li>如果异步事件监听器抛出异常，它不会传播给调用者。有关更多详细信息，请参阅
// {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler AsyncUncaughtExceptionHandler}。
// <li> <li>异步事件监听器方法无法通过返回值来发布后续事件。如果你需要发布另一个事件作为处理的结果，注入一个
// {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher} 手动发布事件。<li> <ul>
//
// <h3>排序监听器<h3>
// <p>还可以定义调用特定事件的侦听器的顺序。为此，请在此事件监听器注解旁边添加 Spring 的通用
// {@link org.springframework.core.annotation.Order @Order} 注解。
//
// @EventListener 的工作原理：标注在方法上，所标注的方法必须是在一个 Configuration 的 Class 里面
// 或者在一个 Bean 里，它必然是在 Bean 的方法上进行二次处理。
// {@link org.springframework.context.event.EventListenerMethodProcessor}
//
// 方法上加上 @EventListener，最后会把它变成一个 ApplicationListenerMethodAdapter,
// 然后进行插入存储到 SimpleApplicationEventMulticaster
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

	/**
	 * Alias for {@link #classes}.
	 */
	// {@link classes} 的别名
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The event classes that this listener handles.
	 * <p>If this attribute is specified with a single value, the
	 * annotated method may optionally accept a single parameter.
	 * However, if this attribute is specified with multiple values,
	 * the annotated method must <em>not</em> declare any parameters.
	 */
	// 此监听器处理的事件类。
	// <p>如果使用单个值指定此属性，则带注解的方法可以选择接受单个参数。
	// 但是，如果此属性指定了多个值，则带注释的方法必须<em>不<em>声明任何参数。
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL) expression used for making the event
	 * handling conditional.
	 * <p>The event will be handled if the expression evaluates to boolean
	 * {@code true} or one of the following strings: {@code "true"}, {@code "on"},
	 * {@code "yes"}, or {@code "1"}.
	 * <p>The default expression is {@code ""}, meaning the event is always handled.
	 * <p>The SpEL expression will be evaluated against a dedicated context that
	 * provides the following metadata:
	 * <ul>
	 * <li>{@code #root.event} or {@code event} for references to the
	 * {@link ApplicationEvent}</li>
	 * <li>{@code #root.args} or {@code args} for references to the method
	 * arguments array</li>
	 * <li>Method arguments can be accessed by index. For example, the first
	 * argument can be accessed via {@code #root.args[0]}, {@code args[0]},
	 * {@code #a0}, or {@code #p0}.</li>
	 * <li>Method arguments can be accessed by name (with a preceding hash tag)
	 * if parameter names are available in the compiled byte code.</li>
	 * </ul>
	 */
	// Spring 表达式语言 (SpEL) 表达式用于条件事件处理。
	// <p>如果表达式的计算结果为布尔值 {@code true} 或以下字符串之一，则将处理该事件：
	// {@code "true"}, {@code "on"}, {@code "yes"},或{@代码“1”}。
	// <p>默认表达式是 {@code ""}，意味着事件总是被处理。
	// <p>SpEL 表达式将根据提供以下元数据的专用上下文进行评估：
	// <ul>
	// <li>{@code #root.event} 或{@code event} 用于引用 {@link ApplicationEvent}<li>
	// <li>{@code root.args} 或 {@code args} 用于引用方法参数数组<li>
	// <li>方法参数可以通过索引访问。例如，第一个参数可以通过 {@code #root.args[0]}、{@code args[0]}、
	// {@code #a0} 或 {@code #p0} 访问。<li>
	// <li>方法如果参数名称在编译的字节码中可用，则可以通过名称（带有前面的哈希标记）访问参数。<li>
	// <ul>
	String condition() default "";

	/**
	 * An optional identifier for the listener, defaulting to the fully-qualified
	 * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
	 *
	 * @see SmartApplicationListener#getListenerId()
	 * @see ApplicationEventMulticaster#removeApplicationListeners(Predicate)
	 * @since 5.3.5
	 */
	// 监听器的可选标识符，默认为声明方法的完全限定签名（例如“mypackage.MyClass.myMethod()”）。
	String id() default "";

}
