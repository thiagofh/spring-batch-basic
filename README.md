# spring-batch-basic

Descrição simplificada dos componentes:

- Spring Job Launcher: Inicia e executa jobs em lote.
- Job Repository: Armazena metadados sobre jobs em lote, permitindo a capacidade de reiniciar jobs.
- Step: Representa uma unidade de trabalho dentro de um job em lote.

- Step Orientado a Chunk: Processa dados em pedaços (chunks).
- Step de Tasklet: Executa uma única tarefa.
- Reader: Recupera dados de uma fonte, como um arquivo ou banco de dados.
- Processor: Transforma ou processa os dados de entrada.
- Writer: Escreve os dados processados em um destino, como um arquivo ou banco de dados.

Este código é uma configuração Spring Batch para um processo de ETL (Extract, Transform, Load) que envolve a leitura de um arquivo CSV, o download desse arquivo da internet, o processamento dos dados e a carga desses dados em um banco de dados.

**Vamos explicar o código passo a passo:**

1. **Configuração Principal (`com.example.SpringBatchConfig`):**
    - A classe é anotada com `@Configuration`, indicando que é uma classe de configuração do Spring.
    - Possui três campos privados: `jobRepository`, `transactionManager` e `dataSource`. Esses campos são injetados no construtor da classe.
    - O construtor inicializa esses campos com os valores fornecidos ao instanciar a classe.
    - Existem métodos `@Bean` que configuram diferentes partes do processo Spring Batch.

2. **Configuração do Job de Download do Arquivo CSV (`downloadCsvFileJob`):**
    - Cria um objeto `Job` que representa o trabalho de download do arquivo CSV.
    - Utiliza um `JobBuilder` para configurar o nome do job, o repositório de jobs e um incrementador automático de IDs.
    - Esse job possui apenas um passo, que é o passo de download configurado no método `downloadCsvFileStep`.

3. **Configuração do Passo de Download do Arquivo CSV (`downloadCsvFileStep`):**
    - Cria um objeto `Step` que representa o passo de download do arquivo CSV.
    - Utiliza um `StepBuilder` para configurar o nome do passo, o repositório de jobs, o `Tasklet` (tarefa) responsável pelo download e o gerenciador de transações.
    - Esse passo possui apenas uma tarefa, que é a tarefa de download configurada no método `downloadCsvFileTasklet`.

4. **Configuração da Tarefa de Download (`downloadCsvFileTasklet`):**
    - Cria um `Tasklet` responsável por realizar o download do arquivo CSV da URL fornecida.
    - Utiliza parâmetros (`sourceFileUrl` e `targetFilePath`) passados dinamicamente durante a execução do job.
    - O método `execute` realiza o download do arquivo e retorna `RepeatStatus.FINISHED`.

5. **Configuração do Job de Carregamento do CSV no Banco de Dados (`loadCsvToDatabaseJob`):**
    - Cria um objeto `Job` que representa o trabalho de carregamento dos dados CSV no banco de dados.
    - Utiliza um `JobBuilder` para configurar o nome do job, o repositório de jobs.
    - Esse job possui apenas um passo, que é o passo de carregamento configurado no método `loadCsvToDatabaseStep`.

6. **Configuração do Passo de Carregamento do CSV no Banco de Dados (`loadCsvToDatabaseStep`):**
    - Cria um objeto `Step` que representa o passo de carregamento dos dados CSV no banco de dados.
    - Utiliza um `StepBuilder` para configurar o nome do passo, o repositório de jobs, um leitor, um processador e um escritor.
    - Define que o passo processará os dados em chunks de tamanho 10 e utiliza o gerenciador de transações.

7. **Configuração do Leitor (`reader`):**
    - Cria um `FlatFileItemReader` que lê objetos do tipo `com.example.PersonCsv` de um arquivo CSV.
    - Utiliza anotações `@StepScope` para permitir a passagem dinâmica de parâmetros.
    - O caminho do arquivo é fornecido como parâmetro dinâmico.

8. **Configuração do Processador (`processor`):**
    - Cria um processador que converte objetos `com.example.PersonCsv` em objetos `com.example.PersonDb`.
    - Se o título da pessoa contiver "Professor", retorna `null`, indicando que essa entrada não deve ser processada.

9. **Configuração do Escritor (`writer`):**
    - Cria um escritor `JdbcBatchItemWriter` que insere objetos `com.example.PersonDb` no banco de dados usando JDBC.

10. **Classe Interna (`DownloadCsvFileTasklet`):**
- Classe interna que implementa a interface `Tasklet` para realizar a tarefa de download do arquivo.
- O método `execute` realiza o download do arquivo e loga a conclusão.

11. **Como executar o projeto:**

- Criar container com postgres:

  docker run --name spring-batch-postgres -p 5432:5432 -e POSTGRES_USER=postgresql -e POSTGRES_PASSWORD=postgresql -e POSTGRES_DB=spring-batch-basic -d postgres:13

12. As configuração Spring Batch define dois jobs: um para o download do arquivo CSV e outro para o carregamento dos dados no banco de dados. Cada job possui um ou mais passos, e cada passo envolve um leitor, um processador e um escritor. O processo é altamente configurável e pode ser executado com diferentes parâmetros durante a execução.

- ./mvnw spring-boot:run \
-Dspring-boot.run.jvmArguments="-Dspring.batch.job.name=downloadCsvFileJob" \
-Dspring-boot.run.arguments="sourceFileUrl=https://raw.githubusercontent.com/lawlesst/vivo-sample-data/master/data/csv/people.csv targetFilePath=src/main/resources/data/people.csv"

- ./mvnw spring-boot:run \
-Dspring-boot.run.jvmArguments="-Dspring.batch.job.name=loadCsvToDatabaseJob" \
-Dspring-boot.run.arguments="targetFilePath=src/main/resources/data/people.csv"