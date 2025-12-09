package org.ssssssss.magicapi.spring.boot.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.impl.ApiInfoMagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceInfoMagicResourceStorage;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;
import org.ssssssss.magicapi.elasticsearch.model.MagicDynamicESClient;
import org.ssssssss.magicapi.elasticsearch.service.ESMagicDynamicRegistry;
import org.ssssssss.magicapi.elasticsearch.service.ESMagicResourceStorage;
import org.ssssssss.magicapi.function.service.FunctionInfoMagicResourceStorage;
import org.ssssssss.magicapi.function.service.FunctionMagicDynamicRegistry;
import org.ssssssss.magicapi.hbase.model.MagicDynamicHbaseClient;
import org.ssssssss.magicapi.hbase.service.HbaseMagicDynamicRegistry;
import org.ssssssss.magicapi.hbase.service.HbaseMagicResourceStorage;
import org.ssssssss.magicapi.kafka.model.MagicDynamicKafkaClient;
import org.ssssssss.magicapi.kafka.service.KafkaMagicDynamicRegistry;
import org.ssssssss.magicapi.kafka.service.KafkaMagicResourceStorage;
import org.ssssssss.magicapi.mqtt.model.MagicDynamicMqttClient;
import org.ssssssss.magicapi.mqtt.service.MqttMagicDynamicRegistry;
import org.ssssssss.magicapi.mqtt.service.MqttMagicResourceStorage;
import org.ssssssss.magicapi.redis.model.MagicDynamicRedisClient;
import org.ssssssss.magicapi.redis.service.RedisMagicDynamicRegistry;
import org.ssssssss.magicapi.redis.service.RedisMagicResourceStorage;
import org.ssssssss.magicapi.tcp.model.MagicDynamicTcpClient;
import org.ssssssss.magicapi.tcp.service.TcpMagicDynamicRegistry;
import org.ssssssss.magicapi.tcp.service.TcpMagicResourceStorage;
import org.ssssssss.magicapi.utils.Mapping;

@Configuration
@AutoConfigureAfter(MagicModuleConfiguration.class)
public class MagicDynamicRegistryConfiguration {


	private final MagicAPIProperties properties;

	@Autowired
	@Lazy
	private RequestMappingHandlerMapping requestMappingHandlerMapping;


