# Checkout — projeto inicial dos alunos

Leia o arquivo `Enunciado-alunos.md`. O objetivo é implementar `ServicoCheckout` por TDD, testando seus quatro colaboradores com Mockito, sem banco de dados, rede ou Spring.

## Requisitos

Use **JDK 21 ou superior** — não apenas JRE — e Maven 3.8 ou superior. Confira `java -version`, `javac -version` e `mvn -version`. O `JAVA_HOME` deve apontar para o JDK usado pelo Maven.

O projeto fixa JUnit Jupiter 5.13.4, Mockito 5.18.0 e Java release 21 para reprodução. O primeiro uso do Maven precisa de acesso à internet para baixar as dependências.

## Execução

Na pasta que contém o `pom.xml`, execute:

```bash
mvn test
```

**A primeira execução deve falhar com um teste vermelho.** O teste inicial exige `IllegalArgumentException` para entrada nula, mas o serviço ainda lança `UnsupportedOperationException`. Corrija o comportamento pelo menor incremento possível e avance para os cenários restantes. Não remova o teste para obter um build verde.

Para conferir somente a compilação, incluindo a dos testes, sem executá-los:

```bash
mvn test-compile
```

O Maven Surefire já recebe a opção de agente Mockito para Java 21+. Na IDE, prefira delegar a execução ao Maven para usar a mesma configuração.

## Estrutura

`src/main/java/br/edu/checkout` contém records, interfaces, exceções e a classe do serviço com método pendente. `src/test/java/br/edu/checkout/ServicoCheckoutTest.java` contém apenas o primeiro teste. O `pom.xml` contém as dependências e a configuração de execução.

Use mocks somente para as interfaces externas. Instancie o serviço e os records de verdade. Acrescente testes antes de implementar cada novo comportamento e registre três ciclos reais Red–Green–Refactor durante o trabalho.
