# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Hob-v<версия>.aab`.
  Первая версия для Play — **v1.0.1**. Секреты `SIGNING_*` в репозитории заданы; без них сборка
  подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
  Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.hob`.
  2. **Select key** → ключ с SHA-256 `3b5d7a2cf4f2066bfed2e82786d1b46d6442a54ef426f2de4e065acb141ddc39`
     (сертификат CN=Hob, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.hob` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/hob/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Hob: Home Inventory` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать (приложение только портретное) | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из скриншотов README (`docs/screenshots/<язык>`), а те
сняты с телефона `tools/shots.py`: в рендерах Robolectric часть эмодзи рисуется квадратиками. Play не
берёт картинки длиннее двух ширин, поэтому экран уходит за нижний край под подписью. Как пересобрать
(телефон подключён по USB и разблокирован, demo-сборка уже стоит — MIUI не даёт ставить новые пакеты по USB):

```bash
./gradlew installDebug -Pdemo -Plang=ru && python tools/shots.py ru
./gradlew installDebug -Pdemo -Plang=en && python tools/shots.py en
python tools/play.py   # home, find, room, thing, kid, people, clear-out, hob
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **House & Home** |
| Tags (до 5) | House & home, Productivity, Notebook, Parenting, Lifestyle |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** В House & Home ищут «домашняя опись», «где лежит», «home inventory». Альтернатива —
**Productivity**. Категорию можно сменить позже в Store settings.

**Теги** — из списка Play. House & home — главный: опись дома. Productivity и Notebook — записать
вещь и найти её. Parenting — вещи на вырост, рост и размеры детей. Lifestyle — разбор и уют.
Interior design и Home automation не подходят: про ремонт и умный дом приложение не говорит.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, как у Липовки: тогда Play сам покажет
под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- На все вопросы — **No**: насилия, страха, сексуального контента, ненормативной лексики нет; азартных
  игр и покупок нет; общения между пользователями и обмена контентом нет; местоположения нет; браузера нет.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **18 and over** (можно добавить 16–17). Приложение для взрослых, которые ведут дом;
  данные о детях вносит родитель.
- Младше 13 не отмечать: иначе действует политика Families с отдельными требованиями.
- *Could your store listing unintentionally appeal to children?* → **No**: домовой нарисован, но приложение —
  опись вещей для взрослых.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- У приложения **нет разрешения INTERNET** — оно физически не может ничего передать. Нет серверов,
  аналитики, отчётов о сбоях. Фото берутся через системную камеру или выбор фото, без разрешений.
- *Does your app collect or share any of the required user data types?* → **No**.
- Итог в карточке: «No data collected», «No data shared with third parties».
- Копию дома (zip, CSV) и напоминание через «Поделиться» пользователь отправляет сам, системными
  средствами — по определению Google это не сбор данных приложением.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — ничего не отмечать (рост и размер обуви детей — для подбора одежды, не медицинские данные).

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Разрешения** — особых деклараций не нужно: `POST_NOTIFICATIONS` и `RECEIVE_BOOT_COMPLETED` не относятся
к чувствительным. Точных будильников (exact alarm) и фоновых служб нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Hob-v1.0.1.aab` из GitHub Release.
3. Release name: `1.0.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; утреннее уведомление приходит; фото с камеры добавляется; копия дома сохраняется
   и открывается.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.hob`.
