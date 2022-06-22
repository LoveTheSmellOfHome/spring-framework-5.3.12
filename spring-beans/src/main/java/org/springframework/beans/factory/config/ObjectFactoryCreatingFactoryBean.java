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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * A {@link org.springframework.beans.factory.FactoryBean} implementation that
 * returns a value which is an {@link org.springframework.beans.factory.ObjectFactory}
 * that in turn returns a bean sourced from a {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>As such, this may be used to avoid having a client object directly calling
 * {@link org.springframework.beans.factory.BeanFactory#getBean(String)} to get
 * a (typically prototype) bean from a
 * {@link org.springframework.beans.factory.BeanFactory}, which would be a
 * violation of the inversion of control principle. Instead, with the use
 * of this class, the client object can be fed an
 * {@link org.springframework.beans.factory.ObjectFactory} instance as a
 * property which directly returns only the one target bean (again, which is
 * typically a prototype bean).
 *
 * <p>A sample config in an XML-based
 * {@link org.springframework.beans.factory.BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean"&gt;
 *     &lt;property name="targetBeanName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might look
 * something like this:
 *
 * <pre class="code">package a.b.c;
 *
 * import org.springframework.beans.factory.ObjectFactory;
 *
 * public class MyClientBean {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // get a 'fresh', brand new MyService instance
 *     MyService service = this.myServiceFactory.getObject();
 *     // use the service object to effect the business logic...
 *   }
 * }</pre>
 *
 * <p>An alternate approach to this application of an object creational pattern
 * would be to use the {@link ServiceLocatorFactoryBean}
 * to source (prototype) beans. The {@link ServiceLocatorFactoryBean} approach
 * has the advantage of the fact that one doesn't have to depend on any
 * Spring-specific interface such as {@link org.springframework.beans.factory.ObjectFactory},
 * but has the disadvantage of requiring runtime class generation. Please do
 * consult the {@link ServiceLocatorFactoryBean ServiceLocatorFactoryBean JavaDoc}
 * for a fuller discussion of this issue.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.beans.factory.ObjectFactory
 * @see ServiceLocatorFactoryBean
 */
// 一个 org.springframework.beans.factory.FactoryBean 实现，它返回一个 ObjectFactory 值，该值又返回一个来自 BeanFactory 的 bean。
//
// 因此，这可以用来避免让客户端对象直接调用BeanFactory.getBean(String)从BeanFactory获取（通常是原型）bean，这将违反控制反转原则。相反，通过使用这个类，可以为客户端对象提供一个ObjectFactory实例作为一个属性，该属性直接返回一个目标 bean（同样，它通常是一个原型 bean）。
//
// 基于 XML 的BeanFactory中的示例配置可能如下所示：
//
// <beans>
//
//     <!-- Prototype bean since we have state -->
//     <bean id="myService" class="a.b.c.MyService" scope="prototype"/>
//
//     <bean id="myServiceFactory"
//         class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean">
//       <property name="targetBeanName"><idref local="myService"/></property>
//     </bean>
//
//     <bean id="clientBean" class="a.b.c.MyClientBean">
//       <property name="myServiceFactory" ref="myServiceFactory"/>
//     </bean>
//
//  </beans>
// 随之而来的 MyClientBean 类实现可能如下所示：
//package a.b.c;
//
//   import org.springframework.beans.factory.ObjectFactory;
//
//   public class MyClientBean {
//
//     private ObjectFactory<MyService> myServiceFactory;
//
//     public void setMyServiceFactory(ObjectFactory<MyService> myServiceFactory) {
//       this.myServiceFactory = myServiceFactory;
//     }
//
//     public void someBusinessMethod() {
//       // get a 'fresh', brand new MyService instance
//       MyService service = this.myServiceFactory.getObject();
//       // use the service object to effect the business logic...
//     }
//   }
// 对象创建模式的这种应用的另一种方法是使用 ServiceLocatorFactoryBean 来获取（原型）bean。 ServiceLocatorFactoryBean方法的
// 优点是不必依赖任何特定于 Spring 的接口，例如 ObjectFactory ，但缺点是需要生成运行时类。请查阅
// ServiceLocatorFactoryBean JavaDoc以更全面地讨论此问题。
//自：
//1.0.2
public class ObjectFactoryCreatingFactoryBean extends AbstractFactoryBean<ObjectFactory<Object>> {

	@Nullable
	private String targetBeanName;


	/**
	 * Set the name of the target bean.
	 * <p>The target does not <i>have</i> to be a non-singleton bean, but realistically
	 * always will be (because if the target bean were a singleton, then said singleton
	 * bean could simply be injected straight into the dependent object, thus obviating
	 * the need for the extra level of indirection afforded by this factory approach).
	 */
	// 设置目标bean的名称。
	// 目标不必是非单例 bean，但实际上总是会是（因为如果目标 bean 是单例，那么可以简单地将所述单例 bean 直接注入到依赖对象中，
	// 从而消除对额外级别的需要这种工厂方法提供的间接性）
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return ObjectFactory.class;
	}

	// 创建实例
	@Override
	protected ObjectFactory<Object> createInstance() {
		BeanFactory beanFactory = getBeanFactory();
		Assert.state(beanFactory != null, "No BeanFactory available");
		Assert.state(this.targetBeanName != null, "No target bean name specified");
		return new TargetBeanObjectFactory(beanFactory, this.targetBeanName);
	}


	/**
	 * Independent inner class - for serialization purposes.
	 */
	// 独立的内部类 - 用于序列化目的。
	@SuppressWarnings("serial")
	private static class TargetBeanObjectFactory implements ObjectFactory<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanObjectFactory(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object getObject() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
