package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
public class SpringBatchConfig {
    private static final Logger logger = LoggerFactory.getLogger(SpringBatchConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    public SpringBatchConfig(
        final JobRepository jobRepository,
        final PlatformTransactionManager transactionManager,
        final DataSource dataSource
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
    }

    @Bean
    public Job downloadCsvFileJob(Step downloadCsvFileStep) {
        return new JobBuilder("downloadCsvFileJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(downloadCsvFileStep)
            .build();
    }

    @Bean
    public Step downloadCsvFileStep(Tasklet downloadCsvFileTasklet) {
        return new StepBuilder("downloadCsvFileStep", jobRepository)
            .tasklet(downloadCsvFileTasklet, transactionManager)
            .build();
    }

    @Bean
    @StepScope
    public Tasklet downloadCsvFileTasklet(
        @Value("#{jobParameters['sourceFileUrl']}") String sourceFileUrl,
        @Value("#{jobParameters['targetFilePath']}") String targetFilePath
    ) throws MalformedURLException {
        return new DownloadCsvFileTasklet(new URL(sourceFileUrl), Paths.get(targetFilePath));
    }

    @Bean
    public Job loadCsvToDatabaseJob(Step loadCsvToDatabaseStep) {
        return new JobBuilder("loadCsvToDatabaseJob", jobRepository)
            .start(loadCsvToDatabaseStep)
            .build();
    }

    @Bean
    public Step loadCsvToDatabaseStep(
        ItemReader<PersonCsv> reader,
        ItemProcessor<PersonCsv, PersonDb> processor,
        ItemWriter<PersonDb> writer
    ) {
        return new StepBuilder("loadCsvToDatabaseStep", jobRepository)
            .<PersonCsv, PersonDb>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PersonCsv> reader(
        @Value("#{jobParameters['targetFilePath']}") FileSystemResource fileSystemResource
    ) {
        return new FlatFileItemReaderBuilder<PersonCsv>()
            .name("personItemReader")
            .resource(fileSystemResource)
            .linesToSkip(1)
            .delimited()
            .names("person_ID", "name", "first", "last", "middle", "email", "phone", "fax", "title")
            .targetType(PersonCsv.class)
            .build();
    }

    @Bean
    public ItemProcessor<PersonCsv, PersonDb> processor() {
        return personCsv -> {
            if (personCsv.title().contains("Professor")) {
                return null;
            }
            return new PersonDb(personCsv.first(), personCsv.last());
        };
    }

    @Bean
    public JdbcBatchItemWriter<PersonDb> writer() {
        return new JdbcBatchItemWriterBuilder<PersonDb>()
            .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
            .dataSource(dataSource)
            .beanMapped()
            .build();
    }

    private static class DownloadCsvFileTasklet implements Tasklet {
        private final URL url;
        private final Path path;

        public DownloadCsvFileTasklet(final URL url, final Path path) {
            this.url = url;
            this.path = path;
        }

        @Override
        public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
            downloadCsvFile(url, path);
            return RepeatStatus.FINISHED;
        }

        private static void downloadCsvFile(final URL url, final Path path) {
            try (InputStream in = url.openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                logger.info("File '{}' has been downloaded from '{}'", path, url);
            } catch (IOException e) {
                logger.error("Failed to get csv file", e);
            }
        }
    }

}