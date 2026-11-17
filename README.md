# MalinaTicket

MalinaTicket - тикет-плагин для Paper/Purpur. Игрок оставляет обращение через GUI, персонал отвечает, назначает ответственного и закрывает тикет с причиной.

Автор: `MEvgeniy`

## Совместимость

- Целевой диапазон Paper/Purpur: `1.16.5-26.3`.
- Java для JAR: `16+`
- Для новых серверов используй Java, которую требует само ядро. Например, Paper/Purpur `26.1+` требует Java `25`.
- Spigot и Folia не заявлены как совместимые.

Один JAR для всего диапазона. Базовый API сборки: Paper 1.16.5. Встроенный Adventure обрабатывает MiniMessage и кликабельные сообщения.

## Сборка

По умолчанию для сборки нужен JDK 17. С установленным JDK 27 можно запустить ту же сборку с параметром `-PmalinaticketBuildJava=27`.

```powershell
.\gradlew.bat clean test build
```

Для публичного релиза можно указать хэш коммита:

```powershell
$env:GIT_COMMIT = "<хэш коммита>"
.\gradlew.bat clean build
```

Готовые файлы:

- `build/libs/MalinaTicket-26.11.1.jar`
- `build/libs/MalinaTicket-26.11.1.jar.sha256`

## Установка

1. Останови сервер.
2. Сделай резервную копию папки `plugins/MalinaTicket`, если обновляешь плагин.
3. Положи `MalinaTicket-26.11.1.jar` в `plugins`, заменив старый JAR.
4. Запусти Paper/Purpur.
5. Проверь файлы в `plugins/MalinaTicket`: `config.yml`, `messages.yml`, `gui.yml`, `categories.yml`, `permissions.yml`.
6. Выдай персоналу `malinaticket.staff`, полному администратору - `malinaticket.admin`.

Тикеты хранятся в `plugins/MalinaTicket/tickets/`. При конфликте имён используется файл с UUID игрока. Резервные копии сохраняются в `plugins/MalinaTicket/tickets/backups`. Существующие YAML читаются в прежнем формате.

## Команды

- `/ticket` - открыть меню игрока.
- `/ticket create` - создать тикет через GUI.
- `/ticket list` - открыть свои тикеты. С `closed` открываются свои закрытые, с `all` - все открытые при наличии права просмотра.
- `/ticket view <id>` - открыть тикет.
- `/ticket comment <id> <текст>` - добавить сообщение.
- `/ticket close <id> [причина]` - закрыть тикет.
- `/ticket staff` - меню персонала.
- `/ticket assign <id> <ник>` - назначить ответственного из игроков онлайн.
- `/ticket reopen <id>` - вернуть тикет в работу.
- `/ticket delete <id>` и `/ticket purge <id>` - мягкое и окончательное удаление.
- `/ticket tp <id>` - перейти к месту создания тикета.
- `/ticket cancel` - отменить ввод текста через чат.
- `/ticket reload` - перечитать конфиги.
- `/ticket stats` - посмотреть сводку тикетов.
- `/ticket ban <ник>` и `/ticket unban <ник>` - запретить или разрешить создание тикетов.
- `/ticket help` - показать подсказку по командам.

Псевдонимы команды: `/tickets`, `/report`, `/reports`, `/support`, `/mt`, `/mticket`.

## Права

Игрокам обычно достаточно прав по умолчанию. Меню персонала открывается с `malinaticket.staff` или `malinaticket.staff.gui`; отдельные действия и списки требуют соответствующих прав. Полный доступ - `malinaticket.admin`.

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

Категории меняются в `categories.yml`. Предметы меню в `gui.yml` поддерживают `material`, `name`, `lore`, `slot`, `amount`, `glow` и `custom-model-data`. Подсказки категорий и важности задаются в `menus.create.selection-hint` и `menus.create.selected-hint`. Если ключ отсутствует, используется исходный текст. Пустое значение скрывает подсказку.

В шаблонах можно писать `{#D94F70}` или `<#D94F70>`. Старые `gui.yml` и `messages.yml` можно оставить на месте.

## Проверка перед публикацией

- Для `26.11.1` прошли 36 модульных тестов. Проверены SHA-256 и версия классов Java 16 (`major version 60`).
- Игровой сценарий и перезапуск проверены на Paper 1.16.5 #794 и Purpur 1.16.5 #1171 с Java 16.
- Игровой сценарий и перезапуск проверены на Paper 26.3 #253 и Purpur 26.3 #2659 с Java 25. Для автоматизации использовался клиент 1.21.11 через ViaBackwards.
- Сценарий включает меню, создание нескольких тикетов, кликабельные уведомления, ответы без вывода в общий чат, отмену, назначение, телепорт, закрытие, переоткрытие, удаление и резервные копии.
- На Paper 1.16.5 и 26.3 дополнительно проверены свои подсказки меню, `amount`, `glow`, `custom-model-data` и ввод своей причины закрытия.
- Загрузка плагина также проверена на Paper 1.21.11 #132 и Purpur 1.21.11 #2568, Paper 26.1.2 #74 и Purpur 26.1.2 #2592, Paper 26.2 #135, Purpur 1.18.2 #1632 и Purpur 1.19.4 #1985. Остальные промежуточные сборки требуют отдельной проверки.
