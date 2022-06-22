/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * Bean definition reader for a simple properties format.
 *
 * <p>Provides bean definition registration methods for Map/Properties and
 * ResourceBundle. Typically applied to a DefaultListableBeanFactory.
 *
 * <p><b>Example:</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // bean is of class MyClass
 * employee.(abstract)=true       // this bean can't be instantiated directly
 * employee.group=Insurance       // real property
 * employee.usesDialUp=false      // real property (potentially overridden)
 *
 * salesrep.(parent)=employee     // derives from "employee" bean definition
 * salesrep.(lazy-init)=true      // lazily initialize this singleton bean
 * salesrep.manager(ref)=tony     // reference to another bean
 * salesrep.department=Sales      // real property
 *
 * techie.(parent)=employee       // derives from "employee" bean definition
 * techie.(scope)=prototype       // bean is a prototype (not a shared instance)
 * techie.manager(ref)=jeff       // reference to another bean
 * techie.department=Engineering  // real property
 * techie.usesDialUp=true         // real property (overriding parent value)
 *
 * ceo.$0(ref)=secretary          // inject 'secretary' bean as 0th constructor arg
 * ceo.$1=1000000                 // inject value '1000000' at 1st constructor arg
 * </pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 26.11.2003
 * @see DefaultListableBeanFactory
 * @deprecated as of 5.3, in favor of Spring's common bean definition formats
 * and/or custom reader implementations
 */
