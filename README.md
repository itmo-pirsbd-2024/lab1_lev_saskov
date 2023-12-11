# Векторная база данных

## Модули
БД разделена на несколько модулей:
* ldr-vector-db - ядро, позволяет создать бд на одной машине
* ldr-worker - веб сервер для бд на одной машине
* ldr-master - веб сервер позволяет управлять большим количеством worker-ов

## Сборка
Поосле клонирования репозитория:
1. Настройте project structure проекта, добавивив все gradle модули. 
При импорте модулей выбирайте опцию "import module from existing model" -> "gradle".

## Пример развертывания
1. Развернуть воркеров
```
java -jar ldr-worker-1.0.0.jar --server.port=8001 --database.location="worker1"
```
```
java -jar ldr-worker-1.0.0.jar --server-port=8002 --database.location="worker2" 
```
2. Развернуть мастера
```
java -jar ldr-master1-1.0.0.jar "--workers.configs={worker1:'http://127.0.0.1:8001/database/collection',worker2:'http://127.0.0.1:8002/database/collection'}"
```