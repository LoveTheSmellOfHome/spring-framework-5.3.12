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

package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.asm.Type;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.ClassLoaderAwareGeneratorStrategy;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.cglib.transform.ClassEmitterTransformer;
import org.springframework.cglib.transform.TransformingClassGenerator;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Enhances {@link Configuration} classes by generating a CGLIB subclass which
 * interacts with the Spring container to respect bean scoping semantics for
 * {@code @Bean} methods. Each such {@code @Bean} method will be overridden in
 * the generated subclass, only delegating to the actual {@code @Bean} method
 * implementation if the container actually requests the construction of a new
 * instance. Otherwise, a call to such an {@code @Bean} method serves as a
 * reference back to the container, obtaining the corresponding bean by name.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see #enhance
 * @see ConfigurationClassPostProcessor
 */
// 通过生成与 Spring 容器交互的 CGLIB 子类来增强 {@link Configuration} Configuration类，以尊重@Bean方法的 bean 范围语义。
// 每个这样的@Bean方法都将在生成的子类中被覆盖，只有在容器实际请求构造新实例时才委托给实际的@Bean方法实现。
// 否则，对此类@Bean方法的调用将作为对容器的引用，通过名称获取相应的 bean
//
// 这里边用到了 CGLIB 相关的 API。如果在类上标注了 @Configuration，那么该类字节码就会被提升，只有 @Bean 就不会提升
class ConfigurationClassEnhancer {

	// The callbacks to use. Note that these callbacks must be stateless.
	// 要使用的回调。请注意，这些回调必须是无状态的
	private static final Callback[] CALLBACKS = new Callback[] {
			new BeanMethodInterceptor(),
			new BeanFactoryAwareMethodInterceptor(),
			NoOp.INSTANCE
	};

