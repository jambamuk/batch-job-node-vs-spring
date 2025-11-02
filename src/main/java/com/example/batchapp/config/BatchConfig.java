package com.example.batchapp.config;

import com.example.batchapp.listener.JobCompletionNotificationListener;
import com.example.batchapp.model.Person;
import com.example.batchapp.processor.PersonItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import org.springframework.batch.core.launch.support.RunIdIncrementer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@Configuration
public class BatchConfig {

    @Value("${batch.chunk.size:2000}")
    private int chunkSize;

    @Value("${batch.threads.count:10}")
    private int threadsCount;

    @Value("${batch.virtual.threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Autowired
    private DataSource dataSource;

    @Bean
    public Job personJob(JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
        return new JobBuilder("personJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager, ItemWriter<Person> writer, JdbcPagingItemReader<Person> reader, PersonItemProcessor processor) {
        return new StepBuilder("step1", jobRepository)
                .<Person, Person>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<Person> reader() throws Exception {
        JdbcPagingItemReader<Person> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(chunkSize);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Person.class));
        reader.setQueryProvider(queryProvider());
        return reader;
    }

    @Bean
    public PagingQueryProvider queryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource);
        factory.setSelectClause("id, first_name, last_name, email, age, address, job_title, created_at, updated_at");
        factory.setFromClause("from person");
        factory.setSortKey("id");
        factory.setDatabaseType("POSTGRES");
        return factory.getObject();
    }

    @Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }

    @Bean
    public ItemWriter<Person> writer() {
        return items -> {
            System.out.println("Processing " + items.size() + " items on thread " + Thread.currentThread().getName());
            for (Person item : items) {
                // System.out.println(item.toString());
            }
        };
    }

    @Bean
    public TaskExecutor taskExecutor() {
        if (virtualThreadsEnabled) {
            SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
            asyncTaskExecutor.setVirtualThreads(true);
            return asyncTaskExecutor;
        } else {
            ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
            taskExecutor.setCorePoolSize(threadsCount);
            taskExecutor.setMaxPoolSize(threadsCount);
            taskExecutor.setQueueCapacity(threadsCount);
            return taskExecutor;
        }
    }
}
