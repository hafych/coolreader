# Google Play listing draft

Status: content draft only. Do not upload it until the permanent product name,
application ID, publisher identity, support contacts and asset rights in
[`IDENTITY_AND_ASSETS.md`](IDENTITY_AND_ASSETS.md) are approved.

Proposed category: **Books & Reference**.

## en-US

### Title

CoolReader Next

### Short description

Private, customizable ebook reader for local libraries and E-Ink devices.

### Full description

Read your own ebook library with a fast, highly customizable open-source reader.
CoolReader Next is designed for local collections, long reading sessions and
both LCD and E-Ink screens.

Open EPUB, FB2, TXT, RTF, HTML, CHM, DOC, PDB, MOBI and other supported ebook
formats. Choose individual documents with Android's system picker or grant
access to library folders that you control.

Features:
• themes, fonts, margins, line spacing and page-turn controls;
• bookmarks, text search, selection and reading-position history;
• text-to-speech controls with a foreground playback notification;
• OPDS catalog browsing and downloads;
• optional consumption-only LitRes access for trials and books already owned by
  a signed-in user;
• no ads, analytics or crash-reporting SDKs.

The app does not request broad access to shared storage. Books, bookmarks and
reading history remain on your device unless you explicitly open an online
catalog, sign in to LitRes, or send selected text to another app. LitRes account
creation and purchases are not offered in the app.

CoolReader Next is free software based on the CoolReader project. Product name,
publisher details and final artwork remain subject to release-owner approval.

## ru-RU

### Title

CoolReader Next

### Short description

Приватная настраиваемая читалка для локальной библиотеки и E-Ink.

### Full description

Читайте собственную электронную библиотеку в быстрой читалке с гибкими
настройками. CoolReader Next создан для локальных коллекций, долгого чтения и
экранов LCD и E-Ink.

Открывайте EPUB, FB2, TXT, RTF, HTML, CHM, DOC, PDB, MOBI и другие
поддерживаемые форматы. Выбирайте отдельные документы через системный диалог
Android или предоставляйте доступ только к выбранным вами папкам библиотеки.

Возможности:
• темы, шрифты, поля, межстрочный интервал и настройка перелистывания;
• закладки, поиск по тексту, выделение и сохранение позиции чтения;
• озвучивание текста с управлением через системное уведомление;
• просмотр OPDS-каталогов и загрузка книг;
• необязательный режим LitRes только для пробных и ранее приобретённых книг;
• без рекламы, аналитики и SDK для отправки сбоев.

Приложение не запрашивает полный доступ к общему хранилищу. Книги, закладки и
история чтения остаются на устройстве, пока вы сами не откроете сетевой каталог,
не войдёте в LitRes или не передадите выделенный текст другому приложению.
Регистрация аккаунта и покупки LitRes внутри приложения недоступны.

CoolReader Next — свободное ПО на основе проекта CoolReader. Название продукта,
данные издателя и финальная графика требуют утверждения владельцем релиза.

## Required assets

The repository currently has adaptive launcher assets only up to 192 × 192 px.
They are not a valid source for a 512 × 512 Play icon and their fork-specific
rights/provenance have not been approved.

| Asset | Play requirement | Planned evidence |
| --- | --- | --- |
| App icon | 512 × 512 PNG, up to 1 MiB | Export from approved vector master; verify at actual size |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG, no alpha | Create only after brand approval |
| Phone screenshots | At least 2; target 4 portrait images at 1080 × 1920 | Capture from the signed release candidate |
| Tablet screenshots | Optional but recommended for supported form factors | Capture after tablet layout review |

Screenshot shot list:

1. Library root with a user-selected SAF folder.
2. Reader view showing typography controls.
3. Search/bookmarks panel.
4. E-Ink-friendly reader theme.

No screenshot may contain private books, account identifiers, credentials,
device notifications from other apps or unlicensed cover art.

## Console fields still required

- approved permanent title and publisher/developer name;
- support email, optional website and public privacy-policy URL;
- final category/tags and content-rating questionnaire;
- declarations for ads, app access, target audience, Data safety and payments;
- approved icon, feature graphic and release-candidate screenshots.

Play listing limits and asset requirements must be rechecked immediately before
upload:

- <https://support.google.com/googleplay/android-developer/answer/9859152>
- <https://support.google.com/googleplay/android-developer/answer/9866151>
