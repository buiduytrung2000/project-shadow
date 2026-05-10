# Fonts

The game runtime expects `BeVietnamPro-Regular.ttf` in this directory to render
Vietnamese tone marks correctly. The TTF is **not** committed to the repo —
download it once and drop the file here.

## Where to get it

**Be Vietnam Pro** by Lampluc — SIL Open Font License (free, redistributable):

- Google Fonts: <https://fonts.google.com/specimen/Be+Vietnam+Pro>
  - Click "Download family" → unzip → take `BeVietnamPro-Regular.ttf`.
- GitHub source: <https://github.com/bettergui/BeVietnamPro>

Drop the file at:

```
assets/fonts/BeVietnamPro-Regular.ttf
```

## Why a runtime download instead of committing the file

- TTF is a binary blob (~280 KB) that bloats the diff history.
- License redistribution is fine but Google Fonts already hosts the canonical copy.
- If the file is missing at runtime, `FontFactory` throws a clear
  `IllegalArgumentException` pointing here.

## Swapping fonts

`FontFactory` is constructed in `CombatScreen` with an explicit path. To switch
to a different VN-supporting TTF (Noto Sans, Roboto, Source Sans 3, etc.):

1. Drop the new `.ttf` here.
2. Update the path passed to `new FontFactory(Gdx.files.internal("fonts/<name>.ttf"))`.

The character set covered (`VietnameseCharset.ALL`) includes ASCII, Latin-1,
Latin Extended-A, and the Latin Extended Additional block (0x1EA0–0x1EF9)
where Vietnamese tone marks live.