	private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);

	private static final String BEAN_FACTORY_FIELD = "$$beanFactory";


	private static final Log logger = LogFactory.getLog(ConfigurationClassEnhancer.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();


	/**
	 * Loads the specified class and generates a CGLIB subclass of it equipped with
	 * container-aware callbacks capable of respecting scoping and other bean semantics.
	 * @return the enhanced subclass
	 */
	// 加载指定的类并生成它的 CGLIB 子类，该子类配备了能够尊重作用域和其他 bean 语义的容器感知回调。
	// 返回值：增强的子类
	public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
		if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Ignoring request to enhance %s as it has " +
						"already been enhanced. This usually indicates that more than one " +
						"ConfigurationClassPostProcessor has been registered (e.g. via " +
						"<context:annotation-config>). This is harmless, but you may " +
						"want check your configuration and remove one CCPP if possible",
						configClass.getName()));
			}
			return configClass;
		}
		Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Successfully enhanced %s; enhanced class name is: %s",
					configClass.getName(), enhancedClass.getName()));
		}
		return enhancedClass;
	}

	/**
	 * Creates a new CGLIB {@link Enhancer} instance.
	 */
	// 创建一个新的 CGLIB {@link Enhancer} 实例。
	// 如果在类上标注了 @Configuration，那么该类字节码就会被提升，只有 @Bean 就不会提升
	private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
		Enhancer enhancer = new Enhancer();
		// 标注了 @Configuration 的类就是 configSuperClass，也就是说 CGLIB 会生成子类，父类就是要代理的对象
		enhancer.setSuperclass(configSuperClass);
		enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
		enhancer.setUseFactory(false);
		// 命名策略：单例常量 BySpringCGLIB
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
		enhancer.setCallbackFilter(CALLBACK_FILTER);
		enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
		return enhancer;
	}

	/**
	 * Uses enhancer to generate a subclass of superclass,
	 * ensuring that callbacks are registered for the new subclass.
	 */
	// 使用增强器生成超类的子类，确保为新子类注册回调。
	private Class<?> createClass(Enhancer enhancer) {
		Class<?> subclass = enhancer.createClass();
		// Registering callbacks statically (as opposed to thread-local)
		// is critical for usage in an OSGi environment (SPR-5932)...
		// 静态注册回调（而不是线程本地）对于在 OSGi 环境中使用至关重要 (SPR-5932)...
		Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
		return subclass;
	}


	/**
	 * Marker interface to be implemented by all @Configuration CGLIB subclasses.
	 * Facilitates idempotent behavior for {@link ConfigurationClassEnhancer#enhance}
	 * through checking to see if candidate classes are already assignable to it, e.g.
	 * have already been enhanced.
	 * <p>Also extends {@link BeanFactoryAware}, as all enhanced {@code @Configuration}
	 * classes require access to the {@link BeanFactory} that created them.
	 * <p>Note that this interface is intended for framework-internal use only, however
	 * must remain public in order to allow access to subclasses generated from other
	 * packages (i.e. user code).
	 */
	// 所有 @Configuration CGLIB 子类要实现的标记接口。 通过检查候选类是否已经可以分配给它，
	// 例如已经被增强，从而促进用于 enhance 幂等行为。
	// 还扩展了 BeanFactoryAware ，因为所有增强的 @Configuration 类都需要访问创建它们 的BeanFactory 。
	// 请注意，此接口仅供框架内部使用，但必须保持公共状态，以允许访问从其他包（即用户代码）生成的子类。
	public interface EnhancedConfiguration extends BeanFactoryAware {
	}


	/**
	 * Conditional {@link Callback}.
	 * @see ConditionalCallbackFilter
	 */
	// 条件回调
	private interface ConditionalCallback extends Callback {

		boolean isMatch(Method candidateMethod);
	}


	/**
	 * A {@link CallbackFilter} that works by interrogating {@link Callback Callbacks} in the order
	 * that they are defined via {@link ConditionalCallback}.
	 */
	// 一个 CallbackFilter 通过询问工程 Callbacks 的顺序，他们是通过定义 ConfigurationClassEnhancer.ConditionalCallback
	private static class ConditionalCallbackFilter implements CallbackFilter {

		private final Callback[] callbacks;

		private final Class<?>[] callbackTypes;

		public ConditionalCallbackFilter(Callback[] callbacks) {
			this.callbacks = callbacks;
			this.callbackTypes = new Class<?>[callbacks.length];
			for (int i = 0; i < callbacks.length; i++) {
				this.callbackTypes[i] = callbacks[i].getClass();
			}
		}

		@Override
		public int accept(Method method) {
			for (int i = 0; i < this.callbacks.length; i++) {
				Callback callback = this.callbacks[i];
				if (!(callback instanceof ConditionalCallback) || ((ConditionalCallback) callback).isMatch(method)) {
					return i;
				}
			}
			throw new IllegalStateException("No callback available for method " + method.getName());
		}

		public Class<?>[] getCallbackTypes() {
			return this.callbackTypes;
		}
	}


	/**
	 * Custom extension of CGLIB's DefaultGeneratorStrategy, introducing a {@link BeanFactory} field.
	 * Also exposes the application ClassLoader as thread context ClassLoader for the time of
	 * class generation (in order for ASM to pick it up when doing common superclass resolution).
	 */
	// CGLIB 的 DefaultGeneratorStrategy 的自定义扩展，引入了 BeanFactory 字段。
	// 还将应用程序 ClassLoader 公开为类生成时的线程上下文 ClassLoader（以便 ASM 在执行常见超类解析时将其拾取）。
	private static class BeanFactoryAwareGeneratorStrategy extends
			ClassLoaderAwareGeneratorStrategy {

		public BeanFactoryAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
			super(classLoader);
		}

		@Override
		protected ClassGenerator transform(ClassGenerator cg) throws Exception {
			ClassEmitterTransformer transformer = new ClassEmitterTransformer() {
				@Override
				public void end_class() {
					declare_field(Constants.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.getType(BeanFactory.class), null);
					super.end_class();
				}
			};
			return new TransformingClassGenerator(cg, transformer);
		}

	}


	/**
	 * Intercepts the invocation of any {@link BeanFactoryAware#setBeanFactory(BeanFactory)} on
	 * {@code @Configuration} class instances for the purpose of recording the {@link BeanFactory}.
	 * @see EnhancedConfiguration
	 */
	// 拦截对 @Configuration 类实例的任何 BeanFactoryAware.setBeanFactory(BeanFactory) 的调用，以记录 BeanFactory 。
	private static class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {

		@Override
		@Nullable
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
			Assert.state(field != null, "Unable to find generated BeanFactory field");
			field.set(obj, args[0]);

			// Does the actual (non-CGLIB) superclass implement BeanFactoryAware?
			// If so, call its setBeanFactory() method. If not, just exit.
			// 实际的（非 CGLIB）超类是否实现了 BeanFactoryAware？
			// 如果是，调用它的 setBeanFactory() 方法。如果没有，就退出。
			if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
				return proxy.invokeSuper(obj, args);
			}
			return null;
		}

		@Override
		public boolean isMatch(Method candidateMethod) {
			return isSetBeanFactory(candidateMethod);
		}

		public static boolean isSetBeanFactory(Method candidateMethod) {
			return (candidateMethod.getName().equals("setBeanFactory") &&
					candidateMethod.getParameterCount() == 1 &&
					BeanFactory.class == candidateMethod.getParameterTypes()[0] &&
					BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass()));
		}
	}

	/**
	 * Intercepts the invocation of any {@link Bean}-annotated methods in order to ensure proper
	 * handling of bean semantics such as scoping and AOP proxying.
	 * @see Bean
	 * @see ConfigurationClassEnhancer
	 */
	// 拦截任何 @Bean 注解方法的调用，以确保正确处理 bean 语义，例如范围和 AOP 代理
	private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {

		/**
		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
		 * existence of this bean object.
		 * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
		 * super implementation of the proxied method i.e., the actual {@code @Bean} method
		 */
		// 增强@Bean方法以检查提供的 BeanFactory 是否存在此 bean 对象。
		// 异常：Throwable - 作为调用代理方法的超级实现时可能抛出的任何异常的全部捕获，即实际的 @Bean 方法
		@Override
		@Nullable
		public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
					MethodProxy cglibMethodProxy) throws Throwable {

			ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

			// Determine whether this bean is a scoped-proxy
			// 确定此 bean 是否为作用域代理
			if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {
				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
				if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
					beanName = scopedBeanName;
				}
			}

			// To handle the case of an inter-bean method reference, we must explicitly check the
			// container for already cached instances.

			// First, check to see if the requested bean is a FactoryBean. If so, create a subclass
			// proxy that intercepts calls to getObject() and returns any cached bean instance.
			// This ensures that the semantics of calling a FactoryBean from within @Bean methods
			// is the same as that of referring to a FactoryBean within XML. See SPR-6602.
			//
			// 要处理 bean 间方法引用的情况，我们必须显式检查容器中是否存在已缓存的实例。
			//
			// 首先，检查请求的 bean 是否是 FactoryBean。如果是这样，创建一个子类代理来拦截对 getObject() 的调用
			// 并返回任何缓存的 bean 实例。这确保了从 @Bean 方法中调用 FactoryBean 的语义与在 XML 中引用
			// FactoryBean 的语义相同。见 SPR-6602
			if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&
					factoryContainsBean(beanFactory, beanName)) {
				Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
				if (factoryBean instanceof ScopedProxyFactoryBean) {
					// Scoped proxy factory beans are a special case and should not be further proxied
					// 作用域代理factory bean 是一种特殊情况，不应进一步代理
				}
				else {
					// It is a candidate FactoryBean - go ahead with enhancement
					// 它是一个候选 FactoryBean - 继续增强
					return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
				}
			}

			if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
				// The factory is calling the bean method in order to instantiate and register the bean
				// (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
				// create the bean instance.
				// 工厂正在调用 bean 方法以实例化和注册 bean（即通过 getBean() 调用）-> 调用该方法的超级实现以实际创建 bean 实例
				if (logger.isInfoEnabled() &&
						BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
					logger.info(String.format("@Bean method %s.%s is non-static and returns an object " +
									"assignable to Spring's BeanFactoryPostProcessor interface. This will " +
									"result in a failure to process annotations such as @Autowired, " +
									"@Resource and @PostConstruct within the method's declaring " +
									"@Configuration class. Add the 'static' modifier to this method to avoid " +
									"these container lifecycle issues; see @Bean javadoc for complete details.",
							beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
				}
				// CGLIB 调用目标方法
				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
			}

			return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
		}

		private Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs,
				ConfigurableBeanFactory beanFactory, String beanName) {

			// The user (i.e. not the factory) is requesting this bean through a call to
			// the bean method, direct or indirect. The bean may have already been marked
			// as 'in creation' in certain autowiring scenarios; if so, temporarily set
			// the in-creation status to false in order to avoid an exception.
			// 用户（即不是工厂）通过直接或间接调用 bean 方法来请求此 bean。在某些自动装配场景中，
			// bean 可能已经被标记为“创建中”；如果是这样，请暂时将创建状态设置为false以避免异常。
			boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
			try {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, false);
				}
				boolean useArgs = !ObjectUtils.isEmpty(beanMethodArgs);
				if (useArgs && beanFactory.isSingleton(beanName)) {
					// Stubbed null arguments just for reference purposes,
					// expecting them to be autowired for regular singleton references?
					// A safe assumption since @Bean singleton arguments cannot be optional...
					// 仅用于参考目的的存根空参数，期望它们为常规单例引用自动装配？一个安全的假设，因为 @Bean 单例参数不能是可选的......
					for (Object arg : beanMethodArgs) {
						if (arg == null) {
							useArgs = false;
							break;
						}
					}
				}
				Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) :
						beanFactory.getBean(beanName));
				if (!ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
					// Detect package-protected NullBean instance through equals(null) check
					// 通过 equals(null) 检查检测包保护的 NullBean 实例
					if (beanInstance.equals(null)) {
						if (logger.isDebugEnabled()) {
							logger.debug(String.format("@Bean method %s.%s called as bean reference " +
									"for type [%s] returned null bean; resolving to null value.",
									beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
									beanMethod.getReturnType().getName()));
						}
						beanInstance = null;
					}
					else {
						String msg = String.format("@Bean method %s.%s called as bean reference " +
								"for type [%s] but overridden by non-compatible bean instance of type [%s].",
								beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
								beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
						try {
							BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
							msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
						}
						catch (NoSuchBeanDefinitionException ex) {
							// Ignore - simply no detailed message then.
							// 忽略 - 只是没有详细的消息。
						}
						throw new IllegalStateException(msg);
					}
				}
				Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
				if (currentlyInvoked != null) {
					String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked);
					beanFactory.registerDependentBean(beanName, outerBeanName);
				}
				return beanInstance;
			}
			finally {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, true);
				}
			}
		}

		@Override
		public boolean isMatch(Method candidateMethod) {
			return (candidateMethod.getDeclaringClass() != Object.class &&
					!BeanFactoryAwareMethodInterceptor.isSetBeanFactory(candidateMethod) &&
					BeanAnnotationHelper.isBeanAnnotated(candidateMethod));
		}

		private ConfigurableBeanFactory getBeanFactory(Object enhancedConfigInstance) {
			Field field = ReflectionUtils.findField(enhancedConfigInstance.getClass(), BEAN_FACTORY_FIELD);
			Assert.state(field != null, "Unable to find generated bean factory field");
			Object beanFactory = ReflectionUtils.getField(field, enhancedConfigInstance);
			Assert.state(beanFactory != null, "BeanFactory has not been injected into @Configuration class");
			Assert.state(beanFactory instanceof ConfigurableBeanFactory,
					"Injected BeanFactory is not a ConfigurableBeanFactory");
			return (ConfigurableBeanFactory) beanFactory;
		}

		/**
		 * Check the BeanFactory to see whether the bean named <var>beanName</var> already
		 * exists. Accounts for the fact that the requested bean may be "in creation", i.e.:
		 * we're in the middle of servicing the initial request for this bean. From an enhanced
		 * factory method's perspective, this means that the bean does not actually yet exist,
		 * and that it is now our job to create it for the first time by executing the logic
		 * in the corresponding factory method.
		 * <p>Said another way, this check repurposes
		 * {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)} to determine whether
		 * the container is calling this method or the user is calling this method.
		 * @param beanName name of bean to check for
		 * @return whether <var>beanName</var> already exists in the factory
		 */
		// 检查 BeanFactory 以查看名为beanName的 bean 是否已存在。 考虑到请求的 bean 可能“正在创建”这一事实，
		// 即：我们正在为这个 bean 的初始请求提供服务。 从增强的工厂方法的角度来看，这意味着 bean 实际上还不存在，
		// 现在我们的工作是通过执行相应工厂方法中的逻辑来首次创建它。
		//
		// 换句话说，此检查重新利用ConfigurableBeanFactory.isCurrentlyInCreation(String)来确定是容器正在调用
		// 此方法还是用户正在调用此方法。
		//
		// 形参：
		//			beanName – 要检查的 bean 的名称
		// 返回值：
		//			beanName是否已经存在于工厂中
		private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory, String beanName) {
			return (beanFactory.containsBean(beanName) && !beanFactory.isCurrentlyInCreation(beanName));
		}

		/**
		 * Check whether the given method corresponds to the container's currently invoked
		 * factory method. Compares method name and parameter types only in order to work
		 * around a potential problem with covariant return types (currently only known
		 * to happen on Groovy classes).
		 */
		// 检查给定的方法是否对应于容器当前调用的工厂方法。 仅比较方法名称和参数类型，
		// 以解决协变返回类型的潜在问题（目前只知道发生在 Groovy 类上）。
		private boolean isCurrentlyInvokedFactoryMethod(Method method) {
			Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
			return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) &&
					Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
		}

		/**
		 * Create a subclass proxy that intercepts calls to getObject(), delegating to the current BeanFactory
		 * instead of creating a new instance. These proxies are created only when calling a FactoryBean from
		 * within a Bean method, allowing for proper scoping semantics even when working against the FactoryBean
		 * instance directly. If a FactoryBean instance is fetched through the container via &-dereferencing,
		 * it will not be proxied. This too is aligned with the way XML configuration works.
		 */
		// 创建一个子类代理来拦截对 getObject() 的调用，委托给当前的 BeanFactory 而不是创建一个新实例。
		// 这些代理仅在从 Bean 方法中调用 FactoryBean 时创建，即使在直接针对 FactoryBean 实例工作时也允许正确的范围语义。
		// 如果 FactoryBean 实例是通过 &-dereferencing 通过容器获取的，它将不会被代理。 这也符合 XML 配置的工作方式。
		private Object enhanceFactoryBean(final Object factoryBean, Class<?> exposedType,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			try {
				Class<?> clazz = factoryBean.getClass();
				boolean finalClass = Modifier.isFinal(clazz.getModifiers());
				boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
				if (finalClass || finalMethod) {
					if (exposedType.isInterface()) {
						if (logger.isTraceEnabled()) {
							logger.trace("Creating interface proxy for FactoryBean '" + beanName + "' of type [" +
									clazz.getName() + "] for use within another @Bean method because its " +
									(finalClass ? "implementation class" : "getObject() method") +
									" is final: Otherwise a getObject() call would not be routed to the factory.");
						}
						return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
					}
					else {
						if (logger.isDebugEnabled()) {
							logger.debug("Unable to proxy FactoryBean '" + beanName + "' of type [" +
									clazz.getName() + "] for use within another @Bean method because its " +
									(finalClass ? "implementation class" : "getObject() method") +
									" is final: A getObject() call will NOT be routed to the factory. " +
									"Consider declaring the return type as a FactoryBean interface.");
						}
						return factoryBean;
					}
				}
			}
			catch (NoSuchMethodException ex) {
				// No getObject() method -> shouldn't happen, but as long as nobody is trying to call it...
				// 没有 getObject() 方法 -> 不应该发生，但只要没有人试图调用它......
			}

			return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
		}

		private Object createInterfaceProxyForFactoryBean(final Object factoryBean, Class<?> interfaceType,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			return Proxy.newProxyInstance(
					factoryBean.getClass().getClassLoader(), new Class<?>[] {interfaceType},
					(proxy, method, args) -> {
						if (method.getName().equals("getObject") && args == null) {
							return beanFactory.getBean(beanName);
						}
						return ReflectionUtils.invokeMethod(method, factoryBean, args);
					});
		}

		private Object createCglibProxyForFactoryBean(final Object factoryBean,
				final ConfigurableBeanFactory beanFactory, final String beanName) {

			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(factoryBean.getClass());
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(MethodInterceptor.class);

			// Ideally create enhanced FactoryBean proxy without constructor side effects,
			// analogous to AOP proxy creation in ObjenesisCglibAopProxy...
			// 理想情况下创建增强的 FactoryBean 代理而没有构造函数的副作用，类似于 ObjenesisCglibAopProxy 中的 AOP 代理创建...
			Class<?> fbClass = enhancer.createClass();
			Object fbProxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " +
							"falling back to regular construction", ex);
				}
			}

			if (fbProxy == null) {
				try {
					fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " +
							"and regular FactoryBean instantiation via default constructor fails as well", ex);
				}
			}

			((Factory) fbProxy).setCallback(0, (MethodInterceptor) (obj, method, args, proxy) -> {
				if (method.getName().equals("getObject") && args.length == 0) {
					return beanFactory.getBean(beanName);
				}
				return proxy.invoke(factoryBean, args);
			});

			return fbProxy;
		}
	}

}
