package com.ho.account.tax.batch;

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
 * Spring Batch Job 등록 시점을 모든 singleton 생성 이후로 늦추는 인프라 설정입니다.
 *
 * 초보자용 설명:
 * Batch 앱이 뜰 때 Job 목록을 너무 일찍 등록하면 DataSource/JPA 같은 빈이 아직 준비되기 전이라 경고가 많이 발생합니다.
 * 이 설정은 그 등록 순서만 조정하며, 세금계산서 금액 검증이나 거래처 검증 같은 업무 규칙은 core 서비스에 그대로 둡니다.
 */
@Configuration(proxyBeanMethods = false)
class TaxBatchJobRegistryConfiguration {

    private static final String JOB_REGISTRY_BEAN_POST_PROCESSOR = "jobRegistryBeanPostProcessor";

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static BeanDefinitionRegistryPostProcessor taxBatchJobRegistryBeanPostProcessorRemoval() {
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
    JobRegistrySmartInitializingSingleton taxJobRegistrySmartInitializingSingleton(JobRegistry jobRegistry) {
        return new JobRegistrySmartInitializingSingleton(jobRegistry);
    }
}