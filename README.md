## [REST API](http://localhost:8080/doc)

## Концепция:

- Spring Modulith
    - [Spring Modulith: достигли ли мы зрелости модульности](https://habr.com/ru/post/701984/)
    - [Introducing Spring Modulith](https://spring.io/blog/2022/10/21/introducing-spring-modulith)
    - [Spring Modulith - Reference documentation](https://docs.spring.io/spring-modulith/docs/current-SNAPSHOT/reference/html/)

```
  url: jdbc:postgresql://localhost:5432/jira
  username: jira
  password: JiraRush
```

- Есть 2 общие таблицы, на которых не fk
    - _Reference_ - справочник. Связь делаем по _code_ (по id нельзя, тк id привязано к окружению-конкретной базе)
    - _UserBelong_ - привязка юзеров с типом (owner, lead, ...) к объекту (таска, проект, спринт, ...). FK вручную будем
      проверять

## Аналоги

- https://java-source.net/open-source/issue-trackers

## Тестирование

- https://habr.com/ru/articles/259055/


✅ Выполненные пункты

1. Удалены социальные сети VK и Yandex
✅ Полностью удалены интеграции с VK и Yandex OAuth из проекта.

2. Вынесена чувствительная информация в отдельный файл application-secret.properties
✅ Вынесены в отдельный файл:
-логин и пароль БД;
-настройки почты (SMTP, логин, пароль);
-OAuth2-клиенты (Google, GitHub, GitLab).

✅ Все значения читаются из переменных окружения через ${...}.

3. Переключение тестов на in-memory базу H2

✅ Настроена база H2 для тестирования.
✅ Созданы два отдельных конфига — для PostgreSQL и H2, переключение через Spring Profile.
✅ Обновлены скрипты Liquibase и тестовые данные для совместимости с H2.

4. Написаны тесты для ProfileRestController

✅ Покрыты все публичные методы контроллера:
GET /api/profile
PUT /api/profile

✅ Написаны тесты для успешных и неуспешных сценариев (неавторизованный доступ, неверные данные и др.).

5. Рефакторинг метода FileUtil.upload()

✅ Метод переработан с использованием современного Java NIO API (Files.copy, Path, и т.д.).

✅ Избавление от устаревших способов работы с файлами.


6. Создан Dockerfile для основного приложения

✅ Написан Dockerfile, который:

-копирует .jar файл;
-добавляет статические ресурсы и шаблоны писем;
-использует базовый образ eclipse-temurin:17-jdk.

7. Создан docker-compose.yml для запуска
✅ Написан файл docker-compose.yml, который поднимает:

  - основной контейнер с Spring Boot-приложением;
  - контейнер с NGINX (reverse proxy).

✅ Файл не стартует PostgreSQL: приложение подключается к уже существующему контейнеру postgres-db, так как используется уже ранее запущенный вручную, согласно заданию.
✅ Подключён конфиг NGINX — config/nginx.conf.
✅ Все сервисы объеденены в одну сеть, проверена связность и работа портов.
