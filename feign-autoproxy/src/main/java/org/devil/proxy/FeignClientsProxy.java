package org.devil.proxy;

import org.devil.proxy.annotation.EnableAutoProxyFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yaojun
 * 2020/5/8 14:47
 */
public class FeignClientsProxy implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {

    private final Logger logger = LoggerFactory.getLogger(FeignClientsProxy.class);

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,@NonNull BeanDefinitionRegistry registry) {
        registerProxy(importingClassMetadata, registry);
    }

    private void registerProxy(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry){
        Map<String,Object> attribute = importingClassMetadata.getAnnotationAttributes(EnableAutoProxyFeign.class.getName(),true);

        Boolean isEnable = (Boolean) attribute.getOrDefault("enable",true);
        if (!isEnable){
            return;
        }

        String[] clients = (String[])attribute.get("clients");

        String[] proxyClients;
        if (clients == null || clients.length == 0){
            Set<String> basepackages = getBasePackage(importingClassMetadata);
            if (logger.isDebugEnabled()){
                logger.debug("find base packages {}",basepackages);
            }
            proxyClients = searchClients(basepackages);
        }else {
            proxyClients = clients;
        }

        if (proxyClients.length > 0) {
            AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(FeignClientBeanPostProcess.class);
            definition.setBeanClassName(FeignClientBeanPostProcess.class.getName());
            definition.setBeanClass(FeignClientBeanPostProcess.class);
            ConstructorArgumentValues values = new ConstructorArgumentValues();
            values.addIndexedArgumentValue(0, proxyClients);
            definition.setConstructorArgumentValues(values);

            registry.registerBeanDefinition("feignClientBeanPostProcess", definition);
        }

    }

    protected Set<String> getBasePackage(AnnotationMetadata metadata){
        Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableAutoProxyFeign.class.getName(),true);
        Set<String> basePackages = new HashSet<>();

        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        for (String clazz : (String[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(metadata.getClassName()));
        }

        return basePackages;
    }

    private String[] searchClients(Set<String> basePackages){
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));

        Set<String> clients = new HashSet<>();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                if (beanDefinition instanceof AnnotatedBeanDefinition){
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition)beanDefinition;
                    AnnotationMetadata annotationMetadata = annotatedBeanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");
                    clients.add(beanDefinition.getBeanClassName());
                }
            }
        }
        if (logger.isDebugEnabled()){
            logger.debug("find client to proxy {}",clients);
        }
        return clients.toArray(new String[0]);
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }
}
