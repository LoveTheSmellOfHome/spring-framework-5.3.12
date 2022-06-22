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

package org.springframework.core;

import java.security.ProtectionDomain;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by a reloading-aware ClassLoader
 * (e.g. a Groovy-based ClassLoader). Detected for example by
 * Spring's CGLIB proxy factory for making a caching decision.
 *
 * <p>If a ClassLoader does <i>not</i> implement this interface,
 * then all of the classes obtained from it should be considered
 * as not reloadable (i.e. cacheable).
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
// 由 reloading-aware(可重新装载) 类的类加载器（例如基于 Groovy 的类加载器）实现的接口。
// 例如，由 Spring 的 CGLIB 代理工厂检测以做出缓存决策。
//
// 如果一个 ClassLoader 没有实现这个接口，那么从它获得的所有类都应该被认为是不可重新加载的（即可缓存的）。
public interface SmartClassLoader {

	/**
	 * Determine whether the given class is reloadable (in this ClassLoader).
	 * <p>Typically used to check whether the result may be cached (for this
	 * ClassLoader) or whether it should be reobtained every time.
	 * The default implementation always returns {@code false}.
	 * @param clazz the class to check (usually loaded from this ClassLoader)
	 * @return whether the class should be expected to appear in a reloaded
	 * version (with a different {@code Class} object) later on
	 */
	// 确定给定的类是否可重新加载（在此 ClassLoader 中）。
	// 通常用于检查结果是否可以被缓存（对于这个 ClassLoader）或者是否应该每次都重新获取。 默认实现总是返回false 。
	// 形参：
	//			clazz – 要检查的类（通常从这个 ClassLoader 加载）
	// 返回值：
	//			是否应该期望该类稍后出现在重新加载的版本中（具有不同的Class对象）
	default boolean isClassReloadable(Class<?> clazz) {
		return false;
	}

	/**
	 * Return the original ClassLoader for this SmartClassLoader, or potentially
	 * the present loader itself if it is self-sufficient.
	 * <p>The default implementation returns the local ClassLoader reference as-is.
	 * In case of a reloadable or other selectively overriding ClassLoader which
	 * commonly deals with unaffected classes from a base application class loader,
	 * this should get implemented to return the original ClassLoader that the
	 * present loader got derived from (e.g. through {@code return getParent();}).
	 * <p>This gets specifically used in Spring's AOP framework to determine the
	 * class loader for a specific proxy in case the target class has not been
	 * defined in the present class loader. In case of a reloadable class loader,
	 * we prefer the base application class loader for proxying general classes
	 * not defined in the reloadable class loader itself.
	 * @return the original ClassLoader (the same reference by default)
	 * @since 5.3.5
	 * @see ClassLoader#getParent()
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
	 */
	// 返回此 SmartClassLoader 的原始 ClassLoader，或者如果它是自给自足的，则可能返回当前加载器本身。
	//
	// 默认实现按原样返回本地 ClassLoader 引用。 在可重载或其他选择性覆盖的 ClassLoader 的情况下，
	// 它通常处理来自基本应用程序类加载器的不受影响的类，这应该被实现以返回当前加载器派生自的原始 ClassLoader（
	// 例如，通过return getParent(); ）
	//
	// 这在 Spring 的 AOP 框架中专门用于确定特定代理的类加载器，以防目标类尚未在当前类加载器中定义。 在
	// 可重载类加载器的情况下，我们更喜欢基本应用程序类加载器来代理可重载类加载器本身中未定义的通用类。
	//
	// 返回值：
	//			原始的 ClassLoader（默认相同的引用）
	default ClassLoader getOriginalClassLoader() {
		return (ClassLoader) this;
	}

	/**
	 * Define a custom class (typically a CGLIB proxy class) in this class loader.
	 * <p>This is a public equivalent of the protected
	 * {@code defineClass(String, byte[], int, int, ProtectionDomain)} method
	 * in {@link ClassLoader} which is traditionally invoked via reflection.
	 * A concrete implementation in a custom class loader should simply delegate
	 * to that protected method in order to make classloader-specific definitions
	 * publicly available without "illegal access" warnings on JDK 9+:
	 * {@code return defineClass(name, b, 0, b.length, protectionDomain)}.
	 * Note that the JDK 9+ {@code Lookup#defineClass} method does not support
	 * a custom target class loader for the new definition; it rather always
	 * defines the class in the same class loader as the lookup's context class.
	 * @param name the name of the class
	 * @param b the bytes defining the class
	 * @param protectionDomain the protection domain for the class, if any
	 * @return the newly created class
	 * @throws LinkageError in case of a bad class definition
	 * @throws SecurityException in case of an invalid definition attempt
	 * @throws UnsupportedOperationException in case of a custom definition attempt
	 * not being possible (thrown by the default implementation in this interface)
	 * @since 5.3.4
	 * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
	 */
	// 在这个类加载器中定义一个自定义类（通常是一个 CGLIB 代理类）。
	//
	// 这是传统上通过反射调用的ClassLoader中受保护的defineClass(String, byte[], int, int,
	// ProtectionDomain)方法的公共等效方法。 自定义类加载器中的具体实现应该简单地委托给该受保护方法，
	// 以使特定于类加载器的定义公开可用，而不会在 JDK 9+ 上出现“非法访问”警告： return defineClass(name,
	// b, 0, b.length, protectionDomain) . 请注意，JDK 9+ Lookup#defineClass方法不支持新定义
	// 的自定义目标类加载器； 它总是在与查找的上下文类相同的类加载器中定义类。
	//
	// 形参：
	//			name – 类的名称
	//			b – 定义类的字节
	//			protectionDomain – 类的保护域（如果有）
	// 返回值：
	//			新创建的类
	// 异常：
	//			LinkageError – 如果类定义错误
	//			SecurityException – 如果定义尝试无效
	//			UnsupportedOperationException – 如果无法进行自定义定义尝试（由此接口中的默认实现抛出）
	default Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
		throw new UnsupportedOperationException();
	}

}
