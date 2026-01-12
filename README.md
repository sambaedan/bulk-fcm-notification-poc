# **Project Overview"

- This project is created to modernize bulk FCM (Firebase Cloud Messaging) implementation using up-to-date best practices and tools.

# **Project Setup**

## **Prerequisites**

- ***Java***

- ***Spring Boot***

- ***Mysql***

- ***Docker***

- ***Maven***

- ***IDE(IntelliJ)***

## **Version**

- ***Java 25***

- ***Spring Boot 3.5.8***

- ***Mysql(latest)***

- ***Docker(Latest)***

# **Build**

- For Java, install the suitable jdk in the project structure of the IDE [Java Installation](https://www.jetbrains.com/guide/java/tips/download-jdk/)

- For Spring Boot build, go to [Spring Initializer](https://start.spring.io/) or in a project directory add suitable dependencies in pom.xml file

- For Docker, install docker using [Docker Installation](https://docs.docker.com/engine/install/ubuntu/)

# **Start**

- For maven build `mvn clean` and  `mvn install`

- To start docker container
  ```bash
     docker compose up -d
   ```

- To stop the docker service run
  ```bash
     docker compose down
  ```

- To see running docker services
    ```bash
       docker ps
    ```
# **Developer Note**

- A firebase sdk is needed in resources classpath to properly intialize the firebase instance

  


