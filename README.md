# MalinaTicket

MalinaTicket - простой тикет-плагин для Paper/Purpur `1.21.11` с API `26.1.2`. Он закрывает обычный сценарий небольшого сервера: игрок оставляет обращение через GUI, персонал отвечает, назначает ответственного и закрывает тикет с причиной.

Автор: `MEvgeniy`

## Совместимость

- Minecraft/Paper/Purpur: `1.21.11`
- Paper API: `26.1.2`
- Java: `25`
- База данных не нужна: тикеты лежат в YAML-файлах внутри папки плагина.

Поддержка заявлена для Paper/Purpur `1.21.11`. Новые версии Paper/Purpur могут работать, если сохраняют совместимость с API `26.1.2`, но без отдельной проверки это не обещается. Плагин не заявляет совместимость со Spigot, Folia и старыми ветками Paper.

## Сборка и релиз

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean build
```

Для публичного релиза лучше собирать с указанием commit hash, чтобы поле `Built-From-Revision` в manifest указывало на конкретный исходный код:

```powershell
$env:GIT_COMMIT = "<commit hash>"
.\gradlew.bat clean build
```

Если `GIT_COMMIT` не задан, в JAR остается значение `local`.

Готовые файлы:

- `build/libs/MalinaTicket-26.5.8.jar`
- `build/libs/MalinaTicket-26.5.8.jar.sha256`

Перед публикацией полезно проверить содержимое:

```powershell
jar tf build/libs/MalinaTicket-26.5.8.jar
```

## Установка

1. Останови сервер.
2. Скопируй `MalinaTicket-26.5.8.jar` в `plugins`.
3. Запусти Paper/Purpur `1.21.11` с API `26.1.2`.
4. Проверь файлы в `plugins/MalinaTicket`: `config.yml`, `messages.yml`, `gui.yml`, `categories.yml`, `permissions.yml`.
5. Выдай персоналу `malinaticket.staff` или `malinaticket.admin`.

Тикеты хранятся в `plugins/MalinaTicket/tickets/<ник>.yml`. Файл `_meta.yml` хранит следующий ID и список игроков, которым временно запрещено создавать тикеты. Последние `.bak` копии сохраняются в `plugins/MalinaTicket/tickets/backups`, но перед обновлениями всё равно лучше копировать всю папку `tickets`.

## Основные команды

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

Для обычных игроков основные права по умолчанию доступны всем. Для персонала обычно достаточно выдать `malinaticket.staff`, для полного управления - `malinaticket.admin`.

### Готовые наборы

| Право | По умолчанию | За что отвечает |
| --- | --- | --- |
| `malinaticket.staff` | op | Базовый набор прав персонала: просмотр тикетов, ответы, назначение, закрытие, переоткрытие, телепорт и уведомления. |
| `malinaticket.admin` | op | Полный доступ к MalinaTicket, включая удаление, статистику, reload, ban/unban и обход лимитов. |

### Игроки

| Право | По умолчанию | За что отвечает |
| --- | --- | --- |
| `malinaticket.use` | everyone | Открытие меню тикетов. |
| `malinaticket.create` | everyone | Создание тикетов. |
| `malinaticket.view.own` | everyone | Просмотр своих тикетов. |
| `malinaticket.comment.own` | everyone | Ответы в своих тикетах. |
| `malinaticket.close.own` | everyone | Закрытие своих тикетов. |

### Персонал

| Право | По умолчанию | За что отвечает |
| --- | --- | --- |
| `malinaticket.staff.gui` | op | Открытие меню персонала. |
| `malinaticket.view.all` | op | Просмотр всех открытых тикетов. |
| `malinaticket.view.closed` | op | Просмотр закрытых тикетов. |
| `malinaticket.view.deleted` | op | Просмотр мягко удаленных тикетов. |
| `malinaticket.comment.staff` | op | Ответы персонала в тикетах. |
| `malinaticket.assign` | op | Назначение ответственного. |
| `malinaticket.close` | op | Закрытие чужих тикетов. |
| `malinaticket.reopen` | op | Переоткрытие тикетов. |
| `malinaticket.delete` | op | Мягкое удаление тикетов. |
| `malinaticket.purge` | op | Окончательное удаление тикетов. |
| `malinaticket.teleport` | op | Телепорт к месту создания тикета. |
| `malinaticket.stats` | op | Просмотр статистики. |
| `malinaticket.reload` | op | Перезагрузка конфигов плагина. |
| `malinaticket.ban` | op | Запрет игроку создавать тикеты. |
| `malinaticket.unban` | op | Снятие запрета на создание тикетов. |

### Уведомления

| Право | По умолчанию | За что отвечает |
| --- | --- | --- |
| `malinaticket.notify.create` | op | Уведомления персоналу о новых тикетах. |
| `malinaticket.notify.comment` | op | Уведомления о новых комментариях. |
| `malinaticket.notify.close` | op | Уведомления о закрытии тикетов. |
| `malinaticket.notify.join` | op | Сводка по тикетам при входе персонала. |

### Обходы

| Право | По умолчанию | За что отвечает |
| --- | --- | --- |
| `malinaticket.bypass.cooldown` | op | Обход задержки создания тикета. |
| `malinaticket.bypass.limit` | op | Обход лимита открытых тикетов. |

### Категории

Права категорий создаются по шаблону `malinaticket.category.<id>.create`. В стандартной конфигурации есть:

- `malinaticket.category.bug.create`
- `malinaticket.category.player_report.create`
- `malinaticket.category.rules.create`
- `malinaticket.category.items.create`
- `malinaticket.category.donate.create`
- `malinaticket.category.question.create`
- `malinaticket.category.other.create`

## Настройка

Категории меняются в `categories.yml`. Для каждой категории можно задать материал, слот, цвет, описание и отдельное право доступа. GUI-предметы в `gui.yml` поддерживают `material`, `name`, `lore`, `slot`, `amount`, `glow` и `custom-model-data`. Причины закрытия тикетов тоже настраиваются в `gui.yml`. Сообщения и GUI-тексты используют MiniMessage-формат, поэтому после правок стоит проверить русские символы и кликабельные подсказки прямо в игре.

Пример ограничения для обычных игроков:

```yaml
settings:
  cooldown-seconds: 60
  max-open-tickets: 3
  max-message-length: 500
```

## Известные ограничения

- Хранилище файловое, поэтому это не лучший вариант для сети серверов с общими тикетами.
- `/ticket assign` назначает только игрока онлайн.
- В закрытый тикет нельзя писать новые сообщения: его нужно сначала переоткрыть.
- Переименование игрока учитывается при входе, но старый файл может оставаться до следующего сохранения его тикетов.
- Проверка релиза на реальном сервере обязательна: часть поведения зависит от Paper runtime, а не только от unit-тестов.

## Troubleshooting

- Если меню открывается, но клики не работают, проверь права и ошибки в консоли.
- Если русские сообщения выглядят сломанными, сохрани YAML-файлы в UTF-8.
- Если тикеты пропали после ручной правки YAML, верни резервную копию `plugins/MalinaTicket/tickets`.
- Если сервер отказывается грузить JAR, проверь версию Java и Paper/Purpur.