	public MagicDynamicRegistryConfiguration(MagicAPIProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public ApiInfoMagicResourceStorage apiInfoMagicResourceStorage() {
		return new ApiInfoMagicResourceStorage(properties.getPrefix());
	}

	@Bean
	@ConditionalOnMissingBean
	public RequestMagicDynamicRegistry magicRequestMagicDynamicRegistry(ApiInfoMagicResourceStorage apiInfoMagicResourceStorage) throws NoSuchMethodException {
		return new RequestMagicDynamicRegistry(apiInfoMagicResourceStorage, Mapping.create(requestMappingHandlerMapping, properties.getWeb()), properties.isAllowOverride(), properties.getPrefix());
	}

	@Bean
	@ConditionalOnMissingBean
	public FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage() {
		return new FunctionInfoMagicResourceStorage();
	}

	@Bean
	@ConditionalOnMissingBean
	public FunctionMagicDynamicRegistry functionMagicDynamicRegistry(FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage) {
		return new FunctionMagicDynamicRegistry(functionInfoMagicResourceStorage);
	}

	@Bean
	@ConditionalOnMissingBean
	public DataSourceInfoMagicResourceStorage dataSourceInfoMagicResourceStorage() {
		return new DataSourceInfoMagicResourceStorage();
	}

	@Bean
	@ConditionalOnMissingBean
	public DataSourceMagicDynamicRegistry dataSourceMagicDynamicRegistry(DataSourceInfoMagicResourceStorage dataSourceInfoMagicResourceStorage, MagicDynamicDataSource magicDynamicDataSource) {
		return new DataSourceMagicDynamicRegistry(dataSourceInfoMagicResourceStorage, magicDynamicDataSource);
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RedisMagicResourceStorage redisMagicResourceStorage() {
		return new RedisMagicResourceStorage();
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicRedisClient magicDynamicRedisClient() {
		return new MagicDynamicRedisClient();
	}

	@Bean
	@ConditionalOnMissingBean
	public RedisMagicDynamicRegistry redisMagicDynamicRegistry(RedisMagicResourceStorage redisMagicResourceStorage,
			MagicDynamicRedisClient magicDynamicRedisClient) {
		return new RedisMagicDynamicRegistry(redisMagicResourceStorage, magicDynamicRedisClient);
	}
	
    @Bean
	@ConditionalOnMissingBean
	public MagicDynamicKafkaClient magicDynamicKafkaClient() {
		return new MagicDynamicKafkaClient();
	}
    
    @Bean
    @ConditionalOnMissingBean
    public KafkaMagicResourceStorage kafkaMagicResourceStorage() {
        return new KafkaMagicResourceStorage();
    }
    @Bean
    @ConditionalOnMissingBean
    public KafkaMagicDynamicRegistry kafkaMagicDynamicRegistry(KafkaMagicResourceStorage kafkaMagicResourceStorage , MagicDynamicKafkaClient magicDynamicKafkaClient) {
        return new KafkaMagicDynamicRegistry(kafkaMagicResourceStorage,magicDynamicKafkaClient);
    }
    
    @Bean
   	@ConditionalOnMissingBean
   	public MagicDynamicMqttClient magicDynamicMqttClient() {
   		return new MagicDynamicMqttClient();
   	}
       
   @Bean
   @ConditionalOnMissingBean
   public MqttMagicResourceStorage mqttMagicResourceStorage() {
       return new MqttMagicResourceStorage();
   }
   @Bean
   @ConditionalOnMissingBean
   public MqttMagicDynamicRegistry mqttMagicDynamicRegistry(MqttMagicResourceStorage mqttMagicResourceStorage , MagicDynamicMqttClient magicDynamicMqttClient) {
       return new MqttMagicDynamicRegistry(mqttMagicResourceStorage,magicDynamicMqttClient);
   }
   
   @Bean
   @ConditionalOnMissingBean
   public MagicDynamicESClient magicDynamicESClient() {
	   return new MagicDynamicESClient();
   }
   
   @Bean
   @ConditionalOnMissingBean
   public ESMagicResourceStorage esMagicResourceStorage() {
	   return new ESMagicResourceStorage();
   }
   @Bean
   @ConditionalOnMissingBean
   public ESMagicDynamicRegistry esMagicDynamicRegistry(ESMagicResourceStorage esMagicResourceStorage , MagicDynamicESClient magicDynamicESClient) {
	   return new ESMagicDynamicRegistry(esMagicResourceStorage,magicDynamicESClient);
   }
   @Bean
   @ConditionalOnMissingBean
   public MagicDynamicHbaseClient magicDynamicHbaseClient() {
	   return new MagicDynamicHbaseClient();
   }
   
   @Bean
   @ConditionalOnMissingBean
   public HbaseMagicResourceStorage hbaseMagicResourceStorage() {
	   return new HbaseMagicResourceStorage();
   }
   @Bean
   @ConditionalOnMissingBean
   public HbaseMagicDynamicRegistry hbaseMagicDynamicRegistry(HbaseMagicResourceStorage hbaseMagicResourceStorage , MagicDynamicHbaseClient magicDynamicHbaseClient) {
	   return new HbaseMagicDynamicRegistry(hbaseMagicResourceStorage,magicDynamicHbaseClient);
   }
   
   @Bean
   @ConditionalOnMissingBean
   public MagicDynamicTcpClient magicDynamicTcpClient() {
	   return new MagicDynamicTcpClient();
   }
   
   @Bean
   @ConditionalOnMissingBean
   public TcpMagicResourceStorage tcpMagicResourceStorage() {
	   return new TcpMagicResourceStorage();
   }
   @Bean
   @ConditionalOnMissingBean
   public TcpMagicDynamicRegistry tcpMagicDynamicRegistry(TcpMagicResourceStorage tcpMagicResourceStorage , MagicDynamicTcpClient magicDynamicTcpClient) {
	   return new TcpMagicDynamicRegistry(tcpMagicResourceStorage,magicDynamicTcpClient);
   }
}