@Deprecated
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	// 代表 true 的 T/F 属性的值。其他任何东西都代表虚假。区分大小写。
	public static final String TRUE_VALUE = "true";

	/**
	 * Separator between bean name and property name.
	 * We follow normal Java conventions.
	 */
	// bean 名称和属性名称之间的分隔符。我们遵循正常的 Java 约定
	public static final String SEPARATOR = ".";

	/**
	 * Special key to distinguish {@code owner.(class)=com.myapp.MyClass}.
	 */
	// 区分 {@code owner.(class)=com.myapp.MyClass} 的特殊键
	public static final String CLASS_KEY = "(class)";

	/**
	 * Special key to distinguish {@code owner.(parent)=parentBeanName}.
	 */
	// 区分 {@code owner.(parent)=parentBeanName} 的特殊键
	public static final String PARENT_KEY = "(parent)";

	/**
	 * Special key to distinguish {@code owner.(scope)=prototype}.
	 * Default is "true".
	 */
	// 区分 {@code owner.(scope)=prototype} 的特殊键。默认值为“真”
	public static final String SCOPE_KEY = "(scope)";

	/**
	 * Special key to distinguish {@code owner.(singleton)=false}.
	 * Default is "true".
	 */
	// 区分 {@code owner.(singleton)=false} 的特殊键。默认值为“真”。
	public static final String SINGLETON_KEY = "(singleton)";

	/**
	 * Special key to distinguish {@code owner.(abstract)=true}
	 * Default is "false".
	 */
	// 区分{@code owner.(abstract)=true} 的特殊键默认为“false”。
	public static final String ABSTRACT_KEY = "(abstract)";

	/**
	 * Special key to distinguish {@code owner.(lazy-init)=true}
	 * Default is "false".
	 */
	// 区分{@code owner.(lazy-init)=true} 的特殊键默认为“false”
	public static final String LAZY_INIT_KEY = "(lazy-init)";

	/**
	 * Property suffix for references to other beans in the current
	 * BeanFactory: e.g. {@code owner.dog(ref)=fido}.
	 * Whether this is a reference to a singleton or a prototype
	 * will depend on the definition of the target bean.
	 */
	// 引用当前 BeanFactory 中其他 bean 的属性后缀：例如{@code owner.dog(ref)=fido}。
	// 这是对单例或原型的引用将取决于目标 bean 的定义。
	public static final String REF_SUFFIX = "(ref)";

	/**
	 * Prefix before values referencing other beans.
	 */
	// 引用其他 bean 的值前的前缀。
	public static final String REF_PREFIX = "*";

	/**
	 * Prefix used to denote a constructor argument definition.
	 */
	// 用于表示构造函数参数定义的前缀
	public static final String CONSTRUCTOR_ARG_PREFIX = "$";


	@Nullable
	private String defaultParentBean;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;


	/**
	 * Create new PropertiesBeanDefinitionReader for the given bean factory.
	 * @param registry the BeanFactory to load bean definitions into,
	 * in the form of a BeanDefinitionRegistry
	 */
	// 为给定的 bean 工厂创建新的 PropertiesBeanDefinitionReader。
	// @param registry 以 BeanDefinitionRegistry 的形式加载 bean 定义的 BeanFactory
	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * Set the default parent bean for this bean factory.
	 * If a child bean definition handled by this factory provides neither
	 * a parent nor a class attribute, this default value gets used.
	 * <p>Can be used e.g. for view definition files, to define a parent
	 * with a default view class and common attributes for all views.
	 * View definitions that define their own parent or carry their own
	 * class can still override this.
	 * <p>Strictly speaking, the rule that a default parent setting does
	 * not apply to a bean definition that carries a class is there for
	 * backwards compatibility reasons. It still matches the typical use case.
	 */
	// 为这个 bean 工厂设置默认的父 bean。如果此工厂处理的子 bean 定义既不提供父属性也不提供类属性，则使用此默认值。
	// <p>可以使用例如对于视图定义文件，为所有视图定义具有默认视图类和通用属性的父级。
	// 定义自己的父级或携带自己的类的视图定义仍然可以覆盖它。
	// <p>严格来说，默认父设置不适用于带有类的 bean 定义的规则是出于向后兼容的原因。它仍然与典型用例相匹配。
	public void setDefaultParentBean(@Nullable String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}

	/**
	 * Return the default parent bean for this bean factory.
	 */
	// 返回此 bean 工厂的默认父 bean
	@Nullable
	public String getDefaultParentBean() {
		return this.defaultParentBean;
	}

	/**
	 * Set the PropertiesPersister to use for parsing properties files.
	 * The default is ResourcePropertiesPersister.
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	// 设置 PropertiesPersister 以用于解析属性文件。默认值为 ResourcePropertiesPersister。
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * Return the PropertiesPersister to use for parsing properties files.
	 */
	// 返回 PropertiesPersister 以用于解析属性文件
	public PropertiesPersister getPropertiesPersister() {
		return this.propertiesPersister;
	}


	/**
	 * Load bean definitions from the specified properties file,
	 * using all property keys (i.e. not filtering by prefix).
	 * @param resource the resource descriptor for the properties file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource, String)
	 */
	// 使用所有属性键（即不按前缀过滤）从指定的属性文件加载 bean 定义
	// @param resource 属性文件的资源描述符
	// @return 找到的 bean 定义数
	// @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), null);
	}

	/**
	 * Load bean definitions from the specified properties file.
	 * @param resource the resource descriptor for the properties file
	 * @param prefix a filter within the keys in the map: e.g. 'beans.'
	 * (can be empty or {@code null})
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 从指定的属性文件加载 bean 定义
	// @param resource 属性文件的资源描述符
	// @param 在地图中的键中为过滤器添加前缀：例如'beans.' （可以为空或 {@code null}）
	public int loadBeanDefinitions(Resource resource, @Nullable String prefix) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), prefix);
	}

	/**
	 * Load bean definitions from the specified properties file.
	 * @param encodedResource the resource descriptor for the properties file,
	 * allowing to specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 从指定的属性文件加载 bean 定义。
	// @param encodingResource 属性文件的资源描述符，允许指定用于解析文件的编码
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(encodedResource, null);
	}

	/**
	 * Load bean definitions from the specified properties file.
	 * @param encodedResource the resource descriptor for the properties file,
	 * allowing to specify an encoding to use for parsing the file
	 * @param prefix a filter within the keys in the map: e.g. 'beans.'
	 * (can be empty or {@code null})
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource, @Nullable String prefix)
			throws BeanDefinitionStoreException {

		if (logger.isTraceEnabled()) {
			logger.trace("Loading properties bean definitions from " + encodedResource);
		}

		Properties props = new Properties();
		try {
			try (InputStream is = encodedResource.getResource().getInputStream()) {
				if (encodedResource.getEncoding() != null) {
					getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
				}
				else {
					getPropertiesPersister().load(props, is);
				}
			}

			// 注册 beanDefinitions
			int count = registerBeanDefinitions(props, prefix, encodedResource.getResource().getDescription());
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
			}
			return count;
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
		}
	}

	/**
	 * Register bean definitions contained in a resource bundle,
	 * using all property keys (i.e. not filtering by prefix).
	 * @param rb the ResourceBundle to load from
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #registerBeanDefinitions(java.util.ResourceBundle, String)
	 */
	// 使用所有属性键（即不按前缀过滤）注册包含在资源包中的 bean 定义
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}

	/**
	 * Register bean definitions contained in a ResourceBundle.
	 * <p>Similar syntax as for a Map. This method is useful to enable
	 * standard Java internationalization support.
	 * @param rb the ResourceBundle to load from
	 * @param prefix a filter within the keys in the map: e.g. 'beans.'
	 * (can be empty or {@code null})
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 注册包含在 ResourceBundle 中的 bean 定义。
	// <p>类似于 Map 的语法。此方法对于启用标准 Java 国际化支持很有用
	public int registerBeanDefinitions(ResourceBundle rb, @Nullable String prefix) throws BeanDefinitionStoreException {
		// Simply create a map and call overloaded method.
		// 只需创建一个map并调用重载方法。
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(map, prefix);
	}


	/**
	 * Register bean definitions contained in a Map, using all property keys (i.e. not
	 * filtering by prefix).
	 * @param map a map of {@code name} to {@code property} (String or Object). Property
	 * values will be strings if coming from a Properties file etc. Property names
	 * (keys) <b>must</b> be Strings. Class keys must be Strings.
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 * @see #registerBeanDefinitions(java.util.Map, String, String)
	 */
	// 使用所有属性键（即不按前缀过滤）注册包含在 Map 中的 bean 定义
	public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
		return registerBeanDefinitions(map, null);
	}

	/**
	 * Register bean definitions contained in a Map.
	 * Ignore ineligible properties.
	 * @param map a map of {@code name} to {@code property} (String or Object). Property
	 * values will be strings if coming from a Properties file etc. Property names
	 * (keys) <b>must</b> be Strings. Class keys must be Strings.
	 * @param prefix a filter within the keys in the map: e.g. 'beans.'
	 * (can be empty or {@code null})
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 */
	// 注册包含在 Map 中的 bean 定义。忽略不合格的属性
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix) throws BeansException {
		return registerBeanDefinitions(map, prefix, "Map " + map);
	}

	/**
	 * Register bean definitions contained in a Map.
	 * Ignore ineligible properties.
	 * @param map a map of {@code name} to {@code property} (String or Object). Property
	 * values will be strings if coming from a Properties file etc. Property names
	 * (keys) <b>must</b> be Strings. Class keys must be Strings.
	 * @param prefix a filter within the keys in the map: e.g. 'beans.'
	 * (can be empty or {@code null})
	 * @param resourceDescription description of the resource that the
	 * Map came from (for logging purposes)
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 * @see #registerBeanDefinitions(Map, String)
	 */
	// 注册包含在 Map 中的 bean 定义。忽略不合格的属性。 Properties 本身继承 HashTable,HashTable 实现 Map 接口
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription)
			throws BeansException {

		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		// map 中包含了 user.properties 中定义的相关信息
		for (Object key : map.keySet()) {
			// 规定 map 中只能存储 String 类型
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
			}
			// keyString 就是properties 中的 key
			String keyString = (String) key;
			if (keyString.startsWith(prefix)) {
				// Key is of form: prefix<name>.property
				// 键的格式为：prefix<name>.property 比如 user. 作为前缀
				String nameAndProperty = keyString.substring(prefix.length());
				// Find dot before property name, ignoring dots in property keys.
				// 在属性名称之前查找点，忽略属性键中的点
				int sepIdx ;
				int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
				if (propKeyIdx != -1) {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
				}
				else {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
				}
				if (sepIdx != -1) {
					// beanName 类名的第一个字符变成小写
					String beanName = nameAndProperty.substring(0, sepIdx);
					if (logger.isTraceEnabled()) {
						logger.trace("Found bean name '" + beanName + "'");
					}
					if (!getRegistry().containsBeanDefinition(beanName)) {
						// If we haven't already registered it...
						// 如果我们还没有注册...
						registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
						++beanCount;
					}
				}
				else {
					// Ignore it: It wasn't a valid bean name and property,
					// although it did start with the required prefix.
					// 忽略它：它不是有效的 bean 名称和属性，尽管它确实以所需的前缀开头。
					if (logger.isDebugEnabled()) {
						logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
					}
				}
			}
		}

		return beanCount;
	}

	/**
	 * Get all property values, given a prefix (which will be stripped)
	 * and add the bean they define to the factory with the given name.
	 * @param beanName name of the bean to define
	 * @param map a Map containing string pairs
	 * @param prefix prefix of each entry, which will be stripped
	 * @param resourceDescription description of the resource that the
	 * Map came from (for logging purposes)
	 * @throws BeansException if the bean definition could not be parsed or registered
	 */
	// 获取所有属性值，给定一个前缀（将被剥离）并将它们定义的 bean 添加到具有给定名称的工厂中。
	protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription)
			throws BeansException {

		String className = null;
		String parent = null;
		String scope = BeanDefinition.SCOPE_SINGLETON;
		boolean isAbstract = false;
		boolean lazyInit = false;

		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		MutablePropertyValues pvs = new MutablePropertyValues();

		String prefixWithSep = prefix + SEPARATOR;
		int beginIndex = prefixWithSep.length();

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = StringUtils.trimWhitespace((String) entry.getKey());
			if (key.startsWith(prefixWithSep)) {
				String property = key.substring(beginIndex);
				if (CLASS_KEY.equals(property)) {
					className = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (PARENT_KEY.equals(property)) {
					parent = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (ABSTRACT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					isAbstract = TRUE_VALUE.equals(val);
				}
				else if (SCOPE_KEY.equals(property)) {
					// Spring 2.0 style
					scope = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (SINGLETON_KEY.equals(property)) {
					// Spring 1.2 style
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					scope = (!StringUtils.hasLength(val) || TRUE_VALUE.equals(val) ?
							BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
				}
				else if (LAZY_INIT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					lazyInit = TRUE_VALUE.equals(val);
				}
				else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
					if (property.endsWith(REF_SUFFIX)) {
						int index = Integer.parseInt(property.substring(1, property.length() - REF_SUFFIX.length()));
						cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
					}
					else {
						int index = Integer.parseInt(property.substring(1));
						cas.addIndexedArgumentValue(index, readValue(entry));
					}
				}
				else if (property.endsWith(REF_SUFFIX)) {
					// This isn't a real property, but a reference to another prototype
					// Extract property name: property is of form dog(ref)
					// 这不是真正的属性，而是对另一个原型的引用 Extract property name: property is of form dog(ref)
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					String ref = StringUtils.trimWhitespace((String) entry.getValue());

					// It doesn't matter if the referenced bean hasn't yet been registered:
					// this will ensure that the reference is resolved at runtime.
					// 引用的 bean 是否尚未注册并不重要：这将确保在运行时解析引用
					Object val = new RuntimeBeanReference(ref);
					pvs.add(property, val);
				}
				else {
					// It's a normal bean property.
					// 这是一个普通的 bean 属性
					pvs.add(property, readValue(entry));
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Registering bean definition for bean name '" + beanName + "' with " + pvs);
		}

		// Just use default parent if we're not dealing with the parent itself,
		// and if there's no class name specified. The latter has to happen for
		// backwards compatibility reasons.
		// 如果我们不处理父级本身，并且没有指定类名，只需使用默认父级。出于向后兼容性的原因，后者必须发生
		if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
			parent = this.defaultParentBean;
		}

		try {
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					parent, className, getBeanClassLoader());
			bd.setScope(scope);
			bd.setAbstract(isAbstract);
			bd.setLazyInit(lazyInit);
			bd.setConstructorArgumentValues(cas);
			bd.setPropertyValues(pvs);
			getRegistry().registerBeanDefinition(beanName, bd);
		}
		catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, ex);
		}
		catch (LinkageError err) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, err);
		}
	}

	/**
	 * Reads the value of the entry. Correctly interprets bean references for
	 * values that are prefixed with an asterisk.
	 */
	// 读取条目的值。正确解释以星号为前缀的值的 bean 引用
	private Object readValue(Map.Entry<?, ?> entry) {
		Object val = entry.getValue();
		if (val instanceof String) {
			String strVal = (String) val;
			// If it starts with a reference prefix...
			// 如果它以引用前缀开头
			if (strVal.startsWith(REF_PREFIX)) {
				// Expand the reference.
				String targetName = strVal.substring(1);
				if (targetName.startsWith(REF_PREFIX)) {
					// Escaped prefix -> use plain value.
					val = targetName;
				}
				else {
					val = new RuntimeBeanReference(targetName);
				}
			}
		}
		return val;
	}

}
