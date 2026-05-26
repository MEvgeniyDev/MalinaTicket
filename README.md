# MalinaTicket

MalinaTicket - тикет-плагин для Paper/Purpur. Игрок оставляет обращение через GUI, персонал отвечает, назначает ответственного и закрывает тикет с причиной.

Автор: `MEvgeniy`

## Совместимость

- Paper/Purpur: `1.16.5-26.1.2`
- Java для JAR: `16+`
- Для новых серверов используй Java, которую требует само ядро. Например, Paper/Purpur `26.1+` требует Java `25`.
- Spigot и Folia не заявлены как совместимые.

Плагин собирается как универсальный JAR: байткод Java 16, Paper API 1.16.5 как базовый API сборки и встроенный Adventure для MiniMessage/кликабельных сообщений.

## Сборка

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean shadowJar
```

Для публичного релиза можно указать хэш коммита:

```powershell
$env:GIT_COMMIT = "<хэш коммита>"
.\gradlew.bat clean build
```

Готовые файлы:

- `build/libs/MalinaTicket-26.5.9.jar`
- `build/libs/MalinaTicket-26.5.9.jar.sha256`

## Установка

1. Останови сервер.
2. Скопируй `MalinaTicket-26.5.9.jar` в `plugins`.
3. Запусти Paper/Purpur.
4. Проверь файлы в `plugins/MalinaTicket`: `config.yml`, `messages.yml`, `gui.yml`, `categories.yml`, `permissions.yml`.
5. Выдай персоналу `malinaticket.staff` или `malinaticket.admin`.

Тикеты хранятся в `plugins/MalinaTicket/tickets/<ник>.yml`. Последние `.bak` копии сохраняются в `plugins/MalinaTicket/tickets/backups`.

## Команды

- `/ticket` - открыть меню игрока.
- `/ticket create` - создать тикет через GUI.
- `/ticket list [closed|all]` - открыть список тикетов.
- `/ticket view <id>` - открыть тикет.
- `/ticket comment <id> <текст>` - добавить сообщение.
- `/ticket close <id> [причина]` - закрыть тикет.
- `/ticket staff` - меню персонала.
- `/ticket assign <id> <ник>` - назначить ответственного.
- `/ticket reopen <id>` - вернуть тикет в работу.
- `/ticket delete <id>` и `/ticket purge <id>` - мягкое и окончательное удаление.
- `/ticket tp <id>` - перейти к месту создания тикета.
- `/ticket cancel` - отменить ввод текста через чат.
- `/ticket reload` - перечитать конфиги.

## Права

Обычно игрокам достаточно прав по умолчанию, персоналу - `malinaticket.staff`, полному администратору - `malinaticket.admin`.

Базовые права игрока:

- `malinaticket.use`
- `malinaticket.create`
- `malinaticket.view.own`
- `malinaticket.comment.own`
- `malinaticket.close.own`

Категории используют шаблон `malinaticket.category.<id>.create`. Стандартные категории:

- `malinaticket.category.bug.create`
- `malinaticket.category.player_report.create`
- `malinaticket.category.rules.create`
- `malinaticket.category.items.create`
- `malinaticket.category.donate.create`
- `malinaticket.category.question.create`
- `malinaticket.category.other.create`

Основные права персонала:

- `malinaticket.staff.gui`
- `malinaticket.view.all`
- `malinaticket.view.closed`
- `malinaticket.view.deleted`
- `malinaticket.comment.staff`
- `malinaticket.assign`
- `malinaticket.close`
- `malinaticket.reopen`
- `malinaticket.delete`
- `malinaticket.purge`
- `malinaticket.teleport`
- `malinaticket.stats`
- `malinaticket.reload`
- `malinaticket.ban`
- `malinaticket.unban`
- `malinaticket.bypass.cooldown`
- `malinaticket.bypass.limit`
- `malinaticket.notify.create`
- `malinaticket.notify.comment`
- `malinaticket.notify.close`
- `malinaticket.notify.join`

Групповые права:

- `malinaticket.staff`
- `malinaticket.admin`

## Настройка

Категории меняются в `categories.yml`. GUI-предметы в `gui.yml` поддерживают `material`, `name`, `lore`, `slot`, `amount`, `glow` и `custom-model-data`. Сообщения используют MiniMessage; на старых серверах текст конвертируется через встроенный Adventure/Bukkit platform.

## Проверка перед публикацией

- `.\gradlew.bat clean test`
- `.\gradlew.bat clean shadowJar`
- проверить `major version 60` у классов JAR;
- проверить запуск на Paper/Purpur `1.16.5`, `1.17.1`, `1.18.2`, `1.19.4`, `1.20.4`, `1.20.6`, `1.21.1`, `1.21.11`, `26.1.2`;
- вручную проверить `/ticket`, создание, комментарии, закрытие, кликабельные сообщения и резервные копии в `tickets/backups`.
