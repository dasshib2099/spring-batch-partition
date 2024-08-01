package com.shib.batch.config;

import com.shib.batch.entity.User;
import com.shib.batch.processor.ColumnRangePartitioner;
import com.shib.batch.processor.UserProcessor;
import com.shib.batch.repository.UserRepository;
import com.shib.batch.writer.UserWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final UserWriter userWriter;

    @Bean
    public FlatFileItemReader<User> itemReader(){

        FlatFileItemReader<User> itemReader  = new FlatFileItemReader<User>();
        itemReader.setResource( new FileSystemResource("src/main/resources/users.csv"));
        itemReader.setName("usersReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    @Bean
    public UserProcessor processor(){
        return new UserProcessor();
    }

//    @Bean
//    public RepositoryItemWriter<User> writer(){
//        RepositoryItemWriter<User> writer = new RepositoryItemWriter<>();
//        writer.setRepository(userRepository);
//        writer.setMethodName("save");
//        return  writer;
//    }

    @Bean
    public ColumnRangePartitioner partitioner(){
        return new ColumnRangePartitioner();
    }

    @Bean
    public PartitionHandler partitionHandler(){
        TaskExecutorPartitionHandler taskExecutorPartitionHandler = new TaskExecutorPartitionHandler();
        taskExecutorPartitionHandler.setGridSize(2);
        taskExecutorPartitionHandler.setTaskExecutor(taskExecutor());
        taskExecutorPartitionHandler.setStep(slaveStep());
        return taskExecutorPartitionHandler;
    }

    @Bean
    public Step slaveStep(){
        return new StepBuilder("slaveStep", jobRepository)
                .<User, User>chunk(500, platformTransactionManager)
                .reader(itemReader())
                .processor(processor())
                .writer(userWriter)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStep(){
        return new StepBuilder("masterStep", jobRepository)
                .partitioner(slaveStep().getName(), partitioner())
                .partitionHandler(partitionHandler())
                .build();
    }



//    @Bean
//    public Step importStep(){
//        return new StepBuilder("csv-step", jobRepository)
//                .<User, User>chunk(10, platformTransactionManager)
//                .reader(itemReader())
//                .processor(processor())
//                .writer(writer())
//                .taskExecutor(taskExecutor())
//                .build();
//    }

//    @Bean
//    public Job runJob(){
//        return new JobBuilder("importUsers", jobRepository)
//                .start(importStep())
//                .build();
//    }

    @Bean
    public Job runJob(){
        return new JobBuilder("importUsers", jobRepository)
                .start(masterStep())
                .build();
    }


//    @Bean
//    public TaskExecutor taskExecutor(){
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//        asyncTaskExecutor.setConcurrencyLimit(10);
//        return asyncTaskExecutor;
//    }

    @Bean
    public TaskExecutor taskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(4);
        executor.setCorePoolSize(4);
        executor.setQueueCapacity(4);
        return executor;
    }

    private LineMapper<User> lineMapper(){

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(new String[] {"firstName","lastName","emailId","mobileNumber","country","state","city","address","postalCode"});
        //lineTokenizer.setIncludedFields(2,3,4,5,6,7,8,9);
        BeanWrapperFieldSetMapper<User> fieldSetMapper = new BeanWrapperFieldSetMapper<User>();
        fieldSetMapper.setTargetType(User.class);

        DefaultLineMapper<User> lineMapper = new DefaultLineMapper<User>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return lineMapper;
    }
}
