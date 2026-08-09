-- Lays out the window of the macOS disk image.
--
-- jpackage substitutes every DEPLOY_ token before running this, and falls back to its own template
-- when the file is missing. The template positions extra content in rows of three and resizes the
-- window to fit them, which leaves the artwork — a fixed-size picture Finder never scales — covering
-- only the top of a window grown taller than it. Here the three items the image is known to hold are
-- placed by hand instead, on the grid documented in app/dmg/README.md.
--
-- The window bounds are the frame, not the content: the height carries the title bar on top of the
-- 440 points of artwork. 36 points of it were measured off a screenshot of the built image rather
-- than assumed — 28, the height a title bar is usually quoted at, left the artwork cut short and the
-- window scrolling.

tell application "Finder"
  set theDisk to a reference to (disks whose URL = "DEPLOY_VOLUME_URL")
  open theDisk

  set theWindow to a reference to (container window of disks whose URL = "DEPLOY_VOLUME_URL")

  set current view of theWindow to icon view
  set toolbar visible of theWindow to false
  set statusbar visible of theWindow to false
  set the bounds of theWindow to {400, 120, 1060, 596}

  set theViewOptions to a reference to the icon view options of theWindow
  set arrangement of theViewOptions to not arranged
  set icon size of theViewOptions to 128
  set text size of theViewOptions to 12
  set label position of theViewOptions to bottom
  set shows item info of theViewOptions to false
  set shows icon preview of theViewOptions to false
  set background picture of theViewOptions to POSIX file "DEPLOY_BG_FILE"

  -- The destination of the one gesture the image exists for.
  make new alias file at POSIX file "DEPLOY_VOLUME_PATH" to POSIX file "DEPLOY_INSTALL_LOCATION" with properties {name:"DEPLOY_INSTALL_LOCATION_DISPLAY_NAME"}

  -- Matched by name rather than addressed directly: Finder reports a bundle with or without its
  -- extension depending on the machine's settings, so "MJLogs.app" is not a name that can be relied
  -- on, while everything that is neither the alias nor the notices is the application.
  --
  -- The dotted names are tested first and parked together on the left, clear of the captions. A
  -- volume carries .background, .VolumeIcon.icns, .fseventsd and .Trashes, and a machine set to show
  -- hidden items shows every one of them: without a branch here they fall through to the
  -- application's position and pile their captions on top of its own, and left where Finder puts
  -- them they drag the content past the bottom of the window and bring up a scroll bar.
  set allTheFiles to the name of every item of theWindow
  repeat with theFile in allTheFiles
    set theName to theFile as string
    if theName starts with "." then
      set position of item theFile of theWindow to {68, 250}
    else if theName ends with "DEPLOY_INSTALL_LOCATION_DISPLAY_NAME" then
      set position of item theFile of theWindow to {475, 158}
    else if theName is "Licenses" then
      set position of item theFile of theWindow to {560, 328}
    else
      set position of item theFile of theWindow to {185, 158}
    end if
  end repeat

  update theDisk without registering applications
  delay 3
  close (get window of theDisk)

  -- A second pass, for the one item the first cannot reach.
  --
  -- .DS_Store is what stores everything above, so it does not exist until the window is closed. On a
  -- machine that shows hidden items it is then an item like any other, unplaced, and Finder drops it
  -- wherever it likes — below the window, which is what kept the scroll bar there after every other
  -- name had been parked. Opening the window again finds it and files it with the rest, and closing
  -- writes that position into the very file being positioned. Where nothing is hidden this pass sees
  -- no dotted names at all and only costs the reopen.
  delay 2
  open theDisk
  set theSecondWindow to a reference to (container window of disks whose URL = "DEPLOY_VOLUME_URL")
  set theRemainingFiles to the name of every item of theSecondWindow
  repeat with theFile in theRemainingFiles
    set theName to theFile as string
    if theName starts with "." then
      set position of item theFile of theSecondWindow to {68, 250}
    end if
  end repeat

  update theDisk without registering applications
  delay 3
  close (get window of theDisk)
end tell
