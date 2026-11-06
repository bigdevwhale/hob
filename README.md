# Hob

*«Домовой»* – a cosy home inventory for Android. The hob remembers where everything lies: the extension lead, the passports, the tree decorations, the winter boots waiting for the younger child.

- **Your own house:** places (a flat, a dacha, a garage), rooms, the spots in them (the top shelf, under the bed) and numbered boxes.
- **«Where is…?»** – search by name, room, box label or number.
- **Notes from the hob:** what's needed soon (seasons), what's lent and overdue, what a child has grown into.
- **Growing into things:** children's clothes and shoes with sizes from the label; the hob forecasts from each child's measurements when a thing will fit.
- **Lending:** who took what, with a reminder; things borrowed from others.
- **Clear-out:** things untouched for a year come one by one – keep, give away, sell or throw out.
- **Cosiness:** dark corners light up as rooms are written down; levels from guest to old-timer; a bowl of milk after a clear-out.
- Evening and day light, Russian and English, a copy of the whole house in one file. No internet access, no accounts, no ads.

Part of [StillCraft](https://bigdevwhale.github.io/stillcraft/).

## Build

```
./gradlew assembleDebug              # app.hob.dev
./gradlew assembleDebug -Pdemo       # app.hob.demo with a lived-in flat and dacha
./gradlew testDebugUnitTest -Prender # screens drawn to app/build/art
python tools/res.py                  # strings (EN + RU) from one table
```

## License

MIT. Fonts: Lora (Cyreal) and Nunito (Vernon Adams), SIL Open Font License.
