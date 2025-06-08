# Rocket Dodge Game

## MP3 Playback Setup

To enable MP3 playback in the game, you need to set up JavaFX:

1. Download JavaFX SDK from: https://gluonhq.com/products/javafx/
2. Extract the downloaded ZIP file to a location on your computer
3. Add the JavaFX libraries to your project:

### Using IDE (Eclipse, IntelliJ IDEA, etc.)

- Add JavaFX libraries to your project's build path
- For the modules, make sure to include at least:
  - javafx.base
  - javafx.media
  - javafx.swing

### Running from Command Line

When running the game from command line, add these VM arguments:

```
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.media,javafx.swing
```

Replace `/path/to/javafx-sdk` with the actual path where you extracted the JavaFX SDK.

## Alternative Solution

If you don't want to set up JavaFX, convert your MP3 file to WAV format using any online converter, and place the WAV file in the assets directory. Then in the Game class, call:

```java
playBackgroundMusic("c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\background_music.wav");
```

instead of the MP3 version.
