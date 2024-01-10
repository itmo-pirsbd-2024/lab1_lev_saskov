# Векторная база данных

## Модули
БД разделена на несколько модулей:
* ldr-vector-db - ядро, позволяет создать бд на одной машине.
* ldr-worker - машина, дающая доступ к бд, все воркеры равноправны. 

## Сборка
Поосле клонирования репозитория:
1. Настройте project structure проекта, добавивив все gradle модули. 
При импорте модулей выбирайте опцию "import module from existing model" -> "gradle".
To build worker app
```
./gradlew bootJar
```

## Пример развертывания
1. Развернуть воркеров
```
java -jar ldr-worker-1.0.0.jar --server.port=8001 --database.location="worker1" --local.worker.name="worker1" --zookeeper.host="127.0.0.1:2181"
```
```
java -jar ldr-worker-1.0.0.jar --server.port=8002 --database.location="worker2" --local.worker.name="worker2" --zookeeper.host="127.0.0.1:2181"
```

## Пример работы
Вся работа идет на нодах-воркерах. Запросы можно отправлять на любого воркера.
Далее идут примеры заросов.
1. Создание коллекции
```
curl -X POST -d "name=Collection1&vectorLen=5" http://localhost:8001/database/collection
```
2. Добавление в коллекцию
```
curl -X PUT 'localhost:8001/database/collection/Collection1' -H 'Content-Type: application/json' -d '{"id":10, "vector":[5.0, 320.3, 32.4, 7.9, 22.3]}'
```
3. Удаление из коллекции
4. Удаление коллекции
```
curl -X DELETE -d "name=Colection1" http://localhost:8001/database/collection
```
5. Найти ближайшие вектора в коллекции 
```
curl -X GET 'localhost:80001/database/collection/collTest?vector=10.0,11.0&maxNeighboursCount=10'
```
