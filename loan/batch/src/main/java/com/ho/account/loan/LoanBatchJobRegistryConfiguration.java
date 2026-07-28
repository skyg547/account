package com.ho.account.loan;

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

/**
 * Spring Batch Job 등록 시점을 모든 singleton 생성 이후로 늦추는 인프라 설정.
 *
 * <p>초보자 관점에서는 "Job 목록을 너무 일찍 등록하려다 나는 경고"를 막는 장치입니다.
 * 대출 이자 계산이나 전표 생성 같은 업무 로직은 넣지 않고, Batch 컨테이너 초기화 순서만 조정합니다.
 */
@Configuration(proxyBeanMethods = false)
class LoanBatchJobRegistryConfiguration {

    private static final String JOB_REGISTRY_BEAN_POST_PROCESSOR = "jobRegistryBeanPostProcessor";

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static BeanDefinitionRegistryPostProcessor loanBatchJobRegistryBeanPostProcessorRemoval() {
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
    JobRegistrySmartInitializingSingleton loanJobRegistrySmartInitializingSingleton(JobRegistry jobRegistry) {
        return new JobRegistrySmartInitializingSingleton(jobRegistry);
    }
}