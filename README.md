# AIMalb - Интеллектуальный ИИ-ассистент для Wear OS

**AIMalb** — это приложение для смарт-часов на базе Wear OS, предоставляющее мгновенный доступ к современным языковым моделям (LLM) через Groq API. Приложение оптимизировано для работы в условиях ограниченного интернета и предоставляет актуальные данные из сети в реальном времени.

---

## 🚀 Основные возможности

*   **Флагманский интеллект**: Поддержка моделей `gpt-oss-120b`, `llama-3.3-70b` и других через Groq LPU.
*   **Доступ в интернет**: Автоматический поиск через DuckDuckGo Lite для получения актуальных курсов валют, погоды и новостей.
*   **Прямые данные**: Мгновенное получение официальных курсов ЦБ РФ без галлюцинаций ИИ.
*   **Focus UI**: Текст ответа плавно прокручивается и центрируется по мере прочтения. Активная фраза всегда подсвечена.
*   **Companion App**: Удобное приложение для смартфона для мгновенной синхронизации API-ключа и настроек с часами.
*   **Виджет**: Запуск ассистента из виджета в боковом меню.
*   **Голос**: Доступно 2 варианта зачитывания текста: локальный tts от google и сторонний tts microsoft edge с естественным звучанием, в котором так же можно выбирать конкретный голос из списка. Так же можно регулировать скорость зачитывания ответа.

---

## 🛠 Установка

### 1. Часы (Wear OS)
1. Скачайте последний билд: [AIMalb1.6.8-beta-release.apk](https://github.com/Malboron/AIMalbWearOS/releases/download/1.6.8-beta/AIMalb1.6.8-beta-release.apk) 
2. Установите его через ADB или любой установщик APK для Wear OS.

### 2. Смартфон (Companion)
1. Скачайте: [AIMalbCompanion1.1.1-beta-release.apk](https://github.com/Malboron/AIMalbWearOS/releases/download/1.6.8-beta/AIMalbCompanion1.1.1-beta-release.apk)
2. Установите на телефон для удобного ввода настроек.

---

## ⚙️ Настройка и Использование

### Первый запуск
1. Получите бесплатный API ключ на [console.groq.com](https://console.groq.com/).
2. Откройте **AIMalb Companion** на телефоне.
3. Вставьте ключ и нажмите **Sync to Watch**. Ключ мгновенно «прилетит» на ваши часы.

### Настройки на часах (Шестеренка)
*   **Настройки ИИ**: Выбор модели и редактирование системного промпта. Список доступных моделей загружается через api Groq (не все модели работают с приложением, рекомендуется использовать модели от OpenAI).
*   **Слушать при запуске**: Если включено, микрофон активируется сразу после открытия приложения.
*   **Виджет**: В боковое меню можно добавить виджет для быстрого запуска приложения, при активной функции "слушать при запуске" можно сразу произносить запрос без дополнительных действий.

---

## 📷 Скриншоты

<img width="454" height="454" alt="Watch_Screenshot_1786604370082 png" src="https://github.com/user-attachments/assets/aefcb0ba-006f-4baa-bda7-0a2e0785163f" />
<img width="454" height="454" alt="Watch_Screenshot_1786604477474 png" src="https://github.com/user-attachments/assets/f2aaa819-18be-4806-98cd-be5c98dd3690" />
<img width="454" height="454" alt="Watch_Screenshot_1786604488233 png" src="https://github.com/user-attachments/assets/ca847e92-f881-4cbe-bef2-1330f7a54a04" />
<img width="454" height="454" alt="Watch_Screenshot_1786604387504 png" src="https://github.com/user-attachments/assets/97bf249d-6901-46d3-a353-a76441dac933" />
<img width="454" height="454" alt="Watch_Screenshot_1786604424787 png" src="https://github.com/user-attachments/assets/c3e77e32-ce52-47a2-9ea1-d6ee0fb0a544" />
<img width="454" height="454" alt="Watch_Screenshot_1786604435664 png" src="https://github.com/user-attachments/assets/08d2725d-c5a6-4c7f-a0c8-8693ee0f4516" />
<img width="454" height="454" alt="Watch_Screenshot_1786604455187 png" src="https://github.com/user-attachments/assets/03332b20-d0a7-4cf5-80cd-ce8ba6bbdd1a" />
<img width="454" height="454" alt="Watch_Screenshot_1786604341769 png" src="https://github.com/user-attachments/assets/ada15cc9-1d55-4a1c-9b65-c14db48147a6" />
<img width="1440" height="3088" alt="Screenshot_20260813_140633_AIMalb" src="https://github.com/user-attachments/assets/44e804b1-d19f-4611-a083-b65fff87c634" />
<img width="1440" height="3088" alt="Screenshot_20260813_140703_AIMalb" src="https://github.com/user-attachments/assets/05347467-ffc0-4615-8571-006d76d9a73e" />


---

## Технологии

* Kotlin
* Jetpack Compose
* Wear OS
* Android Data Layer API
* Retrofit
* Kotlin Coroutines
* Groq API

## Архитектура

Проект состоит из двух приложений:

* [AIMalb] — основное приложение для часов
* [AIMalbCompanion]— приложение-компаньон для смартфона

Связь между устройствами выполняется через Wear OS Data Layer.

## Статус проекта

В разработке.

## Использование ИИ

При создании проекта использовались инструменты искусственного интеллекта для помощи в разработке, анализе кода и поиске решений.

Основной AI-инструмент, использованный в процессе разработки:

* Google Gemini — помощь с генерацией и анализом кода, архитектурными решениями и отладкой.

ИИ использовался как вспомогательный инструмент. Все решения, интеграция компонентов и финальная проверка кода выполнялись в процессе разработки проекта.

## Лицензия

Проект распространяется с открытым исходным кодом.

## Поддержать проект

Если проект оказался полезным, вы можете поддержать его развитие:

[💙 Поддержать на ЮMoney](https://yoomoney.ru/to/4100119587032789/0)
