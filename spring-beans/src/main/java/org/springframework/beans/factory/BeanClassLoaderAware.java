/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * Callback that allows a bean to be aware of the bean
 * {@link ClassLoader class loader}; that is, the class loader used by the
 * present bean factory to load bean classes.
 *
 * <p>This is mainly intended to be implemented by framework classes which
 * have to pick up application classes by name despite themselves potentially
 * being loaded from a shared class loader.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
// 允许 bean 知道 bean {@link ClassLoader class loader} 的回调；
// 即当前 bean 工厂用来加载 bean 类的类加载器
// <p>这主要是由框架类实现的，尽管它们可能是从共享类加载器加载的，但它们必须按名称获取应用程序类
// <p>有关所有 bean 生命周期方法的列表，请参阅 {@link BeanFactory BeanFactory javadocs}。
// 是个 临时类加载器
public interface BeanClassLoaderAware extends Aware {

	/**
	 * Callback that supplies the bean {@link ClassLoader class loader} to
	 * a bean instance.
	 * <p>Invoked <i>after</i> the population of normal bean properties but
	 * <i>before</i> an initialization callback such as
	 * {@link InitializingBean InitializingBean's}
	 * {@link InitializingBean#afterPropertiesSet()}
	 * method or a custom init-method.
	 * @param classLoader the owning class loader
	 */
	// 将 bean {@link ClassLoader class loader} 提供给 bean 实例的回调
	// <p>调用 <i>after<i> 普通 bean 属性的填充，但 <i>before<i> 初始化回调，
	// 例如 {@link InitializingBean InitializingBean's} {@link InitializingBeanafterPropertiesSet()}
	// 方法或自定义初始化方法.
	// @param classLoader 拥有的类加载器
	void setBeanClassLoader(ClassLoader classLoader);
}
