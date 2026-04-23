package com.ho.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * 留덉뒪???곗씠??愿由?留덉씠?щ줈?쒕퉬??(Master Data Service)
 * 怨꾩젙怨쇰ぉ, 嫄곕옒泥? 遺?????쒖뒪???꾨컲?먯꽌 ?ъ슜?섎뒗 湲곗? ?뺣낫瑜?愿由ы빀?덈떎.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients // ?ㅻⅨ 留덉씠?щ줈?쒕퉬?ㅼ? ?곗븘?섍쾶 ?꾪솕(API ?몄텧)?????덈뒗 湲곕뒫??耳?땲??
@EnableJpaRepositories(basePackages = "com.ho.account")
@EntityScan(basePackages = "com.ho.account")
public class MasterDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterDataApplication.class, args);
    }
}
