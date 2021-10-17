# SPPU
Spigot Premium Plugins Updater

### Installation
 - Minimum Java 8 required
 - Download the [SPPU.jar](https://github.com/Osiris-Team/SPPU/releases/latest) and put it into your servers root directory
 - Download the [startup script](https://github.com/Osiris-Team/SPPU/releases) for your servers operating system.
   - The startup file for Windows ends with .cmd
   - The startup file for Linux/Mac/etc. ends with .sh
 - Run the startup script. 

### Read the below, it's important!
- The usage of SPPU might get you in trouble. Remember to read the [lincense](LICENSE) before dealing with this software.
- SPPU will automatically install Node.js with Playwright (a headless-chrome driver) which will take around 300=500mb of extra disk space.
- Your server needs to have an GUI because headless-chrome currently can only bypass cloudflare in headfull (GUI) mode.
- Even though we aren't hurting spigotmc.org, it is against their terms of service to use bots/automation on their website, which means that you might get yourself banned (and lose all your paid plugins) by using the premium plugins updater (I already opened an issue on their spigot web api to add endpoints for third party application logins, but there was no response, so I doubt there will be another solution of automatically updating premium plugins in the future).
- This way of updating premium plugins is obviously more resource intensive and time demanding than it would be if spigotmc.org had a web/rest api of their own.
- SPPU will store the cookies used to login and your credentials in plain text, which makes it pretty easy for other people that have access to your servers files to get access to your spigotmc.org account (I opened an issue on GitHub to discuss how to fix this)
- This should be pretty much all, at least I can't think of any more downsides at the moment. The only upside of this is of course being able to automatically update your premium plugins 🎉
