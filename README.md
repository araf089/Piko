<div align="center">

# 🌙 Piko

**Your cute, friendly AI phone buddy for Android**

*Talk to it in English or Bangla · it opens apps, searches, controls your phone — and reacts with adorable animations.*

Made with 💙 by **Araf** · Discord: **araf089**

[![Join the Discord](https://img.shields.io/badge/Discord-Join%20the%20server-5865F2?logo=discord&logoColor=white)](https://discord.gg/PDtCFJR7PM)

</div>

---

## ✨ What is Piko?

Piko is a little blue dome with a big personality. Leave your phone on a stand, tap Piko once, and just talk. It listens, answers like a close friend, and can operate your phone by voice. It also reacts to the world around it: it sleeps when idle, gets happy when you plug in the charger, dances when music plays, and gives a suspicious side-eye when a notification arrives.

## 🎯 Features

| | Feature |
|---|---|
| 🗣️ | **Voice assistant**: tap to talk, Piko listens and speaks back |
| 🇬🇧🇧🇩 | **English + Bangla**: understands and replies in both (auto-detect, or lock a language) |
| 🧠 | **Powered by Gemini**: friendly, short, natural answers (free API key supported) |
| 📱 | **Phone control**: open apps, YouTube / Google search, brightness, volume, flashlight, back / home / recents, scroll, tap by text, type text, screenshot, lock screen |
| 🔋 | **Charging animation**: happy face, floating ⚡ and live battery % |
| 🎵 | **Music animation**: Piko bops and sings along with ♪ ♫ when music plays |
| 🔔 | **Suspicious notification eyes**: squinty eyes and slanted eyebrows when a notification arrives |
| 💙 | **Cute reactions**: poke it for a squish and floating hearts, it blinks, looks around, and falls asleep when idle |
| 🫧 | **Floating bubble**: leave the app and Piko shrinks into a small draggable ball (like a chat bubble) that keeps listening. Tap = talk, long-press = open Piko |
| 📱↔️ | **Portrait + landscape**: full-screen dome when your phone is sideways, a cute round ball when it's upright |
| 🎧 | **YouTube & Spotify search**: really searches and plays, not just opens the app |
| ⚙️ | **In-app settings**: paste your Gemini key once, no code editing needed |
| ⚡ | **Optimized**: GPU-friendly animations, pauses in the background, low battery use |

## 🗣️ Voice commands

| Say this | Piko does this |
|---|---|
| `open youtube` | Opens YouTube |
| `open youtube and search mr indian hacker` | Searches YouTube |
| `search who is araf on google` | Searches Google |
| `open spotify and play <song>` | Plays the song in Spotify |
| `play despacito on spotify` / `search lofi on spotify` | Plays / searches in Spotify |
| `open instagram` / `open camera` / any app name | Opens that app |
| `dim my brightness` / `increase my brightness` | Changes brightness |
| `brightness 50 percent` | Sets an exact level |
| `volume up` / `volume down` | Changes volume |
| `flashlight on` / `flashlight off` | Torch |
| `go back` / `go home` / `recent apps` | Navigation |
| `scroll down` / `scroll up` | Scrolls the screen |
| `click <text on screen>` | Taps a button by its label |
| `type <text>` | Types into the selected text box |
| `take a screenshot` / `lock the screen` | System actions |
| `what time is it` | Tells the time |
| `speak bangla` / `speak english` / `speak auto` | Sets the language |
| *anything else* | Piko chats with you using Gemini |

**Bangla works too:** `ইউটিউব খোল` · `ব্রাইটনেস কমাও` · `ব্রাইটনেস বাড়াও` · `আরাফ কে গুগলে সার্চ করো`

## 📲 Install

1. **Build the APK on GitHub (no Android Studio needed)**
   - Create a **private** GitHub repo and upload the contents of this folder (include the hidden `.github` folder).
   - Open **Actions → Build APK → Run workflow**. When it turns green, download **piko-apk**.
2. Unzip it and install `app-debug.apk` on your phone (allow *Install unknown apps* if asked).

## 🚀 First-time setup

1. Open Piko and allow the **microphone**.
2. The **Settings** screen opens on first launch: paste your **Gemini API key** and tap **Save key**.
   Get a free key at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).
   You can change it anytime from the ⚙ button in the top-right corner.
3. Turn on **Notification access → Piko** (for the suspicious-eyes reaction).
4. Turn on **Settings → Accessibility → Piko** (for back/home/scroll/tap/type).
   On Android 13+, you may first need *App info → ⋮ → Allow restricted settings*.
5. Allow **Display over other apps** for Piko (this powers the floating bubble and lets Piko open apps while you are outside the app).
6. The first time you ask for brightness, allow **Modify system settings**.
7. **Tap the dome** to start talking, and tap again to stop. Press Home and Piko turns into a floating ball: drag it anywhere, tap it to talk, long-press it to reopen the app. Use the notification's **Stop** button to close Piko completely.

## 🔐 Permissions & privacy

| Permission | Why |
|---|---|
| Microphone | Listen to your voice |
| Internet | Send your questions to Gemini |
| Accessibility service | Press buttons, scroll, type and navigate when you ask |
| Notification access | Make Piko react when a notification arrives (content is never read or stored) |
| Modify system settings | Change screen brightness |
| Query all packages | Find installed apps to open by name |
| Display over other apps | Show the floating bubble |
| Foreground service (microphone) | Keep listening while Piko is in the bubble |

Your API key is stored privately inside the app on your phone. Only the text of your questions is sent to Gemini. Keep your GitHub repo **private** and never share your key.

## 💡 Tips & troubleshooting

- Piko keeps listening in the floating bubble after you leave the app (while listening is on). Tap the bubble to turn listening on or off.
- If voice commands that open other apps do nothing from the bubble, make sure **Display over other apps** is allowed.
- For reliable Bangla, say **"speak bangla"**. Install the Bengali voice under *Settings → Text-to-speech → Google → Install voice data*.
- If notification reactions stop, set Piko's battery use to **Unrestricted**.
- If Piko says it can't reach its brain, check your internet and your API key in Settings.
- Gemini's free tier is rate-limited. If you hit the limit, wait a minute and try again.

## 🧩 Project structure

```
piko/
├─ app/src/main/
│  ├─ assets/index.html          # Character, animations, settings & about screen
│  ├─ java/com/araf/buddy/
│  │  ├─ MainActivity.kt         # Full-screen UI host
│  │  ├─ PikoService.kt          # Always-on brain: voice loop, commands, Gemini, floating bubble
│  │  ├─ Hub.kt                  # Shared state + JS bridge (settings, key)
│  │  ├─ PhoneService.kt         # Accessibility actions (back, scroll, click, type…)
│  │  └─ NotifService.kt         # Notification listener
│  └─ AndroidManifest.xml
└─ .github/workflows/build.yml   # Builds the APK on GitHub
```

## 👤 About

| | |
|---|---|
| **Author** | Araf |
| **Discord** | araf089 |
| **Community** | [Join the Discord server](https://discord.gg/PDtCFJR7PM) |
| **Version** | 1.2 |

<div align="center">

Made with 💙 by **Araf**

</div>
