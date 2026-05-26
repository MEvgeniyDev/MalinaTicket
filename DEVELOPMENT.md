# Разработка

MalinaTicket `26.5.9` собирается как универсальный Paper/Purpur-плагин. При сборке используется Paper API `1.16.5-R0.1-SNAPSHOT`, а совместимость с Paper/Purpur до `26.1.2` сохраняется через небольшие адаптеры совместимости.

## Локальная сборка

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean shadowJar
```

Готовые файлы релиза:

- `build/libs/MalinaTicket-26.5.9.jar`
- `build/libs/MalinaTicket-26.5.9.jar.sha256`

Для публичного релиза перед сборкой можно указать хэш коммита:

```powershell
$env:GIT_COMMIT = "<хэш коммита>"
.\gradlew.bat clean build
```

Если `GIT_COMMIT` не задан, в манифесте JAR-файла останется `Built-From-Revision: local`.

## Совместимость

- Базовый API сборки: `com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT`.
- Java для сборки: `17`, компиляция идет с `--release 16`.
- Целевой байткод: Java `16` (`major version 60`).
- На новых серверах используется та Java, которую требует само ядро.
- Adventure/MiniMessage добавлен внутрь JAR и перенесен в `ru.mevgeniy.malinaticket.libs.kyori`.
- Новые вызовы Paper API нельзя подключать напрямую. Если они нужны, их нужно прятать за рефлексию или адаптеры совместимости.

## Проверка релиза

1. Запустить `.\gradlew.bat clean test`.
2. Запустить `.\gradlew.bat clean build`.
3. Проверить JAR командой `jar tf build/libs/MalinaTicket-26.5.9.jar`.
4. Проверить версию классов через `javap -verbose`.
5. Проверить запуск минимум на Paper/Purpur `1.16.5`, `1.21.11` и `26.1.2`.
6. Вручную проверить `/ticket`, создание тикета, комментарии, закрытие, GUI персонала, reload, сохранение после рестарта, перехват чата и кликабельные сообщения.

## Правила поддержки

- Формат хранения тикетов должен оставаться совместимым, если в журнале изменений явно не указано обратное.
- Нельзя добавлять в исходники данные живого сервера или сгенерированные артефакты сборки.
- Ссылка GitHub в `plugin.yml` должна оставаться `https://github.com/MEvgeniyDev/MalinaTicket`.
