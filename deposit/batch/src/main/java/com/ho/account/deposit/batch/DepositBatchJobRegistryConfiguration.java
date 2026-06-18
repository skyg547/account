package com.ho.account.deposit.batch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration(proxyBeanMethods = false)
class DepositBatchJobRegistryConfiguration {

    private static final String JOB_REGISTRY_BEAN_POST_PROCESSOR = "jobRegistryBeanPostProcessor";

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static BeanDefinitionRegistryPostProcessor depositBatchJobRegistryBeanPostProcessorRemoval() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                if (registry.containsBeanDefinition(JOB_REGISTRY_BEAN_POST_PROCESSOR)) {
                    registry.removeBeanDefinition(JOB_REGISTRY_BEAN_POST_PROCESSOR);
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            }
        };
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    JobRegistrySmartInitializingSingleton depositJobRegistrySmartInitializingSingleton(JobRegistry jobRegistry) {
        return new JobRegistrySmartInitializingSingleton(jobRegistry);
    }
}
