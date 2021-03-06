# Сетевое хранилище на Java

Приложение имеет клиент-серверную архитектуру.
Ни в коем случае не используем: Java EE, Spring.

## Основной функционал:
1. Регистрация и аутентификация на сервере
2. Передача файлов с клиента на сервер, скачивание файлов с сервера
3. Просмотр списка файлов на сервере
4. Переименование, удаление файлов на сервере
5. Файлы хранятся на сервере в папках, названия которых
соответствуют логину пользователя

## Основные вопросы (ответить на них в ДЗ!!!):
1. Как вы собираетесь передавать файлы?
Поток байт (свой протокол), Сериализация
2. Нужно ли передавать что-то кроме файлов?
Передаем файлы, команды, ?
3. Как передавать файлы/команды по одному сетевому соединению? 
Или открываем несколько соединений?
4. Нужна ли нам база данных, если да, то зачем?
5. Клиент: консоль, GUI (Swing, JavaFX)

## Чего не стоит делать на первом этапе:
1. Вложенные папки
2. Синхронизация папок на сервере/клиенте
3. Шифрование/сжатие данных при передаче
4. Подсчет контрольной суммы
5. Параллельная передача файлов
6. Докачка файлов
7. Sharing файлов

## Можно не отвечать:
1. Какую библиотеку использовать? (java.io, java.nio, Netty)

## Прояснить вопросы:
Клиент.
1. Как указать путь к корневой папке приложения или 
папке с документами пользователя? Если задать жесткий 
абсолютный путь, то он будет неверный при установке приложения
на другом ПК. Хотелось бы, чтобы по умолчанию открывалась 
какая-то определенная папка.
Сервер.
1. Как указать относительный путь к папке resources(server module)? 
Чтобы, например, хранить в ней папки с файлами пользователей.
В Idea мне удалось задать только абсолютный путь, но он не будет
работать при установке приложения на другом ПК.